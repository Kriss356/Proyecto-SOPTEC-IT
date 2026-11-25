package soptecit.util;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.util.StringConverter;

/**
 * Lista con casillas para elegir varios registros a la vez (relaciones muchos a muchos):
 * se marcan todos los que corresponden y se guardan de una sola vez.
 */
public class ListaCasillas<T> {

    private final ListView<T> lista;
    private final Map<T, BooleanProperty> marcas = new IdentityHashMap<>();
    private final Label lblConteo;
    private final String sustantivo;

    public ListaCasillas(ListView<T> lista, Function<T, String> texto, Label lblConteo, String sustantivo) {
        this.lista = lista;
        this.lblConteo = lblConteo;
        this.sustantivo = sustantivo;
        lista.setCellFactory(CheckBoxListCell.forListView(this::marca, new StringConverter<T>() {
            @Override
            public String toString(T obj) {
                return obj == null ? "" : texto.apply(obj);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        }));
        lista.setPlaceholder(new Label("No hay registros activos"));
    }

    private BooleanProperty marca(T item) {
        return marcas.computeIfAbsent(item, k -> {
            SimpleBooleanProperty p = new SimpleBooleanProperty(false);
            p.addListener((o, a, n) -> actualizarConteo());
            return p;
        });
    }

    public void setElementos(List<T> elementos) {
        marcas.clear();
        lista.setItems(FXCollections.observableArrayList(elementos));
        actualizarConteo();
    }

    /** Marca exactamente los elementos que cumplan la condición. */
    public void marcarSolo(Predicate<T> condicion) {
        for (T item : lista.getItems()) {
            marca(item).set(condicion.test(item));
        }
        lista.refresh();
        actualizarConteo();
    }

    public List<T> getMarcados() {
        List<T> res = new ArrayList<>();
        for (T item : lista.getItems()) {
            if (marca(item).get()) {
                res.add(item);
            }
        }
        return res;
    }

    private void actualizarConteo() {
        if (lblConteo != null) {
            int n = 0;
            for (BooleanProperty p : marcas.values()) {
                if (p.get()) {
                    n++;
                }
            }
            lblConteo.setText(n + " " + sustantivo + " marcado(s)");
        }
    }
}
