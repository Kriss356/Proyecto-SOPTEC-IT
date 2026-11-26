package soptecit.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyEvent;

/**
 * Listas fijas que se eligen de un combo en lugar de escribirse a mano.
 */
public final class Catalogos {

    /** Las 32 entidades federativas de México, en orden alfabético. */
    public static final List<String> ESTADOS_MEXICO = Collections.unmodifiableList(Arrays.asList(
            "Aguascalientes", "Baja California", "Baja California Sur", "Campeche", "Chiapas", "Chihuahua",
            "Ciudad de México", "Coahuila", "Colima", "Durango", "Estado de México", "Guanajuato", "Guerrero",
            "Hidalgo", "Jalisco", "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca", "Puebla",
            "Querétaro", "Quintana Roo", "San Luis Potosí", "Sinaloa", "Sonora", "Tabasco", "Tamaulipas",
            "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas"));

    private Catalogos() {
    }

    /**
     * Llena el combo con los 32 estados. Al escribir letras con el combo enfocado salta al estado
     * que empieza así (por ejemplo "nu" → Nuevo León), sin distinguir acentos ni mayúsculas.
     */
    public static void configurarEstados(ComboBox<String> combo) {
        combo.setItems(FXCollections.observableArrayList(ESTADOS_MEXICO));
        combo.setVisibleRowCount(12);
        combo.setEditable(false);
        StringBuilder tecleado = new StringBuilder();
        long[] ultimaTecla = {0};
        combo.addEventHandler(KeyEvent.KEY_TYPED, e -> {
            String c = e.getCharacter();
            if (c == null || c.isEmpty() || Character.isISOControl(c.charAt(0))) {
                return;
            }
            long ahora = System.currentTimeMillis();
            if (ahora - ultimaTecla[0] > 900) {
                tecleado.setLength(0);
            }
            ultimaTecla[0] = ahora;
            tecleado.append(c);
            String buscado = sinAcentos(tecleado.toString());
            for (String estado : ESTADOS_MEXICO) {
                if (sinAcentos(estado).startsWith(buscado)) {
                    combo.setValue(estado);
                    if (combo.isShowing()) {
                        combo.getSelectionModel().select(estado);
                    }
                    break;
                }
            }
            e.consume();
        });
        combo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? combo.getPromptText() : item);
            }
        });
    }

    /**
     * Devuelve el nombre oficial del estado si el texto guardado coincide con uno (sin importar acentos,
     * mayúsculas o abreviaturas comunes); si no coincide, regresa el texto tal cual.
     */
    public static String normalizarEstado(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = sinAcentos(texto).replace(".", "").trim();
        switch (t) {
            case "cdmx": case "df": case "distrito federal": case "cd de mexico": case "mexico city":
                return "Ciudad de México";
            case "edomex": case "edo mex": case "edo de mexico": case "mexico": case "estado de mexico":
                return "Estado de México";
            case "bc": return "Baja California";
            case "bcs": return "Baja California Sur";
            case "nl": return "Nuevo León";
            case "slp": return "San Luis Potosí";
            case "qroo": case "q roo": return "Quintana Roo";
            case "coahuila de zaragoza": return "Coahuila";
            case "michoacan de ocampo": return "Michoacán";
            case "veracruz de ignacio de la llave": return "Veracruz";
            default:
                break;
        }
        for (String estado : ESTADOS_MEXICO) {
            if (sinAcentos(estado).equals(t)) {
                return estado;
            }
        }
        return texto.trim();
    }

    /** Pone en el combo el estado guardado (normalizado). */
    public static void seleccionarEstado(ComboBox<String> combo, String guardado) {
        combo.setValue(normalizarEstado(guardado));
    }

    static String sinAcentos(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }
}
