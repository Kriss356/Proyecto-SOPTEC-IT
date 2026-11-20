package com.mycompany.soptec.it.pruebas.imagenes;

import com.mycompany.soptec.it.excepciones.ArchivoException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.EntornoPruebas;
import com.mycompany.soptec.it.pruebas.soporte.LogPruebas;
import com.mycompany.soptec.it.pruebas.soporte.PruebaUnitaria;
import com.mycompany.soptec.it.util.GestorImagenes;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas de almacenamiento de imágenes en disco. Usan una carpeta temporal que simula
 * {@code <usuario>/Documents/SOPTEC-IT/imagenes} para no llenar tus Documentos reales.
 */
@DisplayName("Imágenes · Almacenamiento y validación de archivos")
class GestorImagenesTest extends PruebaUnitaria {

    private static final String MODULO = "PruebasImagenes";

    @TempDir
    Path temporal;
    private Path carpetaApp;
    private Path origen;

    @BeforeEach
    void configurarCarpetaSimulada() throws Exception {
        carpetaApp = temporal.resolve("Documents").resolve("SOPTEC-IT").resolve("imagenes");
        GestorImagenes.configurarDirectorioBase(carpetaApp);
        origen = temporal.resolve("origen");
        Files.createDirectories(origen);
    }

    @AfterEach
    void restaurar() {
        GestorImagenes.configurarDirectorioBase(EntornoPruebas.DIR_IMAGENES_PRUEBA);
    }

    @Test
    @DisplayName("La carpeta por defecto está en Documentos/SOPTEC-IT/imagenes del usuario")
    void carpetaPorDefectoEnDocumentos() {
        Path porDefecto = GestorImagenes.directorioPorDefecto();
        assertTrue(porDefecto.startsWith(Paths.get(System.getProperty("user.home"))), porDefecto.toString());
        assertTrue(porDefecto.endsWith(Paths.get("Documents", "SOPTEC-IT", "imagenes")), porDefecto.toString());
    }

    @Test
    @DisplayName("Se crea automáticamente la carpeta de la aplicación cuando no existe")
    void creaCarpetaAutomaticamente() throws Exception {
        assertFalse(Files.exists(carpetaApp), "Precondición: la carpeta no existe");
        Path creada = GestorImagenes.asegurarCarpetaAplicacion(MODULO);
        assertTrue(Files.isDirectory(creada));
        assertEquals(carpetaApp, creada);
    }

    @Test
    @DisplayName("Guardar una imagen crea su subcarpeta y se recupera con el mismo contenido")
    void guardarYRecuperar() throws Exception {
        Path png = DatosPrueba.crearPng(origen, "foto.png");
        assertFalse(Files.exists(carpetaApp.resolve("clientes")));

        String ruta = GestorImagenes.guardarImagen(png.toFile(), "clientes", MODULO);

        assertTrue(ruta.startsWith("clientes/"), "En la BD se guarda la ruta relativa: " + ruta);
        assertTrue(Files.isDirectory(carpetaApp.resolve("clientes")), "Subcarpeta creada automáticamente");
        assertTrue(GestorImagenes.existeImagen(ruta));
        assertArrayEquals(DatosPrueba.PNG_1X1, GestorImagenes.leerImagen(ruta, MODULO));
        assertTrue(Files.exists(png), "El archivo original del usuario no se mueve ni se borra");
    }

    @Test
    @DisplayName("Acepta PNG, JPG, GIF y BMP en cada categoría (clientes, empleados, proveedores, croquis)")
    void formatosYCategoriasValidos() throws Exception {
        Path gif = Files.write(origen.resolve("logo.gif"), DatosPrueba.GIF_1X1);
        Path jpg = Files.write(origen.resolve("foto.jpg"), new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10});
        Path bmp = Files.write(origen.resolve("plano.bmp"), new byte[]{'B', 'M', 0, 0, 0, 0});
        assertNotNull(GestorImagenes.guardarImagen(gif.toFile(), "proveedores", MODULO));
        assertNotNull(GestorImagenes.guardarImagen(jpg.toFile(), "empleados", MODULO));
        assertNotNull(GestorImagenes.guardarImagen(bmp.toFile(), "croquis", MODULO));
        assertNotNull(GestorImagenes.guardarImagen(DatosPrueba.crearPng(origen, "c.png").toFile(), "clientes", MODULO));
    }

    @Test
    @DisplayName("El nombre del archivo guardado no contiene caracteres problemáticos")
    void nombreSeguro() throws Exception {
        Path png = DatosPrueba.crearPng(origen, "mi foto (1)#.png");
        String ruta = GestorImagenes.guardarImagen(png.toFile(), "clientes", MODULO);
        String nombre = ruta.substring(ruta.indexOf('/') + 1);
        assertTrue(nombre.matches("[a-zA-Z0-9._-]+"), nombre);
    }

    @Test
    @DisplayName("Imagen inexistente: se rechaza con mensaje claro")
    void imagenInexistente() throws Exception {
        File fantasma = origen.resolve("no-existe.png").toFile();
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> GestorImagenes.guardarImagen(fantasma, "clientes", MODULO));
        assertTrue(ex.getMessage().contains("no existe"), ex.getMessage());

        assertFalse(GestorImagenes.existeImagen("clientes/fantasma.png"));
        assertThrows(ValidacionException.class, () -> GestorImagenes.leerImagen("clientes/fantasma.png", MODULO));
        assertNull(GestorImagenes.guardarImagen(null, "clientes", MODULO), "Sin archivo elegido no se guarda nada");
    }

    @Test
    @DisplayName("Formato incorrecto: extensión no permitida")
    void extensionNoPermitida() throws Exception {
        Path txt = DatosPrueba.crearArchivoTexto(origen, "documento.txt");
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> GestorImagenes.guardarImagen(txt.toFile(), "clientes", MODULO));
        assertTrue(ex.getMessage().contains("Formato"), ex.getMessage());
    }

    @Test
    @DisplayName("Formato incorrecto: archivo con extensión .png que en realidad no es imagen")
    void contenidoFalso() throws Exception {
        Path falso = DatosPrueba.crearArchivoTexto(origen, "virus.png");
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> GestorImagenes.guardarImagen(falso.toFile(), "clientes", MODULO));
        assertTrue(ex.getMessage().contains("no es una imagen válida"), ex.getMessage());

        Path vacio = Files.write(origen.resolve("vacio.jpg"), new byte[0]);
        assertThrows(ValidacionException.class, () -> GestorImagenes.guardarImagen(vacio.toFile(), "clientes", MODULO));
        assertFalse(Files.exists(carpetaApp.resolve("clientes")), "No se copia ningún archivo inválido");
    }

    @Test
    @DisplayName("Categoría inválida (intento de salir de la carpeta) es rechazada")
    void subcarpetaInvalida() throws Exception {
        Path png = DatosPrueba.crearPng(origen, "foto.png");
        assertThrows(ValidacionException.class,
                () -> GestorImagenes.guardarImagen(png.toFile(), "../../Windows", MODULO));
    }

    @Test
    @DisplayName("Error de acceso: la carpeta de imágenes no se puede crear")
    void errorDeAcceso() throws Exception {
        Path bloqueo = Files.write(temporal.resolve("bloqueo"), new byte[]{1});
        GestorImagenes.configurarDirectorioBase(bloqueo.resolve("imagenes")); // un archivo ocupa el lugar de la carpeta
        Path png = DatosPrueba.crearPng(origen, "foto.png");
        int marca = LogPruebas.marca();

        ArchivoException ex = assertThrows(ArchivoException.class,
                () -> GestorImagenes.guardarImagen(png.toFile(), "clientes", MODULO));

        assertEquals("ARCHIVO", ex.getCategoria());
        assertTrue(ex.getMessage().contains("carpeta de imágenes"), ex.getMessage());
        assertTrue(LogPruebas.textoDesde(marca).contains("TipoExcepcion=ARCHIVO"), "El error se registra en el log");
    }

    @Test
    @DisplayName("Integridad ante fallo de almacenamiento: no quedan archivos a medias y lo previo sigue intacto")
    void integridadAnteFallo() throws Exception {
        String rutaPrevia = GestorImagenes.guardarImagen(DatosPrueba.crearPng(origen, "a.png").toFile(), "clientes", MODULO);
        // Un archivo llamado "empleados" impide crear esa subcarpeta: el guardado debe fallar limpiamente.
        Files.write(carpetaApp.resolve("empleados"), new byte[]{1});

        assertThrows(ArchivoException.class, () -> GestorImagenes.guardarImagen(
                DatosPrueba.crearPng(origen, "b.png").toFile(), "empleados", MODULO));

        assertArrayEquals(DatosPrueba.PNG_1X1, GestorImagenes.leerImagen(rutaPrevia, MODULO),
                "La imagen guardada antes del fallo sigue disponible");
        try (Stream<Path> archivos = Files.walk(carpetaApp)) {
            assertTrue(archivos.noneMatch(p -> p.toString().endsWith(".tmp")), "No quedan archivos temporales");
        }
    }

    @Test
    @DisplayName("Eliminar imagen solo borra archivos dentro de la carpeta de la aplicación")
    void eliminarImagen() throws Exception {
        String ruta = GestorImagenes.guardarImagen(DatosPrueba.crearPng(origen, "x.png").toFile(), "croquis", MODULO);
        assertTrue(GestorImagenes.eliminarImagen(ruta));
        assertFalse(GestorImagenes.existeImagen(ruta));

        Path externo = DatosPrueba.crearPng(origen, "externo.png");
        assertFalse(GestorImagenes.eliminarImagen(externo.toString()), "No toca archivos fuera de SOPTEC-IT");
        assertTrue(Files.exists(externo));
    }
}
