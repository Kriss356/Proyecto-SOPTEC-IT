package soptecit.util;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Ventanas de mensaje con el estilo del sistema. */
public final class Mensajes {

    private static final String CSS = "/soptecit/css/estilos.css";

    private Mensajes() {
    }

    public static void info(String titulo, String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, titulo, mensaje);
    }

    public static void error(String titulo, String mensaje) {
        mostrar(Alert.AlertType.ERROR, titulo, mensaje);
    }

    public static boolean confirmar(String titulo, String mensaje) {
        Alert alerta = crear(Alert.AlertType.CONFIRMATION, titulo, mensaje);
        Optional<ButtonType> r = alerta.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        crear(tipo, titulo, mensaje).showAndWait();
    }

    private static Alert crear(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle("SOPTEC-IT");
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        alerta.getDialogPane().getStylesheets().add(Mensajes.class.getResource(CSS).toExternalForm());
        // Se muestra encima de la ventana activa (importante en pantalla completa)
        Window activa = ventanaActiva();
        if (activa != null) {
            alerta.initOwner(activa);
        }
        return alerta;
    }

    /** Devuelve la ventana que tiene el foco (o la primera abierta). */
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
