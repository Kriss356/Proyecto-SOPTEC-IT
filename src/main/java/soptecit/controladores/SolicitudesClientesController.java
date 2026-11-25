package soptecit.controladores;

import com.mycompany.soptec.it.dao.PortalClienteDao;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.PedidoCliente;
import com.mycompany.soptec.it.servicio.PortalClienteService;
import com.mycompany.soptec.it.util.AlertaUtil;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import soptecit.util.Tablas;

/**
 * Módulo para el personal: recibe lo que los clientes piden desde el Portal del cliente.
 * Confirma o rechaza citas solicitadas y controla los pedidos de la tienda.
 */
public class SolicitudesClientesController implements Initializable {

    private static final String MODULO = "Solicitudes clientes";
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label lblCitasPendientes;
    @FXML private Label lblPedidosPendientes;
    @FXML private Label lblPedidosListos;
    @FXML private Label lblMontoApartado;

    @FXML private TableView<Cita> tblCitas;
    @FXML private TableColumn<Cita, String> colCitaId;
    @FXML private TableColumn<Cita, String> colCitaFecha;
    @FXML private TableColumn<Cita, String> colCitaHora;
    @FXML private TableColumn<Cita, String> colCitaCliente;
    @FXML private TableColumn<Cita, String> colCitaServicio;
    @FXML private TableColumn<Cita, String> colCitaDetalle;
    @FXML private Label lblConteoCitas;
    @FXML private Button btnRechazarCita;
    @FXML private Button btnConfirmarCita;

    @FXML private CheckBox chkTodosPedidos;
    @FXML private TableView<PedidoCliente> tblPedidos;
    @FXML private TableColumn<PedidoCliente, String> colPedFolio;
    @FXML private TableColumn<PedidoCliente, String> colPedFecha;
    @FXML private TableColumn<PedidoCliente, String> colPedCliente;
    @FXML private TableColumn<PedidoCliente, String> colPedProductos;
    @FXML private TableColumn<PedidoCliente, String> colPedTotal;
    @FXML private TableColumn<PedidoCliente, String> colPedEstado;
    @FXML private Label lblConteoPedidos;
    @FXML private Button btnCancelarPedido;
    @FXML private Button btnPedidoListo;
    @FXML private Button btnPedidoEntregado;

    private final PortalClienteService service = new PortalClienteService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colCitaId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getIdCita())));
        colCitaFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaCita() != null ? d.getValue().getFechaCita().format(FECHA) : ""));
        colCitaHora.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getHoraCita() != null ? d.getValue().getHoraCita().toString() : ""));
        colCitaCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreCliente()));
        colCitaServicio.setCellValueFactory(d -> new SimpleStringProperty(PortalClienteService.servicioDeCita(d.getValue())));
        colCitaDetalle.setCellValueFactory(d -> new SimpleStringProperty(detalleCita(d.getValue())));

        colPedFolio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFolio()));
        colPedFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaPedido() != null ? d.getValue().getFechaPedido().format(FECHA_HORA) : ""));
        colPedCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreCliente()));
        colPedProductos.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResumenProductos()));
        colPedTotal.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getTotal())));
        colPedEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        colPedEstado.setCellFactory(c -> Tablas.crearCeldaEstado());
        // Textos largos: al pasar el mouse se ve completo.
        colCitaDetalle.setCellFactory(c -> celdaConTooltip());
        colCitaCliente.setCellFactory(c -> celdaConTooltip());
        colPedProductos.setCellFactory(c -> celdaConTooltip());

        tblCitas.setPlaceholder(new Label("No hay citas pendientes de confirmar."));
        tblPedidos.setPlaceholder(new Label("No hay pedidos de clientes."));
        tblCitas.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarBotones());
        tblPedidos.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarBotones());
        refrescar(null);
    }

    private static <T> TableCell<T, String> celdaConTooltip() {
        return new TableCell<T, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTooltip(empty || item == null || item.isBlank() ? null : new Tooltip(item));
            }
        };
    }

    /** Quita el prefijo del portal y el servicio para mostrar solo teléfono, domicilio y nota. */
    static String detalleCita(Cita c) {
        String obs = c.getObservaciones() == null ? "" : c.getObservaciones();
        obs = obs.replace(PortalClienteDao.PREFIJO_PORTAL, "").trim();
        obs = obs.replaceFirst("^Servicio: [^·]+·?\\s*", "");
        return obs.trim();
    }

    @FXML
    private void refrescar(ActionEvent event) {
        try {
            List<Cita> citas = service.citasSolicitadas();
            tblCitas.setItems(FXCollections.observableArrayList(citas));
            lblCitasPendientes.setText(String.valueOf(citas.size()));
            lblConteoCitas.setText(citas.size() == 1 ? "1 cita por confirmar" : citas.size() + " citas por confirmar");

            List<PedidoCliente> pendientes = service.listarPedidos(true);
            List<PedidoCliente> mostrados = chkTodosPedidos.isSelected() ? service.listarPedidos(false) : pendientes;
            tblPedidos.setItems(FXCollections.observableArrayList(mostrados));
            long apartados = pendientes.stream().filter(p -> PedidoCliente.APARTADO.equalsIgnoreCase(p.getEstado())).count();
            long listos = pendientes.size() - apartados;
            BigDecimal porCobrar = pendientes.stream().map(PedidoCliente::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            lblPedidosPendientes.setText(String.valueOf(apartados));
            lblPedidosListos.setText(listos == 1 ? "1 listo para recoger" : listos + " listos para recoger");
            lblMontoApartado.setText(MONEDA.format(porCobrar));
            lblConteoPedidos.setText(mostrados.size() == 1 ? "1 pedido" : mostrados.size() + " pedidos");
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_SOLICITUDES", ex);
        }
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean hayCita = tblCitas.getSelectionModel().getSelectedItem() != null;
        btnConfirmarCita.setDisable(!hayCita);
        btnRechazarCita.setDisable(!hayCita);
        PedidoCliente p = tblPedidos.getSelectionModel().getSelectedItem();
        boolean vigente = p != null && p.isCancelable();
        btnCancelarPedido.setDisable(!vigente);
        btnPedidoListo.setDisable(!(p != null && PedidoCliente.APARTADO.equalsIgnoreCase(p.getEstado())));
        btnPedidoEntregado.setDisable(!vigente);
    }

    // ------------------------------------------------------------------ citas

    @FXML
    private void confirmarCita(ActionEvent event) {
        cambiarCita("Confirmada", "¿Ya hablaste con el cliente? La cita quedará como Confirmada.");
    }

    @FXML
    private void rechazarCita(ActionEvent event) {
        cambiarCita("Cancelada", "La cita se marcará como Cancelada y el horario quedará libre. ¿Continuar?");
    }

    private void cambiarCita(String estado, String pregunta) {
        Cita c = tblCitas.getSelectionModel().getSelectedItem();
        if (c == null) {
            AlertaUtil.advertencia("Selecciona una cita", "Elige primero una cita de la tabla.");
            return;
        }
        if (!AlertaUtil.confirmar("Cita #" + c.getIdCita() + " · " + c.getNombreCliente(), pregunta)) {
            return;
        }
        try {
            service.cambiarEstadoCita(c.getIdCita(), estado);
            AlertaUtil.info("Cita actualizada", "La cita #" + c.getIdCita() + " ahora está " + estado + ".");
            refrescar(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CAMBIAR_ESTADO_CITA", ex);
        }
    }

    // ------------------------------------------------------------------ pedidos

    @FXML
    private void marcarListo(ActionEvent event) {
        cambiarPedido(PedidoCliente.LISTO, "El cliente verá que su pedido está listo para recoger. ¿Continuar?");
    }

    @FXML
    private void marcarEntregado(ActionEvent event) {
        cambiarPedido(PedidoCliente.ENTREGADO, "¿El cliente ya recogió y pagó su pedido?");
    }

    @FXML
    private void cancelarPedido(ActionEvent event) {
        cambiarPedido(PedidoCliente.CANCELADO, "Se cancelará el pedido y las piezas regresarán al almacén. ¿Continuar?");
    }

    private void cambiarPedido(String estado, String pregunta) {
        PedidoCliente p = tblPedidos.getSelectionModel().getSelectedItem();
        if (p == null) {
            AlertaUtil.advertencia("Selecciona un pedido", "Elige primero un pedido de la tabla.");
            return;
        }
        if (!AlertaUtil.confirmar("Pedido " + p.getFolio() + " · " + p.getNombreCliente(), pregunta)) {
            return;
        }
        try {
            if (service.cambiarEstadoPedido(p.getIdPedido(), estado)) {
                AlertaUtil.info("Pedido actualizado", "El pedido " + p.getFolio() + " ahora está: " + estado + ".");
            } else {
                AlertaUtil.advertencia("Sin cambios", "El pedido ya no se puede cambiar (quizá ya se entregó o canceló).");
            }
            refrescar(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CAMBIAR_ESTADO_PEDIDO", ex);
        }
    }
}
