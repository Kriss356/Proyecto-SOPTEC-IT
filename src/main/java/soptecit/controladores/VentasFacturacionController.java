package soptecit.controladores;

import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.DetalleServicio;
import com.mycompany.soptec.it.modelo.Pago;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.modelo.Servicio;
import com.mycompany.soptec.it.modelo.Venta;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.servicio.ServicioService;
import com.mycompany.soptec.it.servicio.VentaService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.ComboBuscadorUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import soptecit.util.Edicion;
import soptecit.util.EditorDetalle;
import soptecit.util.Tablas;

/**
 * Ventas y facturación conectadas a MySQL.
 * <p>El formulario permite elegir cliente, proyecto y cita sin volver a capturar sus datos, y armar la
 * lista de servicios vendidos (relación muchos a muchos venta_servicio). Venta, servicios y pago
 * inicial se guardan en una sola transacción.</p>
 */
public class VentasFacturacionController implements Initializable {

    private static final String MODULO = "Ventas";
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VentaService ventaService = new VentaService();
    private final ClienteService clienteService = new ClienteService();
    private final ProyectoService proyectoService = new ProyectoService();
    private final CitaService citaService = new CitaService();
    private final ServicioService servicioService = new ServicioService();

    // Historial
    // Botones del formulario de tblVentas (modo nuevo / editando)
    @FXML private Button btnRegistrarVenta;
    @FXML private Button btnActualizarVenta;
    @FXML private CheckBox chkCanceladas;
    @FXML private TextField txtBuscarVenta;
    @FXML private ComboBox<String> cmbFiltroEstadoVenta;
    @FXML private TableView<Venta> tblVentas;
    @FXML private TableColumn<Venta, String> colVenFolio;
    @FXML private TableColumn<Venta, String> colVenCliente;
    @FXML private TableColumn<Venta, String> colVenProyecto;
    @FXML private TableColumn<Venta, String> colVenFecha;
    @FXML private TableColumn<Venta, String> colVenTotal;
    @FXML private TableColumn<Venta, String> colVenSaldo;
    @FXML private TableColumn<Venta, String> colVenEstado;
    @FXML private Label lblResumenVentas;

    // Formulario
    @FXML private Label lblModoVenta;
    @FXML private TextField txtFiltroClienteVenta;
    @FXML private ComboBox<Cliente> cmbClienteVenta;
    @FXML private Label lblInfoClienteVenta;
    @FXML private ComboBox<Proyecto> cmbProyectoVenta;
    @FXML private ComboBox<Cita> cmbCitaVenta;
    @FXML private DatePicker dpFechaVenta;
    @FXML private DatePicker dpVencimientoVenta;
    @FXML private ComboBox<Servicio> cmbServicioVenta;
    @FXML private Spinner<Integer> spnCantidadVenta;
    @FXML private TableView<DetalleServicio> tblServiciosVenta;
    @FXML private Label lblRenglonesVenta;
    @FXML private Label lblSubtotalVenta;
    @FXML private Label lblIvaVenta;
    @FXML private Label lblTotalVenta;
    @FXML private ComboBox<String> cmbMetodoPagoVenta;
    @FXML private Label lblPagoInicial;
    @FXML private TextField txtPagoInicial;
    @FXML private TextField txtObservacionesVenta;

    private EditorDetalle<Servicio, DetalleServicio> editorServicios;
    private Venta ventaSeleccionada;
    private Integer idCitaOrigen;
    private List<Proyecto> todosLosProyectos = new ArrayList<>();
    private List<Cita> todasLasCitas = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaVenta.setValue(LocalDate.now());
        dpVencimientoVenta.setValue(LocalDate.now().plusDays(30));
        cmbFiltroEstadoVenta.setValue("Todas");
        cmbFiltroEstadoVenta.valueProperty().addListener((o, a, n) -> cargarVentas(null));

        colVenFolio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFolio()));
        colVenCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClienteNombre()));
        colVenProyecto.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProyectoNombre() != null ? d.getValue().getProyectoNombre() : "—"));
        colVenFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaVenta() != null ? d.getValue().getFechaVenta().format(FECHA) : ""));
        colVenTotal.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getTotal())));
        colVenSaldo.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getSaldo())));
        colVenEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        colVenEstado.setCellFactory(c -> Tablas.crearCeldaEstado());
        tblVentas.setPlaceholder(new Label("No hay ventas registradas"));
        tblVentas.getSelectionModel().selectedItemProperty().addListener((o, a, v) -> {
            if (v != null) {
                cargarEnFormulario(v);
            }
        });
        // Edición rápida: lápiz en cada fila
        Edicion.agregarBotonEditar(tblVentas, this::cargarEnFormulario);
        Edicion.modo(false, lblModoVenta, btnRegistrarVenta, btnActualizarVenta);
        tblVentas.setRowFactory(t -> {
            TableRow<Venta> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    DetalleFacturaController.abrir(aFactura(fila.getItem()), tblVentas.getScene().getWindow());
                }
            });
            return fila;
        });

        editorServicios = new EditorDetalle<>(cmbServicioVenta, spnCantidadVenta, tblServiciosVenta, lblRenglonesVenta,
                "Servicios: ", "Agrega los servicios vendidos",
                EditorDetalle.adaptador(DetalleServicio::de, Servicio::getIdServicio, DetalleServicio::getIdServicio,
                        DetalleServicio::getNombreServicio, DetalleServicio::getCantidad, DetalleServicio::setCantidad,
                        DetalleServicio::getPrecioUnitario, DetalleServicio::getImporte));
        editorServicios.setAlCambiar(this::actualizarTotales);
        editorServicios.setTextoCatalogoVacio("No hay servicios en el catálogo. Agrégalos en Ventas › Catálogo de servicios.");
        // Cada vez que se abre la lista se vuelve a leer el catálogo (así aparecen los servicios nuevos)
        editorServicios.setFuenteCatalogo(() -> servicioService.listar(false));

        cmbClienteVenta.valueProperty().addListener((o, a, cli) -> alCambiarCliente(cli));
        // Autorrelleno: al elegir la cita (venta nueva) se copian solos sus servicios
        cmbCitaVenta.valueProperty().addListener((o, a, cita) -> autollenarServiciosDeCita(cita));
        configurarConvertidores();
        refrescarTodo(null);
    }

    private void configurarConvertidores() {
        cmbCitaVenta.setConverter(new StringConverter<Cita>() {
            @Override
            public String toString(Cita c) {
                return c == null ? "" : "Cita #" + c.getIdCita() + " · " + c.getFechaCita() + " · " + c.getEstadoCita();
            }

            @Override
            public Cita fromString(String s) {
                return null;
            }
        });
    }

    // =====================================================================
    // Carga de datos
    // =====================================================================
    @FXML
    private void refrescarTodo(ActionEvent event) {
        try {
            ComboBuscadorUtil.configurar(cmbClienteVenta, txtFiltroClienteVenta, clienteService.listar(false), Cliente::toString);
            editorServicios.setCatalogo(servicioService.listar(false));
            todosLosProyectos = proyectoService.listar(false);
            todasLasCitas = citaService.listar(false);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_CATALOGOS_VENTA", ex);
        }
        alCambiarCliente(cmbClienteVenta.getValue());
        cargarVentas(null);
    }

    @FXML
    private void cargarVentas(ActionEvent event) {
        try {
            List<Venta> ventas = ventaService.buscar(txtBuscarVenta.getText(), chkCanceladas.isSelected());
            String estado = cmbFiltroEstadoVenta.getValue();
            if (estado != null && !"Todas".equals(estado)) {
                ventas = ventas.stream().filter(v -> estado.equalsIgnoreCase(v.getEstado())).collect(Collectors.toList());
            }
            tblVentas.setItems(FXCollections.observableArrayList(ventas));
            BigDecimal total = BigDecimal.ZERO;
            for (Venta v : ventas) {
                if (!v.isCancelada()) {
                    total = total.add(v.getTotal());
                }
            }
            lblResumenVentas.setText(ventas.size() + (ventas.size() == 1 ? " venta" : " ventas")
                    + "  ·  Vendido " + MONEDA.format(total)
                    + "  ·  Por cobrar " + MONEDA.format(VentaService.saldoTotal(ventas)));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_VENTAS", ex);
        }
    }

    private void alCambiarCliente(Cliente cli) {
        Integer proyectoActual = cmbProyectoVenta.getValue() != null ? cmbProyectoVenta.getValue().getIdProyecto() : null;
        if (cli == null) {
            cmbProyectoVenta.setItems(FXCollections.observableArrayList());
            cmbCitaVenta.setItems(FXCollections.observableArrayList());
            lblInfoClienteVenta.setText("Selecciona el cliente: se cargan sus proyectos y citas.");
            return;
        }
        List<Proyecto> proyectos = new ArrayList<>();
        proyectos.add(null);
        proyectos.addAll(todosLosProyectos.stream().filter(p -> p.getIdCliente() == cli.getIdCliente())
                .collect(Collectors.toList()));
        cmbProyectoVenta.setItems(FXCollections.observableArrayList(proyectos));
        if (proyectoActual != null) {
            ComboBuscadorUtil.seleccionarPorPredicado(cmbProyectoVenta, p -> p != null && p.getIdProyecto() == proyectoActual);
        }
        List<Cita> citas = todasLasCitas.stream()
                .filter(c -> c.getIdCliente() == cli.getIdCliente() && !"Cancelada".equalsIgnoreCase(c.getEstadoCita()))
                .collect(Collectors.toList());
        cmbCitaVenta.setItems(FXCollections.observableArrayList(citas));
        lblInfoClienteVenta.setText("RFC: " + (cli.getRfc() != null ? cli.getRfc() : "—")
                + "  ·  " + (proyectos.size() - 1) + " proyecto(s)  ·  " + citas.size() + " cita(s)");
        // Venta nueva: si el cliente solo tiene un proyecto o una cita, se eligen solos
        if (ventaSeleccionada == null) {
            if (proyectoActual == null && proyectos.size() == 2) {
                cmbProyectoVenta.setValue(proyectos.get(1));
            }
            if (citas.size() == 1) {
                cmbCitaVenta.setValue(citas.get(0));
            }
        }
    }

    /** En una venta nueva, al elegir la cita se copian sus servicios si aún no hay ninguno capturado. */
    private void autollenarServiciosDeCita(Cita cita) {
        if (cita == null || ventaSeleccionada != null || !editorServicios.getRenglones().isEmpty()) {
            return;
        }
        try {
            List<DetalleServicio> servicios = citaService.listarServicios(cita.getIdCita());
            if (!servicios.isEmpty()) {
                editorServicios.setRenglones(servicios);
                idCitaOrigen = cita.getIdCita();
                lblInfoClienteVenta.setText("Se copiaron " + servicios.size() + " servicio(s) de la cita #"
                        + cita.getIdCita() + ". Puedes ajustar las cantidades.");
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_SERVICIOS_CITA", ex);
        }
    }

    // =====================================================================
    // Servicios de la venta
    // =====================================================================
    @FXML
    private void agregarServicio(ActionEvent event) {
        String aviso = editorServicios.agregarSeleccion();
        if (aviso != null) {
            AlertaUtil.advertencia("Agregar servicio", aviso);
        }
    }

    @FXML
    private void quitarServicio(ActionEvent event) {
        String aviso = editorServicios.quitarSeleccion();
        if (aviso != null) {
            AlertaUtil.advertencia("Quitar servicio", aviso);
        }
    }

    @FXML
    private void cargarServiciosDeCita(ActionEvent event) {
        Cita cita = cmbCitaVenta.getValue();
        if (cita == null) {
            AlertaUtil.advertencia("Elige una cita", "Selecciona primero una de las citas del cliente.");
            return;
        }
        try {
            List<DetalleServicio> servicios = citaService.listarServicios(cita.getIdCita());
            if (servicios.isEmpty()) {
                AlertaUtil.advertencia("Cita sin servicios", "La cita #" + cita.getIdCita()
                        + " no tiene servicios registrados. Agrégalos desde la lista de servicios.");
                return;
            }
            editorServicios.setRenglones(servicios);
            idCitaOrigen = cita.getIdCita();
            AlertaUtil.info("Servicios cargados", "Se copiaron " + servicios.size()
                    + " servicio(s) de la cita #" + cita.getIdCita() + ". Puedes ajustar las cantidades antes de guardar.");
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_SERVICIOS_CITA", ex);
        }
    }

    private void actualizarTotales() {
        BigDecimal subtotal = editorServicios.getTotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = subtotal.multiply(Venta.TASA_IVA).setScale(2, RoundingMode.HALF_UP);
        lblSubtotalVenta.setText(MONEDA.format(subtotal));
        lblIvaVenta.setText(MONEDA.format(iva));
        lblTotalVenta.setText(MONEDA.format(subtotal.add(iva)));
    }

    // =====================================================================
    // CRUD
    // =====================================================================
    @FXML
    private void registrarVenta(ActionEvent event) {
        try {
            Venta v = construirDesdeFormulario(new Venta(), "REGISTRAR_VENTA");
            Pago pagoInicial = leerPagoInicial("REGISTRAR_VENTA");
            Venta creada = ventaService.registrar(v, pagoInicial);
            Venta leida = ventaService.consultarPorId(creada.getIdVenta());
            limpiarVenta(null);
            refrescarTodo(null);
            if (AlertaUtil.confirmar("Venta " + creada.getFolio() + " registrada",
                    "Total: " + MONEDA.format(leida.getTotal()) + "  ·  Pagado: " + MONEDA.format(leida.getPagado())
                    + "  ·  Estado: " + leida.getEstado() + "\n\n¿Deseas ver la factura ahora?")) {
                DetalleFacturaController.abrir(aFactura(leida), tblVentas.getScene().getWindow());
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "REGISTRAR_VENTA", ex);
        }
    }

    @FXML
    private void actualizarVenta(ActionEvent event) {
        if (ventaSeleccionada == null) {
            AlertaUtil.advertencia("Selecciona una venta", "Elige una venta del historial para modificarla.");
            return;
        }
        try {
            Venta v = construirDesdeFormulario(ventaSeleccionada, "ACTUALIZAR_VENTA");
            ventaService.actualizar(v);
            AlertaUtil.info("Venta actualizada", "Se actualizaron la venta " + v.getFolio()
                    + " y sus servicios. Los pagos se registran en Administración › Cuentas por cobrar.");
            limpiarVenta(null);
            cargarVentas(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ACTUALIZAR_VENTA", ex);
        }
    }

    @FXML
    private void cancelarVenta(ActionEvent event) {
        Venta v = tblVentas.getSelectionModel().getSelectedItem();
        if (v == null) {
            AlertaUtil.advertencia("Selecciona una venta", "Elige la venta que deseas cancelar.");
            return;
        }
        if (!AlertaUtil.confirmar("Cancelar venta", "¿Marcar como Cancelada la venta " + v.getFolio() + "?\n"
                + "Se conserva en el historial (baja lógica).")) {
            return;
        }
        try {
            ventaService.cancelar(v.getIdVenta());
            AlertaUtil.info("Venta cancelada", "La venta " + v.getFolio() + " quedó cancelada.");
            limpiarVenta(null);
            cargarVentas(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CANCELAR_VENTA", ex);
        }
    }

    @FXML
    private void eliminarVenta(ActionEvent event) {
        Venta v = tblVentas.getSelectionModel().getSelectedItem();
        if (v == null) {
            AlertaUtil.advertencia("Selecciona una venta", "Elige la venta que deseas eliminar.");
            return;
        }
        if (!AlertaUtil.confirmar("Eliminar venta definitivamente",
                "Se borrará la venta " + v.getFolio() + " junto con sus servicios. ¿Continuar?")) {
            return;
        }
        try {
            ventaService.eliminarFisico(v.getIdVenta());
            AlertaUtil.info("Venta eliminada", "La venta y sus servicios se eliminaron de la base de datos.");
            limpiarVenta(null);
            cargarVentas(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ELIMINAR_FISICO_VENTA", ex);
        }
    }

    @FXML
    private void verFactura(ActionEvent event) {
        Venta v = tblVentas.getSelectionModel().getSelectedItem();
        if (v == null) {
            AlertaUtil.advertencia("Selecciona una venta", "Elige una venta del historial para ver su factura.");
            return;
        }
        DetalleFacturaController.abrir(aFactura(v), tblVentas.getScene().getWindow());
    }

    @FXML
    private void limpiarVenta(ActionEvent event) {
        ventaSeleccionada = null;
        idCitaOrigen = null;
        lblModoVenta.setText("Nueva venta");
        txtFiltroClienteVenta.clear();
        cmbClienteVenta.setValue(null);
        cmbProyectoVenta.setValue(null);
        cmbCitaVenta.setValue(null);
        dpFechaVenta.setValue(LocalDate.now());
        dpVencimientoVenta.setValue(LocalDate.now().plusDays(30));
        editorServicios.limpiar();
        cmbMetodoPagoVenta.setValue(null);
        txtPagoInicial.clear();
        txtPagoInicial.setDisable(false);
        lblPagoInicial.setText("Pago inicial (opcional)");
        txtObservacionesVenta.clear();
        tblVentas.getSelectionModel().clearSelection();
        Edicion.modo(false, lblModoVenta, btnRegistrarVenta, btnActualizarVenta);
    }

    private void cargarEnFormulario(Venta v) {
        ventaSeleccionada = v;
        idCitaOrigen = v.getIdCita();
        lblModoVenta.setText("Editando venta " + v.getFolio() + " · " + v.getEstado());
        txtFiltroClienteVenta.clear();
        ComboBuscadorUtil.seleccionarPorPredicado(cmbClienteVenta, c -> c.getIdCliente() == v.getIdCliente());
        alCambiarCliente(cmbClienteVenta.getValue());
        if (v.getIdProyecto() != null) {
            ComboBuscadorUtil.seleccionarPorPredicado(cmbProyectoVenta, p -> p != null && p.getIdProyecto() == v.getIdProyecto());
        } else {
            cmbProyectoVenta.setValue(null);
        }
        if (v.getIdCita() != null) {
            ComboBuscadorUtil.seleccionarPorPredicado(cmbCitaVenta, c -> c.getIdCita() == v.getIdCita());
        } else {
            cmbCitaVenta.setValue(null);
        }
        dpFechaVenta.setValue(v.getFechaVenta());
        dpVencimientoVenta.setValue(v.getFechaVencimiento());
        editorServicios.setRenglones(v.getDetalles());
        cmbMetodoPagoVenta.setValue(v.getMetodoPago());
        txtPagoInicial.clear();
        txtPagoInicial.setDisable(true);
        lblPagoInicial.setText("Pagado: " + MONEDA.format(v.getPagado()));
        txtObservacionesVenta.setText(v.getObservaciones() != null ? v.getObservaciones() : "");
        Edicion.modo(true, lblModoVenta, btnRegistrarVenta, btnActualizarVenta);
    }

    private Venta construirDesdeFormulario(Venta base, String op) throws ValidacionException {
        Cliente cli = cmbClienteVenta.getValue();
        if (cli == null) {
            throw new ValidacionException(MODULO, op, "Selecciona el cliente de la lista desplegable.");
        }
        base.setIdCliente(cli.getIdCliente());
        base.setClienteNombre(cli.getNombreCliente());
        base.setClienteRfc(cli.getRfc());
        Proyecto p = cmbProyectoVenta.getValue();
        base.setIdProyecto(p != null ? p.getIdProyecto() : null);
        Cita cita = cmbCitaVenta.getValue();
        base.setIdCita(cita != null ? cita.getIdCita() : idCitaOrigen);
        base.setFechaVenta(dpFechaVenta.getValue());
        base.setFechaVencimiento(dpVencimientoVenta.getValue());
        base.setDetalles(editorServicios.getRenglones());
        base.setMetodoPago(cmbMetodoPagoVenta.getValue());
        base.setObservaciones(txtObservacionesVenta.getText());
        base.calcularTotales();
        return base;
    }

    private Pago leerPagoInicial(String op) throws ValidacionException {
        String texto = txtPagoInicial.getText();
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            BigDecimal monto = new BigDecimal(texto.replace("$", "").replace(",", "").trim());
            if (monto.signum() < 0) {
                throw new ValidacionException(MODULO, op, "El pago inicial no puede ser negativo.");
            }
            return new Pago(monto, dpFechaVenta.getValue() != null ? dpFechaVenta.getValue() : LocalDate.now(),
                    cmbMetodoPagoVenta.getValue(), null);
        } catch (NumberFormatException ex) {
            throw new ValidacionException(MODULO, op, "Escribe el pago inicial solo con números, por ejemplo 5000.", ex);
        }
    }

    /** Convierte la venta al formato que muestra la ventana de factura imprimible. */
    static String[] aFactura(Venta v) {
        return new String[]{
            v.getFolio(),
            v.getIdProyecto() != null ? "PRY-" + String.format("%04d", v.getIdProyecto()) : "Sin proyecto",
            v.getClienteNombre(),
            v.getFechaVenta() != null ? v.getFechaVenta().format(FECHA) : "",
            MONEDA.format(v.getTotal()),
            v.getEstado(),
            v.getClienteRfc() != null ? v.getClienteRfc() : "",
            v.getFechaVencimiento() != null ? v.getFechaVencimiento().format(FECHA) : "",
            v.getConceptoResumen(),
            MONEDA.format(v.getSubtotal()),
            MONEDA.format(v.getIva()),
            v.getMetodoPago() != null ? v.getMetodoPago() : "Por definir"
        };
    }
}
