# UCA-CFC Connect

## Correcciones y verificación

La comparación del código anterior y el corregido, los archivos afectados y las
pruebas de regresión están en [Correcciones y pruebas](docs/correcciones-y-pruebas.md).

Para verificar todas las pruebas Java con una base PostgreSQL dedicada:

```bash
cd backend/uca-cfc-connect
docker compose -f compose.test.yaml up -d --wait
DATABASE_URL=jdbc:postgresql://localhost:55432/uca_cfc_connect_test sh ./mvnw '-Dtest=*Test,*IT' verify
docker compose -f compose.test.yaml down
```

Las pruebas de integración crean datos y requieren exclusivamente una base de pruebas.
`sh ./mvnw test` ejecuta las pruebas unitarias sin PostgreSQL. En el frontend,
ejecutar `npm ci`, `npm test`, `npm run lint` y `npm run build`.

Flyway aplica también `V5__sesiones_y_correlativos.sql`: agrega la revisión de
cuentas, corrige estados financieros inconsistentes y prepara los correlativos
anuales atómicos. Después de actualizar, se debe iniciar sesión nuevamente:
los JWT anteriores no incluyen la revisión de la cuenta.

La recuperación permite usar un código de prueba en el perfil `dev`. Para correo
real, usar un perfil distinto de `dev` y configurar `SPRING_MAIL_HOST`,
`SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` y `FRONTEND_URL`.
El transporte SMTP y TLS se configuran con las variables documentadas en
`backend/uca-cfc-connect/.env.example`.

API REST para digitalizar la operación del Centro de Formación Continua (CFC):
cursos, diplomados, inscripciones, clientes, cotizaciones, alquileres,
catering, agenda y pagos.

Este repositorio contiene el backend estable, el frontend React + Vite y la
documentación de la entrega. El frontend consume el contrato publicado por el
backend y mantiene separada la configuración de la URL mediante
`VITE_API_URL`.

## Tecnologías

- Java 21.
- Spring Boot 4.0.6.
- Spring Web MVC.
- Spring Data JPA y Hibernate.
- Jakarta Bean Validation.
- Spring Security 7 y JWT con Nimbus.
- PostgreSQL 16.
- Flyway.
- Springdoc OpenAPI / Swagger UI.
- Maven Wrapper.
- JUnit 5, Mockito y AssertJ.
- Docker Compose para PostgreSQL.

La rúbrica menciona `javax.validation`; en Spring Boot 4 el namespace correcto
es `jakarta.validation`. El comportamiento de validación requerido es el mismo,
pero no se debe mezclar `javax` con las dependencias Jakarta del proyecto.

## Estructura

```text
.
├── backend/uca-cfc-connect/
│   ├── src/main/java/com/uca/cfc/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── mapper/
│   │   ├── repository/
│   │   ├── security/
│   │   ├── service/
│   │   └── util/
│   ├── src/main/resources/db/migration/
│   ├── src/test/java/com/uca/cfc/
│   ├── db/esquema_completo.sql
│   ├── evidencias/
│   ├── compose.yaml
│   ├── pom.xml
│   └── README.md
├── docs/
├── frontend/uca-cfc-connect/
│   ├── src/
│   ├── public/
│   ├── .env.example
│   ├── package.json
│   └── vite.config.js
└── .github/
```

## Ejecutar el backend

Requisitos: JDK 21, Docker Desktop y Git.

```powershell
cd backend\uca-cfc-connect
docker compose up -d
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

La API estará disponible en `http://localhost:8080`.

URLs principales:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Flyway ejecuta automáticamente:

- [V1__esquema_inicial.sql](backend/uca-cfc-connect/src/main/resources/db/migration/V1__esquema_inicial.sql):
  esquema, claves, restricciones e índices.
- [V2__datos_iniciales.sql](backend/uca-cfc-connect/src/main/resources/db/migration/V2__datos_iniciales.sql):
  datos de demostración.
- [V3__seguridad_jwt.sql](backend/uca-cfc-connect/src/main/resources/db/migration/V3__seguridad_jwt.sql):
  revocación y recuperación de contraseña.
- [V4__cotizacion_estado_en_proceso.sql](backend/uca-cfc-connect/src/main/resources/db/migration/V4__cotizacion_estado_en_proceso.sql):
  estados de cotización.

## Usuarios de desarrollo

| Rol | Correo | Contraseña |
| --- | --- | --- |
| ADMIN | `admin@cfc.uca.edu.sv` | `Admin#2026.Cfc` |
| RECEPCIONISTA | `recepcion@cfc.uca.edu.sv` | `Recepcion#2026.Cfc` |
| CONTABILIDAD | `contabilidad@cfc.uca.edu.sv` | `Conta#2026.Cfc` |
| CLIENTE | `marta.gomez@vitalab.com.sv` | `Cliente#2026.Cfc` |

Son credenciales únicamente para desarrollo y demostración. No deben utilizarse
en producción.

## API y frontend

El frontend React + Vite debe definir:

```env
VITE_API_URL=http://localhost:8080/api/v1
```

Las rutas protegidas requieren:

```http
Authorization: Bearer <accessToken>
```

Flujo:

1. `POST /auth/login` devuelve `accessToken` y `refreshToken`.
2. El frontend guarda ambos tokens sin guardar la contraseña.
3. El `accessToken` se envía en las solicitudes protegidas.
4. `POST /auth/refresh` rota ambos tokens; se deben reemplazar los dos.
5. `POST /auth/logout` invalida la sesión; se deben eliminar ambos tokens.
6. Ante `401`, se intenta una sola renovación; ante `403`, se muestra falta de
   permisos y no se repite automáticamente la solicitud.

Recursos disponibles bajo `/api/v1`:

`auth`, `usuarios`, `roles`, `cursos`, `diplomados`, `categorias`,
`modalidades`, `docentes`, `clientes`, `inscripciones`, `cotizaciones`,
`espacios`, `alquileres`, `servicios-catering`, `solicitudes-catering`,
`agenda` y `pagos`.

Los nombres exactos de campos y respuestas deben consultarse en OpenAPI. Las
listas usan, según el recurso, parámetros como:

```text
?page=0&size=10&sort=nombre,asc
```

La página comienza en cero. La interfaz debe contemplar estados de carga, lista
vacía, error, paginación y los estados HTTP `400`, `401`, `403`, `404`, `409` y
`422`.

## Ejecutar el frontend

Requisitos: Node.js y npm.

```powershell
cd frontend\uca-cfc-connect
npm install
Copy-Item .env.example .env.local
npm run dev
```

Vite inicia en `http://localhost:3000`. El archivo `.env.local` está excluido
por `.gitignore` y no debe subirse al repositorio. Para desarrollo local:

```env
VITE_API_URL=http://localhost:8080/api/v1
```

Para validar el proyecto:

```powershell
npm run lint
npm run build
```

El frontend incluye autenticación, refresh y logout, rutas privadas,
permisos por rol, dashboards, formularios y pantallas para los módulos del
backend. La autorización definitiva continúa en Spring Security.

La navegación por rol está centralizada en
[navigation.js](frontend/uca-cfc-connect/src/utils/navigation.js). Para el
rol `CLIENTE` se muestran “Mis alquileres” y “Mis solicitudes de catering”.

## Cumplimiento de la rúbrica

Cada criterio se relaciona con una evidencia concreta del backend. Los
fragmentos son representativos; el archivo enlazado contiene la implementación
completa.

### 1. Arquitectura MVC

**Estado: Cumplido.**

Rutas:

- [Controllers](backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller)
- [Services](backend/uca-cfc-connect/src/main/java/com/uca/cfc/service)
- [Repositories](backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository)

Fragmentos:

```java
@RequestMapping("/api/v1/cursos")
public class CursoController {
    private final CursoService servicio;
}
```

```java
@Service
public class CursoService {
    private final CursoRepository repositorio;
}
```

```java
public interface CursoRepository extends JpaRepository<Curso, Long> {
}
```

Archivos:

- [CursoController.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/CursoController.java)
- [CursoService.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/CursoService.java)
- [CursoRepository.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository/CursoRepository.java)

### 2. Persistencia con Spring Data JPA e Hibernate

**Estado: Cumplido.**

Fragmentos:

```java
@Entity
@Table(name = "cursos")
public class Curso {
}
```

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
```

Dependencias y configuración:

- [pom.xml](backend/uca-cfc-connect/pom.xml)
- [application.yml](backend/uca-cfc-connect/src/main/resources/application.yml)
- [entidades](backend/uca-cfc-connect/src/main/java/com/uca/cfc/entity)

### 3. Validaciones

**Estado: Cumplido con Jakarta Bean Validation.**

Fragmento:

```java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {
}
```

Archivo:

- [LoginRequest.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/dto/auth/LoginRequest.java)

Los controladores reciben los DTO con `@Valid`, por ejemplo en
[AgendaController.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/AgendaController.java).

### 4. Manejo de errores con `@ControllerAdvice`

**Estado: Cumplido.**

Fragmento:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

Archivo:

- [GlobalExceptionHandler.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/exception/GlobalExceptionHandler.java)
- [ErrorResponse.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/exception/ErrorResponse.java)

### 5. Servicios REST CRUD y búsquedas personalizadas

**Estado: Cumplido en los módulos implementados.**

Fragmento:

```java
@GetMapping
public PageResponse<CursoResponse> listar(Pageable pageable) {
    return servicio.listar(pageable);
}
```

Archivo de ejemplo:

- [CursoController.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/CursoController.java)
- [CursoService.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/CursoService.java)
- [CursoRepository.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository/CursoRepository.java)

La lista de endpoints real y actualizada se obtiene desde
`http://localhost:8080/v3/api-docs`.

### 6. Documentación Swagger/OpenAPI

**Estado: Cumplido.**

Fragmento:

```java
@Bean
public OpenAPI api() {
    return new OpenAPI()
        .components(new Components().addSecuritySchemes("bearerAuth", ...));
}
```

Archivo:

- [OpenApiConfig.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/config/OpenApiConfig.java)

También se declara el esquema `ErrorResponse` para evitar referencias OpenAPI
incompletas.

### 7. Spring Security y JWT

**Estado: Cumplido.**

Fragmentos:

```java
http.oauth2ResourceServer(oauth -> oauth
    .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidor)));
```

```java
@Bean
public JwtDecoder jwtDecoder(TokenRevocadoRepository repositorio) {
    return NimbusJwtDecoder.withSecretKey(clave()).build();
}
```

Archivo:

- [SecurityConfig.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/SecurityConfig.java)
- [JwtService.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/JwtService.java)
- [AuthService.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/AuthService.java)

### 8. Control de acceso por roles

**Estado: Cumplido.**

Fragmento:

```java
rutas.requestMatchers(BASE + "/usuarios/**", BASE + "/roles/**")
     .hasRole(ADMIN);
```

Los roles implementados son `ADMIN`, `RECEPCIONISTA`, `CLIENTE` y
`CONTABILIDAD`. Además del filtro global, los servicios aplican aislamiento de
recursos por cliente.

Archivo:

- [SecurityConfig.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/SecurityConfig.java)
- [UsuarioActual.java](backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/UsuarioActual.java)

### 9. Pruebas unitarias

**Estado: Cumplido.**

Comando:

```powershell
cd backend\uca-cfc-connect
.\mvnw.cmd -q clean test
```

Pruebas:

- [AuthServiceTest.java](backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/AuthServiceTest.java)
- [JwtServiceTest.java](backend/uca-cfc-connect/src/test/java/com/uca/cfc/security/JwtServiceTest.java)
- [CotizacionServiceTest.java](backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/CotizacionServiceTest.java)
- [AlquilerServiceTest.java](backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/AlquilerServiceTest.java)
- [InscripcionServiceTest.java](backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/InscripcionServiceTest.java)
- [PagoServiceTest.java](backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/PagoServiceTest.java)

Resultado verificado: **PASS**.

### 10. Base de datos PostgreSQL

**Estado: Cumplido.**

Fragmentos:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/uca_cfc_connect}
```

Archivos:

- [compose.yaml](backend/uca-cfc-connect/compose.yaml)
- [application.yml](backend/uca-cfc-connect/src/main/resources/application.yml)
- [db/esquema_completo.sql](backend/uca-cfc-connect/db/esquema_completo.sql)
- [migraciones Flyway](backend/uca-cfc-connect/src/main/resources/db/migration)

### 11. Git, GitHub y ramas por funcionalidad

**Estado: Cumplido.**

La convención está documentada en [backend README](backend/uca-cfc-connect/README.md)
y en la plantilla [PULL_REQUEST_TEMPLATE.md](.github/PULL_REQUEST_TEMPLATE.md).

Para mostrar este criterio debe mostrarse la evidencia desde el Repositorio:

- URL del repositorio GitHub entregado.
- Ramas `develop` y `feature/<modulo>-<nombre>`.
- Commits por funcionalidad.
- Pull Requests y revisiones.

### 12. Entregable PDF

**Estado: Cumplido.**

El PDF debe contener:

- Carátula.
- Enlace al repositorio.
- Capturas reales de pruebas unitarias.

## Postman: colección base y colección completa

La colección disponible y validada es:

- [UCA-CFC Connect Completo.postman_collection.json](backend/uca-cfc-connect/evidencias/UCA-CFC%20Connect%20Completo.postman_collection.json)

Incluye:

- Swagger y OpenAPI.
- Login correcto e incorrecto.
- Perfil.
- Refresh con rotación.
- `401` sin token.
- `403` por rol.
- Filtros, paginación y ordenamiento.
- Logout.
- Verificación de token revocado.

### Orden de ejecución

Para cualquiera de las colecciones:

1. Iniciar PostgreSQL y Spring Boot.
2. Ejecutar `00 - Salud y documentacion`.
3. Ejecutar `01 - Autenticacion`.
4. Ejecutar `02 - Autorizacion y errores`.
5. Ejecutar `03 - Busquedas y paginacion`.
6. Ejecutar las carpetas adicionales de la colección completa.
7. Ejecutar `04 - Cierre de sesion` al final.
8. Limpiar `accessToken` y `refreshToken` en las variables de colección antes
   de una nueva ejecución completa.

La prueba de token revocado debe producir `401`; ese resultado es correcto.

## Git y colaboración

Usar ramas por funcionalidad:

```powershell
git checkout develop
git pull origin develop
git checkout -b feature/frontend-auth
git add .
git commit -m "feat(frontend): agrega flujo de autenticacion"
git push origin feature/frontend-auth
```

Convenciones recomendadas: `feat:`, `fix:`, `test:`, `docs:`, `refactor:` y
`chore:`.

## Licencia y datos

Las credenciales incluidas son datos de demostración. Cambiar secretos, usuarios
y contraseñas antes de cualquier despliegue real.
