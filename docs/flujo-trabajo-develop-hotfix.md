# Flujo de trabajo paso a paso (develop y hotfix)

Esta guía explica, de forma literal y simple, los pasos que debe seguir cualquier integrante para subir cambios sin romper el orden del repositorio.

Todos los pasos se explican usando **GitHub Desktop** (la app con interfaz gráfica). Si alguien prefiere usar la terminal, se deja el comando equivalente como referencia opcional.

> 📥 Si aún no tienes GitHub Desktop: descárgalo en https://desktop.github.com/, ábrelo, inicia sesión con tu cuenta de GitHub y usa **File → Clone repository** para clonar `Bry-07/DWF_proyecto_uca-cfc`.

---

## A. Cuando tu cambio es normal (va a `develop`)

### Paso 1: actualizar tu repositorio local
1. Abre GitHub Desktop.
2. Arriba a la izquierda, en **Current branch**, selecciona `develop`.
3. Haz clic en **Fetch origin** (o **Pull origin** si aparece) para traer los cambios más recientes.

**Terminal (opcional):**
```bash
git checkout develop
git pull origin develop
```

### Paso 2: crear tu rama de trabajo
1. En GitHub Desktop, haz clic en **Current branch → New branch**.
2. Asegúrate de que dice "based on develop" (si no, primero repite el Paso 1).
3. Ponle nombre con este formato: `feature/<modulo>-<nombre>` (o `fix/...`, `docs/...`, `chore/...`).
4. Haz clic en **Create branch**. GitHub Desktop te deja automáticamente parado en tu rama nueva.

**Ejemplo:** `feature/inscripciones-maria`

**Terminal (opcional):**
```bash
git checkout -b feature/inscripciones-maria
```

### Paso 3: hacer tus cambios
1. Edita los archivos que necesites en tu editor (VS Code, etc.).
2. Guarda los archivos.
3. Vuelve a GitHub Desktop: en la pestaña **Changes** verás la lista de archivos modificados, con lo agregado en verde y lo eliminado en rojo.
4. Revisa esa lista para confirmar que solo aparecen los cambios que realmente hiciste.

### Paso 4: crear commit
1. En la parte inferior izquierda de GitHub Desktop hay dos campos de texto.
2. En el primero (título del commit) escribe el mensaje con la convención: `feat:`, `fix:`, `docs:`, `chore:`.
3. El segundo campo (descripción) es opcional, para detalles extra.
4. Haz clic en **Commit to feature/...** (el nombre de tu rama aparece en el botón).

**Ejemplo:** `feat(inscripciones): agrega validación de cupos`

**Terminal (opcional):**
```bash
git add .
git commit -m "feat(inscripciones): agrega validación de cupos"
```

### Paso 5: subir tu rama
1. Después de hacer commit, haz clic en el botón **Publish branch** (arriba, centro de la ventana).
2. Si ya la habías publicado antes, el botón dirá **Push origin** — haz clic ahí.

**Terminal (opcional):**
```bash
git push origin feature/inscripciones-maria
```

### Paso 6: abrir Pull Request a `develop`
1. En GitHub Desktop, después de publicar la rama, haz clic en **Create Pull Request** (abre el navegador).
2. En GitHub (la página web), verifica que quede así:
   - **base:** `develop`
   - **compare:** tu rama `feature/...`
3. Completa la plantilla del PR.
4. Pide revisión a un compañero.

### Paso 7: corregir observaciones (si aplica)
1. Si piden cambios, edítalos en tu editor.
2. En GitHub Desktop repite el Paso 3 y 4 (Changes → commit).
3. Haz clic en **Push origin** para subir el nuevo commit.
4. El PR se actualiza automáticamente, no hay que crear uno nuevo.

### Paso 8: merge a `develop`
1. Cuando el PR esté aprobado y en verde (checks pasados), se puede hacer merge desde GitHub (web).
2. No hagas push directo a `develop`.

---

## B. Cuando es una corrección urgente (flujo `hotfix/*`)

> Usa este flujo solo para arreglos urgentes que deben salir rápido y llegar a `main`.

### Paso 1: crear rama `hotfix/*` desde `main`
1. En GitHub Desktop, en **Current branch**, selecciona `main`.
2. Haz clic en **Fetch origin** para actualizarla.
3. Haz clic en **Current branch → New branch**, confirma que dice "based on main".
4. Nómbrala `hotfix/<descripcion-corta>` y haz clic en **Create branch**.

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
1. En GitHub Desktop, ve a **Changes**, escribe el mensaje del commit y haz clic en **Commit to hotfix/...**.

**Ejemplo:** `fix(seguridad): corrige expiración de sesión`

### Paso 4: subir la rama hotfix
1. Haz clic en **Publish branch**.

**Terminal (opcional):**
```bash
git push origin hotfix/login-timeout
```

### Paso 5: abrir PR de `hotfix/*` hacia `main`
1. En GitHub Desktop, clic en **Create Pull Request**.
2. En GitHub (web), confirma:
   - **base:** `main`
   - **compare:** `hotfix/...`
3. Completa plantilla y solicita revisión.
4. Espera checks y aprobación.

### Paso 6: abrir PR de sincronización hacia `develop`
1. Después de mergear en `main`, en GitHub Desktop cambia **Current branch** a `develop`, haz **Fetch origin**.
2. Crea una nueva rama basada en `develop` si necesitas reaplicar el fix, o abre directamente un PR con:
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
