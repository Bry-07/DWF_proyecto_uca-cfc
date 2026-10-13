package com.uca.cfc.mapper;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.cliente.ClienteResponse;
import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.Cliente;
import com.uca.cfc.entity.Empresa;

/**
 * Convierte los clientes en DTOs. Si el cliente es una empresa, incluye
 * tambien los datos de la empresa; si es una persona, ese campo va nulo.
 */
@Component
public class ClienteMapper {

    public ClienteResponse aResponse(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getTipo(), cliente.getNombre(), cliente.getDui(),
                cliente.getNit(), cliente.getEmail(), cliente.getTelefono(), cliente.getDireccion(),
                aResponse(cliente.getEmpresa()), cliente.getCreatedAt(), cliente.getUpdatedAt());
    }

    public ResumenReferencia aResumen(Cliente cliente) {
        return cliente == null ? null : new ResumenReferencia(cliente.getId(), cliente.getNombre());
    }

    private ClienteResponse.EmpresaResponse aResponse(Empresa empresa) {
        if (empresa == null) {
            return null;
        }
        return new ClienteResponse.EmpresaResponse(empresa.getId(), empresa.getRazonSocial(), empresa.getGiro(),
                empresa.getContactoNombre(), empresa.getContactoCargo());
    }
}
