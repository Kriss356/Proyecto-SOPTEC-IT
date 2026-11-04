package soptecit.controladores;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import soptecit.util.Mensajes;

/**
 * Consulta de avance: muestra en una sola pantalla en qué área va el proyecto,
 * su porcentaje, pagos y la línea de tiempo. Resuelve la problemática de que el
 * cliente tenga que preguntar a cada departamento.
 */
public class SeguimientoController implements Initializable {

    @FXML private TextField txtFolioBuscar;
    @FXML private Label lblFolio;
    @FXML private Label lblCliente;
    @FXML private Label lblDescripcion;
    @FXML private Label lblEstadoProyecto;
    @FXML private ProgressBar pbAvance;
    @FXML private Label lblAvance;
    @FXML private Label lblAreaActual;
    @FXML private TextArea txtNotasCliente;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Los datos de ejemplo están en el FXML. Al conectar la BD se llenan en buscarProyecto().
    }

    @FXML
    private void buscarProyecto(ActionEvent event) {
        String folio = txtFolioBuscar.getText().trim();
        if (folio.isEmpty()) {
            Mensajes.info("Buscar proyecto", "Escribe el folio del proyecto o el nombre del cliente.");
            return;
        }
        // TODO: SELECT ... FROM proyectos WHERE folio = ? OR cliente LIKE ?
        //       y llenar lblFolio, lblCliente, pbAvance, línea de tiempo, pagos, etc.
        lblFolio.setText(folio.toUpperCase());
    }

    @FXML
    private void enviarActualizacion(ActionEvent event) {
        if (txtNotasCliente.getText().trim().isEmpty()) {
            Mensajes.error("Mensaje vacío", "Escribe la actualización que recibirá el cliente.");
            return;
        }
        Mensajes.info("Actualización enviada", "Se envió el estado del proyecto al contacto del cliente.");
        txtNotasCliente.clear();
    }

    @FXML
    private void exportarReporte(ActionEvent event) {
        Mensajes.info("Exportar reporte", "Aquí se generará el reporte de avance para el cliente (PDF).");
    }
}
