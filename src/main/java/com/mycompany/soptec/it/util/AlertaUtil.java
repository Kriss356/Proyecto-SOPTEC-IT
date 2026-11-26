package com.mycompany.soptec.it.util;

import com.mycompany.soptec.it.excepciones.SoptecException;
import java.net.URL;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * Utilidad para mostrar mensajes claros y comprensibles al usuario sin exponer trazas SQL,
 * aplicando la hoja de estilos institucional de SOPTEC-IT.
 */
public final class AlertaUtil {

    private static final String CSS = "/soptecit/css/estilos.css";

    private AlertaUtil() {
    }

    public static void info(String titulo, String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, titulo, mensaje);
    }

    public static void advertencia(String titulo, String mensaje) {
        mostrar(Alert.AlertType.WARNING, titulo, mensaje);
    }

    public static void error(String titulo, String mensaje) {
        if (titulo != null && titulo.toLowerCase().startsWith("falta")) {
            mensaje = mensaje + notaCamposMarcados();
        }
        mostrar(Alert.AlertType.ERROR, titulo, mensaje);
    }

    /**
     * Gancho que pone la pantalla principal: marca en rojo los campos obligatorios vacíos de la
     * pantalla abierta y devuelve cuántos marcó (para avisarlo en el mensaje).
     */
    private static java.util.function.IntSupplier marcarCamposVacios;

    public static void setMarcarCamposVacios(java.util.function.IntSupplier gancho) {
        marcarCamposVacios = gancho;
    }

    private static String notaCamposMarcados() {
        if (marcarCamposVacios == null) {
            return "";
        }
        try {
            int n = marcarCamposVacios.getAsInt();
            return n == 0 ? "" : "\n\nTe marcamos en rojo " + (n == 1 ? "el campo que falta." : "los " + n + " campos que faltan.");
        } catch (RuntimeException ex) {
            return "";
        }
    }

    /** Mensaje genérico para errores no previstos (nunca muestra trazas ni SQL). */
    public static final String MENSAJE_ERROR_INESPERADO =
            "Ocurrió un problema inesperado al realizar la operación, pero el sistema continúa funcionando.\n"
            + "Si el problema se repite, avisa al Administrador general del sistema.";

    public static void errorControlado(String modulo, String operacion, Throwable ex) {
        if (ex instanceof SoptecException) {
            String nota = "VALIDACION".equals(((SoptecException) ex).getCategoria()) ? notaCamposMarcados() : "";
            mostrar(Alert.AlertType.WARNING, tituloPara(ex), mensajeUsuario(ex) + nota);
        } else {
            AppLogger.registrarExcepcion(modulo, "ControladorUI", operacion, ex);
            mostrar(Alert.AlertType.ERROR, tituloPara(ex), mensajeUsuario(ex));
        }
    }

    /**
     * Título descriptivo que verá el usuario según el tipo de error.
     * Es un método puro (sin ventanas) para poder verificarlo con pruebas unitarias.
     */
    public static String tituloPara(Throwable ex) {
        if (!(ex instanceof SoptecException)) {
            return "Error inesperado";
        }
        String categoria = ((SoptecException) ex).getCategoria();
        switch (categoria == null ? "" : categoria) {
            case "VALIDACION":
                return "Datos inválidos o incompletos";
            case "INTEGRIDAD_REFERENCIAL":
                return "Operación bloqueada por registros relacionados";
            case "CONEXION_BD":
                return "Sin conexión con la base de datos";
            case "ARCHIVO":
                return "Problema con el archivo de imagen";
            default:
                return "Error al procesar la operación";
        }
    }

    /**
     * Texto que verá el usuario: el mensaje amigable de la excepción del sistema o uno genérico,
     * nunca el detalle técnico (SQL, rutas JDBC, trazas).
     */
    public static String mensajeUsuario(Throwable ex) {
        if (ex instanceof SoptecException && ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage() + "\n\nEl detalle técnico quedó registrado en el archivo de log.";
        }
        return MENSAJE_ERROR_INESPERADO;
    }

    public static boolean confirmar(String titulo, String mensaje) {
        Alert alerta = crear(Alert.AlertType.CONFIRMATION, titulo, mensaje);
        Optional<ButtonType> res = alerta.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }

    /**
     * Pregunta con varias opciones (botones con texto propio).
     *
     * @return índice de la opción elegida, o -1 si se cerró / canceló
     */
    public static int elegir(String titulo, String mensaje, String... opciones) {
        Alert alerta = crear(Alert.AlertType.CONFIRMATION, titulo, mensaje);
        java.util.List<ButtonType> botones = new java.util.ArrayList<>();
        for (int i = 0; i < opciones.length; i++) {
            boolean ultima = i == opciones.length - 1;
            botones.add(new ButtonType(opciones[i], ultima && opciones[i].toLowerCase().startsWith("cancelar")
                    ? javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE
                    : (i == 0 ? javafx.scene.control.ButtonBar.ButtonData.OK_DONE
                            : javafx.scene.control.ButtonBar.ButtonData.OTHER)));
        }
        alerta.getButtonTypes().setAll(botones);
        Optional<ButtonType> res = alerta.showAndWait();
        return res.isPresent() ? botones.indexOf(res.get()) : -1;
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        if ((tipo == Alert.AlertType.ERROR || tipo == Alert.AlertType.WARNING) && alMostrarAviso != null) {
            try {
                alMostrarAviso.run();
            } catch (RuntimeException ignorada) {
                // el aviso se muestra de todos modos
            }
        }
        crear(tipo, titulo, mensaje).showAndWait();
    }

    /** Gancho que se ejecuta al mostrar un error o advertencia (lo usa el aviso de cambios sin guardar). */
    private static Runnable alMostrarAviso;

    public static void setAlMostrarAviso(Runnable gancho) {
        alMostrarAviso = gancho;
    }

    private static Alert crear(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle("SOPTEC-IT");
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        // Los mensajes largos (varias líneas) se mostraban cortados con «...»:
        // el cuadro ahora crece a lo alto lo necesario para que se lea completo.
        alerta.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        URL cssUrl = AlertaUtil.class.getResource(CSS);
        if (cssUrl != null) {
            alerta.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }
        Window activa = ventanaActiva();
        if (activa != null) {
            alerta.initOwner(activa);
        }
        return alerta;
    }

    private static Window ventanaActiva() {
        Window primera = null;
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w.isFocused()) {
                return w;
            }
            if (primera == null && w.isShowing()) {
                primera = w;
            }
        }
        return primera;
    }
}
