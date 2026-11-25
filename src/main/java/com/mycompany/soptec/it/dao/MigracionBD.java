package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.util.AppLogger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Prepara la base de datos para los módulos de servicios, ventas, pagos y órdenes de compra
 * (práctica CRUD del 20/11/2026) sin borrar información.
 *
 * <p>Se ejecuta una sola vez por base de datos y por ejecución del sistema. Crea las tablas
 * nuevas si no existen y agrega las columnas que faltan en {@code pago} y {@code orden_compra}.
 * Usa {@code information_schema}, así que funciona igual en MariaDB (XAMPP) y en MySQL 8.</p>
 *
 * <p>Equivale a importar {@code database/soptec_it_ventas_servicios.sql}.</p>
 */
public final class MigracionBD {

    private static final String MODULO = "Sistema";
    private static final String PROCESO = "MigracionBD";
    private static volatile String urlVerificada;

    private MigracionBD() {
    }

    /** Verifica y, si hace falta, actualiza el esquema de la base activa. */
    public static void asegurarEsquema() throws SoptecException {
        String url = Conexion.getUrlActual();
        if (url.equals(urlVerificada)) {
            return;
        }
        synchronized (MigracionBD.class) {
            if (url.equals(urlVerificada)) {
                return;
            }
            try (Connection con = Conexion.conectarStrict(MODULO, "VERIFICAR_ESQUEMA");
                 Statement st = con.createStatement()) {
                int cambios = 0;
                cambios += crearSiNoExiste(con, st, "servicio",
                        "CREATE TABLE servicio ("
                        + " id_servicio INT(11) AUTO_INCREMENT PRIMARY KEY,"
                        + " nombre_servicio VARCHAR(100) NOT NULL UNIQUE,"
                        + " descripcion VARCHAR(255) NULL,"
                        + " precio DECIMAL(12,2) NOT NULL DEFAULT 0.00,"
                        + " imagen VARCHAR(500) NULL,"
                        + " activo TINYINT(1) NOT NULL DEFAULT 1"
                        + ") ENGINE=InnoDB");
                cambios += crearSiNoExiste(con, st, "cita_servicio",
                        "CREATE TABLE cita_servicio ("
                        + " id_cita INT(11) NOT NULL, id_servicio INT(11) NOT NULL,"
                        + " cantidad INT NOT NULL DEFAULT 1, precio_unitario DECIMAL(12,2) NOT NULL DEFAULT 0.00,"
                        + " PRIMARY KEY (id_cita, id_servicio),"
                        + " CONSTRAINT fk_cs_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita)"
                        + "   ON DELETE CASCADE ON UPDATE CASCADE,"
                        + " CONSTRAINT fk_cs_servicio FOREIGN KEY (id_servicio) REFERENCES servicio(id_servicio)"
                        + "   ON DELETE RESTRICT ON UPDATE CASCADE"
                        + ") ENGINE=InnoDB");
                cambios += crearSiNoExiste(con, st, "venta",
                        "CREATE TABLE venta ("
                        + " id_venta INT(11) AUTO_INCREMENT PRIMARY KEY, folio VARCHAR(20) NULL UNIQUE,"
                        + " id_cliente INT(11) NOT NULL, id_proyecto INT(11) NULL, id_cita INT(11) NULL,"
                        + " fecha_venta DATE NOT NULL, fecha_vencimiento DATE NULL,"
                        + " subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00, iva DECIMAL(12,2) NOT NULL DEFAULT 0.00,"
                        + " total DECIMAL(12,2) NOT NULL DEFAULT 0.00, metodo_pago VARCHAR(30) NULL,"
                        + " estado VARCHAR(30) NOT NULL DEFAULT 'Pendiente', observaciones TEXT NULL,"
                        + " activo TINYINT(1) NOT NULL DEFAULT 1,"
                        + " CONSTRAINT fk_venta_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)"
                        + "   ON DELETE RESTRICT ON UPDATE CASCADE,"
                        + " CONSTRAINT fk_venta_proyecto FOREIGN KEY (id_proyecto) REFERENCES proyecto(id_proyecto)"
                        + "   ON DELETE RESTRICT ON UPDATE CASCADE,"
                        + " CONSTRAINT fk_venta_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita)"
                        + "   ON DELETE RESTRICT ON UPDATE CASCADE"
                        + ") ENGINE=InnoDB");
                cambios += crearSiNoExiste(con, st, "venta_servicio",
                        "CREATE TABLE venta_servicio ("
                        + " id_venta INT(11) NOT NULL, id_servicio INT(11) NOT NULL,"
                        + " cantidad INT NOT NULL DEFAULT 1, precio_unitario DECIMAL(12,2) NOT NULL DEFAULT 0.00,"
                        + " PRIMARY KEY (id_venta, id_servicio),"
                        + " CONSTRAINT fk_vs_venta FOREIGN KEY (id_venta) REFERENCES venta(id_venta)"
                        + "   ON DELETE CASCADE ON UPDATE CASCADE,"
                        + " CONSTRAINT fk_vs_servicio FOREIGN KEY (id_servicio) REFERENCES servicio(id_servicio)"
                        + "   ON DELETE RESTRICT ON UPDATE CASCADE"
                        + ") ENGINE=InnoDB");

                // pago: abonos de una venta
                cambios += hacerOpcional(con, st, "pago", "id_proyecto");
                cambios += agregarColumna(con, st, "pago", "id_venta", "INT(11) NULL AFTER id_proyecto");
                cambios += agregarColumna(con, st, "pago", "metodo_pago", "VARCHAR(30) NULL AFTER fecha_pago");
                cambios += agregarLlave(con, st, "pago", "fk_pago_venta",
                        "FOREIGN KEY (id_venta) REFERENCES venta(id_venta) ON DELETE RESTRICT ON UPDATE CASCADE");

                // orden_compra: proveedor y estado
                cambios += hacerOpcional(con, st, "orden_compra", "id_proyecto");
                cambios += agregarColumna(con, st, "orden_compra", "id_proveedor", "INT(11) NULL AFTER id_proyecto");
                cambios += agregarColumna(con, st, "orden_compra", "estado",
                        "VARCHAR(30) NOT NULL DEFAULT 'Solicitada'");
                cambios += agregarLlave(con, st, "orden_compra", "fk_oc_proveedor",
                        "FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor) ON DELETE RESTRICT ON UPDATE CASCADE");

                if (cambios > 0) {
                    AppLogger.registrarInfo(MODULO, PROCESO, "ACTUALIZAR_ESQUEMA",
                            "Esquema actualizado para servicios, ventas y pagos (" + cambios + " cambios).");
                }
                urlVerificada = url;
            } catch (SQLException ex) {
                throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "ACTUALIZAR_ESQUEMA",
                        "No se pudo preparar la base de datos para servicios y ventas. "
                        + "Importa database/soptec_it_ventas_servicios.sql en phpMyAdmin e intenta de nuevo.", ex);
            }
        }
    }

    /** Fuerza una nueva verificación (lo usan las pruebas al recrear su base). */
    public static void reiniciar() {
        urlVerificada = null;
    }

    private static int crearSiNoExiste(Connection con, Statement st, String tabla, String ddl) throws SQLException {
        if (existe(con, "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                tabla)) {
            return 0;
        }
        st.execute(ddl);
        return 1;
    }

    private static int agregarColumna(Connection con, Statement st, String tabla, String columna, String definicion)
            throws SQLException {
        if (existe(con, "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? AND COLUMN_NAME = ?", tabla, columna)) {
            return 0;
        }
        st.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
        return 1;
    }

    private static int hacerOpcional(Connection con, Statement st, String tabla, String columna) throws SQLException {
        if (existe(con, "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? AND COLUMN_NAME = ? AND IS_NULLABLE = 'YES'", tabla, columna)) {
            return 0;
        }
        st.execute("ALTER TABLE " + tabla + " MODIFY " + columna + " INT(11) NULL");
        return 1;
    }

    private static int agregarLlave(Connection con, Statement st, String tabla, String nombre, String definicion)
            throws SQLException {
        if (existe(con, "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?", tabla, nombre)) {
            return 0;
        }
        st.execute("ALTER TABLE " + tabla + " ADD CONSTRAINT " + nombre + " " + definicion);
        return 1;
    }

    private static boolean existe(Connection con, String sql, String... parametros) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setString(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
