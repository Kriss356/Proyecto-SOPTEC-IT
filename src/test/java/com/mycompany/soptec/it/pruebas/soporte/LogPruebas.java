package com.mycompany.soptec.it.pruebas.soporte;

import com.mycompany.soptec.it.util.AppLogger;
import java.util.List;

/**
 * Lectura del log para verificar lo que se registró durante una prueba concreta.
 */
public final class LogPruebas {

    private LogPruebas() {
    }

    /** Número de líneas actuales del log (marca de inicio). */
    public static int marca() {
        return AppLogger.leerLineas().size();
    }

    /** Líneas escritas desde la marca indicada. */
    public static List<String> lineasDesde(int marca) {
        List<String> todas = AppLogger.leerLineas();
        return todas.subList(Math.min(marca, todas.size()), todas.size());
    }

    public static String textoDesde(int marca) {
        return String.join("\n", lineasDesde(marca));
    }
}
