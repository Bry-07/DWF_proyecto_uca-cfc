package com.uca.cfc.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uca.cfc.dto.cliente.ClienteRequest;
import com.uca.cfc.dto.cliente.ClienteResponse;
import com.uca.cfc.dto.cliente.HistorialClienteResponse;
import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.inscripcion.InscripcionResponse;
import com.uca.cfc.dto.pago.PagoResponse;
import com.uca.cfc.entity.enums.EstadoInscripcion;
import com.uca.cfc.entity.enums.EstadoPago;
import com.uca.cfc.entity.enums.TipoCliente;
import com.uca.cfc.service.ClienteService;
import com.uca.cfc.service.InscripcionService;
import com.uca.cfc.service.PagoService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de los clientes, que pueden ser personas particulares o empresas.
 * Ademas del CRUD tiene el endpoint /{id}/historial, que junta en una sola
 * respuesta las inscripciones, cotizaciones y pagos del cliente.
 */
@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "Personas particulares y empresas atendidas por el centro")
public class ClienteController {

    private final ClienteService clienteService;
    private final InscripcionService inscripcionService;
    private final PagoService pagoService;

    public ClienteController(ClienteService clienteService, InscripcionService inscripcionService,
            PagoService pagoService) {
        this.clienteService = clienteService;
        this.inscripcionService = inscripcionService;
        this.pagoService = pagoService;
    }

    @GetMapping
    @Operation(summary = "Listar clientes",
            description = "Filtros por nombre, DUI, NIT, tipo y razón social de la empresa.")
    public PageResponse<ClienteResponse> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String dui,
            @RequestParam(required = false) String nit,
            @RequestParam(required = false) TipoCliente tipo,
            @Parameter(description = "Coincidencia parcial de la razón social")
            @RequestParam(required = false) String empresa,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return clienteService.listar(nombre, dui, nit, tipo, empresa, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un cliente")
    public ClienteResponse obtener(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Consultar el historial de un cliente",
            description = """
                    Devuelve las inscripciones, cotizaciones y pagos del cliente junto con
                    un resumen de totales. A partir de la Fase 3, un cliente solo podrá
                    consultar su propio historial.""")
    public HistorialClienteResponse historial(@PathVariable Long id) {
        return clienteService.historial(id);
    }

    @GetMapping("/{id}/inscripciones")
    @Operation(summary = "Listar las inscripciones de un cliente")
    public PageResponse<InscripcionResponse> inscripciones(@PathVariable Long id,
            @RequestParam(required = false) EstadoInscripcion estado,
            @PageableDefault(size = 10, sort = "fechaInscripcion", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return inscripcionService.listarPorCliente(id, estado, pageable);
    }

    @GetMapping("/{id}/pagos")
    @Operation(summary = "Listar los pagos de un cliente")
    public PageResponse<PagoResponse> pagos(@PathVariable Long id,
            @RequestParam(required = false) EstadoPago estado,
            @PageableDefault(size = 10, sort = "fechaPago", direction = Sort.Direction.DESC) Pageable pageable) {
        return pagoService.listarPorCliente(id, estado, pageable);
    }

    @PostMapping
    @Operation(summary = "Registrar un cliente",
            description = """
                    Una PERSONA_PARTICULAR requiere DUI; una EMPRESA requiere NIT y los
                    datos de empresa (razón social, giro y contacto).""")
    @ApiResponse(responseCode = "201", description = "Cliente registrado")
    @ApiResponse(responseCode = "409", description = "Ya existe un cliente con ese correo, DUI o NIT")
    @ApiResponse(responseCode = "422", description = "La identificación no corresponde al tipo de cliente")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse creado = clienteService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un cliente")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un cliente",
            description = "No se permite si el cliente tiene movimientos registrados en el sistema.")
    @ApiResponse(responseCode = "204", description = "Cliente eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
