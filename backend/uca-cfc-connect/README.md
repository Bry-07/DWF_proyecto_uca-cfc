# UCA-CFC Connect

API REST para el Centro de Formación Continua (CFC) de la UCA, hecha con Spring Boot y PostgreSQL.

Proyecto de cátedra de **Desarrollo de Aplicaciones con Web Frameworks**, Universidad Don Bosco,
ciclo 02-2026. Docente: Ing. Yesenia Escobar.

Esta entrega incluye las fases de base de datos, CRUD de los módulos, persistencia,
validaciones, manejo de excepciones, documentación con Swagger y seguridad JWT con
control de acceso por roles.

---

## Descripción del sistema

El CFC maneja cursos, diplomados, alquiler de salones y servicios de catering, pero cada cosa la
lleva por separado: hojas de cálculo, correos y registros en papel. Eso hace que la información se
duplique, que nadie pueda ver el calendario completo del centro y que dos eventos terminen
reservados en el mismo salón a la misma hora. Controlar los pagos también se vuelve difícil.

Lo que hicimos es una API que junta todos esos procesos en un solo sistema. El backend expone 99
endpoints bajo `/api/v1` y cubre los nueve módulos que pedía el enunciado:

1. Gestión académica (cursos, diplomados, categorías, modalidades y docentes)
2. Clientes
3. Inscripciones
4. Cotizaciones
5. Alquiler de espacios
6. Catering
7. Agenda institucional
8. Pagos
9. Seguridad (usuarios y roles)

La API usa autenticación JWT. Primero debe obtenerse un access token mediante
`POST /api/v1/auth/login` y luego usarlo en Swagger con **Authorize**.

## Tecnologías

- Java 21
- Spring Boot 4.0.6 (Spring Web MVC, Spring Data JPA, Bean Validation)
- Hibernate 7.2
- PostgreSQL 16
- Flyway para las migraciones
- springdoc-openapi para Swagger
- Spring Security 7 y JWT con Nimbus
- JUnit 5 y Mockito para las pruebas
- Maven (viene el wrapper, no hace falta instalarlo)

## Requisitos para ejecutarlo

- JDK 21 o superior
- PostgreSQL 16 corriendo

No hace falta instalar Maven: el proyecto trae `mvnw.cmd` (Windows) y `mvnw` (Linux/Mac).

Si se prefiere levantar la base con Docker en vez de instalarla, el archivo `compose.yaml` ya
tiene la configuración lista:

```bash
docker compose up -d
```

## Configuración de la base de datos

Hay que crear el rol y las bases una sola vez:

```sql
CREATE ROLE cfc_app LOGIN PASSWORD 'cfc_app_dev';
CREATE DATABASE uca_cfc_connect      OWNER cfc_app;
CREATE DATABASE uca_cfc_connect_test OWNER cfc_app;
```

Las tablas no se crean a mano. Flyway aplica las migraciones solo, en el primer arranque:

- `V1__esquema_inicial.sql` crea las 20 tablas con sus llaves foráneas, restricciones e índices.
- `V2__datos_iniciales.sql` carga los datos de demostración (roles, usuarios, catálogos, clientes,
  inscripciones, cotizaciones, alquileres, catering, agenda y pagos).
- `V3__seguridad_jwt.sql` crea la revocación de sesiones y los tokens de recuperación de contraseña.
- `V4__cotizacion_estado_en_proceso.sql` alinea los estados de cotización con el
  contrato funcional (`PENDIENTE`, `EN_PROCESO`, `APROBADA` y `RECHAZADA`).

Por si se necesita recrear la base sin ejecutar la aplicación, dejamos las dos migraciones juntas
en un solo archivo:

```bash
psql -U cfc_app -d uca_cfc_connect -f db/esquema_completo.sql
```

Los datos de conexión se pueden cambiar con variables de entorno (`DATABASE_URL`,
`DATABASE_USERNAME`, `DATABASE_PASSWORD`, `SERVER_PORT`, `JWT_SECRET`,
`JWT_ACCESS_MINUTOS`, `JWT_REFRESH_DIAS`, `CORS_ORIGENES`). En `.env.example` están
los valores que usamos para desarrollo.

## Instrucciones de ejecución

En Windows:

```powershell
.\mvnw.cmd clean install     # compila y corre las pruebas
.\mvnw.cmd spring-boot:run   # levanta la aplicación
```

En Linux o Mac es lo mismo pero con `./mvnw`.

La primera vez tarda varios minutos porque Maven descarga las dependencias. Sabemos que arrancó
bien cuando en el log aparece:

```
Migrating schema "public" to version "1 - esquema inicial"
Migrating schema "public" to version "2 - datos iniciales"
Started UcaCfcConnectApplication in 18.05 seconds
```

Las dos líneas de Flyway solo salen la primera vez. Después dice `Successfully validated 2 migrations`,
que también está bien.

La API queda en `http://localhost:8080`.

## Documentación de la API

Con la aplicación corriendo:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Especificación OpenAPI: http://localhost:8080/v3/api-docs

### Probar con Swagger y Postman

Las dos herramientas son compatibles y usan el mismo contrato HTTP:

- Swagger UI permite explorar la documentación y ejecutar solicitudes desde el
  navegador.
- Postman permite ejecutar la colección automatizada con variables, captura del
  JWT y pruebas de estado HTTP.

Para Swagger:

1. Ejecutar `POST /api/v1/auth/login`.
2. Copiar `accessToken` de la respuesta.
3. Presionar **Authorize** en la parte superior derecha.
4. En el campo `bearerAuth`, escribir `Bearer <accessToken>` y presionar
   **Authorize**.
5. Cerrar la ventana con **Close** y ejecutar `GET /api/v1/auth/me` u otro
   endpoint protegido.

Si el botón **Authorize** no aparece, recargar Swagger después de reiniciar la
aplicación y comprobar `http://localhost:8080/v3/api-docs`; el JSON debe contener
`components.securitySchemes.bearerAuth`.

Para Postman, importar
`evidencias/uca-cfc-connect.postman_collection.json`. La colección guarda el
access token y refresh token automáticamente después del login administrador.
Después de ejecutar logout, limpia esas variables antes de iniciar una nueva
ejecución completa; el `401` posterior del token revocado es esperado.
La variable `baseUrl` puede cambiarse si el backend utiliza otro host o puerto.

Los 99 endpoints están documentados en español, agrupados por módulo, con los parámetros, los
ejemplos y los códigos de error que puede devolver cada uno.

### Usuarios de prueba

Los carga la migración de datos iniciales. Solo sirven para desarrollo:

| Rol | Correo | Contraseña |
|-----|--------|------------|
| ADMIN | admin@cfc.uca.edu.sv | `Admin#2026.Cfc` |
| RECEPCIONISTA | recepcion@cfc.uca.edu.sv | `Recepcion#2026.Cfc` |
| CONTABILIDAD | contabilidad@cfc.uca.edu.sv | `Conta#2026.Cfc` |
| CLIENTE | marta.gomez@vitalab.com.sv | `Cliente#2026.Cfc` |

Las contraseñas se guardan cifradas con BCrypt, nunca en texto plano.

### Flujo de autenticación en Swagger

1. Ejecutar `POST /api/v1/auth/login` con uno de los usuarios de prueba.
2. Copiar el valor de `accessToken` de la respuesta.
3. Presionar **Authorize** y escribir `Bearer <accessToken>`.
4. Ejecutar los endpoints protegidos.
5. Para renovar la sesión usar `POST /api/v1/auth/refresh`; cada refresh token
   solo puede utilizarse una vez.

## Estructura de paquetes

```
src/main/java/com/uca/cfc/
├── config/        OpenAPI, auditoría JPA, reloj inyectable y BCrypt
├── controller/    17 controladores REST bajo /api/v1
├── dto/           51 records de petición y respuesta, agrupados por módulo
├── entity/        24 entidades JPA y 17 enums del dominio
├── exception/     Excepciones de negocio y el manejador global
├── mapper/        10 componentes que convierten entidad a DTO
├── repository/    19 repositorios con las consultas JPQL
├── service/       16 servicios, donde están las reglas de negocio
└── util/          Intervalos horarios, códigos y cabecera Location

src/main/resources/
├── db/migration/  Migraciones de Flyway
└── application*.yml

src/test/java/     52 pruebas unitarias
db/                Script completo de la base de datos
evidencias/        Evidencias de prueba y el script que las genera
```

La idea es que cada módulo atraviese las mismas capas: el controlador recibe la petición y la
delega, el servicio tiene las reglas y maneja la transacción, y el repositorio habla con la base
de datos. Las entidades nunca salen hacia afuera, todo lo que entra y sale son DTOs.

## Reglas de negocio

El enunciado marcaba dos reglas que el sistema no puede dejar incumplir.

**Los cupos.** Una inscripción nunca puede superar el cupo máximo de la oferta. La verificación se
hace dentro de la misma transacción que inserta la inscripción y con la fila del curso bloqueada
(`SELECT ... FOR UPDATE`), porque si dos personas se inscriben al mismo tiempo al último cupo, las
dos podrían leer el mismo conteo y colarse. Si ya no hay cupo responde 409 y no guarda nada. Las
inscripciones canceladas liberan su lugar.

**Los traslapes.** Un espacio no puede reservarse dos veces en horarios que se superponen. La
condición es `nuevoInicio < existenteFin AND nuevoFin > existenteInicio`, y el final del intervalo
es exclusivo: una reserva que empieza justo cuando termina la anterior sí se permite. La
comprobación cruza los alquileres vigentes y también las actividades de la agenda, así que un
alquiler tampoco puede pisar una clase programada.

Otras reglas que implementamos:

- Una persona particular se identifica con DUI y una empresa con NIT más su ficha de empresa.
- El precio de una inscripción se congela al inscribir, para que un cambio de precio posterior no
  altere lo que ya se cobró.
- El tipo de cotización no se envía: se deduce de los ítems, y si hay de varios tipos queda como
  COMBINADA.
- Los estados de los pagos solo avanzan (PENDIENTE, PARCIAL, PAGADO), nunca retroceden.
- Los pagos usan borrado lógico, para no perder la trazabilidad contable.
- No se elimina un registro que esté siendo usado por otro; en esos casos se desactiva.

## Manejo de errores

Ningún controlador maneja errores por su cuenta. Todas las excepciones pasan por una sola clase
anotada con `@RestControllerAdvice` y salen con el mismo formato:

```json
{
  "timestamp": "2026-09-25T02:03:37.165097",
  "status": 409,
  "error": "Conflict",
  "message": "El espacio 'Auditorio Norte' ya está ocupado para el horario solicitado. Conflicto con 'Capacitacion corporativa' (10/11/2026 08:00 – 10/11/2026 10:00)",
  "path": "/api/v1/alquileres"
}
```

En los errores de validación se agrega un arreglo `errores` con el campo que falló y el motivo.

Los códigos que usamos son 200, 201 y 204 para las respuestas correctas; 400 cuando la petición
viene mal; 404 si el recurso no existe; 405 para métodos no permitidos; 409 para conflictos como
el cupo agotado o el espacio ocupado; 422 cuando se incumple una regla de negocio; y 500 para
errores inesperados, cuyo detalle técnico queda en el log y no en la respuesta.

## Pruebas

```powershell
.\mvnw.cmd test
```

Son 52 pruebas unitarias sobre las reglas de negocio, con los repositorios simulados con Mockito
para no depender de la base de datos:

- `IntervaloHorarioTest` prueba la regla de solapamiento aislada, incluidos los casos límite de
  horarios contiguos.
- `InscripcionServiceTest` cubre inscripción válida, último cupo, cupo agotado, curso inexistente
  y participante repetido.
- `AlquilerServiceTest` cubre reserva válida, traslape con otro alquiler y con la agenda, espacio
  inexistente, capacidad y el cálculo del monto.
- `PagoServiceTest` cubre pago válido, cambios de estado, pago inexistente, abonos, validación y
  confirmación, y el borrado lógico.
- `CotizacionServiceTest` cubre el cálculo del total, la deducción del tipo y el flujo de aprobación.

## Evidencias

En `evidencias/` está el script que recorre la API con la aplicación corriendo y genera un informe
con la petición, el código HTTP y la respuesta de cada caso:

```powershell
.\evidencias\generar-evidencias.ps1
```

El resultado queda en `evidencias/EVIDENCIAS.md` y cubre CRUD, validaciones, errores, búsquedas
personalizadas y una consulta por cada módulo. En la misma carpeta está el documento con las
capturas de Swagger.

## Integrantes y responsabilidades

| Integrante | Carné | Responsabilidad en la Fase 2 |
|------------|-------|------------------------------|
| David Ezequiel Alvarado Alvarado | AA250233 | Base de datos: esquema, migraciones y datos iniciales |
| Bryan Ernesto Anaya Brizuela | AB250136 | Repositorio, flujo de ramas y revisión de Pull Requests |
| Dominic Alejandro Castillo Gonzales | CG251726 | Endpoints REST, DTOs y documentación OpenAPI |
| Concepción Getsemaní Miranda Cuéllar | MC250288 | Documentación, README y evidencias de prueba |
| Dania Merari Urias Viscarra | UV253195 | Entidades JPA, enums y modelo de dominio |

Cada quien siguió en esta fase la línea de trabajo que tuvo a cargo en la planificación.

## Lo que queda para la Fase 3

Falta la autenticación con JWT y la autorización por rol en cada endpoint, que era lo que el
enunciado ubicaba en esta fase. El terreno ya está preparado: la entidad y la tabla de refresh
tokens existen, los cuatro roles están cargados, las contraseñas se guardan cifradas y el esquema
`bearerAuth` ya está declarado en la configuración de OpenAPI. También queda conectar la auditoría
al usuario autenticado, que por ahora guarda un valor fijo.

## Decisiones que tomamos

El enunciado no especificaba algunos puntos, así que decidimos nosotros y lo dejamos anotado por
si hay que discutirlo:

- **Tipo de cotización:** no se envía, se deduce de los ítems.
- **Lugar del catering:** puede ser un espacio del centro o una dirección externa. Solo el primero
  ocupa la agenda, porque un servicio fuera del centro no bloquea ningún salón.
- **Horario del catering:** lo modelamos con fecha, hora de inicio y hora de fin, para poder
  cruzarlo con la agenda igual que un alquiler.
- **Pagos parciales:** con `montoEsperado` y `montoPagado`, más un endpoint aparte para los abonos.
- **Cambiar la oferta de una inscripción:** no se permite editarla. Hay que cancelar y crear una
  nueva, si no los cupos se descuadran.
- **Actividades de agenda:** las de tipo ALQUILER y CATERING no se crean a mano, las genera su
  propio módulo para que el calendario no se desincronice.
