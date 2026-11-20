package com.mycompany.soptec.it.pruebas.excepciones;

import com.mycompany.soptec.it.dao.Conexion;
import com.mycompany.soptec.it.excepciones.ConexionBDException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.EntornoPruebas;
import com.mycompany.soptec.it.pruebas.soporte.LogPruebas;
import com.mycompany.soptec.it.pruebas.soporte.PruebaUnitaria;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.util.AppLogger;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Logs · Registro de errores técnicos")
class RegistroLogTest extends PruebaUnitaria {

    /** [fecha hora] [NIVEL] | Modulo=... | Proceso=... | Operacion=... | TipoExcepcion=... | Detalle=... */
    private static final Pattern FORMATO = Pattern.compile(
            "^\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\] \\[(ERROR|INFO)\\] \\| Modulo=([^|]+) \\| Proceso=([^|]+)"
            + " \\| Operacion=([^|]+) \\| TipoExcepcion=([^|]+) \\| Detalle=(.*)$");

    private static String buscarLinea(List<String> lineas, String contiene) {
        return lineas.stream().filter(l -> l.contains(contiene)).findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró en el log: " + contiene + "\n" + lineas));
    }

    @Test
    @DisplayName("El log de pruebas se genera en logs/soptec_pruebas.log")
    void archivoDeLog() {
        assertTrue(AppLogger.getRutaLog().endsWith(EntornoPruebas.RUTA_LOG_PRUEBAS));
        assertTrue(Files.exists(AppLogger.getRutaLog()));
    }

    @Test
    @DisplayName("Cada error registra fecha, hora, módulo, operación y tipo de excepción")
    void formatoCompleto() {
        int marca = LogPruebas.marca();
        Cliente invalido = new Cliente(0, "", "Sin RFC", "a@b.com", null, null, null, null, null, true);
        assertThrows(ValidacionException.class, () -> new ClienteService().crear(invalido));

        String linea = buscarLinea(LogPruebas.lineasDesde(marca), "Operacion=CREAR_CLIENTE");
        Matcher m = FORMATO.matcher(linea);
        assertTrue(m.matches(), "Formato inesperado: " + linea);
        assertEquals("ERROR", m.group(2));
        assertEquals("Clientes", m.group(3).trim());
        assertEquals("ClienteService", m.group(4).trim());
        assertEquals("CREAR_CLIENTE", m.group(5).trim());
        assertEquals("VALIDACION", m.group(6).trim());

        LocalDateTime fecha = LocalDateTime.parse(m.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        assertTrue(Duration.between(fecha, LocalDateTime.now()).abs().toMinutes() < 2, "Fecha y hora actuales");
    }

    @Test
    @DisplayName("Tipos distintos de excepción quedan identificados (VALIDACION, CONEXION_BD)")
    void tiposDeExcepcion() {
        int marca = LogPruebas.marca();
        assertThrows(ValidacionException.class, () -> new MaterialService().crear(
                new com.mycompany.soptec.it.modelo.Material(0, "Cable", new java.math.BigDecimal("-1"), 1, true)));
        Conexion.setSimularFalloConexion(true);
        assertThrows(ConexionBDException.class, () -> new ClienteService().listar(false));

        List<String> nuevas = LogPruebas.lineasDesde(marca);
        assertTrue(buscarLinea(nuevas, "Modulo=Materiales").contains("TipoExcepcion=VALIDACION"));
        String conexion = buscarLinea(nuevas, "TipoExcepcion=CONEXION_BD");
        assertTrue(conexion.contains("Modulo=Clientes") && conexion.contains("Operacion=LISTAR_CLIENTES"), conexion);
    }

    @Test
    @DisplayName("No se registra la contraseña de la base de datos")
    void sinContrasenaDeConexion() {
        String secreta = "ClaveSuperSecreta2026";
        Conexion.configurar(BaseDatosPrueba.URL_BD, "root", secreta);
        Conexion.setSimularFalloConexion(true);
        int marca = LogPruebas.marca();

        assertThrows(ConexionBDException.class, () -> new ClienteService().listar(false));

        String texto = LogPruebas.textoDesde(marca);
        assertTrue(texto.contains("password=***"), texto);
        assertFalse(texto.contains(secreta), "La contraseña real no debe aparecer en el log");
        assertFalse(texto.contains("allowPublicKeyRetrieval"), "Los parámetros de la URL JDBC se recortan");
    }

    @Test
    @DisplayName("Contraseñas, tokens y claves dentro de mensajes se ocultan con ***")
    void sanitizaInformacionSensible() {
        int marca = LogPruebas.marca();
        AppLogger.registrarError("Autenticacion", "LoginController", "INICIO_SESION", "AUTENTICACION",
                "Intento fallido usuario=admin password=Secreta123 token: abc999 contraseña=hola123", null);
        AppLogger.registrarExcepcion("Sistema", "Conexion", "CONECTAR",
                new RuntimeException("Access denied pwd=Root2026 en jdbc:mysql://localhost:3306/soptec_it?user=root&password=Root2026"));

        String texto = LogPruebas.textoDesde(marca);
        for (String sensible : new String[]{"Secreta123", "abc999", "hola123", "Root2026"}) {
            assertFalse(texto.contains(sensible), "Se filtró información sensible: " + sensible);
        }
        assertTrue(texto.contains("usuario=admin"), "Los datos no sensibles sí se conservan");
    }

    @Test
    @DisplayName("Los saltos de línea no rompen el formato (una línea por evento)")
    void unaLineaPorEvento() {
        int marca = LogPruebas.marca();
        AppLogger.registrarError("Pruebas", "RegistroLogTest", "MULTILINEA", "PRUEBA",
                "linea uno\nlinea dos\r\nlinea tres", null);
        List<String> nuevas = LogPruebas.lineasDesde(marca);
        assertEquals(1, nuevas.size(), nuevas.toString());
        assertTrue(FORMATO.matcher(nuevas.get(0)).matches());
    }

    @Test
    @DisplayName("Las operaciones exitosas también se registran como INFO")
    void registroInfo() {
        int marca = LogPruebas.marca();
        AppLogger.registrarInfo("Clientes", "ClienteService", "CREAR_CLIENTE", "Cliente registrado ID=1");
        String linea = buscarLinea(LogPruebas.lineasDesde(marca), "Cliente registrado ID=1");
        assertTrue(linea.contains("[INFO]") && linea.contains("TipoExcepcion=OPERACION_OK"), linea);
    }
}
