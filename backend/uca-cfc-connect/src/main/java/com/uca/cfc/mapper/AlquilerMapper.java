package com.uca.cfc.mapper;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.alquiler.AlquilerResponse;
import com.uca.cfc.entity.Alquiler;
import com.uca.cfc.util.CodigoUtil;

/**
 * Convierte los alquileres en DTOs.
 * De paso calcula cuantas horas dura la reserva, que es el dato con el que se
 * cobra el monto.
 */
@Component
public class AlquilerMapper {

    private static final String PREFIJO = "ALQ";
    private static final double MINUTOS_POR_HORA = 60.0;

    private final ClienteMapper clienteMapper;
    private final CatalogoMapper catalogoMapper;

    public AlquilerMapper(ClienteMapper clienteMapper, CatalogoMapper catalogoMapper) {
        this.clienteMapper = clienteMapper;
        this.catalogoMapper = catalogoMapper;
    }

    public AlquilerResponse aResponse(Alquiler alquiler) {
        double horas = alquiler.getIntervalo().duracion().toMinutes() / MINUTOS_POR_HORA;

        return new AlquilerResponse(alquiler.getId(), CodigoUtil.formatear(PREFIJO, alquiler.getId(), 4),
                clienteMapper.aResumen(alquiler.getCliente()), catalogoMapper.aResumen(alquiler.getEspacio()),
                alquiler.getFechaHoraInicio(), alquiler.getFechaHoraFin(), horas, alquiler.getProposito(),
                alquiler.getCantidadAsistentes(), alquiler.getMontoTotal(), alquiler.getEstado(),
                alquiler.getObservaciones(), alquiler.getCreatedAt(), alquiler.getUpdatedAt());
    }
}
