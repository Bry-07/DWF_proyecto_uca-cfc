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
