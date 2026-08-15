# UCA·CFC Connect

Sistema web empresarial para automatizar los procesos académicos y administrativos del **Centro de Formación Continua (CFC) de la UCA**: cursos y diplomados, inscripciones, cotizaciones, alquiler de espacios, catering, agenda institucional y pagos.

Proyecto de cátedra — **Desarrollo de Aplicaciones con Web Frameworks (DWF404)**, Escuela de Computación, Facultad de Ingeniería, Universidad Don Bosco. Año 2026.

## Equipo

| Integrante | Rol / módulo a cargo |
|---|---|
| Concepción Getsemaní Miranda Cuéllar | Contexto del proyecto, documento final, plan de trabajo |
| Dania Merari Urias Viscarra          | Modelo de dominio (diagrama de clases)                  |
| David Ezequiel Alvarado Alvarado     | Modelo de base de datos (diagrama relacional)           |
| Dominic Alejandro Castillo Gónzales  | Definición de endpoints REST                            |
| Bryan Ernesto Anaya Brizuela         | Mockups, flujos de proceso, estructura del repositorio  |

## Estructura del repositorio

```
DWF_proyecto_uca-cfc/
├── backend/                  # Código fuente Spring Boot
├── docs/
│   ├── analisis-diseno.md    # Problemática, objetivos, alcance, flujos
│   ├── diagramas/
│   │   ├── clases/           # Modelo de dominio
│   │   └── base-datos/       # Modelo relacional
│   ├── endpoints/            # Definición de endpoints REST por módulo
│   └── mockups/              # Mockups .SVG/Figma de las pantallas principales
│   └── documentacion/        # Archivos y documentos .pdf/.docx
├── .github/
│   └── PULL_REQUEST_TEMPLATE.md
├── .gitignore
└── README.md
```

## Módulos del sistema

1. Gestión académica (cursos, diplomados)
2. Gestión de clientes
3. Inscripciones
4. Cotizaciones
5. Alquiler de espacios
6. Catering
7. Agenda institucional
8. Pagos
9. Seguridad (usuarios, roles, JWT)



## Flujo de trabajo en Git

- `main` — rama protegida, siempre desplegable, refleja el estado entregado en cada fase.
- `develop` — rama de integración, donde se juntan los módulos antes de pasar a `main`.
- `feature/<modulo>-<nombre>` — una rama por funcionalidad y por integrante, ej. `feature/inscripciones-bryan`.

Cada integrante trabaja en su rama `feature/...` y abre un **Pull Request hacia `develop`**. Ningún PR se mergea sin al menos una revisión de otro integrante del equipo.

## Gobernanza y reglas del repositorio

Para mantener un estándar más cercano a entorno empresarial (restricciones, revisiones obligatorias y validaciones automáticas), revisar y aplicar:

- `docs/gobernanza-repositorio.md`
- `.github/workflows/policy-checks.yml`
- `.github/CODEOWNERS`
