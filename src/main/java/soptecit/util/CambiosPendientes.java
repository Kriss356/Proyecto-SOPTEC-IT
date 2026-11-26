package soptecit.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * Detecta si el usuario escribió o eligió algo en un formulario y todavía no lo guarda, para avisarle
 * antes de cambiar de pantalla, cerrar sesión o salir (y que no pierda lo capturado).
 *
 * <p>Solo cuenta lo que hace el usuario (teclear o elegir con el control enfocado), no lo que el sistema
 * llena solo al abrir un registro. Los buscadores y filtros no cuentan. Al presionar Guardar, Nuevo, etc.
 * se considera atendido, salvo que en ese momento aparezca un error (por ejemplo, faltó un dato).</p>
 */
public final class CambiosPendientes {

    private static final String CLAVE = "soptec.cambios";
    private static Estado actual;

    private CambiosPendientes() {
    }

    private static final class Estado {
        boolean sucio;
        boolean enAccion;
    }

    /** Empieza a vigilar la pantalla recién abierta. */
    public static void vigilar(Node raiz) {
        if (raiz == null) {
            return;
        }
        Estado estado = new Estado();
        raiz.getProperties().put(CLAVE, estado);
        actual = estado;

        List<Node> nodos = new ArrayList<>();
        MejorasPantalla.recorrer(raiz, nodos);
        for (Node n : nodos) {
            if (esFiltro(n)) {
                continue;
            }
            if (n instanceof TextInputControl) {
                TextInputControl t = (TextInputControl) n;
                t.addEventFilter(KeyEvent.KEY_TYPED, e -> {
                    if (t.isEditable() && !esFiltro(t)) {
                        estado.sucio = true;
                    }
                });
            } else if (n instanceof ComboBoxBase) {
                ComboBoxBase<?> c = (ComboBoxBase<?>) n;
                c.valueProperty().addListener((o, a, v) -> {
                    if (c.isFocused() || c.isShowing()) {
                        estado.sucio = true;
                    }
                });
            } else if (n instanceof Spinner) {
                Spinner<?> sp = (Spinner<?>) n;
                sp.valueProperty().addListener((o, a, v) -> {
                    if (sp.isFocused() || sp.getEditor().isFocused()) {
                        estado.sucio = true;
                    }
                });
            }
        }
        // Botones que guardan o limpian el formulario: lo capturado ya se atendió
        raiz.addEventFilter(ActionEvent.ACTION, e -> {
            if (e.getTarget() instanceof Button && atiende(((Button) e.getTarget()).getText())) {
                estado.enAccion = true;
                estado.sucio = false;
            }
        });
        raiz.addEventHandler(ActionEvent.ACTION, e -> estado.enAccion = false);
        // Al abrir otro registro desde la tabla, lo que se había escrito se reemplaza
        raiz.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            for (Node p = e.getTarget() instanceof Node ? (Node) e.getTarget() : null; p != null; p = p.getParent()) {
                if (p instanceof TableView) {
                    estado.sucio = false;
                    return;
                }
            }
        });
    }

    /** Lo llama AlertaUtil al mostrar un error o advertencia: si fue durante un Guardar, sigue sin guardarse. */
    public static void alMostrarError() {
        if (actual != null && actual.enAccion) {
            actual.sucio = true;
        }
    }

    /** El usuario decidió salir sin guardar: ya no se vuelve a preguntar por esa pantalla. */
    public static void descartar(Node raiz) {
        Object e = raiz != null ? raiz.getProperties().get(CLAVE) : null;
        if (e instanceof Estado) {
            ((Estado) e).sucio = false;
        }
    }

    /** ¿Hay algo capturado sin guardar en esa pantalla? */
    public static boolean haySinGuardar(Node raiz) {
        Object e = raiz != null ? raiz.getProperties().get(CLAVE) : null;
        return e instanceof Estado && ((Estado) e).sucio;
    }

    private static boolean atiende(String texto) {
        if (texto == null) {
            return false;
        }
        String t = texto.toLowerCase(Locale.ROOT);
        return t.startsWith("guardar") || t.startsWith("registrar") || t.startsWith("aplicar") || t.startsWith("generar")
                || t.startsWith("nuevo") || t.startsWith("nueva") || t.startsWith("limpiar") || t.startsWith("cancelar")
                || t.startsWith("eliminar") || t.startsWith("desactivar") || t.startsWith("reactivar")
                || t.startsWith("actualizar") || t.startsWith("enviar") || t.startsWith("aprobar")
                || t.startsWith("rechazar") || t.startsWith("cerrar proyecto") || t.startsWith("marcar");
    }

    private static boolean esFiltro(Node n) {
        if (n.getStyleClass().contains("campo-busqueda")) {
            return true;
        }
        String id = n.getId() == null ? "" : n.getId().toLowerCase(Locale.ROOT);
        if (id.contains("buscar") || id.contains("filtro") || id.contains("consultarid")) {
            return true;
        }
        if (n instanceof TextInputControl) {
            String prompt = ((TextInputControl) n).getPromptText();
            String p = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
            return p.startsWith("buscar") || p.startsWith("filtrar") || p.startsWith("escribe nombre o rfc");
        }
        return false;
    }
}
