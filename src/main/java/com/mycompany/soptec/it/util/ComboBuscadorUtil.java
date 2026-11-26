package com.mycompany.soptec.it.util;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * Conecta un ComboBox descriptivo con un TextField buscador para evitar captura manual de IDs.
 */
public final class ComboBuscadorUtil {

    private static final String CLAVE_FILTRO = "soptec.buscador.filtro";

    private ComboBuscadorUtil() {
    }

    public static <T> void configurar(ComboBox<T> combo, TextField buscador,
            List<T> elementos, Function<T, String> descriptor) {
        ObservableList<T> base = FXCollections.observableArrayList(elementos);
        FilteredList<T> filtrados = new FilteredList<>(base, item -> true);

        combo.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : descriptor.apply(object);
            }

            @Override
            public T fromString(String string) {
                if (string == null) {
                    return null;
                }
                for (T item : base) {
                    if (descriptor.apply(item).equals(string)) {
                        return item;
                    }
                }
                return null;
            }
        });

        combo.setItems(filtrados);

        if (buscador != null) {
            // Al recargar la lista se quita el filtro anterior (si no, se acumulan y trabajan con datos viejos)
            Object anterior = buscador.getProperties().get(CLAVE_FILTRO);
            if (anterior instanceof ChangeListener) {
                @SuppressWarnings("unchecked")
                ChangeListener<String> viejo = (ChangeListener<String>) anterior;
                buscador.textProperty().removeListener(viejo);
            }
            ChangeListener<String> filtroListener = (obs, oldVal, newVal) -> {
                // Sin distinguir mayúsculas ni acentos: «sofia ramirez» encuentra a «Sofía Ramírez»
                String filtro = normalizar(newVal);
                String[] palabras = filtro.isEmpty() ? new String[0] : filtro.split("\\s+");
                T seleccionado = combo.getValue();
                filtrados.setPredicate(item -> {
                    String texto = normalizar(descriptor.apply(item));
                    for (String p : palabras) {
                        if (!texto.contains(p)) {
                            return false;
                        }
                    }
                    return true;
                });
                if (seleccionado != null && filtrados.contains(seleccionado)) {
                    combo.setValue(seleccionado);
                } else if (!filtro.isEmpty() && filtrados.size() == 1) {
                    // Si solo queda una coincidencia se elige sola
                    combo.setValue(filtrados.get(0));
                }
            };
            buscador.getProperties().put(CLAVE_FILTRO, filtroListener);
            buscador.textProperty().addListener(filtroListener);
            // Si ya había texto escrito, se aplica a la lista nueva
            filtroListener.changed(buscador.textProperty(), null, buscador.getText());
        }
    }

    /** Minúsculas y sin acentos, para comparar lo que se teclea con los nombres guardados. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim();
    }

    public static <T> void seleccionarPorPredicado(ComboBox<T> combo, java.util.function.Predicate<T> predicado) {
        if (combo.getItems() == null) {
            return;
        }
        for (T item : combo.getItems()) {
            if (predicado.test(item)) {
                combo.setValue(item);
                return;
            }
        }
    }
}
