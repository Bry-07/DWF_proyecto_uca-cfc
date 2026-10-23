package com.uca.cfc.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.cliente.ClienteRequest;
import com.uca.cfc.dto.cliente.ClienteResponse;
import com.uca.cfc.dto.cliente.EmpresaRequest;
import com.uca.cfc.dto.cliente.HistorialClienteResponse;
import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.cotizacion.CotizacionResponse;
import com.uca.cfc.dto.inscripcion.InscripcionResponse;
import com.uca.cfc.dto.pago.PagoResponse;
import com.uca.cfc.entity.Cliente;
import com.uca.cfc.entity.Empresa;
import com.uca.cfc.entity.enums.TipoCliente;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceInUseException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.ClienteMapper;
import com.uca.cfc.mapper.CotizacionMapper;
import com.uca.cfc.mapper.InscripcionMapper;
import com.uca.cfc.mapper.PagoMapper;
import com.uca.cfc.repository.AlquilerRepository;
import com.uca.cfc.repository.ClienteRepository;
import com.uca.cfc.repository.CotizacionRepository;
import com.uca.cfc.repository.InscripcionRepository;
import com.uca.cfc.repository.PagoRepository;
import com.uca.cfc.repository.SolicitudCateringRepository;

/**
 * Reglas de negocio de los clientes.
 * Se encarga de revisar que el tipo de cliente y su identificacion sean
 * coherentes (persona con DUI, empresa con NIT), que no se repitan correo,
 * DUI ni NIT, y de armar el historial consolidado del cliente.
 */
@Service
@Transactional(readOnly = true)
public class ClienteService {

    private static final String RECURSO = "Cliente";

    private final ClienteRepository clienteRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CotizacionRepository cotizacionRepository;
    private final PagoRepository pagoRepository;
    private final AlquilerRepository alquilerRepository;
    private final SolicitudCateringRepository solicitudCateringRepository;
    private final ClienteMapper clienteMapper;
    private final InscripcionMapper inscripcionMapper;
    private final CotizacionMapper cotizacionMapper;
    private final PagoMapper pagoMapper;

    public ClienteService(ClienteRepository clienteRepository, InscripcionRepository inscripcionRepository,
            CotizacionRepository cotizacionRepository, PagoRepository pagoRepository,
            AlquilerRepository alquilerRepository, SolicitudCateringRepository solicitudCateringRepository,
            ClienteMapper clienteMapper, InscripcionMapper inscripcionMapper, CotizacionMapper cotizacionMapper,
            PagoMapper pagoMapper) {
        this.clienteRepository = clienteRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.pagoRepository = pagoRepository;
        this.alquilerRepository = alquilerRepository;
        this.solicitudCateringRepository = solicitudCateringRepository;
        this.clienteMapper = clienteMapper;
        this.inscripcionMapper = inscripcionMapper;
        this.cotizacionMapper = cotizacionMapper;
        this.pagoMapper = pagoMapper;
    }

    public PageResponse<ClienteResponse> listar(String nombre, String dui, String nit, TipoCliente tipo,
            String empresa, Pageable pageable) {
        return PageResponse.de(clienteRepository.buscar(normalizar(nombre), normalizar(dui), normalizar(nit), tipo,
                normalizar(empresa), pageable), clienteMapper::aResponse);
    }

    public ClienteResponse obtener(Long id) {
        return clienteMapper.aResponse(buscar(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        validarCoherencia(request);
        validarUnicidad(request, null);

        Cliente cliente = new Cliente();
        aplicar(request, cliente);
        return clienteMapper.aResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscar(id);
        validarCoherencia(request);
        validarUnicidad(request, id);

        aplicar(request, cliente);
        return clienteMapper.aResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscar(id);
        boolean tieneMovimientos = inscripcionRepository.existsByClienteId(id)
                || cotizacionRepository.existsByClienteId(id)
                || pagoRepository.existsByClienteId(id)
                || alquilerRepository.existsByClienteId(id)
                || solicitudCateringRepository.existsByClienteId(id);
        if (tieneMovimientos) {
            throw new ResourceInUseException("No se puede eliminar al cliente '" + cliente.getNombre()
                    + "' porque tiene inscripciones, cotizaciones, alquileres, catering o pagos registrados");
        }
        clienteRepository.delete(cliente);
    }

    /** Historial consolidado del cliente: inscripciones, cotizaciones y pagos. */
    public HistorialClienteResponse historial(Long id) {
        Cliente cliente = buscar(id);

        List<InscripcionResponse> inscripciones = inscripcionRepository
                .findByClienteIdOrderByFechaInscripcionDesc(id).stream().map(inscripcionMapper::aResponse).toList();
        List<CotizacionResponse> cotizaciones = cotizacionRepository
                .findByClienteIdOrderByFechaSolicitudDesc(id).stream().map(cotizacionMapper::aResponse).toList();
        List<PagoResponse> pagos = pagoRepository.findByClienteIdOrderByFechaPagoDesc(id).stream()
                .map(pago -> pagoMapper.aResponse(pago, pago.getComprobante() != null)).toList();

        BigDecimal montoPagado = pagoRepository.totalPagadoPorCliente(id);
        HistorialClienteResponse.Resumen resumen = new HistorialClienteResponse.Resumen(inscripciones.size(),
                cotizaciones.size(), pagos.size(), montoPagado == null ? BigDecimal.ZERO : montoPagado);

        return new HistorialClienteResponse(cliente.getId(), cliente.getNombre(), resumen, inscripciones,
                cotizaciones, pagos);
    }

    public Cliente buscar(Long id) {
        return clienteRepository.findWithEmpresaById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    /** Una persona se identifica con DUI; una empresa con NIT y su ficha de empresa. */
    private void validarCoherencia(ClienteRequest request) {
        if (request.tipo() == TipoCliente.PERSONA_PARTICULAR) {
            if (esVacio(request.dui())) {
                throw new BusinessException("El DUI es obligatorio para un cliente de tipo PERSONA_PARTICULAR");
            }
        } else {
            if (esVacio(request.nit())) {
                throw new BusinessException("El NIT es obligatorio para un cliente de tipo EMPRESA");
            }
            if (request.empresa() == null) {
                throw new BusinessException("Los datos de empresa son obligatorios para un cliente de tipo EMPRESA");
            }
        }
    }

    private void validarUnicidad(ClienteRequest request, Long id) {
        String email = request.email().trim();
        boolean emailRepetido = (id == null) ? clienteRepository.existsByEmailIgnoreCase(email)
                : clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, id);
        if (emailRepetido) {
            throw new DuplicateResourceException("Ya existe un cliente con el correo '" + email + "'");
        }

        if (!esVacio(request.dui())) {
            boolean duiRepetido = (id == null) ? clienteRepository.existsByDui(request.dui())
                    : clienteRepository.existsByDuiAndIdNot(request.dui(), id);
            if (duiRepetido) {
                throw new DuplicateResourceException("Ya existe un cliente con el DUI '" + request.dui() + "'");
            }
        }

        if (!esVacio(request.nit())) {
            boolean nitRepetido = (id == null) ? clienteRepository.existsByNit(request.nit())
                    : clienteRepository.existsByNitAndIdNot(request.nit(), id);
            if (nitRepetido) {
                throw new DuplicateResourceException("Ya existe un cliente con el NIT '" + request.nit() + "'");
            }
        }
    }

    private void aplicar(ClienteRequest request, Cliente cliente) {
        cliente.setTipo(request.tipo());
        cliente.setNombre(request.nombre().trim());
        cliente.setEmail(request.email().trim().toLowerCase());
        cliente.setTelefono(normalizar(request.telefono()));
        cliente.setDireccion(request.direccion());

        if (request.tipo() == TipoCliente.PERSONA_PARTICULAR) {
            cliente.setDui(normalizar(request.dui()));
            cliente.setNit(normalizar(request.nit()));
            // orphanRemoval elimina la ficha de empresa si el cliente deja de serlo
            cliente.setEmpresa(null);
        } else {
            cliente.setDui(null);
            cliente.setNit(normalizar(request.nit()));
            aplicarEmpresa(request.empresa(), cliente);
        }
    }

    private void aplicarEmpresa(EmpresaRequest request, Cliente cliente) {
        Empresa empresa = cliente.getEmpresa() == null ? new Empresa() : cliente.getEmpresa();
        empresa.setRazonSocial(request.razonSocial().trim());
        empresa.setGiro(request.giro());
        empresa.setContactoNombre(request.contactoNombre());
        empresa.setContactoCargo(request.contactoCargo());
        cliente.setEmpresa(empresa);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String normalizar(String valor) {
        return esVacio(valor) ? null : valor.trim();
    }
}
