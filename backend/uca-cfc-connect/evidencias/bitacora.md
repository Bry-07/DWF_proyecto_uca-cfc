# Bitácora de integración del backend

Proyecto: **UCA-CFC Connect**  
Backend: Spring Boot + PostgreSQL + Maven  
Cliente previsto: React + Vite

## Propósito

Esta bitácora documenta el estado del backend estable, la integración progresiva de
la versión de `prueba` y los contratos que deberá consumir el frontend. Cada fase
debe dejar el proyecto compilable y con una forma concreta de validación.

## Estado inicial

- La versión estable se encuentra en `backend/uca-cfc-connect`.
- La versión de referencia generada por Claude se encuentra en
  `prueba/uca-cfc-connect`.
- Ambas versiones declaran Java 21 y Spring Boot 4.0.6.
- El entorno local validado utiliza OpenJDK 21.
- La versión estable ya contiene MVC, JPA/Hibernate, Flyway, PostgreSQL,
  validaciones, Swagger/OpenAPI, excepciones centralizadas y pruebas de negocio.
- La versión estable todavía no contiene la capa completa de Spring Security + JWT
  de la versión de referencia.
- El frontend aún no debe construirse contra endpoints que no estén documentados
  y verificados en esta bitácora.

## Plan de fases

### Fase 1 — Baseline y contrato de integración

- Registrar el estado inicial y las pruebas reproducibles.
- Definir variables de entorno, perfiles y convenciones de respuesta.
- Inventariar endpoints existentes y riesgos de integración.
- **Criterio de salida:** `mvnw test` pasa y esta bitácora contiene los comandos
  y resultados.

### Fase 2 — Fundamentos de seguridad

- Incorporar dependencias de Spring Security, OAuth2 Resource Server y pruebas.
- Añadir configuración segura, encoder/decoder JWT, CORS y respuestas `401/403`.
- Mantener Java 21 y evitar secretos en el repositorio.
- **Criterio de salida:** la aplicación inicia sin autenticación accidentalmente
  abierta y Swagger permanece accesible.

### Fase 3 — Autenticación y sesiones

- Integrar login, refresh token, logout y perfil del usuario autenticado.
- Añadir revocación de access tokens y rotación de refresh tokens.
- Probar usuario inexistente, contraseña incorrecta, usuario inactivo y token
  expirado/revocado.

### Fase 4 — Registro y recuperación de contraseña

- Integrar registro de clientes con rol `CLIENTE`.
- Añadir recuperación de contraseña de un solo uso y expiración.
- Definir el proveedor de correo; el token solo podrá mostrarse en desarrollo.

### Fase 5 — Autorización por roles y aislamiento de datos

- Aplicar los permisos de `ADMIN`, `RECEPCIONISTA`, `CLIENTE` y `CONTABILIDAD`.
- Validar que un cliente no pueda leer o modificar información de otro cliente.
- Verificar cada regla contra las rutas reales de los controladores.

### Fase 6 — Completitud funcional de la API

- Revisar CRUD, filtros, paginación y ordenamiento en todos los módulos.
- Completar validaciones, documentación OpenAPI y respuestas de error.
- Cubrir reglas de negocio de horarios, cupos, cotizaciones, catering y pagos.

### Fase 7 — Verificación para frontend y entrega

- Ejecutar pruebas completas y pruebas manuales de Swagger.
- Congelar el contrato de API para React + Vite.
- Documentar CORS, autenticación, formatos JSON, paginación y errores.
- Preparar ejemplos de consumo y una lista de regresión.

## Fase 1 — Baseline y contrato de integración

### Cambios realizados

- Se creó esta bitácora para conservar el contexto de integración.
- Se confirmó que el proyecto usa Java 21, no JDK 26.
- Se confirmó que la versión estable ya tiene pruebas unitarias de alquileres,
  cotizaciones, inscripciones, pagos y manejo de intervalos horarios.
- Se confirmó que la integración debe ser incremental: no se reemplazará
  `backend` completo por `prueba`.

### Validación automática

Comando:

```powershell
Push-Location backend\uca-cfc-connect
.\mvnw.cmd -q test
Pop-Location
```

Resultado verificado el 23 de octubre de 2026: **PASS**, con código de salida
`0`. Las advertencias actuales de Mockito/Byte Buddy sobre agentes dinámicos de
Java no constituyen fallos de las pruebas.

### Pruebas manuales de esta fase

1. Ejecutar el comando anterior desde PowerShell.
2. Confirmar que el proceso termina con código `0`.
3. Revisar los reportes en
   `backend/uca-cfc-connect/target/surefire-reports`.
4. No iniciar todavía el frontend contra endpoints de autenticación: la Fase 2
   aún no está integrada.

### Pendientes para la siguiente fase

- Incorporar las dependencias de seguridad en `pom.xml`.
- Definir `JWT_SECRET` y variables de CORS en `.env.example` y perfiles.
- Integrar la configuración JWT sin romper Swagger ni las pruebas existentes.
- Ejecutar nuevamente la suite completa antes de continuar.

## Fase 2 — Fundamentos de seguridad

### Cambios realizados

- Se agregaron Spring Security, OAuth2 Resource Server, OAuth2 JOSE y
  `spring-security-test` al [pom.xml](../pom.xml).
- Se incorporó [SecurityConfig.java](../src/main/java/com/uca/cfc/security/SecurityConfig.java)
  con API stateless, validación JWT HS256, CORS y protección de la API.
- Se incorporó [RespuestasDeSeguridad.java](../src/main/java/com/uca/cfc/security/RespuestasDeSeguridad.java)
  para devolver el formato estándar de la API en errores `401` y `403`.
- Se centralizaron los nombres de claims JWT en
  [JwtClaims.java](../src/main/java/com/uca/cfc/security/JwtClaims.java).
- Se configuraron `JWT_SECRET` y `CORS_ORIGENES` por perfil.
- Login, refresh, logout, registro y revocación persistida quedan explícitamente
  para las fases 3 y 4.

### Validación automática

Comando:

```powershell
Push-Location backend\uca-cfc-connect
.\mvnw.cmd -q test
Pop-Location
```

Resultado verificado el 23 de octubre de 2026: **PASS**, con código de salida
`0`. Durante la primera ejecución se corrigió el `groupId` de
`spring-boot-starter-security`; la dependencia correcta pertenece a
`org.springframework.boot`.

### Pruebas manuales de esta fase

1. Iniciar el backend con PostgreSQL y el perfil `dev`.
2. Abrir `/swagger-ui.html`; debe responder sin token.
3. Abrir un endpoint de `/api/v1` desde Swagger sin token; debe responder `401`
   con un objeto `ErrorResponse`, no con HTML.
4. Enviar una petición OPTIONS desde `http://localhost:3000` y confirmar que
   CORS permite el origen configurado.
5. No probar login todavía: el endpoint se habilitará en la Fase 3.

### Resultado y pendientes

- La API queda protegida antes de integrar operaciones de autenticación.
- El secreto de desarrollo tiene un valor local de respaldo; producción exige
  `JWT_SECRET`.
- La fase no se considera completa hasta que `mvnw test` pase y se comprueben
  Swagger, `401` y CORS manualmente.

## Fases 3, 4 y 5 — Autenticación, recuperación y autorización

### Cambios realizados

- Se incorporaron los DTO y [AuthController.java](../src/main/java/com/uca/cfc/controller/AuthController.java)
  para login, refresh, logout, perfil, registro y recuperación de contraseña.
- Se incorporó [AuthService.java](../src/main/java/com/uca/cfc/service/AuthService.java)
  con access tokens JWT, refresh tokens con rotación, cierre de sesión y
  verificación de usuarios activos.
- Los refresh tokens se almacenan únicamente como hash y pueden revocarse de
  forma individual o por usuario.
- Se agregaron las entidades y repositorios de tokens revocados y de recuperación
  de contraseña.
- Se agregó la migración
  [V3__seguridad_jwt.sql](../src/main/resources/db/migration/V3__seguridad_jwt.sql).
- Se agregó el registro transaccional de clientes con rol `CLIENTE`.
- Se agregó recuperación de contraseña con tokens aleatorios, hash SHA-256,
  expiración, uso único y revocación de sesiones.
- Se aplicaron permisos por `ADMIN`, `RECEPCIONISTA`, `CLIENTE` y `CONTABILIDAD`
  en [SecurityConfig.java](../src/main/java/com/uca/cfc/security/SecurityConfig.java).
- Se añadió aislamiento de cliente mediante
  [UsuarioActual.java](../src/main/java/com/uca/cfc/security/UsuarioActual.java);
  los servicios/controladores deberán usar sus verificaciones cuando se
  implementen los flujos específicos de cada módulo.
- Se actualizó OpenAPI para indicar que la API usa Bearer JWT.

### Validación automática

Comando:

```powershell
Push-Location backend\uca-cfc-connect
.\mvnw.cmd -q test
Pop-Location
```

Resultado verificado el 23 de octubre de 2026: **PASS**, con código de salida
`0`.

### Pruebas manuales

Con PostgreSQL y el perfil `dev` activos:

1. Ejecutar `.\mvnw.cmd spring-boot:run` desde `backend\uca-cfc-connect`.
2. Abrir `http://localhost:8080/swagger-ui.html`. Si no carga, comprobar
   primero que el proceso de Spring Boot siga activo y que el puerto `8080` no
   esté ocupado.
3. Ejecutar `POST /api/v1/auth/login` con una cuenta sembrada. Las contraseñas
   de demostración están documentadas en el README del backend; no deben
   reutilizarse en producción.
4. Copiar `accessToken` y usar **Authorize** en Swagger con el valor
   `Bearer <accessToken>`.
5. Ejecutar `GET /api/v1/auth/me` y confirmar que devuelve usuario y rol sin
   exponer contraseña, hash ni refresh token.
6. Ejecutar un endpoint protegido sin autorización y confirmar `401`.
7. Usar un usuario válido sin permiso para una ruta administrativa y confirmar
   `403`.
8. Ejecutar `/auth/refresh`; confirmar que devuelve un nuevo access/refresh token
   y que el refresh token anterior deja de funcionar.
9. Ejecutar `/auth/logout`; confirmar que el access token deja de funcionar y que
   el refresh token enviado tampoco puede reutilizarse.
10. Solicitar recuperación de contraseña con un correo existente y uno inexistente;
    ambos deben devolver el mismo mensaje. El token solo se muestra en `dev`
    según la configuración actual; nunca debe activarse en producción.
11. Registrar un cliente nuevo en `/auth/registro`, iniciar sesión con él y
    verificar que no puede acceder a rutas administrativas.

### Resultado y alcance

- Las rutas de autenticación, documentación y recursos protegidos ya tienen
  reglas explícitas por rol.
- Los controladores de clientes, cotizaciones, alquileres, catering,
  inscripciones y pagos fuerzan el `clienteId` del usuario autenticado cuando
  el rol es `CLIENTE`, y verifican la propiedad en consultas individuales.
- La revisión completa de filtros, paginación, ordenamiento y reglas de negocio
  queda para la Fase 6.

### Nota sobre Swagger

La URL solo responde cuando la aplicación Spring Boot está ejecutándose. En la
validación de esta fase Docker Desktop no estaba iniciado, por lo que PostgreSQL
no pudo levantarse desde `compose.yaml` y no fue posible hacer una prueba de
arranque HTTP. Las pruebas Maven sí pasaron. Antes de abrir Swagger se debe
iniciar Docker/PostgreSQL o configurar una instancia PostgreSQL local.

### Compatibilidad de clientes de prueba

Se mantienen las dos opciones:

1. **Swagger UI:** el esquema `bearerAuth` está definido en OpenAPI y aplicado
   globalmente a las operaciones protegidas; `/auth/login`, `/auth/refresh`,
   `/auth/registro` y recuperación permanecen públicas mediante
   `@SecurityRequirements`.
2. **Postman:** se creó
   [uca-cfc-connect.postman_collection.json](./uca-cfc-connect.postman_collection.json).
   La colección captura automáticamente `accessToken`, `refreshToken` y el token
   del usuario cliente para ejecutar casos de autenticación, roles, validación,
   paginación, ordenamiento y logout.

El `401` recibido en `GET /api/v1/auth/me` ocurre cuando no se envía el JWT en
el encabezado. En cualquier cliente protegido debe enviarse exactamente:

```http
Authorization: Bearer <accessToken>
```

La colección Postman ya agrega ese encabezado mediante autenticación Bearer
heredada, por lo que no es necesario escribirlo manualmente.

## Auditoría de requisitos técnicos

| Requisito | Estado | Evidencia |
|---|---|---|
| Arquitectura MVC: Controller, Service, Repository | Cumplido | Paquetes `controller`, `service` y `repository` |
| Spring Data JPA y Hibernate | Cumplido | Dependencias y entidades JPA |
| Validaciones | Cumplido | DTO con `jakarta.validation`, `@Valid` y restricciones |
| `@ControllerAdvice` | Cumplido | `GlobalExceptionHandler` |
| CRUD y búsquedas personalizadas | Cumplido | Controladores y repositorios con filtros |
| Paginación y ordenamiento | Cumplido | `Pageable` y `PageResponse` |
| Swagger/OpenAPI | Cumplido | `OpenApiConfig`, Swagger UI y esquema Bearer |
| Spring Security y JWT | Cumplido | `SecurityConfig`, `JwtService` y OAuth2 Resource Server |
| Control por roles | Cumplido | Reglas para ADMIN, RECEPCIONISTA, CLIENTE y CONTABILIDAD |
| Pruebas unitarias | Cumplido | JUnit/Mockito para alquileres, inscripciones, pagos, cotizaciones y seguridad |
| PostgreSQL | Cumplido | `compose.yaml`, Flyway y driver PostgreSQL |
| Git/GitHub y ramas por funcionalidad | Parcial/verificación externa | Deben conservarse ramas y evidencias de PR en el repositorio |
| PDF de entrega | Pendiente | Carátula, enlace GitHub y capturas de pruebas deben prepararse como entregable |

La rúbrica menciona `javax.validation`, pero este proyecto usa correctamente
`jakarta.validation`, que es el namespace requerido por Spring Boot 4. No se debe
retroceder a `javax.validation`.

## Fase 6 — Completitud funcional y consistencia de datos

### Cambios realizados

- Se corrigió la especificación OpenAPI para declarar `ErrorResponse` dentro de
  `components.schemas`; Swagger ya no muestra referencias no resueltas.
- Se añadió la migración
  [V4__cotizacion_estado_en_proceso.sql](../src/main/resources/db/migration/V4__cotizacion_estado_en_proceso.sql).
- Se alinearon `EstadoCotizacion` y `CotizacionService` con los estados exigidos:
  `PENDIENTE`, `EN_PROCESO`, `APROBADA` y `RECHAZADA`.
- Se verificó que los controladores expongan CRUD en los módulos principales y
  que las búsquedas utilicen paginación y ordenamiento.

### Validación automática y runtime

- `.\mvnw.cmd -q clean test`: **PASS**, código `0`.
- `GET /swagger-ui.html`: **200**.
- `GET /v3/api-docs`: **200**, con `ErrorResponse` y `bearerAuth`.
- `POST /api/v1/auth/login` con administrador de desarrollo: **200**.
- `GET /api/v1/auth/me` con `Authorization: Bearer <accessToken>`: **200**.
- PostgreSQL 16 de Docker: conexión verificada.

### Incidencia de Flyway resuelta

Durante el reinicio posterior a la incorporación de V4 apareció un checksum
mismatch. La base tenía el checksum `718788615`, mientras que Maven estaba
ejecutando una copia antigua de la migración desde `target/classes`, con el
checksum `-742373755`. La migración fuente actual produce `718788615`, por lo
que no era necesario ejecutar `flyway repair` ni modificar el historial de la
base.

Se ejecutó:

```powershell
Push-Location backend\uca-cfc-connect
.\mvnw.cmd -q clean package -DskipTests
Pop-Location
```

Resultado: **PASS**. Después de limpiar y recompilar, la aplicación inició
correctamente y se verificó:

- `GET /v3/api-docs`: **200**.
- Esquema `ErrorResponse`: presente.
- Esquema `bearerAuth`: presente.
- `POST /api/v1/auth/login`: **200**.
- `GET /api/v1/auth/me` con el Bearer obtenido: **200**.

La limpieza de `target` debe repetirse si se modifica una migración local antes
de volver a arrancar la aplicación.

## Fase 7 — Preparación para frontend y entrega

### Estado

- No se modificó `frontend`.
- Swagger y Postman consumen la misma API y el mismo JWT.
- La colección Postman está en
  [uca-cfc-connect.postman_collection.json](./uca-cfc-connect.postman_collection.json).
- El PDF académico todavía requiere carátula, enlace del repositorio y
  capturas reales de ejecución de pruebas; no se inventaron capturas.

## Contrato preliminar para el frontend

Este contrato se completará por fase; no se considera congelado todavía.

- Base prevista de API: `/api/v1`.
- Formato de fechas del backend: ISO-8601.
- Autenticación prevista: `Authorization: Bearer <access-token>`.
- El frontend deberá manejar al menos `400`, `401`, `403`, `404`, `409` y `422`.
- Los tokens no deben almacenarse en código fuente ni enviarse en URLs.
- La URL y el puerto del backend deberán configurarse mediante variables de
  entorno de Vite, no quedar codificados en componentes React.

  ## Cierre de validación del backend

  ### Resultado de Postman

  La colección fue ejecutada nuevamente después de limpiar las variables
  persistentes de la colección. Las pruebas pasan cuando se ejecutan en este
  orden:

  1. Salud y documentación.
  2. Login administrador.
  3. Perfil autenticado.
  4. Refresh con rotación.
  5. Autorización, roles y errores.
  6. Búsquedas, filtros, paginación y ordenamiento.
  7. Cierre de sesión.
  8. Verificación de que el token revocado recibe `401`.

  Después de `Logout`, `accessToken` y `refreshToken` quedan inválidos por diseño.
  Para una nueva ejecución completa se deben limpiar esas dos variables y volver
  a ejecutar `Login administrador`. El resultado `401` de la prueba de token
  revocado es esperado y constituye evidencia de seguridad.

  ### Contrato para React + Vite

  El frontend puede comenzar a construirse sin modificar el backend. Debe usar:

  - Base de desarrollo: `http://localhost:8080`.
  - Prefijo de API: `/api/v1`.
  - Variable recomendada: `VITE_API_URL=http://localhost:8080/api/v1`.
  - Todas las peticiones protegidas deben incluir
    `Authorization: Bearer <accessToken>`.
  - El `accessToken` se obtiene desde `POST /auth/login`.
  - El `refreshToken` solo se envía a `POST /auth/refresh`.
  - `POST /auth/refresh` rota ambos tokens; el frontend debe reemplazar los dos
    valores almacenados.
  - Después de `POST /auth/logout`, el frontend debe borrar ambos tokens locales.
  - Nunca se deben enviar tokens en query strings ni incluir secretos en el
    repositorio.
  - El origen local esperado es `http://localhost:3000`; debe coincidir con
    `CORS_ORIGENES`.

  #### Flujo de autenticación del frontend

  1. Mostrar el formulario de login.
  2. Enviar `{ "email": "...", "password": "..." }` a `/auth/login`.
  3. Guardar `accessToken`, `refreshToken` y el perfil retornado, sin guardar la
     contraseña.
  4. Enviar el access token en cada ruta protegida.
  5. Si una operación retorna `401` por expiración, intentar una sola renovación
     con `/auth/refresh`.
  6. Repetir la solicitud original solo si la renovación retorna `200`.
  7. Si refresh o logout fallan por token inválido, borrar la sesión y redirigir a
     login.
  8. Si la respuesta es `403`, mostrar falta de permisos; no repetir
     automáticamente la petición.

  #### Respuestas y estados que debe manejar la interfaz

  | Estado | Uso en frontend |
  |---|---|
  | `200` | Consulta o actualización exitosa |
  | `201` | Recurso creado |
  | `204` | Eliminación o logout exitoso sin cuerpo |
  | `400` | Datos o parámetros inválidos |
  | `401` | Sesión ausente, expirada o revocada |
  | `403` | Rol sin permiso o recurso aislado |
  | `404` | Recurso inexistente |
  | `409` | Conflicto de negocio, como cupos u horarios |
  | `422` | Validación semántica, si el endpoint la utiliza |

  Las pantallas no deben asumir que todos los errores tienen el mismo mensaje:
  deben mostrar el mensaje de `ErrorResponse` cuando exista y un mensaje genérico
  solo cuando la respuesta no tenga el formato de la API.

  #### Módulos y recursos disponibles

  Los recursos REST están bajo `/api/v1`:

  - `/auth`: login, refresh, logout, perfil, registro y recuperación de contraseña.
  - `/usuarios` y `/roles`: administración de usuarios y roles.
  - `/cursos`, `/diplomados`, `/categorias`, `/modalidades` y `/docentes`:
    catálogo académico.
  - `/clientes`: clientes, historial, inscripciones y pagos relacionados.
  - `/inscripciones`: inscripción y cambios de estado.
  - `/cotizaciones`: cotizaciones, aprobación y rechazo.
  - `/espacios`: espacios y disponibilidad.
  - `/alquileres`: reservas y cambios de estado.
  - `/servicios-catering` y `/solicitudes-catering`: catálogo y solicitudes.
  - `/agenda`: actividades y conflictos.
  - `/pagos`: pagos, abonos, comprobantes, validación, confirmación y reportes.

  Los nombres exactos de campos, DTO, parámetros, roles y códigos de respuesta
  deben consultarse en `http://localhost:8080/v3/api-docs`; no deben duplicarse
  manualmente en el frontend si ya están definidos en OpenAPI.

  #### Paginación y formularios

  Las listas aceptan, según el recurso, parámetros como:

  ```text
  ?page=0&size=10&sort=nombre,asc
  ```

  `page` comienza en cero. La interfaz debe conservar el tamaño y página
  seleccionados, mostrar estados de carga, lista vacía y error, y no asumir que
  los identificadores son consecutivos. Para crear o actualizar recursos se debe
  usar el JSON mostrado por Swagger para ese endpoint y respetar las validaciones
  del DTO.

  #### Roles y navegación

  - `ADMIN`: administración completa.
  - `RECEPCIONISTA`: operación de atención, agenda, reservas y cotizaciones
    permitidas por el backend.
  - `CONTABILIDAD`: operaciones financieras permitidas.
  - `CLIENTE`: consulta y gestión de sus propios recursos permitidos.

  El frontend puede ocultar opciones según el rol para mejorar la experiencia,
  pero la seguridad real siempre la aplica el backend. Una ruta oculta no
  sustituye la comprobación de `403`.

  ### Criterios de la rúbrica

  Implementación técnica verificada:

  - MVC con Controller, Service y Repository.
  - Spring Data JPA, Hibernate y PostgreSQL.
  - Migraciones Flyway y datos de demostración.
  - Validación con `jakarta.validation`, que es el namespace correcto para Spring
    Boot 4 aunque la rúbrica mencione `javax.validation`.
  - `@ControllerAdvice` y formato de error.
  - CRUD, búsquedas, filtros, paginación y ordenamiento.
  - Swagger/OpenAPI con `bearerAuth`.
  - Spring Security, JWT, refresh, logout y revocación.
  - Roles y aislamiento de datos por cliente.
  - Pruebas unitarias con JUnit y Mockito.
  - Colección Postman funcional para las pruebas manuales.

  Pendientes de entrega, no de implementación principal:

  1. Crear el PDF final con carátula, enlace al repositorio y capturas reales de
     pruebas unitarias.
  2. Añadir capturas de Swagger, Postman y PostgreSQL si el docente las acepta
     como evidencia complementaria.
  3. Conservar evidencia de ramas por funcionalidad, commits y Pull Requests de
     GitHub.
  4. Integrar y probar el frontend React + Vite contra este contrato.

  ### Limpieza del repositorio

  Se eliminaron únicamente artefactos generados que no forman parte del código ni
  de la rúbrica:

  - `backend/uca-cfc-connect/target/`
  - `backend/uca-cfc-connect/.idea/`

  Se conservaron deliberadamente:

  - `evidencias/`, incluyendo la bitácora, las colecciones Postman, el documento
    de evidencias y el script de generación.
  - `db/esquema_completo.sql` como respaldo reproducible de la base.
  - `src/main/resources/db/migration/` como fuente oficial de Flyway.
  - `docs/` y `frontend/`, porque pertenecen al proyecto académico.

  La carpeta `prueba/` ya no es necesaria para ejecutar el backend estable ni para
  crear el frontend: todas las implementaciones relevantes fueron integradas en
  `backend/uca-cfc-connect`, y las pruebas finales de Postman pasaron. Antes de
  eliminarla, conviene conservar una copia fuera del repositorio por si se
  necesita consultar la comparación histórica. No existe una dependencia de
  ejecución del backend hacia esa carpeta.
