package com.uca.cfc.dto.cliente;

import java.time.LocalDateTime;

import com.uca.cfc.entity.enums.TipoCliente;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cliente tal como se devuelve por la API.
 * El bloque de empresa solo viene cuando el cliente es de tipo EMPRESA.
 */
@Schema(description = "Cliente del centro")
public record ClienteResponse(
        Long id,
        TipoCliente tipo,
        String nombre,
        String dui,
        String nit,
        String email,
        String telefono,
        String direccion,
        EmpresaResponse empresa,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    @Schema(description = "Datos de empresa, presentes solo en clientes de tipo EMPRESA")
    public record EmpresaResponse(
            Long id,
            String razonSocial,
            String giro,
            String contactoNombre,
            String contactoCargo) {
    }
}
