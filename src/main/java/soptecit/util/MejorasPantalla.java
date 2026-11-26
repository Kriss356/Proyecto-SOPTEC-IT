package soptecit.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;

/**
 * Aplica a cualquier pantalla recién abierta las ayudas de captura:
 * <ul>
 *   <li>Cada campo "Buscar..." filtra en vivo la tabla de su tarjeta ({@link BusquedaTabla}).</li>
 *   <li>Cada combo permite escribir para buscar en la lista ({@link Autocompletar}).</li>
 *   <li>Cada área de texto se puede ampliar a una ventana grande ({@link TextoAmpliable}).</li>
 *   <li>Fechas, cantidades, precios, teléfonos, RFC y correos protegidos contra errores ({@link CamposSeguros}).</li>
 * </ul>
 * Un campo puede indicar a qué tabla busca con {@code userData="tblAlgo"} (fx:id de la tabla).
 */
public final class MejorasPantalla {

    private MejorasPantalla() {
    }

    public static void aplicar(Node raiz) {
        if (raiz == null) {
            return;
        }
        List<Node> nodos = new ArrayList<>();
        recorrer(raiz, nodos);
        for (Node n : nodos) {
            if (n instanceof TextField && esBuscador((TextField) n)) {
                TableView<?> tabla = tablaPara((TextField) n, raiz);
                if (tabla != null) {
                    BusquedaTabla.conectar((TextField) n, tabla);
                }
            } else if (n instanceof TextField) {
                CamposSeguros.textoSegunId((TextField) n);
            } else if (n instanceof DatePicker) {
                CamposSeguros.fecha((DatePicker) n);
            } else if (n instanceof Spinner) {
                CamposSeguros.spinner((Spinner<?>) n);
            } else if (n instanceof ComboBox) {
                Autocompletar.aplicar((ComboBox<?>) n);
            } else if (n instanceof TextArea) {
                TextoAmpliable.aplicar((TextArea) n);
            }
        }
        // Campos obligatorios (etiqueta con *): se marcan en rojo si faltan al guardar
        Requeridos.registrar(raiz);
    }

    private static boolean esBuscador(TextField campo) {
        if (campo.getProperties().containsKey(BusquedaTabla.CLAVE) || !campo.isEditable()) {
            return false;
        }
        if (campo.getUserData() instanceof String && ((String) campo.getUserData()).startsWith("tbl")) {
            return true;
        }
        String prompt = campo.getPromptText() == null ? "" : campo.getPromptText().toLowerCase(Locale.ROOT);
        return prompt.startsWith("buscar");
    }

    /** La tabla indicada en userData o, si no, la primera tabla de la tarjeta más cercana al campo. */
    private static TableView<?> tablaPara(TextField campo, Node raiz) {
        if (campo.getUserData() instanceof String) {
            String id = (String) campo.getUserData();
            List<Node> todos = new ArrayList<>();
            recorrer(raiz, todos);
            for (Node n : todos) {
                if (n instanceof TableView && id.equals(n.getId())) {
                    return (TableView<?>) n;
                }
            }
        }
        Parent p = campo.getParent();
        while (p != null && p != raiz.getParent()) {
            List<Node> dentro = new ArrayList<>();
            recorrer(p, dentro);
            for (Node n : dentro) {
                if (n instanceof TableView && !n.getProperties().containsKey(BusquedaTabla.CLAVE)) {
                    return (TableView<?>) n;
                }
            }
            p = p.getParent();
        }
        return null;
    }

    /** Recorre el árbol completo, incluso el contenido de pestañas y paneles con scroll aún no dibujados. */
    public static void recorrer(Node n, List<Node> salida) {
        if (n == null) {
            return;
        }
        salida.add(n);
        if (n instanceof TableView || n instanceof ComboBox || n instanceof TextArea
                || n instanceof DatePicker || n instanceof Spinner || n instanceof TextField) {
            return; // no hace falta entrar a sus partes internas
        }
        if (n instanceof TabPane) {
            for (Tab t : ((TabPane) n).getTabs()) {
                recorrer(t.getContent(), salida);
            }
            return;
        }
        if (n instanceof ScrollPane) {
            recorrer(((ScrollPane) n).getContent(), salida);
            return;
        }
        if (n instanceof SplitPane) {
            for (Node item : ((SplitPane) n).getItems()) {
                recorrer(item, salida);
            }
            return;
        }
        if (n instanceof TitledPane) {
            recorrer(((TitledPane) n).getContent(), salida);
            return;
        }
        if (n instanceof Parent) {
            for (Node hijo : ((Parent) n).getChildrenUnmodifiable()) {
                recorrer(hijo, salida);
            }
        }
    }
}
