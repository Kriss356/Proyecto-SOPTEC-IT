package soptecit.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.IdentityHashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Búsqueda en vivo: mientras se escribe en el campo, la tabla muestra solo las filas que contienen
 * el texto en cualquiera de sus columnas o datos (sin importar mayúsculas ni acentos).
 * Varias palabras se buscan juntas: "ro jal" encuentra a "Roberto Juárez · Jalisco".
 *
 * <p>Funciona aunque el controlador vuelva a llenar la tabla con {@code setItems(...)}: la nueva lista
 * se envuelve sola y se le aplica el texto que esté escrito.</p>
 */
public final class BusquedaTabla {

    /** Clave en getProperties() del campo (→ tabla) y de la tabla (→ campo). */
    public static final String CLAVE = "soptec.busqueda";

    private BusquedaTabla() {
    }

    public static <T> void conectar(TextField campo, TableView<T> tabla) {
        if (campo == null || tabla == null
                || campo.getProperties().containsKey(CLAVE) || tabla.getProperties().containsKey(CLAVE)) {
            return;
        }
        Estado<T> estado = new Estado<>(campo, tabla);
        campo.getProperties().put(CLAVE, tabla);
        tabla.getProperties().put(CLAVE, campo);
        if (!campo.getStyleClass().contains("campo-busqueda")) {
            campo.getStyleClass().add("campo-busqueda");
        }
        estado.envolver(tabla.getItems());
        tabla.itemsProperty().addListener((o, a, nuevos) -> {
            if (nuevos != estado.ordenada) {
                estado.envolver(nuevos);
            }
        });
        campo.textProperty().addListener((o, a, n) -> estado.filtrar());
        campo.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && !campo.getText().isEmpty()) {
                campo.clear();
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN && !tabla.getItems().isEmpty()) {
                tabla.requestFocus();
                tabla.getSelectionModel().selectFirst();
                e.consume();
            }
        });
    }

    /** Tabla que filtra el campo (o null). */
    @SuppressWarnings("unchecked")
    public static <T> TableView<T> tablaDe(TextField campo) {
        Object t = campo.getProperties().get(CLAVE);
        return t instanceof TableView ? (TableView<T>) t : null;
    }

    private static final class Estado<T> {
        private final TextField campo;
        private final TableView<T> tabla;
        private final Node placeholderOriginal;
        private final Map<T, String> indice = new IdentityHashMap<>();
        private FilteredList<T> filtrada;
        private SortedList<T> ordenada;

        Estado(TextField campo, TableView<T> tabla) {
            this.campo = campo;
            this.tabla = tabla;
            this.placeholderOriginal = tabla.getPlaceholder();
        }

        void envolver(ObservableList<T> nuevos) {
            ObservableList<T> fuente = nuevos != null ? nuevos : FXCollections.observableArrayList();
            indice.clear();
            if (ordenada != null) {
                ordenada.comparatorProperty().unbind();
            }
            filtrada = new FilteredList<>(fuente);
            ordenada = new SortedList<>(filtrada);
            ordenada.comparatorProperty().bind(tabla.comparatorProperty());
            tabla.setItems(ordenada);
            filtrar();
        }

        void filtrar() {
            String[] palabras = Catalogos.sinAcentos(campo.getText() == null ? "" : campo.getText()).split("\\s+");
            boolean vacio = palabras.length == 0 || (palabras.length == 1 && palabras[0].isEmpty());
            if (vacio) {
                filtrada.setPredicate(null);
            } else {
                filtrada.setPredicate(item -> {
                    String texto = indice.computeIfAbsent(item, this::textoDe);
                    for (String p : palabras) {
                        if (!p.isEmpty() && !texto.contains(p)) {
                            return false;
                        }
                    }
                    return true;
                });
            }
            if (!vacio && filtrada.isEmpty() && !filtrada.getSource().isEmpty()) {
                Label sin = new Label("Nada coincide con \"" + campo.getText().trim() + "\" (Esc para limpiar)");
                sin.getStyleClass().add("texto-tenue");
                tabla.setPlaceholder(sin);
            } else {
                tabla.setPlaceholder(placeholderOriginal);
            }
        }

        /** Todo lo que se ve en la fila + los datos del objeto (aunque no estén en una columna). */
        private String textoDe(T item) {
            StringBuilder sb = new StringBuilder();
            if (item instanceof Object[]) {
                for (Object o : (Object[]) item) {
                    sb.append(o).append(' ');
                }
            } else if (item != null) {
                for (TableColumn<T, ?> col : tabla.getVisibleLeafColumns()) {
                    try {
                        Object v = col.getCellData(item);
                        if (v != null) {
                            sb.append(v).append(' ');
                        }
                    } catch (RuntimeException ignorada) {
                        // columnas de botones o celdas especiales
                    }
                }
                agregarPropiedades(item, sb);
            }
            return Catalogos.sinAcentos(sb.toString());
        }
    }

    private static void agregarPropiedades(Object item, StringBuilder sb) {
        for (Method m : item.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || m.getDeclaringClass() == Object.class
                    || !Modifier.isPublic(m.getModifiers()) || !m.getName().startsWith("get")) {
                continue;
            }
            Class<?> r = m.getReturnType();
            if (r == String.class || Number.class.isAssignableFrom(r) || r == int.class || r == long.class
                    || r == double.class || TemporalAccessor.class.isAssignableFrom(r)) {
                try {
                    Object v = m.invoke(item);
                    if (v != null) {
                        sb.append(v).append(' ');
                    }
                } catch (ReflectiveOperationException | RuntimeException ignorada) {
                    // propiedad no legible: se omite
                }
            }
        }
    }
}
