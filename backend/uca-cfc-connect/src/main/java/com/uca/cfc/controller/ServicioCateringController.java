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

import com.uca.cfc.dto.catering.ServicioCateringRequest;
import com.uca.cfc.dto.catering.ServicioCateringResponse;
import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.entity.enums.TipoServicioCatering;
import com.uca.cfc.service.CateringService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador del catalogo de servicios de catering (coffee break, almuerzo...).
 * Es el catalogo de lo que ofrece el centro, no las solicitudes de los clientes.
 */
@RestController
@RequestMapping("/api/v1/servicios-catering")
@Tag(name = "Servicios de catering", description = "Catálogo de servicios de alimentación del centro")
public class ServicioCateringController {

    private final CateringService cateringService;

    public ServicioCateringController(CateringService cateringService) {
        this.cateringService = cateringService;
    }

    @GetMapping
    @Operation(summary = "Listar servicios de catering", description = "Filtros por tipo de servicio y estado.")
    public PageResponse<ServicioCateringResponse> listar(
            @RequestParam(required = false) TipoServicioCatering tipo,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return cateringService.listarServicios(tipo, activo, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un servicio de catering")
    public ServicioCateringResponse obtener(@PathVariable Long id) {
        return cateringService.obtenerServicio(id);
    }

    @PostMapping
    @Operation(summary = "Crear un servicio de catering")
    @ApiResponse(responseCode = "201", description = "Servicio creado")
    public ResponseEntity<ServicioCateringResponse> crear(@Valid @RequestBody ServicioCateringRequest request) {
        ServicioCateringResponse creado = cateringService.crearServicio(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un servicio de catering")
    public ServicioCateringResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody ServicioCateringRequest request) {
        return cateringService.actualizarServicio(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un servicio de catering",
            description = "Si tiene solicitudes registradas, desactívelo en lugar de eliminarlo.")
    @ApiResponse(responseCode = "204", description = "Servicio eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cateringService.eliminarServicio(id);
        return ResponseEntity.noContent().build();
    }
}
