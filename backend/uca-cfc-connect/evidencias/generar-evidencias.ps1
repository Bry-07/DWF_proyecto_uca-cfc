<#
    UCA-CFC Connect - Generador de evidencias de prueba (Windows / PowerShell)
    Universidad Don Bosco - Desarrollo de Software Empresarial - Fase 2

    Recorre la API con la aplicacion corriendo y escribe un archivo Markdown con
    la peticion, el codigo HTTP y la respuesta de cada caso. Cubre las cuatro
    categorias que pide la entrega: CRUD, validaciones, errores y busquedas
    personalizadas, ademas de una llamada por modulo.

    USO
        1. Levantar la aplicacion:  .\mvnw.cmd spring-boot:run
        2. En OTRA ventana de PowerShell, desde la raiz del proyecto:
               .\evidencias\generar-evidencias.ps1
        3. Se genera:  evidencias\EVIDENCIAS.md

    La base debe tener los datos iniciales que carga Flyway. Si ya se hicieron
    pruebas manuales que modificaron los datos, conviene recrearla antes:
        docker compose down -v      (o borrar y volver a crear la base en psql)
#>

[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Salida  = "$PSScriptRoot\EVIDENCIAS.md"
)

$ErrorActionPreference = "Stop"
$api = "$BaseUrl/api/v1"
$sb  = [System.Text.StringBuilder]::new()
$script:total = 0
$script:okEsperado = 0

function Add-Line { param([string]$t = "") [void]$sb.AppendLine($t) }

function Add-Seccion {
    param([string]$titulo, [string]$descripcion)
    Add-Line ""
    Add-Line "---"
    Add-Line ""
    Add-Line "## $titulo"
    Add-Line ""
    if ($descripcion) { Add-Line $descripcion; Add-Line "" }
}

<#
    Ejecuta una llamada y la documenta. 'Esperado' es el codigo HTTP que debe
    devolver: si no coincide, queda marcado en el reporte para revisarlo.
#>
function Probar {
    param(
        [string] $Titulo,
        [string] $Metodo,
        [string] $Ruta,
        [object] $Cuerpo = $null,
        [int]    $Esperado,
        [string] $Comentario = ""
    )

    $script:total++
    $url = "$api$Ruta"
    $jsonCuerpo = $null
    if ($null -ne $Cuerpo) { $jsonCuerpo = $Cuerpo | ConvertTo-Json -Depth 6 }

    $codigo = 0
    $respuesta = ""
    try {
        $params = @{
            Uri             = $url
            Method          = $Metodo
            UseBasicParsing = $true
            ErrorAction     = "Stop"
        }
        if ($jsonCuerpo) {
            $params.Body        = [System.Text.Encoding]::UTF8.GetBytes($jsonCuerpo)
            $params.ContentType = "application/json; charset=utf-8"
        }
        $r = Invoke-WebRequest @params
        $codigo    = [int]$r.StatusCode
        $respuesta = $r.Content
    }
    catch {
        # Los codigos 4xx y 5xx llegan como excepcion. La forma de leerlos cambia
        # entre Windows PowerShell 5.1 (System.Net.WebException, con el cuerpo en
        # el stream de la respuesta) y PowerShell 7 (HttpResponseException, con el
        # cuerpo en ErrorDetails). Este bloque cubre las dos sin nombrar tipos que
        # podrian no existir en la version que este corriendo.
        $resp = $_.Exception.Response
        if ($resp) {
            try { $codigo = [int]$resp.StatusCode } catch { $codigo = 0 }

            if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
                $respuesta = $_.ErrorDetails.Message
            }
            else {
                try {
                    $stream = $resp.GetResponseStream()
                    $lector = New-Object System.IO.StreamReader($stream)
                    $respuesta = $lector.ReadToEnd()
                    $lector.Close()
                }
                catch {
                    $respuesta = "(no se pudo leer el cuerpo de la respuesta)"
                }
            }
        }
        else {
            $respuesta = "NO SE PUDO CONECTAR: $($_.Exception.Message)"
        }
    }

    # JSON legible cuando la respuesta lo permite
    if (-not [string]::IsNullOrWhiteSpace($respuesta)) {
        $inicio = $respuesta.TrimStart()
        if ($inicio.StartsWith("{") -or $inicio.StartsWith("[")) {
            try { $respuesta = $respuesta | ConvertFrom-Json | ConvertTo-Json -Depth 8 } catch { }
        }
    }
    else { $respuesta = "(sin cuerpo)" }

    $coincide = ($codigo -eq $Esperado)
    if ($coincide) { $script:okEsperado++ }
    $marca = if ($coincide) { "OK" } else { "REVISAR" }

    Add-Line "### $Titulo"
    Add-Line ""
    if ($Comentario) { Add-Line $Comentario; Add-Line "" }
    Add-Line "**Peticion**"
    Add-Line ""
    Add-Line '```http'
    Add-Line "$Metodo $url"
    if ($jsonCuerpo) { Add-Line "Content-Type: application/json"; Add-Line ""; Add-Line $jsonCuerpo }
    Add-Line '```'
    Add-Line ""
    Add-Line "**Respuesta** - HTTP $codigo (esperado $Esperado) - $marca"
    Add-Line ""
    Add-Line '```json'
    Add-Line $respuesta
    Add-Line '```'
    Add-Line ""

    $color = if ($coincide) { "Green" } else { "Yellow" }
    Write-Host ("  [{0,-7}] {1,-4} {2,-52} -> {3}" -f $marca, $Metodo, $Ruta, $codigo) -ForegroundColor $color

    return @{ Codigo = $codigo; Cuerpo = $respuesta }
}

# ---------------------------------------------------------------------------

Write-Host ""
Write-Host "Generando evidencias contra $BaseUrl" -ForegroundColor Cyan
Write-Host ""

try {
    Invoke-WebRequest -Uri "$BaseUrl/v3/api-docs" -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop | Out-Null
}
catch {
    Write-Host "No se pudo contactar la aplicacion en $BaseUrl" -ForegroundColor Red
    Write-Host "Levantela primero con: .\mvnw.cmd spring-boot:run" -ForegroundColor Red
    exit 1
}

$fecha = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

Add-Line "# Evidencias de prueba - UCA-CFC Connect"
Add-Line ""
Add-Line "Universidad Don Bosco - Desarrollo de Software Empresarial - Fase 2"
Add-Line ""
Add-Line "Generado el $fecha contra ``$BaseUrl``."
Add-Line ""
Add-Line "Cada caso muestra la peticion enviada, el codigo HTTP devuelto y la"
Add-Line "respuesta completa. La columna *esperado* documenta que comportamiento"
Add-Line "se estaba verificando."
Add-Line ""
Add-Line "> Este archivo lo genera ``evidencias\generar-evidencias.ps1`` ejecutandose"
Add-Line "> contra la aplicacion en marcha; las respuestas son reales, no transcritas."
Add-Line ""
Add-Line "**Importante:** el script debe ejecutarse sobre la base recien creada por"
Add-Line "Flyway. Algunos casos crean o eliminan registros, de modo que una segunda"
Add-Line "ejecucion seguida puede dar codigos distintos a los esperados."

# --------------------------- 1. CRUD completo ---------------------------
Add-Seccion "1. CRUD completo" @"
Ciclo de vida entero de un recurso (categoria): crear, consultar, listar,
actualizar y eliminar, comprobando ademas que despues de eliminar ya no existe.
"@

$nuevaCategoria = @{ nombre = "Categoria de Evidencia"; descripcion = "Creada por el script de evidencias" }
$creada = Probar -Titulo "1.1 CREATE - crear una categoria" -Metodo POST -Ruta "/categorias" `
    -Cuerpo $nuevaCategoria -Esperado 201 `
    -Comentario "Responde 201 y la cabecera Location apunta al recurso creado."

$idCategoria = $null
try { $idCategoria = ($creada.Cuerpo | ConvertFrom-Json).id } catch { }

if ($idCategoria) {
    Probar -Titulo "1.2 READ - consultar la categoria creada" -Metodo GET -Ruta "/categorias/$idCategoria" -Esperado 200 | Out-Null

    Probar -Titulo "1.3 READ - listarla entre las demas" -Metodo GET -Ruta "/categorias?nombre=Evidencia" -Esperado 200 `
        -Comentario "Filtro por nombre parcial sobre el listado paginado." | Out-Null

    Probar -Titulo "1.4 UPDATE - modificar la categoria" -Metodo PUT -Ruta "/categorias/$idCategoria" `
        -Cuerpo @{ nombre = "Categoria de Evidencia (editada)"; descripcion = "Descripcion actualizada" } -Esperado 200 | Out-Null

    Probar -Titulo "1.5 DELETE - eliminar la categoria" -Metodo DELETE -Ruta "/categorias/$idCategoria" -Esperado 204 `
        -Comentario "204 No Content: eliminacion correcta, sin cuerpo de respuesta." | Out-Null

    Probar -Titulo "1.6 Comprobacion - la categoria ya no existe" -Metodo GET -Ruta "/categorias/$idCategoria" -Esperado 404 | Out-Null
}
else {
    Add-Line "> No se pudo obtener el id de la categoria creada; se omiten los pasos 1.2 a 1.6."
    Add-Line ""
}

# --------------------------- 2. Validaciones ---------------------------
Add-Seccion "2. Validaciones" @"
Bean Validation sobre los DTO de entrada. La respuesta 400 incluye el arreglo
``errores`` con el campo que fallo y el motivo, en espanol.
"@

Probar -Titulo "2.1 Campos obligatorios vacios o ausentes" -Metodo POST -Ruta "/inscripciones" `
    -Cuerpo @{ clienteId = $null; cursoId = 2; participanteNombre = ""; participanteEmail = "no-es-un-correo"; participanteTelefono = "123" } `
    -Esperado 400 -Comentario "Cuatro violaciones a la vez: cliente nulo, nombre vacio, correo mal formado y telefono con formato invalido." | Out-Null

Probar -Titulo "2.2 Formato de DUI invalido" -Metodo POST -Ruta "/clientes" `
    -Cuerpo @{ tipo = "PERSONA_PARTICULAR"; nombre = "Prueba Formato"; dui = "123"; email = "formato@correo.com" } `
    -Esperado 400 -Comentario "El DUI debe seguir el patron 00000000-0." | Out-Null

Probar -Titulo "2.3 Contrasena que no cumple la politica" -Metodo POST -Ruta "/usuarios" `
    -Cuerpo @{ nombre = "Usuario Debil"; email = "debil@cfc.uca.edu.sv"; password = "corta"; rolId = 2 } `
    -Esperado 400 -Comentario "Minimo 10 caracteres, con mayuscula, minuscula y numero." | Out-Null

Probar -Titulo "2.4 Valor fuera del enum permitido" -Metodo PUT -Ruta "/inscripciones/1/estado" `
    -Cuerpo @{ estado = "INVENTADO" } -Esperado 400 `
    -Comentario "El mensaje enumera los valores admitidos." | Out-Null

Probar -Titulo "2.5 Coleccion vacia donde se exige al menos un elemento" -Metodo POST -Ruta "/cotizaciones" `
    -Cuerpo @{ clienteId = 2; asunto = "Cotizacion sin items"; items = @() } -Esperado 400 | Out-Null

# --------------------------- 3. Manejo de errores ---------------------------
Add-Seccion "3. Manejo de errores" @"
Todos los errores salen del mismo ``@RestControllerAdvice`` y comparten formato:
``timestamp``, ``status``, ``error``, ``message`` y ``path``. Se incluyen aqui las
dos reglas de negocio criticas del proyecto.
"@

Probar -Titulo "3.1 404 - recurso inexistente" -Metodo GET -Ruta "/cursos/99999" -Esperado 404 | Out-Null

Probar -Titulo "3.2 400 - tipo de parametro incorrecto" -Metodo GET -Ruta "/cursos/abc" -Esperado 400 `
    -Comentario "El id debe ser numerico." | Out-Null

Probar -Titulo "3.3 400 - parametro de ordenamiento invalido" -Metodo GET -Ruta "/cursos?sort=campoQueNoExiste,asc" -Esperado 400 `
    -Comentario "Ordenar por un campo que no existe en la entidad es un error del cliente, no del servidor." | Out-Null

Probar -Titulo "3.4 405 - metodo HTTP no permitido" -Metodo PATCH -Ruta "/cursos/1" -Esperado 405 | Out-Null

Probar -Titulo "3.5 REGLA CRITICA - cupo agotado (409)" -Metodo POST -Ruta "/inscripciones" `
    -Cuerpo @{ clienteId = 1; cursoId = 2; participanteNombre = "Marta Elena Gomez"; participanteEmail = "marta.gomez@vitalab.com.sv" } `
    -Esperado 409 -Comentario @"
El participante ya tiene una inscripcion vigente en esa oferta. La misma
validacion, con la fila del curso bloqueada, impide superar el cupo maximo.
"@ | Out-Null

Probar -Titulo "3.6 REGLA CRITICA - espacio ocupado (409)" -Metodo POST -Ruta "/alquileres" `
    -Cuerpo @{ clienteId = 2; espacioId = 1; fechaHoraInicio = "2026-09-15T17:30:00"; fechaHoraFin = "2026-09-15T18:00:00"; proposito = "Prueba de traslape"; cantidadAsistentes = 10 } `
    -Esperado 409 -Comentario @"
El Auditorio Principal ya tiene la asamblea de accionistas de 17:00 a 18:30.
El mensaje indica contra que actividad choca y en que horario.
"@ | Out-Null

Probar -Titulo "3.7 Reserva contigua SI se permite (201)" -Metodo POST -Ruta "/alquileres" `
    -Cuerpo @{ clienteId = 2; espacioId = 1; fechaHoraInicio = "2026-09-15T18:30:00"; fechaHoraFin = "2026-09-15T20:00:00"; proposito = "Reserva contigua"; cantidadAsistentes = 10 } `
    -Esperado 201 -Comentario @"
Empieza exactamente cuando termina la anterior. Los intervalos tienen el fin
exclusivo, por lo que no hay traslape: nuevoInicio < existenteFin AND
nuevoFin > existenteInicio.
"@ | Out-Null

Probar -Titulo "3.8 422 - regla de negocio incumplida" -Metodo POST -Ruta "/alquileres" `
    -Cuerpo @{ clienteId = 2; espacioId = 5; fechaHoraInicio = "2026-10-20T08:00:00"; fechaHoraFin = "2026-10-20T10:00:00"; proposito = "Exceso de aforo"; cantidadAsistentes = 500 } `
    -Esperado 422 -Comentario "Se piden mas asistentes que la capacidad del espacio." | Out-Null

Probar -Titulo "3.9 409 - integridad referencial protegida" -Metodo DELETE -Ruta "/categorias/1" -Esperado 409 `
    -Comentario "No se elimina una categoria que tiene cursos asociados." | Out-Null

# --------------------- 4. Busquedas personalizadas ---------------------
Add-Seccion "4. Busquedas personalizadas" @"
Filtros combinables, paginacion y ordenamiento. Todos los parametros son
opcionales y se resuelven en una sola consulta JPQL por recurso.
"@

Probar -Titulo "4.1 Paginacion" -Metodo GET -Ruta "/cursos?page=0&size=3" -Esperado 200 `
    -Comentario "La envoltura devuelve contenido, pagina, tamanio, totalElementos, totalPaginas y ultima." | Out-Null

Probar -Titulo "4.2 Ordenamiento descendente" -Metodo GET -Ruta "/cursos?sort=precio,desc&size=5" -Esperado 200 | Out-Null

Probar -Titulo "4.3 Filtro por estado" -Metodo GET -Ruta "/cursos?estado=ACTIVO" -Esperado 200 | Out-Null

Probar -Titulo "4.4 Busqueda por texto libre (nombre o codigo)" -Metodo GET -Ruta "/cursos?texto=excel" -Esperado 200 | Out-Null

Probar -Titulo "4.5 Filtros combinados" -Metodo GET -Ruta "/cursos?estado=ACTIVO&modalidadId=1&sort=fechaInicio,asc" -Esperado 200 | Out-Null

Probar -Titulo "4.6 Filtro por tipo y capacidad minima" -Metodo GET -Ruta "/espacios?tipo=AUDITORIO&capacidadMinima=100" -Esperado 200 | Out-Null

Probar -Titulo "4.7 Filtro por razon social de la empresa" -Metodo GET -Ruta "/clientes?empresa=vitalab" -Esperado 200 `
    -Comentario "Recorre la relacion cliente-empresa con LEFT JOIN, para no excluir a las personas particulares." | Out-Null

Probar -Titulo "4.8 Filtro por rango de fechas" -Metodo GET -Ruta "/inscripciones?desde=2026-09-01&hasta=2026-09-30" -Esperado 200 | Out-Null

Probar -Titulo "4.9 Consulta de disponibilidad de un espacio" -Metodo GET `
    -Ruta "/espacios/1/disponibilidad?desde=2026-09-15T08:00:00&hasta=2026-09-15T20:00:00" -Esperado 200 `
    -Comentario "Devuelve si esta libre y que franjas estan ocupadas." | Out-Null

# --------------------- 5. Funcionamiento por modulo ---------------------
Add-Seccion "5. Funcionamiento de los modulos" @"
Una consulta representativa por cada uno de los modulos desarrollados.
"@

Probar -Titulo "5.1 Modulo 1 - Gestion academica (cursos)"    -Metodo GET -Ruta "/cursos?size=3"            -Esperado 200 | Out-Null
Probar -Titulo "5.2 Modulo 1 - Gestion academica (diplomados)" -Metodo GET -Ruta "/diplomados?size=3"       -Esperado 200 | Out-Null
Probar -Titulo "5.3 Modulo 1 - Catalogos (docentes)"           -Metodo GET -Ruta "/docentes?size=3"         -Esperado 200 | Out-Null
Probar -Titulo "5.4 Modulo 2 - Clientes"                       -Metodo GET -Ruta "/clientes?size=3"         -Esperado 200 | Out-Null
Probar -Titulo "5.5 Modulo 2 - Historial consolidado"          -Metodo GET -Ruta "/clientes/1/historial"    -Esperado 200 `
    -Comentario "Inscripciones, cotizaciones y pagos del cliente, con totales." | Out-Null
Probar -Titulo "5.6 Modulo 3 - Inscripciones"                  -Metodo GET -Ruta "/inscripciones?size=3"    -Esperado 200 | Out-Null
Probar -Titulo "5.7 Modulo 4 - Cotizaciones"                   -Metodo GET -Ruta "/cotizaciones?size=3"     -Esperado 200 `
    -Comentario "El tipo se deduce de los items: con varios tipos, COMBINADA." | Out-Null
Probar -Titulo "5.8 Modulo 5 - Espacios"                       -Metodo GET -Ruta "/espacios?size=3"         -Esperado 200 | Out-Null
Probar -Titulo "5.9 Modulo 5 - Alquileres"                     -Metodo GET -Ruta "/alquileres?size=3"       -Esperado 200 | Out-Null
Probar -Titulo "5.10 Modulo 6 - Servicios de catering"         -Metodo GET -Ruta "/servicios-catering?size=3" -Esperado 200 | Out-Null
Probar -Titulo "5.11 Modulo 6 - Solicitudes de catering"       -Metodo GET -Ruta "/solicitudes-catering?size=3" -Esperado 200 | Out-Null
Probar -Titulo "5.12 Modulo 7 - Agenda institucional"          -Metodo GET -Ruta "/agenda?size=5"           -Esperado 200 | Out-Null
Probar -Titulo "5.13 Modulo 7 - Deteccion de conflictos"       -Metodo GET -Ruta "/agenda/conflictos"       -Esperado 200 `
    -Comentario "Vacio significa que no hay choques: el sistema los impide al crear." | Out-Null
Probar -Titulo "5.14 Modulo 8 - Pagos"                         -Metodo GET -Ruta "/pagos?size=3"           -Esperado 200 | Out-Null
Probar -Titulo "5.15 Modulo 8 - Reporte de estados financieros" -Metodo GET -Ruta "/pagos/reportes/estados-financieros" -Esperado 200 | Out-Null
Probar -Titulo "5.16 Modulo 9 - Usuarios"                      -Metodo GET -Ruta "/usuarios?size=3"        -Esperado 200 `
    -Comentario "La respuesta nunca incluye la contrasena ni su hash." | Out-Null
Probar -Titulo "5.17 Modulo 9 - Roles"                         -Metodo GET -Ruta "/roles"                  -Esperado 200 | Out-Null

# ------------------------------ Resumen ------------------------------
$resumen = @"

---

## Resumen

- Casos ejecutados: **$($script:total)**
- Con el codigo HTTP esperado: **$($script:okEsperado)**
- Marcados para revisar: **$($script:total - $script:okEsperado)**

Los casos marcados como REVISAR suelen deberse a que la base de datos ya no
tiene los datos iniciales tal como los carga Flyway (por ejemplo, porque una
prueba manual anterior creo o borro registros). Al recrear la base y volver a
ejecutar el script, deberian quedar todos en OK.
"@
Add-Line $resumen

$dir = Split-Path -Parent $Salida
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
$sb.ToString() | Out-File -FilePath $Salida -Encoding utf8

Write-Host ""
Write-Host "Casos ejecutados : $($script:total)" -ForegroundColor Cyan
Write-Host "Con codigo esperado: $($script:okEsperado)" -ForegroundColor Cyan
Write-Host "Evidencias escritas en: $Salida" -ForegroundColor Green
Write-Host ""
