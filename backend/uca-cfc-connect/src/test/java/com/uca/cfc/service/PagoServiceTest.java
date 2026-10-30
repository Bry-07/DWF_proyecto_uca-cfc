package com.uca.cfc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.uca.cfc.dto.pago.AbonoPagoRequest;
import com.uca.cfc.dto.pago.PagoRequest;
import com.uca.cfc.entity.Cliente;
import com.uca.cfc.entity.Pago;
import com.uca.cfc.entity.Inscripcion;
import com.uca.cfc.entity.enums.ConceptoPago;
import com.uca.cfc.entity.enums.EstadoPago;
import com.uca.cfc.entity.enums.MetodoPago;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.ClienteMapper;
import com.uca.cfc.mapper.PagoMapper;
import com.uca.cfc.repository.AlquilerRepository;
import com.uca.cfc.repository.ComprobanteRepository;
import com.uca.cfc.repository.CotizacionRepository;
import com.uca.cfc.repository.InscripcionRepository;
import com.uca.cfc.repository.PagoRepository;
import com.uca.cfc.repository.SolicitudCateringRepository;

@DisplayName("PagoService")
class PagoServiceTest {

    private static final Long CLIENTE_ID = 1L;
    private static final Long PAGO_ID = 30L;

    private PagoRepository pagoRepository;
    private ComprobanteRepository comprobanteRepository;
    private InscripcionRepository inscripcionRepository;
    private PagoService service;

    private Cliente cliente;

    @BeforeEach
    void preparar() {
        pagoRepository = mock(PagoRepository.class);
        comprobanteRepository = mock(ComprobanteRepository.class);
        inscripcionRepository = mock(InscripcionRepository.class);
        CotizacionRepository cotizacionRepository = mock(CotizacionRepository.class);
        AlquilerRepository alquilerRepository = mock(AlquilerRepository.class);
        SolicitudCateringRepository cateringRepository = mock(SolicitudCateringRepository.class);
        ClienteService clienteService = mock(ClienteService.class);

        Clock reloj = Clock.fixed(Instant.parse("2026-09-11T15:00:00Z"), ZoneId.of("America/El_Salvador"));
        service = new PagoService(pagoRepository, comprobanteRepository, inscripcionRepository,
                cotizacionRepository, alquilerRepository, cateringRepository, clienteService,
                new PagoMapper(new ClienteMapper()), reloj, mock(NumeroComprobanteService.class));

        cliente = new Cliente();
        cliente.setId(CLIENTE_ID);
        cliente.setNombre("Laboratorios Vitalab");

        when(clienteService.buscar(CLIENTE_ID)).thenReturn(cliente);
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setCliente(cliente);
        when(inscripcionRepository.findById(anyLong())).thenReturn(Optional.of(inscripcion));
        when(comprobanteRepository.existsByPagoId(anyLong())).thenReturn(false);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocacion -> {
            Pago guardado = invocacion.getArgument(0);
            if (guardado.getId() == null) {
                guardado.setId(PAGO_ID);
            }
            return guardado;
        });
    }

    private PagoRequest solicitud(BigDecimal esperado, BigDecimal pagado) {
        return new PagoRequest(CLIENTE_ID, ConceptoPago.INSCRIPCION, 5L, esperado, pagado,
                MetodoPago.TRANSFERENCIA, null, "TRF-001");
    }

    private Pago pagoExistente(String esperado, String pagado, EstadoPago estado) {
        Pago pago = new Pago();
        pago.setId(PAGO_ID);
        pago.setCliente(cliente);
        pago.setConcepto(ConceptoPago.INSCRIPCION);
        pago.setReferenciaId(5L);
        pago.setMontoEsperado(new BigDecimal(esperado));
        pago.setMontoPagado(new BigDecimal(pagado));
        pago.setMetodo(MetodoPago.TRANSFERENCIA);
        pago.setEstado(estado);
        when(pagoRepository.findWithRelacionesById(PAGO_ID)).thenReturn(Optional.of(pago));
        when(pagoRepository.findForUpdateById(PAGO_ID)).thenReturn(Optional.of(pago));
        return pago;
    }

    @Test
    @DisplayName("un pago sin monto recibido queda PENDIENTE")
    void sinMonto() {
        var respuesta = service.crear(solicitud(new BigDecimal("185.00"), null));

        assertEquals(EstadoPago.PENDIENTE, respuesta.estado());
        assertEquals(new BigDecimal("185.00"), respuesta.saldo());
    }

    @Test
    @DisplayName("un pago con monto menor al esperado queda PARCIAL")
    void montoParcial() {
        var respuesta = service.crear(solicitud(new BigDecimal("185.00"), new BigDecimal("100.00")));

        assertEquals(EstadoPago.PARCIAL, respuesta.estado());
        assertEquals(new BigDecimal("85.00"), respuesta.saldo());
    }

    @Test
    @DisplayName("un pago con el monto completo queda PAGADO")
    void montoCompleto() {
        var respuesta = service.crear(solicitud(new BigDecimal("185.00"), new BigDecimal("185.00")));

        assertEquals(EstadoPago.PAGADO, respuesta.estado());
        assertEquals(0, respuesta.saldo().compareTo(BigDecimal.ZERO));
        assertFalse(respuesta.tieneComprobante());
    }

    @Test
    @DisplayName("rechaza un monto pagado mayor al esperado")
    void montoMayorAlEsperado() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.crear(solicitud(new BigDecimal("185.00"), new BigDecimal("200.00"))));

        assertTrue(ex.getMessage().contains("no puede superar"));
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    @DisplayName("falla cuando el registro referenciado no existe")
    void referenciaInexistente() {
        when(inscripcionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.crear(solicitud(new BigDecimal("185.00"), null)));
    }

    @Test
    @DisplayName("consultar un pago que no existe devuelve 404")
    void pagoInexistente() {
        when(pagoRepository.findWithRelacionesById(PAGO_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.obtener(PAGO_ID));

        assertTrue(ex.getMessage().contains("Pago"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("operar sobre un pago que no existe devuelve 404")
    void operacionesSobrePagoInexistente() {
        when(pagoRepository.findWithRelacionesById(PAGO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.abonar(PAGO_ID, new AbonoPagoRequest(new BigDecimal("10.00"), null)));
        assertThrows(ResourceNotFoundException.class,
                () -> service.cambiarEstado(PAGO_ID, EstadoPago.PAGADO));
        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(PAGO_ID));
        // Ninguna de las tres debe haber tocado la base de datos
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    @DisplayName("un abono que completa el monto deja el pago PAGADO")
    void abonoCompleta() {
        pagoExistente("185.00", "100.00", EstadoPago.PARCIAL);

        var respuesta = service.abonar(PAGO_ID, new AbonoPagoRequest(new BigDecimal("85.00"), "TRF-002"));

        assertEquals(EstadoPago.PAGADO, respuesta.estado());
        assertEquals(new BigDecimal("185.00"), respuesta.montoPagado());
    }

    @Test
    @DisplayName("rechaza un abono que excede el saldo pendiente")
    void abonoExcedeSaldo() {
        pagoExistente("185.00", "100.00", EstadoPago.PARCIAL);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.abonar(PAGO_ID, new AbonoPagoRequest(new BigDecimal("200.00"), null)));

        assertTrue(ex.getMessage().contains("excede el saldo"));
    }

    @Test
    @DisplayName("rechaza abonar a un pago ya saldado")
    void abonoSobrePagado() {
        pagoExistente("185.00", "185.00", EstadoPago.PAGADO);

        assertThrows(BusinessException.class,
                () -> service.abonar(PAGO_ID, new AbonoPagoRequest(new BigDecimal("10.00"), null)));
    }

    @Test
    @DisplayName("no permite confirmar un pago que contabilidad no ha validado")
    void confirmarSinValidar() {
        pagoExistente("185.00", "185.00", EstadoPago.PAGADO);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmar(PAGO_ID, "recepcion@cfc.uca.edu.sv"));

        assertTrue(ex.getMessage().contains("validado"));
    }

    @Test
    @DisplayName("validar y luego confirmar deja la trazabilidad de ambos responsables")
    void validarYConfirmar() {
        pagoExistente("185.00", "185.00", EstadoPago.PAGADO);

        service.validar(PAGO_ID, "contabilidad@cfc.uca.edu.sv");
        var respuesta = service.confirmar(PAGO_ID, "recepcion@cfc.uca.edu.sv");

        assertEquals("contabilidad@cfc.uca.edu.sv", respuesta.validadoPor());
        assertEquals("recepcion@cfc.uca.edu.sv", respuesta.confirmadoPor());
    }

    @Test
    @DisplayName("no permite validar un pago sin monto recibido")
    void validarPendiente() {
        pagoExistente("185.00", "0.00", EstadoPago.PENDIENTE);

        assertThrows(BusinessException.class, () -> service.validar(PAGO_ID, "contabilidad@cfc.uca.edu.sv"));
    }

    @Test
    @DisplayName("el flujo financiero no retrocede de PAGADO a PENDIENTE")
    void estadoNoRetrocede() {
        pagoExistente("185.00", "185.00", EstadoPago.PAGADO);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cambiarEstado(PAGO_ID, EstadoPago.PENDIENTE));

        assertTrue(ex.getMessage().contains("no permite"));
    }

    @Test
    @DisplayName("eliminar marca el pago como eliminado en lugar de borrarlo")
    void borradoLogico() {
        Pago pago = pagoExistente("185.00", "185.00", EstadoPago.PAGADO);

        service.eliminar(PAGO_ID);

        assertTrue(pago.isEliminado());
        verify(pagoRepository).save(pago);
        verify(pagoRepository, never()).delete(any(Pago.class));
    }

    @Test
    @DisplayName("no elimina un pago que ya tiene comprobante emitido")
    void noEliminaConComprobante() {
        pagoExistente("185.00", "185.00", EstadoPago.PAGADO);
        when(comprobanteRepository.existsByPagoId(PAGO_ID)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.eliminar(PAGO_ID));
    }

    @Test
    void rechazaDocumentoDeOtroCliente() {
        Cliente otro = new Cliente();
        otro.setId(99L);
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setCliente(otro);
        when(inscripcionRepository.findById(5L)).thenReturn(Optional.of(inscripcion));

        assertThrows(BusinessException.class,
                () -> service.crear(solicitud(new BigDecimal("100"), BigDecimal.TEN)));
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void noPermiteParcialSinDinero() {
        pagoExistente("100", "0", EstadoPago.PENDIENTE);
        assertThrows(BusinessException.class, () -> service.cambiarEstado(PAGO_ID, EstadoPago.PARCIAL));
    }

    @Test
    void noInventaDineroAlMarcarPagado() {
        Pago pago = pagoExistente("100", "20", EstadoPago.PARCIAL);
        assertThrows(BusinessException.class, () -> service.cambiarEstado(PAGO_ID, EstadoPago.PAGADO));
        assertEquals(new BigDecimal("20"), pago.getMontoPagado());
    }

    @Test
    void noValidaEstadoHistoricoParcialSinDinero() {
        pagoExistente("100", "0", EstadoPago.PARCIAL);
        assertThrows(BusinessException.class, () -> service.validar(PAGO_ID, "contabilidad"));
    }
}
