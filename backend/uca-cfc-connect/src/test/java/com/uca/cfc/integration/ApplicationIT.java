package com.uca.cfc.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.uca.cfc.dto.auth.LoginRequest;
import com.uca.cfc.dto.auth.RegistroClienteRequest;
import com.uca.cfc.dto.catering.SolicitudCateringRequest;
import com.uca.cfc.dto.pago.AbonoPagoRequest;
import com.uca.cfc.dto.pago.PagoRequest;
import com.uca.cfc.dto.usuario.UsuarioRequest;
import com.uca.cfc.dto.usuario.UsuarioUpdateRequest;
import com.uca.cfc.entity.enums.*;
import com.uca.cfc.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

/** Ejecutar con PostgreSQL dedicado: ./mvnw -Dtest=ApplicationIT test. Nunca usar una base productiva. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.seguridad.mostrar-token-recuperacion=true")
class ApplicationIT {
    private static final String PASSWORD = "Pruebas#2026.Cfc";
    private static final AtomicInteger IDS = new AtomicInteger(71000000);
    private static final String RUN = UUID.randomUUID().toString();
    @Autowired private Environment environment;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthService auth;
    @Autowired private UsuarioService usuarios;
    @Autowired private RegistroService registro;
    @Autowired private PagoService pagos;
    @Autowired private CateringService catering;
    @Autowired private EspacioService espacios;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private record Cuenta(long clienteId, String email, String token) { }

    private Cuenta cliente() {
        int numero;
        do {
            numero = IDS.incrementAndGet();
        } while (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM clientes WHERE dui=?)", Boolean.class, numero + "-1")));
        String email = "it-" + RUN + "-" + numero + "@example.com";
        long id = registro.registrar(new RegistroClienteRequest(TipoCliente.PERSONA_PARTICULAR,
                "Cliente de pruebas", email, PASSWORD, numero + "-1", null, null, null, null)).id();
        return new Cuenta(id, email, auth.login(new LoginRequest(email, PASSWORD)).accessToken());
    }

    private String token(String rol) {
        String email = "rol-" + RUN + "-" + IDS.incrementAndGet() + "@example.com";
        long rolId = jdbc.queryForObject("SELECT id FROM roles WHERE nombre = ?", Long.class, rol);
        usuarios.crear(new UsuarioRequest("Operador de pruebas", email, PASSWORD, rolId));
        return auth.login(new LoginRequest(email, PASSWORD)).accessToken();
    }

    private HttpResponse<String> request(String method, String path, String token, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
                + environment.getRequiredProperty("local.server.port") + "/api/v1" + path))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return http.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }

    private void status(int expected, HttpResponse<String> response) {
        assertEquals(expected, response.statusCode(), response.body());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(HttpResponse<String> response) {
        return json.readValue(response.body(), Map.class);
    }

    private long pago(String esperado) {
        var referencia = jdbc.queryForMap("SELECT id, cliente_id FROM inscripciones ORDER BY id LIMIT 1");
        return pagos.crear(new PagoRequest(((Number) referencia.get("cliente_id")).longValue(),
                ConceptoPago.INSCRIPCION, ((Number) referencia.get("id")).longValue(),
                new BigDecimal(esperado), BigDecimal.ZERO, MetodoPago.EFECTIVO, null, null)).id();
    }

    @Test void desactivarYReactivarNoResucitaElJwt() throws Exception {
        Cuenta cuenta = cliente();
        long uid = jdbc.queryForObject("SELECT id FROM usuarios WHERE email = ?", Long.class, cuenta.email());
        status(200, request("GET", "/auth/me", cuenta.token(), null));
        usuarios.cambiarEstado(uid, EstadoUsuario.INACTIVO);
        status(401, request("GET", "/auth/me", cuenta.token(), null));
        usuarios.cambiarEstado(uid, EstadoUsuario.ACTIVO);
        status(401, request("GET", "/auth/me", cuenta.token(), null));
        String nuevo = auth.login(new LoginRequest(cuenta.email(), PASSWORD)).accessToken();
        status(200, request("GET", "/auth/me", nuevo, null));
    }

    @Test void cambiarRolInvalidaElTokenYElRefreshAnteriores() throws Exception {
        String email = "admin-" + RUN + "-" + IDS.incrementAndGet() + "@example.com";
        long admin = jdbc.queryForObject("SELECT id FROM roles WHERE nombre = 'ADMIN'", Long.class);
        long contabilidad = jdbc.queryForObject("SELECT id FROM roles WHERE nombre = 'CONTABILIDAD'", Long.class);
        long uid = usuarios.crear(new UsuarioRequest("Administrador temporal", email, PASSWORD, admin)).id();
        var sesion = auth.login(new LoginRequest(email, PASSWORD));
        status(200, request("GET", "/usuarios", sesion.accessToken(), null));
        usuarios.actualizar(uid, new UsuarioUpdateRequest("Administrador temporal", email, null, contabilidad));
        status(401, request("GET", "/usuarios", sesion.accessToken(), null));
        status(401, request("POST", "/auth/refresh", null, Map.of("refreshToken", sesion.refreshToken())));
        String nuevo = auth.login(new LoginRequest(email, PASSWORD)).accessToken();
        status(403, request("GET", "/usuarios", nuevo, null));
    }

    @Test void restablecerPasswordInvalidaAccesoYPermiteLoginNuevo() throws Exception {
        Cuenta cuenta = cliente();
        var respuesta = request("POST", "/auth/recuperar-password", null, Map.of("email", cuenta.email()));
        status(200, respuesta);
        String codigo = (String) data(respuesta).get("tokenDePrueba");
        assertNotNull(codigo);
        var body = Map.of("token", codigo, "nuevaPassword", "Nueva#2026.Clave");
        status(200, request("POST", "/auth/restablecer-password", null, body));
        status(401, request("GET", "/auth/me", cuenta.token(), null));
        status(422, request("POST", "/auth/restablecer-password", null, body));
        status(200, request("POST", "/auth/login", null,
                Map.of("email", cuenta.email(), "password", "Nueva#2026.Clave")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"INSCRIPCION", "COTIZACION", "ALQUILER", "CATERING"})
    void rechazaPagosCruzadosEntreClientes(String concepto) throws Exception {
        String tabla = switch (concepto) {
            case "INSCRIPCION" -> "inscripciones";
            case "COTIZACION" -> "cotizaciones";
            case "ALQUILER" -> "alquileres";
            default -> "solicitudes_catering";
        };
        Long referencia = jdbc.queryForObject("SELECT id FROM " + tabla + " ORDER BY id LIMIT 1", Long.class);
        Cuenta otro = cliente();
        long antes = jdbc.queryForObject("SELECT COUNT(*) FROM pagos", Long.class);
        status(422, request("POST", "/pagos", token("CONTABILIDAD"), Map.of("clienteId", otro.clienteId(),
                "concepto", concepto, "referenciaId", referencia, "montoEsperado", 100, "metodo", "EFECTIVO")));
        assertEquals(antes, jdbc.queryForObject("SELECT COUNT(*) FROM pagos", Long.class));
    }

    @Test void clienteSeInscribeSoloASuNombre() throws Exception {
        Cuenta cuenta = cliente();
        Cuenta otro = cliente();
        Long curso = jdbc.queryForObject("SELECT id FROM cursos WHERE estado='ACTIVO' ORDER BY cupo_maximo DESC LIMIT 1", Long.class);
        var body = Map.of("clienteId", cuenta.clienteId(), "cursoId", curso,
                "participanteNombre", "Participante", "participanteEmail", cuenta.email());
        status(201, request("POST", "/inscripciones", cuenta.token(), body));
        status(403, request("POST", "/inscripciones", cuenta.token(), Map.of("clienteId", otro.clienteId(),
                "cursoId", curso, "participanteNombre", "Otro", "participanteEmail", otro.email())));
    }

    @Test void contabilidadValidaYRecepcionConfirma() throws Exception {
        long id = pago("100");
        pagos.abonar(id, new AbonoPagoRequest(new BigDecimal("100"), null));
        String conta = token("CONTABILIDAD");
        String recepcion = token("RECEPCIONISTA");
        status(200, request("GET", "/pagos", recepcion, null));
        status(403, request("PUT", "/pagos/" + id + "/validar", recepcion, Map.of("responsable", "Recepción")));
        status(200, request("PUT", "/pagos/" + id + "/validar", conta, Map.of("responsable", "Contabilidad")));
        status(403, request("PUT", "/pagos/" + id + "/confirmar", conta, Map.of("responsable", "Contabilidad")));
        status(200, request("PUT", "/pagos/" + id + "/confirmar", recepcion, Map.of("responsable", "Recepción")));
    }

    @Test void estadoNoPuedeCrearDineroNiValidarCero() throws Exception {
        long id = pago("100");
        String conta = token("CONTABILIDAD");
        status(422, request("PUT", "/pagos/" + id + "/estado", conta, Map.of("estado", "PARCIAL")));
        status(422, request("PUT", "/pagos/" + id + "/estado", conta, Map.of("estado", "PAGADO")));
        status(422, request("PUT", "/pagos/" + id + "/validar", conta, Map.of("responsable", "Contabilidad")));
        assertEquals(0, pagos.obtener(id).montoPagado().signum());
    }

    @Test void cateringOcupadoNoApareceDisponible() {
        Cuenta cuenta = cliente();
        var servicio = jdbc.queryForMap("SELECT id, minimo_personas FROM servicios_catering WHERE activo=true ORDER BY minimo_personas LIMIT 1");
        long espacio = jdbc.queryForObject("SELECT id FROM espacios WHERE estado='DISPONIBLE' ORDER BY capacidad DESC LIMIT 1", Long.class);
        LocalDate fecha = LocalDate.of(2032, 5, 11);
        catering.crearSolicitud(new SolicitudCateringRequest(cuenta.clienteId(), ((Number) servicio.get("id")).longValue(),
                espacio, null, ((Number) servicio.get("minimo_personas")).intValue(), null,
                fecha, LocalTime.of(9, 0), LocalTime.of(10, 0), null));
        var disponibilidad = espacios.consultarDisponibilidad(espacio, fecha.atTime(9, 0), fecha.atTime(10, 0));
        assertFalse(disponibilidad.disponible());
        assertFalse(disponibilidad.ocupaciones().isEmpty());
    }

    private <T> List<T> simultaneos(List<Callable<T>> trabajos) throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(trabajos.size())) {
            var futures = trabajos.stream().map(trabajo -> executor.submit(() -> {
                inicio.await();
                return trabajo.call();
            })).toList();
            inicio.countDown();
            List<T> resultados = new ArrayList<>();
            for (var future : futures) resultados.add(future.get(20, TimeUnit.SECONDS));
            return resultados;
        }
    }

    @Test void ochoAbonosSimultaneosNoPierdenDinero() throws Exception {
        long id = pago("100");
        List<Callable<Long>> trabajos = new ArrayList<>();
        for (int i = 0; i < 8; i++) trabajos.add(() -> pagos.abonar(id, new AbonoPagoRequest(BigDecimal.TEN, null)).id());
        simultaneos(trabajos);
        assertEquals(0, new BigDecimal("80").compareTo(pagos.obtener(id).montoPagado()));
    }

    @Test void comprobantesSimultaneosTienenNumerosUnicos() throws Exception {
        List<Callable<String>> trabajos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            long id = pago("100");
            pagos.abonar(id, new AbonoPagoRequest(BigDecimal.TEN, null));
            trabajos.add(() -> pagos.emitirComprobante(id, null).numero());
        }
        var numeros = simultaneos(trabajos);
        assertEquals(6, new HashSet<>(numeros).size());
        for (String numero : numeros) assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM comprobantes WHERE numero=?", Integer.class, numero));
    }

    @Test void noDuplicaElComprobanteDelMismoPago() throws Exception {
        long id = pago("100");
        pagos.abonar(id, new AbonoPagoRequest(BigDecimal.TEN, null));
        Callable<Boolean> emitir = () -> {
            try {
                pagos.emitirComprobante(id, null);
                return true;
            } catch (com.uca.cfc.exception.DuplicateResourceException ex) {
                return false;
            }
        };
        assertEquals(1L, simultaneos(List.of(emitir, emitir)).stream().filter(Boolean::booleanValue).count());
    }
}
