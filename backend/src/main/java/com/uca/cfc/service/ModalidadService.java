package com.uca.cfc.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.modalidad.ModalidadRequest;
import com.uca.cfc.dto.modalidad.ModalidadResponse;
import com.uca.cfc.entity.Modalidad;
import com.uca.cfc.entity.enums.TipoModalidad;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceInUseException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.CatalogoMapper;
import com.uca.cfc.repository.CursoRepository;
import com.uca.cfc.repository.DiplomadoRepository;
import com.uca.cfc.repository.ModalidadRepository;

/**
 * Reglas de negocio de las modalidades.
 * Igual que con las categorias, no se puede borrar una modalidad que este
 * siendo usada por algun curso o diplomado.
 */
@Service
@Transactional(readOnly = true)
public class ModalidadService {

    private static final String RECURSO = "Modalidad";

    private final ModalidadRepository modalidadRepository;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final CatalogoMapper mapper;

    public ModalidadService(ModalidadRepository modalidadRepository, CursoRepository cursoRepository,
            DiplomadoRepository diplomadoRepository, CatalogoMapper mapper) {
        this.modalidadRepository = modalidadRepository;
        this.cursoRepository = cursoRepository;
        this.diplomadoRepository = diplomadoRepository;
        this.mapper = mapper;
    }

    /** Las modalidades son pocas y fijas: se devuelven completas, sin paginar. */
    public List<ModalidadResponse> listar(TipoModalidad tipo) {
        List<Modalidad> modalidades = (tipo == null) ? modalidadRepository.findAll()
                : modalidadRepository.findByTipo(tipo);
        return modalidades.stream().map(mapper::aResponse).toList();
    }

    public ModalidadResponse obtener(Long id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public ModalidadResponse crear(ModalidadRequest request) {
        if (modalidadRepository.existsByNombreIgnoreCase(request.nombre().trim())) {
            throw new DuplicateResourceException("Ya existe una modalidad con el nombre '" + request.nombre() + "'");
        }
        Modalidad modalidad = new Modalidad();
        aplicar(request, modalidad);
        return mapper.aResponse(modalidadRepository.save(modalidad));
    }

    @Transactional
    public ModalidadResponse actualizar(Long id, ModalidadRequest request) {
        Modalidad modalidad = buscar(id);
        modalidadRepository.findAll().stream()
                .filter(otra -> !otra.getId().equals(id) && otra.getNombre().equalsIgnoreCase(request.nombre().trim()))
                .findFirst()
                .ifPresent(otra -> {
                    throw new DuplicateResourceException(
                            "Ya existe una modalidad con el nombre '" + request.nombre() + "'");
                });
        aplicar(request, modalidad);
        return mapper.aResponse(modalidadRepository.save(modalidad));
    }

    @Transactional
    public void eliminar(Long id) {
        Modalidad modalidad = buscar(id);
        if (cursoRepository.existsByModalidadId(id) || diplomadoRepository.existsByModalidadId(id)) {
            throw new ResourceInUseException("No se puede eliminar la modalidad '" + modalidad.getNombre()
                    + "' porque tiene cursos o diplomados asociados");
        }
        modalidadRepository.delete(modalidad);
    }

    public Modalidad buscar(Long id) {
        return modalidadRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private void aplicar(ModalidadRequest request, Modalidad modalidad) {
        modalidad.setNombre(request.nombre().trim());
        modalidad.setTipo(request.tipo());
        modalidad.setDescripcion(request.descripcion());
    }
}
