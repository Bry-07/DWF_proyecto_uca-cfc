-- La revisión de la cuenta permite invalidar JWT tras cualquier cambio del usuario.
ALTER TABLE usuarios ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Reparar estados históricos sin inventar dinero recibido.
UPDATE pagos SET estado = CASE
    WHEN monto_pagado = 0 THEN 'PENDIENTE'
    WHEN monto_pagado < monto_esperado THEN 'PARCIAL'
    ELSE 'PAGADO'
END;

ALTER TABLE pagos ADD CONSTRAINT ck_pagos_estado_monto CHECK (
    (monto_pagado = 0 AND estado = 'PENDIENTE') OR
    (monto_pagado > 0 AND monto_pagado < monto_esperado AND estado = 'PARCIAL') OR
    (monto_pagado = monto_esperado AND estado = 'PAGADO')
);

-- Un contador por año, incrementado atómicamente dentro de la transacción del comprobante.
CREATE TABLE comprobante_correlativos (
    anio INTEGER PRIMARY KEY,
    ultimo BIGINT NOT NULL CHECK (ultimo > 0)
);

INSERT INTO comprobante_correlativos (anio, ultimo)
SELECT split_part(numero, '-', 2)::INTEGER, MAX(split_part(numero, '-', 3)::BIGINT)
FROM comprobantes
WHERE numero ~ '^CFC-[0-9]{4}-[0-9]+$'
GROUP BY split_part(numero, '-', 2)::INTEGER;
