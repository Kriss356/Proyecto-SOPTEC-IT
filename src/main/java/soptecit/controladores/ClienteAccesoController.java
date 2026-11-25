package soptecit.controladores;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.servicio.PortalClienteService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.AppLogger;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import soptecit.util.SesionCliente;

/**
 * Acceso al Portal del cliente: el cliente entra solo con su número de teléfono.
 * Si el número no existe, se le muestra un registro rápido (solo nombre y, opcional, correo).
 */
public class ClienteAccesoController implements Initializable {

    private static final int DIGITOS = 10;

    @FXML private VBox panelTelefono;
    @FXML private VBox panelRegistro;
    @FXML private TextField txtTelefono;
    @FXML private Label lblContador;
    @FXML private Label lblError;
    @FXML private Button btnContinuar;
    @FXML private Label lblNumeroNuevo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtCorreo;
    @FXML private Label lblErrorRegistro;
    @FXML private Button btnRegistrar;
    @FXML private Label lblAyuda;

    private final PortalClienteService service = new PortalClienteService();
    private String telefonoNuevo;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SesionCliente.cerrar();
        // Solo deja escribir números y como máximo 10.
        txtTelefono.setTextFormatter(new TextFormatter<String>(cambio -> {
            String nuevo = cambio.getControlNewText();
            return nuevo.matches("\\d{0," + DIGITOS + "}") ? cambio : null;
        }));
        txtTelefono.textProperty().addListener((o, a, n) -> {
            actualizarContador();
            mostrar(lblError, null);
        });
        txtNombre.textProperty().addListener((o, a, n) -> mostrar(lblErrorRegistro, null));
        lblAyuda.setText("¿Necesitas ayuda? Llámanos al " + PortalClienteController.TELEFONO_AYUDA
                + " y con gusto te atendemos.");
        actualizarContador();
        Platform.runLater(txtTelefono::requestFocus);
    }

    // ----------------------------------------------------------- teclado en pantalla

    @FXML
    private void tecla(ActionEvent event) {
        String digito = ((Button) event.getSource()).getText();
        if (txtTelefono.getText().length() < DIGITOS) {
            txtTelefono.appendText(digito);
        }
        txtTelefono.positionCaret(txtTelefono.getText().length());
    }

    @FXML
    private void borrarDigito(ActionEvent event) {
        String t = txtTelefono.getText();
        if (!t.isEmpty()) {
            txtTelefono.setText(t.substring(0, t.length() - 1));
            txtTelefono.positionCaret(txtTelefono.getText().length());
        }
    }

    @FXML
    private void limpiarTelefono(ActionEvent event) {
        txtTelefono.clear();
        txtTelefono.requestFocus();
    }

    private void actualizarContador() {
        int n = txtTelefono.getText().length();
        lblContador.setText(n == DIGITOS ? "Número completo. Toca «Continuar»." : n + " de " + DIGITOS + " números");
        lblContador.setStyle(n == DIGITOS ? "-fx-text-fill: -c-verde-osc;" : "");
    }

    // ----------------------------------------------------------- entrar

    @FXML
    private void continuar(ActionEvent event) {
        try {
            String telefono = service.normalizarTelefono(txtTelefono.getText());
            Cliente cliente = service.buscarPorTelefono(telefono);
            if (cliente != null) {
                entrar(cliente);
                return;
            }
            telefonoNuevo = telefono;
            lblNumeroNuevo.setText("No encontramos el número " + formatear(telefono)
                    + ". Regístrate en un momento: solo pedimos tu nombre.");
            cambiarPanel(true);
            Platform.runLater(txtNombre::requestFocus);
        } catch (SoptecException ex) {
            mostrar(lblError, ex.getMessage() != null ? AlertaUtil.mensajeUsuario(ex) : "Revisa tu número.");
        } catch (Exception ex) {
            AlertaUtil.errorControlado("Portal cliente", "ACCESO_CLIENTE", ex);
        }
    }

    @FXML
    private void registrar(ActionEvent event) {
        try {
            Cliente nuevo = service.registrar(txtNombre.getText(), telefonoNuevo, txtCorreo.getText());
            AlertaUtil.info("¡Bienvenido, " + nuevo.getNombreCliente() + "!",
                    "Ya quedaste registrado. La próxima vez solo escribe tu número "
                    + formatear(telefonoNuevo) + " para entrar.");
            entrar(nuevo);
        } catch (SoptecException ex) {
            mostrar(lblErrorRegistro, AlertaUtil.mensajeUsuario(ex));
        } catch (Exception ex) {
            AlertaUtil.errorControlado("Portal cliente", "REGISTRO_CLIENTE", ex);
        }
    }

    @FXML
    private void cambiarNumero(ActionEvent event) {
        cambiarPanel(false);
        txtTelefono.requestFocus();
        txtTelefono.selectAll();
    }

    private void entrar(Cliente cliente) {
        SesionCliente.iniciar(cliente);
        AppLogger.registrarInfo("Portal cliente", "ClienteAccesoController", "INICIO_SESION_CLIENTE",
                "Entró el cliente ID=" + cliente.getIdCliente());
        cambiarVista("/soptecit/vistas/PortalCliente.fxml", btnContinuar);
    }

    @FXML
    private void volverAlLogin(ActionEvent event) {
        SesionCliente.cerrar();
        cambiarVista("/soptecit/vistas/Login.fxml", (Node) event.getSource());
    }

    // ----------------------------------------------------------- utilidades

    private void cambiarVista(String ruta, Node origen) {
        try {
            Parent vista = FXMLLoader.load(getClass().getResource(ruta));
            origen.getScene().setRoot(vista);
        } catch (IOException ex) {
            AppLogger.registrarExcepcion("Portal cliente", "ClienteAccesoController", "CAMBIAR_VISTA", ex);
            AlertaUtil.error("No se pudo abrir la pantalla", "Intenta de nuevo. Si se repite, avisa al personal de SOPTEC-IT.");
        }
    }

    private void cambiarPanel(boolean registro) {
        panelTelefono.setVisible(!registro);
        panelTelefono.setManaged(!registro);
        panelRegistro.setVisible(registro);
        panelRegistro.setManaged(registro);
        mostrar(lblError, null);
        mostrar(lblErrorRegistro, null);
    }

    private void mostrar(Label etiqueta, String mensaje) {
        boolean hay = mensaje != null && !mensaje.isBlank();
        if (hay) {
            etiqueta.setText(mensaje);
        }
        etiqueta.setVisible(hay);
        etiqueta.setManaged(hay);
    }

    /** 5512345678 → 55 1234 5678 */
    public static String formatear(String telefono) {
        if (telefono == null || telefono.length() != 10) {
            return telefono == null ? "" : telefono;
        }
        return telefono.substring(0, 2) + " " + telefono.substring(2, 6) + " " + telefono.substring(6);
    }
}
