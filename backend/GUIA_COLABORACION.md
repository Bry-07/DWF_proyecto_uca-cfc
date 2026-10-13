# Guía de colaboración — UCA·CFC Connect

Guía para el equipo sobre cómo trabajar en el repositorio usando **GitHub Desktop**. Incluye alternativa por terminal para quien la necesite.

Repositorio: https://github.com/Bry-07/DWF_proyecto_uca-cfc.git

---

## 0. Requisito previo: ser colaborador del repo

El dueño del repositorio debe agregar a cada integrante como colaborador antes de que puedan pushear nada:

**Settings → Collaborators and teams → Add people** → agregar el usuario de GitHub de cada integrante con permiso de escritura.

---

## 1. Instalar GitHub Desktop y clonar el repositorio

1. Descargar e instalar **GitHub Desktop** desde [desktop.github.com](https://desktop.github.com) e iniciar sesión con su cuenta de GitHub.
2. **File → Clone repository...**
3. Pestaña **URL**, pegar:
   ```
   https://github.com/Bry-07/DWF_proyecto_uca-cfc.git
   ```
4. Elegir la carpeta donde se guardará en su computadora → **Clone**.

<details>
<summary>Alternativa por terminal</summary>

```bash
git clone https://github.com/Bry-07/DWF_proyecto_uca-cfc.git
cd DWF_proyecto_uca-cfc
```
</details>

---

## 2. Cambiarse a la rama `develop`

Por defecto queda seleccionada `main`. Hay que moverse a `develop` antes de crear una rama nueva:

1. Arriba, clic en **Current branch**.
2. Seleccionar **develop** de la lista.

<details>
<summary>Alternativa por terminal</summary>

```bash
git checkout develop
git pull origin develop
```
</details>

---

## 3. Crear la rama de trabajo propia

1. Con `develop` seleccionada: **Current branch → New branch**.
2. Nombrarla con la convención `feature/<modulo>-<nombre>`, por ejemplo:
   - `feature/modelo-dominio-juan`
   - `feature/modelo-bd-maria`
   - `feature/endpoints-carlos`
3. Confirmar que diga **"based on develop"**.
4. Clic en **Create branch** (GitHub Desktop cambia automáticamente a la rama nueva).

<details>
<summary>Alternativa por terminal</summary>

```bash
git checkout -b feature/modelo-dominio-juan
```
</details>

---

## 4. Agregar los archivos

Guardar los archivos en la carpeta correspondiente del repo clonado:

| Módulo | Carpeta |
|---|---|
| Modelo de dominio (Persona 2) | `docs/diagramas/clases/` |
| Modelo de base de datos (Persona 3) | `docs/diagramas/base-datos/` |
| Endpoints REST (Persona 4) | `docs/endpoints/` |
| Documento de análisis (Persona 1) | `docs/analisis-diseno.md` |
| Mockups y flujos (Persona 5) | `docs/mockups/` |

En GitHub Desktop, los archivos nuevos aparecen automáticamente en la pestaña **Changes** — no hace falta ningún `git add` manual.

---

## 5. Hacer el commit

1. En **Summary** (abajo a la izquierda), escribir un mensaje corto, ej:
   ```
   docs(modelo-dominio): agrega diagrama de clases inicial
   ```
2. (Opcional) Agregar más detalle en **Description**.
3. Clic en **Commit to feature/...**

<details>
<summary>Alternativa por terminal</summary>

```bash
git add .
git commit -m "docs(modelo-dominio): agrega diagrama de clases inicial"
```
</details>

---

## 6. Subir la rama a GitHub

Clic en **Publish branch** (primera vez) o **Push origin** (si la rama ya existía remotamente).

<details>
<summary>Alternativa por terminal</summary>

```bash
git push origin feature/modelo-dominio-juan
```
</details>

---

## 7. Crear el Pull Request

1. Desde GitHub Desktop, clic en **Create Pull Request** (o ir a la web del repo, donde aparece el aviso **"Compare & pull request"**).
2. Verificar que quede: **base: develop** ← **compare: feature/su-rama**.
3. Completar la plantilla del PR: descripción, módulo, cómo probar.
4. Asignar a un compañero como **Reviewer**.
5. Clic en **Create pull request**.

---

## 8. Esperar aprobación y mergear

Una vez que un compañero revise y apruebe el PR, cualquiera con permiso puede darle **Merge pull request** desde GitHub web.

---

## Antes de crear cada rama nueva

Repetir el paso 2: cambiarse a `develop` y darle **Fetch origin / Pull origin** para traer los últimos cambios ya mergeados. Así cada rama nueva parte siempre de la versión más reciente de `develop` y se evitan conflictos innecesarios.

## Convención de commits

`feat:` nueva funcionalidad · `fix:` corrección · `docs:` documentación · `refactor:` cambio de código sin alterar comportamiento · `test:` pruebas · `chore:` tareas de mantenimiento

## Tener en cuenta que la estructura se ve así

```
DWF_proyecto_uca-cfc/
├── backend/                  # Código fuente Spring Boot
├── docs/
│   ├── analisis-diseno.md    # Problemática, objetivos, alcance, flujos
│   ├── diagramas/
│   │   ├── clases/           # Modelo de dominio
│   │   └── base-datos/       # Modelo relacional
│   ├── endpoints/            # Definición de endpoints REST por módulo
│   ├── mockups/              # Mockups .SVG/Figma de las pantallas principales
│   └── documentacion/        # Archivos y documentos .pdf/.docx/.md
├── .github/
│   └── PULL_REQUEST_TEMPLATE.md
├── .gitignore
└── README.md
```
Intenten que los archivos que suban sigan este orden

## Y a la hora de hacer el pull request les saldrá esta plantilla:

## Descripción

<!-- ¿Qué módulo o funcionalidad agrega/modifica este PR? Literalmente deben poner en qué se basa el PR, por ejemplo: Gestión de clientes, Gestión de espacios, etc. -->

## Módulo

<!-- Deben seleccionarse los módulos que se van a modificar, por ejemplo: Gestión de clientes, Gestión de espacios, etc. -->

- [ ] Gestión académica
- [ ] Gestión de clientes
- [ ] Inscripciones
- [ ] Cotizaciones
- [ ] Alquiler de espacios
- [ ] Catering
- [ ] Agenda institucional
- [ ] Pagos
- [ ] Seguridad

## Checklist

<!-- Deben seleccionarse las casillas que se van a verificar, por ejemplo: El código compila y corre localmente (`./mvnw spring-boot:run`), Se agregaron/actualizaron pruebas unitarias si aplica, Los endpoints nuevos están documentados en Swagger, Se validaron los DTOs de entrada (`@Valid`, `@NotBlank`, etc.), No rompe funcionalidades existentes en `develop` -->

- [ ] El código compila y corre localmente (`./mvnw spring-boot:run`)
- [ ] Se agregaron/actualizaron pruebas unitarias si aplica
- [ ] Los endpoints nuevos están documentados en Swagger
- [ ] Se validaron los DTOs de entrada (`@Valid`, `@NotBlank`, etc.)
- [ ] No rompe funcionalidades existentes en `develop`

## Cómo probar

<!-- Pasos para que el revisor pruebe el cambio y sepa que está funcionando y cómo debe funcionar sin romper nada -->

## Rama base

<!-- Deben ponerse la rama base, por ejemplo: develop aunque estén en la rama derivada de esta por ejemplo: feature/endpoints-carlos -->
`develop`
