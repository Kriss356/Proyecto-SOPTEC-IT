package soptecit.controladores;

import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Pago;
import com.mycompany.soptec.it.modelo.Venta;
import com.mycompany.soptec.it.servicio.VentaService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.Validador;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import soptecit.util.Edicion;
import soptecit.util.Tablas;

/**
 * Cuentas por cobrar (Administración): saldos de las ventas y abonos guardados en la tabla pago.
 * Cada abono y el nuevo estado de la venta se registran en una sola transacción.
 */
public class CuentasCobrarController implements Initializable {

    private static final String MODULO = "CuentasCobrar";
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VentaService ventaService = new VentaService();

    @FXML private Label lblKpiPorCobrar;
    @FXML private Label lblKpiPendientes;
    @FXML private Label lblKpiCobrado;
    @FXML private Label lblKpiPagadas;
    @FXML private CheckBox chkSoloSaldo;
    @FXML private TableView<Venta> tblCuentas;
    @FXML private TableColumn<Venta, String> colCtaFolio;
    @FXML private TableColumn<Venta, String> colCtaCliente;
    @FXML private TableColumn<Venta, String> colCtaProyecto;
    @FXML private TableColumn<Venta, String> colCtaVence;
    @FXML private TableColumn<Venta, String> colCtaTotal;
    @FXML private TableColumn<Venta, String> colCtaPagado;
    @FXML private TableColumn<Venta, String> colCtaSaldo;
    @FXML private TableColumn<Venta, String> colCtaEstado;
    @FXML private Label lblPagosDe;
    @FXML private TableView<Pago> tblPagos;
    @FXML private TableColumn<Pago, String> colPagFecha;
    @FXML private TableColumn<Pago, String> colPagConcepto;
    @FXML private TableColumn<Pago, String> colPagMetodo;
    @FXML private TableColumn<Pago, String> colPagMonto;
    @FXML private TableColumn<Pago, String> colPagEstado;

    @FXML private ComboBox<Venta> cmbVentaPago;
    @FXML private Label lblSaldoVenta;
    @FXML private DatePicker dpFechaPago;
    @FXML private TextField txtMontoPago;
    @FXML private ComboBox<String> cmbMetodoPagoAdm;
    @FXML private TextField txtConceptoPago;
    @FXML private Label lblModoPago;
    @FXML private Label lblChipPago;
    @FXML private Label lblAyudaPago;
    @FXML private Button btnRegistrarPago;
    @FXML private Button btnNuevoPago;

    /** Pago cargado en el formulario para corregirlo; null cuando se registra uno nuevo. */
    private Pago pagoEditando;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaPago.setValue(LocalDate.now());
        colCtaFolio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFolio()));
        colCtaCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClienteNombre()));
        colCtaProyecto.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProyectoNombre() != null ? d.getValue().getProyectoNombre() : "—"));
        colCtaTotal.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getTotal())));
        colCtaPagado.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getPagado())));
        colCtaSaldo.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getSaldo())));
        colCtaVence.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaVencimiento() != null ? d.getValue().getFechaVencimiento().format(FECHA) : "—"));
        // Si ya pasó la fecha límite y todavía debe, se marca como "Vencida" (en rojo)
        colCtaEstado.setCellValueFactory(d -> {
            Venta v = d.getValue();
            boolean vencida = v.getSaldo().signum() > 0 && v.getFechaVencimiento() != null
                    && v.getFechaVencimiento().isBefore(LocalDate.now())
                    && !Venta.CANCELADA.equalsIgnoreCase(v.getEstado());
            return new SimpleStringProperty(vencida ? "Vencida" : v.getEstado());
        });
        colCtaEstado.setCellFactory(c -> Tablas.crearCeldaEstado());
        tblCuentas.setPlaceholder(new Label("No hay ventas por cobrar"));

        colPagFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaPago() != null ? d.getValue().getFechaPago().format(FECHA) : ""));
        colPagConcepto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getConcepto()));
        colPagMetodo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getMetodoPago() != null ? d.getValue().getMetodoPago() : "—"));
        colPagMonto.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getMonto())));
        colPagEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstadoCuenta()));
        colPagEstado.setCellFactory(c -> Tablas.crearCeldaEstado());
        tblPagos.setPlaceholder(new Label("Selecciona una venta para ver sus pagos"));

        tblCuentas.getSelectionModel().selectedItemProperty().addListener((o, a, v) -> {
            if (v != null) {
                cmbVentaPago.getItems().stream().filter(x -> x.getIdVenta() == v.getIdVenta()).findFirst()
                        .ifPresentOrElse(cmbVentaPago::setValue, () -> mostrarVenta(v));
            }
        });
        cmbVentaPago.valueProperty().addListener((o, a, v) -> mostrarVenta(v));
        // Edición rápida de pagos: lápiz, doble clic o Enter
        Edicion.habilitar(tblPagos, this::cargarPagoEnFormulario, pg -> eliminarPago(null));
        modoPago(false);
        cargar(null);
    }

    @FXML
    private void cargar(ActionEvent event) {
        try {
            Integer seleccion = cmbVentaPago.getValue() != null ? cmbVentaPago.getValue().getIdVenta() : null;
            List<Venta> ventas = ventaService.listar(false);
            BigDecimal cobrado = BigDecimal.ZERO;
            for (Venta v : ventas) {
                cobrado = cobrado.add(v.getPagado());
            }
            List<Venta> conSaldo = ventas.stream().filter(v -> v.getSaldo().signum() > 0).collect(Collectors.toList());
            lblKpiPorCobrar.setText(MONEDA.format(VentaService.saldoTotal(ventas)));
            lblKpiPendientes.setText(String.valueOf(conSaldo.size()));
            lblKpiCobrado.setText(MONEDA.format(cobrado));
            lblKpiPagadas.setText(String.valueOf(ventas.stream().filter(v -> Venta.PAGADA.equals(v.getEstado())).count()));

            tblCuentas.setItems(FXCollections.observableArrayList(chkSoloSaldo.isSelected() ? conSaldo : ventas));
            cmbVentaPago.setItems(FXCollections.observableArrayList(conSaldo));
            if (seleccion != null) {
                conSaldo.stream().filter(v -> v.getIdVenta() == seleccion).findFirst().ifPresentOrElse(
                        cmbVentaPago::setValue, () -> ventas.stream().filter(v -> v.getIdVenta() == seleccion)
                                .findFirst().ifPresent(this::mostrarVenta));
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_CUENTAS", ex);
        }
    }

    private void mostrarVenta(Venta v) {
        if (v == null) {
            lblSaldoVenta.setText("$0.00");
            lblPagosDe.setText("PAGOS DE LA VENTA SELECCIONADA");
            tblPagos.setItems(FXCollections.observableArrayList());
            return;
        }
        lblSaldoVenta.setText(MONEDA.format(v.getSaldo()));
        txtMontoPago.setPromptText("Saldo: " + MONEDA.format(v.getSaldo()));
        lblPagosDe.setText("PAGOS DE " + v.getFolio() + " · " + v.getClienteNombre());
        try {
            tblPagos.setItems(FXCollections.observableArrayList(ventaService.listarPagos(v.getIdVenta())));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_PAGOS", ex);
        }
    }

    @FXML
    private void liquidar(ActionEvent event) {
        Venta v = cmbVentaPago.getValue();
        if (v == null) {
            AlertaUtil.advertencia("Selecciona una venta", "Elige la venta que se va a liquidar.");
            return;
        }
        txtMontoPago.setText(v.getSaldo().toPlainString());
    }

    @FXML
    private void registrarPago(ActionEvent event) {
        if (pagoEditando != null) {
            guardarCambiosPago();
            return;
        }
        String op = "REGISTRAR_ABONO";
        try {
            Venta v = cmbVentaPago.getValue();
            if (v == null) {
                throw new ValidacionException(MODULO, op, "Selecciona la venta a la que se aplicará el pago.");
            }
            BigDecimal monto = Validador.parsearDecimalNoNegativo(txtMontoPago.getText(), "Monto", MODULO, op);
            Pago p = new Pago(monto, dpFechaPago.getValue(), cmbMetodoPagoAdm.getValue(), txtConceptoPago.getText());
            Pago guardado = ventaService.registrarAbono(v.getIdVenta(), p);
            AlertaUtil.info("Pago registrado", "Se registró el pago de " + MONEDA.format(guardado.getMonto())
                    + " a la venta " + v.getFolio() + " (" + guardado.getEstadoCuenta() + ").");
            txtMontoPago.clear();
            txtConceptoPago.clear();
            cargar(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, op, ex);
        }
    }

    private void guardarCambiosPago() {
        String op = "ACTUALIZAR_PAGO";
        try {
            BigDecimal monto = Validador.parsearDecimalNoNegativo(txtMontoPago.getText(), "Monto", MODULO, op);
            Pago p = new Pago(monto, dpFechaPago.getValue(), cmbMetodoPagoAdm.getValue(), txtConceptoPago.getText());
            p.setIdPago(pagoEditando.getIdPago());
            Integer idVenta = pagoEditando.getIdVenta();
            ventaService.actualizarPago(p);
            AlertaUtil.info("Pago corregido", "El pago quedó en " + MONEDA.format(p.getMonto())
                    + " y el saldo de la venta se recalculó.");
            nuevoPago(null);
            cargar(null);
            if (idVenta != null) {
                tblCuentas.getItems().stream().filter(x -> x.getIdVenta() == idVenta).findFirst()
                        .ifPresent(x -> tblCuentas.getSelectionModel().select(x));
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, op, ex);
        }
    }

    private void cargarPagoEnFormulario(Pago p) {
        pagoEditando = p;
        dpFechaPago.setValue(p.getFechaPago());
        txtMontoPago.setText(p.getMonto() != null ? p.getMonto().toPlainString() : "");
        cmbMetodoPagoAdm.setValue(p.getMetodoPago());
        txtConceptoPago.setText(p.getConcepto());
        modoPago(true);
        txtMontoPago.requestFocus();
    }

    @FXML
    private void nuevoPago(ActionEvent event) {
        pagoEditando = null;
        dpFechaPago.setValue(LocalDate.now());
        txtMontoPago.clear();
        cmbMetodoPagoAdm.setValue(null);
        txtConceptoPago.clear();
        tblPagos.getSelectionModel().clearSelection();
        modoPago(false);
    }

    private void modoPago(boolean editando) {
        lblModoPago.setText(editando ? "Corregir pago" : "Registrar pago");
        lblChipPago.setText(editando ? "Editando" : "Nuevo");
        lblChipPago.getStyleClass().remove("chip-modo-editando");
        if (editando) {
            lblChipPago.getStyleClass().add("chip-modo-editando");
        }
        lblAyudaPago.setText(editando
                ? "Cambia monto, fecha, método o concepto; el saldo y el estado de la venta se recalculan solos"
                : "El abono y el nuevo estado de la venta se guardan juntos");
        btnRegistrarPago.setText(editando ? "Guardar cambios" : "Registrar pago");
        Edicion.modo(editando, lblModoPago, null, null);
        Edicion.mostrar(btnNuevoPago, editando);
    }

    @FXML
    private void eliminarPago(ActionEvent event) {
        Pago p = pagoEditando != null ? pagoEditando : tblPagos.getSelectionModel().getSelectedItem();
        if (p == null) {
            AlertaUtil.advertencia("Selecciona un pago", "Elige en la tabla de pagos el que deseas eliminar.");
            return;
        }
        if (!AlertaUtil.confirmar("Eliminar pago", "¿Eliminar el pago de " + MONEDA.format(p.getMonto())
                + " del " + (p.getFechaPago() != null ? p.getFechaPago().format(FECHA) : "") + "?\n"
                + "El saldo y el estado de la venta se recalculan.")) {
            return;
        }
        try {
            ventaService.eliminarPago(p.getIdPago());
            AlertaUtil.info("Pago eliminado", "Se eliminó el pago y se recalculó el saldo de la venta.");
            nuevoPago(null);
            cargar(null);
            Venta v = tblCuentas.getItems().stream().filter(x -> p.getIdVenta() != null && x.getIdVenta() == p.getIdVenta())
                    .findFirst().orElse(null);
            if (v != null) {
                tblCuentas.getSelectionModel().select(v);
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ELIMINAR_PAGO", ex);
        }
    }
}
