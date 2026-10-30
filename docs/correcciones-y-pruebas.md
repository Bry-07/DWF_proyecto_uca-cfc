# Correcciones y pruebas — UCA-CFC Connect

Fecha: 30 de octubre de 2026. Base de comparación: el ZIP original adjuntado en este chat.

Los 12 puntos revisados fueron corregidos. Los fragmentos siguientes son extractos literales del código; el parche adjunto contiene todos los cambios.

## Resultado de verificación

| Verificación | Resultado |
| --- | --- |
| Backend: pruebas unitarias | 94 aprobadas |
| Backend: integración HTTP/PostgreSQL | 14 aprobadas |
| Backend: transporte SMTP local | 1 aprobada |
| Total backend, `verify` y JAR ejecutable | 109 aprobadas, 0 fallos, 0 omitidas |
| Repetición sobre base ya migrada | 15 de integración aprobadas nuevamente |
| Frontend: React Testing Library/Vitest | 14 aprobadas |
| Frontend: lint y build | Aprobados |

Entorno verificado: JDK Temurin 21.0.9, Maven 3.9.11, PostgreSQL 16.15, Node 24.19.0. Se verificaron las migraciones V1–V5 en una base nueva y su validación al repetir las pruebas. Las pruebas React renderizan componentes reales con servicios HTTP simulados; las de integración Java usan servidor HTTP, PostgreSQL y transporte SMTP local reales. No se ejecutó automatización de navegador ni envío a un proveedor SMTP externo.

El build de Vite conserva un aviso por el bundle principal de aproximadamente 503 kB sin comprimir. No impidió generar el build.

## 1. Invalidación de JWT al cambiar la cuenta

**Archivos:**
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/entity/Usuario.java`
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/JwtClaims.java`
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/JwtService.java`
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/UsuarioVigenteValidator.java`
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/SecurityConfig.java`
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/UsuarioService.java`
- `backend/uca-cfc-connect/src/main/resources/db/migration/V5__sesiones_y_correlativos.sql`

**Por qué fallaba:** El decodificador comprobaba firma, expiración y revocación individual. No consultaba si el usuario seguía activo ni si habían cambiado su contraseña o sus permisos. Revocar los refresh tokens no invalidaba los access tokens existentes.

**Antes:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/SecurityConfig.java` (línea 203).

```java
        OAuth2TokenValidator<Jwt> validadores = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(JwtService.EMISOR),
                new TokenRevocadoValidator(tokenRevocadoRepository));
        decodificador.setJwtValidator(validadores);
        return decodificador;
```

**Ahora:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/UsuarioVigenteValidator.java` (línea 20).

```java
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object uid = jwt.getClaim(JwtClaims.USUARIO_ID);
        Object version = jwt.getClaim(JwtClaims.VERSION_SESION);
        if (uid instanceof Number id && version instanceof Number revision && jwt.getId() != null) {
            boolean vigente = usuarios.findWithRolById(id.longValue()).filter(usuario ->
                    usuario.getEstado() == EstadoUsuario.ACTIVO
                    && usuario.getVersion() == revision.longValue()
                    && usuario.getEmail().equals(jwt.getSubject())
                    && usuario.getRol().getNombre().equals(jwt.getClaimAsString(JwtClaims.ROL))).isPresent();
            if (vigente) {
                return OAuth2TokenValidatorResult.success();
            }
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "La cuenta o sus permisos cambiaron. Inicie sesión nuevamente", null));
    }
}
```

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/entity/Usuario.java` (línea 24).

```java
    @Version
    private long version;

    public long getVersion() {
        return version;
    }
```

**Por qué funciona:** Usuario tiene una revisión JPA con @Version; el JWT incluye sv. UsuarioVigenteValidator exige que el usuario exista, esté activo y coincidan revisión, correo y rol. Los cambios administrativos revocan también sus refresh tokens. El restablecimiento de contraseña modifica Usuario y aumenta automáticamente la revisión al confirmar la transacción. Desactivar y reactivar no resucita JWT antiguos. Los tokens anteriores a esta actualización requieren iniciar sesión nuevamente.

**Comprobación:** UsuarioVigenteValidatorTest: seis casos. ApplicationIT verifica HTTP 401 tras desactivar, reactivar, cambiar rol y restablecer contraseña; verifica también que el nuevo login funciona y que el refresh anterior no sirve.

## 2. Pagos asociados al cliente equivocado

**Archivos:**
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java`

**Por qué fallaba:** validarReferencia solo comprobaba existsById. Una referencia válida podía pertenecer a un cliente diferente al indicado en el pago.

**Antes:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java` (línea 286).

```java
    private void validarReferencia(ConceptoPago concepto, Long referenciaId) {
        boolean existe = switch (concepto) {
            case INSCRIPCION -> inscripcionRepository.existsById(referenciaId);
            case COTIZACION -> cotizacionRepository.existsById(referenciaId);
            case ALQUILER -> alquilerRepository.existsById(referenciaId);
            case CATERING -> solicitudCateringRepository.existsById(referenciaId);
        };
        if (!existe) {
            throw new ResourceNotFoundException(
                    "No existe " + concepto.name().toLowerCase() + " con id " + referenciaId);
        }
    }
```

**Ahora:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java` (línea 291).

```java
    private void validarReferencia(ConceptoPago concepto, Long referenciaId, Long clienteId) {
        String mensaje = "No existe " + concepto.name().toLowerCase() + " con id " + referenciaId;
        Cliente propietario = switch (concepto) {
            case INSCRIPCION -> inscripcionRepository.findById(referenciaId)
                    .orElseThrow(() -> new ResourceNotFoundException(mensaje)).getCliente();
            case COTIZACION -> cotizacionRepository.findById(referenciaId)
                    .orElseThrow(() -> new ResourceNotFoundException(mensaje)).getCliente();
            case ALQUILER -> alquilerRepository.findById(referenciaId)
                    .orElseThrow(() -> new ResourceNotFoundException(mensaje)).getCliente();
            case CATERING -> solicitudCateringRepository.findById(referenciaId)
                    .orElseThrow(() -> new ResourceNotFoundException(mensaje)).getCliente();
        };
        if (!propietario.getId().equals(clienteId)) {
            throw new BusinessException("El documento referenciado no pertenece al cliente del pago");
        }
    }
}
```

**Por qué funciona:** Se carga el documento del concepto indicado y se compara el id de su propietario con clienteId antes de guardar. Un documento inexistente conserva 404; un documento ajeno produce 422 sin insertar un pago.

**Comprobación:** PagoServiceTest rechaza el propietario incorrecto. ApplicationIT prueba por HTTP los cuatro conceptos: inscripción, cotización, alquiler y catering, y comprueba que no aumente el número de pagos.

## 3. Estados financieros sin dinero recibido

**Archivos:**
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java`
- `backend/uca-cfc-connect/src/main/resources/db/migration/V5__sesiones_y_correlativos.sql`

**Por qué fallaba:** El cambio a PARCIAL no modificaba el importe y marcar PAGADO inventaba el monto recibido asignándole montoEsperado. validar() se fiaba del estado PENDIENTE para detectar un pago sin dinero.

**Antes:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java` (línea 186).

```java
        // El monto acompaña al estado: marcar PAGADO salda el registro
        if (destino == EstadoPago.PAGADO) {
            pago.setMontoPagado(pago.getMontoEsperado());
        }
        pago.setEstado(destino);
        log.info("Pago {}: estado {} → {}", id, actual, destino);
```

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java` (línea 150).

```java
        if (pago.getEstado() == EstadoPago.PENDIENTE) {
            throw new BusinessException("No se puede validar un pago sin monto recibido");
        }
```

**Ahora:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java` (línea 187).

```java
        if (destino != calcularEstado(pago.getMontoPagado(), pago.getMontoEsperado())) {
            throw new BusinessException("El estado debe corresponder al monto recibido. Registre un abono");
        }
        pago.setEstado(destino);
        log.info("Pago {}: estado {} → {}", id, actual, destino);
        return mapper.aResponse(pagoRepository.save(pago), comprobanteRepository.existsByPagoId(id));
```

Archivo: `backend/uca-cfc-connect/src/main/resources/db/migration/V5__sesiones_y_correlativos.sql` (línea 11).

```sql
ALTER TABLE pagos ADD CONSTRAINT ck_pagos_estado_monto CHECK (
    (monto_pagado = 0 AND estado = 'PENDIENTE') OR
    (monto_pagado > 0 AND monto_pagado < monto_esperado AND estado = 'PARCIAL') OR
    (monto_pagado = monto_esperado AND estado = 'PAGADO')
);
```

**Por qué funciona:** El estado solicitado debe coincidir con el calculado a partir de los importes. El dinero recibido solo aumenta mediante el abono. validar() comprueba directamente que montoPagado sea positivo. V5 normaliza estados históricos según los importes y añade un CHECK que impide volver a guardar estados incoherentes.

**Comprobación:** Tres regresiones unitarias nuevas: PARCIAL con cero, PAGADO sin cobrar y validación de un estado histórico incoherente. ApplicationIT comprueba las respuestas 422 y que montoPagado permanezca en cero.

## 4. Confirmación de pagos por el rol correcto

**Archivos:**
- `frontend/uca-cfc-connect/src/pages/pagos/Pagos.jsx`
- `frontend/uca-cfc-connect/src/utils/navigation.js`
- `frontend/uca-cfc-connect/src/utils/permissions.js`

**Por qué fallaba:** La pantalla agrupaba todas las acciones bajo finance. Recepción no tenía la ruta en su menú y contabilidad veía Confirmar pese a no tener ese permiso en la API.

**Antes:**

Archivo: `frontend/uca-cfc-connect/src/pages/pagos/Pagos.jsx` (línea 151).

```jsx
              finance && (
                <div className="row-actions">
                  {Number(r.saldo) > 0 && <button type="button" className="link-action" onClick={() => setAbono(r)}>Abono</button>}
                  {!r.fechaValidacion && Number(r.montoPagado) > 0 && <button type="button" className="link-action" onClick={() => act('validar', r)}>Validar</button>}
                  {r.fechaValidacion && !r.fechaConfirmacion && <button type="button" className="link-action" onClick={() => act('confirmar', r)}>Confirmar</button>}
                </div>
              ),
```

**Ahora:**

Archivo: `frontend/uca-cfc-connect/src/pages/pagos/Pagos.jsx` (línea 154).

```jsx
              (finance || canConfirm) && (
                <div className="row-actions">
                  {finance && Number(r.saldo) > 0 && <button type="button" className="link-action" onClick={() => setAbono(r)}>Abono</button>}
                  {finance && !r.fechaValidacion && Number(r.montoPagado) > 0 && <button type="button" className="link-action" onClick={() => act('validar', r)}>Validar</button>}
                  {canConfirm && r.fechaValidacion && !r.fechaConfirmacion && <button type="button" className="link-action" onClick={() => act('confirmar', r)}>Confirmar</button>}
                </div>
              ),
```

**Por qué funciona:** Recepción tiene acceso a Pagos y puede confirmar después de la validación. Contabilidad puede registrar, abonar y validar, pero no confirmar. ADMIN conserva ambas funciones. Recepción no solicita ni muestra el resumen financiero global. Los clientes conservan la consulta de sus propios pagos sin acciones de gestión.

**Comprobación:** Cuatro pruebas React cubren las acciones por rol. ApplicationIT comprueba GET de recepción, validación de contabilidad, confirmación de recepción y 403 para los roles incorrectos.

## 5. Inscripción propia del cliente

**Archivos:**
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/SecurityConfig.java`
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/InscripcionController.java`

**Por qué fallaba:** El asistente React permitía al cliente inscribirse, pero POST /inscripciones estaba reservado al personal. El formulario terminaba en 403.

**Antes:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/InscripcionController.java` (línea 87).

```java
    public ResponseEntity<InscripcionResponse> crear(@Valid @RequestBody InscripcionRequest request) {
        InscripcionResponse creada = inscripcionService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creada.id())).body(creada);
    }
```

**Ahora:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/SecurityConfig.java` (línea 114).

```java
        // ---- Inscripciones: el cliente puede inscribirse únicamente a su propio nombre ----
        rutas.requestMatchers(HttpMethod.POST, BASE + "/inscripciones")
                .hasAnyRole(ADMIN, RECEPCIONISTA, CLIENTE);
        rutas.requestMatchers(HttpMethod.GET, BASE + "/inscripciones", BASE + "/inscripciones/**")
```

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/InscripcionController.java` (línea 87).

```java
    public ResponseEntity<InscripcionResponse> crear(@Valid @RequestBody InscripcionRequest request) {
        usuarioActual.verificarCliente(request.clienteId());
        InscripcionResponse creada = inscripcionService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creada.id())).body(creada);
    }
```

**Por qué funciona:** Se conserva el flujo ya ofrecido por la interfaz. POST admite CLIENTE y el controlador llama verificarCliente antes del servicio. El id del cliente se obtiene de la cuenta autenticada para verificar la propiedad: una petición manipulada a nombre de otro cliente devuelve 403. Los cambios de estado y edición siguen reservados al personal.

**Comprobación:** ApplicationIT: el cliente crea su inscripción (201) y no puede crearla a nombre de otra cuenta (403).

## 6. Controles administrativos ofrecidos a recepción

**Archivos:**
- `frontend/uca-cfc-connect/src/utils/permissions.js`
- `frontend/uca-cfc-connect/src/pages/catalogo/Catalogo.jsx`
- `frontend/uca-cfc-connect/src/pages/recursos/Recursos.jsx`
- `frontend/uca-cfc-connect/src/pages/catering/Catering.jsx`
- `frontend/uca-cfc-connect/src/pages/cotizaciones/Cotizaciones.jsx`
- `frontend/uca-cfc-connect/src/utils/navigation.js`

**Por qué fallaba:** isStaff incluía ADMIN y RECEPCIONISTA y se reutilizaba para crear/editar catálogos y resolver cotizaciones. La API reserva esas acciones a ADMIN. Además se mostraba Agenda a CONTABILIDAD aunque el servidor la rechaza.

**Antes:**

Archivo: `frontend/uca-cfc-connect/src/utils/permissions.js` (línea 4).

```js
export const STAFF = [ROLES.ADMIN, ROLES.RECEPCIONISTA]
export const FINANCE = [ROLES.ADMIN, ROLES.CONTABILIDAD]
export const isStaff = (role) => STAFF.includes(role)
export const isFinance = (role) => FINANCE.includes(role)
```

Archivo: `frontend/uca-cfc-connect/src/pages/catalogo/Catalogo.jsx` (línea 97).

```jsx
  const staff = isStaff(role)
```

**Ahora:**

Archivo: `frontend/uca-cfc-connect/src/utils/permissions.js` (línea 8).

```js
export const canManageCatalog = (role) => role === ROLES.ADMIN
export const canResolveQuotation = (role) => role === ROLES.ADMIN
export const canConfirmPayment = (role) => STAFF.includes(role)
```

Archivo: `frontend/uca-cfc-connect/src/pages/catalogo/Catalogo.jsx` (línea 97).

```jsx
  const canEdit = canManageCatalog(role)
```

**Por qué funciona:** Se separan canManageCatalog y canResolveQuotation, exclusivos de ADMIN. Los catálogos usan canEdit y la cotización canResolve. La gestión operativa de alquileres y solicitudes de catering sigue disponible al personal. Agenda se muestra únicamente a ADMIN y RECEPCIONISTA.

**Comprobación:** Cinco pruebas React comprueban ausencia de controles indebidos en cursos, espacios, catering y cotizaciones, y que ADMIN conserve la creación.

## 7. Disponibilidad falsa cuando hay catering

**Archivos:**
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/EspacioService.java`

**Por qué fallaba:** Se omitía cualquier actividad sincronizada para no duplicar los alquileres. esSincronizado() también incluye CATERING, que desaparecía de la consulta aunque sí bloqueaba el alquiler.

**Antes:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/EspacioService.java` (línea 128).

```java
            if (actividad.getTipo().esSincronizado() && actividad.getOrigenId() != null) {
                continue;
            }
```

**Ahora:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/EspacioService.java` (línea 129).

```java
            if (actividad.getTipo() == TipoActividad.ALQUILER && actividad.getOrigenId() != null) {
                continue;
            }
```

**Por qué funciona:** Solo se excluyen las actividades de tipo ALQUILER que ya fueron incorporadas desde la tabla de alquileres. CATERING permanece en las ocupaciones y disponible resulta false.

**Comprobación:** ApplicationIT crea una solicitud de catering real y consulta el mismo intervalo: disponible=false y ocupaciones no vacías.

## 8. Cotizaciones nuevas ausentes del tablero

**Archivos:**
- `frontend/uca-cfc-connect/src/pages/dashboard/StaffDashboard.jsx`
- `frontend/uca-cfc-connect/src/pages/cotizaciones/CotizacionNueva.jsx`

**Por qué fallaba:** El tablero filtraba únicamente PENDIENTE, mientras el servicio crea cotizaciones EN_PROCESO. La pantalla de creación también anunciaba el estado anterior.

**Antes:**

Archivo: `frontend/uca-cfc-connect/src/pages/dashboard/StaffDashboard.jsx` (línea 19).

```jsx
    cotizacionesApi.list({ estado: 'PENDIENTE', size: 5 }),
```

**Ahora:**

Archivo: `frontend/uca-cfc-connect/src/pages/dashboard/StaffDashboard.jsx` (línea 27).

```jsx
async function loadCotizacionesAbiertas() {
  const paginas = await Promise.all(['PENDIENTE', 'EN_PROCESO'].map((estado) => cotizacionesApi.list({ estado, size: 5 })))
  return {
    total: paginas.reduce((sum, pagina) => sum + pagina.total, 0),
    items: paginas.flatMap((pagina) => pagina.items).sort((a, b) => b.id - a.id).slice(0, 5),
  }
}
```

**Por qué funciona:** El tablero consulta ambos estados, suma sus totales y muestra las cinco solicitudes abiertas más recientes. La pantalla de creación anuncia EN_PROCESO y explica que cotizar no reserva un espacio.

**Comprobación:** Una prueba React entrega una cotización EN_PROCESO y verifica que se vea en el tablero y que se consulten ambos estados.

## 9. Recuperación de contraseña sin entrega del código

**Archivos:**
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/CorreoService.java`
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/exception/CorreoNoDisponibleException.java`
- `backend/uca-cfc-connect/pom.xml`
- `backend/uca-cfc-connect/src/main/resources/application.yml`
- `backend/uca-cfc-connect/src/main/resources/application-dev.yml`
- `backend/uca-cfc-connect/.env.example`

**Por qué fallaba:** enviarRecuperacion solo escribía el token en DEBUG. El usuario no recibía correo y, por defecto, la respuesta tampoco incluía el código de prueba.

**Antes:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/CorreoService.java` (línea 19).

```java
    public void enviarRecuperacion(Usuario usuario, String token) {
        log.debug("Recuperación de contraseña para {}. Token: {}", usuario.getEmail(), token);
    }
```

**Ahora:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/CorreoService.java` (línea 34).

```java
    public void enviarRecuperacion(Usuario usuario, String token) {
        JavaMailSender sender = correo.getIfAvailable();
        if (sender == null) {
            if (modoPrueba) return;
            throw new CorreoNoDisponibleException();
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(usuario.getEmail());
        mensaje.setSubject("Restablecer contraseña de UCA-CFC Connect");
        mensaje.setText("Use este enlace para restablecer su contraseña: " + frontend
                + "/restablecer?token=" + token + "\nCódigo de recuperación: " + token
                + "\nEl código tiene un solo uso. Si no solicitó el cambio, ignore este correo.");
        try {
            sender.send(mensaje);
        } catch (MailException ex) {
            // No registrar tokens ni informar éxito si el transporte rechazó el envío.
            throw new CorreoNoDisponibleException();
        }
    }
}
```

**Por qué funciona:** Se integra JavaMailSender y un mensaje con destinatario, enlace y código. La configuración SMTP usa variables de entorno y STARTTLS requerido por defecto. No se registran códigos en logs. Solo el perfil dev habilita tokenDePrueba para completar el flujo sin un servidor de correo. Fuera de ese modo, ausencia o fallo del transporte devuelve 503 y la transacción no confirma un token que no pudo enviarse.

**Comprobación:** CorreoServiceTest cubre destinatario/enlace, modo desarrollo y fallo del transporte. CorreoSmtpIT entrega un correo mediante SMTP real a un servidor local y comprueba el MIME recibido. ApplicationIT completa la recuperación y verifica el uso único del código. No se utilizó un proveedor SMTP externo: sus credenciales y dominio deben configurarse para producción.

## 10. DUI/NIT obligatorios presentados como opcionales

**Archivos:**
- `frontend/uca-cfc-connect/src/pages/auth/Registro.jsx`

**Por qué fallaba:** El formulario etiquetaba DUI y NIT como opcionales y no exigía su presencia según el tipo, aunque ClienteService requiere DUI para personas y NIT para empresas.

**Antes:**

Archivo: `frontend/uca-cfc-connect/src/pages/auth/Registro.jsx` (línea 102).

```jsx
          <AuthField id="dui" label="DUI (opcional)" placeholder="00000000-0" value={f.dui} onChange={set('dui')} error={errors.dui} />
```

Archivo: `frontend/uca-cfc-connect/src/pages/auth/Registro.jsx` (línea 104).

```jsx
        <AuthField id="nit" label="NIT (opcional)" placeholder="0000-000000-000-0" value={f.nit} onChange={set('nit')} error={errors.nit} />
```

**Ahora:**

Archivo: `frontend/uca-cfc-connect/src/pages/auth/Registro.jsx` (línea 37).

```jsx
    if (!empresa && f.dui && !RE.DUI.test(f.dui)) er.dui = 'Formato 00000000-0.'
    if (f.nit && !RE.NIT.test(f.nit)) er.nit = 'Formato 0000-000000-000-0.'
    if (!empresa && !f.dui.trim()) er.dui = 'El DUI es obligatorio para una persona particular.'
    if (empresa && !f.nit.trim()) er.nit = 'El NIT es obligatorio para una empresa.'
```

Archivo: `frontend/uca-cfc-connect/src/pages/auth/Registro.jsx` (línea 55).

```jsx
      dui: empresa ? undefined : f.dui, nit: f.nit, telefono: f.telefono, direccion: f.direccion.trim(),
```

**Por qué funciona:** La validación exige la identificación del tipo seleccionado y las etiquetas lo indican. El DUI se oculta y se omite al enviar una empresa; su valor anterior no bloquea el formulario al cambiar de tipo. El backend conserva sus validaciones independientes.

**Comprobación:** Cuatro pruebas React: persona sin DUI, empresa sin NIT, persona válida y cambio de persona a empresa con un DUI incompleto oculto.

## 11. Pérdida de abonos simultáneos

**Archivos:**
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository/PagoRepository.java`
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java`

**Por qué fallaba:** Cada transacción leía el mismo montoPagado y después lo sobrescribía. @Transactional por sí solo no serializa estas lecturas bajo READ_COMMITTED.

**Antes:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java` (línea 124).

```java
    public PagoResponse abonar(Long id, AbonoPagoRequest request) {
        Pago pago = buscar(id);
        if (pago.getEstado() == EstadoPago.PAGADO) {
            throw new BusinessException("El pago ya está saldado");
```

**Ahora:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository/PagoRepository.java` (línea 28).

```java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pago p WHERE p.id = :id")
    Optional<Pago> findForUpdateById(@Param("id") Long id);
```

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java` (línea 125).

```java
    public PagoResponse abonar(Long id, AbonoPagoRequest request) {
        Pago pago = buscarParaActualizar(id);
        if (pago.getEstado() == EstadoPago.PAGADO) {
            throw new BusinessException("El pago ya está saldado");
```

**Por qué funciona:** Las operaciones que escriben un pago usan findForUpdateById con PESSIMISTIC_WRITE. El bloqueo dura toda la transacción: la siguiente operación lee el importe que dejó la anterior. Se usa también al validar, confirmar, eliminar y emitir comprobantes para que esas escrituras no sobrescriban un abono concurrente.

**Comprobación:** ApplicationIT lanza ocho abonos de $10 simultáneamente contra PostgreSQL READ_COMMITTED. Todos completan y el importe final es exactamente $80.

## 12. Colisiones del número de comprobante

**Archivos:**
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/NumeroComprobanteService.java`
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java`
- `backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository/ComprobanteRepository.java`
- `backend/uca-cfc-connect/src/main/resources/db/migration/V5__sesiones_y_correlativos.sql`

**Por qué fallaba:** COUNT + 1 podía devolver el mismo correlativo a dos transacciones sobre pagos diferentes. La restricción UNIQUE evitaba duplicar el número, pero hacía fallar una emisión válida.

**Antes:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java` (línea 299).

```java
    private String generarNumero() {
        int anio = LocalDate.now(clock).getYear();
        String prefijo = PREFIJO_COMPROBANTE + "-" + anio + "-";
        long correlativo = comprobanteRepository.countByNumeroStartingWith(prefijo) + 1;
        return prefijo + String.format("%06d", correlativo);
    }
```

**Ahora:**

Archivo: `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/NumeroComprobanteService.java` (línea 16).

```java
    @Transactional
    public String siguiente(int anio) {
        Long numero = jdbc.queryForObject("""
                INSERT INTO comprobante_correlativos (anio, ultimo) VALUES (?, 1)
                ON CONFLICT (anio) DO UPDATE SET ultimo = comprobante_correlativos.ultimo + 1
                RETURNING ultimo
                """, Long.class, anio);
        return "CFC-" + anio + "-" + String.format("%06d", numero);
    }
}
```

**Por qué funciona:** Un contador por año usa INSERT ... ON CONFLICT ... DO UPDATE ... RETURNING. PostgreSQL serializa el incremento; JDBC participa de la transacción JPA del comprobante. La migración inicializa cada contador con el máximo número existente, sin renumerar comprobantes. Se elimina el método de conteo obsoleto. El bloqueo del pago impide emitir dos comprobantes para el mismo pago.

**Comprobación:** ApplicationIT emite seis comprobantes simultáneos sobre pagos distintos y verifica seis números únicos persistidos. Otra prueba lanza dos emisiones del mismo pago y comprueba que solo una tenga éxito, con rechazo de negocio para la segunda.

## Archivos de soporte y pruebas

- `backend/uca-cfc-connect/compose.test.yaml`: PostgreSQL dedicado a las pruebas en el puerto local 55432.
- `backend/uca-cfc-connect/src/main/resources/application-test.yml`: comentarios corregidos para reflejar que Flyway también carga datos iniciales.
- `backend/uca-cfc-connect/src/test/java/com/uca/cfc/integration/ApplicationIT.java`: 14 casos HTTP, persistencia y concurrencia.
- `backend/uca-cfc-connect/src/test/java/com/uca/cfc/integration/CorreoSmtpIT.java`: transporte SMTP local real.
- `backend/uca-cfc-connect/src/test/java/com/uca/cfc/security/UsuarioVigenteValidatorTest.java`: vigencia, revisión, rol, cuenta inactiva/eliminada y token antiguo.
- `backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/CorreoServiceTest.java`: composición, configuración y errores de correo.
- `backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/PagoServiceTest.java`: fixtures adaptadas al bloqueo y a la propiedad, más regresiones financieras.
- `frontend/uca-cfc-connect/src/test/regression.test.jsx`, `src/test/setup.js`, `vitest.config.js`: 14 pruebas de interfaz y configuración.
- `frontend/uca-cfc-connect/package.json` y `package-lock.json`: comando `npm test` y dependencias de pruebas fijadas.
- `README.md`: ejecución de pruebas completas, migración V5 y configuración SMTP.

## Reproducir

Con JDK 21 completo y Docker:

```bash
cd backend/uca-cfc-connect
docker compose -f compose.test.yaml up -d --wait
DATABASE_URL=jdbc:postgresql://localhost:55432/uca_cfc_connect_test sh ./mvnw "-Dtest=*Test,*IT" verify
docker compose -f compose.test.yaml down
```

Solo unitarias: `sh ./mvnw test`. En Windows usar `mvnw.cmd` y definir `DATABASE_URL` con la sintaxis de PowerShell.

```bash
cd frontend/uca-cfc-connect
npm ci
npm run lint
npm test
npm run build
```

## Actualización y correo

Flyway aplica V5 al iniciar sobre una base existente. No se modificaron las migraciones V1–V4. V5 agrega la revisión de cuenta, normaliza estados según importes, crea la restricción financiera e inicializa los correlativos a partir de comprobantes existentes. Tras actualizar, iniciar sesión nuevamente.

En `dev`, la pantalla de recuperación ofrece usar el código de prueba. Para producción, usar un perfil distinto de `dev`, configurar `SPRING_MAIL_HOST` y las variables SMTP/FRONTEND_URL de `.env.example`. Las credenciales se proporcionan mediante el entorno, sin guardarlas en el proyecto. El transporte SMTP requiere TLS por defecto.

## Inventario completo de archivos cambiados

| Tipo | Archivo |
| --- | --- |
| Modificado | `README.md` |
| Modificado | `backend/uca-cfc-connect/.env.example` |
| Nuevo | `backend/uca-cfc-connect/compose.test.yaml` |
| Modificado | `backend/uca-cfc-connect/pom.xml` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/controller/InscripcionController.java` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/entity/Usuario.java` |
| Nuevo | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/exception/CorreoNoDisponibleException.java` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository/ComprobanteRepository.java` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/repository/PagoRepository.java` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/JwtClaims.java` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/JwtService.java` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/SecurityConfig.java` |
| Nuevo | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/security/UsuarioVigenteValidator.java` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/CorreoService.java` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/EspacioService.java` |
| Nuevo | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/NumeroComprobanteService.java` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/PagoService.java` |
| Modificado | `backend/uca-cfc-connect/src/main/java/com/uca/cfc/service/UsuarioService.java` |
| Modificado | `backend/uca-cfc-connect/src/main/resources/application-dev.yml` |
| Modificado | `backend/uca-cfc-connect/src/main/resources/application-test.yml` |
| Modificado | `backend/uca-cfc-connect/src/main/resources/application.yml` |
| Nuevo | `backend/uca-cfc-connect/src/main/resources/db/migration/V5__sesiones_y_correlativos.sql` |
| Nuevo | `backend/uca-cfc-connect/src/test/java/com/uca/cfc/integration/ApplicationIT.java` |
| Nuevo | `backend/uca-cfc-connect/src/test/java/com/uca/cfc/integration/CorreoSmtpIT.java` |
| Nuevo | `backend/uca-cfc-connect/src/test/java/com/uca/cfc/security/UsuarioVigenteValidatorTest.java` |
| Nuevo | `backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/CorreoServiceTest.java` |
| Modificado | `backend/uca-cfc-connect/src/test/java/com/uca/cfc/service/PagoServiceTest.java` |
| Nuevo | `docs/correcciones-y-pruebas.md` |
| Nuevo | `docs/resultados-pruebas.json` |
| Modificado | `frontend/uca-cfc-connect/package-lock.json` |
| Modificado | `frontend/uca-cfc-connect/package.json` |
| Modificado | `frontend/uca-cfc-connect/src/pages/auth/Registro.jsx` |
| Modificado | `frontend/uca-cfc-connect/src/pages/catalogo/Catalogo.jsx` |
| Modificado | `frontend/uca-cfc-connect/src/pages/catering/Catering.jsx` |
| Modificado | `frontend/uca-cfc-connect/src/pages/cotizaciones/CotizacionNueva.jsx` |
| Modificado | `frontend/uca-cfc-connect/src/pages/cotizaciones/Cotizaciones.jsx` |
| Modificado | `frontend/uca-cfc-connect/src/pages/dashboard/StaffDashboard.jsx` |
| Modificado | `frontend/uca-cfc-connect/src/pages/pagos/Pagos.jsx` |
| Modificado | `frontend/uca-cfc-connect/src/pages/recursos/Recursos.jsx` |
| Nuevo | `frontend/uca-cfc-connect/src/test/regression.test.jsx` |
| Nuevo | `frontend/uca-cfc-connect/src/test/setup.js` |
| Modificado | `frontend/uca-cfc-connect/src/utils/navigation.js` |
| Modificado | `frontend/uca-cfc-connect/src/utils/permissions.js` |
| Nuevo | `frontend/uca-cfc-connect/vitest.config.js` |
