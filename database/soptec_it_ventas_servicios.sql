-- =====================================================================
-- SOPTEC-IT | Actualizacion: servicios, citas con servicios, ventas y pagos
-- Practica "CRUD con gestion de excepciones y tolerancia a fallos" (20/11/2026)
--
-- Usalo SOLO si tu base soptec_it ya existia antes del 20/11/2026.
-- NO borra nada: crea las tablas nuevas si no existen y agrega columnas.
-- (El sistema tambien hace estos cambios solo al abrirlo por primera vez.)
--
-- Tablas nuevas: servicio, cita_servicio (M:N), venta, venta_servicio (M:N)
-- Cambios: pago.id_venta, pago.metodo_pago, orden_compra.id_proveedor,
--          orden_compra.estado; pago.id_proyecto y orden_compra.id_proyecto
--          pasan a ser opcionales.
-- Sintaxis para MariaDB (la que trae XAMPP).
-- =====================================================================
SET NAMES utf8mb4;
USE soptec_it;

-- (Practica CRUD 20/11/2026) Tablas de servicios y ventas: se crean si aun no existen
CREATE TABLE IF NOT EXISTS servicio (
  id_servicio INT(11) AUTO_INCREMENT PRIMARY KEY, nombre_servicio VARCHAR(100) NOT NULL UNIQUE,
  descripcion VARCHAR(255) NULL, precio DECIMAL(12,2) NOT NULL DEFAULT 0.00, imagen VARCHAR(500) NULL,
  activo TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS cita_servicio (
  id_cita INT(11) NOT NULL, id_servicio INT(11) NOT NULL, cantidad INT NOT NULL DEFAULT 1,
  precio_unitario DECIMAL(12,2) NOT NULL DEFAULT 0.00, PRIMARY KEY (id_cita, id_servicio),
  CONSTRAINT fk_cs_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_cs_servicio FOREIGN KEY (id_servicio) REFERENCES servicio(id_servicio) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS venta (
  id_venta INT(11) AUTO_INCREMENT PRIMARY KEY, folio VARCHAR(20) NULL UNIQUE, id_cliente INT(11) NOT NULL,
  id_proyecto INT(11) NULL, id_cita INT(11) NULL, fecha_venta DATE NOT NULL, fecha_vencimiento DATE NULL,
  subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00, iva DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  total DECIMAL(12,2) NOT NULL DEFAULT 0.00, metodo_pago VARCHAR(30) NULL,
  estado VARCHAR(30) NOT NULL DEFAULT 'Pendiente', observaciones TEXT NULL, activo TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_venta_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_venta_proyecto FOREIGN KEY (id_proyecto) REFERENCES proyecto(id_proyecto) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_venta_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS venta_servicio (
  id_venta INT(11) NOT NULL, id_servicio INT(11) NOT NULL, cantidad INT NOT NULL DEFAULT 1,
  precio_unitario DECIMAL(12,2) NOT NULL DEFAULT 0.00, PRIMARY KEY (id_venta, id_servicio),
  CONSTRAINT fk_vs_venta FOREIGN KEY (id_venta) REFERENCES venta(id_venta) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_vs_servicio FOREIGN KEY (id_servicio) REFERENCES servicio(id_servicio) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;
-- pago: ahora tambien se relaciona con la venta (abonos de una factura)
ALTER TABLE pago MODIFY id_proyecto INT(11) NULL;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS id_venta INT(11) NULL AFTER id_proyecto;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS metodo_pago VARCHAR(30) NULL AFTER fecha_pago;
ALTER TABLE pago ADD CONSTRAINT fk_pago_venta FOREIGN KEY IF NOT EXISTS (id_venta) REFERENCES venta(id_venta) ON DELETE RESTRICT ON UPDATE CASCADE;
-- orden_compra: ahora guarda el proveedor y el estado; el proyecto es opcional (compra para stock)
ALTER TABLE orden_compra MODIFY id_proyecto INT(11) NULL;
ALTER TABLE orden_compra ADD COLUMN IF NOT EXISTS id_proveedor INT(11) NULL AFTER id_proyecto;
ALTER TABLE orden_compra ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'Solicitada';
ALTER TABLE orden_compra ADD CONSTRAINT fk_oc_proveedor FOREIGN KEY IF NOT EXISTS (id_proveedor) REFERENCES proveedor(id_proveedor) ON DELETE RESTRICT ON UPDATE CASCADE;
