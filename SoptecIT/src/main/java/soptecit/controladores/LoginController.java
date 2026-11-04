package soptecit.controladores;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import soptecit.util.Mensajes;

public class LoginController implements Initializable {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private ComboBox<String> cmbArea;
    @FXML private CheckBox chkRecordar;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbArea.getSelectionModel().selectFirst();
        // Al escribir de nuevo se oculta el mensaje de error
        txtUsuario.textProperty().addListener((o, a, n) -> mostrarError(null));
        txtContrasena.textProperty().addListener((o, a, n) -> mostrarError(null));
    }

    @FXML
    private void ingresar(ActionEvent event) {
        String usuario = txtUsuario.getText().trim();
        String contrasena = txtContrasena.getText();
        String area = cmbArea.getValue();

        if (usuario.isEmpty() || contrasena.isEmpty() || area == null) {
            mostrarError("Completa usuario, contraseña y área para continuar.");
            return;
        }

        // TODO: validar contra la tabla de usuarios en MySQL (usuario, contraseña y área)

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/soptecit/vistas/Principal.fxml"));
            Parent principal = loader.load();
            PrincipalController controlador = loader.getController();
            controlador.setUsuario(usuario, area);
            // Se cambia el contenido de la misma ventana (conserva la pantalla completa)
            btnIngresar.getScene().setRoot(principal);
        } catch (IOException ex) {
            mostrarError("No se pudo abrir la ventana principal: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void mostrarError(String mensaje) {
        boolean hay = mensaje != null;
        if (hay) {
            lblError.setText(mensaje);
        }
        lblError.setVisible(hay);
        lblError.setManaged(hay);
    }

    /** Cierra la aplicación completa (pide confirmación). */
    @FXML
    private void salirAplicacion(ActionEvent event) {
        if (Mensajes.confirmar("Cerrar la aplicación", "¿Deseas cerrar SOPTEC-IT?")) {
            Platform.exit();
        }
    }
}
