package soptecit.util;

import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.shape.SVGPath;

/**
 * Edición rápida para las tablas administrativas de SOPTEC-IT.
 *
 * <ul>
 *   <li>Agrega una columna con el botón <b>Editar</b> (lápiz) en cada fila.</li>
 *   <li>Doble clic o Enter sobre una fila también la abre para editar.</li>
 *   <li>Clic derecho muestra el menú Editar / Eliminar.</li>
 *   <li>{@link #modo} cambia el formulario entre "registrar nuevo" y "editando": el título se pinta en azul,
 *       "Guardar" se oculta y queda visible "Guardar cambios".</li>
 * </ul>
 */
public final class Edicion {

    private static final String ICONO_LAPIZ = "M4 20 H8 L18.5 9.5 L14.5 5.5 L4 16 Z M13 7 L17 11";

    private Edicion() {
    }

    /** Edición rápida sin opción de eliminar en el menú contextual. */
    public static <T> void habilitar(TableView<T> tabla, Consumer<T> alEditar) {
        habilitar(tabla, alEditar, null);
    }

    /**
     * Agrega botón Editar, doble clic, Enter y menú contextual a la tabla.
     *
     * @param alEditar   carga la fila en el formulario (recibe la fila ya seleccionada)
     * @param alEliminar acción del menú "Eliminar" (puede ser null para no mostrarla)
     */
    public static <T> void habilitar(TableView<T> tabla, Consumer<T> alEditar, Consumer<T> alEliminar) {
        if (tabla == null || alEditar == null) {
            return;
        }
        agregarBotonEditar(tabla, alEditar);

        tabla.setRowFactory(tv -> {
            TableRow<T> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    editar(tabla, fila.getItem(), alEditar);
                }
            });
            ContextMenu menu = new ContextMenu();
            MenuItem miEditar = new MenuItem("Editar");
            miEditar.setOnAction(e -> editar(tabla, fila.getItem(), alEditar));
            menu.getItems().add(miEditar);
            if (alEliminar != null) {
                MenuItem miEliminar = new MenuItem("Eliminar");
                miEliminar.setOnAction(e -> {
                    tabla.getSelectionModel().select(fila.getItem());
                    alEliminar.accept(fila.getItem());
                });
                menu.getItems().add(miEliminar);
            }
            fila.contextMenuProperty().bind(javafx.beans.binding.Bindings
                    .when(fila.emptyProperty()).then((ContextMenu) null).otherwise(menu));
            return fila;
        });

        tabla.setOnKeyPressed(e -> {
            T sel = tabla.getSelectionModel().getSelectedItem();
            if (e.getCode() == KeyCode.ENTER && sel != null) {
                editar(tabla, sel, alEditar);
                e.consume();
            }
        });
    }

    /**
     * Solo agrega la columna con el botón Editar (para tablas que ya usan el doble clic para otra cosa).
     */
    public static <T> void agregarBotonEditar(TableView<T> tabla, Consumer<T> alEditar) {
        if (tabla != null && alEditar != null) {
            tabla.getColumns().add(crearColumnaEditar(tabla, alEditar));
        }
    }

    private static <T> void editar(TableView<T> tabla, T item, Consumer<T> alEditar) {
        if (item == null) {
            return;
        }
        tabla.getSelectionModel().select(item);
        alEditar.accept(item);
    }

    private static <T> TableColumn<T, Void> crearColumnaEditar(TableView<T> tabla, Consumer<T> alEditar) {
        TableColumn<T, Void> col = new TableColumn<>("");
        col.setSortable(false);
        col.setReorderable(false);
        col.setResizable(false);
        col.setMinWidth(48);
        col.setPrefWidth(48);
        col.setMaxWidth(48);
        col.getStyleClass().add("col-editar");
        col.setCellFactory(c -> new TableCell<T, Void>() {
            private final Button boton = new Button();

            {
                SVGPath icono = new SVGPath();
                icono.setContent(ICONO_LAPIZ);
                icono.getStyleClass().add("icono-editar");
                boton.setGraphic(icono);
                boton.getStyleClass().add("boton-editar-fila");
                boton.setTooltip(new Tooltip("Editar"));
                boton.setFocusTraversable(false);
                boton.setOnAction(e -> {
                    T item = getTableRow() != null ? getTableRow().getItem() : null;
                    editar(tabla, item, alEditar);
                });
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                boolean vacia = empty || getTableRow() == null || getTableRow().getItem() == null;
                setGraphic(vacia ? null : boton);
            }
        });
        return col;
    }

    /**
     * Cambia el formulario entre "nuevo" y "editando".
     *
     * @param editando       true cuando hay un registro cargado en el formulario
     * @param titulo         etiqueta del título del formulario (se pinta en azul al editar); puede ser null
     * @param btnGuardar     botón que crea registros nuevos (se oculta al editar); puede ser null
     * @param btnActualizar  botón que guarda cambios (solo visible al editar); puede ser null
     * @param soloEdicion    otros controles que solo tienen sentido con un registro cargado (se deshabilitan)
     */
    public static void modo(boolean editando, Label titulo, Button btnGuardar, Button btnActualizar, Node... soloEdicion) {
        if (titulo != null) {
            titulo.getStyleClass().remove("titulo-editando");
            if (editando) {
                titulo.getStyleClass().add("titulo-editando");
            }
        }
        mostrar(btnGuardar, !editando);
        if (btnActualizar != null) {
            mostrar(btnActualizar, editando);
            btnActualizar.setText("Guardar cambios");
            btnActualizar.setDefaultButton(false);
        }
        if (soloEdicion != null) {
            for (Node n : soloEdicion) {
                if (n != null) {
                    n.setDisable(!editando);
                }
            }
        }
    }

    /** Muestra u oculta un control sin dejar el hueco en el diseño. */
    public static void mostrar(Node nodo, boolean visible) {
        if (nodo != null) {
            nodo.setVisible(visible);
            nodo.setManaged(visible);
        }
    }
}
