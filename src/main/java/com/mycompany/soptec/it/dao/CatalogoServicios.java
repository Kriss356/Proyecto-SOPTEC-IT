package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.util.AppLogger;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Catálogo base de servicios de SOPTEC-IT (26/11/2026).
 *
 * <p>Si la tabla {@code servicio} está vacía, el combo «Servicio del catálogo» de Citas y de
 * Ventas aparece en blanco y no se puede elegir nada. Para evitarlo, al abrir el sistema se
 * agregan estos servicios <b>una sola vez</b> y solo si la tabla no tiene ninguno (queda marcado
 * en {@code soptec_config}, así que si después los borras o editas no vuelven a aparecer).
 * No toca la base de pruebas ({@code soptec_it_test}). Son los mismos que
 * {@code database/soptec_it_datos_prueba.sql}.</p>
 */
final class CatalogoServicios {

    static final String MARCA = "catalogo_servicios_base_v1";

    /** {nombre, descripción, precio sin IVA}. */
    private static final String[][] SERVICIOS = {
        {"Levantamiento técnico", "Visita para medir y revisar el lugar de la instalación", "850.00"},
        {"Instalación de nodo de red Cat6", "Tendido de cable, conectorizado y certificación por nodo", "1450.00"},
        {"Fusión de fibra óptica", "Fusión y prueba de cada hilo de fibra", "650.00"},
        {"Configuración de switch o router", "Configuración de VLAN, seguridad y respaldo", "1200.00"},
        {"Mantenimiento preventivo de red", "Revisión, limpieza y pruebas de la red existente", "1800.00"},
        {"Instalación de cámara de seguridad", "Montaje, cableado y configuración por cámara", "1650.00"},
    };

    private CatalogoServicios() {
    }

    /** @return cuántos servicios se agregaron (0 si ya había servicios, ya se aplicó o es la base de pruebas). */
    static int sembrar(Connection con, Statement st, String url) throws SQLException {
        if (url != null && url.toLowerCase().contains("_test")) {
            return 0;
        }
        st.execute("CREATE TABLE IF NOT EXISTS soptec_config ("
                + " clave VARCHAR(80) PRIMARY KEY, valor VARCHAR(255) NULL) ENGINE=InnoDB");
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM soptec_config WHERE clave = ?")) {
            ps.setString(1, MARCA);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return 0;
                }
            }
        }
        int existentes;
        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM servicio")) {
            existentes = rs.next() ? rs.getInt(1) : 0;
        }
        int agregados = 0;
        if (existentes == 0) {
            try (PreparedStatement inserta = con.prepareStatement(
                    "INSERT INTO servicio (nombre_servicio, descripcion, precio, activo) VALUES (?, ?, ?, 1)")) {
                for (String[] s : SERVICIOS) {
                    inserta.setString(1, s[0]);
                    inserta.setString(2, s[1]);
                    inserta.setBigDecimal(3, new BigDecimal(s[2]));
                    inserta.executeUpdate();
                    agregados++;
                }
            }
        }
        try (PreparedStatement marca = con.prepareStatement("INSERT INTO soptec_config (clave, valor) VALUES (?, ?)")) {
            marca.setString(1, MARCA);
            marca.setString(2, existentes == 0 ? "Aplicado: " + agregados + " servicios"
                    : "No necesario: ya había " + existentes + " servicios");
            marca.executeUpdate();
        }
        if (agregados > 0) {
            AppLogger.registrarInfo("Servicios", "CatalogoServicios", "SEMBRAR_CATALOGO",
                    "El catálogo de servicios estaba vacío: se agregaron " + agregados + " servicios base.");
        }
        return agregados;
    }
}
