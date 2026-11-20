-- SOPTEC-IT | Datos de prueba para el video demostrativo
-- Importar DESPUES de soptec_it_completa.sql (sobre la base vacia).
-- Se puede importar las veces que quieras: primero vacia las tablas y reinicia los IDs.
USE soptec_it;
-- Vaciar tablas (de hijas a padres, funciona con la revision de llaves foraneas activada)
DELETE FROM orden_compra_material;
DELETE FROM orden_compra;
DELETE FROM proyecto_material;
DELETE FROM proyecto_empleado;
DELETE FROM material_proveedor;
DELETE FROM pago;
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
