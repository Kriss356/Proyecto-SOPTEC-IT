package soptecit.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Conecta en un formulario los controles para armar una lista "muchos a muchos" con cantidad
 * (servicios de una cita o venta, partidas de una orden de compra, material de un proyecto):
 * un ComboBox para elegir del catálogo (sin volver a capturar datos), un Spinner de cantidad,
 * una tabla con los renglones y una etiqueta con el total.
 *
 * @param <C> elemento del catálogo (Servicio, Material)
 * @param <D> renglón de detalle (DetalleServicio, PartidaMaterial)
 */
public class EditorDetalle<C, D> {

    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    /** Cómo crear y leer un renglón. */
    public interface Adaptador<C, D> {
        D crear(C elemento, int cantidad);
        int idDeCatalogo(C elemento);
        int idDeDetalle(D detalle);
        String nombre(D detalle);
        int cantidad(D detalle);
        void setCantidad(D detalle, int cantidad);
        BigDecimal precio(D detalle);
        BigDecimal importe(D detalle);
    }

    private final ComboBox<C> combo;
    private final Spinner<Integer> cantidad;
    private final TableView<D> tabla;
    private final Label lblTotal;
    private final String prefijoTotal;
    private final Adaptador<C, D> adaptador;
    private final ObservableList<D> renglones = FXCollections.observableArrayList();
    private Runnable alCambiar = () -> { };
    private final Label avisoCatalogoVacio = new Label("No hay elementos en el catálogo.");

    /** De dónde se vuelve a leer el catálogo cada vez que se abre la lista desplegable. */
    @FunctionalInterface
    public interface FuenteCatalogo<C> {
        List<C> leer() throws Exception;
    }

    @SuppressWarnings("unchecked")
    public EditorDetalle(ComboBox<C> combo, Spinner<Integer> cantidad, TableView<D> tabla, Label lblTotal,
            String prefijoTotal, String textoVacio, Adaptador<C, D> adaptador) {
        this.combo = combo;
        this.cantidad = cantidad;
        this.tabla = tabla;
        this.lblTotal = lblTotal;
        this.prefijoTotal = prefijoTotal == null ? "" : prefijoTotal;
        this.adaptador = adaptador;

        cantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));
        cantidad.setEditable(true);
        tabla.setItems(renglones);
        tabla.setPlaceholder(new Label(textoVacio));
        // Si el catálogo está vacío, la lista desplegable muestra un aviso en lugar de quedar en blanco
        avisoCatalogoVacio.setWrapText(true);
        avisoCatalogoVacio.setMaxWidth(320);
        avisoCatalogoVacio.setStyle("-fx-padding: 10 12; -fx-text-fill: #b45309;");
        combo.setPlaceholder(avisoCatalogoVacio);
        // JavaFX deja el combo en blanco (sin «Servicio del catálogo») después de limpiarlo;
        // esta celda vuelve a mostrar el texto guía cuando no hay nada elegido.
        combo.setButtonCell(new ListCell<C>() {
            @Override
            protected void updateItem(C item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(combo.getPromptText());
                } else {
                    setText(combo.getConverter() != null ? combo.getConverter().toString(item) : String.valueOf(item));
                }
            }
        });

        List<TableColumn<D, ?>> cols = tabla.getColumns();
        if (cols.size() >= 1) {
            ((TableColumn<D, String>) cols.get(0)).setCellValueFactory(d -> new SimpleStringProperty(adaptador.nombre(d.getValue())));
        }
        if (cols.size() >= 2) {
            ((TableColumn<D, String>) cols.get(1)).setCellValueFactory(d -> new SimpleStringProperty(
                    String.valueOf(adaptador.cantidad(d.getValue()))));
        }
        if (cols.size() >= 3) {
            ((TableColumn<D, String>) cols.get(2)).setCellValueFactory(d -> new SimpleStringProperty(
                    MONEDA.format(adaptador.precio(d.getValue()))));
        }
        if (cols.size() >= 4) {
            ((TableColumn<D, String>) cols.get(3)).setCellValueFactory(d -> new SimpleStringProperty(
                    MONEDA.format(adaptador.importe(d.getValue()))));
        }
        actualizarTotal();
    }

    /** Fábrica corta para catálogos simples. */
    public static <C, D> Adaptador<C, D> adaptador(java.util.function.BiFunction<C, Integer, D> crear,
            ToIntFunction<C> idCatalogo, ToIntFunction<D> idDetalle, Function<D, String> nombre,
            ToIntFunction<D> cant, java.util.function.ObjIntConsumer<D> setCant,
            Function<D, BigDecimal> precio, Function<D, BigDecimal> importe) {
        return new Adaptador<C, D>() {
            @Override public D crear(C e, int c) { return crear.apply(e, c); }
            @Override public int idDeCatalogo(C e) { return idCatalogo.applyAsInt(e); }
            @Override public int idDeDetalle(D d) { return idDetalle.applyAsInt(d); }
            @Override public String nombre(D d) { return nombre.apply(d); }
            @Override public int cantidad(D d) { return cant.applyAsInt(d); }
            @Override public void setCantidad(D d, int c) { setCant.accept(d, c); }
            @Override public BigDecimal precio(D d) { return precio.apply(d); }
            @Override public BigDecimal importe(D d) { return importe.apply(d); }
        };
    }

    public void setCatalogo(List<C> elementos) {
        C actual = combo.getValue();
        List<C> lista = elementos == null ? List.of() : elementos;
        combo.setItems(FXCollections.observableArrayList(lista));
        if (actual != null) {
            // Se conserva lo elegido aunque el catálogo se haya vuelto a leer (se compara por ID)
            int idActual = adaptador.idDeCatalogo(actual);
            C igual = null;
            for (C e : lista) {
                if (adaptador.idDeCatalogo(e) == idActual) {
                    igual = e;
                    break;
                }
            }
            combo.setValue(igual);
        }
    }

    /** Texto que aparece dentro de la lista desplegable cuando el catálogo no tiene elementos. */
    public void setTextoCatalogoVacio(String texto) {
        avisoCatalogoVacio.setText(texto);
    }

    /**
     * Vuelve a leer el catálogo cada vez que se abre la lista desplegable, para que lo agregado
     * en otra pantalla (por ejemplo, Ventas › Catálogo de servicios) aparezca sin reiniciar.
     * No interrumpe la búsqueda al teclear ({@link Autocompletar}).
     */
    public void setFuenteCatalogo(FuenteCatalogo<C> fuente) {
        if (fuente == null) {
            return;
        }
        combo.addEventHandler(ComboBoxBase.ON_SHOWING, e -> {
            if (combo.getItems() instanceof FilteredList) {
                return; // el usuario está buscando: no se reemplaza la lista filtrada
            }
            try {
                setCatalogo(fuente.leer());
            } catch (Exception ex) {
                // Si falla la lectura se conserva la lista que ya estaba
            }
        });
    }

    public void setAlCambiar(Runnable r) {
        this.alCambiar = r == null ? () -> { } : r;
    }

    /**
     * Agrega el elemento elegido con la cantidad del spinner. Si ya estaba, suma la cantidad.
     *
     * @return mensaje para el usuario si falta algo, o null si se agregó
     */
    public String agregarSeleccion() {
        C elegido = combo.getValue();
        if (elegido == null) {
            return "Elige primero un elemento de la lista desplegable.";
        }
        Integer cant = leerCantidad();
        if (cant == null || cant <= 0) {
            return "La cantidad debe ser un número entero mayor a cero.";
        }
        int id = adaptador.idDeCatalogo(elegido);
        for (D d : renglones) {
            if (adaptador.idDeDetalle(d) == id) {
                adaptador.setCantidad(d, adaptador.cantidad(d) + cant);
                tabla.refresh();
                actualizarTotal();
                return null;
            }
        }
        renglones.add(adaptador.crear(elegido, cant));
        cantidad.getValueFactory().setValue(1);
        actualizarTotal();
        return null;
    }

    /** @return mensaje si no hay renglón seleccionado, o null si se quitó */
    public String quitarSeleccion() {
        D sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return "Selecciona en la tabla el renglón que deseas quitar.";
        }
        renglones.remove(sel);
        actualizarTotal();
        return null;
    }

    public void setRenglones(List<D> lista) {
        renglones.setAll(lista == null ? List.of() : lista);
        actualizarTotal();
    }

    public List<D> getRenglones() {
        return new ArrayList<>(renglones);
    }

    public void limpiar() {
        renglones.clear();
        combo.setValue(null);
        cantidad.getValueFactory().setValue(1);
        actualizarTotal();
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (D d : renglones) {
            total = total.add(adaptador.importe(d));
        }
        return total;
    }

    private Integer leerCantidad() {
        try {
            String texto = cantidad.getEditor().getText();
            int valor = Integer.parseInt(texto == null ? "" : texto.trim());
            cantidad.getValueFactory().setValue(valor);
            return valor;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void actualizarTotal() {
        if (lblTotal != null) {
            lblTotal.setText(prefijoTotal + MONEDA.format(getTotal()) + "  ·  " + renglones.size()
                    + (renglones.size() == 1 ? " renglón" : " renglones"));
        }
        alCambiar.run();
    }
}
