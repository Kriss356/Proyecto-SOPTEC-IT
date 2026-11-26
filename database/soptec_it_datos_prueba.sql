-- SOPTEC-IT | Datos de prueba para el video demostrativo
-- Importar DESPUES de soptec_it_completa.sql (sobre la base vacia).
-- Se puede importar las veces que quieras: primero vacia las tablas y reinicia los IDs.
USE soptec_it;
-- Vaciar tablas (de hijas a padres, funciona con la revision de llaves foraneas activada)
-- (Portal del cliente) las tablas de pedidos se crean si aun no existen, para poder vaciarlas
CREATE TABLE IF NOT EXISTS pedido_cliente (
  id_pedido INT(11) AUTO_INCREMENT PRIMARY KEY, id_cliente INT(11) NOT NULL,
  fecha_pedido DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, estado VARCHAR(30) NOT NULL DEFAULT 'Apartado',
  total DECIMAL(12,2) NOT NULL DEFAULT 0.00, observaciones TEXT NULL, activo TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_pedcli_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS pedido_cliente_material (
  id_pedido INT(11) NOT NULL, id_material INT(11) NOT NULL, cantidad INT NOT NULL DEFAULT 1,
  precio_unitario DECIMAL(10,2) NOT NULL DEFAULT 0.00, PRIMARY KEY (id_pedido, id_material),
  CONSTRAINT fk_pcm_pedido FOREIGN KEY (id_pedido) REFERENCES pedido_cliente(id_pedido) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_pcm_material FOREIGN KEY (id_material) REFERENCES material(id_material) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;
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
DELETE FROM venta_servicio;
DELETE FROM cita_servicio;

DELETE FROM pedido_cliente;
ALTER TABLE pedido_cliente AUTO_INCREMENT = 1;
DELETE FROM orden_compra_material;
DELETE FROM orden_compra;
DELETE FROM proyecto_material;
DELETE FROM proyecto_empleado;
DELETE FROM material_proveedor;
DELETE FROM pago;
DELETE FROM venta;
DELETE FROM servicio;
DELETE FROM levantamiento;
DELETE FROM cita;
DELETE FROM proyecto;
DELETE FROM cliente_telefono;
DELETE FROM cliente;
DELETE FROM empleado;
DELETE FROM proveedor;
DELETE FROM material;
ALTER TABLE orden_compra AUTO_INCREMENT = 1;
ALTER TABLE pago AUTO_INCREMENT = 1;
ALTER TABLE venta AUTO_INCREMENT = 1;
ALTER TABLE servicio AUTO_INCREMENT = 1;
ALTER TABLE levantamiento AUTO_INCREMENT = 1;
ALTER TABLE cita AUTO_INCREMENT = 1;
ALTER TABLE proyecto AUTO_INCREMENT = 1;
ALTER TABLE cliente AUTO_INCREMENT = 1;
ALTER TABLE empleado AUTO_INCREMENT = 1;
ALTER TABLE proveedor AUTO_INCREMENT = 1;
ALTER TABLE material AUTO_INCREMENT = 1;

INSERT INTO cliente (id_cliente, rfc, nombre_cliente, correo, calle, numero, colonia, estado, activo) VALUES
(1, 'GOMA850312AB1', 'Ana Gómez Martínez',        'ana.gomez@correo.com',     'Av. Vallarta',     '1520', 'Arcos Vallarta', 'Jalisco', 1),
(2, 'LOPJ900705CD2', 'Jorge López Pérez',         'jorge.lopez@correo.com',   'Calle Morelos',    '88',   'Centro',         'Jalisco', 1),
(3, 'RAMS781120EF3', 'Sofía Ramírez Sánchez',     'sofia.ramirez@correo.com', 'Av. Patria',       '230',  'Jardines Univ.', 'Jalisco', 1),
(4, 'TEC010101GH4',  'Tecnologías del Bajío SA',  'contacto@tecbajio.com',    'Blvd. Industrial', '45-B', 'Zona Industrial','Guanajuato', 1),
(5, 'HERC950214IJ5', 'Carlos Hernández Cruz',     'carlos.hdz@correo.com',    'Calle Juárez',     '12',   'San Andrés',     'Jalisco', 0);

INSERT INTO cliente_telefono (id_cliente, telefono, activo) VALUES
(1, '3312345678', 1), (1, '3398765432', 1),
(2, '3323456789', 1),
(3, '3334567890', 1),
(4, '4771234567', 1), (4, '4777654321', 1),
(5, '3345678901', 0);

INSERT INTO empleado (id_empleado, nombre, telefono, puesto, correo, activo) VALUES
(1, 'Luis Torres Medina',    '3311112222', 'Técnico instalador', 'luis.torres@soptec.com',   1),
(2, 'María Fernández Ruiz',  '3322223333', 'Agente de ventas',   'maria.fernandez@soptec.com', 1),
(3, 'Roberto Castillo Vega', '3333334444', 'Técnico de redes',   'roberto.castillo@soptec.com', 1),
(4, 'Laura Méndez Ortiz',    '3344445555', 'Administradora',     'laura.mendez@soptec.com',  1);

INSERT INTO proveedor (id_proveedor, nombre_proveedor, telefono_proveedor, correo_proveedor, activo) VALUES
(1, 'Redes y Cableado MX',   '3355556666', 'ventas@redesmx.com',    1),
(2, 'FibraTec Distribución', '5512341234', 'contacto@fibratec.com', 1),
(3, 'Ubiquiti Partner GDL',  '3366667777', 'gdl@ubiquitipartner.com', 1);

INSERT INTO material (id_material, descripcion_material, precio_costo, existencias, activo) VALUES
(1, 'Bobina cable UTP Cat6 305 m',   2450.00, 12, 1),
(2, 'Conector RJ45 Cat6 (bolsa 100)', 380.00, 25, 1),
(3, 'Switch administrable 24 puertos', 5890.00, 4, 1),
(4, 'Fibra óptica monomodo 1 km',    7200.00,  3, 1),
(5, 'Access Point Wi-Fi 6',          2150.00,  8, 1);

INSERT INTO material_proveedor (id_material, id_proveedor) VALUES (1,1),(2,1),(3,3),(4,2),(5,3);

INSERT INTO proyecto (id_proyecto, nombre_proyecto, fecha_inicio, fecha_estimada_fin, estado, costo_total, id_cliente, activo) VALUES
(1, 'Red de oficina corporativa',   '2026-11-03', '2026-11-28', 'En proceso', 48500.00, 1, 1),
(2, 'Cableado estructurado bodega', '2026-11-10', '2026-12-05', 'En proceso', 32750.00, 4, 1),
(3, 'Enlace de fibra óptica',       '2026-11-20', '2026-12-15', 'Planeado',   61200.00, 3, 1);

INSERT INTO cita (id_cita, fecha_cita, hora_cita, estado_cita, observaciones, id_cliente, activo) VALUES
(1, '2026-11-02', '10:00:00', 'Atendida',   'Visita para revisar instalación actual', 1, 1),
(2, '2026-11-08', '12:30:00', 'Atendida',   'Medición de bodega',                     4, 1),
(3, '2026-11-25', '09:00:00', 'Programada', 'Revisión de ruta para la fibra',         3, 1),
(4, '2026-11-27', '16:00:00', 'Confirmada', 'Cotización de cámaras',                  2, 1);

INSERT INTO levantamiento (id_levantamiento, fecha_visita, croquis, observaciones_tecnicas, activo, id_cita, id_proyecto) VALUES
(1, '2026-11-02', NULL, 'Se requieren 24 nodos y un rack de 12U',            1, 1, 1),
(2, '2026-11-08', NULL, 'Bodega de 800 m2, canaleta por techo, 16 nodos',    1, 2, 2);

INSERT INTO proyecto_empleado (id_proyecto, id_empleado) VALUES (1,1),(1,2),(2,3);

-- ---------------- (20/11/2026) Catalogo de servicios, citas con servicios, ventas y pagos ----------------
INSERT INTO servicio (id_servicio, nombre_servicio, descripcion, precio, activo) VALUES
(1, 'Levantamiento técnico',            'Visita para medir y revisar el lugar de la instalación', 850.00, 1),
(2, 'Instalación de nodo de red Cat6',  'Tendido de cable, conectorizado y certificación por nodo', 1450.00, 1),
(3, 'Fusión de fibra óptica',           'Fusión y prueba de cada hilo de fibra',                    650.00, 1),
(4, 'Configuración de switch o router', 'Configuración de VLAN, seguridad y respaldo',              1200.00, 1),
(5, 'Mantenimiento preventivo de red',  'Revisión, limpieza y pruebas de la red existente',         1800.00, 1),
(6, 'Instalación de cámara de seguridad','Montaje, cableado y configuración por cámara',            1650.00, 1);

INSERT INTO cita_servicio (id_cita, id_servicio, cantidad, precio_unitario) VALUES
(1, 1, 1, 850.00),
(2, 1, 1, 850.00),
(3, 1, 1, 850.00), (3, 3, 12, 650.00),
(4, 6, 4, 1650.00), (4, 4, 1, 1200.00);

INSERT INTO venta (id_venta, folio, id_cliente, id_proyecto, id_cita, fecha_venta, fecha_vencimiento, subtotal, iva, total, metodo_pago, estado, observaciones, activo) VALUES
(1, 'F-0001', 1, 1, 1, '2026-11-04', '2026-12-04', 35400.00, 5664.00, 41064.00, 'Transferencia', 'Parcial', 'Red de oficina: 24 nodos y configuración', 1),
(2, 'F-0002', 4, 2, 2, '2026-11-11', '2026-12-11', 24050.00, 3848.00, 27898.00, 'Transferencia', 'Pagada',  'Cableado de bodega', 1);

INSERT INTO venta_servicio (id_venta, id_servicio, cantidad, precio_unitario) VALUES
(1, 1, 1, 850.00), (1, 2, 23, 1450.00), (1, 4, 1, 1200.00),
(2, 1, 1, 850.00), (2, 2, 16, 1450.00);

INSERT INTO pago (id_pago, id_proyecto, id_venta, concepto, monto, fecha_pago, metodo_pago, estado_cuenta) VALUES
(1, 1, 1, 'Anticipo 50 %',   20532.00, '2026-11-04', 'Transferencia', 'Abono'),
(2, 2, 2, 'Pago total',      27898.00, '2026-11-11', 'Transferencia', 'Liquidado');

INSERT INTO orden_compra (id_orden_compra, id_proyecto, id_proveedor, fecha_orden, total_compra, tiempo_entrega, estado) VALUES
(1, 1, 1, '2026-11-05', 8490.00, '2026-11-09', 'Recibida');
INSERT INTO orden_compra_material (id_orden_compra, id_material, cantidad) VALUES (1, 1, 3), (1, 2, 3);
-- (26/11/2026) Orden pendiente de recibir para la demo en vivo (Victor: «Registrar recepcion»)
INSERT INTO orden_compra (id_orden_compra, id_proyecto, id_proveedor, fecha_orden, total_compra, tiempo_entrega, estado) VALUES
(2, NULL, 3, '2026-11-26', 10750.00, '2026-12-01', 'Solicitada');
INSERT INTO orden_compra_material (id_orden_compra, id_material, cantidad) VALUES (2, 5, 5);

INSERT INTO proyecto_material (id_proyecto, id_material, cantidad) VALUES (1, 1, 2), (1, 2, 1), (2, 1, 1);
