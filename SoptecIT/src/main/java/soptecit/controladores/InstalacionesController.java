package soptecit.controladores;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import soptecit.util.Mensajes;
import soptecit.util.Tablas;

public class InstalacionesController implements Initializable {

    @FXML private TableView<String[]> tblInstalaciones;
    @FXML private TableView<String[]> tblHistorial;
    @FXML private Label lblProyectoHistorial;

    @FXML private ComboBox<String> cmbProyectoAvance;
    @FXML private DatePicker dpFechaAvance;
    @FXML private Spinner<Integer> spnHoras;
    @FXML private ComboBox<String> cmbTarea;
    @FXML private ComboBox<String> cmbTecnico;
    @FXML private Slider sldAvance;
    @FXML private Label lblPorcentaje;
    @FXML private ComboBox<String> cmbEstadoAvance;
    @FXML private TextField txtMaterialUsado;
    @FXML private TextArea txtObservacionesAvance;
    @FXML private Label lblEvidencias;
    @FXML private CheckBox chkTerminado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaAvance.setValue(LocalDate.now());
        spnHoras.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 24, 8));
        cmbProyectoAvance.getItems().addAll("PRY-2026-014 · Corporativo Avante", "PRY-2026-012 · Oficinas LTJO",
                "PRY-2026-011 · Sucursal Sur");
        cmbTecnico.getItems().addAll("Carlos Méndez", "Ricardo Ramírez", "Daniel Flores");

        // El porcentaje se muestra mientras se mueve el control
        sldAvance.valueProperty().addListener((o, a, n) -> lblPorcentaje.setText(Math.round(n.doubleValue()) + " %"));
        chkTerminado.selectedProperty().addListener((o, a, terminado) -> {
            if (terminado) {
                sldAvance.setValue(100);
                cmbEstadoAvance.setValue("Terminado");
            }
        });

        // Proyecto | Cliente | Tarea actual | Avance | Entrega | Estado
        Tablas.cargar(tblInstalaciones, new String[][]{
            {"PRY-2026-014", "Corporativo Avante", "Ponchado", "65", "20/11/2026", "En tiempo"},
            {"PRY-2026-012", "Oficinas LTJO", "Certificación", "90", "06/11/2026", "En tiempo"},
            {"PRY-2026-011", "Sucursal Sur", "Canalización", "40", "30/10/2026", "Retrasado"},
            {"PRY-2026-009", "Oficinas LTJO", "Entrega", "100", "30/10/2026", "Terminado"}
        });
        // Fecha | Tarea | Avance | Técnico | Observaciones
        Tablas.cargar(tblHistorial, new String[][]{
            {"02/11/2026", "Ponchado / terminación", "65 %", "C. Méndez", "24 jacks terminados en piso 5"},
            {"31/10/2026", "Tendido de cable", "50 %", "C. Méndez", "Tendido completo, falta peinado en rack"},
            {"28/10/2026", "Canalización", "25 %", "D. Flores", "Canaleta instalada en 3 áreas"},
            {"26/10/2026", "Inicio de obra", "5 %", "C. Méndez", "Recepción de material en sitio"}
        });

        tblInstalaciones.getSelectionModel().selectedItemProperty().addListener((o, a, fila) -> {
            if (fila != null) {
                lblProyectoHistorial.setText(fila[0] + " · " + fila[1]);
                sldAvance.setValue(Double.parseDouble(fila[3]));
            }
        });
    }

    @FXML
    private void adjuntarEvidencia(ActionEvent event) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar fotos de evidencia");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        List<File> fotos = selector.showOpenMultipleDialog(lblEvidencias.getScene().getWindow());
        if (fotos != null && !fotos.isEmpty()) {
            lblEvidencias.setText(fotos.size() + (fotos.size() == 1 ? " foto adjunta" : " fotos adjuntas"));
        }
    }

    @FXML
    private void guardarAvance(ActionEvent event) {
        if (cmbProyectoAvance.getValue() == null || cmbTarea.getEditor().getText().trim().isEmpty()
                || cmbTecnico.getValue() == null) {
            Mensajes.error("Faltan datos", "Selecciona proyecto, tarea realizada y técnico responsable.");
            return;
        }
        int avance = (int) Math.round(sldAvance.getValue());
        // TODO: INSERT INTO avances (proyecto, fecha, tarea, porcentaje, tecnico, horas, observaciones, ...)
        tblHistorial.getItems().add(0, new String[]{dpFechaAvance.getValue().toString(), cmbTarea.getEditor().getText(),
            avance + " %", cmbTecnico.getValue(), txtObservacionesAvance.getText()});
        String mensaje = chkTerminado.isSelected()
                ? "El proyecto se envió a Administración para su revisión final."
                : "Se registró un avance del " + avance + " %.";
        Mensajes.info("Avance guardado", mensaje);
        txtMaterialUsado.clear();
        txtObservacionesAvance.clear();
    }
}
