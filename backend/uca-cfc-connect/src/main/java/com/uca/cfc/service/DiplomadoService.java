package com.uca.cfc.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.curso.OfertaAcademicaRequest;
import com.uca.cfc.dto.diplomado.DiplomadoResponse;
import com.uca.cfc.entity.Diplomado;
import com.uca.cfc.entity.enums.EstadoOferta;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceInUseException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.OfertaAcademicaMapper;
import com.uca.cfc.repository.DiplomadoRepository;
import com.uca.cfc.repository.InscripcionRepository;

/**
 * Reglas de negocio de los diplomados. Funciona igual que CursoService.
 */
@Service
@Transactional(readOnly = true)
public class DiplomadoService {

    private static final String RECURSO = "Diplomado";

    private final DiplomadoRepository diplomadoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final OfertaAcademicaValidator validator;
    private final OfertaAcademicaMapper mapper;

    public DiplomadoService(DiplomadoRepository diplomadoRepository, InscripcionRepository inscripcionRepository,
            OfertaAcademicaValidator validator, OfertaAcademicaMapper mapper) {
        this.diplomadoRepository = diplomadoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    public PageResponse<DiplomadoResponse> listar(Long categoriaId, Long modalidadId, Long docenteId,
            EstadoOferta estado, String texto, Pageable pageable) {
        return PageResponse.de(
                diplomadoRepository.buscar(categoriaId, modalidadId, docenteId, estado, normalizar(texto), pageable),
                this::aResponse);
    }

    public DiplomadoResponse obtener(Long id) {
        return aResponse(buscar(id));
    }

    @Transactional
    public DiplomadoResponse crear(OfertaAcademicaRequest request) {
        if (diplomadoRepository.existsByCodigoIgnoreCase(request.codigo().trim())) {
            throw new DuplicateResourceException("Ya existe un diplomado con el código '" + request.codigo() + "'");
        }
        Diplomado diplomado = new Diplomado();
        validator.aplicar(request, diplomado);
        return aResponse(diplomadoRepository.save(diplomado));
    }

    @Transactional
    public DiplomadoResponse actualizar(Long id, OfertaAcademicaRequest request) {
        Diplomado diplomado = buscar(id);
        if (diplomadoRepository.existsByCodigoIgnoreCaseAndIdNot(request.codigo().trim(), id)) {
            throw new DuplicateResourceException("Ya existe un diplomado con el código '" + request.codigo() + "'");
        }
        long ocupados = inscripcionRepository.contarOcupadasPorDiplomado(id);
        if (request.cupoMaximo() < ocupados) {
            throw new BusinessException("El cupo máximo no puede ser menor que las " + ocupados
                    + " inscripciones vigentes del diplomado");
        }
        validator.aplicar(request, diplomado);
        return aResponse(diplomadoRepository.save(diplomado));
    }

    @Transactional
    public DiplomadoResponse cambiarEstado(Long id, EstadoOferta estado) {
        Diplomado diplomado = buscar(id);
        diplomado.setEstado(estado);
        return aResponse(diplomadoRepository.save(diplomado));
    }

    @Transactional
    public void eliminar(Long id) {
        Diplomado diplomado = buscar(id);
        if (inscripcionRepository.existsByDiplomadoId(id)) {
            throw new ResourceInUseException("No se puede eliminar el diplomado '" + diplomado.getNombre()
                    + "' porque tiene inscripciones registradas. Cámbielo a estado INACTIVO");
        }
        diplomadoRepository.delete(diplomado);
    }

    public Diplomado buscar(Long id) {
        return diplomadoRepository.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private DiplomadoResponse aResponse(Diplomado diplomado) {
        return mapper.aResponse(diplomado, inscripcionRepository.contarOcupadasPorDiplomado(diplomado.getId()));
    }

    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
