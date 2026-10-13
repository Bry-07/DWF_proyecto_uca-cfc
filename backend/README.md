# UCA-CFC Connect

Plataforma empresarial del **Centro de Formación Continua (CFC)** de la Universidad Centroamericana.
API REST construida con Spring Boot 4 y PostgreSQL que centraliza la gestión académica, comercial y
administrativa del centro.

> **Cátedra:** Desarrollo de Software Empresarial · Universidad Don Bosco
> **Fase 2 — Primer avance:** base de datos, CRUD completos, persistencia, validaciones, manejo de
> excepciones y documentación Swagger.
> **Entrega:** 25 de septiembre de 2026

---

## Tabla de contenido

1. [Descripción del proyecto](#1-descripción-del-proyecto)
2. [Módulos del sistema](#2-módulos-del-sistema)
3. [Arquitectura](#3-arquitectura)
4. [Tecnologías](#4-tecnologías)
5. [Requisitos previos](#5-requisitos-previos)
6. [Configuración de la base de datos](#6-configuración-de-la-base-de-datos)
7. [Variables de entorno](#7-variables-de-entorno)
8. [Ejecución](#8-ejecución)
9. [Documentación de la API](#9-documentación-de-la-api)
10. [Credenciales de prueba](#10-credenciales-de-prueba)
11. [Estructura del proyecto](#11-estructura-del-proyecto)
12. [Reglas de negocio](#12-reglas-de-negocio)
13. [Manejo de errores](#13-manejo-de-errores)
14. [Convenciones de la API](#14-convenciones-de-la-api)
15. [Pruebas](#15-pruebas)
16. [Evidencias de prueba](#16-evidencias-de-prueba)
17. [Integrantes y responsabilidades](#17-integrantes-y-responsabilidades)
18. [Estado del proyecto y próximos pasos](#18-estado-del-proyecto-y-próximos-pasos)

---

## 1. Descripción del proyecto

El CFC gestiona actualmente su oferta académica, el alquiler de espacios, el servicio de catering y el
control de pagos con herramientas dispersas, lo que genera duplicidad de información, conflictos de
agenda y pérdida de trazabilidad financiera.

**UCA-CFC Connect** unifica esos procesos en una sola API REST que sirve como back-end para el portal
web del centro. El sistema permite publicar la oferta académica, inscribir participantes controlando
cupos, cotizar servicios, reservar espacios sin choques de horario, coordinar catering, mantener una
agenda institucional única y registrar los pagos con su respaldo documental.

## 2. Módulos del sistema

| # | Módulo | Responsabilidad | Endpoint base |
|---|--------|-----------------|---------------|
| 1 | Catálogo académico | Cursos y diplomados con categoría, modalidad, docente y cupos | `/api/v1/cursos`, `/api/v1/diplomados` |
| 2 | Clientes | Personas particulares y empresas, con su historial consolidado | `/api/v1/clientes` |
| 3 | Inscripciones | Registro de participantes con control de cupo y estados | `/api/v1/inscripciones` |
| 4 | Cotizaciones | Cotizaciones por ítems con cálculo de totales y flujo de aprobación | `/api/v1/cotizaciones` |
| 5 | Espacios y alquileres | Auditorios, aulas y laboratorios; reservas sin traslape | `/api/v1/espacios`, `/api/v1/alquileres` |
| 6 | Catering | Catálogo de servicios y solicitudes de los clientes | `/api/v1/servicios-catering`, `/api/v1/solicitudes-catering` |
| 7 | Agenda institucional | Calendario único con detección de conflictos | `/api/v1/agenda` |
| 8 | Pagos | Registro, abonos, validación, confirmación y comprobantes | `/api/v1/pagos` |
| 9 | Seguridad | Usuarios y roles del sistema | `/api/v1/usuarios`, `/api/v1/roles` |

Catálogos de apoyo: `/api/v1/categorias`, `/api/v1/modalidades`, `/api/v1/docentes`.

## 3. Arquitectura

Arquitectura MVC + REST organizada por capas, con una responsabilidad clara en cada nivel:

```
Cliente HTTP
    │
    ▼
┌─────────────────┐   Recibe la petición, valida el formato (@Valid) y devuelve el código HTTP.
│  Controller     │   No contiene lógica de negocio.
└────────┬────────┘
         ▼
┌─────────────────┐   Reglas de negocio, transacciones y orquestación entre módulos.
│  Service        │   Es donde viven el control de cupos y la prevención de traslapes.
└────────┬────────┘
         ▼
┌─────────────────┐   Acceso a datos con Spring Data JPA y consultas JPQL.
│  Repository     │
└────────┬────────┘
         ▼
┌─────────────────┐   Entidades JPA. El esquema lo administra Flyway.
│  PostgreSQL     │
└─────────────────┘
```

Decisiones de diseño relevantes:

- **Los DTO nunca exponen entidades.** Las peticiones y respuestas usan `record` inmutables; los
  mappers traducen entre capas. Ninguna respuesta incluye contraseñas ni hashes.
- **Las reglas de negocio viven en el Service**, no en el controlador ni en la base de datos, aunque
  la base de datos las respalda con restricciones `CHECK` e índices únicos como segunda línea de defensa.
- **El esquema es responsabilidad de Flyway.** Hibernate corre con `ddl-auto: validate`, de modo que
  cualquier discrepancia entre las entidades y las tablas detiene el arranque en lugar de corromper
  datos en silencio.
- **La agenda se sincroniza automáticamente.** Un alquiler o una solicitud de catering en un espacio
  del centro crea, actualiza o retira su actividad en el calendario sin intervención manual.

## 4. Tecnologías

| Componente | Versión |
|------------|---------|
| Java | 21 (LTS) |
| Spring Boot | 4.0.6 |
| Spring Data JPA / Hibernate | 4.0.x / 7.2 |
| PostgreSQL | 16 |
| Flyway | 11.14 |
| springdoc-openapi (Swagger UI) | 3.0.3 |
| Bean Validation | Jakarta Validation |
| BCrypt | spring-security-crypto |
| Maven | Wrapper incluido (`./mvnw`) |
| JUnit 5 + Mockito | vía `spring-boot-starter-test` |

## 5. Requisitos previos

- JDK 21 o superior
- PostgreSQL 16 en ejecución
- Maven no es necesario: el proyecto incluye el *wrapper* (`./mvnw`)

Alternativamente, `compose.yaml` levanta PostgreSQL con Docker:

```bash
docker compose up -d
```

## 6. Configuración de la base de datos

Cree el rol y las bases de datos (desarrollo y pruebas):

```sql
CREATE ROLE cfc_app LOGIN PASSWORD 'cfc_app_dev';
CREATE DATABASE uca_cfc_connect      OWNER cfc_app;
CREATE DATABASE uca_cfc_connect_test OWNER cfc_app;
```

No hace falta crear tablas: **Flyway aplica las migraciones en el primer arranque**.

| Migración | Contenido |
|-----------|-----------|
| `V1__esquema_inicial.sql` | 20 tablas con llaves foráneas, restricciones `CHECK`, índices e índices únicos parciales |
| `V2__datos_iniciales.sql` | Datos de demostración: roles, usuarios, catálogos, clientes, inscripciones, cotizaciones, alquileres, catering, agenda y pagos |

### Script completo de la base de datos

Para recrear la base a mano, sin ejecutar la aplicación, el archivo
`db/esquema_completo.sql` reúne las dos migraciones en orden:

```powershell
psql -U cfc_app -d uca_cfc_connect -f db\esquema_completo.sql
```

## 7. Variables de entorno

Todas tienen un valor por defecto apto para desarrollo local. El archivo `.env.example` documenta el
conjunto completo.

| Variable | Valor por defecto |
|----------|-------------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/uca_cfc_connect` |
| `DATABASE_USERNAME` | `cfc_app` |
| `DATABASE_PASSWORD` | `cfc_app_dev` |
| `SERVER_PORT` | `8080` |

## 8. Ejecución

**Windows (PowerShell):**

```powershell
# Compilar y ejecutar las pruebas
.\mvnw.cmd clean install

# Levantar la aplicación (perfil dev por omisión)
.\mvnw.cmd spring-boot:run

# Ejecutar el JAR empaquetado
java -jar target\uca-cfc-connect-0.2.0-SNAPSHOT.jar
```

**Linux / macOS:**

```bash
./mvnw clean install
./mvnw spring-boot:run
java -jar target/uca-cfc-connect-0.2.0-SNAPSHOT.jar
```

La API queda disponible en `http://localhost:8080`.

La primera ejecución tarda varios minutos porque Maven descarga las dependencias. El arranque
termina cuando aparece en el log:

```
Migrating schema "public" to version "1 - esquema inicial"
Migrating schema "public" to version "2 - datos iniciales"
Started UcaCfcConnectApplication in 18.05 seconds
```

Las dos líneas de Flyway solo aparecen la primera vez; después se lee
`Successfully validated 2 migrations`.

Perfiles disponibles:

| Perfil | Uso |
|--------|-----|
| `dev` (por omisión) | Muestra el SQL generado y eleva el nivel de log |
| `test` | Apunta a `uca_cfc_connect_test`, sin datos de demostración |

## 9. Documentación de la API

Con la aplicación en ejecución:

- **Swagger UI:** <http://localhost:8080/swagger-ui.html>
- **Especificación OpenAPI:** <http://localhost:8080/v3/api-docs>

Los 99 endpoints están documentados en español, agrupados por módulo, con descripción de cada
parámetro, ejemplos de valores y las respuestas de error que puede devolver cada operación.

## 10. Credenciales de prueba

Usuarios cargados por el seed. **Válidos únicamente en desarrollo.**

| Rol | Correo | Contraseña |
|-----|--------|------------|
| ADMIN | `admin@cfc.uca.edu.sv` | `Admin#2026.Cfc` |
| RECEPCIONISTA | `recepcion@cfc.uca.edu.sv` | `Recepcion#2026.Cfc` |
| CONTABILIDAD | `contabilidad@cfc.uca.edu.sv` | `Conta#2026.Cfc` |
| CLIENTE | `marta.gomez@vitalab.com.sv` | `Cliente#2026.Cfc` |

Las contraseñas se almacenan cifradas con BCrypt; la base de datos nunca guarda texto plano.

> La autenticación con JWT corresponde a la Fase 3. En esta entrega los endpoints se pueden probar
> directamente desde Swagger UI, y los roles ya están modelados y poblados.

## 11. Estructura del proyecto

```
src/main/java/com/uca/cfc/
├── config/        Configuración: OpenAPI, auditoría JPA, reloj inyectable, BCrypt
├── controller/    16 controladores REST bajo /api/v1
├── dto/           51 records de petición y respuesta, agrupados por módulo
├── entity/        24 entidades JPA + 17 enums de dominio
├── exception/     Excepciones de negocio y @RestControllerAdvice global
├── mapper/        10 componentes de conversión entidad ↔ DTO
├── repository/    19 repositorios Spring Data con consultas JPQL
├── service/       16 servicios con las reglas de negocio
└── util/          Utilidades: intervalos horarios, códigos, cabecera Location

src/main/resources/
├── db/migration/  Migraciones Flyway
└── application*.yml

src/test/java/com/uca/cfc/   52 pruebas unitarias

db/                          Script completo de la base de datos
evidencias/                  Script generador y evidencias de prueba
```

## 12. Reglas de negocio

### Control de cupos (crítica)

Una inscripción **nunca** puede superar el cupo máximo de la oferta. La verificación ocurre dentro de
la misma transacción que inserta la inscripción, y sobre la fila del curso bloqueada con
`SELECT ... FOR UPDATE`, de modo que dos solicitudes simultáneas no puedan leer el mismo conteo y
superar el cupo entre ambas. Si no hay cupo, la respuesta es `409 CONFLICT` y no se persiste nada.

Las inscripciones canceladas liberan su cupo. Un mismo participante no puede tener dos inscripciones
vigentes en la misma oferta, lo que además respalda un índice único parcial en la base de datos.

### Prevención de traslapes (crítica)

Un espacio **nunca** puede reservarse dos veces en horarios que se superponen. La condición aplicada es:

```
nuevoInicio < existenteFin  AND  nuevoFin > existenteInicio
```

El fin del intervalo es exclusivo: una reserva de 8:00 a 11:00 no choca con otra que empieza a las
11:00. La comprobación cruza tanto los alquileres vigentes como las actividades de la agenda
institucional (clases, eventos, catering), y se ejecuta con la fila del espacio bloqueada. Los
alquileres cancelados liberan el horario.

### Otras reglas

| Ámbito | Regla |
|--------|-------|
| Clientes | Una persona particular se identifica con DUI; una empresa, con NIT y su ficha de empresa |
| Inscripciones | Transiciones: PENDIENTE → CONFIRMADA \| CANCELADA; CONFIRMADA → FINALIZADA \| CANCELADA |
| Inscripciones | El precio se congela al inscribir: cambios posteriores del catálogo no lo alteran |
| Cotizaciones | El tipo se deduce de los ítems; con más de un tipo, es COMBINADA |
| Cotizaciones | Una cotización vencida no puede aprobarse; una aprobada no puede eliminarse |
| Alquileres | El monto se prorratea por minutos según el precio por hora del espacio |
| Alquileres | No se reserva un espacio en MANTENIMIENTO ni por encima de su capacidad |
| Catering | Se respeta el mínimo de personas del servicio y la capacidad del espacio |
| Catering | Solo el catering servido en un espacio del centro ocupa la agenda |
| Pagos | El flujo financiero solo avanza: PENDIENTE → PARCIAL → PAGADO |
| Pagos | Contabilidad valida antes de que recepción confirme |
| Pagos | La eliminación es lógica, para conservar la trazabilidad contable |
| Comprobantes | Un pago tiene como máximo un comprobante, con correlativo anual `CFC-2026-000001` |
| Catálogos | No se elimina un registro que esté referenciado; se desactiva |

## 13. Manejo de errores

Todos los errores comparten el mismo formato, producido por un `@RestControllerAdvice` global:

```json
{
  "timestamp": "2026-09-11T22:47:22.996",
  "status": 409,
  "error": "Conflict",
  "message": "El espacio 'Aula 5' ya está ocupado para el horario solicitado. Conflicto con 'Capacitación interna' (20/09/2026 08:00 – 20/09/2026 11:00)",
  "path": "/api/v1/alquileres"
}
```

Los errores de validación agregan el detalle por campo:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene datos inválidos",
  "path": "/api/v1/inscripciones",
  "errores": [
    { "campo": "participanteEmail", "mensaje": "el correo del participante no tiene un formato válido" },
    { "campo": "clienteId",         "mensaje": "el cliente es obligatorio" }
  ]
}
```

| Código | Cuándo se devuelve |
|--------|--------------------|
| `200 OK` | Consulta o actualización correcta |
| `201 Created` | Recurso creado, con cabecera `Location` |
| `204 No Content` | Eliminación correcta |
| `400 Bad Request` | Validación fallida, JSON mal formado, enum inválido, parámetro de tipo incorrecto |
| `404 Not Found` | El recurso solicitado no existe |
| `405 Method Not Allowed` | Método HTTP no soportado por la ruta |
| `409 Conflict` | Cupo agotado, espacio ocupado, duplicados, recurso en uso |
| `422 Unprocessable Content` | Regla de negocio incumplida (transición inválida, capacidad, coherencia) |
| `500 Internal Server Error` | Error inesperado; el detalle técnico queda en el log, no en la respuesta |

## 14. Convenciones de la API

| Convención | Formato | Ejemplo |
|------------|---------|---------|
| Versionado | Prefijo de ruta | `/api/v1/cursos` |
| Paginación | `page`, `size` | `?page=0&size=10` |
| Ordenamiento | `sort=campo,dirección` | `?sort=fechaInicio,desc` |
| Fechas | ISO-8601 | `2026-09-15` |
| Fecha y hora | ISO-8601 | `2026-09-15T17:00:00` |
| Montos | Decimal con dos posiciones | `185.00` |

Las respuestas paginadas usan una envoltura propia y estable:

```json
{
  "contenido": [ ... ],
  "pagina": 0,
  "tamanio": 10,
  "totalElementos": 42,
  "totalPaginas": 5,
  "ultima": false
}
```

### Ejemplos

```bash
# Catálogo de cursos activos, ordenado por fecha de inicio
curl "http://localhost:8080/api/v1/cursos?estado=ACTIVO&sort=fechaInicio,asc"

# Inscribir un participante
curl -X POST http://localhost:8080/api/v1/inscripciones \
  -H 'Content-Type: application/json' \
  -d '{"clienteId":1,"cursoId":2,"participanteNombre":"Marta Elena Gómez",
       "participanteEmail":"marta.gomez@vitalab.com.sv","participanteTelefono":"7890-1122"}'

# Consultar la disponibilidad de un espacio
curl "http://localhost:8080/api/v1/espacios/1/disponibilidad?desde=2026-09-15T08:00:00&hasta=2026-09-15T20:00:00"

# Detectar conflictos en la agenda
curl "http://localhost:8080/api/v1/agenda/conflictos"

# Reporte de estados financieros
curl "http://localhost:8080/api/v1/pagos/reportes/estados-financieros?desde=2026-09-01&hasta=2026-09-30"
```

## 15. Pruebas

```powershell
.\mvnw.cmd test
```

52 pruebas unitarias sobre las reglas de negocio, con los repositorios simulados mediante Mockito:

| Clase | Cubre |
|-------|-------|
| `IntervaloHorarioTest` | La regla de solapamiento aislada, incluidos los casos límite (horarios contiguos) |
| `InscripcionServiceTest` | Inscripción válida, último cupo, cupo agotado, curso inexistente, participante repetido, oferta inactiva |
| `AlquilerServiceTest` | Reserva válida, traslape con alquiler y con la agenda, espacio inexistente, capacidad, prorrateo del monto |
| `PagoServiceTest` | Pago válido, cambio de estado, pago inexistente, abonos, validación y confirmación, borrado lógico |
| `CotizacionServiceTest` | Deducción del tipo, cálculo del total, vencimiento y flujo de resolución |

## 16. Evidencias de prueba

La carpeta `evidencias/` contiene un script que recorre la API con la aplicación en marcha y
genera un informe con la petición, el código HTTP y la respuesta de cada caso:

```powershell
# 1. En una ventana: levantar la aplicación
.\mvnw.cmd spring-boot:run

# 2. En otra ventana, desde la raíz del proyecto
.\evidencias\generar-evidencias.ps1
```

El resultado se escribe en `evidencias/EVIDENCIAS.md` y cubre las cuatro categorías solicitadas:

| Sección | Contenido |
|---------|-----------|
| 1. CRUD completo | Crear, consultar, listar, actualizar y eliminar un recurso, y comprobar que deja de existir |
| 2. Validaciones | Campos obligatorios, formatos (DUI, correo, teléfono), política de contraseña, enums y colecciones vacías |
| 3. Manejo de errores | 404, 400, 405, 409 de las dos reglas críticas, 422 de negocio e integridad referencial |
| 4. Búsquedas personalizadas | Paginación, ordenamiento, filtros simples y combinados, rangos de fecha y disponibilidad |
| 5. Módulos | Una consulta representativa por cada módulo desarrollado |

Las respuestas del informe son reales, obtenidas de la aplicación en ejecución. El script debe
correrse sobre la base recién creada por Flyway, porque algunos casos crean y eliminan registros.

## 17. Integrantes y responsabilidades

| Integrante | Carné | Responsabilidad en la Fase 2 |
|------------|-------|------------------------------|
| Dominic Alejandro Castillo González | CG251726 | _(por completar)_ |
| _(por completar)_ | | |
| _(por completar)_ | | |
| _(por completar)_ | | |
| _(por completar)_ | | |

> Módulos disponibles para repartir: Gestión Académica (cursos, diplomados, categorías,
> modalidades, docentes) · Clientes e Inscripciones · Cotizaciones · Espacios y Alquileres ·
> Catering y Agenda · Pagos y Comprobantes · Seguridad (usuarios y roles) · Base de datos y
> migraciones · Manejo de excepciones y validaciones · Documentación y evidencias.

## 18. Estado del proyecto y próximos pasos

### Entregado en esta fase

- [x] Base de datos implementada con migraciones versionadas
- [x] CRUD completo de los 9 módulos (99 endpoints)
- [x] Persistencia con Spring Data JPA y consultas optimizadas con `@EntityGraph`
- [x] Validaciones con Bean Validation y reglas de negocio en la capa de servicio
- [x] Manejo centralizado de excepciones con respuestas uniformes
- [x] Documentación completa con Swagger UI
- [x] Datos de demostración coherentes con el prototipo
- [x] 52 pruebas unitarias
- [x] Script completo de la base de datos (`db/esquema_completo.sql`)
- [x] Evidencias de prueba reproducibles (`evidencias/`)

### Pendiente para la Fase 3

- [ ] Autenticación con JWT y refresh tokens (la entidad y el esquema de seguridad ya están declarados)
- [ ] Autorización por rol en cada endpoint (`@PreAuthorize`)
- [ ] Auditoría con el usuario autenticado real, en lugar del valor fijo actual
- [ ] Front-end del portal según el prototipo de Figma
- [ ] Pruebas de integración sobre la API completa

### Decisiones tomadas ante puntos no especificados

| Punto | Decisión |
|-------|----------|
| Tipo de cotización | No se envía: se deduce de los ítems; con varios tipos es COMBINADA |
| Lugar del catering | Puede ser un espacio del centro o una dirección externa; solo el primero ocupa la agenda |
| Horario del catering | Se modela con fecha, hora de inicio y hora de fin, para poder cruzarlo con la agenda |
| Pagos parciales | Se modelan con `montoEsperado` y `montoPagado`, más un endpoint de abonos |
| Cambio de oferta en una inscripción | No se permite editar: se cancela y se crea una nueva, para no descuadrar los cupos |
| Actividades de agenda | Las de tipo ALQUILER y CATERING no se crean a mano: las genera su módulo |
