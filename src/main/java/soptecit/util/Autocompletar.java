package soptecit.util;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

/**
 * Combos con búsqueda al teclear: con el combo seleccionado basta escribir unas letras
 * ("cat6", "ro", "jal") y la lista se reduce a lo que coincide; con ↑/↓ y Enter (o el mouse) se elige.
 * Retroceso borra letras y Esc cancela. No distingue mayúsculas ni acentos y busca en cualquier parte del texto.
 */
public final class Autocompletar {

    public static final String CLAVE = "soptec.autocompletar";

    private Autocompletar() {
    }

    public static <T> void aplicar(ComboBox<T> combo) {
        if (combo == null || combo.isEditable() || combo.getProperties().containsKey(CLAVE)) {
            return;
        }
        combo.getProperties().put(CLAVE, Boolean.TRUE);
        mostrarTextoGuiaAlLimpiar(combo);
        new Estado<>(combo);
    }

    /**
     * JavaFX deja el combo totalmente en blanco (sin su texto guía, p. ej. «Seleccionar proveedor»)
     * cuando se limpia con {@code setValue(null)} después de haber elegido algo. Esta celda vuelve
     * a mostrar el texto guía. No se toca a los combos que ya tienen su propia celda.
     */
    private static <T> void mostrarTextoGuiaAlLimpiar(ComboBox<T> combo) {
        if (combo.getButtonCell() != null) {
            return;
        }
        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || item == null) {
                    setText(combo.getPromptText());
                } else if (item instanceof Node) {
                    setText(null);
                    setGraphic((Node) item);
                } else {
                    StringConverter<T> conv = combo.getConverter();
                    setText(conv != null ? conv.toString(item) : String.valueOf(item));
                }
            }
        });
    }

    private static final class Estado<T> {
        private final ComboBox<T> combo;
        private final StringBuilder filtro = new StringBuilder();
        private final Tooltip aviso = new Tooltip();
        private ObservableList<T> fuente;
        private boolean cambiando;

        Estado(ComboBox<T> combo) {
            this.combo = combo;
            this.fuente = combo.getItems();
            aviso.getStyleClass().add("aviso-busqueda");
            combo.itemsProperty().addListener((o, a, n) -> {
                if (!cambiando) {
                    fuente = n;
                    filtro.setLength(0);
                }
            });
            combo.addEventFilter(KeyEvent.KEY_TYPED, e -> {
                String c = e.getCharacter();
                if (c == null || c.isEmpty() || Character.isISOControl(c.charAt(0)) || e.isControlDown()) {
                    return;
                }
                filtro.append(c);
                aplicar();
                e.consume();
            });
            combo.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (filtro.length() == 0) {
                    return;
                }
                if (e.getCode() == KeyCode.BACK_SPACE) {
                    filtro.setLength(filtro.length() - 1);
                    aplicar();
                    e.consume();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    restaurar();
                }
            });
            combo.showingProperty().addListener((o, a, mostrando) -> {
                if (!mostrando) {
                    restaurar();
                }
            });
            combo.focusedProperty().addListener((o, a, f) -> {
                if (!f) {
                    aviso.hide();
                }
            });
            String tip = "Escribe para buscar en la lista";
            if (combo.getTooltip() == null) {
                combo.setTooltip(new Tooltip(tip));
            }
        }

        private void aplicar() {
            if (fuente == null) {
                return;
            }
            if (filtro.length() == 0) {
                restaurar();
                combo.show();
                return;
            }
            String[] palabras = Catalogos.sinAcentos(filtro.toString()).split("\\s+");
            T valor = combo.getValue();
            cambiando = true;
            try {
                FilteredList<T> filtrada = new FilteredList<>(fuente, item -> {
                    String t = Catalogos.sinAcentos(texto(item));
                    for (String p : palabras) {
                        if (!p.isEmpty() && !t.contains(p)) {
                            return false;
                        }
                    }
                    return true;
                });
                combo.setItems(filtrada);
                combo.setValue(valor);
                mostrarAviso(filtrada.size());
            } finally {
                cambiando = false;
            }
            if (!combo.isShowing()) {
                combo.show();
            }
        }

        private void restaurar() {
            aviso.hide();
            if (filtro.length() == 0 && combo.getItems() == fuente) {
                return;
            }
            filtro.setLength(0);
            if (fuente != null && combo.getItems() != fuente) {
                T valor = combo.getValue();
                cambiando = true;
                try {
                    combo.setItems(fuente);
                    combo.setValue(valor);
                } finally {
                    cambiando = false;
                }
            }
        }

        private void mostrarAviso(int encontrados) {
            aviso.setText("Buscando: \"" + filtro + "\"  ·  " + (encontrados == 0 ? "sin coincidencias"
                    : encontrados + (encontrados == 1 ? " resultado" : " resultados")) + "   (Retroceso borra · Esc cancela)");
            if (combo.getScene() == null || combo.getScene().getWindow() == null) {
                return;
            }
            Bounds b = combo.localToScreen(combo.getBoundsInLocal());
            if (b != null) {
                aviso.show(combo, b.getMinX(), b.getMinY() - 34);
            }
        }

        private String texto(T item) {
            if (item == null) {
                return "";
            }
            StringConverter<T> conv = combo.getConverter();
            String t = conv != null ? conv.toString(item) : null;
            return t != null ? t : String.valueOf(item);
        }
    }
}
