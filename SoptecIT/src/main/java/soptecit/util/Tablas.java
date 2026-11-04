package soptecit.util;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;

/**
 * Utilidades para llenar las tablas con datos de ejemplo (arreglos de texto).
 *
 * Cuando conecten la base de datos pueden seguir usando este método:
 * solo conviertan cada registro del ResultSet en un String[] en el mismo
 * orden que las columnas de la tabla.
 *
 * Columnas con styleClass "col-estado" se pintan como insignia de color.
 * Columnas con styleClass "col-avance" se pintan como barra de progreso (valor 0-100).
 */
public final class Tablas {

    private Tablas() {
    }

    @SuppressWarnings("unchecked")
    public static void cargar(TableView<String[]> tabla, String[][] filas) {
        for (int i = 0; i < tabla.getColumns().size(); i++) {
            final int indice = i;
            TableColumn<String[], String> columna = (TableColumn<String[], String>) tabla.getColumns().get(i);
            columna.setCellValueFactory(c -> new SimpleStringProperty(
                    indice < c.getValue().length ? c.getValue()[indice] : ""));
            if (columna.getStyleClass().contains("col-estado")) {
                columna.setCellFactory(c -> celdaEstado());
            } else if (columna.getStyleClass().contains("col-avance")) {
                columna.setCellFactory(c -> celdaAvance());
            }
        }
        tabla.setItems(FXCollections.observableArrayList(filas));
        tabla.setPlaceholder(new Label("No hay registros para mostrar"));
    }

    /** Devuelve la clase CSS de la insignia según el texto del estado. */
    public static String claseEstado(String estado) {
        String e = estado.toLowerCase();
        if (contiene(e, "vencid", "retras", "cancel", "rechaz", "agotad", "bajo stock", "detenid")) {
            return "badge-peligro";
        }
        if (contiene(e, "pendiente", "programad", "revisión", "revision", "por surtir", "parcial", "borrador", "reprogram")) {
            return "badge-alerta";
        }
        if (contiene(e, "pagad", "complet", "finaliz", "termin", "aprobad", "en tiempo", "confirm",
                "disponible", "atendid", "recibid", "entrada", "cerrad", "autorizad")) {
            return "badge-exito";
        }
        if (contiene(e, "salida")) {
            return "badge-naranja";
        }
        if (contiene(e, "proceso", "curso", "enviad", "en sitio", "instalaci", "solicitad", "camino")) {
            return "badge-info";
        }
        return "badge-neutro";
    }

    private static boolean contiene(String texto, String... palabras) {
        for (String p : palabras) {
            if (texto.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private static TableCell<String[], String> celdaEstado() {
        return new TableCell<String[], String>() {
            private final Label insignia = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null || item.isEmpty()) {
                    setGraphic(null);
                    return;
                }
                insignia.setText(item);
                insignia.getStyleClass().setAll("label", "badge", claseEstado(item));
                setGraphic(insignia);
            }
        };
    }

    private static TableCell<String[], String> celdaAvance() {
        return new TableCell<String[], String>() {
            private final ProgressBar barra = new ProgressBar();
            private final Label texto = new Label();
            private final HBox caja = new HBox(8, barra, texto);

            {
                caja.setAlignment(Pos.CENTER_LEFT);
                barra.setMaxWidth(Double.MAX_VALUE);
                barra.setPrefHeight(10);
                HBox.setHgrow(barra, Priority.ALWAYS);
                texto.setMinWidth(38);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null || item.isEmpty()) {
                    setGraphic(null);
                    return;
                }
                double valor;
                try {
                    valor = Double.parseDouble(item.replace("%", "").trim());
                } catch (NumberFormatException ex) {
                    valor = 0;
                }
                barra.setProgress(valor / 100.0);
                barra.getStyleClass().removeAll("progreso-exito", "progreso-alerta");
                if (valor >= 100) {
                    barra.getStyleClass().add("progreso-exito");
                } else if (valor < 35) {
                    barra.getStyleClass().add("progreso-alerta");
                }
                texto.setText((int) valor + " %");
                setGraphic(caja);
            }
        };
    }
}
