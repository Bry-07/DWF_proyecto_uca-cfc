package com.uca.cfc.mapper;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.pago.ComprobanteResponse;
import com.uca.cfc.dto.pago.PagoResponse;
import com.uca.cfc.entity.Comprobante;
import com.uca.cfc.entity.Pago;
import com.uca.cfc.util.CodigoUtil;

/**
 * Convierte los pagos y comprobantes en DTOs.
 * Calcula el saldo pendiente y arma el codigo visible del pago (PAG-0231).
 */
@Component
public class PagoMapper {

    private static final String PREFIJO = "PAG";

    private final ClienteMapper clienteMapper;

    public PagoMapper(ClienteMapper clienteMapper) {
        this.clienteMapper = clienteMapper;
    }

    public PagoResponse aResponse(Pago pago, boolean tieneComprobante) {
        return new PagoResponse(pago.getId(), CodigoUtil.formatear(PREFIJO, pago.getId(), 4),
                clienteMapper.aResumen(pago.getCliente()), pago.getConcepto(), pago.getReferenciaId(),
                pago.getMontoEsperado(), pago.getMontoPagado(), pago.getSaldo(), pago.getMetodo(), pago.getEstado(),
                pago.getFechaPago(), pago.getReferenciaBancaria(), pago.getFechaValidacion(), pago.getValidadoPor(),
                pago.getFechaConfirmacion(), pago.getConfirmadoPor(), tieneComprobante, pago.getCreatedAt(),
                pago.getUpdatedAt());
    }

    public ComprobanteResponse aResponse(Comprobante comprobante) {
        Pago pago = comprobante.getPago();
        return new ComprobanteResponse(comprobante.getId(), comprobante.getNumero(), pago.getId(),
                CodigoUtil.formatear(PREFIJO, pago.getId(), 4), comprobante.getEmitidoA(), comprobante.getMonto(),
                pago.getMetodo(), comprobante.getFechaEmision(), comprobante.getObservaciones());
    }
}
