# UCA·CFC Connect

Sistema web empresarial para automatizar los procesos académicos y administrativos del **Centro de Formación Continua (CFC) de la UCA**: cursos y diplomados, inscripciones, cotizaciones, alquiler de espacios, catering, agenda institucional y pagos.

Proyecto de cátedra — **Desarrollo de Software Empresarial (DWF)**, Escuela de Computación, Facultad de Ingeniería, Universidad Don Bosco. Año 2026.

## Equipo

| Integrante | Rol / módulo a cargo |
|---|---|
| Persona 1 | Contexto del proyecto, documento final, plan de trabajo |
| Persona 2 | Modelo de dominio (diagrama de clases) |
| Persona 3 | Modelo de base de datos (diagrama relacional) |
| Persona 4 | Definición de endpoints REST |
| Bryan — Persona 5 | Mockups, flujos de proceso, estructura del repositorio |

## Stack técnico

- **Backend:** Java 21, Spring Boot, Spring MVC, Spring Data JPA, Hibernate, Spring Security + JWT, Maven
- **Base de datos:** MySQL o PostgreSQL
- **Documentación API:** Springdoc-OpenAPI (Swagger)
- **Pruebas:** JUnit 5, Mockito
- **Validación:** javax.validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`, `@Pattern`, `@Positive`)
- **Manejo de errores:** `@ControllerAdvice` + `@ExceptionHandler`

## Estructura del repositorio

```
uca-cfc-connect/
├── backend/                  # Código fuente Spring Boot
├── docs/
│   ├── analisis-diseno.md    # Problemática, objetivos, alcance, flujos
│   ├── diagramas/
│   │   ├── clases/           # Modelo de dominio
│   │   └── base-datos/       # Modelo relacional
│   ├── endpoints/            # Definición de endpoints REST por módulo
│   └── mockups/              # Mockups HTML/Figma de las pantallas principales
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

## Cómo levantar el proyecto (backend)

```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080` y la documentación Swagger en `http://localhost:8080/swagger-ui.html`.

## Flujo de trabajo en Git

- `main` — rama protegida, siempre desplegable, refleja el estado entregado en cada fase.
- `develop` — rama de integración, donde se juntan los módulos antes de pasar a `main`.
- `feature/<modulo>-<nombre>` — una rama por funcionalidad y por integrante, ej. `feature/inscripciones-bryan`.

Cada integrante trabaja en su rama `feature/...` y abre un **Pull Request hacia `develop`**. Ningún PR se mergea sin al menos una revisión de otro integrante del equipo.

```bash
git checkout develop
git pull origin develop
git checkout -b feature/agenda-bryan
# ... cambios ...
git add .
git commit -m "feat(agenda): agrega vista de calendario semanal"
git push origin feature/agenda-bryan
```

Convención de commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.

## Fases de entrega

| Fase | Entregable | Fecha |
|---|---|---|
| 1 — Planificación y diseño (10%) | Documento de análisis, plan de trabajo, repo inicial | 14 ago |
| 2 — Primer avance (15%) | Base de datos, CRUD, JPA, validaciones, Swagger | 11 sep |
| 3 — Proyecto funcional y defensa (20%) | Sistema completo, seguridad, pruebas unitarias | 17 oct |
