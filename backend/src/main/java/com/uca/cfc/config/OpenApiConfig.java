package com.uca.cfc.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Documentación OpenAPI de la API. El esquema Bearer queda declarado desde ahora
 * para que la Fase 3 (seguridad con JWT) solo tenga que referenciarlo.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI ucaCfcConnectOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("UCA-CFC Connect API")
                        .version("v1")
                        .description("""
                                API REST del Centro de Formación Continua (CFC) de la UCA.

                                Centraliza la gestión académica, los clientes, las inscripciones,
                                las cotizaciones, el alquiler de espacios, el catering, la agenda
                                institucional y los pagos del centro.

                                **Reglas de negocio destacadas**
                                - Una inscripción nunca puede superar el cupo máximo de la oferta (409 CONFLICT).
                                - Un espacio nunca puede reservarse dos veces en horarios que se superponen (409 CONFLICT).
                                - Los pagos usan borrado lógico para conservar la trazabilidad contable.

                                **Convenciones**
                                - Paginación: `?page=0&size=10`
                                - Ordenamiento: `?sort=campo,asc` o `?sort=campo,desc`
                                - Todos los errores comparten el mismo formato de respuesta.

                                La autenticación con JWT se incorpora en la Fase 3; por ahora los
                                endpoints pueden probarse directamente desde esta interfaz.""")
                        .contact(new Contact().name("Equipo UCA-CFC Connect")
                                .email("cfc@uca.edu.sv"))
                        .license(new License().name("Uso académico — Universidad Don Bosco")))
                .servers(List.of(new Server().url("/").description("Servidor actual")))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Encabezado: Authorization: Bearer {jwt}")));
    }

    /**
     * Agrega a cada operación las respuestas de error que produce el manejador
     * global, en lugar de repetirlas anotación por anotación en los controllers.
     */
    @Bean
    public OpenApiCustomizer respuestasDeErrorComunes() {
        return openApi -> {
            Schema<?> errorSchema = new Schema<>().$ref("#/components/schemas/ErrorResponse");
            Content contenido = new Content().addMediaType("application/json",
                    new MediaType().schema(errorSchema));

            for (PathItem path : openApi.getPaths().values()) {
                for (Operation operation : path.readOperations()) {
                    ApiResponses responses = operation.getResponses();
                    agregar(responses, contenido, "400", "Solicitud inválida o datos que no pasan la validación");
                    agregar(responses, contenido, "404", "El recurso solicitado no existe");
                    agregar(responses, contenido, "500", "Error interno del servidor");
                    responses.forEach((codigo, respuesta) -> {
                        if (!codigo.startsWith("2") && respuesta.getContent() == null) {
                            respuesta.setContent(contenido);
                        }
                    });
                }
            }
        };
    }

    private void agregar(ApiResponses responses, Content contenido, String codigo, String descripcion) {
        if (!responses.containsKey(codigo)) {
            responses.addApiResponse(codigo,
                    new ApiResponse().description(descripcion).content(contenido));
        }
    }
}
