package com.uca.cfc.service;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.inscripcion.InscripcionRequest;
import com.uca.cfc.dto.inscripcion.InscripcionResponse;
import com.uca.cfc.dto.inscripcion.InscripcionUpdateRequest;
import com.uca.cfc.entity.Cliente;
import com.uca.cfc.entity.Curso;
import com.uca.cfc.entity.Diplomado;
import com.uca.cfc.entity.Inscripcion;
import com.uca.cfc.entity.OfertaAcademica;
import com.uca.cfc.entity.enums.EstadoInscripcion;
import com.uca.cfc.entity.enums.EstadoOferta;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.CupoAgotadoException;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.InscripcionMapper;
import com.uca.cfc.repository.CursoRepository;
import com.uca.cfc.repository.DiplomadoRepository;
import com.uca.cfc.repository.InscripcionRepository;

/**
 * Regla crítica del módulo: nunca permitir una inscripción cuando la oferta
 * alcanzó su capacidad máxima. La verificación se hace dentro de la transacción
 * y con la fila del curso bloqueada, para que dos solicitudes simultáneas no
 * puedan leer el mismo conteo y superar el cupo entre ambas.
 */
@Service
@Transactional(readOnly = true)
public class InscripcionService {

    private static final String RECURSO = "Inscripción";

    private static final Logger log = LoggerFactory.getLogger(InscripcionService.class);

    private final InscripcionRepository inscripcionRepository;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final ClienteService clienteService;
    private final InscripcionMapper mapper;
    private final Clock clock;

    public InscripcionService(InscripcionRepository inscripcionRepository, CursoRepository cursoRepository,
            DiplomadoRepository diplomadoRepository, ClienteService clienteService, InscripcionMapper mapper,
            Clock clock) {
        this.inscripcionRepository = inscripcionRepository;
        this.cursoRepository = cursoRepository;
        this.diplomadoRepository = diplomadoRepository;
        this.clienteService = clienteService;
        this.mapper = mapper;
        this.clock = clock;
    }

    public PageResponse<InscripcionResponse> listar(Long clienteId, Long cursoId, Long diplomadoId,
            EstadoInscripcion estado, LocalDate desde, LocalDate hasta, Pageable pageable) {
        return PageResponse.de(
                inscripcionRepository.buscar(clienteId, cursoId, diplomadoId, estado, desde, hasta, pageable),
                mapper::aResponse);
    }

    public PageResponse<InscripcionResponse> listarPorCliente(Long clienteId, EstadoInscripcion estado,
            Pageable pageable) {
        clienteService.buscar(clienteId);
        return PageResponse.de(
                inscripcionRepository.buscar(clienteId, null, null, estado, null, null, pageable),
                mapper::aResponse);
    }

    public InscripcionResponse obtener(Long id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public InscripcionResponse crear(InscripcionRequest request) {
        validarOfertaUnica(request.cursoId(), request.diplomadoId());

        Cliente cliente = clienteService.buscar(request.clienteId());
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setCliente(cliente);

        OfertaAcademica oferta;
        if (request.cursoId() != null) {
            Curso curso = cursoRepository.findForUpdateById(request.cursoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Curso", request.cursoId()));
            validarDisponible(curso);
            validarParticipanteNoRepetido(request.participanteEmail(), curso.getId(), null, true);
            validarCupo(curso, inscripcionRepository.contarOcupadasPorCurso(curso.getId()));
            inscripcion.setCurso(curso);
            oferta = curso;
        } else {
            Diplomado diplomado = diplomadoRepository.findForUpdateById(request.diplomadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Diplomado", request.diplomadoId()));
            validarDisponible(diplomado);
            validarParticipanteNoRepetido(request.participanteEmail(), diplomado.getId(), null, false);
            validarCupo(diplomado, inscripcionRepository.contarOcupadasPorDiplomado(diplomado.getId()));
            inscripcion.setDiplomado(diplomado);
            oferta = diplomado;
        }

        inscripcion.setParticipanteNombre(request.participanteNombre().trim());
        inscripcion.setParticipanteEmail(request.participanteEmail().trim().toLowerCase());
        inscripcion.setParticipanteTelefono(normalizar(request.participanteTelefono()));
        inscripcion.setFechaInscripcion(
                request.fechaInscripcion() == null ? LocalDate.now(clock) : request.fechaInscripcion());
        // El precio se congela al inscribir: cambios posteriores del catálogo no lo afectan
        inscripcion.setMontoTotal(oferta.getPrecio());
        inscripcion.setEstado(EstadoInscripcion.PENDIENTE);
        inscripcion.setObservaciones(request.observaciones());

        Inscripcion guardada = inscripcionRepository.save(inscripcion);
        log.info("Inscripción {} creada para la oferta '{}'", guardada.getId(), oferta.getNombre());
        return mapper.aResponse(guardada);
    }

    @Transactional
    public InscripcionResponse actualizar(Long id, InscripcionUpdateRequest request) {
        Inscripcion inscripcion = buscar(id);
        if (inscripcion.getEstado().esFinal()) {
            throw new BusinessException("No se puede modificar una inscripción en estado "
                    + inscripcion.getEstado());
        }

        boolean esCurso = inscripcion.esDeCurso();
        Long ofertaId = esCurso ? inscripcion.getCurso().getId() : inscripcion.getDiplomado().getId();
        validarParticipanteNoRepetido(request.participanteEmail(), ofertaId, id, esCurso);

        inscripcion.setParticipanteNombre(request.participanteNombre().trim());
        inscripcion.setParticipanteEmail(request.participanteEmail().trim().toLowerCase());
        inscripcion.setParticipanteTelefono(normalizar(request.participanteTelefono()));
        inscripcion.setObservaciones(request.observaciones());
        return mapper.aResponse(inscripcionRepository.save(inscripcion));
    }

    @Transactional
    public InscripcionResponse cambiarEstado(Long id, EstadoInscripcion destino, String motivo) {
        Inscripcion inscripcion = buscar(id);
        EstadoInscripcion actual = inscripcion.getEstado();

        if (actual == destino) {
            return mapper.aResponse(inscripcion);
        }
        if (!actual.puedeCambiarA(destino)) {
            throw new BusinessException("No es posible pasar la inscripción de " + actual + " a " + destino);
        }

        // Reactivar una inscripción cancelada volvería a ocupar un cupo que pudo ser tomado
        inscripcion.setEstado(destino);
        if (motivo != null && !motivo.isBlank()) {
            inscripcion.setObservaciones(motivo.trim());
        }
        log.info("Inscripción {}: estado {} → {}", id, actual, destino);
        return mapper.aResponse(inscripcionRepository.save(inscripcion));
    }

    @Transactional
    public void eliminar(Long id) {
        Inscripcion inscripcion = buscar(id);
        inscripcionRepository.delete(inscripcion);
    }

    public Inscripcion buscar(Long id) {
        return inscripcionRepository.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private void validarOfertaUnica(Long cursoId, Long diplomadoId) {
        if (cursoId == null && diplomadoId == null) {
            throw new BusinessException("Debe indicar el curso o el diplomado al que se inscribe el participante");
        }
        if (cursoId != null && diplomadoId != null) {
            throw new BusinessException("La inscripción corresponde a un curso o a un diplomado, no a ambos");
        }
    }

    private void validarDisponible(OfertaAcademica oferta) {
        if (oferta.getEstado() != EstadoOferta.ACTIVO) {
            throw new BusinessException("La oferta '" + oferta.getNombre()
                    + "' está inactiva y no admite inscripciones");
        }
    }

    /** Nunca permitir una inscripción cuando la oferta alcanzó su capacidad máxima. */
    private void validarCupo(OfertaAcademica oferta, long ocupados) {
        if (ocupados >= oferta.getCupoMaximo()) {
            throw new CupoAgotadoException(oferta.getNombre(), oferta.getCupoMaximo());
        }
    }

    private void validarParticipanteNoRepetido(String email, Long ofertaId, Long idExcluido, boolean esCurso) {
        String normalizado = email.trim().toLowerCase();
        boolean repetido = esCurso
                ? inscripcionRepository.existeParticipanteEnCurso(ofertaId, normalizado, idExcluido)
                : inscripcionRepository.existeParticipanteEnDiplomado(ofertaId, normalizado, idExcluido);
        if (repetido) {
            throw new DuplicateResourceException(
                    "El participante '" + normalizado + "' ya tiene una inscripción vigente en esta oferta");
        }
    }

    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
