package com.mycompany.soptec.it.util;

import com.mycompany.soptec.it.excepciones.ArchivoException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/**
 * Guarda una copia de documentos adjuntos (PDF, Word, Excel, imágenes) en
 * {@code Documentos/SOPTEC-IT/documentos/<subcarpeta>/} y devuelve la ruta relativa que se guarda en la BD.
 * Así el documento sigue disponible aunque el técnico borre o mueva su archivo original.
 */
public final class GestorDocumentos {

    public static final Set<String> EXTENSIONES_VALIDAS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "txt", "png", "jpg", "jpeg");
    public static final long TAMANO_MAXIMO_BYTES = 25L * 1024 * 1024;
    private static final DateTimeFormatter FORMATO_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String OPERACION = "GUARDAR_DOCUMENTO";

    private GestorDocumentos() {
    }

    /** Carpeta {@code .../SOPTEC-IT/documentos} (junto a la carpeta de imágenes). */
    public static Path carpetaDocumentos() {
        Path imagenes = GestorImagenes.getDirectorioBase();
        Path padre = imagenes.getParent() != null ? imagenes.getParent() : imagenes;
        return padre.resolve("documentos");
    }

    /**
     * Valida y copia el documento.
     *
     * @return ruta relativa (por ejemplo {@code levantamientos/20261124_101500_ficha.pdf}) o null si no hay archivo
     */
    public static String guardar(File origen, String subcarpeta, String modulo) throws SoptecException {
        if (origen == null) {
            return null;
        }
        try {
            Path ruta = origen.toPath();
            if (!Files.isRegularFile(ruta) || !Files.isReadable(ruta)) {
                throw new ValidacionException(modulo, OPERACION, "No se encontró el archivo seleccionado o no se puede leer.");
            }
            String nombre = origen.getName();
            int punto = nombre.lastIndexOf('.');
            String ext = punto >= 0 ? nombre.substring(punto + 1).toLowerCase(Locale.ROOT) : "";
            if (!EXTENSIONES_VALIDAS.contains(ext)) {
                throw new ValidacionException(modulo, OPERACION,
                        "Ese tipo de archivo no se puede adjuntar. Usa PDF, Word, Excel, texto o una imagen (JPG/PNG).");
            }
            long tam = Files.size(ruta);
            if (tam == 0) {
                throw new ValidacionException(modulo, OPERACION, "El archivo está vacío.");
            }
            if (tam > TAMANO_MAXIMO_BYTES) {
                throw new ValidacionException(modulo, OPERACION, "El archivo pesa más de 25 MB. Reduce su tamaño e intenta de nuevo.");
            }
            Path carpeta = carpetaDocumentos().resolve(subcarpeta);
            Files.createDirectories(carpeta);
            String seguro = LocalDateTime.now().format(FORMATO_TS) + "_" + nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
            Files.copy(ruta, carpeta.resolve(seguro), StandardCopyOption.REPLACE_EXISTING);
            String relativa = subcarpeta + "/" + seguro;
            AppLogger.registrarInfo(modulo, "GestorDocumentos", OPERACION, "Documento guardado en " + relativa);
            return relativa;
        } catch (IOException ex) {
            ArchivoException ae = new ArchivoException(modulo, OPERACION,
                    "No se pudo copiar el documento a la carpeta de SOPTEC-IT. Verifica que tengas espacio y permisos.", ex);
            AppLogger.registrarExcepcion(modulo, "GestorDocumentos", OPERACION, ae);
            throw ae;
        } catch (ValidacionException ex) {
            AppLogger.registrarExcepcion(modulo, "GestorDocumentos", OPERACION, ex);
            throw ex;
        }
    }

    /** Ruta real en disco de lo guardado en la BD (acepta también rutas absolutas). */
    public static Path resolver(String guardado) {
        if (guardado == null || guardado.isBlank()) {
            return null;
        }
        try {
            Path p = Paths.get(guardado.trim());
            return p.isAbsolute() ? p : carpetaDocumentos().resolve(p).normalize();
        } catch (InvalidPathException ex) {
            return null;
        }
    }

    /** Nombre legible del documento (sin la marca de fecha que se agrega al guardarlo). */
    public static String nombreVisible(String guardado) {
        if (guardado == null || guardado.isBlank()) {
            return null;
        }
        String nombre = Paths.get(guardado).getFileName().toString();
        return nombre.matches("\\d{8}_\\d{6}_.*") ? nombre.substring(16) : nombre;
    }
}
