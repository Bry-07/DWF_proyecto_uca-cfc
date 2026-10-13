package com.uca.cfc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uca.cfc.dto.alquiler.AlquilerRequest;
import com.uca.cfc.entity.Alquiler;
import com.uca.cfc.entity.Cliente;
import com.uca.cfc.entity.Espacio;
import com.uca.cfc.entity.enums.EstadoEspacio;
import com.uca.cfc.entity.enums.EstadoReserva;
import com.uca.cfc.entity.enums.TipoEspacio;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.EspacioOcupadoException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.AlquilerMapper;
import com.uca.cfc.mapper.CatalogoMapper;
import com.uca.cfc.mapper.ClienteMapper;
import com.uca.cfc.repository.AlquilerRepository;
import com.uca.cfc.repository.EspacioRepository;

/**
 * Pruebas de la regla crítica del módulo: un espacio nunca puede reservarse dos
 * veces en horarios que se superponen.
 */
@DisplayName("AlquilerService")
class AlquilerServiceTest {

    private static final Long ESPACIO_ID = 1L;
    private static final Long CLIENTE_ID = 2L;

    private AlquilerRepository alquilerRepository;
    private EspacioRepository espacioRepository;
    private ClienteService clienteService;
    private AgendaService agendaService;
    private AlquilerService service;

    private Espacio espacio;

    @BeforeEach
    void preparar() {
        alquilerRepository = mock(AlquilerRepository.class);
        espacioRepository = mock(EspacioRepository.class);
        clienteService = mock(ClienteService.class);
        agendaService = mock(AgendaService.class);

        service = new AlquilerService(alquilerRepository, espacioRepository, clienteService, agendaService,
                new AlquilerMapper(new ClienteMapper(), new CatalogoMapper()));

        espacio = new Espacio();
        espacio.setId(ESPACIO_ID);
        espacio.setNombre("Auditorio Principal");
        espacio.setTipo(TipoEspacio.AUDITORIO);
        espacio.setCapacidad(180);
        espacio.setPrecioPorHora(new BigDecimal("45.00"));
        espacio.setEstado(EstadoEspacio.DISPONIBLE);

        Cliente cliente = new Cliente();
        cliente.setId(CLIENTE_ID);
        cliente.setNombre("Corporativo Alfa");

        when(espacioRepository.findForUpdateById(ESPACIO_ID)).thenReturn(Optional.of(espacio));
        when(clienteService.buscar(CLIENTE_ID)).thenReturn(cliente);
        when(agendaService.buscarConflictoDeEspacio(anyLong(), any(), any(), any())).thenReturn(Optional.empty());
        when(alquilerRepository.save(any(Alquiler.class))).thenAnswer(invocacion -> {
            Alquiler guardado = invocacion.getArgument(0);
            guardado.setId(50L);
            return guardado;
        });
    }

    private AlquilerRequest solicitud(String inicio, String fin, int asistentes) {
        return new AlquilerRequest(CLIENTE_ID, ESPACIO_ID, LocalDateTime.parse(inicio), LocalDateTime.parse(fin),
                "Asamblea de accionistas", asistentes, null);
    }

    private Alquiler existente(String inicio, String fin) {
        Alquiler alquiler = new Alquiler();
        alquiler.setId(11L);
        alquiler.setEspacio(espacio);
        alquiler.setProposito("Congreso anual");
        alquiler.setFechaHoraInicio(LocalDateTime.parse(inicio));
        alquiler.setFechaHoraFin(LocalDateTime.parse(fin));
        return alquiler;
    }

    @Test
    @DisplayName("registra la reserva cuando el espacio está libre")
    void reservaValida() {
        when(alquilerRepository.buscarSolapamientos(anyLong(), any(), any(), any())).thenReturn(List.of());

        var respuesta = service.crear(solicitud("2026-09-15T17:00:00", "2026-09-15T18:30:00", 120));

        assertEquals(EstadoReserva.PENDIENTE, respuesta.estado());
        assertEquals("Auditorio Principal", respuesta.espacio().nombre());
        // 1.5 horas × $45.00 prorrateado por minuto
        assertEquals(new BigDecimal("67.50"), respuesta.montoTotal());
        assertEquals(1.5, respuesta.horas());
        verify(alquilerRepository).save(any(Alquiler.class));
    }

    @Test
    @DisplayName("cobra el prorrateo por minutos, no solo por horas completas")
    void montoProrrateado() {
        when(alquilerRepository.buscarSolapamientos(anyLong(), any(), any(), any())).thenReturn(List.of());

        var respuesta = service.crear(solicitud("2026-09-15T08:00:00", "2026-09-15T08:45:00", 10));

        assertEquals(new BigDecimal("33.75"), respuesta.montoTotal());
    }

    @Test
    @DisplayName("rechaza la reserva cuando el espacio ya está ocupado en ese horario")
    void espacioOcupado() {
        when(alquilerRepository.buscarSolapamientos(anyLong(), any(), any(), any()))
                .thenReturn(List.of(existente("2026-09-15T16:00:00", "2026-09-15T18:00:00")));

        EspacioOcupadoException ex = assertThrows(EspacioOcupadoException.class,
                () -> service.crear(solicitud("2026-09-15T17:00:00", "2026-09-15T18:30:00", 120)));

        assertTrue(ex.getMessage().contains("Auditorio Principal"));
        assertTrue(ex.getMessage().contains("Congreso anual"));
        verify(alquilerRepository, never()).save(any(Alquiler.class));
        // Tampoco debe tocarse la agenda si la reserva no se guardó
        verify(agendaService, never()).sincronizar(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("rechaza la reserva que choca con una actividad de la agenda")
    void conflictoConAgenda() {
        when(alquilerRepository.buscarSolapamientos(anyLong(), any(), any(), any())).thenReturn(List.of());
        com.uca.cfc.entity.ActividadAgenda actividad = new com.uca.cfc.entity.ActividadAgenda();
        actividad.setTitulo("Clase de Gestión Ágil");
        actividad.setFechaHoraInicio(LocalDateTime.parse("2026-09-15T17:30:00"));
        actividad.setFechaHoraFin(LocalDateTime.parse("2026-09-15T19:30:00"));
        when(agendaService.buscarConflictoDeEspacio(anyLong(), any(), any(), any()))
                .thenReturn(Optional.of(actividad));

        EspacioOcupadoException ex = assertThrows(EspacioOcupadoException.class,
                () -> service.crear(solicitud("2026-09-15T17:00:00", "2026-09-15T18:30:00", 120)));

        assertTrue(ex.getMessage().contains("Clase de Gestión Ágil"));
    }

    @Test
    @DisplayName("falla cuando el espacio no existe")
    void espacioInexistente() {
        when(espacioRepository.findForUpdateById(ESPACIO_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.crear(solicitud("2026-09-15T17:00:00", "2026-09-15T18:30:00", 10)));

        assertTrue(ex.getMessage().contains("Espacio"));
    }

    @Test
    @DisplayName("rechaza más asistentes que la capacidad del espacio")
    void capacidadExcedida() {
        when(alquilerRepository.buscarSolapamientos(anyLong(), any(), any(), any())).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.crear(solicitud("2026-09-15T17:00:00", "2026-09-15T18:30:00", 200)));

        assertTrue(ex.getMessage().contains("180"));
        verify(alquilerRepository, never()).save(any(Alquiler.class));
    }

    @Test
    @DisplayName("rechaza reservar un espacio que no está disponible")
    void espacioEnMantenimiento() {
        espacio.setEstado(EstadoEspacio.MANTENIMIENTO);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.crear(solicitud("2026-09-15T17:00:00", "2026-09-15T18:30:00", 10)));

        assertTrue(ex.getMessage().contains("MANTENIMIENTO"));
    }

    @Test
    @DisplayName("rechaza un horario cuyo fin no es posterior al inicio")
    void horarioInvertido() {
        assertThrows(BusinessException.class,
                () -> service.crear(solicitud("2026-09-15T18:30:00", "2026-09-15T17:00:00", 10)));
    }

    @Test
    @DisplayName("la reserva registrada se refleja en la agenda institucional")
    void sincronizaAgenda() {
        when(alquilerRepository.buscarSolapamientos(anyLong(), any(), any(), any())).thenReturn(List.of());

        service.crear(solicitud("2026-09-15T17:00:00", "2026-09-15T18:30:00", 120));

        verify(agendaService).sincronizar(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
