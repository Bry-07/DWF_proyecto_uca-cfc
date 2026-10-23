-- =====================================================================
-- Fase 3 · El enunciado define los estados de una cotización como
-- Pendiente, En proceso, Aprobada y Rechazada. El estado GENERADA de las
-- fases anteriores pasa a llamarse EN_PROCESO.
-- =====================================================================

ALTER TABLE cotizaciones DROP CONSTRAINT ck_cotizaciones_estado;

UPDATE cotizaciones SET estado = 'EN_PROCESO' WHERE estado = 'GENERADA';

ALTER TABLE cotizaciones ADD CONSTRAINT ck_cotizaciones_estado
    CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'APROBADA', 'RECHAZADA'));
