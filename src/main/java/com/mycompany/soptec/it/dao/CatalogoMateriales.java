package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.util.AppLogger;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Catálogo de material para instalaciones de red, CCTV y fibra (24/11/2026).
 *
 * <p>Se agrega UNA sola vez a la base del sistema (queda marcado en la tabla {@code soptec_config}),
 * así que si después borras o editas un material no vuelve a aparecer. Solo inserta los que no existan
 * con la misma descripción y no toca la base de pruebas ({@code soptec_it_test}).
 * Equivale a importar {@code database/soptec_it_materiales_instalacion.sql}.</p>
 */
final class CatalogoMateriales {

    static final String MARCA = "catalogo_materiales_instalacion_v1";

    /** Proveedor sugerido (por nombre) para cada grupo de material. */
    private static final String PROV_CABLEADO = "Redes y Cableado MX";
    private static final String PROV_FIBRA = "FibraTec Distribución";
    private static final String PROV_EQUIPOS = "Ubiquiti Partner GDL";

    /** {descripción, precio de costo, existencias, proveedor sugerido}. */
    private static final Object[][] MATERIALES = {
        // ---- Cableado de cobre ----
        {"Bobina cable UTP Cat5e 305 m", "1690.00", 10, PROV_CABLEADO},
        {"Bobina cable UTP Cat6A 305 m", "4890.00", 5, PROV_CABLEADO},
        {"Bobina cable UTP Cat6 exterior (intemperie) 305 m", "3350.00", 6, PROV_CABLEADO},
        {"Patch cord Cat6 1 m", "45.00", 120, PROV_CABLEADO},
        {"Patch cord Cat6 3 m", "75.00", 80, PROV_CABLEADO},
        {"Patch cord Cat6 7 m", "130.00", 40, PROV_CABLEADO},
        // ---- Conectividad ----
        {"Jack RJ45 Cat6 keystone", "68.00", 200, PROV_CABLEADO},
        {"Jack RJ45 Cat6A keystone blindado", "145.00", 60, PROV_CABLEADO},
        {"Conector RJ45 Cat5e (bolsa 100)", "210.00", 15, PROV_CABLEADO},
        {"Bota protectora para RJ45 (bolsa 100)", "95.00", 20, PROV_CABLEADO},
        {"Placa de pared (faceplate) 1 puerto", "28.00", 150, PROV_CABLEADO},
        {"Placa de pared (faceplate) 2 puertos", "32.00", 150, PROV_CABLEADO},
        {"Caja de montaje superficial 2 puertos", "48.00", 100, PROV_CABLEADO},
        // ---- Racks y gabinetes ----
        {"Rack abierto de piso 42U", "5450.00", 2, PROV_CABLEADO},
        {"Gabinete de pared 12U con puerta de vidrio", "3890.00", 4, PROV_CABLEADO},
        {"Gabinete de pared 6U", "2150.00", 5, PROV_CABLEADO},
        {"Rack 24U de piso", "4290.00", 3, PROV_CABLEADO},
        {"Patch panel Cat6 24 puertos", "980.00", 10, PROV_CABLEADO},
        {"Patch panel Cat6 48 puertos", "1790.00", 6, PROV_CABLEADO},
        {"Organizador horizontal de cables 1U", "260.00", 20, PROV_CABLEADO},
        {"Organizador vertical para rack 42U", "1350.00", 4, PROV_CABLEADO},
        {"Charola (bandeja) fija para rack 1U", "420.00", 12, PROV_CABLEADO},
        {"Multicontacto (PDU) para rack 8 contactos", "890.00", 8, PROV_CABLEADO},
        {"Tornillos y tuercas enjauladas para rack (kit 50)", "180.00", 25, PROV_CABLEADO},
        // ---- Canalización ----
        {"Canaleta plástica 20x12 mm (tramo 2 m)", "38.00", 300, PROV_CABLEADO},
        {"Canaleta plástica 40x25 mm (tramo 2 m)", "72.00", 150, PROV_CABLEADO},
        {"Tubo conduit PVC 3/4\" (tramo 3 m)", "45.00", 200, PROV_CABLEADO},
        {"Tubo conduit galvanizado 3/4\" (tramo 3 m)", "165.00", 80, PROV_CABLEADO},
        {"Curva conduit PVC 3/4\"", "12.00", 300, PROV_CABLEADO},
        {"Charola tipo malla 10 cm (tramo 3 m)", "520.00", 20, PROV_CABLEADO},
        {"Registro eléctrico 4x4 con tapa", "35.00", 120, PROV_CABLEADO},
        // ---- Fijación y consumibles ----
        {"Cinchos plásticos 30 cm (bolsa 100)", "75.00", 60, PROV_CABLEADO},
        {"Cinta velcro para cables (rollo 5 m)", "110.00", 30, PROV_CABLEADO},
        {"Taquetes y pijas 1/4\" (bolsa 100)", "85.00", 40, PROV_CABLEADO},
        {"Etiquetas para identificación de cables (rollo)", "240.00", 15, PROV_CABLEADO},
        {"Grapas para cable UTP (caja 100)", "65.00", 50, PROV_CABLEADO},
        // ---- Equipo de red ----
        {"Router empresarial (balanceo de carga)", "3450.00", 5, PROV_EQUIPOS},
        {"Router Wi-Fi doble banda (hogar)", "1290.00", 10, PROV_EQUIPOS},
        {"Switch no administrable 8 puertos Gigabit", "690.00", 15, PROV_EQUIPOS},
        {"Switch PoE 8 puertos Gigabit", "1890.00", 8, PROV_EQUIPOS},
        {"Switch administrable PoE 16 puertos", "5290.00", 4, PROV_EQUIPOS},
        {"Switch administrable 48 puertos", "9850.00", 2, PROV_EQUIPOS},
        {"Access Point Wi-Fi 6 para exterior", "3290.00", 4, PROV_EQUIPOS},
        {"Firewall / UTM para pequeña empresa", "8900.00", 2, PROV_EQUIPOS},
        {"Módulo SFP 1G monomodo", "390.00", 20, PROV_EQUIPOS},
        {"No break (UPS) 1500 VA para rack", "4650.00", 3, PROV_EQUIPOS},
        {"Inyector PoE 48 V", "290.00", 20, PROV_EQUIPOS},
        // ---- Fibra óptica ----
        {"Fibra óptica multimodo OM3 12 hilos (por metro)", "38.00", 1000, PROV_FIBRA},
        {"Distribuidor de fibra (ODF) para rack 24 puertos", "1650.00", 5, PROV_FIBRA},
        {"Pigtail SC/UPC monomodo 1.5 m", "45.00", 150, PROV_FIBRA},
        {"Jumper de fibra LC-LC dúplex 3 m", "160.00", 60, PROV_FIBRA},
        {"Jumper de fibra SC-LC dúplex 3 m", "170.00", 40, PROV_FIBRA},
        {"Convertidor de medios (media converter) fibra-cobre", "590.00", 10, PROV_FIBRA},
        {"Manga de empalme para fibra 24 hilos", "780.00", 8, PROV_FIBRA},
        {"Protector de empalme por fusión (bolsa 100)", "120.00", 20, PROV_FIBRA},
        // ---- Videovigilancia (CCTV) ----
        {"Cámara IP tipo domo 4 MP", "1450.00", 12, PROV_EQUIPOS},
        {"Cámara IP tipo bala 4 MP exterior", "1590.00", 10, PROV_EQUIPOS},
        {"Grabador NVR 8 canales PoE", "3890.00", 4, PROV_EQUIPOS},
        {"Disco duro 2 TB para videovigilancia", "1350.00", 6, PROV_EQUIPOS},
        {"Caja de conexiones para cámara (exterior)", "95.00", 40, PROV_EQUIPOS},
    };

    private CatalogoMateriales() {
    }

    /** @return cuántos materiales se agregaron (0 si ya se había aplicado o es la base de pruebas). */
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
        int agregados = 0;
        try (PreparedStatement existe = con.prepareStatement(
                     "SELECT id_material FROM material WHERE LOWER(descripcion_material) = LOWER(?)");
             PreparedStatement inserta = con.prepareStatement(
                     "INSERT INTO material (descripcion_material, precio_costo, existencias, activo) VALUES (?, ?, ?, 1)",
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement proveedor = con.prepareStatement(
                     "SELECT id_proveedor FROM proveedor WHERE nombre_proveedor = ? LIMIT 1");
             PreparedStatement liga = con.prepareStatement(
                     "INSERT IGNORE INTO material_proveedor (id_material, id_proveedor) VALUES (?, ?)")) {
            for (Object[] m : MATERIALES) {
                existe.setString(1, (String) m[0]);
                try (ResultSet rs = existe.executeQuery()) {
                    if (rs.next()) {
                        continue;
                    }
                }
                inserta.setString(1, (String) m[0]);
                inserta.setBigDecimal(2, new BigDecimal((String) m[1]));
                inserta.setInt(3, (Integer) m[2]);
                inserta.executeUpdate();
                agregados++;
                int idMaterial = 0;
                try (ResultSet k = inserta.getGeneratedKeys()) {
                    if (k.next()) {
                        idMaterial = k.getInt(1);
                    }
                }
                proveedor.setString(1, (String) m[3]);
                try (ResultSet rp = proveedor.executeQuery()) {
                    if (idMaterial > 0 && rp.next()) {
                        liga.setInt(1, idMaterial);
                        liga.setInt(2, rp.getInt(1));
                        liga.executeUpdate();
                    }
                }
            }
        }
        try (PreparedStatement marca = con.prepareStatement("INSERT INTO soptec_config (clave, valor) VALUES (?, ?)")) {
            marca.setString(1, MARCA);
            marca.setString(2, "Aplicado: " + agregados + " materiales");
            marca.executeUpdate();
        }
        AppLogger.registrarInfo("Materiales", "CatalogoMateriales", "SEMBRAR_CATALOGO",
                "Catálogo de instalación agregado: " + agregados + " materiales nuevos.");
        return agregados;
    }

    /** Script SQL equivalente (para importarlo a mano en phpMyAdmin). */
    static String comoSql() {
        StringBuilder sb = new StringBuilder();
        for (Object[] m : MATERIALES) {
            String d = ((String) m[0]).replace("'", "''");
            sb.append("INSERT INTO material (descripcion_material, precio_costo, existencias, activo)\n  SELECT '")
                    .append(d).append("', ").append(m[1]).append(", ").append(m[2])
                    .append(", 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE descripcion_material = '")
                    .append(d).append("');\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.print(comoSql());
    }
}
