-- =====================================================================
-- SOPTEC-IT · Catálogo de material para instalaciones (24/11/2026)
-- NO es obligatorio importarlo: el sistema lo agrega solo una vez al iniciar sesión.
-- Solo inserta los materiales que aún no existan (no duplica ni borra nada).
-- =====================================================================
SET NAMES utf8mb4;
USE soptec_it;

INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Bobina cable UTP Cat5e 305 m', 1690.00, 10, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Bobina cable UTP Cat5e 305 m');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Bobina cable UTP Cat6A 305 m', 4890.00, 5, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Bobina cable UTP Cat6A 305 m');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Bobina cable UTP Cat6 exterior (intemperie) 305 m', 3350.00, 6, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Bobina cable UTP Cat6 exterior (intemperie) 305 m');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Patch cord Cat6 1 m', 45.00, 120, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Patch cord Cat6 1 m');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Patch cord Cat6 3 m', 75.00, 80, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Patch cord Cat6 3 m');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Patch cord Cat6 7 m', 130.00, 40, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Patch cord Cat6 7 m');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Jack RJ45 Cat6 keystone', 68.00, 200, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Jack RJ45 Cat6 keystone');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Jack RJ45 Cat6A keystone blindado', 145.00, 60, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Jack RJ45 Cat6A keystone blindado');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Conector RJ45 Cat5e (bolsa 100)', 210.00, 15, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Conector RJ45 Cat5e (bolsa 100)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Bota protectora para RJ45 (bolsa 100)', 95.00, 20, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Bota protectora para RJ45 (bolsa 100)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Placa de pared (faceplate) 1 puerto', 28.00, 150, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Placa de pared (faceplate) 1 puerto');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Placa de pared (faceplate) 2 puertos', 32.00, 150, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Placa de pared (faceplate) 2 puertos');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Caja de montaje superficial 2 puertos', 48.00, 100, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Caja de montaje superficial 2 puertos');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Rack abierto de piso 42U', 5450.00, 2, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Rack abierto de piso 42U');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Gabinete de pared 12U con puerta de vidrio', 3890.00, 4, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Gabinete de pared 12U con puerta de vidrio');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Gabinete de pared 6U', 2150.00, 5, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Gabinete de pared 6U');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Rack 24U de piso', 4290.00, 3, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Rack 24U de piso');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Patch panel Cat6 24 puertos', 980.00, 10, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Patch panel Cat6 24 puertos');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Patch panel Cat6 48 puertos', 1790.00, 6, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Patch panel Cat6 48 puertos');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Organizador horizontal de cables 1U', 260.00, 20, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Organizador horizontal de cables 1U');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Organizador vertical para rack 42U', 1350.00, 4, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Organizador vertical para rack 42U');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Charola (bandeja) fija para rack 1U', 420.00, 12, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Charola (bandeja) fija para rack 1U');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Multicontacto (PDU) para rack 8 contactos', 890.00, 8, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Multicontacto (PDU) para rack 8 contactos');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Tornillos y tuercas enjauladas para rack (kit 50)', 180.00, 25, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Tornillos y tuercas enjauladas para rack (kit 50)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Canaleta plástica 20x12 mm (tramo 2 m)', 38.00, 300, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Canaleta plástica 20x12 mm (tramo 2 m)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Canaleta plástica 40x25 mm (tramo 2 m)', 72.00, 150, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Canaleta plástica 40x25 mm (tramo 2 m)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Tubo conduit PVC 3/4" (tramo 3 m)', 45.00, 200, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Tubo conduit PVC 3/4" (tramo 3 m)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Tubo conduit galvanizado 3/4" (tramo 3 m)', 165.00, 80, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Tubo conduit galvanizado 3/4" (tramo 3 m)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Curva conduit PVC 3/4"', 12.00, 300, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Curva conduit PVC 3/4"');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Charola tipo malla 10 cm (tramo 3 m)', 520.00, 20, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Charola tipo malla 10 cm (tramo 3 m)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Registro eléctrico 4x4 con tapa', 35.00, 120, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Registro eléctrico 4x4 con tapa');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Cinchos plásticos 30 cm (bolsa 100)', 75.00, 60, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Cinchos plásticos 30 cm (bolsa 100)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Cinta velcro para cables (rollo 5 m)', 110.00, 30, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Cinta velcro para cables (rollo 5 m)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Taquetes y pijas 1/4" (bolsa 100)', 85.00, 40, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Taquetes y pijas 1/4" (bolsa 100)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Etiquetas para identificación de cables (rollo)', 240.00, 15, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Etiquetas para identificación de cables (rollo)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Grapas para cable UTP (caja 100)', 65.00, 50, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Grapas para cable UTP (caja 100)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Router empresarial (balanceo de carga)', 3450.00, 5, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Router empresarial (balanceo de carga)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Router Wi-Fi doble banda (hogar)', 1290.00, 10, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Router Wi-Fi doble banda (hogar)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Switch no administrable 8 puertos Gigabit', 690.00, 15, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Switch no administrable 8 puertos Gigabit');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Switch PoE 8 puertos Gigabit', 1890.00, 8, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Switch PoE 8 puertos Gigabit');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Switch administrable PoE 16 puertos', 5290.00, 4, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Switch administrable PoE 16 puertos');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Switch administrable 48 puertos', 9850.00, 2, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Switch administrable 48 puertos');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Access Point Wi-Fi 6 para exterior', 3290.00, 4, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Access Point Wi-Fi 6 para exterior');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Firewall / UTM para pequeña empresa', 8900.00, 2, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Firewall / UTM para pequeña empresa');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Módulo SFP 1G monomodo', 390.00, 20, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Módulo SFP 1G monomodo');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'No break (UPS) 1500 VA para rack', 4650.00, 3, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'No break (UPS) 1500 VA para rack');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Inyector PoE 48 V', 290.00, 20, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Inyector PoE 48 V');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Fibra óptica multimodo OM3 12 hilos (por metro)', 38.00, 1000, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Fibra óptica multimodo OM3 12 hilos (por metro)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Distribuidor de fibra (ODF) para rack 24 puertos', 1650.00, 5, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Distribuidor de fibra (ODF) para rack 24 puertos');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Pigtail SC/UPC monomodo 1.5 m', 45.00, 150, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Pigtail SC/UPC monomodo 1.5 m');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Jumper de fibra LC-LC dúplex 3 m', 160.00, 60, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Jumper de fibra LC-LC dúplex 3 m');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Jumper de fibra SC-LC dúplex 3 m', 170.00, 40, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Jumper de fibra SC-LC dúplex 3 m');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Convertidor de medios (media converter) fibra-cobre', 590.00, 10, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Convertidor de medios (media converter) fibra-cobre');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Manga de empalme para fibra 24 hilos', 780.00, 8, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Manga de empalme para fibra 24 hilos');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Protector de empalme por fusión (bolsa 100)', 120.00, 20, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Protector de empalme por fusión (bolsa 100)');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Cámara IP tipo domo 4 MP', 1450.00, 12, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Cámara IP tipo domo 4 MP');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Cámara IP tipo bala 4 MP exterior', 1590.00, 10, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Cámara IP tipo bala 4 MP exterior');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Grabador NVR 8 canales PoE', 3890.00, 4, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Grabador NVR 8 canales PoE');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Disco duro 2 TB para videovigilancia', 1350.00, 6, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Disco duro 2 TB para videovigilancia');
INSERT INTO material (descripcion_material, precio_costo, existencias, activo)
  SELECT 'Caja de conexiones para cámara (exterior)', 95.00, 40, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = 'Caja de conexiones para cámara (exterior)');

-- Marca para que el sistema no lo vuelva a agregar
CREATE TABLE IF NOT EXISTS soptec_config (clave VARCHAR(80) PRIMARY KEY, valor VARCHAR(255) NULL) ENGINE=InnoDB;
INSERT IGNORE INTO soptec_config (clave, valor) VALUES ('catalogo_materiales_instalacion_v1', 'Importado desde phpMyAdmin');
