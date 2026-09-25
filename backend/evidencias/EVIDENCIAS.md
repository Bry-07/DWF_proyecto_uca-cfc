# Evidencias de prueba - UCA-CFC Connect

Universidad Don Bosco - Desarrollo de Software Empresarial - Fase 2

Generado el 2026-09-24 21:05:06 contra `http://localhost:8080`.

Cada caso muestra la peticion enviada, el codigo HTTP devuelto y la
respuesta completa. La columna *esperado* documenta que comportamiento
se estaba verificando.

> Este archivo lo genera `evidencias\generar-evidencias.ps1` ejecutandose
> contra la aplicacion en marcha; las respuestas son reales, no transcritas.

**Importante:** el script debe ejecutarse sobre la base recien creada por
Flyway. Algunos casos crean o eliminan registros, de modo que una segunda
ejecucion seguida puede dar codigos distintos a los esperados.

---

## 1. CRUD completo

Ciclo de vida entero de un recurso (categoria): crear, consultar, listar,
actualizar y eliminar, comprobando ademas que despues de eliminar ya no existe.

### 1.1 CREATE - crear una categoria

Responde 201 y la cabecera Location apunta al recurso creado.

**Peticion**

```http
POST http://localhost:8080/api/v1/categorias
Content-Type: application/json

{
    "nombre":  "Categoria de Evidencia",
    "descripcion":  "Creada por el script de evidencias"
}
```

**Respuesta** - HTTP 201 (esperado 201) - OK

```json
{
    "id":  6,
    "nombre":  "Categoria de Evidencia",
    "descripcion":  "Creada por el script de evidencias",
    "createdAt":  "2026-09-24T21:05:08.0573614",
    "updatedAt":  "2026-09-24T21:05:08.0573614"
}
```

### 1.2 READ - consultar la categoria creada

**Peticion**

```http
GET http://localhost:8080/api/v1/categorias/6
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "id":  6,
    "nombre":  "Categoria de Evidencia",
    "descripcion":  "Creada por el script de evidencias",
    "createdAt":  "2026-09-24T21:05:08.057361",
    "updatedAt":  "2026-09-24T21:05:08.057361"
}
```

### 1.3 READ - listarla entre las demas

Filtro por nombre parcial sobre el listado paginado.

**Peticion**

```http
GET http://localhost:8080/api/v1/categorias?nombre=Evidencia
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  6,
                          "nombre":  "Categoria de Evidencia",
                          "descripcion":  "Creada por el script de evidencias",
                          "createdAt":  "2026-09-24T21:05:08.057361",
                          "updatedAt":  "2026-09-24T21:05:08.057361"
                      }
                  ],
    "pagina":  0,
    "tamanio":  10,
    "totalElementos":  1,
    "totalPaginas":  1,
    "ultima":  true
}
```

### 1.4 UPDATE - modificar la categoria

**Peticion**

```http
PUT http://localhost:8080/api/v1/categorias/6
Content-Type: application/json

{
    "nombre":  "Categoria de Evidencia (editada)",
    "descripcion":  "Descripcion actualizada"
}
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "id":  6,
    "nombre":  "Categoria de Evidencia (editada)",
    "descripcion":  "Descripcion actualizada",
    "createdAt":  "2026-09-24T21:05:08.057361",
    "updatedAt":  "2026-09-24T21:05:08.057361"
}
```

### 1.5 DELETE - eliminar la categoria

204 No Content: eliminacion correcta, sin cuerpo de respuesta.

**Peticion**

```http
DELETE http://localhost:8080/api/v1/categorias/6
```

**Respuesta** - HTTP 204 (esperado 204) - OK

```json
(sin cuerpo)
```

### 1.6 Comprobacion - la categoria ya no existe

**Peticion**

```http
GET http://localhost:8080/api/v1/categorias/6
```

**Respuesta** - HTTP 404 (esperado 404) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:08.9697942",
    "status":  404,
    "error":  "Not Found",
    "message":  "Categoría con id 6 no encontrado",
    "path":  "/api/v1/categorias/6"
}
```


---

## 2. Validaciones

Bean Validation sobre los DTO de entrada. La respuesta 400 incluye el arreglo
`errores` con el campo que fallo y el motivo, en espanol.

### 2.1 Campos obligatorios vacios o ausentes

Cuatro violaciones a la vez: cliente nulo, nombre vacio, correo mal formado y telefono con formato invalido.

**Peticion**

```http
POST http://localhost:8080/api/v1/inscripciones
Content-Type: application/json

{
    "participanteEmail":  "no-es-un-correo",
    "clienteId":  null,
    "participanteTelefono":  "123",
    "participanteNombre":  "",
    "cursoId":  2
}
```

**Respuesta** - HTTP 400 (esperado 400) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:09.2496515",
    "status":  400,
    "error":  "Bad Request",
    "message":  "La solicitud contiene datos inválidos",
    "path":  "/api/v1/inscripciones",
    "errores":  [
                    {
                        "campo":  "participanteEmail",
                        "mensaje":  "el correo del participante no tiene un formato válido"
                    },
                    {
                        "campo":  "participanteNombre",
                        "mensaje":  "el nombre del participante es obligatorio"
                    },
                    {
                        "campo":  "clienteId",
                        "mensaje":  "el cliente es obligatorio"
                    },
                    {
                        "campo":  "participanteTelefono",
                        "mensaje":  "el teléfono debe tener el formato 7890-1122"
                    }
                ]
}
```

### 2.2 Formato de DUI invalido

El DUI debe seguir el patron 00000000-0.

**Peticion**

```http
POST http://localhost:8080/api/v1/clientes
Content-Type: application/json

{
    "email":  "formato@correo.com",
    "nombre":  "Prueba Formato",
    "tipo":  "PERSONA_PARTICULAR",
    "dui":  "123"
}
```

**Respuesta** - HTTP 400 (esperado 400) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:09.3453494",
    "status":  400,
    "error":  "Bad Request",
    "message":  "La solicitud contiene datos inválidos",
    "path":  "/api/v1/clientes",
    "errores":  [
                    {
                        "campo":  "dui",
                        "mensaje":  "el DUI debe tener el formato 04512378-9"
                    }
                ]
}
```

### 2.3 Contrasena que no cumple la politica

Minimo 10 caracteres, con mayuscula, minuscula y numero.

**Peticion**

```http
POST http://localhost:8080/api/v1/usuarios
Content-Type: application/json

{
    "email":  "debil@cfc.uca.edu.sv",
    "nombre":  "Usuario Debil",
    "password":  "corta",
    "rolId":  2
}
```

**Respuesta** - HTTP 400 (esperado 400) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:09.3894827",
    "status":  400,
    "error":  "Bad Request",
    "message":  "La solicitud contiene datos inválidos",
    "path":  "/api/v1/usuarios",
    "errores":  [
                    {
                        "campo":  "password",
                        "mensaje":  "la contraseña debe tener entre 10 y 72 caracteres"
                    },
                    {
                        "campo":  "password",
                        "mensaje":  "la contraseña debe incluir al menos una minúscula, una mayúscula y un número"
                    }
                ]
}
```

### 2.4 Valor fuera del enum permitido

El mensaje enumera los valores admitidos.

**Peticion**

```http
PUT http://localhost:8080/api/v1/inscripciones/1/estado
Content-Type: application/json

{
    "estado":  "INVENTADO"
}
```

**Respuesta** - HTTP 400 (esperado 400) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:09.412759",
    "status":  400,
    "error":  "Bad Request",
    "message":  "Valor inválido \u0027INVENTADO\u0027 para el campo \u0027estado\u0027. Valores permitidos: [PENDIENTE, CONFIRMADA, CANCELADA, FINALIZADA]",
    "path":  "/api/v1/inscripciones/1/estado"
}
```

### 2.5 Coleccion vacia donde se exige al menos un elemento

**Peticion**

```http
POST http://localhost:8080/api/v1/cotizaciones
Content-Type: application/json

{
    "clienteId":  2,
    "items":  [

              ],
    "asunto":  "Cotizacion sin items"
}
```

**Respuesta** - HTTP 400 (esperado 400) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:09.6123212",
    "status":  400,
    "error":  "Bad Request",
    "message":  "La solicitud contiene datos inválidos",
    "path":  "/api/v1/cotizaciones",
    "errores":  [
                    {
                        "campo":  "items",
                        "mensaje":  "la cotización debe tener al menos un ítem"
                    }
                ]
}
```


---

## 3. Manejo de errores

Todos los errores salen del mismo `@RestControllerAdvice` y comparten formato:
`timestamp`, `status`, `error`, `message` y `path`. Se incluyen aqui las
dos reglas de negocio criticas del proyecto.

### 3.1 404 - recurso inexistente

**Peticion**

```http
GET http://localhost:8080/api/v1/cursos/99999
```

**Respuesta** - HTTP 404 (esperado 404) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:09.7621239",
    "status":  404,
    "error":  "Not Found",
    "message":  "Curso con id 99999 no encontrado",
    "path":  "/api/v1/cursos/99999"
}
```

### 3.2 400 - tipo de parametro incorrecto

El id debe ser numerico.

**Peticion**

```http
GET http://localhost:8080/api/v1/cursos/abc
```

**Respuesta** - HTTP 400 (esperado 400) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:09.7811281",
    "status":  400,
    "error":  "Bad Request",
    "message":  "Valor inválido \u0027abc\u0027 para el parámetro \u0027id\u0027",
    "path":  "/api/v1/cursos/abc"
}
```

### 3.3 400 - parametro de ordenamiento invalido

Ordenar por un campo que no existe en la entidad es un error del cliente, no del servidor.

**Peticion**

```http
GET http://localhost:8080/api/v1/cursos?sort=campoQueNoExiste,asc
```

**Respuesta** - HTTP 400 (esperado 400) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:09.888484",
    "status":  400,
    "error":  "Bad Request",
    "message":  "No se puede ordenar o filtrar por \u0027campoQueNoExiste\u0027: el campo no existe",
    "path":  "/api/v1/cursos"
}
```

### 3.4 405 - metodo HTTP no permitido

**Peticion**

```http
PATCH http://localhost:8080/api/v1/cursos/1
```

**Respuesta** - HTTP 405 (esperado 405) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:09.9053964",
    "status":  405,
    "error":  "Method Not Allowed",
    "message":  "El método PATCH no está permitido en esta ruta",
    "path":  "/api/v1/cursos/1"
}
```

### 3.5 REGLA CRITICA - cupo agotado (409)

El participante ya tiene una inscripcion vigente en esa oferta. La misma
validacion, con la fila del curso bloqueada, impide superar el cupo maximo.

**Peticion**

```http
POST http://localhost:8080/api/v1/inscripciones
Content-Type: application/json

{
    "participanteEmail":  "marta.gomez@vitalab.com.sv",
    "clienteId":  1,
    "participanteNombre":  "Marta Elena Gomez",
    "cursoId":  2
}
```

**Respuesta** - HTTP 409 (esperado 409) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:10.0783924",
    "status":  409,
    "error":  "Conflict",
    "message":  "El participante \u0027marta.gomez@vitalab.com.sv\u0027 ya tiene una inscripción vigente en esta oferta",
    "path":  "/api/v1/inscripciones"
}
```

### 3.6 REGLA CRITICA - espacio ocupado (409)

El Auditorio Principal ya tiene la asamblea de accionistas de 17:00 a 18:30.
El mensaje indica contra que actividad choca y en que horario.

**Peticion**

```http
POST http://localhost:8080/api/v1/alquileres
Content-Type: application/json

{
    "fechaHoraFin":  "2026-09-15T18:00:00",
    "fechaHoraInicio":  "2026-09-15T17:30:00",
    "cantidadAsistentes":  10,
    "espacioId":  1,
    "clienteId":  2,
    "proposito":  "Prueba de traslape"
}
```

**Respuesta** - HTTP 409 (esperado 409) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:10.1978094",
    "status":  409,
    "error":  "Conflict",
    "message":  "El espacio \u0027Auditorio Principal\u0027 ya está ocupado para el horario solicitado. Conflicto con \u0027Asamblea de accionistas\u0027 (15/09/2026 17:00 – 15/09/2026 18:30)",
    "path":  "/api/v1/alquileres"
}
```

### 3.7 Reserva contigua SI se permite (201)

Empieza exactamente cuando termina la anterior. Los intervalos tienen el fin
exclusivo, por lo que no hay traslape: nuevoInicio < existenteFin AND
nuevoFin > existenteInicio.

**Peticion**

```http
POST http://localhost:8080/api/v1/alquileres
Content-Type: application/json

{
    "fechaHoraFin":  "2026-09-15T20:00:00",
    "fechaHoraInicio":  "2026-09-15T18:30:00",
    "cantidadAsistentes":  10,
    "espacioId":  1,
    "clienteId":  2,
    "proposito":  "Reserva contigua"
}
```

**Respuesta** - HTTP 201 (esperado 201) - OK

```json
{
    "id":  4,
    "codigo":  "ALQ-0004",
    "cliente":  {
                    "id":  2,
                    "nombre":  "Corporativo Alfa"
                },
    "espacio":  {
                    "id":  1,
                    "nombre":  "Auditorio Principal"
                },
    "fechaHoraInicio":  "2026-09-15T18:30:00",
    "fechaHoraFin":  "2026-09-15T20:00:00",
    "horas":  1.5,
    "proposito":  "Reserva contigua",
    "cantidadAsistentes":  10,
    "montoTotal":  67.50,
    "estado":  "PENDIENTE",
    "createdAt":  "2026-09-24T21:05:10.2980951",
    "updatedAt":  "2026-09-24T21:05:10.2980951"
}
```

### 3.8 422 - regla de negocio incumplida

Se piden mas asistentes que la capacidad del espacio.

**Peticion**

```http
POST http://localhost:8080/api/v1/alquileres
Content-Type: application/json

{
    "fechaHoraFin":  "2026-10-20T10:00:00",
    "fechaHoraInicio":  "2026-10-20T08:00:00",
    "cantidadAsistentes":  500,
    "espacioId":  5,
    "clienteId":  2,
    "proposito":  "Exceso de aforo"
}
```

**Respuesta** - HTTP 422 (esperado 422) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:10.4933897",
    "status":  422,
    "error":  "Unprocessable Content",
    "message":  "El espacio \u0027Aula 5\u0027 tiene capacidad para 30 personas y se solicitaron 500",
    "path":  "/api/v1/alquileres"
}
```

### 3.9 409 - integridad referencial protegida

No se elimina una categoria que tiene cursos asociados.

**Peticion**

```http
DELETE http://localhost:8080/api/v1/categorias/1
```

**Respuesta** - HTTP 409 (esperado 409) - OK

```json
{
    "timestamp":  "2026-09-24T21:05:10.519393",
    "status":  409,
    "error":  "Conflict",
    "message":  "No se puede eliminar la categoría \u0027Gestión de Proyectos\u0027 porque tiene cursos o diplomados asociados",
    "path":  "/api/v1/categorias/1"
}
```


---

## 4. Busquedas personalizadas

Filtros combinables, paginacion y ordenamiento. Todos los parametros son
opcionales y se resuelven en una sola consulta JPQL por recurso.

### 4.1 Paginacion

La envoltura devuelve contenido, pagina, tamanio, totalElementos, totalPaginas y ultima.

**Peticion**

```http
GET http://localhost:8080/api/v1/cursos?page=0&size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  6,
                          "codigo":  "CUR-002",
                          "nombre":  "RedacciÃ³n de Informes TÃ©cnicos",
                          "descripcion":  "Estructura, claridad y presentaciÃ³n de informes institucionales.",
                          "categoria":  {
                                            "id":  4,
                                            "nombre":  "Desarrollo Humano"
                                        },
                          "modalidad":  {
                                            "id":  2,
                                            "nombre":  "Virtual"
                                        },
                          "tipoModalidad":  "VIRTUAL",
                          "docente":  {
                                          "id":  4,
                                          "nombre":  "Ãscar Villalta"
                                      },
                          "fechaInicio":  "2026-03-02",
                          "fechaFin":  "2026-04-10",
                          "horario":  "SÃ¡b Â· 8:00â11:00 am",
                          "duracionHoras":  18,
                          "cupoMaximo":  25,
                          "cuposOcupados":  0,
                          "cuposDisponibles":  25,
                          "precio":  75.00,
                          "estado":  "INACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  2,
                          "codigo":  "CUR-014",
                          "nombre":  "GestiÃ³n Ãgil de Proyectos",
                          "descripcion":  "Scrum, Kanban y gestiÃ³n de equipos en entornos de cambio permanente.",
                          "categoria":  {
                                            "id":  1,
                                            "nombre":  "GestiÃ³n de Proyectos"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  1,
                                          "nombre":  "Ana Beatriz Rivas"
                                      },
                          "espacio":  {
                                          "id":  3,
                                          "nombre":  "Aula 3"
                                      },
                          "fechaInicio":  "2026-09-14",
                          "fechaFin":  "2026-10-21",
                          "horario":  "LunâMiÃ© Â· 6:00â8:00 pm",
                          "duracionHoras":  32,
                          "cupoMaximo":  20,
                          "cuposOcupados":  2,
                          "cuposDisponibles":  18,
                          "precio":  185.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  3,
                          "codigo":  "CUR-021",
                          "nombre":  "Excel Avanzado para Finanzas",
                          "descripcion":  "Tablas dinÃ¡micas, modelado financiero y automatizaciÃ³n con macros.",
                          "categoria":  {
                                            "id":  2,
                                            "nombre":  "Finanzas"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  3,
                                          "nombre":  "Karla MenjÃ­var"
                                      },
                          "espacio":  {
                                          "id":  6,
                                          "nombre":  "Laboratorio 1"
                                      },
                          "fechaInicio":  "2026-09-22",
                          "fechaFin":  "2026-10-29",
                          "horario":  "MarâJue Â· 4:00â6:00 pm",
                          "duracionHoras":  24,
                          "cupoMaximo":  20,
                          "cuposOcupados":  1,
                          "cuposDisponibles":  19,
                          "precio":  95.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  6,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 4.2 Ordenamiento descendente

**Peticion**

```http
GET http://localhost:8080/api/v1/cursos?sort=precio,desc&size=5
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  5,
                          "codigo":  "CUR-033",
                          "nombre":  "InglÃ©s de Negocios Â· Nivel I",
                          "descripcion":  "ComunicaciÃ³n escrita y oral en contextos corporativos.",
                          "categoria":  {
                                            "id":  5,
                                            "nombre":  "Idiomas"
                                        },
                          "modalidad":  {
                                            "id":  3,
                                            "nombre":  "HÃ­brida"
                                        },
                          "tipoModalidad":  "HIBRIDA",
                          "docente":  {
                                          "id":  4,
                                          "nombre":  "Ãscar Villalta"
                                      },
                          "espacio":  {
                                          "id":  4,
                                          "nombre":  "Aula 4"
                                      },
                          "fechaInicio":  "2026-10-06",
                          "fechaFin":  "2026-12-15",
                          "horario":  "MarâJue Â· 6:00â8:00 pm",
                          "duracionHoras":  48,
                          "cupoMaximo":  18,
                          "cuposOcupados":  0,
                          "cuposDisponibles":  18,
                          "precio":  240.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  2,
                          "codigo":  "CUR-014",
                          "nombre":  "GestiÃ³n Ãgil de Proyectos",
                          "descripcion":  "Scrum, Kanban y gestiÃ³n de equipos en entornos de cambio permanente.",
                          "categoria":  {
                                            "id":  1,
                                            "nombre":  "GestiÃ³n de Proyectos"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  1,
                                          "nombre":  "Ana Beatriz Rivas"
                                      },
                          "espacio":  {
                                          "id":  3,
                                          "nombre":  "Aula 3"
                                      },
                          "fechaInicio":  "2026-09-14",
                          "fechaFin":  "2026-10-21",
                          "horario":  "LunâMiÃ© Â· 6:00â8:00 pm",
                          "duracionHoras":  32,
                          "cupoMaximo":  20,
                          "cuposOcupados":  2,
                          "cuposDisponibles":  18,
                          "precio":  185.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  4,
                          "codigo":  "CUR-030",
                          "nombre":  "IntroducciÃ³n a Power BI",
                          "descripcion":  "Modelado de datos, DAX bÃ¡sico y publicaciÃ³n de tableros.",
                          "categoria":  {
                                            "id":  3,
                                            "nombre":  "TecnologÃ­a"
                                        },
                          "modalidad":  {
                                            "id":  2,
                                            "nombre":  "Virtual"
                                        },
                          "tipoModalidad":  "VIRTUAL",
                          "docente":  {
                                          "id":  3,
                                          "nombre":  "Karla MenjÃ­var"
                                      },
                          "fechaInicio":  "2026-10-05",
                          "fechaFin":  "2026-11-09",
                          "horario":  "Lun Â· 6:00â9:00 pm",
                          "duracionHoras":  18,
                          "cupoMaximo":  20,
                          "cuposOcupados":  1,
                          "cuposDisponibles":  19,
                          "precio":  110.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  3,
                          "codigo":  "CUR-021",
                          "nombre":  "Excel Avanzado para Finanzas",
                          "descripcion":  "Tablas dinÃ¡micas, modelado financiero y automatizaciÃ³n con macros.",
                          "categoria":  {
                                            "id":  2,
                                            "nombre":  "Finanzas"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  3,
                                          "nombre":  "Karla MenjÃ­var"
                                      },
                          "espacio":  {
                                          "id":  6,
                                          "nombre":  "Laboratorio 1"
                                      },
                          "fechaInicio":  "2026-09-22",
                          "fechaFin":  "2026-10-29",
                          "horario":  "MarâJue Â· 4:00â6:00 pm",
                          "duracionHoras":  24,
                          "cupoMaximo":  20,
                          "cuposOcupados":  1,
                          "cuposDisponibles":  19,
                          "precio":  95.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  6,
                          "codigo":  "CUR-002",
                          "nombre":  "RedacciÃ³n de Informes TÃ©cnicos",
                          "descripcion":  "Estructura, claridad y presentaciÃ³n de informes institucionales.",
                          "categoria":  {
                                            "id":  4,
                                            "nombre":  "Desarrollo Humano"
                                        },
                          "modalidad":  {
                                            "id":  2,
                                            "nombre":  "Virtual"
                                        },
                          "tipoModalidad":  "VIRTUAL",
                          "docente":  {
                                          "id":  4,
                                          "nombre":  "Ãscar Villalta"
                                      },
                          "fechaInicio":  "2026-03-02",
                          "fechaFin":  "2026-04-10",
                          "horario":  "SÃ¡b Â· 8:00â11:00 am",
                          "duracionHoras":  18,
                          "cupoMaximo":  25,
                          "cuposOcupados":  0,
                          "cuposDisponibles":  25,
                          "precio":  75.00,
                          "estado":  "INACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  5,
    "totalElementos":  6,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 4.3 Filtro por estado

**Peticion**

```http
GET http://localhost:8080/api/v1/cursos?estado=ACTIVO
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  2,
                          "codigo":  "CUR-014",
                          "nombre":  "GestiÃ³n Ãgil de Proyectos",
                          "descripcion":  "Scrum, Kanban y gestiÃ³n de equipos en entornos de cambio permanente.",
                          "categoria":  {
                                            "id":  1,
                                            "nombre":  "GestiÃ³n de Proyectos"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  1,
                                          "nombre":  "Ana Beatriz Rivas"
                                      },
                          "espacio":  {
                                          "id":  3,
                                          "nombre":  "Aula 3"
                                      },
                          "fechaInicio":  "2026-09-14",
                          "fechaFin":  "2026-10-21",
                          "horario":  "LunâMiÃ© Â· 6:00â8:00 pm",
                          "duracionHoras":  32,
                          "cupoMaximo":  20,
                          "cuposOcupados":  2,
                          "cuposDisponibles":  18,
                          "precio":  185.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  3,
                          "codigo":  "CUR-021",
                          "nombre":  "Excel Avanzado para Finanzas",
                          "descripcion":  "Tablas dinÃ¡micas, modelado financiero y automatizaciÃ³n con macros.",
                          "categoria":  {
                                            "id":  2,
                                            "nombre":  "Finanzas"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  3,
                                          "nombre":  "Karla MenjÃ­var"
                                      },
                          "espacio":  {
                                          "id":  6,
                                          "nombre":  "Laboratorio 1"
                                      },
                          "fechaInicio":  "2026-09-22",
                          "fechaFin":  "2026-10-29",
                          "horario":  "MarâJue Â· 4:00â6:00 pm",
                          "duracionHoras":  24,
                          "cupoMaximo":  20,
                          "cuposOcupados":  1,
                          "cuposDisponibles":  19,
                          "precio":  95.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  1,
                          "codigo":  "CUR-009",
                          "nombre":  "AtenciÃ³n al Cliente de Alto Impacto",
                          "descripcion":  "TÃ©cnicas de servicio, manejo de objeciones y recuperaciÃ³n de clientes.",
                          "categoria":  {
                                            "id":  4,
                                            "nombre":  "Desarrollo Humano"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  5,
                                          "nombre":  "Silvia Arteaga"
                                      },
                          "espacio":  {
                                          "id":  9,
                                          "nombre":  "Sala Multimedia"
                                      },
                          "fechaInicio":  "2026-09-25",
                          "fechaFin":  "2026-10-23",
                          "horario":  "Vie Â· 2:00â6:00 pm",
                          "duracionHoras":  20,
                          "cupoMaximo":  20,
                          "cuposOcupados":  0,
                          "cuposDisponibles":  20,
                          "precio":  60.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  4,
                          "codigo":  "CUR-030",
                          "nombre":  "IntroducciÃ³n a Power BI",
                          "descripcion":  "Modelado de datos, DAX bÃ¡sico y publicaciÃ³n de tableros.",
                          "categoria":  {
                                            "id":  3,
                                            "nombre":  "TecnologÃ­a"
                                        },
                          "modalidad":  {
                                            "id":  2,
                                            "nombre":  "Virtual"
                                        },
                          "tipoModalidad":  "VIRTUAL",
                          "docente":  {
                                          "id":  3,
                                          "nombre":  "Karla MenjÃ­var"
                                      },
                          "fechaInicio":  "2026-10-05",
                          "fechaFin":  "2026-11-09",
                          "horario":  "Lun Â· 6:00â9:00 pm",
                          "duracionHoras":  18,
                          "cupoMaximo":  20,
                          "cuposOcupados":  1,
                          "cuposDisponibles":  19,
                          "precio":  110.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  5,
                          "codigo":  "CUR-033",
                          "nombre":  "InglÃ©s de Negocios Â· Nivel I",
                          "descripcion":  "ComunicaciÃ³n escrita y oral en contextos corporativos.",
                          "categoria":  {
                                            "id":  5,
                                            "nombre":  "Idiomas"
                                        },
                          "modalidad":  {
                                            "id":  3,
                                            "nombre":  "HÃ­brida"
                                        },
                          "tipoModalidad":  "HIBRIDA",
                          "docente":  {
                                          "id":  4,
                                          "nombre":  "Ãscar Villalta"
                                      },
                          "espacio":  {
                                          "id":  4,
                                          "nombre":  "Aula 4"
                                      },
                          "fechaInicio":  "2026-10-06",
                          "fechaFin":  "2026-12-15",
                          "horario":  "MarâJue Â· 6:00â8:00 pm",
                          "duracionHoras":  48,
                          "cupoMaximo":  18,
                          "cuposOcupados":  0,
                          "cuposDisponibles":  18,
                          "precio":  240.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  10,
    "totalElementos":  5,
    "totalPaginas":  1,
    "ultima":  true
}
```

### 4.4 Busqueda por texto libre (nombre o codigo)

**Peticion**

```http
GET http://localhost:8080/api/v1/cursos?texto=excel
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  3,
                          "codigo":  "CUR-021",
                          "nombre":  "Excel Avanzado para Finanzas",
                          "descripcion":  "Tablas dinÃ¡micas, modelado financiero y automatizaciÃ³n con macros.",
                          "categoria":  {
                                            "id":  2,
                                            "nombre":  "Finanzas"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  3,
                                          "nombre":  "Karla MenjÃ­var"
                                      },
                          "espacio":  {
                                          "id":  6,
                                          "nombre":  "Laboratorio 1"
                                      },
                          "fechaInicio":  "2026-09-22",
                          "fechaFin":  "2026-10-29",
                          "horario":  "MarâJue Â· 4:00â6:00 pm",
                          "duracionHoras":  24,
                          "cupoMaximo":  20,
                          "cuposOcupados":  1,
                          "cuposDisponibles":  19,
                          "precio":  95.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  10,
    "totalElementos":  1,
    "totalPaginas":  1,
    "ultima":  true
}
```

### 4.5 Filtros combinados

**Peticion**

```http
GET http://localhost:8080/api/v1/cursos?estado=ACTIVO&modalidadId=1&sort=fechaInicio,asc
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  2,
                          "codigo":  "CUR-014",
                          "nombre":  "GestiÃ³n Ãgil de Proyectos",
                          "descripcion":  "Scrum, Kanban y gestiÃ³n de equipos en entornos de cambio permanente.",
                          "categoria":  {
                                            "id":  1,
                                            "nombre":  "GestiÃ³n de Proyectos"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  1,
                                          "nombre":  "Ana Beatriz Rivas"
                                      },
                          "espacio":  {
                                          "id":  3,
                                          "nombre":  "Aula 3"
                                      },
                          "fechaInicio":  "2026-09-14",
                          "fechaFin":  "2026-10-21",
                          "horario":  "LunâMiÃ© Â· 6:00â8:00 pm",
                          "duracionHoras":  32,
                          "cupoMaximo":  20,
                          "cuposOcupados":  2,
                          "cuposDisponibles":  18,
                          "precio":  185.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  3,
                          "codigo":  "CUR-021",
                          "nombre":  "Excel Avanzado para Finanzas",
                          "descripcion":  "Tablas dinÃ¡micas, modelado financiero y automatizaciÃ³n con macros.",
                          "categoria":  {
                                            "id":  2,
                                            "nombre":  "Finanzas"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  3,
                                          "nombre":  "Karla MenjÃ­var"
                                      },
                          "espacio":  {
                                          "id":  6,
                                          "nombre":  "Laboratorio 1"
                                      },
                          "fechaInicio":  "2026-09-22",
                          "fechaFin":  "2026-10-29",
                          "horario":  "MarâJue Â· 4:00â6:00 pm",
                          "duracionHoras":  24,
                          "cupoMaximo":  20,
                          "cuposOcupados":  1,
                          "cuposDisponibles":  19,
                          "precio":  95.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  1,
                          "codigo":  "CUR-009",
                          "nombre":  "AtenciÃ³n al Cliente de Alto Impacto",
                          "descripcion":  "TÃ©cnicas de servicio, manejo de objeciones y recuperaciÃ³n de clientes.",
                          "categoria":  {
                                            "id":  4,
                                            "nombre":  "Desarrollo Humano"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  5,
                                          "nombre":  "Silvia Arteaga"
                                      },
                          "espacio":  {
                                          "id":  9,
                                          "nombre":  "Sala Multimedia"
                                      },
                          "fechaInicio":  "2026-09-25",
                          "fechaFin":  "2026-10-23",
                          "horario":  "Vie Â· 2:00â6:00 pm",
                          "duracionHoras":  20,
                          "cupoMaximo":  20,
                          "cuposOcupados":  0,
                          "cuposDisponibles":  20,
                          "precio":  60.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  10,
    "totalElementos":  3,
    "totalPaginas":  1,
    "ultima":  true
}
```

### 4.6 Filtro por tipo y capacidad minima

**Peticion**

```http
GET http://localhost:8080/api/v1/espacios?tipo=AUDITORIO&capacidadMinima=100
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  1,
                          "nombre":  "Auditorio Principal",
                          "tipo":  "AUDITORIO",
                          "capacidad":  180,
                          "precioPorHora":  45.00,
                          "equipamiento":  "Proyector 4K, sonido profesional, tarima, 2 micrÃ³fonos inalÃ¡mbricos",
                          "ubicacion":  "Edificio Central, planta baja",
                          "estado":  "DISPONIBLE"
                      }
                  ],
    "pagina":  0,
    "tamanio":  10,
    "totalElementos":  1,
    "totalPaginas":  1,
    "ultima":  true
}
```

### 4.7 Filtro por razon social de la empresa

Recorre la relacion cliente-empresa con LEFT JOIN, para no excluir a las personas particulares.

**Peticion**

```http
GET http://localhost:8080/api/v1/clientes?empresa=vitalab
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  1,
                          "tipo":  "EMPRESA",
                          "nombre":  "Laboratorios Vitalab",
                          "nit":  "0614-120589-102-3",
                          "email":  "contacto@vitalab.com.sv",
                          "telefono":  "2245-8800",
                          "direccion":  "Blvd. Los PrÃ³ceres, San Salvador",
                          "empresa":  {
                                          "id":  1,
                                          "razonSocial":  "Laboratorios Vitalab, S.A. de C.V.",
                                          "giro":  "Industria farmacÃ©utica",
                                          "contactoNombre":  "Marta Elena GÃ³mez",
                                          "contactoCargo":  "Jefa de Talento Humano"
                                      },
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  10,
    "totalElementos":  1,
    "totalPaginas":  1,
    "ultima":  true
}
```

### 4.8 Filtro por rango de fechas

**Peticion**

```http
GET http://localhost:8080/api/v1/inscripciones?desde=2026-09-01&hasta=2026-09-30
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  4,
                          "codigo":  "INS-0004",
                          "cliente":  {
                                          "id":  5,
                                          "nombre":  "JosÃ© Antonio MelÃ©ndez"
                                      },
                          "tipoOferta":  "CURSO",
                          "oferta":  {
                                         "id":  4,
                                         "nombre":  "IntroducciÃ³n a Power BI"
                                     },
                          "participanteNombre":  "JosÃ© Antonio MelÃ©ndez",
                          "participanteEmail":  "ja.melendez@correo.com",
                          "participanteTelefono":  "7845-2210",
                          "fechaInscripcion":  "2026-09-08",
                          "montoTotal":  110.00,
                          "estado":  "PENDIENTE",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  3,
                          "codigo":  "INS-0003",
                          "cliente":  {
                                          "id":  2,
                                          "nombre":  "Corporativo Alfa"
                                      },
                          "tipoOferta":  "CURSO",
                          "oferta":  {
                                         "id":  3,
                                         "nombre":  "Excel Avanzado para Finanzas"
                                     },
                          "participanteNombre":  "Luis MenÃ©ndez",
                          "participanteEmail":  "luis.menendez@alfa.com.sv",
                          "participanteTelefono":  "7033-5566",
                          "fechaInscripcion":  "2026-09-05",
                          "montoTotal":  95.00,
                          "estado":  "PENDIENTE",
                          "observaciones":  "Pendiente de confirmaciÃ³n de pago",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  1,
                          "codigo":  "INS-0001",
                          "cliente":  {
                                          "id":  1,
                                          "nombre":  "Laboratorios Vitalab"
                                      },
                          "tipoOferta":  "CURSO",
                          "oferta":  {
                                         "id":  2,
                                         "nombre":  "GestiÃ³n Ãgil de Proyectos"
                                     },
                          "participanteNombre":  "Marta Elena GÃ³mez",
                          "participanteEmail":  "marta.gomez@vitalab.com.sv",
                          "participanteTelefono":  "7890-1122",
                          "fechaInscripcion":  "2026-09-02",
                          "montoTotal":  185.00,
                          "estado":  "CONFIRMADA",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  2,
                          "codigo":  "INS-0002",
                          "cliente":  {
                                          "id":  1,
                                          "nombre":  "Laboratorios Vitalab"
                                      },
                          "tipoOferta":  "CURSO",
                          "oferta":  {
                                         "id":  2,
                                         "nombre":  "GestiÃ³n Ãgil de Proyectos"
                                     },
                          "participanteNombre":  "Ricardo Aguilar",
                          "participanteEmail":  "r.aguilar@vitalab.com.sv",
                          "participanteTelefono":  "7890-1133",
                          "fechaInscripcion":  "2026-09-02",
                          "montoTotal":  185.00,
                          "estado":  "CONFIRMADA",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  10,
    "totalElementos":  4,
    "totalPaginas":  1,
    "ultima":  true
}
```

### 4.9 Consulta de disponibilidad de un espacio

Devuelve si esta libre y que franjas estan ocupadas.

**Peticion**

```http
GET http://localhost:8080/api/v1/espacios/1/disponibilidad?desde=2026-09-15T08:00:00&hasta=2026-09-15T20:00:00
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "espacioId":  1,
    "espacio":  "Auditorio Principal",
    "desde":  "2026-09-15T08:00:00",
    "hasta":  "2026-09-15T20:00:00",
    "disponible":  false,
    "ocupaciones":  [
                        {
                            "origen":  "ALQUILER",
                            "referenciaId":  1,
                            "descripcion":  "Asamblea de accionistas",
                            "inicio":  "2026-09-15T17:00:00",
                            "fin":  "2026-09-15T18:30:00"
                        },
                        {
                            "origen":  "ALQUILER",
                            "referenciaId":  4,
                            "descripcion":  "Reserva contigua",
                            "inicio":  "2026-09-15T18:30:00",
                            "fin":  "2026-09-15T20:00:00"
                        }
                    ]
}
```


---

## 5. Funcionamiento de los modulos

Una consulta representativa por cada uno de los modulos desarrollados.

### 5.1 Modulo 1 - Gestion academica (cursos)

**Peticion**

```http
GET http://localhost:8080/api/v1/cursos?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  6,
                          "codigo":  "CUR-002",
                          "nombre":  "RedacciÃ³n de Informes TÃ©cnicos",
                          "descripcion":  "Estructura, claridad y presentaciÃ³n de informes institucionales.",
                          "categoria":  {
                                            "id":  4,
                                            "nombre":  "Desarrollo Humano"
                                        },
                          "modalidad":  {
                                            "id":  2,
                                            "nombre":  "Virtual"
                                        },
                          "tipoModalidad":  "VIRTUAL",
                          "docente":  {
                                          "id":  4,
                                          "nombre":  "Ãscar Villalta"
                                      },
                          "fechaInicio":  "2026-03-02",
                          "fechaFin":  "2026-04-10",
                          "horario":  "SÃ¡b Â· 8:00â11:00 am",
                          "duracionHoras":  18,
                          "cupoMaximo":  25,
                          "cuposOcupados":  0,
                          "cuposDisponibles":  25,
                          "precio":  75.00,
                          "estado":  "INACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  2,
                          "codigo":  "CUR-014",
                          "nombre":  "GestiÃ³n Ãgil de Proyectos",
                          "descripcion":  "Scrum, Kanban y gestiÃ³n de equipos en entornos de cambio permanente.",
                          "categoria":  {
                                            "id":  1,
                                            "nombre":  "GestiÃ³n de Proyectos"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  1,
                                          "nombre":  "Ana Beatriz Rivas"
                                      },
                          "espacio":  {
                                          "id":  3,
                                          "nombre":  "Aula 3"
                                      },
                          "fechaInicio":  "2026-09-14",
                          "fechaFin":  "2026-10-21",
                          "horario":  "LunâMiÃ© Â· 6:00â8:00 pm",
                          "duracionHoras":  32,
                          "cupoMaximo":  20,
                          "cuposOcupados":  2,
                          "cuposDisponibles":  18,
                          "precio":  185.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  3,
                          "codigo":  "CUR-021",
                          "nombre":  "Excel Avanzado para Finanzas",
                          "descripcion":  "Tablas dinÃ¡micas, modelado financiero y automatizaciÃ³n con macros.",
                          "categoria":  {
                                            "id":  2,
                                            "nombre":  "Finanzas"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  3,
                                          "nombre":  "Karla MenjÃ­var"
                                      },
                          "espacio":  {
                                          "id":  6,
                                          "nombre":  "Laboratorio 1"
                                      },
                          "fechaInicio":  "2026-09-22",
                          "fechaFin":  "2026-10-29",
                          "horario":  "MarâJue Â· 4:00â6:00 pm",
                          "duracionHoras":  24,
                          "cupoMaximo":  20,
                          "cuposOcupados":  1,
                          "cuposDisponibles":  19,
                          "precio":  95.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  6,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 5.2 Modulo 1 - Gestion academica (diplomados)

**Peticion**

```http
GET http://localhost:8080/api/v1/diplomados?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  1,
                          "codigo":  "DIP-006",
                          "nombre":  "Diplomado en Finanzas Corporativas",
                          "descripcion":  "ValuaciÃ³n, estructura de capital, presupuesto y anÃ¡lisis de inversiones.",
                          "categoria":  {
                                            "id":  2,
                                            "nombre":  "Finanzas"
                                        },
                          "modalidad":  {
                                            "id":  2,
                                            "nombre":  "Virtual"
                                        },
                          "tipoModalidad":  "VIRTUAL",
                          "docente":  {
                                          "id":  2,
                                          "nombre":  "Mario PeÃ±ate"
                                      },
                          "fechaInicio":  "2026-09-19",
                          "fechaFin":  "2027-02-27",
                          "horario":  "SÃ¡b Â· 8:00â12:00 pm",
                          "duracionHoras":  120,
                          "cupoMaximo":  30,
                          "cuposOcupados":  1,
                          "cuposDisponibles":  29,
                          "precio":  620.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  2,
                          "codigo":  "DIP-011",
                          "nombre":  "Diplomado en Liderazgo Ejecutivo",
                          "descripcion":  "DirecciÃ³n de equipos, comunicaciÃ³n estratÃ©gica y gestiÃ³n del cambio.",
                          "categoria":  {
                                            "id":  4,
                                            "nombre":  "Desarrollo Humano"
                                        },
                          "modalidad":  {
                                            "id":  1,
                                            "nombre":  "Presencial"
                                        },
                          "tipoModalidad":  "PRESENCIAL",
                          "docente":  {
                                          "id":  4,
                                          "nombre":  "Ãscar Villalta"
                                      },
                          "espacio":  {
                                          "id":  1,
                                          "nombre":  "Auditorio Principal"
                                      },
                          "fechaInicio":  "2026-10-03",
                          "fechaFin":  "2027-03-06",
                          "horario":  "SÃ¡b Â· 9:00â1:00 pm",
                          "duracionHoras":  110,
                          "cupoMaximo":  25,
                          "cuposOcupados":  1,
                          "cuposDisponibles":  24,
                          "precio":  540.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  3,
                          "codigo":  "DIP-014",
                          "nombre":  "Diplomado en AnalÃ­tica de Datos",
                          "descripcion":  "Del dato al tablero: SQL, visualizaciÃ³n y storytelling con datos.",
                          "categoria":  {
                                            "id":  3,
                                            "nombre":  "TecnologÃ­a"
                                        },
                          "modalidad":  {
                                            "id":  3,
                                            "nombre":  "HÃ­brida"
                                        },
                          "tipoModalidad":  "HIBRIDA",
                          "docente":  {
                                          "id":  3,
                                          "nombre":  "Karla MenjÃ­var"
                                      },
                          "espacio":  {
                                          "id":  6,
                                          "nombre":  "Laboratorio 1"
                                      },
                          "fechaInicio":  "2026-11-07",
                          "fechaFin":  "2027-04-24",
                          "horario":  "SÃ¡b Â· 8:00â12:00 pm",
                          "duracionHoras":  130,
                          "cupoMaximo":  20,
                          "cuposOcupados":  0,
                          "cuposDisponibles":  20,
                          "precio":  680.00,
                          "estado":  "ACTIVO",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  3,
    "totalPaginas":  1,
    "ultima":  true
}
```

### 5.3 Modulo 1 - Catalogos (docentes)

**Peticion**

```http
GET http://localhost:8080/api/v1/docentes?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  5,
                          "nombres":  "Silvia",
                          "apellidos":  "Arteaga",
                          "nombreCompleto":  "Silvia Arteaga",
                          "email":  "silvia.arteaga@cfc.uca.edu.sv",
                          "telefono":  "2210-6604",
                          "especialidad":  "Servicio al Cliente"
                      },
                      {
                          "id":  3,
                          "nombres":  "Karla",
                          "apellidos":  "MenjÃ­var",
                          "nombreCompleto":  "Karla MenjÃ­var",
                          "email":  "karla.menjivar@cfc.uca.edu.sv",
                          "telefono":  "2210-6602",
                          "especialidad":  "AnÃ¡lisis de Datos"
                      },
                      {
                          "id":  2,
                          "nombres":  "Mario",
                          "apellidos":  "PeÃ±ate",
                          "nombreCompleto":  "Mario PeÃ±ate",
                          "email":  "mario.penate@cfc.uca.edu.sv",
                          "telefono":  "2210-6601",
                          "especialidad":  "Finanzas Corporativas"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  5,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 5.4 Modulo 2 - Clientes

**Peticion**

```http
GET http://localhost:8080/api/v1/clientes?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  6,
                          "tipo":  "PERSONA_PARTICULAR",
                          "nombre":  "Claudia Regina Flores",
                          "dui":  "03987421-5",
                          "email":  "claudia.flores@correo.com",
                          "telefono":  "7712-9987",
                          "direccion":  "Mejicanos, San Salvador",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  2,
                          "tipo":  "EMPRESA",
                          "nombre":  "Corporativo Alfa",
                          "nit":  "0614-030794-101-8",
                          "email":  "administracion@alfa.com.sv",
                          "telefono":  "2260-1122",
                          "direccion":  "Col. EscalÃ³n, San Salvador",
                          "empresa":  {
                                          "id":  2,
                                          "razonSocial":  "Corporativo Alfa, S.A.",
                                          "giro":  "Servicios corporativos",
                                          "contactoNombre":  "Luis MenÃ©ndez",
                                          "contactoCargo":  "Gerente Administrativo"
                                      },
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  3,
                          "tipo":  "EMPRESA",
                          "nombre":  "FundaciÃ³n Horizonte",
                          "nit":  "0614-250603-103-1",
                          "email":  "proyectos@horizonte.org.sv",
                          "telefono":  "2298-4477",
                          "direccion":  "Santa Tecla, La Libertad",
                          "empresa":  {
                                          "id":  3,
                                          "razonSocial":  "FundaciÃ³n Horizonte",
                                          "giro":  "OrganizaciÃ³n sin fines de lucro",
                                          "contactoNombre":  "Delmy Portillo",
                                          "contactoCargo":  "Coordinadora de Proyectos"
                                      },
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  6,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 5.5 Modulo 2 - Historial consolidado

Inscripciones, cotizaciones y pagos del cliente, con totales.

**Peticion**

```http
GET http://localhost:8080/api/v1/clientes/1/historial
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "clienteId":  1,
    "cliente":  "Laboratorios Vitalab",
    "resumen":  {
                    "totalInscripciones":  2,
                    "totalCotizaciones":  1,
                    "totalPagos":  1,
                    "montoPagado":  185.00
                },
    "inscripciones":  [
                          {
                              "id":  1,
                              "codigo":  "INS-0001",
                              "cliente":  {
                                              "id":  1,
                                              "nombre":  "Laboratorios Vitalab"
                                          },
                              "tipoOferta":  "CURSO",
                              "oferta":  {
                                             "id":  2,
                                             "nombre":  "GestiÃ³n Ãgil de Proyectos"
                                         },
                              "participanteNombre":  "Marta Elena GÃ³mez",
                              "participanteEmail":  "marta.gomez@vitalab.com.sv",
                              "participanteTelefono":  "7890-1122",
                              "fechaInscripcion":  "2026-09-02",
                              "montoTotal":  185.00,
                              "estado":  "CONFIRMADA",
                              "createdAt":  "2026-09-24T21:04:25.28431",
                              "updatedAt":  "2026-09-24T21:04:25.28431"
                          },
                          {
                              "id":  2,
                              "codigo":  "INS-0002",
                              "cliente":  {
                                              "id":  1,
                                              "nombre":  "Laboratorios Vitalab"
                                          },
                              "tipoOferta":  "CURSO",
                              "oferta":  {
                                             "id":  2,
                                             "nombre":  "GestiÃ³n Ãgil de Proyectos"
                                         },
                              "participanteNombre":  "Ricardo Aguilar",
                              "participanteEmail":  "r.aguilar@vitalab.com.sv",
                              "participanteTelefono":  "7890-1133",
                              "fechaInscripcion":  "2026-09-02",
                              "montoTotal":  185.00,
                              "estado":  "CONFIRMADA",
                              "createdAt":  "2026-09-24T21:04:25.28431",
                              "updatedAt":  "2026-09-24T21:04:25.28431"
                          }
                      ],
    "cotizaciones":  [
                         {
                             "id":  2,
                             "codigo":  "COT-0002",
                             "cliente":  {
                                             "id":  1,
                                             "nombre":  "Laboratorios Vitalab"
                                         },
                             "tipo":  "DIPLOMADO",
                             "estado":  "APROBADA",
                             "asunto":  "Diplomado en Finanzas para cinco colaboradores",
                             "fechaSolicitud":  "2026-08-02",
                             "fechaVencimiento":  "2026-09-02",
                             "total":  3100.00,
                             "responsableRevision":  "Ing. Yesenia Escobar â AdministraciÃ³n",
                             "items":  [
                                           {
                                               "id":  4,
                                               "tipo":  "DIPLOMADO",
                                               "concepto":  "Diplomado en Finanzas Corporativas",
                                               "detalle":  "DIP-006 Â· 5 participantes",
                                               "referenciaId":  1,
                                               "cantidad":  5,
                                               "precioUnitario":  620.00,
                                               "subtotal":  3100.00
                                           }
                                       ],
                             "createdAt":  "2026-09-24T21:04:25.28431",
                             "updatedAt":  "2026-09-24T21:04:25.28431"
                         }
                     ],
    "pagos":  [
                  {
                      "id":  1,
                      "codigo":  "PAG-0001",
                      "cliente":  {
                                      "id":  1,
                                      "nombre":  "Laboratorios Vitalab"
                                  },
                      "concepto":  "INSCRIPCION",
                      "referenciaId":  1,
                      "montoEsperado":  185.00,
                      "montoPagado":  185.00,
                      "saldo":  0.00,
                      "metodo":  "TARJETA",
                      "estado":  "PAGADO",
                      "fechaPago":  "2026-09-02",
                      "referenciaBancaria":  "AUT-884512",
                      "fechaValidacion":  "2026-09-02T10:30:00",
                      "validadoPor":  "contabilidad@cfc.uca.edu.sv",
                      "fechaConfirmacion":  "2026-09-02T11:00:00",
                      "confirmadoPor":  "recepcion@cfc.uca.edu.sv",
                      "tieneComprobante":  true,
                      "createdAt":  "2026-09-24T21:04:25.28431",
                      "updatedAt":  "2026-09-24T21:04:25.28431"
                  }
              ]
}
```

### 5.6 Modulo 3 - Inscripciones

**Peticion**

```http
GET http://localhost:8080/api/v1/inscripciones?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  4,
                          "codigo":  "INS-0004",
                          "cliente":  {
                                          "id":  5,
                                          "nombre":  "JosÃ© Antonio MelÃ©ndez"
                                      },
                          "tipoOferta":  "CURSO",
                          "oferta":  {
                                         "id":  4,
                                         "nombre":  "IntroducciÃ³n a Power BI"
                                     },
                          "participanteNombre":  "JosÃ© Antonio MelÃ©ndez",
                          "participanteEmail":  "ja.melendez@correo.com",
                          "participanteTelefono":  "7845-2210",
                          "fechaInscripcion":  "2026-09-08",
                          "montoTotal":  110.00,
                          "estado":  "PENDIENTE",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  3,
                          "codigo":  "INS-0003",
                          "cliente":  {
                                          "id":  2,
                                          "nombre":  "Corporativo Alfa"
                                      },
                          "tipoOferta":  "CURSO",
                          "oferta":  {
                                         "id":  3,
                                         "nombre":  "Excel Avanzado para Finanzas"
                                     },
                          "participanteNombre":  "Luis MenÃ©ndez",
                          "participanteEmail":  "luis.menendez@alfa.com.sv",
                          "participanteTelefono":  "7033-5566",
                          "fechaInscripcion":  "2026-09-05",
                          "montoTotal":  95.00,
                          "estado":  "PENDIENTE",
                          "observaciones":  "Pendiente de confirmaciÃ³n de pago",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  1,
                          "codigo":  "INS-0001",
                          "cliente":  {
                                          "id":  1,
                                          "nombre":  "Laboratorios Vitalab"
                                      },
                          "tipoOferta":  "CURSO",
                          "oferta":  {
                                         "id":  2,
                                         "nombre":  "GestiÃ³n Ãgil de Proyectos"
                                     },
                          "participanteNombre":  "Marta Elena GÃ³mez",
                          "participanteEmail":  "marta.gomez@vitalab.com.sv",
                          "participanteTelefono":  "7890-1122",
                          "fechaInscripcion":  "2026-09-02",
                          "montoTotal":  185.00,
                          "estado":  "CONFIRMADA",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  6,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 5.7 Modulo 4 - Cotizaciones

El tipo se deduce de los items: con varios tipos, COMBINADA.

**Peticion**

```http
GET http://localhost:8080/api/v1/cotizaciones?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  1,
                          "codigo":  "COT-0001",
                          "cliente":  {
                                          "id":  2,
                                          "nombre":  "Corporativo Alfa"
                                      },
                          "tipo":  "COMBINADA",
                          "estado":  "GENERADA",
                          "asunto":  "Evento institucional de cierre de aÃ±o",
                          "fechaSolicitud":  "2026-08-06",
                          "fechaVencimiento":  "2026-09-06",
                          "total":  5017.50,
                          "notaInterna":  "Verificar disponibilidad del auditorio antes de aprobar.",
                          "responsableRevision":  "Ing. Yesenia Escobar â AdministraciÃ³n",
                          "items":  [
                                        {
                                            "id":  1,
                                            "tipo":  "CURSO",
                                            "concepto":  "Curso empresarial",
                                            "detalle":  "CUR-014 Â· 25 participantes",
                                            "referenciaId":  2,
                                            "cantidad":  25,
                                            "precioUnitario":  185.00,
                                            "subtotal":  4625.00
                                        },
                                        {
                                            "id":  2,
                                            "tipo":  "ESPACIO",
                                            "concepto":  "Auditorio principal",
                                            "detalle":  "Medio dÃ­a (4 horas)",
                                            "referenciaId":  1,
                                            "cantidad":  4,
                                            "precioUnitario":  45.00,
                                            "subtotal":  180.00
                                        },
                                        {
                                            "id":  3,
                                            "tipo":  "CATERING",
                                            "concepto":  "Catering â coffee break",
                                            "detalle":  "25 personas",
                                            "referenciaId":  2,
                                            "cantidad":  25,
                                            "precioUnitario":  8.50,
                                            "subtotal":  212.50
                                        }
                                    ],
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  2,
                          "codigo":  "COT-0002",
                          "cliente":  {
                                          "id":  1,
                                          "nombre":  "Laboratorios Vitalab"
                                      },
                          "tipo":  "DIPLOMADO",
                          "estado":  "APROBADA",
                          "asunto":  "Diplomado en Finanzas para cinco colaboradores",
                          "fechaSolicitud":  "2026-08-02",
                          "fechaVencimiento":  "2026-09-02",
                          "total":  3100.00,
                          "responsableRevision":  "Ing. Yesenia Escobar â AdministraciÃ³n",
                          "items":  [
                                        {
                                            "id":  4,
                                            "tipo":  "DIPLOMADO",
                                            "concepto":  "Diplomado en Finanzas Corporativas",
                                            "detalle":  "DIP-006 Â· 5 participantes",
                                            "referenciaId":  1,
                                            "cantidad":  5,
                                            "precioUnitario":  620.00,
                                            "subtotal":  3100.00
                                        }
                                    ],
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  3,
                          "codigo":  "COT-0003",
                          "cliente":  {
                                          "id":  3,
                                          "nombre":  "FundaciÃ³n Horizonte"
                                      },
                          "tipo":  "COMBINADA",
                          "estado":  "RECHAZADA",
                          "asunto":  "Jornada de formaciÃ³n comunitaria",
                          "fechaSolicitud":  "2026-07-28",
                          "fechaVencimiento":  "2026-08-28",
                          "total":  740.00,
                          "notaInterna":  "La fecha solicitada coincide con un evento institucional.",
                          "responsableRevision":  "Ing. Yesenia Escobar â AdministraciÃ³n",
                          "items":  [
                                        {
                                            "id":  5,
                                            "tipo":  "ESPACIO",
                                            "concepto":  "Auditorio Norte",
                                            "detalle":  "Jornada completa (8 horas)",
                                            "referenciaId":  2,
                                            "cantidad":  8,
                                            "precioUnitario":  30.00,
                                            "subtotal":  240.00
                                        },
                                        {
                                            "id":  6,
                                            "tipo":  "CATERING",
                                            "concepto":  "Refrigerio para asistentes",
                                            "detalle":  "100 personas",
                                            "referenciaId":  6,
                                            "cantidad":  100,
                                            "precioUnitario":  5.00,
                                            "subtotal":  500.00
                                        }
                                    ],
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  3,
    "totalPaginas":  1,
    "ultima":  true
}
```

### 5.8 Modulo 5 - Espacios

**Peticion**

```http
GET http://localhost:8080/api/v1/espacios?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  2,
                          "nombre":  "Auditorio Norte",
                          "tipo":  "AUDITORIO",
                          "capacidad":  90,
                          "precioPorHora":  30.00,
                          "equipamiento":  "Proyector, sonido, atril",
                          "ubicacion":  "Edificio Norte, primer nivel",
                          "estado":  "DISPONIBLE"
                      },
                      {
                          "id":  1,
                          "nombre":  "Auditorio Principal",
                          "tipo":  "AUDITORIO",
                          "capacidad":  180,
                          "precioPorHora":  45.00,
                          "equipamiento":  "Proyector 4K, sonido profesional, tarima, 2 micrÃ³fonos inalÃ¡mbricos",
                          "ubicacion":  "Edificio Central, planta baja",
                          "estado":  "DISPONIBLE"
                      },
                      {
                          "id":  3,
                          "nombre":  "Aula 3",
                          "tipo":  "AULA",
                          "capacidad":  25,
                          "precioPorHora":  8.00,
                          "equipamiento":  "Proyector, pizarra acrÃ­lica, aire acondicionado",
                          "ubicacion":  "Edificio Central, segundo nivel",
                          "estado":  "DISPONIBLE"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  9,
    "totalPaginas":  3,
    "ultima":  false
}
```

### 5.9 Modulo 5 - Alquileres

**Peticion**

```http
GET http://localhost:8080/api/v1/alquileres?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  3,
                          "codigo":  "ALQ-0003",
                          "cliente":  {
                                          "id":  4,
                                          "nombre":  "Grupo Sarti"
                                      },
                          "espacio":  {
                                          "id":  8,
                                          "nombre":  "Sala de Juntas"
                                      },
                          "fechaHoraInicio":  "2026-09-22T09:00:00",
                          "fechaHoraFin":  "2026-09-22T12:00:00",
                          "horas":  3.0,
                          "proposito":  "ReuniÃ³n de planificaciÃ³n trimestral",
                          "cantidadAsistentes":  10,
                          "montoTotal":  36.00,
                          "estado":  "PENDIENTE",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  2,
                          "codigo":  "ALQ-0002",
                          "cliente":  {
                                          "id":  3,
                                          "nombre":  "FundaciÃ³n Horizonte"
                                      },
                          "espacio":  {
                                          "id":  9,
                                          "nombre":  "Sala Multimedia"
                                      },
                          "fechaHoraInicio":  "2026-09-17T13:00:00",
                          "fechaHoraFin":  "2026-09-17T15:00:00",
                          "horas":  2.0,
                          "proposito":  "Feria de egresados",
                          "cantidadAsistentes":  35,
                          "montoTotal":  40.00,
                          "estado":  "CONFIRMADA",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  4,
                          "codigo":  "ALQ-0004",
                          "cliente":  {
                                          "id":  2,
                                          "nombre":  "Corporativo Alfa"
                                      },
                          "espacio":  {
                                          "id":  1,
                                          "nombre":  "Auditorio Principal"
                                      },
                          "fechaHoraInicio":  "2026-09-15T18:30:00",
                          "fechaHoraFin":  "2026-09-15T20:00:00",
                          "horas":  1.5,
                          "proposito":  "Reserva contigua",
                          "cantidadAsistentes":  10,
                          "montoTotal":  67.50,
                          "estado":  "PENDIENTE",
                          "createdAt":  "2026-09-24T21:05:10.298095",
                          "updatedAt":  "2026-09-24T21:05:10.298095"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  4,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 5.10 Modulo 6 - Servicios de catering

**Peticion**

```http
GET http://localhost:8080/api/v1/servicios-catering?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  4,
                          "codigo":  "CAT-0004",
                          "nombre":  "Almuerzo ejecutivo",
                          "tipo":  "ALMUERZO",
                          "descripcion":  "Entrada, plato fuerte, postre y bebida",
                          "precioPorPersona":  14.00,
                          "minimoPersonas":  15,
                          "activo":  true
                      },
                      {
                          "id":  5,
                          "codigo":  "CAT-0005",
                          "nombre":  "Cena institucional",
                          "tipo":  "CENA",
                          "descripcion":  "MenÃº de tres tiempos con servicio de mesa",
                          "precioPorPersona":  18.50,
                          "minimoPersonas":  20,
                          "activo":  true
                      },
                      {
                          "id":  2,
                          "codigo":  "CAT-0002",
                          "nombre":  "Coffee break ejecutivo",
                          "tipo":  "COFFEE_BREAK",
                          "descripcion":  "CafÃ© de especialidad, jugos, fruta y reposterÃ­a fina",
                          "precioPorPersona":  8.50,
                          "minimoPersonas":  10,
                          "activo":  true
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  6,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 5.11 Modulo 6 - Solicitudes de catering

**Peticion**

```http
GET http://localhost:8080/api/v1/solicitudes-catering?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  2,
                          "codigo":  "SCA-0002",
                          "cliente":  {
                                          "id":  4,
                                          "nombre":  "Grupo Sarti"
                                      },
                          "servicio":  {
                                           "id":  4,
                                           "nombre":  "Almuerzo ejecutivo"
                                       },
                          "espacio":  {
                                          "id":  8,
                                          "nombre":  "Sala de Juntas"
                                      },
                          "lugar":  "Sala de Juntas â Edificio Central",
                          "cantidadAsistentes":  12,
                          "menu":  "Crema de ayote, pollo en salsa de hierbas, flan de vainilla",
                          "fecha":  "2026-09-22",
                          "horaInicio":  "12:00:00",
                          "horaFin":  "13:30:00",
                          "montoTotal":  168.00,
                          "estado":  "PENDIENTE",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  1,
                          "codigo":  "SCA-0001",
                          "cliente":  {
                                          "id":  3,
                                          "nombre":  "FundaciÃ³n Horizonte"
                                      },
                          "servicio":  {
                                           "id":  2,
                                           "nombre":  "Coffee break ejecutivo"
                                       },
                          "espacio":  {
                                          "id":  9,
                                          "nombre":  "Sala Multimedia"
                                      },
                          "lugar":  "Sala Multimedia â Edificio Norte",
                          "cantidadAsistentes":  30,
                          "menu":  "CafÃ© de especialidad, jugos naturales, fruta de temporada y reposterÃ­a",
                          "fecha":  "2026-09-17",
                          "horaInicio":  "11:30:00",
                          "horaFin":  "12:15:00",
                          "montoTotal":  255.00,
                          "estado":  "CONFIRMADA",
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  2,
    "totalPaginas":  1,
    "ultima":  true
}
```

### 5.12 Modulo 7 - Agenda institucional

**Peticion**

```http
GET http://localhost:8080/api/v1/agenda?size=5
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  1,
                          "titulo":  "GestiÃ³n Ãgil de Proyectos Â· Aula 3",
                          "tipo":  "CURSO",
                          "espacio":  {
                                          "id":  3,
                                          "nombre":  "Aula 3"
                                      },
                          "fechaHoraInicio":  "2026-09-14T18:00:00",
                          "fechaHoraFin":  "2026-09-14T20:00:00",
                          "descripcion":  "Primera sesiÃ³n del curso CUR-014",
                          "responsable":  "Ana Beatriz Rivas",
                          "editable":  true,
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  4,
                          "titulo":  "Asamblea de accionistas Â· Corporativo Alfa",
                          "tipo":  "ALQUILER",
                          "espacio":  {
                                          "id":  1,
                                          "nombre":  "Auditorio Principal"
                                      },
                          "fechaHoraInicio":  "2026-09-15T17:00:00",
                          "fechaHoraFin":  "2026-09-15T18:30:00",
                          "descripcion":  "Alquiler del Auditorio Principal",
                          "responsable":  "RecepciÃ³n CFC",
                          "origenId":  1,
                          "editable":  false,
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  8,
                          "titulo":  "Reserva contigua Â· Corporativo Alfa",
                          "tipo":  "ALQUILER",
                          "espacio":  {
                                          "id":  1,
                                          "nombre":  "Auditorio Principal"
                                      },
                          "fechaHoraInicio":  "2026-09-15T18:30:00",
                          "fechaHoraFin":  "2026-09-15T20:00:00",
                          "descripcion":  "Alquiler de Auditorio Principal",
                          "responsable":  "Corporativo Alfa",
                          "origenId":  4,
                          "editable":  false,
                          "createdAt":  "2026-09-24T21:05:10.378116",
                          "updatedAt":  "2026-09-24T21:05:10.378116"
                      },
                      {
                          "id":  6,
                          "titulo":  "Coffee break Â· FundaciÃ³n Horizonte",
                          "tipo":  "CATERING",
                          "espacio":  {
                                          "id":  9,
                                          "nombre":  "Sala Multimedia"
                                      },
                          "fechaHoraInicio":  "2026-09-17T11:30:00",
                          "fechaHoraFin":  "2026-09-17T12:15:00",
                          "descripcion":  "Servicio de coffee break ejecutivo para 30 personas",
                          "responsable":  "RecepciÃ³n CFC",
                          "origenId":  1,
                          "editable":  false,
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  5,
                          "titulo":  "Feria de egresados Â· FundaciÃ³n Horizonte",
                          "tipo":  "ALQUILER",
                          "espacio":  {
                                          "id":  9,
                                          "nombre":  "Sala Multimedia"
                                      },
                          "fechaHoraInicio":  "2026-09-17T13:00:00",
                          "fechaHoraFin":  "2026-09-17T15:00:00",
                          "descripcion":  "Alquiler de la Sala Multimedia",
                          "responsable":  "RecepciÃ³n CFC",
                          "origenId":  2,
                          "editable":  false,
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  5,
    "totalElementos":  8,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 5.13 Modulo 7 - Deteccion de conflictos

Vacio significa que no hay choques: el sistema los impide al crear.

**Peticion**

```http
GET http://localhost:8080/api/v1/agenda/conflictos
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "value":  [

              ],
    "Count":  0
}
```

### 5.14 Modulo 8 - Pagos

**Peticion**

```http
GET http://localhost:8080/api/v1/pagos?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  3,
                          "codigo":  "PAG-0003",
                          "cliente":  {
                                          "id":  3,
                                          "nombre":  "FundaciÃ³n Horizonte"
                                      },
                          "concepto":  "ALQUILER",
                          "referenciaId":  2,
                          "montoEsperado":  40.00,
                          "montoPagado":  0.00,
                          "saldo":  40.00,
                          "metodo":  "DEPOSITO",
                          "estado":  "PENDIENTE",
                          "fechaPago":  "2026-09-09",
                          "tieneComprobante":  false,
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  4,
                          "codigo":  "PAG-0004",
                          "cliente":  {
                                          "id":  3,
                                          "nombre":  "FundaciÃ³n Horizonte"
                                      },
                          "concepto":  "CATERING",
                          "referenciaId":  1,
                          "montoEsperado":  255.00,
                          "montoPagado":  255.00,
                          "saldo":  0.00,
                          "metodo":  "EFECTIVO",
                          "estado":  "PAGADO",
                          "fechaPago":  "2026-09-07",
                          "fechaValidacion":  "2026-09-07T09:15:00",
                          "validadoPor":  "contabilidad@cfc.uca.edu.sv",
                          "fechaConfirmacion":  "2026-09-07T09:40:00",
                          "confirmadoPor":  "recepcion@cfc.uca.edu.sv",
                          "tieneComprobante":  true,
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  2,
                          "codigo":  "PAG-0002",
                          "cliente":  {
                                          "id":  2,
                                          "nombre":  "Corporativo Alfa"
                                      },
                          "concepto":  "COTIZACION",
                          "referenciaId":  1,
                          "montoEsperado":  5017.50,
                          "montoPagado":  2500.00,
                          "saldo":  2517.50,
                          "metodo":  "TRANSFERENCIA",
                          "estado":  "PARCIAL",
                          "fechaPago":  "2026-09-04",
                          "referenciaBancaria":  "TRF-2026-0910",
                          "fechaValidacion":  "2026-09-04T15:20:00",
                          "validadoPor":  "contabilidad@cfc.uca.edu.sv",
                          "tieneComprobante":  false,
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  5,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 5.15 Modulo 8 - Reporte de estados financieros

**Peticion**

```http
GET http://localhost:8080/api/v1/pagos/reportes/estados-financieros
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "totalCobrado":  3250.00,
    "totalPorCobrar":  2867.50,
    "cantidadPagos":  5,
    "porEstado":  [
                      {
                          "estado":  "PAGADO",
                          "cantidad":  2,
                          "montoEsperado":  440.00,
                          "montoPagado":  440.00,
                          "saldo":  0.00
                      },
                      {
                          "estado":  "PENDIENTE",
                          "cantidad":  1,
                          "montoEsperado":  40.00,
                          "montoPagado":  0.00,
                          "saldo":  40.00
                      },
                      {
                          "estado":  "PARCIAL",
                          "cantidad":  2,
                          "montoEsperado":  5637.50,
                          "montoPagado":  2810.00,
                          "saldo":  2827.50
                      }
                  ],
    "porMetodo":  [
                      {
                          "metodo":  "DEPOSITO",
                          "cantidad":  1,
                          "montoPagado":  0.00
                      },
                      {
                          "metodo":  "TRANSFERENCIA",
                          "cantidad":  2,
                          "montoPagado":  2810.00
                      },
                      {
                          "metodo":  "EFECTIVO",
                          "cantidad":  1,
                          "montoPagado":  255.00
                      },
                      {
                          "metodo":  "TARJETA",
                          "cantidad":  1,
                          "montoPagado":  185.00
                      }
                  ]
}
```

### 5.16 Modulo 9 - Usuarios

La respuesta nunca incluye la contrasena ni su hash.

**Peticion**

```http
GET http://localhost:8080/api/v1/usuarios?size=3
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "contenido":  [
                      {
                          "id":  3,
                          "nombre":  "Carlos Tobar",
                          "email":  "contabilidad@cfc.uca.edu.sv",
                          "estado":  "ACTIVO",
                          "rol":  {
                                      "id":  4,
                                      "nombre":  "CONTABILIDAD"
                                  },
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  4,
                          "nombre":  "Marta Elena GÃ³mez",
                          "email":  "marta.gomez@vitalab.com.sv",
                          "estado":  "ACTIVO",
                          "rol":  {
                                      "id":  3,
                                      "nombre":  "CLIENTE"
                                  },
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      },
                      {
                          "id":  2,
                          "nombre":  "RocÃ­o Cortez",
                          "email":  "recepcion@cfc.uca.edu.sv",
                          "estado":  "ACTIVO",
                          "rol":  {
                                      "id":  2,
                                      "nombre":  "RECEPCIONISTA"
                                  },
                          "createdAt":  "2026-09-24T21:04:25.28431",
                          "updatedAt":  "2026-09-24T21:04:25.28431"
                      }
                  ],
    "pagina":  0,
    "tamanio":  3,
    "totalElementos":  4,
    "totalPaginas":  2,
    "ultima":  false
}
```

### 5.17 Modulo 9 - Roles

**Peticion**

```http
GET http://localhost:8080/api/v1/roles
```

**Respuesta** - HTTP 200 (esperado 200) - OK

```json
{
    "value":  [
                  {
                      "id":  1,
                      "nombre":  "ADMIN",
                      "descripcion":  "Acceso administrativo completo al sistema",
                      "rolDelSistema":  true
                  },
                  {
                      "id":  2,
                      "nombre":  "RECEPCIONISTA",
                      "descripcion":  "AtenciÃ³n al cliente, inscripciones, reservas y confirmaciones",
                      "rolDelSistema":  true
                  },
                  {
                      "id":  3,
                      "nombre":  "CLIENTE",
                      "descripcion":  "Consulta y solicita servicios del centro",
                      "rolDelSistema":  true
                  },
                  {
                      "id":  4,
                      "nombre":  "CONTABILIDAD",
                      "descripcion":  "ValidaciÃ³n de pagos e informaciÃ³n financiera",
                      "rolDelSistema":  true
                  }
              ],
    "Count":  4
}
```


---

## Resumen

- Casos ejecutados: **46**
- Con el codigo HTTP esperado: **46**
- Marcados para revisar: **0**

Los casos marcados como REVISAR suelen deberse a que la base de datos ya no
tiene los datos iniciales tal como los carga Flyway (por ejemplo, porque una
prueba manual anterior creo o borro registros). Al recrear la base y volver a
ejecutar el script, deberian quedar todos en OK.

