-- =====================================================================
-- UCA-CFC Connect · Script completo de la base de datos
-- Universidad Don Bosco · Desarrollo de Software Empresarial · Fase 2
--
-- Este archivo reúne, en orden, las dos migraciones que Flyway aplica
-- automáticamente al arrancar la aplicación. Se incluye aparte para
-- poder recrear la base de datos a mano, sin ejecutar el proyecto:
--
--   psql -U cfc_app -d uca_cfc_connect -f db/esquema_completo.sql
--
-- Si la base se crea con este script en lugar de dejar que lo haga
-- Flyway, la aplicacion marcara las migraciones como ya aplicadas
-- gracias a baseline-on-migrate.
-- =====================================================================

-- ---------- V1: esquema ----------

-- =====================================================================
-- UCA-CFC Connect · Esquema inicial
-- Centro de Formación Continua (CFC) — Universidad Centroamericana
-- =====================================================================

-- ---------------------------------------------------------------------
-- Seguridad
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_roles_nombre UNIQUE (nombre)
);

CREATE TABLE usuarios (
    id            BIGSERIAL PRIMARY KEY,
    nombre        VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    estado        VARCHAR(20)  NOT NULL,
    rol_id        BIGINT       NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles (id),
    CONSTRAINT ck_usuarios_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX ix_usuarios_rol ON usuarios (rol_id);

CREATE TABLE refresh_tokens (
    id                BIGSERIAL PRIMARY KEY,
    token             VARCHAR(255) NOT NULL,
    usuario_id        BIGINT       NOT NULL,
    fecha_expiracion  TIMESTAMP    NOT NULL,
    revocado          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX ix_refresh_tokens_usuario ON refresh_tokens (usuario_id);

-- ---------------------------------------------------------------------
-- Catálogos académicos
-- ---------------------------------------------------------------------
CREATE TABLE categorias (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_categorias_nombre UNIQUE (nombre)
);

CREATE TABLE modalidades (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(50) NOT NULL,
    tipo        VARCHAR(20) NOT NULL,
    descripcion VARCHAR(255),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_modalidades_nombre UNIQUE (nombre),
    CONSTRAINT ck_modalidades_tipo CHECK (tipo IN ('PRESENCIAL', 'VIRTUAL', 'HIBRIDA'))
);

CREATE TABLE docentes (
    id           BIGSERIAL PRIMARY KEY,
    nombres      VARCHAR(100) NOT NULL,
    apellidos    VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    telefono     VARCHAR(20),
    especialidad VARCHAR(120) NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    CONSTRAINT uk_docentes_email UNIQUE (email)
);

CREATE INDEX ix_docentes_especialidad ON docentes (especialidad);

-- ---------------------------------------------------------------------
-- Espacios físicos
-- ---------------------------------------------------------------------
CREATE TABLE espacios (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(100)   NOT NULL,
    tipo            VARCHAR(20)    NOT NULL,
    capacidad       INTEGER        NOT NULL,
    precio_por_hora NUMERIC(10, 2) NOT NULL,
    equipamiento    VARCHAR(500),
    ubicacion       VARCHAR(150),
    estado          VARCHAR(20)    NOT NULL,
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP      NOT NULL,
    CONSTRAINT uk_espacios_nombre UNIQUE (nombre),
    CONSTRAINT ck_espacios_tipo CHECK (tipo IN ('AUDITORIO', 'SALA', 'LABORATORIO', 'AULA', 'SALA_MULTIMEDIA')),
    CONSTRAINT ck_espacios_estado CHECK (estado IN ('DISPONIBLE', 'MANTENIMIENTO', 'INACTIVO')),
    CONSTRAINT ck_espacios_capacidad CHECK (capacidad > 0),
    CONSTRAINT ck_espacios_precio CHECK (precio_por_hora >= 0)
);

CREATE INDEX ix_espacios_tipo ON espacios (tipo);

-- ---------------------------------------------------------------------
-- Oferta académica: cursos y diplomados comparten estructura
-- ---------------------------------------------------------------------
CREATE TABLE cursos (
    id             BIGSERIAL PRIMARY KEY,
    codigo         VARCHAR(20)    NOT NULL,
    nombre         VARCHAR(150)   NOT NULL,
    descripcion    VARCHAR(1000),
    categoria_id   BIGINT         NOT NULL,
    modalidad_id   BIGINT         NOT NULL,
    docente_id     BIGINT,
    espacio_id     BIGINT,
    fecha_inicio   DATE           NOT NULL,
    fecha_fin      DATE           NOT NULL,
    horario        VARCHAR(100),
    duracion_horas INTEGER        NOT NULL,
    cupo_maximo    INTEGER        NOT NULL,
    precio         NUMERIC(10, 2) NOT NULL,
    estado         VARCHAR(20)    NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    CONSTRAINT uk_cursos_codigo UNIQUE (codigo),
    CONSTRAINT fk_cursos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_cursos_modalidad FOREIGN KEY (modalidad_id) REFERENCES modalidades (id),
    CONSTRAINT fk_cursos_docente   FOREIGN KEY (docente_id)   REFERENCES docentes (id),
    CONSTRAINT fk_cursos_espacio   FOREIGN KEY (espacio_id)   REFERENCES espacios (id),
    CONSTRAINT ck_cursos_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_cursos_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ck_cursos_cupo CHECK (cupo_maximo > 0),
    CONSTRAINT ck_cursos_duracion CHECK (duracion_horas > 0),
    CONSTRAINT ck_cursos_precio CHECK (precio >= 0)
);

CREATE INDEX ix_cursos_categoria ON cursos (categoria_id);
CREATE INDEX ix_cursos_modalidad ON cursos (modalidad_id);
CREATE INDEX ix_cursos_docente   ON cursos (docente_id);
CREATE INDEX ix_cursos_estado    ON cursos (estado);

CREATE TABLE diplomados (
    id             BIGSERIAL PRIMARY KEY,
    codigo         VARCHAR(20)    NOT NULL,
    nombre         VARCHAR(150)   NOT NULL,
    descripcion    VARCHAR(1000),
    categoria_id   BIGINT         NOT NULL,
    modalidad_id   BIGINT         NOT NULL,
    docente_id     BIGINT,
    espacio_id     BIGINT,
    fecha_inicio   DATE           NOT NULL,
    fecha_fin      DATE           NOT NULL,
    horario        VARCHAR(100),
    duracion_horas INTEGER        NOT NULL,
    cupo_maximo    INTEGER        NOT NULL,
    precio         NUMERIC(10, 2) NOT NULL,
    estado         VARCHAR(20)    NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    CONSTRAINT uk_diplomados_codigo UNIQUE (codigo),
    CONSTRAINT fk_diplomados_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_diplomados_modalidad FOREIGN KEY (modalidad_id) REFERENCES modalidades (id),
    CONSTRAINT fk_diplomados_docente   FOREIGN KEY (docente_id)   REFERENCES docentes (id),
    CONSTRAINT fk_diplomados_espacio   FOREIGN KEY (espacio_id)   REFERENCES espacios (id),
    CONSTRAINT ck_diplomados_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_diplomados_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ck_diplomados_cupo CHECK (cupo_maximo > 0),
    CONSTRAINT ck_diplomados_duracion CHECK (duracion_horas > 0),
    CONSTRAINT ck_diplomados_precio CHECK (precio >= 0)
);

CREATE INDEX ix_diplomados_categoria ON diplomados (categoria_id);
CREATE INDEX ix_diplomados_modalidad ON diplomados (modalidad_id);
CREATE INDEX ix_diplomados_docente   ON diplomados (docente_id);
CREATE INDEX ix_diplomados_estado    ON diplomados (estado);

-- ---------------------------------------------------------------------
-- Clientes
-- ---------------------------------------------------------------------
CREATE TABLE empresas (
    id              BIGSERIAL PRIMARY KEY,
    razon_social    VARCHAR(200) NOT NULL,
    giro            VARCHAR(150),
    contacto_nombre VARCHAR(150),
    contacto_cargo  VARCHAR(100),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

CREATE TABLE clientes (
    id         BIGSERIAL PRIMARY KEY,
    tipo       VARCHAR(20)  NOT NULL,
    nombre     VARCHAR(150) NOT NULL,
    dui        VARCHAR(10),
    nit        VARCHAR(17),
    email      VARCHAR(150) NOT NULL,
    telefono   VARCHAR(20),
    direccion  VARCHAR(255),
    empresa_id BIGINT,
    usuario_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_clientes_email   UNIQUE (email),
    CONSTRAINT uk_clientes_dui     UNIQUE (dui),
    CONSTRAINT uk_clientes_nit     UNIQUE (nit),
    CONSTRAINT uk_clientes_empresa UNIQUE (empresa_id),
    CONSTRAINT uk_clientes_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_clientes_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id),
    CONSTRAINT fk_clientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_clientes_tipo CHECK (tipo IN ('PERSONA_PARTICULAR', 'EMPRESA')),
    -- Una persona particular se identifica con DUI; una empresa con NIT y su ficha de empresa
    CONSTRAINT ck_clientes_identificacion CHECK (
        (tipo = 'PERSONA_PARTICULAR' AND dui IS NOT NULL AND empresa_id IS NULL)
        OR (tipo = 'EMPRESA' AND nit IS NOT NULL AND empresa_id IS NOT NULL)
    )
);

CREATE INDEX ix_clientes_nombre ON clientes (LOWER(nombre));
CREATE INDEX ix_clientes_tipo   ON clientes (tipo);

-- ---------------------------------------------------------------------
-- Inscripciones
-- ---------------------------------------------------------------------
CREATE TABLE inscripciones (
    id                    BIGSERIAL PRIMARY KEY,
    cliente_id            BIGINT         NOT NULL,
    curso_id              BIGINT,
    diplomado_id          BIGINT,
    participante_nombre   VARCHAR(150)   NOT NULL,
    participante_email    VARCHAR(150)   NOT NULL,
    participante_telefono VARCHAR(20),
    fecha_inscripcion     DATE           NOT NULL,
    monto_total           NUMERIC(10, 2) NOT NULL,
    estado                VARCHAR(20)    NOT NULL,
    observaciones         VARCHAR(500),
    created_at            TIMESTAMP      NOT NULL,
    updated_at            TIMESTAMP      NOT NULL,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    CONSTRAINT fk_inscripciones_cliente   FOREIGN KEY (cliente_id)   REFERENCES clientes (id),
    CONSTRAINT fk_inscripciones_curso     FOREIGN KEY (curso_id)     REFERENCES cursos (id),
    CONSTRAINT fk_inscripciones_diplomado FOREIGN KEY (diplomado_id) REFERENCES diplomados (id),
    CONSTRAINT ck_inscripciones_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'FINALIZADA')),
    CONSTRAINT ck_inscripciones_monto CHECK (monto_total >= 0),
    -- La inscripción es de un curso o de un diplomado, nunca de ambos ni de ninguno
    CONSTRAINT ck_inscripciones_oferta CHECK (
        (curso_id IS NOT NULL AND diplomado_id IS NULL)
        OR (curso_id IS NULL AND diplomado_id IS NOT NULL)
    )
);

CREATE INDEX ix_inscripciones_cliente   ON inscripciones (cliente_id);
CREATE INDEX ix_inscripciones_curso     ON inscripciones (curso_id);
CREATE INDEX ix_inscripciones_diplomado ON inscripciones (diplomado_id);
CREATE INDEX ix_inscripciones_estado    ON inscripciones (estado);

-- Un mismo participante no puede inscribirse dos veces en la misma oferta,
-- salvo que la inscripción anterior haya sido cancelada
CREATE UNIQUE INDEX uk_inscripciones_participante_curso
    ON inscripciones (curso_id, LOWER(participante_email))
    WHERE curso_id IS NOT NULL AND estado <> 'CANCELADA';

CREATE UNIQUE INDEX uk_inscripciones_participante_diplomado
    ON inscripciones (diplomado_id, LOWER(participante_email))
    WHERE diplomado_id IS NOT NULL AND estado <> 'CANCELADA';

-- ---------------------------------------------------------------------
-- Cotizaciones
-- ---------------------------------------------------------------------
CREATE TABLE cotizaciones (
    id                    BIGSERIAL PRIMARY KEY,
    cliente_id            BIGINT         NOT NULL,
    tipo                  VARCHAR(20)    NOT NULL,
    estado                VARCHAR(20)    NOT NULL,
    asunto                VARCHAR(255)   NOT NULL,
    fecha_solicitud       DATE           NOT NULL,
    fecha_vencimiento     DATE           NOT NULL,
    total                 NUMERIC(12, 2) NOT NULL DEFAULT 0,
    nota_interna          VARCHAR(500),
    responsable_revision  VARCHAR(150),
    created_at            TIMESTAMP      NOT NULL,
    updated_at            TIMESTAMP      NOT NULL,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    CONSTRAINT fk_cotizaciones_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_cotizaciones_tipo CHECK (tipo IN ('CURSO', 'DIPLOMADO', 'ESPACIO', 'CATERING', 'COMBINADA')),
    CONSTRAINT ck_cotizaciones_estado CHECK (estado IN ('PENDIENTE', 'GENERADA', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_cotizaciones_fechas CHECK (fecha_vencimiento >= fecha_solicitud),
    CONSTRAINT ck_cotizaciones_total CHECK (total >= 0)
);

CREATE INDEX ix_cotizaciones_cliente ON cotizaciones (cliente_id);
CREATE INDEX ix_cotizaciones_estado  ON cotizaciones (estado);

CREATE TABLE cotizacion_items (
    id              BIGSERIAL PRIMARY KEY,
    cotizacion_id   BIGINT         NOT NULL,
    tipo            VARCHAR(20)    NOT NULL,
    concepto        VARCHAR(255)   NOT NULL,
    detalle         VARCHAR(255),
    referencia_id   BIGINT,
    cantidad        INTEGER        NOT NULL,
    precio_unitario NUMERIC(10, 2) NOT NULL,
    subtotal        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP      NOT NULL,
    CONSTRAINT fk_cotizacion_items_cotizacion FOREIGN KEY (cotizacion_id) REFERENCES cotizaciones (id) ON DELETE CASCADE,
    CONSTRAINT ck_cotizacion_items_tipo CHECK (tipo IN ('CURSO', 'DIPLOMADO', 'ESPACIO', 'CATERING')),
    CONSTRAINT ck_cotizacion_items_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_cotizacion_items_precio CHECK (precio_unitario >= 0)
);

CREATE INDEX ix_cotizacion_items_cotizacion ON cotizacion_items (cotizacion_id);

-- ---------------------------------------------------------------------
-- Alquiler de espacios
-- ---------------------------------------------------------------------
CREATE TABLE alquileres (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_id          BIGINT         NOT NULL,
    espacio_id          BIGINT         NOT NULL,
    fecha_hora_inicio   TIMESTAMP      NOT NULL,
    fecha_hora_fin      TIMESTAMP      NOT NULL,
    proposito           VARCHAR(255)   NOT NULL,
    cantidad_asistentes INTEGER        NOT NULL,
    monto_total         NUMERIC(10, 2) NOT NULL,
    estado              VARCHAR(20)    NOT NULL,
    observaciones       VARCHAR(500),
    created_at          TIMESTAMP      NOT NULL,
    updated_at          TIMESTAMP      NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    CONSTRAINT fk_alquileres_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_alquileres_espacio FOREIGN KEY (espacio_id) REFERENCES espacios (id),
    CONSTRAINT ck_alquileres_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'FINALIZADA')),
    CONSTRAINT ck_alquileres_horario CHECK (fecha_hora_fin > fecha_hora_inicio),
    CONSTRAINT ck_alquileres_asistentes CHECK (cantidad_asistentes > 0),
    CONSTRAINT ck_alquileres_monto CHECK (monto_total >= 0)
);

CREATE INDEX ix_alquileres_cliente ON alquileres (cliente_id);
CREATE INDEX ix_alquileres_estado  ON alquileres (estado);
-- Índice de apoyo para la detección de traslapes por espacio y rango de fechas
CREATE INDEX ix_alquileres_espacio_horario ON alquileres (espacio_id, fecha_hora_inicio, fecha_hora_fin);

-- ---------------------------------------------------------------------
-- Catering
-- ---------------------------------------------------------------------
CREATE TABLE servicios_catering (
    id                 BIGSERIAL PRIMARY KEY,
    nombre             VARCHAR(120)   NOT NULL,
    tipo               VARCHAR(20)    NOT NULL,
    descripcion        VARCHAR(500),
    precio_por_persona NUMERIC(10, 2) NOT NULL,
    minimo_personas    INTEGER        NOT NULL,
    activo             BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP      NOT NULL,
    updated_at         TIMESTAMP      NOT NULL,
    CONSTRAINT uk_servicios_catering_nombre UNIQUE (nombre),
    CONSTRAINT ck_servicios_catering_tipo CHECK (tipo IN ('COFFEE_BREAK', 'DESAYUNO', 'ALMUERZO', 'CENA', 'REFRIGERIO')),
    CONSTRAINT ck_servicios_catering_precio CHECK (precio_por_persona >= 0),
    CONSTRAINT ck_servicios_catering_minimo CHECK (minimo_personas > 0)
);

CREATE INDEX ix_servicios_catering_tipo ON servicios_catering (tipo);

CREATE TABLE solicitudes_catering (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_id          BIGINT         NOT NULL,
    servicio_id         BIGINT         NOT NULL,
    espacio_id          BIGINT,
    lugar               VARCHAR(255)   NOT NULL,
    cantidad_asistentes INTEGER        NOT NULL,
    menu                VARCHAR(1000),
    fecha               DATE           NOT NULL,
    hora_inicio         TIME           NOT NULL,
    hora_fin            TIME           NOT NULL,
    monto_total         NUMERIC(10, 2) NOT NULL,
    estado              VARCHAR(20)    NOT NULL,
    observaciones       VARCHAR(500),
    created_at          TIMESTAMP      NOT NULL,
    updated_at          TIMESTAMP      NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    CONSTRAINT fk_solicitudes_catering_cliente  FOREIGN KEY (cliente_id)  REFERENCES clientes (id),
    CONSTRAINT fk_solicitudes_catering_servicio FOREIGN KEY (servicio_id) REFERENCES servicios_catering (id),
    CONSTRAINT fk_solicitudes_catering_espacio  FOREIGN KEY (espacio_id)  REFERENCES espacios (id),
    CONSTRAINT ck_solicitudes_catering_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'FINALIZADA')),
    CONSTRAINT ck_solicitudes_catering_horario CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_solicitudes_catering_asistentes CHECK (cantidad_asistentes > 0),
    CONSTRAINT ck_solicitudes_catering_monto CHECK (monto_total >= 0)
);

CREATE INDEX ix_solicitudes_catering_cliente ON solicitudes_catering (cliente_id);
CREATE INDEX ix_solicitudes_catering_fecha   ON solicitudes_catering (fecha);
CREATE INDEX ix_solicitudes_catering_estado  ON solicitudes_catering (estado);

-- ---------------------------------------------------------------------
-- Agenda institucional
-- ---------------------------------------------------------------------
CREATE TABLE actividades_agenda (
    id                BIGSERIAL PRIMARY KEY,
    titulo            VARCHAR(200) NOT NULL,
    tipo              VARCHAR(20)  NOT NULL,
    espacio_id        BIGINT,
    fecha_hora_inicio TIMESTAMP    NOT NULL,
    fecha_hora_fin    TIMESTAMP    NOT NULL,
    descripcion       VARCHAR(500),
    responsable       VARCHAR(150),
    origen_id         BIGINT,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    CONSTRAINT fk_actividades_agenda_espacio FOREIGN KEY (espacio_id) REFERENCES espacios (id),
    CONSTRAINT ck_actividades_agenda_tipo CHECK (tipo IN ('CURSO', 'DIPLOMADO', 'ALQUILER', 'CATERING', 'EVENTO')),
    CONSTRAINT ck_actividades_agenda_horario CHECK (fecha_hora_fin > fecha_hora_inicio)
);

CREATE INDEX ix_actividades_agenda_horario ON actividades_agenda (fecha_hora_inicio, fecha_hora_fin);
CREATE INDEX ix_actividades_agenda_espacio ON actividades_agenda (espacio_id);
CREATE INDEX ix_actividades_agenda_tipo    ON actividades_agenda (tipo);

-- Un alquiler o una solicitud de catering generan una sola entrada en la agenda
CREATE UNIQUE INDEX uk_actividades_agenda_origen
    ON actividades_agenda (tipo, origen_id)
    WHERE origen_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- Pagos
-- ---------------------------------------------------------------------
CREATE TABLE pagos (
    id                   BIGSERIAL PRIMARY KEY,
    cliente_id           BIGINT         NOT NULL,
    concepto             VARCHAR(20)    NOT NULL,
    referencia_id        BIGINT         NOT NULL,
    monto_esperado       NUMERIC(12, 2) NOT NULL,
    monto_pagado         NUMERIC(12, 2) NOT NULL DEFAULT 0,
    metodo               VARCHAR(20)    NOT NULL,
    estado               VARCHAR(20)    NOT NULL,
    fecha_pago           DATE           NOT NULL,
    referencia_bancaria  VARCHAR(100),
    fecha_validacion     TIMESTAMP,
    validado_por         VARCHAR(100),
    fecha_confirmacion   TIMESTAMP,
    confirmado_por       VARCHAR(100),
    eliminado            BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP      NOT NULL,
    updated_at           TIMESTAMP      NOT NULL,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    CONSTRAINT fk_pagos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_pagos_concepto CHECK (concepto IN ('INSCRIPCION', 'COTIZACION', 'ALQUILER', 'CATERING')),
    CONSTRAINT ck_pagos_metodo CHECK (metodo IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'DEPOSITO')),
    CONSTRAINT ck_pagos_estado CHECK (estado IN ('PENDIENTE', 'PARCIAL', 'PAGADO')),
    CONSTRAINT ck_pagos_montos CHECK (monto_esperado > 0 AND monto_pagado >= 0 AND monto_pagado <= monto_esperado)
);

CREATE INDEX ix_pagos_cliente   ON pagos (cliente_id);
CREATE INDEX ix_pagos_estado    ON pagos (estado);
CREATE INDEX ix_pagos_concepto  ON pagos (concepto, referencia_id);
CREATE INDEX ix_pagos_eliminado ON pagos (eliminado);

CREATE TABLE comprobantes (
    id            BIGSERIAL PRIMARY KEY,
    pago_id       BIGINT         NOT NULL,
    numero        VARCHAR(30)    NOT NULL,
    fecha_emision TIMESTAMP      NOT NULL,
    emitido_a     VARCHAR(150)   NOT NULL,
    monto         NUMERIC(12, 2) NOT NULL,
    observaciones VARCHAR(500),
    created_at    TIMESTAMP      NOT NULL,
    updated_at    TIMESTAMP      NOT NULL,
    CONSTRAINT uk_comprobantes_pago   UNIQUE (pago_id),
    CONSTRAINT uk_comprobantes_numero UNIQUE (numero),
    CONSTRAINT fk_comprobantes_pago FOREIGN KEY (pago_id) REFERENCES pagos (id),
    CONSTRAINT ck_comprobantes_monto CHECK (monto >= 0)
);


-- ---------- V2: datos iniciales ----------

-- =====================================================================
-- UCA-CFC Connect · Datos iniciales para demostración
--
-- Las contraseñas están cifradas con BCrypt. Las credenciales en texto
-- plano están documentadas en el README y son válidas únicamente para
-- el entorno de desarrollo.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Roles y usuarios
-- ---------------------------------------------------------------------
INSERT INTO roles (nombre, descripcion, created_at, updated_at) VALUES
    ('ADMIN',          'Acceso administrativo completo al sistema',                       NOW(), NOW()),
    ('RECEPCIONISTA',  'Atención al cliente, inscripciones, reservas y confirmaciones',   NOW(), NOW()),
    ('CLIENTE',        'Consulta y solicita servicios del centro',                        NOW(), NOW()),
    ('CONTABILIDAD',   'Validación de pagos e información financiera',                    NOW(), NOW());

INSERT INTO usuarios (nombre, email, password_hash, estado, rol_id, created_at, updated_at, created_by, updated_by) VALUES
    ('Yesenia Escobar',  'admin@cfc.uca.edu.sv',
     '$2a$10$6VfRJHzufMxM8EtQVBhONuNerGVsaPLSTL8EfQTcWg2S.yqC0uRaS', 'ACTIVO',
     (SELECT id FROM roles WHERE nombre = 'ADMIN'), NOW(), NOW(), 'seed', 'seed'),
    ('Rocío Cortez',     'recepcion@cfc.uca.edu.sv',
     '$2a$10$03Kvwdx3MEDLLWmn15YMEOj3Wgq3IMektM2v.JcLBVNqPjuPPrnOm', 'ACTIVO',
     (SELECT id FROM roles WHERE nombre = 'RECEPCIONISTA'), NOW(), NOW(), 'seed', 'seed'),
    ('Carlos Tobar',     'contabilidad@cfc.uca.edu.sv',
     '$2a$10$dZuGiMXmNIaH7RZ6DAVAdu0x2U1vbslBhfM1CaE2LjJ2QJO9/bHX2', 'ACTIVO',
     (SELECT id FROM roles WHERE nombre = 'CONTABILIDAD'), NOW(), NOW(), 'seed', 'seed'),
    ('Marta Elena Gómez','marta.gomez@vitalab.com.sv',
     '$2a$10$gVU.V2st9kJ3U7EZNUdZW.F01GTCkSLYY4nkyQaYqH8YoHAXYwi.S', 'ACTIVO',
     (SELECT id FROM roles WHERE nombre = 'CLIENTE'), NOW(), NOW(), 'seed', 'seed');

-- ---------------------------------------------------------------------
-- Catálogos académicos
-- ---------------------------------------------------------------------
INSERT INTO categorias (nombre, descripcion, created_at, updated_at) VALUES
    ('Gestión de Proyectos', 'Metodologías tradicionales y ágiles de gestión',       NOW(), NOW()),
    ('Finanzas',             'Finanzas corporativas, costos y análisis financiero',  NOW(), NOW()),
    ('Tecnología',           'Herramientas informáticas y análisis de datos',        NOW(), NOW()),
    ('Desarrollo Humano',    'Liderazgo, servicio al cliente y habilidades blandas', NOW(), NOW()),
    ('Idiomas',              'Formación en idiomas para el ámbito profesional',      NOW(), NOW());

INSERT INTO modalidades (nombre, tipo, descripcion, created_at, updated_at) VALUES
    ('Presencial', 'PRESENCIAL', 'Sesiones en las instalaciones del centro',                  NOW(), NOW()),
    ('Virtual',    'VIRTUAL',    'Sesiones en línea por la plataforma CFC Live',              NOW(), NOW()),
    ('Híbrida',    'HIBRIDA',    'Combina sesiones presenciales con sesiones en línea',       NOW(), NOW());

INSERT INTO docentes (nombres, apellidos, email, telefono, especialidad, created_at, updated_at) VALUES
    ('Ana Beatriz',  'Rivas',     'ana.rivas@cfc.uca.edu.sv',      '2210-6600', 'Gestión de Proyectos',   NOW(), NOW()),
    ('Mario',        'Peñate',    'mario.penate@cfc.uca.edu.sv',   '2210-6601', 'Finanzas Corporativas',  NOW(), NOW()),
    ('Karla',        'Menjívar',  'karla.menjivar@cfc.uca.edu.sv', '2210-6602', 'Análisis de Datos',      NOW(), NOW()),
    ('Óscar',        'Villalta',  'oscar.villalta@cfc.uca.edu.sv', '2210-6603', 'Desarrollo Organizacional', NOW(), NOW()),
    ('Silvia',       'Arteaga',   'silvia.arteaga@cfc.uca.edu.sv', '2210-6604', 'Servicio al Cliente',    NOW(), NOW());

-- ---------------------------------------------------------------------
-- Espacios
-- ---------------------------------------------------------------------
INSERT INTO espacios (nombre, tipo, capacidad, precio_por_hora, equipamiento, ubicacion, estado, created_at, updated_at) VALUES
    ('Auditorio Principal',  'AUDITORIO',       180, 45.00, 'Proyector 4K, sonido profesional, tarima, 2 micrófonos inalámbricos', 'Edificio Central, planta baja', 'DISPONIBLE', NOW(), NOW()),
    ('Auditorio Norte',      'AUDITORIO',        90, 30.00, 'Proyector, sonido, atril',                                           'Edificio Norte, primer nivel',  'DISPONIBLE', NOW(), NOW()),
    ('Aula 3',               'AULA',             25,  8.00, 'Proyector, pizarra acrílica, aire acondicionado',                    'Edificio Central, segundo nivel','DISPONIBLE', NOW(), NOW()),
    ('Aula 4',               'AULA',             25,  8.00, 'Proyector, pizarra acrílica',                                        'Edificio Central, segundo nivel','DISPONIBLE', NOW(), NOW()),
    ('Aula 5',               'AULA',             30,  9.00, 'Proyector, pizarra acrílica, mobiliario modular',                    'Edificio Central, tercer nivel', 'DISPONIBLE', NOW(), NOW()),
    ('Laboratorio 1',        'LABORATORIO',      20, 15.00, '20 computadoras, proyector, software de ofimática y BI',             'Edificio de Informática',        'DISPONIBLE', NOW(), NOW()),
    ('Laboratorio 2',        'LABORATORIO',      20, 15.00, '20 computadoras, proyector',                                         'Edificio de Informática',        'MANTENIMIENTO', NOW(), NOW()),
    ('Sala de Juntas',       'SALA',             12, 12.00, 'Pantalla 65", videoconferencia, mesa de juntas',                     'Edificio Central, tercer nivel', 'DISPONIBLE', NOW(), NOW()),
    ('Sala Multimedia',      'SALA_MULTIMEDIA',  40, 20.00, 'Pantalla LED, cabina de audio, cámara de transmisión',               'Edificio Norte, segundo nivel',  'DISPONIBLE', NOW(), NOW());

-- ---------------------------------------------------------------------
-- Cursos
-- ---------------------------------------------------------------------
INSERT INTO cursos (codigo, nombre, descripcion, categoria_id, modalidad_id, docente_id, espacio_id,
                    fecha_inicio, fecha_fin, horario, duracion_horas, cupo_maximo, precio, estado,
                    created_at, updated_at, created_by, updated_by) VALUES
    ('CUR-009', 'Atención al Cliente de Alto Impacto',
     'Técnicas de servicio, manejo de objeciones y recuperación de clientes.',
     (SELECT id FROM categorias WHERE nombre = 'Desarrollo Humano'),
     (SELECT id FROM modalidades WHERE nombre = 'Presencial'),
     (SELECT id FROM docentes WHERE email = 'silvia.arteaga@cfc.uca.edu.sv'),
     (SELECT id FROM espacios WHERE nombre = 'Sala Multimedia'),
     DATE '2026-09-25', DATE '2026-10-23', 'Vie · 2:00–6:00 pm', 20, 20, 60.00, 'ACTIVO', NOW(), NOW(), 'seed', 'seed'),

    ('CUR-014', 'Gestión Ágil de Proyectos',
     'Scrum, Kanban y gestión de equipos en entornos de cambio permanente.',
     (SELECT id FROM categorias WHERE nombre = 'Gestión de Proyectos'),
     (SELECT id FROM modalidades WHERE nombre = 'Presencial'),
     (SELECT id FROM docentes WHERE email = 'ana.rivas@cfc.uca.edu.sv'),
     (SELECT id FROM espacios WHERE nombre = 'Aula 3'),
     DATE '2026-09-14', DATE '2026-10-21', 'Lun–Mié · 6:00–8:00 pm', 32, 20, 185.00, 'ACTIVO', NOW(), NOW(), 'seed', 'seed'),

    ('CUR-021', 'Excel Avanzado para Finanzas',
     'Tablas dinámicas, modelado financiero y automatización con macros.',
     (SELECT id FROM categorias WHERE nombre = 'Finanzas'),
     (SELECT id FROM modalidades WHERE nombre = 'Presencial'),
     (SELECT id FROM docentes WHERE email = 'karla.menjivar@cfc.uca.edu.sv'),
     (SELECT id FROM espacios WHERE nombre = 'Laboratorio 1'),
     DATE '2026-09-22', DATE '2026-10-29', 'Mar–Jue · 4:00–6:00 pm', 24, 20, 95.00, 'ACTIVO', NOW(), NOW(), 'seed', 'seed'),

    ('CUR-030', 'Introducción a Power BI',
     'Modelado de datos, DAX básico y publicación de tableros.',
     (SELECT id FROM categorias WHERE nombre = 'Tecnología'),
     (SELECT id FROM modalidades WHERE nombre = 'Virtual'),
     (SELECT id FROM docentes WHERE email = 'karla.menjivar@cfc.uca.edu.sv'),
     NULL,
     DATE '2026-10-05', DATE '2026-11-09', 'Lun · 6:00–9:00 pm', 18, 20, 110.00, 'ACTIVO', NOW(), NOW(), 'seed', 'seed'),

    ('CUR-033', 'Inglés de Negocios · Nivel I',
     'Comunicación escrita y oral en contextos corporativos.',
     (SELECT id FROM categorias WHERE nombre = 'Idiomas'),
     (SELECT id FROM modalidades WHERE nombre = 'Híbrida'),
     (SELECT id FROM docentes WHERE email = 'oscar.villalta@cfc.uca.edu.sv'),
     (SELECT id FROM espacios WHERE nombre = 'Aula 4'),
     DATE '2026-10-06', DATE '2026-12-15', 'Mar–Jue · 6:00–8:00 pm', 48, 18, 240.00, 'ACTIVO', NOW(), NOW(), 'seed', 'seed'),

    ('CUR-002', 'Redacción de Informes Técnicos',
     'Estructura, claridad y presentación de informes institucionales.',
     (SELECT id FROM categorias WHERE nombre = 'Desarrollo Humano'),
     (SELECT id FROM modalidades WHERE nombre = 'Virtual'),
     (SELECT id FROM docentes WHERE email = 'oscar.villalta@cfc.uca.edu.sv'),
     NULL,
     DATE '2026-03-02', DATE '2026-04-10', 'Sáb · 8:00–11:00 am', 18, 25, 75.00, 'INACTIVO', NOW(), NOW(), 'seed', 'seed');

-- ---------------------------------------------------------------------
-- Diplomados
-- ---------------------------------------------------------------------
INSERT INTO diplomados (codigo, nombre, descripcion, categoria_id, modalidad_id, docente_id, espacio_id,
                        fecha_inicio, fecha_fin, horario, duracion_horas, cupo_maximo, precio, estado,
                        created_at, updated_at, created_by, updated_by) VALUES
    ('DIP-006', 'Diplomado en Finanzas Corporativas',
     'Valuación, estructura de capital, presupuesto y análisis de inversiones.',
     (SELECT id FROM categorias WHERE nombre = 'Finanzas'),
     (SELECT id FROM modalidades WHERE nombre = 'Virtual'),
     (SELECT id FROM docentes WHERE email = 'mario.penate@cfc.uca.edu.sv'),
     NULL,
     DATE '2026-09-19', DATE '2027-02-27', 'Sáb · 8:00–12:00 pm', 120, 30, 620.00, 'ACTIVO', NOW(), NOW(), 'seed', 'seed'),

    ('DIP-011', 'Diplomado en Liderazgo Ejecutivo',
     'Dirección de equipos, comunicación estratégica y gestión del cambio.',
     (SELECT id FROM categorias WHERE nombre = 'Desarrollo Humano'),
     (SELECT id FROM modalidades WHERE nombre = 'Presencial'),
     (SELECT id FROM docentes WHERE email = 'oscar.villalta@cfc.uca.edu.sv'),
     (SELECT id FROM espacios WHERE nombre = 'Auditorio Principal'),
     DATE '2026-10-03', DATE '2027-03-06', 'Sáb · 9:00–1:00 pm', 110, 25, 540.00, 'ACTIVO', NOW(), NOW(), 'seed', 'seed'),

    ('DIP-014', 'Diplomado en Analítica de Datos',
     'Del dato al tablero: SQL, visualización y storytelling con datos.',
     (SELECT id FROM categorias WHERE nombre = 'Tecnología'),
     (SELECT id FROM modalidades WHERE nombre = 'Híbrida'),
     (SELECT id FROM docentes WHERE email = 'karla.menjivar@cfc.uca.edu.sv'),
     (SELECT id FROM espacios WHERE nombre = 'Laboratorio 1'),
     DATE '2026-11-07', DATE '2027-04-24', 'Sáb · 8:00–12:00 pm', 130, 20, 680.00, 'ACTIVO', NOW(), NOW(), 'seed', 'seed');

-- ---------------------------------------------------------------------
-- Clientes
-- ---------------------------------------------------------------------
INSERT INTO empresas (razon_social, giro, contacto_nombre, contacto_cargo, created_at, updated_at) VALUES
    ('Laboratorios Vitalab, S.A. de C.V.', 'Industria farmacéutica',    'Marta Elena Gómez', 'Jefa de Talento Humano', NOW(), NOW()),
    ('Corporativo Alfa, S.A.',             'Servicios corporativos',    'Luis Menéndez',     'Gerente Administrativo', NOW(), NOW()),
    ('Fundación Horizonte',                'Organización sin fines de lucro', 'Delmy Portillo', 'Coordinadora de Proyectos', NOW(), NOW()),
    ('Grupo Sarti',                        'Comercio al por mayor',     'Ernesto Sarti',     'Director General',        NOW(), NOW());

INSERT INTO clientes (tipo, nombre, dui, nit, email, telefono, direccion, empresa_id, usuario_id,
                      created_at, updated_at, created_by, updated_by) VALUES
    ('EMPRESA', 'Laboratorios Vitalab', NULL, '0614-120589-102-3', 'contacto@vitalab.com.sv', '2245-8800',
     'Blvd. Los Próceres, San Salvador',
     (SELECT id FROM empresas WHERE razon_social = 'Laboratorios Vitalab, S.A. de C.V.'),
     (SELECT id FROM usuarios WHERE email = 'marta.gomez@vitalab.com.sv'), NOW(), NOW(), 'seed', 'seed'),

    ('EMPRESA', 'Corporativo Alfa', NULL, '0614-030794-101-8', 'administracion@alfa.com.sv', '2260-1122',
     'Col. Escalón, San Salvador',
     (SELECT id FROM empresas WHERE razon_social = 'Corporativo Alfa, S.A.'), NULL, NOW(), NOW(), 'seed', 'seed'),

    ('EMPRESA', 'Fundación Horizonte', NULL, '0614-250603-103-1', 'proyectos@horizonte.org.sv', '2298-4477',
     'Santa Tecla, La Libertad',
     (SELECT id FROM empresas WHERE razon_social = 'Fundación Horizonte'), NULL, NOW(), NOW(), 'seed', 'seed'),

    ('EMPRESA', 'Grupo Sarti', NULL, '0614-110811-104-6', 'compras@gruposarti.com', '2271-3030',
     'Soyapango, San Salvador',
     (SELECT id FROM empresas WHERE razon_social = 'Grupo Sarti'), NULL, NOW(), NOW(), 'seed', 'seed'),

    ('PERSONA_PARTICULAR', 'José Antonio Meléndez', '04512378-9', NULL, 'ja.melendez@correo.com', '7845-2210',
     'Antiguo Cuscatlán, La Libertad', NULL, NULL, NOW(), NOW(), 'seed', 'seed'),

    ('PERSONA_PARTICULAR', 'Claudia Regina Flores', '03987421-5', NULL, 'claudia.flores@correo.com', '7712-9987',
     'Mejicanos, San Salvador', NULL, NULL, NOW(), NOW(), 'seed', 'seed');

-- ---------------------------------------------------------------------
-- Servicios de catering
-- ---------------------------------------------------------------------
INSERT INTO servicios_catering (nombre, tipo, descripcion, precio_por_persona, minimo_personas, activo, created_at, updated_at) VALUES
    ('Coffee break sencillo',  'COFFEE_BREAK', 'Café, té, agua y bocadillo dulce',                         5.50, 10, TRUE, NOW(), NOW()),
    ('Coffee break ejecutivo', 'COFFEE_BREAK', 'Café de especialidad, jugos, fruta y repostería fina',     8.50, 10, TRUE, NOW(), NOW()),
    ('Desayuno continental',   'DESAYUNO',     'Fruta, pan artesanal, jugos, café y huevos',               9.75, 15, TRUE, NOW(), NOW()),
    ('Almuerzo ejecutivo',     'ALMUERZO',     'Entrada, plato fuerte, postre y bebida',                  14.00, 15, TRUE, NOW(), NOW()),
    ('Cena institucional',     'CENA',         'Menú de tres tiempos con servicio de mesa',               18.50, 20, TRUE, NOW(), NOW()),
    ('Refrigerio escolar',     'REFRIGERIO',   'Sándwich, fruta y bebida natural',                         4.25, 10, TRUE, NOW(), NOW());

-- ---------------------------------------------------------------------
-- Inscripciones
-- ---------------------------------------------------------------------
INSERT INTO inscripciones (cliente_id, curso_id, diplomado_id, participante_nombre, participante_email,
                           participante_telefono, fecha_inscripcion, monto_total, estado, observaciones,
                           created_at, updated_at, created_by, updated_by) VALUES
    ((SELECT id FROM clientes WHERE email = 'contacto@vitalab.com.sv'),
     (SELECT id FROM cursos WHERE codigo = 'CUR-014'), NULL,
     'Marta Elena Gómez', 'marta.gomez@vitalab.com.sv', '7890-1122',
     DATE '2026-09-02', 185.00, 'CONFIRMADA', NULL, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'contacto@vitalab.com.sv'),
     (SELECT id FROM cursos WHERE codigo = 'CUR-014'), NULL,
     'Ricardo Aguilar', 'r.aguilar@vitalab.com.sv', '7890-1133',
     DATE '2026-09-02', 185.00, 'CONFIRMADA', NULL, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'administracion@alfa.com.sv'),
     (SELECT id FROM cursos WHERE codigo = 'CUR-021'), NULL,
     'Luis Menéndez', 'luis.menendez@alfa.com.sv', '7033-5566',
     DATE '2026-09-05', 95.00, 'PENDIENTE', 'Pendiente de confirmación de pago', NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'ja.melendez@correo.com'),
     (SELECT id FROM cursos WHERE codigo = 'CUR-030'), NULL,
     'José Antonio Meléndez', 'ja.melendez@correo.com', '7845-2210',
     DATE '2026-09-08', 110.00, 'PENDIENTE', NULL, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'proyectos@horizonte.org.sv'), NULL,
     (SELECT id FROM diplomados WHERE codigo = 'DIP-011'),
     'Delmy Portillo', 'delmy.portillo@horizonte.org.sv', '7455-9090',
     DATE '2026-08-28', 540.00, 'CONFIRMADA', NULL, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'claudia.flores@correo.com'), NULL,
     (SELECT id FROM diplomados WHERE codigo = 'DIP-006'),
     'Claudia Regina Flores', 'claudia.flores@correo.com', '7712-9987',
     DATE '2026-08-30', 620.00, 'CONFIRMADA', NULL, NOW(), NOW(), 'seed', 'seed');

-- ---------------------------------------------------------------------
-- Cotizaciones
-- ---------------------------------------------------------------------
INSERT INTO cotizaciones (cliente_id, tipo, estado, asunto, fecha_solicitud, fecha_vencimiento, total,
                          nota_interna, responsable_revision, created_at, updated_at, created_by, updated_by) VALUES
    ((SELECT id FROM clientes WHERE email = 'administracion@alfa.com.sv'), 'COMBINADA', 'GENERADA',
     'Evento institucional de cierre de año', DATE '2026-08-06', DATE '2026-09-06', 5017.50,
     'Verificar disponibilidad del auditorio antes de aprobar.', 'Ing. Yesenia Escobar — Administración',
     NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'contacto@vitalab.com.sv'), 'DIPLOMADO', 'APROBADA',
     'Diplomado en Finanzas para cinco colaboradores', DATE '2026-08-02', DATE '2026-09-02', 3100.00,
     NULL, 'Ing. Yesenia Escobar — Administración', NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'proyectos@horizonte.org.sv'), 'COMBINADA', 'RECHAZADA',
     'Jornada de formación comunitaria', DATE '2026-07-28', DATE '2026-08-28', 740.00,
     'La fecha solicitada coincide con un evento institucional.', 'Ing. Yesenia Escobar — Administración',
     NOW(), NOW(), 'seed', 'seed');

INSERT INTO cotizacion_items (cotizacion_id, tipo, concepto, detalle, referencia_id, cantidad, precio_unitario, subtotal, created_at, updated_at) VALUES
    ((SELECT id FROM cotizaciones WHERE asunto = 'Evento institucional de cierre de año'), 'CURSO',
     'Curso empresarial', 'CUR-014 · 25 participantes',
     (SELECT id FROM cursos WHERE codigo = 'CUR-014'), 25, 185.00, 4625.00, NOW(), NOW()),
    ((SELECT id FROM cotizaciones WHERE asunto = 'Evento institucional de cierre de año'), 'ESPACIO',
     'Auditorio principal', 'Medio día (4 horas)',
     (SELECT id FROM espacios WHERE nombre = 'Auditorio Principal'), 4, 45.00, 180.00, NOW(), NOW()),
    ((SELECT id FROM cotizaciones WHERE asunto = 'Evento institucional de cierre de año'), 'CATERING',
     'Catering — coffee break', '25 personas',
     (SELECT id FROM servicios_catering WHERE nombre = 'Coffee break ejecutivo'), 25, 8.50, 212.50, NOW(), NOW()),

    ((SELECT id FROM cotizaciones WHERE asunto = 'Diplomado en Finanzas para cinco colaboradores'), 'DIPLOMADO',
     'Diplomado en Finanzas Corporativas', 'DIP-006 · 5 participantes',
     (SELECT id FROM diplomados WHERE codigo = 'DIP-006'), 5, 620.00, 3100.00, NOW(), NOW()),

    ((SELECT id FROM cotizaciones WHERE asunto = 'Jornada de formación comunitaria'), 'ESPACIO',
     'Auditorio Norte', 'Jornada completa (8 horas)',
     (SELECT id FROM espacios WHERE nombre = 'Auditorio Norte'), 8, 30.00, 240.00, NOW(), NOW()),
    ((SELECT id FROM cotizaciones WHERE asunto = 'Jornada de formación comunitaria'), 'CATERING',
     'Refrigerio para asistentes', '100 personas',
     (SELECT id FROM servicios_catering WHERE nombre = 'Refrigerio escolar'), 100, 5.00, 500.00, NOW(), NOW());

-- ---------------------------------------------------------------------
-- Alquileres de espacios
-- ---------------------------------------------------------------------
INSERT INTO alquileres (cliente_id, espacio_id, fecha_hora_inicio, fecha_hora_fin, proposito,
                        cantidad_asistentes, monto_total, estado, observaciones,
                        created_at, updated_at, created_by, updated_by) VALUES
    ((SELECT id FROM clientes WHERE email = 'administracion@alfa.com.sv'),
     (SELECT id FROM espacios WHERE nombre = 'Auditorio Principal'),
     TIMESTAMP '2026-09-15 17:00:00', TIMESTAMP '2026-09-15 18:30:00',
     'Asamblea de accionistas', 120, 67.50, 'CONFIRMADA', NULL, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'proyectos@horizonte.org.sv'),
     (SELECT id FROM espacios WHERE nombre = 'Sala Multimedia'),
     TIMESTAMP '2026-09-17 13:00:00', TIMESTAMP '2026-09-17 15:00:00',
     'Feria de egresados', 35, 40.00, 'CONFIRMADA', NULL, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'compras@gruposarti.com'),
     (SELECT id FROM espacios WHERE nombre = 'Sala de Juntas'),
     TIMESTAMP '2026-09-22 09:00:00', TIMESTAMP '2026-09-22 12:00:00',
     'Reunión de planificación trimestral', 10, 36.00, 'PENDIENTE', NULL, NOW(), NOW(), 'seed', 'seed');

-- ---------------------------------------------------------------------
-- Solicitudes de catering
-- ---------------------------------------------------------------------
INSERT INTO solicitudes_catering (cliente_id, servicio_id, espacio_id, lugar, cantidad_asistentes, menu,
                                  fecha, hora_inicio, hora_fin, monto_total, estado, observaciones,
                                  created_at, updated_at, created_by, updated_by) VALUES
    ((SELECT id FROM clientes WHERE email = 'proyectos@horizonte.org.sv'),
     (SELECT id FROM servicios_catering WHERE nombre = 'Coffee break ejecutivo'),
     (SELECT id FROM espacios WHERE nombre = 'Sala Multimedia'),
     'Sala Multimedia — Edificio Norte', 30, 'Café de especialidad, jugos naturales, fruta de temporada y repostería',
     DATE '2026-09-17', TIME '11:30:00', TIME '12:15:00', 255.00, 'CONFIRMADA', NULL, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'compras@gruposarti.com'),
     (SELECT id FROM servicios_catering WHERE nombre = 'Almuerzo ejecutivo'),
     (SELECT id FROM espacios WHERE nombre = 'Sala de Juntas'),
     'Sala de Juntas — Edificio Central', 12, 'Crema de ayote, pollo en salsa de hierbas, flan de vainilla',
     DATE '2026-09-22', TIME '12:00:00', TIME '13:30:00', 168.00, 'PENDIENTE', NULL, NOW(), NOW(), 'seed', 'seed');

-- ---------------------------------------------------------------------
-- Agenda institucional
-- ---------------------------------------------------------------------
INSERT INTO actividades_agenda (titulo, tipo, espacio_id, fecha_hora_inicio, fecha_hora_fin, descripcion,
                                responsable, origen_id, created_at, updated_at, created_by, updated_by) VALUES
    ('Gestión Ágil de Proyectos · Aula 3', 'CURSO',
     (SELECT id FROM espacios WHERE nombre = 'Aula 3'),
     TIMESTAMP '2026-09-14 18:00:00', TIMESTAMP '2026-09-14 20:00:00',
     'Primera sesión del curso CUR-014', 'Ana Beatriz Rivas', NULL, NOW(), NOW(), 'seed', 'seed'),

    ('Excel Avanzado · Laboratorio 1', 'CURSO',
     (SELECT id FROM espacios WHERE nombre = 'Laboratorio 1'),
     TIMESTAMP '2026-09-22 16:00:00', TIMESTAMP '2026-09-22 18:00:00',
     'Primera sesión del curso CUR-021', 'Karla Menjívar', NULL, NOW(), NOW(), 'seed', 'seed'),

    ('Diplomado en Finanzas · CFC Live', 'DIPLOMADO', NULL,
     TIMESTAMP '2026-09-19 08:00:00', TIMESTAMP '2026-09-19 12:00:00',
     'Módulo inicial del diplomado DIP-006', 'Mario Peñate', NULL, NOW(), NOW(), 'seed', 'seed'),

    ('Asamblea de accionistas · Corporativo Alfa', 'ALQUILER',
     (SELECT id FROM espacios WHERE nombre = 'Auditorio Principal'),
     TIMESTAMP '2026-09-15 17:00:00', TIMESTAMP '2026-09-15 18:30:00',
     'Alquiler del Auditorio Principal', 'Recepción CFC',
     (SELECT id FROM alquileres WHERE proposito = 'Asamblea de accionistas'), NOW(), NOW(), 'seed', 'seed'),

    ('Feria de egresados · Fundación Horizonte', 'ALQUILER',
     (SELECT id FROM espacios WHERE nombre = 'Sala Multimedia'),
     TIMESTAMP '2026-09-17 13:00:00', TIMESTAMP '2026-09-17 15:00:00',
     'Alquiler de la Sala Multimedia', 'Recepción CFC',
     (SELECT id FROM alquileres WHERE proposito = 'Feria de egresados'), NOW(), NOW(), 'seed', 'seed'),

    ('Coffee break · Fundación Horizonte', 'CATERING',
     (SELECT id FROM espacios WHERE nombre = 'Sala Multimedia'),
     TIMESTAMP '2026-09-17 11:30:00', TIMESTAMP '2026-09-17 12:15:00',
     'Servicio de coffee break ejecutivo para 30 personas', 'Recepción CFC',
     (SELECT id FROM solicitudes_catering WHERE lugar = 'Sala Multimedia — Edificio Norte'), NOW(), NOW(), 'seed', 'seed'),

    ('Jornada de inducción docente', 'EVENTO',
     (SELECT id FROM espacios WHERE nombre = 'Auditorio Norte'),
     TIMESTAMP '2026-09-18 08:00:00', TIMESTAMP '2026-09-18 12:00:00',
     'Actividad interna del centro', 'Coordinación académica', NULL, NOW(), NOW(), 'seed', 'seed');

-- ---------------------------------------------------------------------
-- Pagos y comprobantes
-- ---------------------------------------------------------------------
INSERT INTO pagos (cliente_id, concepto, referencia_id, monto_esperado, monto_pagado, metodo, estado,
                   fecha_pago, referencia_bancaria, fecha_validacion, validado_por, fecha_confirmacion,
                   confirmado_por, eliminado, created_at, updated_at, created_by, updated_by) VALUES
    ((SELECT id FROM clientes WHERE email = 'contacto@vitalab.com.sv'), 'INSCRIPCION',
     (SELECT id FROM inscripciones WHERE participante_email = 'marta.gomez@vitalab.com.sv'),
     185.00, 185.00, 'TARJETA', 'PAGADO', DATE '2026-09-02', 'AUT-884512',
     TIMESTAMP '2026-09-02 10:30:00', 'contabilidad@cfc.uca.edu.sv',
     TIMESTAMP '2026-09-02 11:00:00', 'recepcion@cfc.uca.edu.sv', FALSE, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'administracion@alfa.com.sv'), 'COTIZACION',
     (SELECT id FROM cotizaciones WHERE asunto = 'Evento institucional de cierre de año'),
     5017.50, 2500.00, 'TRANSFERENCIA', 'PARCIAL', DATE '2026-09-04', 'TRF-2026-0910',
     TIMESTAMP '2026-09-04 15:20:00', 'contabilidad@cfc.uca.edu.sv', NULL, NULL, FALSE, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'proyectos@horizonte.org.sv'), 'ALQUILER',
     (SELECT id FROM alquileres WHERE proposito = 'Feria de egresados'),
     40.00, 0.00, 'DEPOSITO', 'PENDIENTE', DATE '2026-09-09', NULL, NULL, NULL, NULL, NULL,
     FALSE, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'proyectos@horizonte.org.sv'), 'CATERING',
     (SELECT id FROM solicitudes_catering WHERE lugar = 'Sala Multimedia — Edificio Norte'),
     255.00, 255.00, 'EFECTIVO', 'PAGADO', DATE '2026-09-07', NULL,
     TIMESTAMP '2026-09-07 09:15:00', 'contabilidad@cfc.uca.edu.sv',
     TIMESTAMP '2026-09-07 09:40:00', 'recepcion@cfc.uca.edu.sv', FALSE, NOW(), NOW(), 'seed', 'seed'),

    ((SELECT id FROM clientes WHERE email = 'claudia.flores@correo.com'), 'INSCRIPCION',
     (SELECT id FROM inscripciones WHERE participante_email = 'claudia.flores@correo.com'),
     620.00, 310.00, 'TRANSFERENCIA', 'PARCIAL', DATE '2026-08-30', 'TRF-2026-0855',
     TIMESTAMP '2026-08-30 16:00:00', 'contabilidad@cfc.uca.edu.sv', NULL, NULL, FALSE, NOW(), NOW(), 'seed', 'seed');

INSERT INTO comprobantes (pago_id, numero, fecha_emision, emitido_a, monto, observaciones, created_at, updated_at) VALUES
    ((SELECT p.id FROM pagos p JOIN clientes c ON c.id = p.cliente_id
      WHERE c.email = 'contacto@vitalab.com.sv' AND p.concepto = 'INSCRIPCION'),
     'CFC-2026-000001', TIMESTAMP '2026-09-02 11:05:00', 'Laboratorios Vitalab', 185.00,
     'Inscripción CUR-014 · Marta Elena Gómez', NOW(), NOW()),

    ((SELECT p.id FROM pagos p JOIN clientes c ON c.id = p.cliente_id
      WHERE c.email = 'proyectos@horizonte.org.sv' AND p.concepto = 'CATERING'),
     'CFC-2026-000002', TIMESTAMP '2026-09-07 09:45:00', 'Fundación Horizonte', 255.00,
     'Coffee break ejecutivo · 30 personas', NOW(), NOW());
