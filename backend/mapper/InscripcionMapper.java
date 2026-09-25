package com.uca.cfc.mapper;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.dto.inscripcion.InscripcionResponse;
import com.uca.cfc.entity.Inscripcion;
import com.uca.cfc.util.CodigoUtil;

/**
 * Convierte las inscripciones en DTOs.
 * Arma el codigo visible (INS-0182) a partir del id, y resuelve si la
 * inscripcion es de un curso o de un diplomado para devolver la oferta correcta.
 */
@Component
public class InscripcionMapper {

    private static final String PREFIJO = "INS";

    private final ClienteMapper clienteMapper;

    public InscripcionMapper(ClienteMapper clienteMapper) {
        this.clienteMapper = clienteMapper;
    }

    public InscripcionResponse aResponse(Inscripcion inscripcion) {
        ResumenReferencia oferta = inscripcion.esDeCurso()
                ? new ResumenReferencia(inscripcion.getCurso().getId(), inscripcion.getCurso().getNombre())
                : new ResumenReferencia(inscripcion.getDiplomado().getId(), inscripcion.getDiplomado().getNombre());

        return new InscripcionResponse(inscripcion.getId(),
                CodigoUtil.formatear(PREFIJO, inscripcion.getId(), 4),
                clienteMapper.aResumen(inscripcion.getCliente()),
                inscripcion.esDeCurso() ? "CURSO" : "DIPLOMADO", oferta,
                inscripcion.getParticipanteNombre(), inscripcion.getParticipanteEmail(),
                inscripcion.getParticipanteTelefono(), inscripcion.getFechaInscripcion(),
                inscripcion.getMontoTotal(), inscripcion.getEstado(), inscripcion.getObservaciones(),
                inscripcion.getCreatedAt(), inscripcion.getUpdatedAt());
    }
}
