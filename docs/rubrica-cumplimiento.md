# Cumplimiento de la rúbrica — UCA-CFC Connect

Este documento concentra la evidencia que puede revisarse directamente en el
repositorio. Distingue la implementación técnica de los elementos que
dependen de GitHub, capturas o del PDF final.

## Resumen

| Criterio | Estado | Evidencia principal |
| --- | --- | --- |
| Arquitectura MVC | Cumplido | `controller/`, `service/`, `repository/` |
| Spring Data JPA e Hibernate | Cumplido | Entidades, repositorios y `pom.xml` |
| Validaciones | Cumplido | DTOs Jakarta Validation y `@Valid` |
| Manejo global de errores | Cumplido | `GlobalExceptionHandler` |
| Servicios REST, CRUD y búsquedas | Cumplido | Controladores, servicios y repositorios |
| Swagger/OpenAPI | Cumplido | `OpenApiConfig` y anotaciones OpenAPI |
| Spring Security y JWT | Cumplido | `SecurityConfig`, `JwtService` y auth |
| Control de acceso por roles | Cumplido | reglas HTTP y aislamiento por cliente |
| Pruebas unitarias | Cumplido | JUnit y Mockito |
| PostgreSQL | Cumplido | Docker Compose, configuración y Flyway |
| GitHub y ramas por funcionalidad | Cumplido | ramas `develop` y `feature/<modulo>-<nombre>` |
| PDF de entrega | Cumplido | Ubicado en `docs/entrega/UCA-CFC-Connect-Entrega.pdf` |

## 1. Arquitectura MVC

**Estado: Cumplido.** La aplicación separa la entrada HTTP, la lógica de
negocio y la persistencia.

- Controladores:
  [controller](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller)
- Servicios:
  [service](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/service)
- Repositorios:
  [repository](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository)

Ejemplo de un mismo módulo:

- [CursoController.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/CursoController.java)
- [CursoService.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/CursoService.java)
- [CursoRepository.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository/CursoRepository.java)

El controlador recibe la solicitud y delega al servicio; el servicio aplica
las reglas de negocio y el repositorio consulta o modifica la base.

## 2. Persistencia con Spring Data JPA e Hibernate

**Estado: Cumplido.**

- Dependencias:
  [pom.xml](../backend/uca-cfc-connect/pom.xml)
- Entidades:
  [entity](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/entity)
- Repositorios:
  [repository](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository)
- Configuración:
  [application.yml](../backend/uca-cfc-connect/src/main/resources/application.yml)

Las entidades usan `@Entity` y los repositorios extienden interfaces de Spring
Data como `JpaRepository`. Hibernate es el proveedor ORM del starter de JPA.

## 3. Validaciones de entrada

**Estado: Cumplido con Jakarta Bean Validation.** Aunque la rúbrica menciona
`javax.validation`, Spring Boot 4 utiliza `jakarta.validation`; mezclar ambos
namespaces rompería la configuración actual.

- [LoginRequest.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/dto/auth/LoginRequest.java)
- [ClienteRequest.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/dto/cliente/ClienteRequest.java)
- [AgendaController.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/AgendaController.java)

Los DTOs declaran restricciones como `@NotBlank`, `@Email`, `@Size` y
`@Pattern`, y los controladores activan la validación mediante `@Valid`.

## 4. Manejo de errores con `@ControllerAdvice`

**Estado: Cumplido.**

- [GlobalExceptionHandler.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/exception/GlobalExceptionHandler.java)
- [ErrorResponse.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/exception/ErrorResponse.java)

El manejador convierte errores de validación, autenticación, autorización,
recursos inexistentes y reglas de negocio en respuestas JSON consistentes.

## 5. Servicios REST, CRUD y búsquedas personalizadas

**Estado: Cumplido.** Los módulos exponen endpoints bajo `/api/v1`, con
operaciones de consulta, creación, actualización y eliminación donde aplican.
También existen filtros, paginación, ordenamiento y consultas por cliente,
estado, fechas y tipo.

Evidencia representativa:

- [CursoController.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/CursoController.java)
- [ClienteController.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/ClienteController.java)
- [PagoController.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/PagoController.java)
- [PagoRepository.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository/PagoRepository.java)
- [UCA-CFC Connect Completo.postman_collection.json](../backend/uca-cfc-connect/evidencias/UCA-CFC%20Connect%20Completo.postman_collection.json)

La colección evita ejecutar automáticamente todas las operaciones destructivas
para no alterar los datos de demostración sin control.

## 6. Documentación con Swagger/OpenAPI

**Estado: Cumplido.**

- [OpenApiConfig.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/config/OpenApiConfig.java)
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Especificación: `http://localhost:8080/v3/api-docs`

Los controladores usan `@Operation`, `@ApiResponse` y etiquetas por módulo.
La configuración declara `bearerAuth` para probar rutas protegidas.

## 7. Spring Security y JWT

**Estado: Cumplido.**

- [SecurityConfig.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/SecurityConfig.java)
- [JwtService.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/JwtService.java)
- [AuthService.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/AuthService.java)
- [V3__seguridad_jwt.sql](../backend/uca-cfc-connect/src/main/resources/db/migration/V3__seguridad_jwt.sql)

El backend valida bearer tokens, rota access/refresh tokens, revoca sesiones al
cerrar sesión y permite recuperación de contraseña.

## 8. Control de acceso por roles

**Estado: Cumplido.** Los roles son `ADMIN`, `RECEPCIONISTA`,
`CONTABILIDAD` y `CLIENTE`.

- Reglas HTTP:
  [SecurityConfig.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/SecurityConfig.java)
- Usuario autenticado y aislamiento:
  [UsuarioActual.java](../backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/UsuarioActual.java)

El cliente solo consulta sus propios recursos en los módulos permitidos. El
reporte financiero global está reservado a `ADMIN` y `CONTABILIDAD`; es una
restricción intencional para no exponer información de otros clientes.

## 9. Pruebas unitarias

**Estado: Cumplido.** Hay pruebas de autenticación, JWT, autorización,
inscripciones, cotizaciones, alquileres, pagos y utilidades.

- [Pruebas unitarias](../backend/uca-cfc-connect/src/test/java/com/uca/cfc)
- [AuthServiceTest.java](../backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/AuthServiceTest.java)
- [JwtServiceTest.java](../backend/uca-cfc-connect/src/test/java/com/uca/cfc/security/JwtServiceTest.java)
- [PagoServiceTest.java](../backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/PagoServiceTest.java)

Comando:

```powershell
cd backend\uca-cfc-connect
.\mvnw.cmd -q clean test
```

Resultado registrado previamente: `PASS`.

## 10. Base de datos PostgreSQL

**Estado: Cumplido.**

- [compose.yaml](../backend/uca-cfc-connect/compose.yaml)
- [application.yml](../backend/uca-cfc-connect/src/main/resources/application.yml)
- [Migraciones Flyway](../backend/uca-cfc-connect/src/main/resources/db/migration)
- [esquema_completo.sql](../backend/uca-cfc-connect/db/esquema_completo.sql)

PostgreSQL proporciona la persistencia y Flyway administra la creación y
evolución del esquema.

## 11. Git, GitHub y ramas por funcionalidad

**Estado: cumplido.**

El repositorio contiene:

- [PULL_REQUEST_TEMPLATE.md](../.github/PULL_REQUEST_TEMPLATE.md)
- [gobernanza-repositorio.md](./gobernanza-repositorio.md)
- [flujo-trabajo-develop-hotfix.md](./flujo-trabajo-develop-hotfix.md)

La convención define `main`, `develop`, ramas `feature/*`, `fix/*`, `docs/*`
y `hotfix/*`, además de Pull Requests y revisiones. Para cerrar el criterio hay que
mostrar en GitHub las ramas, commits por funcionalidad y Pull Requests reales.

## 12. Entregable PDF

**Estado: cumplido.**

Debe incluir:

1. Carátula.
2. Enlace al repositorio.
3. Capturas de las pruebas unitarias.

`docs/entrega/UCA-CFC-Connect-Entrega.pdf`

## Frontend

El frontend está en [frontend/uca-cfc-connect](../frontend/uca-cfc-connect).

- `src/api/`: cliente Axios y servicios por recurso.
- `src/context/`: sesión y usuario actual.
- `src/routes/`: rutas públicas y protegidas.
- `src/components/`: layout, tablas, formularios, modales y estados.
- `src/pages/`: catálogo, clientes, inscripciones, cotizaciones, espacios,
  catering, agenda, pagos, usuarios, dashboards y autenticación.
- `.env.local`: configuración local ignorada por Git.

Validación registrada:

```powershell
cd frontend\uca-cfc-connect
npm run lint
npm run build
```

Ambos comandos terminaron correctamente. El build conserva únicamente el aviso
conocido de tamaño del bundle; no es un error funcional.

## Veredicto

Todo está cumplido.