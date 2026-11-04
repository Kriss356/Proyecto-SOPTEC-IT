package soptecit.controladores;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import soptecit.util.Mensajes;
import soptecit.util.Tablas;

public class LevantamientosController implements Initializable {

    @FXML private DatePicker dpVisitas;
    @FXML private TableView<String[]> tblVisitas;
    @FXML private ComboBox<String> cmbEstadoVisita;
    @FXML private StackPane panelCroquis;
    @FXML private VBox vacioCroquis;
    @FXML private ImageView imgCroquis;
    @FXML private Spinner<Integer> spnNodos;
    @FXML private TableView<String[]> tblMaterialesLev;
    @FXML private ComboBox<String> cmbMaterialLev;
    @FXML private TextField txtCantidadLev;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpVisitas.setValue(LocalDate.now());
        spnNodos.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 24));
        cmbMaterialLev.getItems().addAll("Cable UTP Cat6 (bobina 305 m)", "Jack RJ45 Cat6", "Patch panel 24 puertos",
                "Rack 24U", "Canaleta 40x25", "Fibra multimodo OM3");

        // El croquis se ajusta al tamaño del recuadro (útil en pantalla completa)
        panelCroquis.setMinSize(0, 210);
        imgCroquis.fitWidthProperty().bind(panelCroquis.widthProperty().subtract(24));
        imgCroquis.fitHeightProperty().bind(panelCroquis.heightProperty().subtract(24));

        // Hora | Cliente | Tipo | Estado
        Tablas.cargar(tblVisitas, new String[][]{
            {"09:00", "Clínica del Sol", "Fibra", "Completada"},
            {"11:30", "Corporativo Avante", "Cat6", "En sitio"},
            {"15:00", "Oficinas LTJO", "UTP", "Programada"},
            {"17:00", "Sucursal Sur", "Cat6A", "Cancelada"}
        });
        // Material | Cantidad
        Tablas.cargar(tblMaterialesLev, new String[][]{
            {"Cable UTP Cat6 (bobina 305 m)", "4"},
            {"Jack RJ45 Cat6", "48"},
            {"Patch panel 24 puertos", "1"}
        });
    }

    @FXML
    private void cargarCroquis(ActionEvent event) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar croquis");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File archivo = selector.showOpenDialog(panelCroquis.getScene().getWindow());
        if (archivo != null) {
            imgCroquis.setImage(new Image(archivo.toURI().toString()));
            vacioCroquis.setVisible(false);
            // TODO: guardar la ruta o el archivo del croquis en la BD
        }
    }

    @FXML
    private void quitarCroquis(ActionEvent event) {
        imgCroquis.setImage(null);
        vacioCroquis.setVisible(true);
    }

    @FXML
    private void agregarMaterial(ActionEvent event) {
        String material = cmbMaterialLev.getEditor().getText().trim();
        String cantidad = txtCantidadLev.getText().trim();
        if (material.isEmpty() || cantidad.isEmpty()) {
            return;
        }
        tblMaterialesLev.getItems().add(new String[]{material, cantidad});
        cmbMaterialLev.getEditor().clear();
        txtCantidadLev.clear();
    }

    @FXML
    private void actualizarEstado(ActionEvent event) {
        String[] visita = tblVisitas.getSelectionModel().getSelectedItem();
        if (visita != null && cmbEstadoVisita.getValue() != null) {
            visita[3] = cmbEstadoVisita.getValue();
            tblVisitas.refresh();
        }
    }

    @FXML
    private void guardarLevantamiento(ActionEvent event) {
        Mensajes.info("Levantamiento guardado", "Se guardaron los datos técnicos y el croquis.");
    }

    @FXML
    private void generarReporte(ActionEvent event) {
        Mensajes.info("Reporte técnico", "Aquí se generará el reporte técnico del levantamiento (PDF).");
    }

    @FXML
    private void enviarAVentas(ActionEvent event) {
        Mensajes.info("Enviado a Ventas", "Ventas ya puede preparar la propuesta con este levantamiento.");
    }
}
