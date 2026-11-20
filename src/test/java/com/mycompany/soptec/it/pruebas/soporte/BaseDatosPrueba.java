package com.mycompany.soptec.it.pruebas.soporte;

import com.mycompany.soptec.it.dao.Conexion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Administra la base de datos INDEPENDIENTE de pruebas {@code soptec_it_test}.
 *
 * <p>Al iniciar la ejecución crea (o recrea desde cero) la BD con el script
 * {@code database/soptec_it_test.sql} y apunta la clase {@link Conexion} hacia ella,
 * de modo que las pruebas nunca modifican la base de producción {@code soptec_it}.</p>
 *
 * <p>Se puede cambiar el servidor o las credenciales con propiedades del sistema:
 * {@code -Dsoptec.test.host=localhost:3306 -Dsoptec.test.usuario=root -Dsoptec.test.password=}</p>
 */
public final class BaseDatosPrueba {

    public static final String NOMBRE_BD = "soptec_it_test";
    private static final String HOST = System.getProperty("soptec.test.host", "localhost:3306");
    private static final String USUARIO = System.getProperty("soptec.test.usuario", "root");
    private static final String PASSWORD = System.getProperty("soptec.test.password", "");
    private static final String OPCIONES = "serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true"
            + "&connectTimeout=3000&characterEncoding=UTF-8";
    public static final String URL_SERVIDOR = "jdbc:mysql://" + HOST + "/?" + OPCIONES;
    public static final String URL_BD = "jdbc:mysql://" + HOST + "/" + NOMBRE_BD + "?" + OPCIONES;

    private static Boolean disponible;
    private static String motivoNoDisponible = "";

    private BaseDatosPrueba() {
    }

    /**
     * Crea la BD de pruebas una sola vez por ejecución y configura la conexión hacia ella.
     *
     * @return true si MySQL está disponible y la BD quedó lista.
     */
    public static synchronized boolean preparar() {
        if (disponible == null) {
            try (Connection con = DriverManager.getConnection(URL_SERVIDOR, USUARIO, PASSWORD);
                 Statement st = con.createStatement()) {
                for (String sentencia : leerScript()) {
                    st.execute(sentencia);
                }
                disponible = true;
            } catch (SQLException | IOException ex) {
                disponible = false;
                motivoNoDisponible = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            }
        }
        configurarConexion();
        return disponible;
    }

    /** Apunta la aplicación a soptec_it_test y quita cualquier simulación de fallo. */
    public static void configurarConexion() {
        Conexion.setSimularFalloConexion(false);
        Conexion.configurar(URL_BD, USUARIO, PASSWORD);
    }

    public static String getMotivoNoDisponible() {
        return motivoNoDisponible;
    }

    /** Ejecuta una sentencia directa sobre la BD de pruebas (para preparar escenarios). */
    public static int ejecutar(String sql, Object... parametros) throws SQLException {
        try (Connection con = DriverManager.getConnection(URL_BD, USUARIO, PASSWORD);
             PreparedStatement ps = con.prepareStatement(sql)) {
            asignar(ps, parametros);
            return ps.executeUpdate();
        }
    }

    /** Devuelve el resultado de un SELECT COUNT(*) sobre la BD de pruebas (para verificar sin pasar por el DAO). */
    public static int contar(String sql, Object... parametros) throws SQLException {
        try (Connection con = DriverManager.getConnection(URL_BD, USUARIO, PASSWORD);
             PreparedStatement ps = con.prepareStatement(sql)) {
            asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void asignar(PreparedStatement ps, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, parametros[i]);
        }
    }

    private static List<String> leerScript() throws IOException {
        Path script = localizarScript();
        StringBuilder sinComentarios = new StringBuilder();
        for (String linea : Files.readAllLines(script, StandardCharsets.UTF_8)) {
            String l = linea.trim();
            if (!l.startsWith("--") && !l.isEmpty()) {
                sinComentarios.append(linea).append('\n');
            }
        }
        List<String> sentencias = new ArrayList<>();
        for (String s : sinComentarios.toString().split(";")) {
            if (!s.isBlank()) {
                sentencias.add(s.trim());
            }
        }
        if (sentencias.stream().noneMatch(s -> s.contains("USE " + NOMBRE_BD))) {
            throw new IOException("El script de pruebas no apunta a " + NOMBRE_BD + "; se cancela por seguridad.");
        }
        return sentencias;
    }

    private static Path localizarScript() throws IOException {
        Path actual = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 4 && actual != null; i++) {
            Path candidato = actual.resolve("database").resolve("soptec_it_test.sql");
            if (Files.exists(candidato)) {
                return candidato;
            }
            actual = actual.getParent();
        }
        throw new IOException("No se encontró database/soptec_it_test.sql");
    }
}
