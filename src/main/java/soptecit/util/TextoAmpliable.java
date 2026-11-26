package soptecit.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Observaciones en grande: junto a cada área de texto aparece «Ampliar ⤢» (o se presiona F2 dentro de ella)
 * y se abre una ventana amplia para escribir con comodidad; al presionar «Listo» el texto regresa al formulario.
 */
public final class TextoAmpliable {

    public static final String CLAVE = "soptec.ampliable";
    private static final String CSS = "/soptecit/css/estilos.css";

    private TextoAmpliable() {
    }

    public static void aplicar(TextArea area) {
        if (area == null || !area.isEditable() || area.getProperties().containsKey(CLAVE)) {
            return;
        }
        area.getProperties().put(CLAVE, Boolean.TRUE);
        area.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F2) {
                abrir(area);
                e.consume();
            }
        });
        if (area.getTooltip() == null) {
            area.setTooltip(new Tooltip("F2 o «Ampliar» para escribir en una ventana grande"));
        }
        // Si arriba del área hay una etiqueta, se le agrega el enlace «Ampliar»
        Parent padre = area.getParent();
        if (padre instanceof VBox) {
            VBox caja = (VBox) padre;
            int i = caja.getChildren().indexOf(area);
            if (i > 0 && caja.getChildren().get(i - 1) instanceof Label) {
                Label etiqueta = (Label) caja.getChildren().get(i - 1);
                Hyperlink ampliar = new Hyperlink("Ampliar ⤢");
                ampliar.getStyleClass().add("link-ampliar");
                ampliar.setFocusTraversable(false);
                ampliar.setOnAction(e -> abrir(area));
                Region espacio = new Region();
                HBox.setHgrow(espacio, Priority.ALWAYS);
                HBox fila = new HBox(6, espacio, ampliar);
                fila.setAlignment(Pos.CENTER_LEFT);
                caja.getChildren().set(i - 1, fila);
                fila.getChildren().add(0, etiqueta);
            }
        }
    }

    /** Abre la ventana grande para editar el texto del área. */
    public static void abrir(TextArea area) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.WINDOW_MODAL);
        if (area.getScene() != null && area.getScene().getWindow() != null) {
            ventana.initOwner(area.getScene().getWindow());
        }
        String titulo = tituloDe(area);
        ventana.setTitle(titulo);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("tarjeta-titulo");
        Label ayuda = new Label("Escribe con calma; al presionar «Listo» (o Ctrl+Enter) el texto pasa al formulario.");
        ayuda.getStyleClass().add("tarjeta-subtitulo");

        TextArea grande = new TextArea(area.getText());
        grande.setWrapText(true);
        grande.setPromptText(area.getPromptText());
        grande.getStyleClass().add("area-ampliada");
        VBox.setVgrow(grande, Priority.ALWAYS);

        Label contador = new Label();
        contador.getStyleClass().add("texto-ayuda");
        grande.textProperty().addListener((o, a, n) -> contador.setText(n.length() + " caracteres"));
        contador.setText(grande.getText() == null ? "0 caracteres" : grande.getText().length() + " caracteres");

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("boton-secundario");
        cancelar.setCancelButton(true);
        cancelar.setOnAction(e -> ventana.close());
        Button listo = new Button("Listo");
        listo.getStyleClass().add("boton-exito");
        listo.setOnAction(e -> {
            area.setText(grande.getText());
            area.positionCaret(area.getText() == null ? 0 : area.getText().length());
            ventana.close();
        });
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox botones = new HBox(10, contador, espacio, cancelar, listo);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox raiz = new VBox(10, lblTitulo, ayuda, grande, botones);
        raiz.setPadding(new Insets(20));
        raiz.getStyleClass().add("tarjeta");
        Scene escena = new Scene(raiz, 780, 540);
        if (TextoAmpliable.class.getResource(CSS) != null) {
            escena.getStylesheets().add(TextoAmpliable.class.getResource(CSS).toExternalForm());
        }
        escena.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN), listo::fire);
        ventana.setScene(escena);
        grande.requestFocus();
        grande.positionCaret(grande.getText() == null ? 0 : grande.getText().length());
        ventana.showAndWait();
    }

    private static String tituloDe(TextArea area) {
        Parent p = area.getParent();
        if (p instanceof VBox) {
            for (Node n : ((VBox) p).getChildren()) {
                if (n instanceof HBox) {
                    for (Node h : ((HBox) n).getChildren()) {
                        if (h instanceof Label && !((Label) h).getText().isBlank()) {
                            return ((Label) h).getText().replace("*", "").trim();
                        }
                    }
                } else if (n instanceof Label) {
                    return ((Label) n).getText().replace("*", "").trim();
                }
            }
        }
        return "Observaciones";
    }
}
