-- =====================================================================
-- SOPTEC-IT | Tablas del PORTAL DEL CLIENTE (tienda / pedidos)
-- Solo si tu base soptec_it ya existía antes del 19/11/2026.
-- No borra nada: usa CREATE TABLE IF NOT EXISTS.
-- (El sistema también las crea solo la primera vez que se usa la tienda.)
--
-- Las citas que pide el cliente se guardan en la tabla cita que ya existe,
-- con estado_cita = 'Solicitada' y observaciones que empiezan con
-- "[Portal del cliente]". No hace falta modificar la tabla cita.
-- =====================================================================
SET NAMES utf8mb4;
USE soptec_it;

-- ---------------- pedido_cliente (Cliente 1:N Pedido) ----------------
CREATE TABLE IF NOT EXISTS pedido_cliente (
  id_pedido      INT(11) AUTO_INCREMENT PRIMARY KEY,
  id_cliente     INT(11) NOT NULL,
  fecha_pedido   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  estado         VARCHAR(30) NOT NULL DEFAULT 'Apartado',   -- Apartado | Listo para recoger | Entregado | Cancelado
  total          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  observaciones  TEXT NULL,
  activo         TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_pedcli_cliente FOREIGN KEY (id_cliente)
    REFERENCES cliente(id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- pedido_cliente_material (Pedido M:N Material) ----------------
CREATE TABLE IF NOT EXISTS pedido_cliente_material (
  id_pedido        INT(11) NOT NULL,
  id_material      INT(11) NOT NULL,
  cantidad         INT NOT NULL DEFAULT 1,
  precio_unitario  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (id_pedido, id_material),
  CONSTRAINT fk_pcm_pedido FOREIGN KEY (id_pedido)
    REFERENCES pedido_cliente(id_pedido) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_pcm_material FOREIGN KEY (id_material)
    REFERENCES material(id_material) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;
