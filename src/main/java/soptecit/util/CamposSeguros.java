package soptecit.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

/**
 * Protege los campos contra errores de captura comunes:
 * <ul>
 *   <li><b>Fechas:</b> se escriben como dd/mm/aaaa (también acepta 5/3/2026 o 2026-03-05). Una fecha mal escrita
 *       ya no provoca un error del sistema, y lo escrito se toma aunque no se presione Enter.</li>
 *   <li><b>Cantidades (spinners):</b> solo aceptan números y el valor escrito se guarda aunque no se presione Enter
 *       (antes se quedaba el valor anterior sin avisar).</li>
 *   <li><b>Precios, montos y existencias:</b> solo aceptan números (se ignoran "$" y comas).</li>
 *   <li><b>Teléfonos:</b> solo números, espacios, +, - y paréntesis. <b>RFC:</b> en mayúsculas y sin espacios.
 *       <b>Correo:</b> sin espacios.</li>
 * </ul>
 */
public final class CamposSeguros {

    public static final String CLAVE = "soptec.camposeguro";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter[] ACEPTADOS = {
        estricto("d/M/uuuu"), estricto("d-M-uuuu"), estricto("d.M.uuuu"), estricto("uuuu-M-d"),
        estricto("d/M/uu"), estricto("d-M-uu")
    };
    private static final Pattern DECIMAL = Pattern.compile("\\d{0,10}([.]\\d{0,2})?");
    private static final Pattern ENTERO = Pattern.compile("\\d{0,9}");

    private CamposSeguros() {
    }

    private static DateTimeFormatter estricto(String patron) {
        return DateTimeFormatter.ofPattern(patron, Locale.ROOT).withResolverStyle(java.time.format.ResolverStyle.STRICT);
    }

    // ------------------------------------------------------------------ Fechas
    public static void fecha(DatePicker dp) {
        if (dp == null || dp.getProperties().containsKey(CLAVE)) {
            return;
        }
        dp.getProperties().put(CLAVE, Boolean.TRUE);
        dp.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate f) {
                return f == null ? "" : FORMATO.format(f);
            }

            @Override
            public LocalDate fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                LocalDate f = interpretarFecha(texto);
                return f != null ? f : dp.getValue(); // fecha mal escrita: se conserva la anterior
            }
        });
        if (dp.getPromptText() == null || dp.getPromptText().isBlank()) {
            dp.setPromptText("dd/mm/aaaa");
        }
        // Lo escrito se toma al salir del campo (sin presionar Enter); si no es fecha válida, se regresa a la anterior
        dp.getEditor().focusedProperty().addListener((o, a, enfocado) -> {
            if (!enfocado) {
                String texto = dp.getEditor().getText();
                if (texto == null || texto.isBlank()) {
                    dp.setValue(null);
                    return;
                }
                LocalDate f = interpretarFecha(texto);
                if (f != null) {
                    dp.setValue(f);
                }
                dp.getEditor().setText(dp.getConverter().toString(dp.getValue()));
            }
        });
        if (dp.getTooltip() == null) {
            dp.setTooltip(new Tooltip("Escribe la fecha como dd/mm/aaaa o elígela del calendario"));
        }
    }

    /** Interpreta varias formas de escribir una fecha; null si no se entiende. */
    public static LocalDate interpretarFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = texto.trim();
        for (DateTimeFormatter f : ACEPTADOS) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignorada) {
                // se prueba el siguiente formato
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Spinners
    @SuppressWarnings("unchecked")
    public static void spinner(Spinner<?> sp) {
        if (sp == null || !sp.isEditable() || sp.getProperties().containsKey(CLAVE)) {
            return;
        }
        sp.getProperties().put(CLAVE, Boolean.TRUE);
        sp.getEditor().setTextFormatter(new TextFormatter<>(filtro(ENTERO)));
        sp.getEditor().focusedProperty().addListener((o, a, enfocado) -> {
            if (!enfocado) {
                confirmarSpinner((Spinner<Integer>) sp);
            }
        });
        sp.getEditor().textProperty().addListener((o, a, n) -> {
            if (sp.getEditor().isFocused()) {
                confirmarSpinner((Spinner<Integer>) sp);
            }
        });
    }

    private static void confirmarSpinner(Spinner<Integer> sp) {
        SpinnerValueFactory<Integer> f = sp.getValueFactory();
        if (f == null) {
            return;
        }
        String texto = sp.getEditor().getText();
        if (texto == null || texto.isBlank()) {
            if (!sp.getEditor().isFocused()) {
                sp.getEditor().setText(String.valueOf(f.getValue()));
            }
            return;
        }
        try {
            int v = Integer.parseInt(texto.trim());
            if (f instanceof SpinnerValueFactory.IntegerSpinnerValueFactory) {
                SpinnerValueFactory.IntegerSpinnerValueFactory r = (SpinnerValueFactory.IntegerSpinnerValueFactory) f;
                v = Math.max(r.getMin(), Math.min(r.getMax(), v));
            }
            if (f.getValue() == null || f.getValue() != v) {
                f.setValue(v);
            }
            if (!sp.getEditor().isFocused()) {
                sp.getEditor().setText(String.valueOf(v));
            }
        } catch (NumberFormatException ex) {
            sp.getEditor().setText(String.valueOf(f.getValue()));
        }
    }

    // ------------------------------------------------------------------ Campos de texto
    /** Aplica la protección según el nombre (fx:id) del campo. */
    public static void textoSegunId(TextField campo) {
        if (campo == null || campo.getId() == null || !campo.isEditable() || campo.getProperties().containsKey(CLAVE)) {
            return;
        }
        String id = campo.getId().toLowerCase(Locale.ROOT);
        if (id.contains("buscar") || id.contains("filtro")) {
            return;
        }
        if (id.contains("precio") || id.contains("monto") || id.contains("costo") || id.contains("pagoinicial")) {
            dinero(campo);
        } else if (id.contains("existencias") || id.contains("cantidad")) {
            entero(campo);
        } else if (id.contains("telefono")) {
            telefono(campo);
        } else if (id.contains("rfc")) {
            rfc(campo);
        } else if (id.contains("correo")) {
            sinEspacios(campo);
        }
    }

    public static void dinero(TextField campo) {
        marcar(campo);
        campo.setTextFormatter(new TextFormatter<>(cambio -> {
            String limpio = cambio.getText().replace("$", "").replace(",", "").replace(" ", "");
            cambio.setText(limpio);
            String resultado = cambio.getControlNewText();
            return DECIMAL.matcher(resultado).matches() ? cambio : null;
        }));
        ayuda(campo, "Solo números; usa punto para los centavos (ej. 1250.50)");
    }

    public static void entero(TextField campo) {
        marcar(campo);
        campo.setTextFormatter(new TextFormatter<>(filtro(ENTERO)));
        ayuda(campo, "Solo números enteros");
    }

    public static void telefono(TextField campo) {
        marcar(campo);
        campo.setTextFormatter(new TextFormatter<>(cambio -> {
            String r = cambio.getControlNewText();
            return r.matches("[0-9+()\\- ]{0,20}") ? cambio : null;
        }));
        ayuda(campo, "Solo números (10 dígitos), puede llevar espacios o guiones");
    }

    public static void rfc(TextField campo) {
        marcar(campo);
        campo.setTextFormatter(new TextFormatter<>(cambio -> {
            cambio.setText(cambio.getText().toUpperCase(Locale.ROOT).replace(" ", ""));
            return cambio.getControlNewText().length() <= 13 ? cambio : null;
        }));
        ayuda(campo, "Se escribe en mayúsculas automáticamente (12 o 13 caracteres)");
    }

    public static void sinEspacios(TextField campo) {
        marcar(campo);
        campo.setTextFormatter(new TextFormatter<>(cambio -> {
            cambio.setText(cambio.getText().replace(" ", ""));
            return cambio;
        }));
    }

    private static UnaryOperator<TextFormatter.Change> filtro(Pattern patron) {
        return cambio -> patron.matcher(cambio.getControlNewText()).matches() ? cambio : null;
    }

    private static void marcar(TextField campo) {
        campo.getProperties().put(CLAVE, Boolean.TRUE);
    }

    private static void ayuda(TextField campo, String texto) {
        if (campo.getTooltip() == null) {
            campo.setTooltip(new Tooltip(texto));
        }
    }
}
