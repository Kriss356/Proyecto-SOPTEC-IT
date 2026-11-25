package com.mycompany.soptec.it.pruebas.soporte;

import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Levantamiento;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.modelo.Servicio;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fábrica de datos de prueba con valores únicos (RFC, correo, teléfono) para que las pruebas no choquen entre sí.
 */
public final class DatosPrueba {

    private static final AtomicInteger CONTADOR = new AtomicInteger();
    private static final long BASE = (System.currentTimeMillis() / 1000) % 900000;

    /** PNG real de 1x1 píxel. */
    public static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
    /** GIF real de 1x1 píxel. */
    public static final byte[] GIF_1X1 = Base64.getDecoder().decode(
            "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    private DatosPrueba() {
    }

    public static int siguiente() {
        return CONTADOR.incrementAndGet();
    }

    /** RFC válido y único: 3 letras + 6 dígitos + 3 alfanuméricos. */
    public static String rfcUnico() {
        int n = siguiente();
        return "TST" + String.format("%06d", (BASE + n) % 1000000) + "A" + String.format("%02d", n % 100);
    }

    public static String correoUnico(String prefijo) {
        return prefijo + "." + BASE + "." + siguiente() + "@pruebas.soptec.com";
    }

    public static String telefonoUnico() {
        return "33" + String.format("%08d", (BASE * 13 + siguiente()) % 100000000);
    }

    public static Cliente nuevoCliente() {
        int n = siguiente();
        return new Cliente(0, rfcUnico(), "Cliente Prueba " + n, correoUnico("cliente"),
                "Av. Vallarta", "100", "Centro", "Jalisco", null, true);
    }

    public static Empleado nuevoEmpleado(String puesto) {
        int n = siguiente();
        return new Empleado(0, "Empleado Prueba " + n, telefonoUnico(), puesto, correoUnico("empleado"), null, true);
    }

    public static Material nuevoMaterial() {
        return new Material(0, "Material Prueba " + BASE + "-" + siguiente(), new BigDecimal("150.75"), 10, true);
    }

    public static Proveedor nuevoProveedor() {
        return new Proveedor(0, "Proveedor Prueba " + BASE + "-" + siguiente(), telefonoUnico(),
                correoUnico("proveedor"), null, true);
    }

    public static Proyecto nuevoProyecto(int idCliente) {
        return new Proyecto(0, "Proyecto Prueba " + siguiente(), LocalDate.now(), LocalDate.now().plusDays(30),
                "En proceso", new BigDecimal("25000.00"), idCliente, true, null, null);
    }

    public static Cita nuevaCita(int idCliente) {
        return new Cita(0, LocalDate.now().plusDays(2), LocalTime.of(10, 30), "Programada",
                "Visita técnica de prueba", idCliente, true, null, null);
    }

    public static Levantamiento nuevoLevantamiento(int idProyecto, Integer idCita) {
        return new Levantamiento(0, LocalDate.now(), null, "24 nodos Cat6 de prueba",
                idProyecto, idCita, true, null, null, null);
    }

    public static Servicio nuevoServicio(String precio) {
        return new Servicio(0, "Servicio Prueba " + BASE + "-" + siguiente(), "Servicio de prueba",
                new BigDecimal(precio), null, true);
    }

    public static Path crearPng(Path carpeta, String nombre) throws IOException {
        Files.createDirectories(carpeta);
        return Files.write(carpeta.resolve(nombre), PNG_1X1);
    }

    public static Path crearArchivoTexto(Path carpeta, String nombre) throws IOException {
        Files.createDirectories(carpeta);
        return Files.write(carpeta.resolve(nombre), "esto no es una imagen".getBytes(StandardCharsets.UTF_8));
    }
}
