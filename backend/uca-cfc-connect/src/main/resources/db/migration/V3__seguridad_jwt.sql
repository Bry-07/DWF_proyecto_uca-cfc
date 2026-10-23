-- =====================================================================
-- Fase 3 · Seguridad: cierre de sesión y recuperación de contraseña
-- =====================================================================

-- Access tokens a los que se les cerró la sesión antes de vencer.
-- Se guarda solo el jti (identificador del token), no el token completo.
CREATE TABLE tokens_revocados (
    id               BIGSERIAL PRIMARY KEY,
    jti              VARCHAR(64) NOT NULL,
    fecha_expiracion TIMESTAMP   NOT NULL,
    created_at       TIMESTAMP   NOT NULL,
    updated_at       TIMESTAMP   NOT NULL,
    CONSTRAINT uk_tokens_revocados_jti UNIQUE (jti)
);

CREATE INDEX ix_tokens_revocados_expiracion ON tokens_revocados (fecha_expiracion);

-- Tokens de recuperación de contraseña. Se guarda el hash SHA-256 del token,
-- el valor original solo lo recibe el usuario.
CREATE TABLE password_reset_tokens (
    id               BIGSERIAL PRIMARY KEY,
    token_hash       VARCHAR(64) NOT NULL,
    usuario_id       BIGINT      NOT NULL,
    fecha_expiracion TIMESTAMP   NOT NULL,
    usado            BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP   NOT NULL,
    updated_at       TIMESTAMP   NOT NULL,
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX ix_password_reset_usuario ON password_reset_tokens (usuario_id);
