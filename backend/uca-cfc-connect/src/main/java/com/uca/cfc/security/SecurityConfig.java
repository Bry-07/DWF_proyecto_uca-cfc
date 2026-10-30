package com.uca.cfc.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.uca.cfc.repository.TokenRevocadoRepository;
import com.uca.cfc.repository.UsuarioRepository;

/**
 * Configuración de Spring Security: API sin sesión, autenticada con JWT y con la
 * tabla de permisos por rol que definió el enunciado.
 *
 * Las reglas se leen de arriba hacia abajo y gana la primera que coincide, por eso
 * las rutas específicas (aprobar, validar, confirmar...) van antes que las generales.
 */
@Configuration
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String RECEPCIONISTA = "RECEPCIONISTA";
    private static final String CLIENTE = "CLIENTE";
    private static final String CONTABILIDAD = "CONTABILIDAD";

    private static final String BASE = "/api/v1";

    private final String secreto;

    public SecurityConfig(@Value("${app.jwt.secret}") String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres (variable JWT_SECRET)");
        }
        this.secreto = secreto;
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, JwtAuthenticationConverter convertidor,
            RespuestasDeSeguridad respuestas) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidor))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .authorizeHttpRequests(this::definirPermisos);
        return http.build();
    }

    private void definirPermisos(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rutas) {

        // ---- Públicas: login, registro, recuperación y documentación ----
        rutas.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll();
        rutas.requestMatchers(HttpMethod.POST, BASE + "/auth/login", BASE + "/auth/refresh",
                BASE + "/auth/registro", BASE + "/auth/recuperar-password",
                BASE + "/auth/restablecer-password").permitAll();
        rutas.requestMatchers(BASE + "/auth/**").authenticated();

        // ---- Administración del sistema ----
        rutas.requestMatchers(BASE + "/usuarios/**", BASE + "/roles/**").hasRole(ADMIN);

        // ---- Catálogo académico, espacios y servicios de catering ----
        // Todos los roles autenticados consultan; solo ADMIN crea, modifica y elimina.
        rutas.requestMatchers(HttpMethod.GET, BASE + "/espacios/*/disponibilidad")
                .hasAnyRole(ADMIN, RECEPCIONISTA, CLIENTE);
        for (String catalogo : List.of("cursos", "diplomados", "categorias", "modalidades", "docentes",
                "espacios", "servicios-catering")) {
            rutas.requestMatchers(HttpMethod.GET, BASE + "/" + catalogo, BASE + "/" + catalogo + "/**")
                    .authenticated();
            rutas.requestMatchers(BASE + "/" + catalogo, BASE + "/" + catalogo + "/**").hasRole(ADMIN);
        }

        // ---- Clientes ----
        rutas.requestMatchers(HttpMethod.GET, BASE + "/clientes", BASE + "/clientes/**")
                .hasAnyRole(ADMIN, RECEPCIONISTA, CONTABILIDAD);
        rutas.requestMatchers(HttpMethod.DELETE, BASE + "/clientes/*").hasRole(ADMIN);
        rutas.requestMatchers(BASE + "/clientes", BASE + "/clientes/**").hasAnyRole(ADMIN, RECEPCIONISTA);

        // ---- Inscripciones: el cliente puede inscribirse únicamente a su propio nombre ----
        rutas.requestMatchers(HttpMethod.POST, BASE + "/inscripciones")
                .hasAnyRole(ADMIN, RECEPCIONISTA, CLIENTE);
        rutas.requestMatchers(HttpMethod.GET, BASE + "/inscripciones", BASE + "/inscripciones/**")
                .hasAnyRole(ADMIN, RECEPCIONISTA, CONTABILIDAD, CLIENTE);
        rutas.requestMatchers(HttpMethod.DELETE, BASE + "/inscripciones/*").hasRole(ADMIN);
        rutas.requestMatchers(BASE + "/inscripciones", BASE + "/inscripciones/**")
                .hasAnyRole(ADMIN, RECEPCIONISTA);

        // ---- Cotizaciones: el cliente las solicita, solo ADMIN las aprueba o rechaza ----
        rutas.requestMatchers(HttpMethod.PUT, BASE + "/cotizaciones/*/aprobar", BASE + "/cotizaciones/*/rechazar")
                .hasRole(ADMIN);
        definirSolicitudes(rutas, "cotizaciones");

        // ---- Alquileres y catering: el cliente los solicita, el personal los gestiona ----
        definirSolicitudes(rutas, "alquileres");
        definirSolicitudes(rutas, "solicitudes-catering");

        // ---- Agenda institucional: trabajo de recepción ----
        rutas.requestMatchers(BASE + "/agenda", BASE + "/agenda/**").hasAnyRole(ADMIN, RECEPCIONISTA);

        // ---- Pagos ----
        rutas.requestMatchers(HttpMethod.GET, BASE + "/pagos/reportes/**").hasAnyRole(ADMIN, CONTABILIDAD);
        rutas.requestMatchers(HttpMethod.PUT, BASE + "/pagos/*/confirmar").hasAnyRole(ADMIN, RECEPCIONISTA);
        rutas.requestMatchers(HttpMethod.GET, BASE + "/pagos", BASE + "/pagos/*")
                .hasAnyRole(ADMIN, RECEPCIONISTA, CONTABILIDAD, CLIENTE);
        rutas.requestMatchers(HttpMethod.GET, BASE + "/pagos/*/comprobante")
                .hasAnyRole(ADMIN, RECEPCIONISTA, CONTABILIDAD);
        rutas.requestMatchers(HttpMethod.DELETE, BASE + "/pagos/*").hasRole(ADMIN);
        rutas.requestMatchers(BASE + "/pagos", BASE + "/pagos/**").hasAnyRole(ADMIN, CONTABILIDAD);

        // Cualquier otra ruta exige, como mínimo, estar autenticado
        rutas.anyRequest().authenticated();
    }

    /**
     * Patrón común de cotizaciones, alquileres y catering: todos los roles consultan
     * (el cliente solo lo suyo, lo filtra el controlador), el cliente puede crear,
     * el personal modifica y solo ADMIN elimina.
     */
    private void definirSolicitudes(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rutas,
            String recurso) {
        String ruta = BASE + "/" + recurso;
        rutas.requestMatchers(HttpMethod.GET, ruta, ruta + "/**")
                .hasAnyRole(ADMIN, RECEPCIONISTA, CONTABILIDAD, CLIENTE);
        rutas.requestMatchers(HttpMethod.POST, ruta).hasAnyRole(ADMIN, RECEPCIONISTA, CLIENTE);
        rutas.requestMatchers(HttpMethod.DELETE, ruta + "/*").hasRole(ADMIN);
        rutas.requestMatchers(ruta, ruta + "/**").hasAnyRole(ADMIN, RECEPCIONISTA);
    }

    /**
     * CORS para que el frontend (que corre en otro puerto) pueda llamar a la API desde el
     * navegador. Los orígenes permitidos salen de app.cors.origenes; el token viaja en el
     * encabezado Authorization, no en cookies, por eso no se permiten credenciales.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origenes}") List<String> origenes) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenes);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setExposedHeaders(List.of("Location"));
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    /** El rol del token pasa a ser la autoridad ROLE_xxx que entiende hasRole(). */
    @Bean
    public JwtAuthenticationConverter convertidorJwt() {
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
            Collection<GrantedAuthority> autoridades = List.of(
                    new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString(JwtService.CLAIM_ROL)));
            return autoridades;
        });
        return convertidor;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave()));
    }

    @Bean
    public JwtDecoder jwtDecoder(TokenRevocadoRepository tokenRevocadoRepository, UsuarioRepository usuarios) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> validadores = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(JwtService.EMISOR),
                new TokenRevocadoValidator(tokenRevocadoRepository),
                new UsuarioVigenteValidator(usuarios));
        decodificador.setJwtValidator(validadores);
        return decodificador;
    }

    private SecretKey clave() {
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
