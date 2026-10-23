# Gobernanza del repositorio (nivel empresarial)

Este documento define reglas para evitar cambios directos sin control y mantener orden de trabajo en equipo.

## 1) Flujo oficial de ramas

- `main`: solo recibe cambios desde `develop` o `hotfix/*` por Pull Request.
- `develop`: integra trabajo diario de ramas `feature/*`, `fix/*`, `chore/*`, `docs/*`, `hotfix/*`.
- Rama personal por tarea: `feature/<modulo>-<nombre>`.

## 2) Reglas obligatorias en GitHub (Branch protection)

Configurar **dos reglas**, una para `main` y otra para `develop`.

### Regla para `main`

Activar en **Settings → Branches → Add branch protection rule**:

1. **Branch name pattern**: `main`
2. **Require a pull request before merging**
   - Required approvals: `1` (mínimo)
   - Dismiss stale pull request approvals when new commits are pushed
   - Require review from Code Owners
3. **Require status checks to pass before merging**
   - Check requerido: `Validate branch flow and PR title` (workflow `Policy checks`)
4. **Require conversation resolution before merging**
5. **Restrict who can push to matching branches**
   - Permitir solo administradores o release manager
6. (Opcional recomendado) **Require linear history**
7. (Opcional recomendado) **Do not allow bypassing the above settings**

### Regla para `develop`

1. **Branch name pattern**: `develop`
2. **Require a pull request before merging**
   - Required approvals: `1`
3. **Require status checks to pass before merging**
   - Check requerido: `Validate branch flow and PR title`
4. **Require conversation resolution before merging**
5. **Restrict who can push to matching branches** (recomendado)

## 3) Estándar de Pull Request

- Título en formato Conventional Commits:
  - `feat(modulo): descripción`
  - `fix(modulo): descripción`
  - `docs(modulo): descripción`
- Usar la plantilla existente de PR.
- Todo PR debe indicar módulo afectado y cómo probar.

## 4) Responsabilidad por cambios

Se agregó `CODEOWNERS` para que GitHub pueda exigir revisión del owner designado cuando se activa la opción **Require review from Code Owners**.

## 5) Workflow de validación

Se agregó `.github/workflows/policy-checks.yml` que valida automáticamente:

- Estrategia de ramas (qué ramas pueden abrir PR a `main` y `develop`).
- Convención del título del PR.

Si falla, el PR no debe mergearse hasta corregirlo.
