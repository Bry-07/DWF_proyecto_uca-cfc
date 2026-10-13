# UCA·CFC Connect

Sistema web empresarial para automatizar los procesos académicos y administrativos del **Centro de Formación Continua (CFC) de la UCA**: cursos y diplomados, inscripciones, cotizaciones, alquiler de espacios, catering, agenda institucional y pagos.

Proyecto de cátedra — **Desarrollo de Aplicaciones con Web Frameworks (DWF404)**, Escuela de Computación, Facultad de Ingeniería, Universidad Don Bosco. Año 2026.

## Tecnologías

Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL 16 · Flyway · Swagger (springdoc-openapi) · Maven (wrapper incluido).

## Inicio rápido

Todo el código vive en `backend/`. La guía completa (base de datos, variables de entorno, ejecución, Swagger, credenciales de prueba, reglas de negocio y pruebas) está en [`backend/README.md`](backend/README.md).

```bash
cd backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

## Equipo

| Integrante | Carné | Rol / módulo a cargo (Fase 1) |
|---|---|---|
| Concepción Getsemaní Miranda Cuéllar | MC250288 | Contexto del proyecto, documento final, plan de trabajo |
| Dania Merari Urias Viscarra | UV253195 | Modelo de dominio (diagrama de clases) |
| David Ezequiel Alvarado Alvarado | AA250233 | Modelo de base de datos (diagrama relacional) |
| Dominic Alejandro Castillo Gonzales | CG251726 | Definición de endpoints REST |
| Bryan Ernesto Anaya Brizuela | AB250136 | Mockups, flujos de proceso, estructura del repositorio |

**Docente:** Ing. Yesenia Escobar

Las responsabilidades de la Fase 2 (implementación) están en la sección 17 de [`backend/README.md`](backend/README.md).

## Módulos del sistema

1. Gestión académica (cursos, diplomados)
2. Gestión de clientes
3. Inscripciones
4. Cotizaciones
5. Alquiler de espacios
6. Catering
7. Agenda institucional
8. Pagos
9. Seguridad (usuarios y roles; JWT en la Fase 3)

## Estructura del repositorio


## Flujo de trabajo en Git

- `main` — rama protegida, siempre desplegable, refleja lo entregado en cada fase. Solo recibe PR desde `develop` o `hotfix/*`.
- `develop` — rama de integración, donde se juntan los módulos antes de pasar a `main`.
- `feature/<modulo>-<nombre>` — una rama por funcionalidad y por integrante, ej. `feature/inscripciones-bryan`. También se aceptan `fix/*`, `chore/*`, `docs/*` y `hotfix/*`.

Cada integrante trabaja en su rama y abre un **Pull Request hacia `develop`**, usando la plantilla del repositorio. Ningún PR se fusiona sin al menos una revisión aprobada.

**Título del PR:** debe seguir Conventional Commits, por ejemplo `feat(inscripciones): agrega validación de cupos`. El check automático lo rechaza si no cumple.

Guía paso a paso: [`docs/flujo-trabajo-develop-hotfix.md`](docs/flujo-trabajo-develop-hotfix.md).

## Gobernanza y reglas del repositorio

- [`docs/gobernanza-repositorio.md`](docs/gobernanza-repositorio.md) — protección de ramas y estándar de PR
- [`docs/flujo-trabajo-develop-hotfix.md`](docs/flujo-trabajo-develop-hotfix.md) — flujo de trabajo para `develop` y `hotfix`
- [`.github/workflows/policy-checks.yml`](.github/workflows/policy-checks.yml) — validaciones automáticas
- [`.github/CODEOWNERS`](.github/CODEOWNERS) — responsables de revisión

## Enlaces

- Prototipo y recursos de diseño en Figma: ver [`docs/mockups/Enlaces.txt`](docs/mockups/Enlaces.txt)
- Documento de la Fase 1: [`docs/documentacion/`](docs/documentacion/)