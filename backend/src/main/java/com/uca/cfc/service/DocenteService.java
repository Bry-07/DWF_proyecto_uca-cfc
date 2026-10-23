package com.uca.cfc.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.docente.DocenteRequest;
import com.uca.cfc.dto.docente.DocenteResponse;
import com.uca.cfc.entity.Docente;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceInUseException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.CatalogoMapper;
import com.uca.cfc.repository.CursoRepository;
import com.uca.cfc.repository.DiplomadoRepository;
import com.uca.cfc.repository.DocenteRepository;

/**
 * Reglas de negocio de los docentes.
 * No permite dos docentes con el mismo correo, ni borrar uno que tenga
 * cursos asignados.
 */
@Service
@Transactional(readOnly = true)
public class DocenteService {

    private static final String RECURSO = "Docente";

    private final DocenteRepository docenteRepository;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final CatalogoMapper mapper;

    public DocenteService(DocenteRepository docenteRepository, CursoRepository cursoRepository,
            DiplomadoRepository diplomadoRepository, CatalogoMapper mapper) {
        this.docenteRepository = docenteRepository;
        this.cursoRepository = cursoRepository;
        this.diplomadoRepository = diplomadoRepository;
        this.mapper = mapper;
    }

    public PageResponse<DocenteResponse> listar(String especialidad, String nombre, Pageable pageable) {
        return PageResponse.de(docenteRepository.buscar(normalizar(especialidad), normalizar(nombre), pageable),
                mapper::aResponse);
    }

    public DocenteResponse obtener(Long id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public DocenteResponse crear(DocenteRequest request) {
        if (docenteRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new DuplicateResourceException("Ya existe un docente con el correo '" + request.email() + "'");
        }
        Docente docente = new Docente();
        aplicar(request, docente);
        return mapper.aResponse(docenteRepository.save(docente));
    }

    @Transactional
    public DocenteResponse actualizar(Long id, DocenteRequest request) {
        Docente docente = buscar(id);
        if (docenteRepository.existsByEmailIgnoreCaseAndIdNot(request.email().trim(), id)) {
            throw new DuplicateResourceException("Ya existe un docente con el correo '" + request.email() + "'");
        }
        aplicar(request, docente);
        return mapper.aResponse(docenteRepository.save(docente));
    }

    @Transactional
    public void eliminar(Long id) {
        Docente docente = buscar(id);
        if (cursoRepository.existsByDocenteId(id) || diplomadoRepository.existsByDocenteId(id)) {
            throw new ResourceInUseException("No se puede eliminar al docente '" + docente.getNombreCompleto()
                    + "' porque tiene cursos o diplomados asignados");
        }
        docenteRepository.delete(docente);
    }

    public Docente buscar(Long id) {
        return docenteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private void aplicar(DocenteRequest request, Docente docente) {
        docente.setNombres(request.nombres().trim());
        docente.setApellidos(request.apellidos().trim());
        docente.setEmail(request.email().trim().toLowerCase());
        docente.setTelefono(normalizar(request.telefono()));
        docente.setEspecialidad(request.especialidad().trim());
    }

    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
