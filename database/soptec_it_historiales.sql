-- =====================================================================
-- SOPTEC-IT · Historial de avances (Instalaciones) y de movimientos del almacén
-- (24/11/2026). NO es obligatorio importarlo: el sistema crea estas tablas solo
-- al abrirse (MigracionBD). Sirve como respaldo/documentación. No borra nada.
-- =====================================================================
USE soptec_it;

CREATE TABLE IF NOT EXISTS avance_proyecto (
  id_avance       INT(11) AUTO_INCREMENT PRIMARY KEY,
  id_proyecto     INT(11) NOT NULL,
  fecha           DATE NOT NULL,
  horas           INT NOT NULL DEFAULT 0,
  tarea           VARCHAR(100) NOT NULL,
  id_empleado     INT(11) NULL,
  porcentaje      INT NOT NULL DEFAULT 0,
  estado          VARCHAR(30) NULL,
  material_usado  VARCHAR(255) NULL,
  observaciones   TEXT NULL,
  evidencias      TEXT NULL,            -- rutas de las fotos separadas por |
  CONSTRAINT fk_avance_proyecto FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_avance_empleado FOREIGN KEY (id_empleado)
    REFERENCES empleado(id_empleado) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS movimiento_almacen (
  id_movimiento  INT(11) AUTO_INCREMENT PRIMARY KEY,
  fecha          DATE NOT NULL,
  tipo           VARCHAR(10) NOT NULL,  -- 'Entrada' o 'Salida'
  id_material    INT(11) NOT NULL,
  cantidad       INT NOT NULL,
  id_proyecto    INT(11) NULL,          -- solo en salidas a un proyecto
  id_empleado    INT(11) NULL,
  referencia     VARCHAR(100) NULL,     -- p. ej. OC-0044
  observaciones  TEXT NULL,
  CONSTRAINT fk_mov_material FOREIGN KEY (id_material)
    REFERENCES material(id_material) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_mov_proyecto FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_mov_empleado FOREIGN KEY (id_empleado)
    REFERENCES empleado(id_empleado) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Documento del técnico adjunto al levantamiento (ruta dentro de Documentos/SOPTEC-IT/documentos).
-- Si la columna ya existe, MySQL mostrará "Duplicate column name": se puede ignorar.
ALTER TABLE levantamiento ADD COLUMN documento VARCHAR(500) NULL;
