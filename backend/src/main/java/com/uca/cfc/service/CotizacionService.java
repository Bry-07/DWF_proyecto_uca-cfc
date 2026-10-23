package com.uca.cfc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.cotizacion.CotizacionItemRequest;
import com.uca.cfc.dto.cotizacion.CotizacionRequest;
import com.uca.cfc.dto.cotizacion.CotizacionResponse;
import com.uca.cfc.entity.Cotizacion;
import com.uca.cfc.entity.CotizacionItem;
import com.uca.cfc.entity.enums.EstadoCotizacion;
import com.uca.cfc.entity.enums.TipoCotizacion;
import com.uca.cfc.entity.enums.TipoItemCotizacion;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.CotizacionMapper;
import com.uca.cfc.repository.CotizacionRepository;

/**
 * Reglas de negocio de las cotizaciones.
 * Calcula el total sumando los items, deduce el tipo de cotizacion a partir de
 * ellos, y controla el flujo de aprobacion: una cotizacion vencida no se puede
 * aprobar y una ya resuelta no se puede volver a resolver.
 */
@Service
@Transactional(readOnly = true)
public class CotizacionService {

    private static final String RECURSO = "Cotización";
    private static final int DIAS_VIGENCIA_POR_DEFECTO = 30;

    private final CotizacionRepository cotizacionRepository;
    private final ClienteService clienteService;
    private final CotizacionMapper mapper;
    private final Clock clock;

    public CotizacionService(CotizacionRepository cotizacionRepository, ClienteService clienteService,
            CotizacionMapper mapper, Clock clock) {
        this.cotizacionRepository = cotizacionRepository;
        this.clienteService = clienteService;
        this.mapper = mapper;
        this.clock = clock;
    }

    public PageResponse<CotizacionResponse> listar(Long clienteId, TipoCotizacion tipo, EstadoCotizacion estado,
            LocalDate desde, LocalDate hasta, Pageable pageable) {
        return PageResponse.de(cotizacionRepository.buscar(clienteId, tipo, estado, desde, hasta, pageable),
                mapper::aResponse);
    }

    public CotizacionResponse obtener(Long id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public CotizacionResponse crear(CotizacionRequest request) {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(clienteService.buscar(request.clienteId()));

        LocalDate solicitud = request.fechaSolicitud() == null ? LocalDate.now(clock) : request.fechaSolicitud();
        LocalDate vencimiento = request.fechaVencimiento() == null
                ? solicitud.plusDays(DIAS_VIGENCIA_POR_DEFECTO) : request.fechaVencimiento();
        validarFechas(solicitud, vencimiento);

        cotizacion.setFechaSolicitud(solicitud);
        cotizacion.setFechaVencimiento(vencimiento);
        aplicarDatosGenerales(request, cotizacion);
        reemplazarItems(cotizacion, request.items());
        cotizacion.setEstado(EstadoCotizacion.GENERADA);

        return mapper.aResponse(cotizacionRepository.save(cotizacion));
    }

    @Transactional
    public CotizacionResponse actualizar(Long id, CotizacionRequest request) {
        Cotizacion cotizacion = buscar(id);
        if (!cotizacion.getEstado().esEditable()) {
            throw new BusinessException("No se puede modificar una cotización en estado " + cotizacion.getEstado());
        }

        LocalDate solicitud = request.fechaSolicitud() == null ? cotizacion.getFechaSolicitud()
                : request.fechaSolicitud();
        LocalDate vencimiento = request.fechaVencimiento() == null
                ? solicitud.plusDays(DIAS_VIGENCIA_POR_DEFECTO) : request.fechaVencimiento();
        validarFechas(solicitud, vencimiento);

        cotizacion.setCliente(clienteService.buscar(request.clienteId()));
        cotizacion.setFechaSolicitud(solicitud);
        cotizacion.setFechaVencimiento(vencimiento);
        aplicarDatosGenerales(request, cotizacion);
        reemplazarItems(cotizacion, request.items());

        return mapper.aResponse(cotizacionRepository.save(cotizacion));
    }

    @Transactional
    public CotizacionResponse aprobar(Long id, String comentario) {
        return resolver(id, EstadoCotizacion.APROBADA, comentario);
    }

    @Transactional
    public CotizacionResponse rechazar(Long id, String comentario) {
        return resolver(id, EstadoCotizacion.RECHAZADA, comentario);
    }

    @Transactional
    public void eliminar(Long id) {
        Cotizacion cotizacion = buscar(id);
        if (cotizacion.getEstado() == EstadoCotizacion.APROBADA) {
            throw new BusinessException(
                    "No se puede eliminar una cotización aprobada porque respalda un compromiso con el cliente");
        }
        cotizacionRepository.delete(cotizacion);
    }

    public Cotizacion buscar(Long id) {
        return cotizacionRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private CotizacionResponse resolver(Long id, EstadoCotizacion destino, String comentario) {
        Cotizacion cotizacion = buscar(id);
        if (!cotizacion.getEstado().esEditable()) {
            throw new BusinessException("La cotización ya fue resuelta: su estado actual es "
                    + cotizacion.getEstado());
        }
        if (destino == EstadoCotizacion.APROBADA && cotizacion.getFechaVencimiento().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("La cotización venció el " + cotizacion.getFechaVencimiento()
                    + " y no puede aprobarse. Genere una nueva");
        }

        cotizacion.setEstado(destino);
        if (comentario != null && !comentario.isBlank()) {
            cotizacion.setNotaInterna(comentario.trim());
        }
        return mapper.aResponse(cotizacionRepository.save(cotizacion));
    }

    private void aplicarDatosGenerales(CotizacionRequest request, Cotizacion cotizacion) {
        cotizacion.setAsunto(request.asunto().trim());
        cotizacion.setNotaInterna(request.notaInterna());
        cotizacion.setResponsableRevision(request.responsableRevision());
    }

    /** Reconstruye las líneas, recalcula el total y deduce el tipo de la cotización. */
    private void reemplazarItems(Cotizacion cotizacion, List<CotizacionItemRequest> items) {
        cotizacion.limpiarItems();

        BigDecimal total = BigDecimal.ZERO;
        Set<TipoItemCotizacion> tipos = EnumSet.noneOf(TipoItemCotizacion.class);

        for (CotizacionItemRequest itemRequest : items) {
            CotizacionItem item = new CotizacionItem();
            item.setTipo(itemRequest.tipo());
            item.setConcepto(itemRequest.concepto().trim());
            item.setDetalle(itemRequest.detalle());
            item.setReferenciaId(itemRequest.referenciaId());
            item.setCantidad(itemRequest.cantidad());
            item.setPrecioUnitario(itemRequest.precioUnitario());
            item.calcularSubtotal();

            cotizacion.agregarItem(item);
            tipos.add(itemRequest.tipo());
            total = total.add(item.getSubtotal());
        }

        cotizacion.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        cotizacion.setTipo(deducirTipo(tipos));
    }

    private TipoCotizacion deducirTipo(Set<TipoItemCotizacion> tipos) {
        return tipos.size() == 1 ? tipos.iterator().next().comoTipoCotizacion() : TipoCotizacion.COMBINADA;
    }

    private void validarFechas(LocalDate solicitud, LocalDate vencimiento) {
        if (vencimiento.isBefore(solicitud)) {
            throw new BusinessException("La fecha de vencimiento no puede ser anterior a la fecha de solicitud");
        }
    }
}
