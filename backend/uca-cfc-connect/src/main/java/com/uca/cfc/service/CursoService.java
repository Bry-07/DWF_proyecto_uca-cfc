package com.uca.cfc.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.curso.CursoResponse;
import com.uca.cfc.dto.curso.OfertaAcademicaRequest;
import com.uca.cfc.entity.Curso;
import com.uca.cfc.entity.enums.EstadoOferta;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceInUseException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.OfertaAcademicaMapper;
import com.uca.cfc.repository.CursoRepository;
import com.uca.cfc.repository.InscripcionRepository;

/**
 * Reglas de negocio de los cursos.
 * Ademas del CRUD revisa dos cosas importantes: que no se repita el codigo
 * del curso, y que al editar no se baje el cupo maximo por debajo de las
 * inscripciones que ya existen.
 */
@Service
@Transactional(readOnly = true)
public class CursoService {

    private static final String RECURSO = "Curso";

    private final CursoRepository cursoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final OfertaAcademicaValidator validator;
    private final OfertaAcademicaMapper mapper;

    public CursoService(CursoRepository cursoRepository, InscripcionRepository inscripcionRepository,
            OfertaAcademicaValidator validator, OfertaAcademicaMapper mapper) {
        this.cursoRepository = cursoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    public PageResponse<CursoResponse> listar(Long categoriaId, Long modalidadId, Long docenteId,
            EstadoOferta estado, String texto, Pageable pageable) {
        return PageResponse.de(
                cursoRepository.buscar(categoriaId, modalidadId, docenteId, estado, normalizar(texto), pageable),
                this::aResponse);
    }

    public CursoResponse obtener(Long id) {
        return aResponse(buscar(id));
    }

    @Transactional
    public CursoResponse crear(OfertaAcademicaRequest request) {
        if (cursoRepository.existsByCodigoIgnoreCase(request.codigo().trim())) {
            throw new DuplicateResourceException("Ya existe un curso con el código '" + request.codigo() + "'");
        }
        Curso curso = new Curso();
        validator.aplicar(request, curso);
        return aResponse(cursoRepository.save(curso));
    }

    @Transactional
    public CursoResponse actualizar(Long id, OfertaAcademicaRequest request) {
        Curso curso = buscar(id);
        if (cursoRepository.existsByCodigoIgnoreCaseAndIdNot(request.codigo().trim(), id)) {
            throw new DuplicateResourceException("Ya existe un curso con el código '" + request.codigo() + "'");
        }
        long ocupados = inscripcionRepository.contarOcupadasPorCurso(id);
        if (request.cupoMaximo() < ocupados) {
            throw new BusinessException("El cupo máximo no puede ser menor que las " + ocupados
                    + " inscripciones vigentes del curso");
        }
        validator.aplicar(request, curso);
        return aResponse(cursoRepository.save(curso));
    }

    @Transactional
    public CursoResponse cambiarEstado(Long id, EstadoOferta estado) {
        Curso curso = buscar(id);
        curso.setEstado(estado);
        return aResponse(cursoRepository.save(curso));
    }

    @Transactional
    public void eliminar(Long id) {
        Curso curso = buscar(id);
        if (inscripcionRepository.existsByCursoId(id)) {
            throw new ResourceInUseException("No se puede eliminar el curso '" + curso.getNombre()
                    + "' porque tiene inscripciones registradas. Cámbielo a estado INACTIVO");
        }
        cursoRepository.delete(curso);
    }

    public Curso buscar(Long id) {
        return cursoRepository.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private CursoResponse aResponse(Curso curso) {
        return mapper.aResponse(curso, inscripcionRepository.contarOcupadasPorCurso(curso.getId()));
    }

    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
