package com.uca.cfc.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.pago.AbonoPagoRequest;
import com.uca.cfc.dto.pago.ComprobanteRequest;
import com.uca.cfc.dto.pago.ComprobanteResponse;
import com.uca.cfc.dto.pago.EstadoFinancieroResponse;
import com.uca.cfc.dto.pago.PagoRequest;
import com.uca.cfc.dto.pago.PagoResponse;
import com.uca.cfc.entity.Cliente;
import com.uca.cfc.entity.Comprobante;
import com.uca.cfc.entity.Pago;
import com.uca.cfc.entity.enums.ConceptoPago;
import com.uca.cfc.entity.enums.EstadoPago;
import com.uca.cfc.entity.enums.MetodoPago;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.PagoMapper;
import com.uca.cfc.repository.AlquilerRepository;
import com.uca.cfc.repository.ComprobanteRepository;
import com.uca.cfc.repository.CotizacionRepository;
import com.uca.cfc.repository.InscripcionRepository;
import com.uca.cfc.repository.PagoRepository;
import com.uca.cfc.repository.SolicitudCateringRepository;

/**
 * Gestión de pagos. El estado sigue el flujo financiero (PENDIENTE → PARCIAL →
 * PAGADO, sin retroceder) y la eliminación es lógica para conservar la
 * trazabilidad contable.
 */
@Service
@Transactional(readOnly = true)
public class PagoService {

    private static final String RECURSO = "Pago";
    private static final String PREFIJO_COMPROBANTE = "CFC";

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CotizacionRepository cotizacionRepository;
    private final AlquilerRepository alquilerRepository;
    private final SolicitudCateringRepository solicitudCateringRepository;
    private final ClienteService clienteService;
    private final PagoMapper mapper;
    private final Clock clock;

    public PagoService(PagoRepository pagoRepository, ComprobanteRepository comprobanteRepository,
            InscripcionRepository inscripcionRepository, CotizacionRepository cotizacionRepository,
            AlquilerRepository alquilerRepository, SolicitudCateringRepository solicitudCateringRepository,
            ClienteService clienteService, PagoMapper mapper, Clock clock) {
        this.pagoRepository = pagoRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.alquilerRepository = alquilerRepository;
        this.solicitudCateringRepository = solicitudCateringRepository;
        this.clienteService = clienteService;
        this.mapper = mapper;
        this.clock = clock;
    }

    public PageResponse<PagoResponse> listar(Long clienteId, ConceptoPago concepto, EstadoPago estado,
            MetodoPago metodo, LocalDate desde, LocalDate hasta, Pageable pageable) {
        return PageResponse.de(pagoRepository.buscar(clienteId, concepto, estado, metodo, desde, hasta, pageable),
                pago -> mapper.aResponse(pago, comprobanteRepository.existsByPagoId(pago.getId())));
    }

    public PageResponse<PagoResponse> listarPorCliente(Long clienteId, EstadoPago estado, Pageable pageable) {
        clienteService.buscar(clienteId);
        return listar(clienteId, null, estado, null, null, null, pageable);
    }

    public PagoResponse obtener(Long id) {
        Pago pago = buscar(id);
        return mapper.aResponse(pago, comprobanteRepository.existsByPagoId(id));
    }

    @Transactional
    public PagoResponse crear(PagoRequest request) {
        Cliente cliente = clienteService.buscar(request.clienteId());
        validarReferencia(request.concepto(), request.referenciaId());

        BigDecimal montoPagado = request.montoPagado() == null ? BigDecimal.ZERO : request.montoPagado();
        if (montoPagado.compareTo(request.montoEsperado()) > 0) {
            throw new BusinessException("El monto pagado no puede superar el monto esperado");
        }

        Pago pago = new Pago();
        pago.setCliente(cliente);
        pago.setConcepto(request.concepto());
        pago.setReferenciaId(request.referenciaId());
        pago.setMontoEsperado(request.montoEsperado());
        pago.setMontoPagado(montoPagado);
        pago.setMetodo(request.metodo());
        pago.setFechaPago(request.fechaPago() == null ? LocalDate.now(clock) : request.fechaPago());
        pago.setReferenciaBancaria(request.referenciaBancaria());
        pago.setEstado(calcularEstado(montoPagado, request.montoEsperado()));

        Pago guardado = pagoRepository.save(pago);
        log.info("Pago {} registrado por {} para el concepto {}", guardado.getId(), montoPagado,
                request.concepto());
        return mapper.aResponse(guardado, false);
    }

    /** Registra un abono adicional y recalcula el estado del pago. */
    @Transactional
    public PagoResponse abonar(Long id, AbonoPagoRequest request) {
        Pago pago = buscar(id);
        if (pago.getEstado() == EstadoPago.PAGADO) {
            throw new BusinessException("El pago ya está saldado");
        }

        BigDecimal nuevoMonto = pago.getMontoPagado().add(request.monto());
        if (nuevoMonto.compareTo(pago.getMontoEsperado()) > 0) {
            throw new BusinessException("El abono excede el saldo pendiente de " + pago.getSaldo());
        }

        pago.setMontoPagado(nuevoMonto);
        pago.setEstado(calcularEstado(nuevoMonto, pago.getMontoEsperado()));
        if (request.referenciaBancaria() != null && !request.referenciaBancaria().isBlank()) {
            pago.setReferenciaBancaria(request.referenciaBancaria().trim());
        }
        return mapper.aResponse(pagoRepository.save(pago), comprobanteRepository.existsByPagoId(id));
    }

    /** Validación financiera, a cargo del área de contabilidad. */
    @Transactional
    public PagoResponse validar(Long id, String responsable) {
        Pago pago = buscar(id);
        if (pago.estaValidado()) {
            throw new BusinessException("El pago ya fue validado el " + pago.getFechaValidacion());
        }
        if (pago.getEstado() == EstadoPago.PENDIENTE) {
            throw new BusinessException("No se puede validar un pago sin monto recibido");
        }
        pago.setFechaValidacion(LocalDateTime.now(clock));
        pago.setValidadoPor(responsable.trim());
        return mapper.aResponse(pagoRepository.save(pago), comprobanteRepository.existsByPagoId(id));
    }

    /** Confirmación en recepción, posterior a la validación financiera. */
    @Transactional
    public PagoResponse confirmar(Long id, String responsable) {
        Pago pago = buscar(id);
        if (!pago.estaValidado()) {
            throw new BusinessException("El pago debe ser validado por contabilidad antes de confirmarse");
        }
        if (pago.estaConfirmado()) {
            throw new BusinessException("El pago ya fue confirmado el " + pago.getFechaConfirmacion());
        }
        pago.setFechaConfirmacion(LocalDateTime.now(clock));
        pago.setConfirmadoPor(responsable.trim());
        return mapper.aResponse(pagoRepository.save(pago), comprobanteRepository.existsByPagoId(id));
    }

    @Transactional
    public PagoResponse cambiarEstado(Long id, EstadoPago destino) {
        Pago pago = buscar(id);
        EstadoPago actual = pago.getEstado();

        if (actual == destino) {
            return mapper.aResponse(pago, comprobanteRepository.existsByPagoId(id));
        }
        if (!actual.puedeCambiarA(destino)) {
            throw new BusinessException("El flujo financiero no permite pasar el pago de " + actual + " a "
                    + destino);
        }

        // El monto acompaña al estado: marcar PAGADO salda el registro
        if (destino == EstadoPago.PAGADO) {
            pago.setMontoPagado(pago.getMontoEsperado());
        }
        pago.setEstado(destino);
        log.info("Pago {}: estado {} → {}", id, actual, destino);
        return mapper.aResponse(pagoRepository.save(pago), comprobanteRepository.existsByPagoId(id));
    }

    /** Borrado lógico: el registro deja de aparecer pero permanece en la base de datos. */
    @Transactional
    public void eliminar(Long id) {
        Pago pago = buscar(id);
        if (comprobanteRepository.existsByPagoId(id)) {
            throw new BusinessException(
                    "No se puede eliminar un pago con comprobante emitido. Anule primero el comprobante");
        }
        pago.setEliminado(true);
        pagoRepository.save(pago);
        log.info("Pago {} marcado como eliminado (borrado lógico)", id);
    }

    // --------------------------- Comprobantes ---------------------------

    @Transactional
    public ComprobanteResponse emitirComprobante(Long pagoId, ComprobanteRequest request) {
        Pago pago = buscar(pagoId);
        if (comprobanteRepository.existsByPagoId(pagoId)) {
            throw new DuplicateResourceException("El pago ya tiene un comprobante emitido");
        }
        if (pago.getMontoPagado().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("No se puede emitir un comprobante de un pago sin monto recibido");
        }

        Comprobante comprobante = new Comprobante();
        comprobante.setPago(pago);
        comprobante.setNumero(generarNumero());
        comprobante.setFechaEmision(LocalDateTime.now(clock));
        comprobante.setEmitidoA(pago.getCliente().getNombre());
        comprobante.setMonto(pago.getMontoPagado());
        comprobante.setObservaciones(request == null ? null : request.observaciones());

        return mapper.aResponse(comprobanteRepository.save(comprobante));
    }

    public ComprobanteResponse obtenerComprobante(Long pagoId) {
        buscar(pagoId);
        return comprobanteRepository.findByPagoId(pagoId).map(mapper::aResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El pago " + pagoId + " todavía no tiene comprobante emitido"));
    }

    // ------------------------ Estados financieros ------------------------

    public EstadoFinancieroResponse reporteEstadosFinancieros(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessException("La fecha 'hasta' no puede ser anterior a la fecha 'desde'");
        }

        List<EstadoFinancieroResponse.ResumenEstado> porEstado = new ArrayList<>();
        BigDecimal totalCobrado = BigDecimal.ZERO;
        BigDecimal totalPorCobrar = BigDecimal.ZERO;
        long cantidadPagos = 0;

        for (Object[] fila : pagoRepository.resumirPorEstado(desde, hasta)) {
            EstadoPago estado = (EstadoPago) fila[0];
            long cantidad = (long) fila[1];
            BigDecimal esperado = (BigDecimal) fila[2];
            BigDecimal pagado = (BigDecimal) fila[3];

            porEstado.add(new EstadoFinancieroResponse.ResumenEstado(estado, cantidad, esperado, pagado,
                    esperado.subtract(pagado)));
            totalCobrado = totalCobrado.add(pagado);
            totalPorCobrar = totalPorCobrar.add(esperado.subtract(pagado));
            cantidadPagos += cantidad;
        }

        List<EstadoFinancieroResponse.ResumenMetodo> porMetodo = pagoRepository.resumirPorMetodo(desde, hasta)
                .stream()
                .map(fila -> new EstadoFinancieroResponse.ResumenMetodo((MetodoPago) fila[0], (long) fila[1],
                        (BigDecimal) fila[2]))
                .toList();

        return new EstadoFinancieroResponse(desde, hasta, totalCobrado, totalPorCobrar, cantidadPagos, porEstado,
                porMetodo);
    }

    public Pago buscar(Long id) {
        return pagoRepository.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private EstadoPago calcularEstado(BigDecimal pagado, BigDecimal esperado) {
        if (pagado.compareTo(BigDecimal.ZERO) == 0) {
            return EstadoPago.PENDIENTE;
        }
        return pagado.compareTo(esperado) >= 0 ? EstadoPago.PAGADO : EstadoPago.PARCIAL;
    }

    /** El pago debe apuntar a un registro real del concepto declarado. */
    private void validarReferencia(ConceptoPago concepto, Long referenciaId) {
        boolean existe = switch (concepto) {
            case INSCRIPCION -> inscripcionRepository.existsById(referenciaId);
            case COTIZACION -> cotizacionRepository.existsById(referenciaId);
            case ALQUILER -> alquilerRepository.existsById(referenciaId);
            case CATERING -> solicitudCateringRepository.existsById(referenciaId);
        };
        if (!existe) {
            throw new ResourceNotFoundException(
                    "No existe " + concepto.name().toLowerCase() + " con id " + referenciaId);
        }
    }

    private String generarNumero() {
        int anio = LocalDate.now(clock).getYear();
        String prefijo = PREFIJO_COMPROBANTE + "-" + anio + "-";
        long correlativo = comprobanteRepository.countByNumeroStartingWith(prefijo) + 1;
        return prefijo + String.format("%06d", correlativo);
    }
}
