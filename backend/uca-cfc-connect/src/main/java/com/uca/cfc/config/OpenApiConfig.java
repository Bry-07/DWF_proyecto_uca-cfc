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
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
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
        Schema<Object> campoError = new Schema<>()
                .type("object")
                .addProperty("campo", new StringSchema().example("email"))
                .addProperty("mensaje", new StringSchema().example("el correo es obligatorio"));
        Schema<Object> errorResponse = new Schema<>()
                .type("object")
                .description("Formato estándar de error de la API")
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .addProperty("status", new IntegerSchema().format("int32"))
                .addProperty("error", new StringSchema())
                .addProperty("message", new StringSchema())
                .addProperty("path", new StringSchema())
                .addProperty("errores", new ArraySchema().items(campoError));
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

                                La API usa JWT Bearer. Use /api/v1/auth/login para obtener un token
                                y el botón Authorize para probar endpoints protegidos.""")
                        .contact(new Contact().name("Equipo UCA-CFC Connect")
                                .email("cfc@uca.edu.sv"))
                        .license(new License().name("Uso académico — Universidad Don Bosco")))
                .servers(List.of(new Server().url("/").description("Servidor actual")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components()
                        .addSchemas("ErrorResponse", errorResponse)
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
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
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            if (openApi.getComponents().getSchemas() == null
                    || !openApi.getComponents().getSchemas().containsKey("ErrorResponse")) {
                openApi.getComponents().addSchemas("ErrorResponse", esquemaErrorResponse());
            }
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

    private Schema<Object> esquemaErrorResponse() {
        Schema<Object> campoError = new Schema<>()
                .type("object")
                .addProperty("campo", new StringSchema().example("email"))
                .addProperty("mensaje", new StringSchema().example("el correo es obligatorio"));
        return new Schema<>()
                .type("object")
                .description("Formato estándar de error de la API")
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .addProperty("status", new IntegerSchema().format("int32"))
                .addProperty("error", new StringSchema())
                .addProperty("message", new StringSchema())
                .addProperty("path", new StringSchema())
                .addProperty("errores", new ArraySchema().items(campoError));
    }

    private void agregar(ApiResponses responses, Content contenido, String codigo, String descripcion) {
        if (!responses.containsKey(codigo)) {
            responses.addApiResponse(codigo,
                    new ApiResponse().description(descripcion).content(contenido));
        }
    }
}
