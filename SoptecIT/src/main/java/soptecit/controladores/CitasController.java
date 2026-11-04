package soptecit.controladores;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import soptecit.util.Mensajes;
import soptecit.util.Tablas;

public class CitasController implements Initializable {

    @FXML private DatePicker dpFecha;
    @FXML private TextField txtBuscarCita;
    @FXML private ComboBox<String> cmbFiltroServicio;
    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private TableView<String[]> tblCitas;
    @FXML private Label lblResumen;

    @FXML private ComboBox<String> cmbCliente;
    @FXML private TextField txtEmpresa;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private DatePicker dpFechaCita;
    @FXML private ComboBox<String> cmbHora;
    @FXML private ComboBox<String> cmbServicio;
    @FXML private ComboBox<String> cmbAtiende;
    @FXML private TextField txtDireccion;
    @FXML private TextArea txtNecesidades;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFecha.setValue(LocalDate.now());
        dpFechaCita.setValue(LocalDate.now());

        for (int h = 8; h <= 18; h++) {
            cmbHora.getItems().add(String.format("%02d:00", h));
            cmbHora.getItems().add(String.format("%02d:30", h));
        }
        cmbCliente.getItems().addAll("Laura Hernández", "Roberto Juárez", "María Ortega", "Jorge Salinas");
        cmbAtiende.getItems().addAll("Ana López (Ventas)", "Luis Pérez (Ventas)", "Carlos Méndez (Lev.)");

        // Hora | Cliente | Empresa | Servicio | Atiende | Estado
        Tablas.cargar(tblCitas, new String[][]{
            {"09:00", "Roberto Juárez", "Clínica del Sol", "Levantamiento", "Carlos Méndez", "Atendida"},
            {"10:30", "Laura Hernández", "Corporativo Avante", "Asesoría", "Ana López", "Confirmada"},
            {"12:00", "María Ortega", "Despacho Ortega", "Mantenimiento", "Luis Pérez", "Programada"},
            {"16:00", "Jorge Salinas", "Oficinas LTJO", "Seguimiento", "Ana López", "Confirmada"},
            {"17:30", "Patricia Ruiz", "Sucursal Sur", "Asesoría", "Luis Pérez", "Cancelada"}
        });
    }

    @FXML
    private void irAHoy(ActionEvent event) {
        dpFecha.setValue(LocalDate.now());
    }

    @FXML
    private void guardarCita(ActionEvent event) {
        if (cmbCliente.getEditor().getText().trim().isEmpty() || dpFechaCita.getValue() == null
                || cmbHora.getValue() == null || cmbServicio.getValue() == null) {
            Mensajes.error("Faltan datos", "Indica cliente, fecha, hora y tipo de servicio.");
            return;
        }
        // TODO: INSERT INTO citas (...)
        Mensajes.info("Cita guardada", "La cita se agendó para el " + dpFechaCita.getValue() + " a las " + cmbHora.getValue() + ".");
        limpiar(null);
    }

    @FXML
    private void limpiar(ActionEvent event) {
        cmbCliente.getEditor().clear();
        cmbCliente.setValue(null);
        txtEmpresa.clear();
        txtTelefono.clear();
        txtCorreo.clear();
        cmbHora.setValue(null);
        cmbServicio.setValue(null);
        cmbAtiende.setValue(null);
        txtDireccion.clear();
        txtNecesidades.clear();
    }

    @FXML
    private void nuevoCliente(ActionEvent event) {
        PrincipalController.irA("Ventas");
    }

    @FXML
    private void cancelarCita(ActionEvent event) {
        if (tblCitas.getSelectionModel().getSelectedItem() == null) {
            Mensajes.info("Selecciona una cita", "Elige la cita que deseas cancelar en la tabla.");
            return;
        }
        if (Mensajes.confirmar("Cancelar cita", "¿Seguro que deseas cancelar la cita seleccionada?")) {
            // TODO: UPDATE citas SET estado = 'Cancelada'
            tblCitas.getSelectionModel().getSelectedItem()[5] = "Cancelada";
            tblCitas.refresh();
        }
    }

    @FXML
    private void reprogramar(ActionEvent event) {
        Mensajes.info("Reprogramar", "Selecciona la nueva fecha y hora en el formulario y guarda la cita.");
    }

    @FXML
    private void marcarAtendida(ActionEvent event) {
        String[] cita = tblCitas.getSelectionModel().getSelectedItem();
        if (cita != null) {
            cita[5] = "Atendida";
            tblCitas.refresh();
        }
    }
}
