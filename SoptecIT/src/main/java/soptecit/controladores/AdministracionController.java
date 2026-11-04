package soptecit.controladores;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import soptecit.util.Mensajes;
import soptecit.util.Tablas;

public class AdministracionController implements Initializable {

    // Revisión de propuestas
    @FXML private TableView<String[]> tblExpedientes;
    @FXML private Label lblFolioExp;
    @FXML private Label lblClienteExp;
    @FXML private Label lblServicioExp;
    @FXML private Label lblMontoExp;
    @FXML private TextArea txtComentariosExp;

    // Cuentas por cobrar
    @FXML private TableView<String[]> tblCuentas;
    @FXML private ComboBox<String> cmbFacturaPago;
    @FXML private Label lblSaldoFactura;
    @FXML private DatePicker dpFechaPago;
    @FXML private TextField txtMontoPago;

    // Costos extra
    @FXML private TableView<String[]> tblCostosExtra;
    @FXML private ComboBox<String> cmbProyectoExtra;
    @FXML private ComboBox<String> cmbAutorizaExtra;
    @FXML private TextField txtMontoExtra;

    // Revisión final
    @FXML private TableView<String[]> tblRevisionFinal;
    @FXML private CheckBox chkPruebas;
    @FXML private CheckBox chkCertificacion;
    @FXML private CheckBox chkEtiquetado;
    @FXML private CheckBox chkLimpieza;
    @FXML private CheckBox chkSobrante;
    @FXML private CheckBox chkActa;
    @FXML private CheckBox chkPagos;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaPago.setValue(LocalDate.now());
        cmbFacturaPago.getItems().addAll("F-0105 · Corporativo Avante", "F-0103 · Sucursal Sur", "F-0106 · Clínica del Sol");
        cmbFacturaPago.valueProperty().addListener((o, a, n) -> lblSaldoFactura.setText(n == null ? "$0.00" : "$50,112.00"));
        cmbProyectoExtra.getItems().addAll("PRY-2026-014", "PRY-2026-013", "PRY-2026-012", "PRY-2026-011");
        cmbAutorizaExtra.getItems().addAll("Gerente administrativo", "Dirección general");

        // Al seleccionar un expediente se muestra su detalle
        tblExpedientes.getSelectionModel().selectedItemProperty().addListener((o, a, fila) -> {
            if (fila != null) {
                lblFolioExp.setText(fila[0]);
                lblClienteExp.setText(fila[1]);
                lblServicioExp.setText(fila[2]);
                lblMontoExp.setText(fila[3] + " + IVA");
            }
        });

        // Folio | Cliente | Servicio | Monto | Recibida | Estado
        Tablas.cargar(tblExpedientes, new String[][]{
            {"PRO-2026-019", "Hotel Real Minerva", "Fibra óptica", "$142,000", "28/10/2026", "En revisión"},
            {"PRO-2026-021", "Plaza Galerías", "Cableado Cat6A", "$97,300", "31/10/2026", "En revisión"},
            {"PRO-2026-018", "Corporativo Avante", "Cableado Cat6", "$86,400", "12/10/2026", "Aprobada"},
            {"PRO-2026-016", "Plaza Patria", "Voz y datos", "$38,500", "02/10/2026", "Rechazada"}
        });
        // Factura | Cliente | Vence | Total | Pagado | Saldo | Estado
        Tablas.cargar(tblCuentas, new String[][]{
            {"F-0106", "Clínica del Sol", "30/11/2026", "$75,168", "$0", "$75,168", "Pendiente"},
            {"F-0105", "Corporativo Avante", "14/11/2026", "$100,224", "$50,112", "$50,112", "Parcial"},
            {"F-0104", "Oficinas LTJO", "07/11/2026", "$58,000", "$58,000", "$0", "Pagada"},
            {"F-0103", "Sucursal Sur", "01/10/2026", "$44,660", "$0", "$44,660", "Vencida"},
            {"F-0101", "Hotel Real Minerva", "15/10/2026", "$14,560", "$0", "$14,560", "Vencida"}
        });
        // Folio | Proyecto | Concepto | Monto | Fecha | Estado
        Tablas.cargar(tblCostosExtra, new String[][]{
            {"CE-012", "PRY-2026-014", "4 nodos adicionales en sala de juntas", "$7,200", "24/10/2026", "Autorizado"},
            {"CE-011", "PRY-2026-011", "Trabajo nocturno (horas extra)", "$3,500", "18/10/2026", "Pendiente"},
            {"CE-010", "PRY-2026-012", "Canaleta metálica especial", "$2,150", "10/10/2026", "Pagado"}
        });
        // Proyecto | Cliente | Terminó | Instalador | Estado
        Tablas.cargar(tblRevisionFinal, new String[][]{
            {"PRY-2026-009", "Oficinas LTJO", "30/10/2026", "Equipo Méndez", "En revisión"},
            {"PRY-2026-007", "Despacho Ortega", "25/10/2026", "Equipo Ramírez", "Pendiente"},
            {"PRY-2026-005", "Farmacias Unión", "15/10/2026", "Equipo Méndez", "Cerrado"}
        });
    }

    @FXML
    private void aprobarPropuesta(ActionEvent event) {
        String[] fila = tblExpedientes.getSelectionModel().getSelectedItem();
        if (fila == null) {
            Mensajes.info("Selecciona un expediente", "Elige la propuesta que deseas aprobar.");
            return;
        }
        fila[5] = "Aprobada";
        tblExpedientes.refresh();
        // TODO: UPDATE propuestas + crear proyecto + notificar a Compras
        Mensajes.info("Propuesta aprobada", "Se notificó a Compras-Almacén para surtir el material del proyecto.");
    }

    @FXML
    private void rechazarPropuesta(ActionEvent event) {
        String[] fila = tblExpedientes.getSelectionModel().getSelectedItem();
        if (fila != null && Mensajes.confirmar("Rechazar propuesta", "Se regresará a Ventas con tus comentarios.")) {
            fila[5] = "Rechazada";
            tblExpedientes.refresh();
        }
    }

    @FXML
    private void registrarPago(ActionEvent event) {
        if (cmbFacturaPago.getValue() == null || txtMontoPago.getText().trim().isEmpty()) {
            Mensajes.error("Faltan datos", "Selecciona la factura e indica el monto del pago.");
            return;
        }
        // TODO: INSERT INTO pagos (...)
        Mensajes.info("Pago registrado", "Se actualizó el estado de cuenta de la factura.");
        txtMontoPago.clear();
    }

    @FXML
    private void guardarCostoExtra(ActionEvent event) {
        if (cmbProyectoExtra.getValue() == null || txtMontoExtra.getText().trim().isEmpty()) {
            Mensajes.error("Faltan datos", "Selecciona el proyecto e indica el monto.");
            return;
        }
        Mensajes.info("Costo extra registrado", "Se agregó el cargo al expediente del proyecto.");
    }

    @FXML
    private void cerrarProyecto(ActionEvent event) {
        CheckBox[] lista = {chkPruebas, chkCertificacion, chkEtiquetado, chkLimpieza, chkSobrante, chkActa, chkPagos};
        for (CheckBox c : lista) {
            if (!c.isSelected()) {
                Mensajes.error("Revisión incompleta", "Falta: " + c.getText());
                return;
            }
        }
        // TODO: UPDATE proyectos SET estado = 'Cerrado'
        Mensajes.info("Proyecto cerrado", "El proyecto se cerró correctamente.");
    }

    @FXML
    private void regresarAInstalaciones(ActionEvent event) {
        Mensajes.info("Regresado a Instalaciones", "El proyecto vuelve a Instalaciones con las observaciones.");
    }
}
