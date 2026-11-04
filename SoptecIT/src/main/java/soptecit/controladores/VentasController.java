package soptecit.controladores;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import soptecit.util.Mensajes;
import soptecit.util.Tablas;

public class VentasController implements Initializable {

    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ---- Clientes ----
    @FXML private TableView<String[]> tblClientes;
    @FXML private TextField txtNombreCliente;
    @FXML private TextField txtEmpresaCliente;
    @FXML private TextField txtRfcCliente;
    @FXML private TextField txtTelefonoCliente;
    @FXML private TextField txtCorreoCliente;
    @FXML private TextField txtDireccionCliente;
    @FXML private ComboBox<String> cmbMedioContacto;
    @FXML private TextArea txtNotasCliente;

    // ---- Propuestas y alcance ----
    @FXML private TableView<String[]> tblPropuestas;
    @FXML private ComboBox<String> cmbClientePropuesta;
    @FXML private ComboBox<String> cmbLevantamiento;
    @FXML private ComboBox<String> cmbServicioPropuesta;
    @FXML private Spinner<Integer> spnPuntosRed;
    @FXML private Spinner<Integer> spnDias;
    @FXML private TextArea txtAlcance;
    @FXML private TextField txtMontoPropuesta;

    // ---- Facturación ----
    @FXML private TableView<String[]> tblFacturas;
    @FXML private TextField txtBuscarFactura;
    @FXML private ComboBox<String> cmbFiltroFactura;
    @FXML private Label lblResumenFacturas;
    @FXML private ComboBox<String> cmbProyectoFactura;
    @FXML private TextField txtClienteFactura;
    @FXML private TextField txtRfcFactura;
    @FXML private DatePicker dpEmision;
    @FXML private DatePicker dpVencimiento;
    @FXML private TextField txtConcepto;
    @FXML private TextField txtImporte;
    @FXML private ComboBox<String> cmbMetodoPago;
    @FXML private Label lblSubtotal;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;

    /** Todas las facturas (la tabla muestra una vista filtrada de esta lista). */
    private ObservableList<String[]> facturas;
    private FilteredList<String[]> facturasFiltradas;
    private int siguienteFolio = 106;

    /** Cliente y RFC de cada proyecto (ejemplo; vendrá de la BD). */
    private final Map<String, String[]> clientePorProyecto = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        spnPuntosRed.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 24));
        spnDias.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 15));

        cmbClientePropuesta.getItems().addAll("Corporativo Avante", "Clínica del Sol", "Oficinas LTJO", "Sucursal Sur");
        cmbLevantamiento.getItems().addAll("LEV-2026-031", "LEV-2026-030", "LEV-2026-028");
        cmbProyectoFactura.getItems().addAll("PRY-2026-014", "PRY-2026-013", "PRY-2026-012");

        dpEmision.setValue(LocalDate.now());
        dpVencimiento.setValue(LocalDate.now().plusDays(30));

        clientePorProyecto.put("PRY-2026-014", new String[]{"Corporativo Avante", "CAV150301AB2"});
        clientePorProyecto.put("PRY-2026-013", new String[]{"Clínica del Sol", "CSO170603RT5"});
        clientePorProyecto.put("PRY-2026-012", new String[]{"Oficinas LTJO", "OLT120815KJ3"});

        // Al elegir proyecto se llenan los datos del cliente
        cmbProyectoFactura.valueProperty().addListener((o, a, n) -> {
            String[] cliente = n == null ? null : clientePorProyecto.get(n);
            txtClienteFactura.setText(cliente == null ? "" : cliente[0]);
            txtRfcFactura.setText(cliente == null ? "" : cliente[1]);
        });
        // Cálculo automático de IVA y total
        txtImporte.textProperty().addListener((o, a, n) -> calcularTotales());

        // ID | Nombre | Empresa | Teléfono | Correo | Proyectos
        Tablas.cargar(tblClientes, new String[][]{
            {"C-001", "Laura Hernández", "Corporativo Avante", "33 1234 5678", "lhernandez@avante.mx", "2"},
            {"C-002", "Roberto Juárez", "Clínica del Sol", "33 2345 6789", "rjuarez@clinicasol.mx", "1"},
            {"C-003", "Jorge Salinas", "Oficinas LTJO", "33 3456 7890", "jsalinas@ltjo.com", "3"},
            {"C-004", "Patricia Ruiz", "Sucursal Sur", "33 4567 8901", "pruiz@sucursalsur.mx", "1"},
            {"C-005", "María Ortega", "Despacho Ortega", "33 5678 9012", "mortega@ortega.mx", "1"}
        });
        // Folio | Cliente | Servicio | Monto | Fecha | Estado
        Tablas.cargar(tblPropuestas, new String[][]{
            {"PRO-2026-018", "Corporativo Avante", "Cableado Cat6", "$86,400", "12/10/2026", "Aprobada"},
            {"PRO-2026-019", "Hotel Real Minerva", "Fibra óptica", "$142,000", "28/10/2026", "En revisión"},
            {"PRO-2026-020", "Clínica del Sol", "Fibra óptica", "$64,800", "30/10/2026", "Borrador"},
            {"PRO-2026-016", "Plaza Patria", "Voz y datos", "$38,500", "02/10/2026", "Rechazada"}
        });
        // Columnas visibles: Folio | Proyecto | Cliente | Emisión | Total | Estado
        // Datos extra para ver la factura: RFC | Vencimiento | Concepto | Subtotal | IVA | Método de pago
        Tablas.cargar(tblFacturas, new String[][]{
            {"F-0105", "PRY-2026-014", "Corporativo Avante", "15/10/2026", "$100,224.00", "Parcial",
                "CAV150301AB2", "14/11/2026", "Instalación de red Cat6 - 24 nodos", "$86,400.00", "$13,824.00", "Transferencia"},
            {"F-0104", "PRY-2026-012", "Oficinas LTJO", "08/10/2026", "$58,000.00", "Pagada",
                "OLT120815KJ3", "07/11/2026", "Red de voz y datos - 16 nodos", "$50,000.00", "$8,000.00", "Transferencia"},
            {"F-0103", "PRY-2026-011", "Sucursal Sur", "01/09/2026", "$44,660.00", "Vencida",
                "SSU180210PQ7", "01/10/2026", "Cableado Cat6A - 12 nodos", "$38,500.00", "$6,160.00", "Cheque"},
            {"F-0102", "PRY-2026-009", "Despacho Ortega", "20/09/2026", "$18,560.00", "Pagada",
                "DOR090522MN4", "20/10/2026", "Mantenimiento de red y certificación", "$16,000.00", "$2,560.00", "Efectivo"}
        });
        configurarHistorialFacturas();
    }

    /** Búsqueda, filtro por estado, resumen y doble clic del historial de facturas. */
    private void configurarHistorialFacturas() {
        facturas = tblFacturas.getItems();
        facturasFiltradas = new FilteredList<>(facturas, f -> true);
        tblFacturas.setItems(facturasFiltradas);

        txtBuscarFactura.textProperty().addListener((o, a, n) -> filtrarFacturas());
        cmbFiltroFactura.valueProperty().addListener((o, a, n) -> filtrarFacturas());
        facturasFiltradas.addListener((javafx.collections.ListChangeListener<String[]>) c -> actualizarResumenFacturas());

        // Doble clic en una fila abre la factura
        tblFacturas.setRowFactory(t -> {
            TableRow<String[]> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    DetalleFacturaController.abrir(fila.getItem(), tblFacturas.getScene().getWindow());
                }
            });
            return fila;
        });
        actualizarResumenFacturas();
    }

    private void filtrarFacturas() {
        String texto = txtBuscarFactura.getText() == null ? "" : txtBuscarFactura.getText().trim().toLowerCase();
        String estado = cmbFiltroFactura.getValue();
        facturasFiltradas.setPredicate(f -> {
            boolean coincideTexto = texto.isEmpty() || f[0].toLowerCase().contains(texto)
                    || f[1].toLowerCase().contains(texto) || f[2].toLowerCase().contains(texto);
            boolean coincideEstado = estado == null || estado.equals("Todas") || f[5].equals(estado);
            return coincideTexto && coincideEstado;
        });
    }

    private void actualizarResumenFacturas() {
        double total = 0;
        for (String[] f : facturasFiltradas) {
            total += numero(f[4]);
        }
        int n = facturasFiltradas.size();
        lblResumenFacturas.setText(n + (n == 1 ? " factura" : " facturas") + "  ·  Total facturado " + MONEDA.format(total));
    }

    private static double numero(String texto) {
        try {
            return Double.parseDouble(texto.replace("$", "").replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @FXML
    private void verFactura(ActionEvent event) {
        String[] factura = tblFacturas.getSelectionModel().getSelectedItem();
        if (factura == null) {
            Mensajes.info("Selecciona una factura", "Elige una factura del historial para verla.");
            return;
        }
        DetalleFacturaController.abrir(factura, tblFacturas.getScene().getWindow());
    }

    private void calcularTotales() {
        double subtotal = numero(txtImporte.getText());
        double iva = subtotal * 0.16;
        lblSubtotal.setText(MONEDA.format(subtotal));
        lblIva.setText(MONEDA.format(iva));
        lblTotal.setText(MONEDA.format(subtotal + iva));
    }

    @FXML
    private void guardarCliente(ActionEvent event) {
        if (txtNombreCliente.getText().trim().isEmpty() || txtTelefonoCliente.getText().trim().isEmpty()) {
            Mensajes.error("Faltan datos", "El nombre y el teléfono del cliente son obligatorios.");
            return;
        }
        // TODO: INSERT INTO clientes (...)
        Mensajes.info("Cliente registrado", "Se guardó a " + txtNombreCliente.getText() + ".");
        limpiarCliente(null);
    }

    @FXML
    private void limpiarCliente(ActionEvent event) {
        txtNombreCliente.clear();
        txtEmpresaCliente.clear();
        txtRfcCliente.clear();
        txtTelefonoCliente.clear();
        txtCorreoCliente.clear();
        txtDireccionCliente.clear();
        cmbMedioContacto.setValue(null);
        txtNotasCliente.clear();
    }

    @FXML
    private void guardarBorrador(ActionEvent event) {
        Mensajes.info("Borrador guardado", "La propuesta quedó como borrador.");
    }

    @FXML
    private void enviarPropuesta(ActionEvent event) {
        if (cmbClientePropuesta.getValue() == null || txtAlcance.getText().trim().isEmpty()) {
            Mensajes.error("Faltan datos", "Selecciona el cliente y describe el alcance del proyecto.");
            return;
        }
        // TODO: INSERT INTO propuestas (..., alcance, estado = 'En revisión')
        Mensajes.info("Propuesta enviada", "Administración recibirá el expediente para su revisión.");
    }

    @FXML
    private void emitirFactura(ActionEvent event) {
        if (cmbProyectoFactura.getValue() == null || txtImporte.getText().trim().isEmpty()) {
            Mensajes.error("Faltan datos", "Selecciona el proyecto e indica el importe.");
            return;
        }
        double subtotal = numero(txtImporte.getText());
        if (subtotal <= 0) {
            Mensajes.error("Importe no válido", "Escribe el importe con números, por ejemplo 86400.");
            return;
        }
        double iva = subtotal * 0.16;
        String[] factura = {
            String.format("F-%04d", siguienteFolio++),
            cmbProyectoFactura.getValue(),
            txtClienteFactura.getText(),
            dpEmision.getValue() == null ? "" : dpEmision.getValue().format(FECHA),
            MONEDA.format(subtotal + iva),
            "Pendiente",
            txtRfcFactura.getText(),
            dpVencimiento.getValue() == null ? "" : dpVencimiento.getValue().format(FECHA),
            txtConcepto.getText().trim().isEmpty() ? "Servicios de cableado estructurado" : txtConcepto.getText().trim(),
            MONEDA.format(subtotal),
            MONEDA.format(iva),
            cmbMetodoPago.getValue() == null ? "Por definir" : cmbMetodoPago.getValue()
        };
        // TODO: INSERT INTO facturas (...)

        // La nueva factura aparece al inicio del historial, seleccionada
        txtBuscarFactura.clear();
        cmbFiltroFactura.setValue(null);
        facturas.add(0, factura);
        tblFacturas.getSelectionModel().select(factura);
        tblFacturas.scrollTo(0);
        limpiarFactura(null);

        if (Mensajes.confirmar("Factura " + factura[0] + " emitida",
                "Total: " + factura[4] + ". Se guardó en el historial y se envió a Administración.\n¿Deseas verla ahora?")) {
            DetalleFacturaController.abrir(factura, tblFacturas.getScene().getWindow());
        }
    }

    @FXML
    private void limpiarFactura(ActionEvent event) {
        cmbProyectoFactura.setValue(null);
        txtClienteFactura.clear();
        txtRfcFactura.clear();
        txtConcepto.clear();
        txtImporte.clear();
        cmbMetodoPago.setValue(null);
    }
}
