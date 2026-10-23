package com.uca.cfc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uca.cfc.dto.inscripcion.InscripcionRequest;
import com.uca.cfc.entity.Cliente;
import com.uca.cfc.entity.Curso;
import com.uca.cfc.entity.Inscripcion;
import com.uca.cfc.entity.enums.EstadoInscripcion;
import com.uca.cfc.entity.enums.EstadoOferta;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.CupoAgotadoException;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.ClienteMapper;
import com.uca.cfc.mapper.InscripcionMapper;
import com.uca.cfc.repository.CursoRepository;
import com.uca.cfc.repository.DiplomadoRepository;
import com.uca.cfc.repository.InscripcionRepository;

/**
 * Pruebas de la regla crítica del módulo: una inscripción nunca puede superar
 * el cupo máximo de la oferta.
 */
@DisplayName("InscripcionService")
class InscripcionServiceTest {

    private static final Long CURSO_ID = 7L;
    private static final Long CLIENTE_ID = 3L;

    private InscripcionRepository inscripcionRepository;
    private CursoRepository cursoRepository;
    private DiplomadoRepository diplomadoRepository;
    private ClienteService clienteService;
    private InscripcionService service;

    private Curso curso;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        inscripcionRepository = mock(InscripcionRepository.class);
        cursoRepository = mock(CursoRepository.class);
        diplomadoRepository = mock(DiplomadoRepository.class);
        clienteService = mock(ClienteService.class);

        Clock reloj = Clock.fixed(Instant.parse("2026-09-11T15:00:00Z"), ZoneId.of("America/El_Salvador"));
        service = new InscripcionService(inscripcionRepository, cursoRepository, diplomadoRepository,
                clienteService, new InscripcionMapper(new ClienteMapper()), reloj);

        cliente = new Cliente();
        cliente.setId(CLIENTE_ID);
        cliente.setNombre("Laboratorios Vitalab");

        curso = new Curso();
        curso.setId(CURSO_ID);
        curso.setNombre("Gestión Ágil de Proyectos");
        curso.setCupoMaximo(20);
        curso.setPrecio(new BigDecimal("185.00"));
        curso.setEstado(EstadoOferta.ACTIVO);

        when(clienteService.buscar(CLIENTE_ID)).thenReturn(cliente);
        when(cursoRepository.findForUpdateById(CURSO_ID)).thenReturn(Optional.of(curso));
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(invocacion -> {
            Inscripcion guardada = invocacion.getArgument(0);
            guardada.setId(99L);
            return guardada;
        });
    }

    private InscripcionRequest solicitud() {
        return new InscripcionRequest(CLIENTE_ID, CURSO_ID, null, "Marta Elena Gómez",
                "marta.gomez@vitalab.com.sv", "7890-1122", null, null);
    }

    @Test
    @DisplayName("registra la inscripción cuando hay cupos disponibles")
    void inscripcionValida() {
        when(inscripcionRepository.contarOcupadasPorCurso(CURSO_ID)).thenReturn(13L);
        when(inscripcionRepository.existeParticipanteEnCurso(anyLong(), anyString(), any())).thenReturn(false);

        var respuesta = service.crear(solicitud());

        assertEquals(EstadoInscripcion.PENDIENTE, respuesta.estado());
        assertEquals("CURSO", respuesta.tipoOferta());
        assertEquals(new BigDecimal("185.00"), respuesta.montoTotal());
        assertEquals(LocalDate.of(2026, 9, 11), respuesta.fechaInscripcion());
        // El correo se normaliza a minúsculas
        assertEquals("marta.gomez@vitalab.com.sv", respuesta.participanteEmail());
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("rechaza la inscripción cuando el curso alcanzó su cupo máximo")
    void cupoAgotado() {
        when(inscripcionRepository.contarOcupadasPorCurso(CURSO_ID)).thenReturn(20L);
        when(inscripcionRepository.existeParticipanteEnCurso(anyLong(), anyString(), any())).thenReturn(false);

        CupoAgotadoException ex = assertThrows(CupoAgotadoException.class, () -> service.crear(solicitud()));

        assertTrue(ex.getMessage().contains("Gestión Ágil de Proyectos"));
        assertTrue(ex.getMessage().contains("20"));
        // Nada debe persistirse cuando la regla se incumple
        verify(inscripcionRepository, never()).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("rechaza la inscripción cuando el conteo supera el cupo")
    void cupoExcedido() {
        when(inscripcionRepository.contarOcupadasPorCurso(CURSO_ID)).thenReturn(25L);
        when(inscripcionRepository.existeParticipanteEnCurso(anyLong(), anyString(), any())).thenReturn(false);

        assertThrows(CupoAgotadoException.class, () -> service.crear(solicitud()));
    }

    @Test
    @DisplayName("acepta la inscripción que ocupa el último cupo")
    void ultimoCupo() {
        when(inscripcionRepository.contarOcupadasPorCurso(CURSO_ID)).thenReturn(19L);
        when(inscripcionRepository.existeParticipanteEnCurso(anyLong(), anyString(), any())).thenReturn(false);

        assertEquals(EstadoInscripcion.PENDIENTE, service.crear(solicitud()).estado());
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("falla cuando el curso no existe")
    void cursoInexistente() {
        when(cursoRepository.findForUpdateById(CURSO_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.crear(solicitud()));

        assertTrue(ex.getMessage().contains("Curso"));
        verify(inscripcionRepository, never()).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("rechaza al participante que ya tiene una inscripción vigente en la oferta")
    void participanteRepetido() {
        when(inscripcionRepository.existeParticipanteEnCurso(eq(CURSO_ID), anyString(), any())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.crear(solicitud()));
        verify(inscripcionRepository, never()).save(any(Inscripcion.class));
    }

    @Test
    @DisplayName("rechaza la inscripción a una oferta inactiva")
    void ofertaInactiva() {
        curso.setEstado(EstadoOferta.INACTIVO);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.crear(solicitud()));

        assertTrue(ex.getMessage().contains("inactiva"));
    }

    @Test
    @DisplayName("exige indicar curso o diplomado")
    void sinOferta() {
        InscripcionRequest sinOferta = new InscripcionRequest(CLIENTE_ID, null, null, "Nombre",
                "correo@x.com", null, null, null);

        assertThrows(BusinessException.class, () -> service.crear(sinOferta));
    }

    @Test
    @DisplayName("rechaza indicar curso y diplomado a la vez")
    void cursoYDiplomado() {
        InscripcionRequest ambos = new InscripcionRequest(CLIENTE_ID, CURSO_ID, 4L, "Nombre",
                "correo@x.com", null, null, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.crear(ambos));

        assertTrue(ex.getMessage().contains("no a ambos"));
    }
}
