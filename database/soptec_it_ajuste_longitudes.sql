-- SOPTEC-IT | Ajuste de longitudes (solo si ya importaste soptec_it_completa.sql antes del 19/11/2026)
-- El sistema acepta telefonos y numeros de domicilio de hasta 20 caracteres.
USE soptec_it;
ALTER TABLE cliente          MODIFY numero VARCHAR(20);
ALTER TABLE cliente_telefono MODIFY telefono VARCHAR(20) NOT NULL;
ALTER TABLE empleado         MODIFY telefono VARCHAR(20);
ALTER TABLE proveedor        MODIFY telefono_proveedor VARCHAR(20);
