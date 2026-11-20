package com.mycompany.soptec.it.pruebas.soporte;

import com.mycompany.soptec.it.util.AppLogger;
import com.mycompany.soptec.it.util.GestorImagenes;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuración común de todas las pruebas: log propio de pruebas y conexión hacia soptec_it_test.
 */
public final class EntornoPruebas {

    /** Archivo de log que se genera durante las pruebas (entregable de la práctica). */
    public static final Path RUTA_LOG_PRUEBAS = Paths.get("logs", "soptec_pruebas.log");
    /** Carpeta de imágenes usada durante las pruebas (para no llenar Documentos con archivos de prueba). */
    public static final Path DIR_IMAGENES_PRUEBA = Paths.get("target", "imagenes-prueba");
    private static boolean inicializado = false;

    private EntornoPruebas() {
    }

    public static synchronized void inicializar() {
        // Siempre apuntar a la BD de pruebas, aunque la clase no use MySQL (protección de producción).
        BaseDatosPrueba.configurarConexion();
        GestorImagenes.configurarDirectorioBase(DIR_IMAGENES_PRUEBA);
        if (inicializado) {
            return;
        }
        AppLogger.configurarRutaLog(RUTA_LOG_PRUEBAS);
        try {
            AppLogger.limpiarLog();
        } catch (IOException ignored) {
        }
        AppLogger.registrarInfo("Pruebas", "JUnit5", "INICIO_EJECUCION",
                "Inicio de la ejecucion de pruebas unitarias SOPTEC-IT sobre la BD " + BaseDatosPrueba.NOMBRE_BD);
        ReporteResultados.instancia(); // registra la generación del reporte al terminar
        inicializado = true;
    }
}
