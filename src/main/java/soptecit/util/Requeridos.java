package soptecit.util;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Pane;

/**
 * Campos obligatorios: los que tienen una etiqueta que termina en "*".
 * Cuando al guardar falta algo, se marcan en rojo los que están vacíos (solo en lo que se ve en pantalla)
 * y la marca se quita sola en cuanto el usuario escribe o elige algo.
 */
public final class Requeridos {

    private static final String CLAVE_LISTA = "soptec.requeridos";
    private static final String CLAVE_ESCUCHA = "soptec.requerido.escucha";
    private static final String ESTILO = "campo-error";

    private Requeridos() {
    }

    /** Busca las etiquetas con "*" y guarda qué control acompaña a cada una. */
    public static void registrar(Node raiz) {
        if (raiz == null) {
            return;
        }
        List<Node> todos = new ArrayList<>();
        MejorasPantalla.recorrer(raiz, todos);
        List<Node> campos = new ArrayList<>();
        for (Node n : todos) {
            if (n instanceof Label && ((Label) n).getText() != null && ((Label) n).getText().trim().endsWith("*")) {
                Node campo = campoDeEtiqueta((Label) n);
                if (campo != null && !campos.contains(campo)) {
                    campos.add(campo);
                }
            }
        }
        raiz.getProperties().put(CLAVE_LISTA, campos);
    }

    /** Marca en rojo los obligatorios vacíos que se ven en pantalla. @return cuántos marcó */
    @SuppressWarnings("unchecked")
    public static int marcarVacios(Node raiz) {
        if (raiz == null || !(raiz.getProperties().get(CLAVE_LISTA) instanceof List)) {
            return 0;
        }
        int marcados = 0;
        for (Node campo : (List<Node>) raiz.getProperties().get(CLAVE_LISTA)) {
            if (visible(campo) && !campo.isDisabled() && vacio(campo)) {
                if (!campo.getStyleClass().contains(ESTILO)) {
                    campo.getStyleClass().add(ESTILO);
                }
                escucharParaQuitar(campo);
                marcados++;
            }
        }
        return marcados;
    }

    private static Node campoDeEtiqueta(Label etiqueta) {
        Node actual = etiqueta;
        // La etiqueta puede estar dentro de una fila (por ejemplo, junto al enlace «Ampliar»)
        if (actual.getParent() instanceof javafx.scene.layout.HBox
                && actual.getParent().getParent() instanceof javafx.scene.layout.VBox) {
            actual = actual.getParent();
        }
        if (!(actual.getParent() instanceof Pane)) {
            return null;
        }
        List<Node> hermanos = ((Pane) actual.getParent()).getChildren();
        int i = hermanos.indexOf(actual);
        if (i < 0 || i + 1 >= hermanos.size()) {
            return null;
        }
        Node siguiente = hermanos.get(i + 1);
        if (esCampo(siguiente)) {
            return siguiente;
        }
        if (siguiente instanceof Parent) {
            List<Node> dentro = new ArrayList<>();
            MejorasPantalla.recorrer(siguiente, dentro);
            for (Node d : dentro) {
                if (esCampo(d)) {
                    return d;
                }
            }
        }
        return null;
    }

    private static boolean esCampo(Node n) {
        return (n instanceof TextInputControl && ((TextInputControl) n).isEditable()) || n instanceof ComboBoxBase;
    }

    private static boolean vacio(Node campo) {
        if (campo instanceof TextInputControl) {
            String t = ((TextInputControl) campo).getText();
            return t == null || t.isBlank();
        }
        if (campo instanceof ComboBox) {
            ComboBox<?> c = (ComboBox<?>) campo;
            if (c.isEditable()) {
                String t = c.getEditor().getText();
                return c.getValue() == null && (t == null || t.isBlank());
            }
            return c.getValue() == null;
        }
        if (campo instanceof DatePicker) {
            return ((DatePicker) campo).getValue() == null;
        }
        if (campo instanceof ComboBoxBase) {
            return ((ComboBoxBase<?>) campo).getValue() == null;
        }
        return false;
    }

    private static boolean visible(Node n) {
        if (n.getScene() == null) {
            return false;
        }
        for (Node p = n; p != null; p = p.getParent()) {
            if (!p.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static void escucharParaQuitar(Node campo) {
        if (campo.getProperties().containsKey(CLAVE_ESCUCHA)) {
            return;
        }
        campo.getProperties().put(CLAVE_ESCUCHA, Boolean.TRUE);
        Runnable quitar = () -> campo.getStyleClass().remove(ESTILO);
        if (campo instanceof TextInputControl) {
            ((TextInputControl) campo).textProperty().addListener((o, a, n) -> quitar.run());
        } else if (campo instanceof ComboBoxBase) {
            ((ComboBoxBase<?>) campo).valueProperty().addListener((o, a, n) -> quitar.run());
            if (campo instanceof ComboBox && ((ComboBox<?>) campo).isEditable()) {
                ((ComboBox<?>) campo).getEditor().textProperty().addListener((o, a, n) -> quitar.run());
            }
        }
    }
}
