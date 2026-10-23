package com.uca.cfc.mapper;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.curso.CursoResponse;
import com.uca.cfc.dto.diplomado.DiplomadoResponse;
import com.uca.cfc.entity.Curso;
import com.uca.cfc.entity.Diplomado;
import com.uca.cfc.entity.OfertaAcademica;

/**
 * Convierte cursos y diplomados en sus DTOs de respuesta.
 * Recibe aparte la cantidad de cupos ocupados, porque ese dato no esta en la
 * tabla: se calcula contando las inscripciones.
 */
@Component
public class OfertaAcademicaMapper {

    private final CatalogoMapper catalogoMapper;

    public OfertaAcademicaMapper(CatalogoMapper catalogoMapper) {
        this.catalogoMapper = catalogoMapper;
    }

    public CursoResponse aResponse(Curso curso, long cuposOcupados) {
        return new CursoResponse(curso.getId(), curso.getCodigo(), curso.getNombre(), curso.getDescripcion(),
                catalogoMapper.aResumen(curso.getCategoria()), catalogoMapper.aResumen(curso.getModalidad()),
                curso.getModalidad().getTipo(), catalogoMapper.aResumen(curso.getDocente()),
                catalogoMapper.aResumen(curso.getEspacio()), curso.getFechaInicio(), curso.getFechaFin(),
                curso.getHorario(), curso.getDuracionHoras(), curso.getCupoMaximo(), cuposOcupados,
                cuposDisponibles(curso, cuposOcupados), curso.getPrecio(), curso.getEstado(), curso.getCreatedAt(),
                curso.getUpdatedAt());
    }

    public DiplomadoResponse aResponse(Diplomado diplomado, long cuposOcupados) {
        return new DiplomadoResponse(diplomado.getId(), diplomado.getCodigo(), diplomado.getNombre(),
                diplomado.getDescripcion(), catalogoMapper.aResumen(diplomado.getCategoria()),
                catalogoMapper.aResumen(diplomado.getModalidad()), diplomado.getModalidad().getTipo(),
                catalogoMapper.aResumen(diplomado.getDocente()), catalogoMapper.aResumen(diplomado.getEspacio()),
                diplomado.getFechaInicio(), diplomado.getFechaFin(), diplomado.getHorario(),
                diplomado.getDuracionHoras(), diplomado.getCupoMaximo(), cuposOcupados,
                cuposDisponibles(diplomado, cuposOcupados), diplomado.getPrecio(), diplomado.getEstado(),
                diplomado.getCreatedAt(), diplomado.getUpdatedAt());
    }

    private int cuposDisponibles(OfertaAcademica oferta, long cuposOcupados) {
        return Math.max(0, oferta.getCupoMaximo() - (int) cuposOcupados);
    }
}
