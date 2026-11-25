-- =====================================================================
-- SOPTEC-IT | Base de datos COMPLETA (crear desde cero)
-- Importar en phpMyAdmin (XAMPP). Ya incluye los cambios de
-- soptec_it_crud_actualizacion.sql, NO hace falta importar ese despues.
-- =====================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS soptec_it
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE soptec_it;

-- ---------------- cliente ----------------
DROP TABLE IF EXISTS cliente;
CREATE TABLE cliente (
  id_cliente      INT(11) AUTO_INCREMENT PRIMARY KEY,
  rfc             VARCHAR(13)  UNIQUE,
  nombre_cliente  VARCHAR(100) NOT NULL,
  correo          VARCHAR(100),
  calle           VARCHAR(100),
  numero          VARCHAR(20),
  colonia         VARCHAR(100),
  estado          VARCHAR(50),
  imagen          VARCHAR(500) NULL,
  activo          TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------- cliente_telefono (multivaluado) ----------------
DROP TABLE IF EXISTS cliente_telefono;
CREATE TABLE cliente_telefono (
  id_cliente  INT(11) NOT NULL,
  telefono    VARCHAR(20) NOT NULL,
  activo      TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_cliente, telefono),
  CONSTRAINT cliente_telefono_ibfk_1 FOREIGN KEY (id_cliente)
    REFERENCES cliente(id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- cita (Cliente 1:N Cita) ----------------
DROP TABLE IF EXISTS cita;
CREATE TABLE cita (
  id_cita        INT(11) AUTO_INCREMENT PRIMARY KEY,
  fecha_cita     DATE NOT NULL,
  hora_cita      TIME NOT NULL,
  estado_cita    VARCHAR(30) DEFAULT 'Programada',
  observaciones  TEXT,
  id_cliente     INT(11) NOT NULL,
  activo         TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_cita_cliente FOREIGN KEY (id_cliente)
    REFERENCES cliente(id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- proyecto (Cliente 1:N Proyecto) ----------------
DROP TABLE IF EXISTS proyecto;
CREATE TABLE proyecto (
  id_proyecto         INT(11) AUTO_INCREMENT PRIMARY KEY,
  nombre_proyecto     VARCHAR(150) NOT NULL,
  fecha_inicio        DATE,
  fecha_estimada_fin  DATE,
  estado              VARCHAR(30) DEFAULT 'En proceso',
  costo_total         DECIMAL(12,2) DEFAULT 0.00,
  id_cliente          INT(11) NOT NULL,
  activo              TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT proyecto_ibfk_cliente FOREIGN KEY (id_cliente)
    REFERENCES cliente(id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- levantamiento (Proyecto 1:N, Cita opcional) ----------------
DROP TABLE IF EXISTS levantamiento;
CREATE TABLE levantamiento (
  id_levantamiento        INT(11) AUTO_INCREMENT PRIMARY KEY,
  fecha_visita            DATE NOT NULL,
  croquis                 TEXT NULL,
  observaciones_tecnicas  TEXT,
  activo                  TINYINT(1) NOT NULL DEFAULT 1,
  id_cita                 INT(11) NULL,
  id_proyecto             INT(11) NOT NULL,
  CONSTRAINT levantamiento_ibfk_cita FOREIGN KEY (id_cita)
    REFERENCES cita(id_cita) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT levantamiento_ibfk_proyecto FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- pago (Proyecto 1:N Pago y Venta 1:N Pago) ----------------
DROP TABLE IF EXISTS pago;
CREATE TABLE pago (
  id_pago        INT(11) AUTO_INCREMENT PRIMARY KEY,
  id_proyecto    INT(11) NULL,
  id_venta       INT(11) NULL,
  concepto       VARCHAR(150),
  monto          DECIMAL(12,2) NOT NULL,
  fecha_pago     DATE NOT NULL,
  metodo_pago    VARCHAR(30) NULL,
  estado_cuenta  VARCHAR(30),
  CONSTRAINT fk_pago_proyecto FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_pago_venta FOREIGN KEY (id_venta)
    REFERENCES venta(id_venta) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- empleado ----------------
DROP TABLE IF EXISTS empleado;
CREATE TABLE empleado (
  id_empleado  INT(11) AUTO_INCREMENT PRIMARY KEY,
  nombre       VARCHAR(100) NOT NULL,
  telefono     VARCHAR(20),
  puesto       VARCHAR(50),
  correo       VARCHAR(100),
  imagen       VARCHAR(500) NULL,
  activo       TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------- proyecto_empleado (M:N) ----------------
DROP TABLE IF EXISTS proyecto_empleado;
CREATE TABLE proyecto_empleado (
  id_proyecto  INT(11) NOT NULL,
  id_empleado  INT(11) NOT NULL,
  PRIMARY KEY (id_proyecto, id_empleado),
  CONSTRAINT fk_pe_proyecto FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_pe_empleado FOREIGN KEY (id_empleado)
    REFERENCES empleado(id_empleado) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- proveedor ----------------
DROP TABLE IF EXISTS proveedor;
CREATE TABLE proveedor (
  id_proveedor        INT(11) AUTO_INCREMENT PRIMARY KEY,
  nombre_proveedor    VARCHAR(100) NOT NULL,
  telefono_proveedor  VARCHAR(20),
  correo_proveedor    VARCHAR(100),
  imagen              VARCHAR(500) NULL,
  activo              TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------- material ----------------
DROP TABLE IF EXISTS material;
CREATE TABLE material (
  id_material           INT(11) AUTO_INCREMENT PRIMARY KEY,
  descripcion_material  VARCHAR(255) NOT NULL,
  precio_costo          DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  existencias           INT NOT NULL DEFAULT 0,
  activo                TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------- material_proveedor (M:N) ----------------
DROP TABLE IF EXISTS material_proveedor;
CREATE TABLE material_proveedor (
  id_material   INT(11) NOT NULL,
  id_proveedor  INT(11) NOT NULL,
  PRIMARY KEY (id_material, id_proveedor),
  CONSTRAINT fk_mp_material FOREIGN KEY (id_material)
    REFERENCES material(id_material) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_mp_proveedor FOREIGN KEY (id_proveedor)
    REFERENCES proveedor(id_proveedor) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- proyecto_material (M:N) ----------------
DROP TABLE IF EXISTS proyecto_material;
CREATE TABLE proyecto_material (
  id_proyecto  INT(11) NOT NULL,
  id_material  INT(11) NOT NULL,
  cantidad     DECIMAL(10,2) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_proyecto, id_material),
  CONSTRAINT fk_pm_proyecto FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_pm_material FOREIGN KEY (id_material)
    REFERENCES material(id_material) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- orden_compra (Proveedor 1:N, Proyecto 1:N opcional) ----------------
DROP TABLE IF EXISTS orden_compra;
CREATE TABLE orden_compra (
  id_orden_compra  INT(11) AUTO_INCREMENT PRIMARY KEY,
  id_proyecto      INT(11) NULL,
  id_proveedor     INT(11) NULL,
  fecha_orden      DATE NOT NULL,
  total_compra     DECIMAL(12,2) DEFAULT 0.00,
  tiempo_entrega   VARCHAR(50),
  estado           VARCHAR(30) NOT NULL DEFAULT 'Solicitada',
  CONSTRAINT fk_oc_proyecto FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_oc_proveedor FOREIGN KEY (id_proveedor)
    REFERENCES proveedor(id_proveedor) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- orden_compra_material (M:N) ----------------
DROP TABLE IF EXISTS orden_compra_material;
CREATE TABLE orden_compra_material (
  id_orden_compra  INT(11) NOT NULL,
  id_material      INT(11) NOT NULL,
  cantidad         DECIMAL(10,2) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_orden_compra, id_material),
  CONSTRAINT fk_ocm_orden FOREIGN KEY (id_orden_compra)
    REFERENCES orden_compra(id_orden_compra) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_ocm_material FOREIGN KEY (id_material)
    REFERENCES material(id_material) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- pedido_cliente (Portal del cliente: Cliente 1:N Pedido) ----------------
DROP TABLE IF EXISTS pedido_cliente_material;
DROP TABLE IF EXISTS pedido_cliente;
CREATE TABLE pedido_cliente (
  id_pedido      INT(11) AUTO_INCREMENT PRIMARY KEY,
  id_cliente     INT(11) NOT NULL,
  fecha_pedido   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  estado         VARCHAR(30) NOT NULL DEFAULT 'Apartado',
  total          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  observaciones  TEXT NULL,
  activo         TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_pedcli_cliente FOREIGN KEY (id_cliente)
    REFERENCES cliente(id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- pedido_cliente_material (Pedido M:N Material) ----------------
CREATE TABLE pedido_cliente_material (
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

-- =====================================================================
-- (20/11/2026) Practica CRUD + excepciones: servicios, citas con servicios,
-- ventas con servicios y pagos.
-- =====================================================================

-- ---------------- servicio (catalogo de servicios que ofrece SOPTEC-IT) ----------------
DROP TABLE IF EXISTS venta_servicio;
DROP TABLE IF EXISTS cita_servicio;
DROP TABLE IF EXISTS venta;
DROP TABLE IF EXISTS servicio;
CREATE TABLE servicio (
  id_servicio      INT(11) AUTO_INCREMENT PRIMARY KEY,
  nombre_servicio  VARCHAR(100) NOT NULL UNIQUE,
  descripcion      VARCHAR(255) NULL,
  precio           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  imagen           VARCHAR(500) NULL,
  activo           TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------- cita_servicio (Cita M:N Servicio) ----------------
CREATE TABLE cita_servicio (
  id_cita          INT(11) NOT NULL,
  id_servicio      INT(11) NOT NULL,
  cantidad         INT NOT NULL DEFAULT 1,
  precio_unitario  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (id_cita, id_servicio),
  CONSTRAINT fk_cs_cita FOREIGN KEY (id_cita)
    REFERENCES cita(id_cita) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_cs_servicio FOREIGN KEY (id_servicio)
    REFERENCES servicio(id_servicio) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- venta (factura: Cliente 1:N Venta, opcional proyecto y cita) ----------------
CREATE TABLE venta (
  id_venta           INT(11) AUTO_INCREMENT PRIMARY KEY,
  folio              VARCHAR(20) NULL UNIQUE,
  id_cliente         INT(11) NOT NULL,
  id_proyecto        INT(11) NULL,
  id_cita            INT(11) NULL,
  fecha_venta        DATE NOT NULL,
  fecha_vencimiento  DATE NULL,
  subtotal           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  iva                DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  total              DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  metodo_pago        VARCHAR(30) NULL,
  estado             VARCHAR(30) NOT NULL DEFAULT 'Pendiente',
  observaciones      TEXT NULL,
  activo             TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_venta_cliente FOREIGN KEY (id_cliente)
    REFERENCES cliente(id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_venta_proyecto FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_venta_cita FOREIGN KEY (id_cita)
    REFERENCES cita(id_cita) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------- venta_servicio (Venta M:N Servicio) ----------------
CREATE TABLE venta_servicio (
  id_venta         INT(11) NOT NULL,
  id_servicio      INT(11) NOT NULL,
  cantidad         INT NOT NULL DEFAULT 1,
  precio_unitario  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (id_venta, id_servicio),
  CONSTRAINT fk_vs_venta FOREIGN KEY (id_venta)
    REFERENCES venta(id_venta) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_vs_servicio FOREIGN KEY (id_servicio)
    REFERENCES servicio(id_servicio) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
