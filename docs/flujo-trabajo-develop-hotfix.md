# Flujo de trabajo paso a paso (develop y hotfix)

Esta guía explica, de forma literal y simple, los pasos que debe seguir cualquier integrante para subir cambios sin romper el orden del repositorio.

---

## A. Cuando tu cambio es normal (va a `develop`)

### Paso 1: actualizar tu repositorio local
1. Abre tu copia del repositorio.
2. Cambia a la rama `develop`.
3. Trae los cambios más recientes de `develop`.

**Terminal (opcional):**
```bash
git checkout develop
git pull origin develop
```

### Paso 2: crear tu rama de trabajo
1. Crea una rama nueva basada en `develop`.
2. Usa este formato: `feature/<modulo>-<nombre>` (o `fix/...`, `docs/...`, `chore/...`).

**Ejemplo:** `feature/inscripciones-maria`

**Terminal (opcional):**
```bash
git checkout -b feature/inscripciones-maria
```

### Paso 3: hacer tus cambios
1. Agrega o modifica archivos.
2. Revisa que lo que cambiaste esté correcto.

### Paso 4: crear commit
1. Haz commit con mensaje claro.
2. Usa convención recomendada: `feat:`, `fix:`, `docs:`, `chore:`.

**Ejemplo:** `feat(inscripciones): agrega validación de cupos`

**Terminal (opcional):**
```bash
git add .
git commit -m "feat(inscripciones): agrega validación de cupos"
```

### Paso 5: subir tu rama
1. Publica tu rama en GitHub.

**Terminal (opcional):**
```bash
git push origin feature/inscripciones-maria
```

### Paso 6: abrir Pull Request a `develop`
1. En GitHub, abre un Pull Request.
2. Verifica que quede así:
   - **base:** `develop`
   - **compare:** tu rama `feature/...`
3. Completa la plantilla del PR.
4. Pide revisión a un compañero.

### Paso 7: corregir observaciones (si aplica)
1. Si piden cambios, hazlos en la misma rama.
2. Vuelve a hacer commit y push.
3. El PR se actualiza automáticamente.

### Paso 8: merge a `develop`
1. Cuando el PR esté aprobado y en verde, se puede hacer merge.
2. No hagas push directo a `develop`.

---

## B. Cuando es una corrección urgente (flujo `hotfix/*`)

> Usa este flujo solo para arreglos urgentes que deben salir rápido y llegar a `main`.

### Paso 1: crear rama `hotfix/*` desde `main`
1. Cambia a `main` local.
2. Actualiza `main`.
3. Crea rama `hotfix/<descripcion-corta>`.

**Ejemplo:** `hotfix/login-timeout`

**Terminal (opcional):**
```bash
git checkout main
git pull origin main
git checkout -b hotfix/login-timeout
```

### Paso 2: aplicar el fix
1. Haz solo el cambio urgente necesario.
2. Evita mezclar mejoras no relacionadas.

### Paso 3: commit del hotfix
1. Guarda con mensaje claro.

**Ejemplo:** `fix(seguridad): corrige expiración de sesión`

### Paso 4: subir la rama hotfix
1. Publica `hotfix/...` en GitHub.

**Terminal (opcional):**
```bash
git push origin hotfix/login-timeout
```

### Paso 5: abrir PR de `hotfix/*` hacia `main`
1. Crea PR con:
   - **base:** `main`
   - **compare:** `hotfix/...`
2. Completa plantilla y solicita revisión.
3. Espera checks y aprobación.

### Paso 6: abrir PR de sincronización hacia `develop`
1. Después de mergear en `main`, crea otro PR para que el fix también llegue a `develop`.
2. Usa:
   - **base:** `develop`
   - **compare:** `hotfix/...` (o rama equivalente con el fix)

> Este paso evita que `develop` se quede sin la corrección urgente.

---

## C. Reglas que todos deben respetar

1. No hacer push directo a `main`.
2. No hacer push directo a `develop`.
3. Todo cambio entra por Pull Request.
4. Todo PR debe pasar checks automáticos.
5. Todo PR debe tener al menos una revisión aprobada.
6. Usar nombres de rama y títulos de PR con formato acordado.

---

## D. Resumen rápido de decisión

- ¿Es trabajo normal de módulo? → `develop` con rama `feature/*`.
- ¿Es error urgente en producción o entrega final? → `hotfix/*` hacia `main` y luego sincronizar a `develop`.
