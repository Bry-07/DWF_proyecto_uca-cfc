package com.uca.cfc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uca.cfc.dto.cotizacion.CotizacionItemRequest;
import com.uca.cfc.dto.cotizacion.CotizacionRequest;
import com.uca.cfc.entity.Cliente;
import com.uca.cfc.entity.Cotizacion;
import com.uca.cfc.entity.enums.EstadoCotizacion;
import com.uca.cfc.entity.enums.TipoCotizacion;
import com.uca.cfc.entity.enums.TipoItemCotizacion;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.mapper.ClienteMapper;
import com.uca.cfc.mapper.CotizacionMapper;
import com.uca.cfc.repository.CotizacionRepository;

@DisplayName("CotizacionService")
class CotizacionServiceTest {

    private static final Long CLIENTE_ID = 2L;

    private CotizacionRepository cotizacionRepository;
    private CotizacionService service;

    @BeforeEach
    void preparar() {
        cotizacionRepository = mock(CotizacionRepository.class);
        ClienteService clienteService = mock(ClienteService.class);

        Clock reloj = Clock.fixed(Instant.parse("2026-09-11T15:00:00Z"), ZoneId.of("America/El_Salvador"));
        service = new CotizacionService(cotizacionRepository, clienteService,
                new CotizacionMapper(new ClienteMapper()), reloj);

        Cliente cliente = new Cliente();
        cliente.setId(CLIENTE_ID);
        cliente.setNombre("Corporativo Alfa");
        when(clienteService.buscar(CLIENTE_ID)).thenReturn(cliente);
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(invocacion -> {
            Cotizacion guardada = invocacion.getArgument(0);
            guardada.setId(20L);
            return guardada;
        });
    }

    private CotizacionRequest solicitud(List<CotizacionItemRequest> items) {
        return new CotizacionRequest(CLIENTE_ID, "Evento institucional", null, null, null, null, items);
    }

    private CotizacionItemRequest item(TipoItemCotizacion tipo, int cantidad, String precio) {
        return new CotizacionItemRequest(tipo, "Concepto", null, null, cantidad, new BigDecimal(precio));
    }

    @Test
    @DisplayName("con un solo tipo de ítem, la cotización toma ese tipo")
    void tipoUnico() {
        var respuesta = service.crear(solicitud(List.of(item(TipoItemCotizacion.CURSO, 25, "185.00"))));

        assertEquals(TipoCotizacion.CURSO, respuesta.tipo());
        assertEquals(EstadoCotizacion.EN_PROCESO, respuesta.estado());
    }

    @Test
    @DisplayName("con ítems de varios tipos, la cotización es COMBINADA")
    void tipoCombinado() {
        var respuesta = service.crear(solicitud(List.of(
                item(TipoItemCotizacion.CURSO, 25, "185.00"),
                item(TipoItemCotizacion.ESPACIO, 4, "45.00"),
                item(TipoItemCotizacion.CATERING, 25, "8.50"))));

        assertEquals(TipoCotizacion.COMBINADA, respuesta.tipo());
    }

    @Test
    @DisplayName("el total es la suma de los subtotales de los ítems")
    void totalCalculado() {
        var respuesta = service.crear(solicitud(List.of(
                item(TipoItemCotizacion.CURSO, 25, "185.00"),
                item(TipoItemCotizacion.ESPACIO, 4, "45.00"),
                item(TipoItemCotizacion.CATERING, 25, "8.50"))));

        // 4625.00 + 180.00 + 212.50
        assertEquals(new BigDecimal("5017.50"), respuesta.total());
        assertEquals(new BigDecimal("4625.00"), respuesta.items().get(0).subtotal());
    }

    @Test
    @DisplayName("si no se indica vencimiento, la cotización vence 30 días después")
    void vencimientoPorDefecto() {
        var respuesta = service.crear(solicitud(List.of(item(TipoItemCotizacion.CURSO, 1, "100.00"))));

        assertEquals(LocalDate.of(2026, 9, 11), respuesta.fechaSolicitud());
        assertEquals(LocalDate.of(2026, 10, 11), respuesta.fechaVencimiento());
    }

    @Test
    @DisplayName("rechaza un vencimiento anterior a la solicitud")
    void vencimientoInvalido() {
        CotizacionRequest request = new CotizacionRequest(CLIENTE_ID, "Evento",
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1), null, null,
                List.of(item(TipoItemCotizacion.CURSO, 1, "100.00")));

        assertThrows(BusinessException.class, () -> service.crear(request));
    }

    @Test
    @DisplayName("no permite aprobar una cotización vencida")
    void noApruebaVencida() {
        Cotizacion vencida = new Cotizacion();
        vencida.setId(20L);
        vencida.setCliente(new Cliente());
        vencida.setEstado(EstadoCotizacion.EN_PROCESO);
        vencida.setTipo(TipoCotizacion.CURSO);
        vencida.setAsunto("Vencida");
        vencida.setFechaSolicitud(LocalDate.of(2026, 7, 1));
        vencida.setFechaVencimiento(LocalDate.of(2026, 8, 1));
        when(cotizacionRepository.findWithItemsById(20L)).thenReturn(Optional.of(vencida));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.aprobar(20L, null));

        assertTrue(ex.getMessage().contains("venció"));
    }

    @Test
    @DisplayName("no permite resolver dos veces la misma cotización")
    void noResuelveDosVeces() {
        Cotizacion aprobada = new Cotizacion();
        aprobada.setId(20L);
        aprobada.setCliente(new Cliente());
        aprobada.setEstado(EstadoCotizacion.APROBADA);
        aprobada.setTipo(TipoCotizacion.CURSO);
        aprobada.setAsunto("Ya resuelta");
        aprobada.setFechaSolicitud(LocalDate.of(2026, 9, 1));
        aprobada.setFechaVencimiento(LocalDate.of(2026, 12, 1));
        when(cotizacionRepository.findWithItemsById(20L)).thenReturn(Optional.of(aprobada));

        assertThrows(BusinessException.class, () -> service.rechazar(20L, null));
    }
}
