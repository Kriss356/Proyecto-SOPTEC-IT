package soptecit.controladores;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.PedidoCliente;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.servicio.PortalClienteService;
import com.mycompany.soptec.it.util.AlertaUtil;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import soptecit.util.Mensajes;
import soptecit.util.SesionCliente;

/**
 * "Mis citas y pedidos": el cliente ve el estado de sus citas, compras y proyectos
 * con palabras sencillas y colores, y puede cancelar lo que todavía no se atiende.
 */
public class ClienteMisServiciosController implements Initializable {

    private static final Locale ES_MX = new Locale("es", "MX");
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(ES_MX);
    private static final DateTimeFormatter FECHA_PEDIDO = DateTimeFormatter.ofPattern("d 'de' MMMM, h:mm a", ES_MX);
    private static final int MAX_FILAS = 10;

    @FXML private Button btnActualizar;
    @FXML private Label lblConteoCitas;
    @FXML private VBox listaCitas;
    @FXML private Label lblConteoPedidos;
    @FXML private VBox listaPedidos;
    @FXML private VBox listaProyectos;

    private final PortalClienteService service = new PortalClienteService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargar();
    }

    @FXML
    private void actualizar(ActionEvent event) {
        cargar();
    }

    private void cargar() {
        Cliente cliente = SesionCliente.getCliente();
        listaCitas.getChildren().clear();
        listaPedidos.getChildren().clear();
        listaProyectos.getChildren().clear();
        if (cliente == null) {
            listaCitas.getChildren().add(vacio("Entra con tu número de teléfono para ver tus citas."));
            return;
        }
        try {
            List<Cita> citas = service.citasDeCliente(cliente.getIdCliente());
            lblConteoCitas.setText(citas.isEmpty() ? "" : "(" + citas.size() + ")");
            if (citas.isEmpty()) {
                listaCitas.getChildren().add(vacio("Todavía no tienes citas. Toca «Agendar otra cita» para pedir una."));
            }
            citas.stream().limit(MAX_FILAS).forEach(c -> listaCitas.getChildren().add(filaCita(c, cliente)));

            List<PedidoCliente> pedidos = service.pedidosDeCliente(cliente.getIdCliente());
            lblConteoPedidos.setText(pedidos.isEmpty() ? "" : "(" + pedidos.size() + ")");
            if (pedidos.isEmpty()) {
                listaPedidos.getChildren().add(vacio("Todavía no has apartado productos."));
            }
            pedidos.stream().limit(MAX_FILAS).forEach(p -> listaPedidos.getChildren().add(filaPedido(p, cliente)));

            List<Proyecto> proyectos = service.proyectosDeCliente(cliente.getIdCliente());
            if (proyectos.isEmpty()) {
                listaProyectos.getChildren().add(vacio("No tienes proyectos registrados por ahora."));
            }
            proyectos.forEach(p -> listaProyectos.getChildren().add(filaProyecto(p)));
        } catch (SoptecException ex) {
            AlertaUtil.errorControlado("Portal cliente", "MIS_SERVICIOS", ex);
        }
    }

    // ------------------------------------------------------------------ filas

    private Node filaCita(Cita c, Cliente cliente) {
        Label titulo = new Label(PortalClienteService.servicioDeCita(c));
        titulo.getStyleClass().add("portal-texto-fuerte");
        Label cuando = new Label(ClienteCitaController.capitalizar(ClienteCitaController.fechaLarga(c.getFechaCita()))
                + " · " + ClienteCitaController.horaTexto(c.getHoraCita()));
        cuando.getStyleClass().add("portal-texto");
        VBox info = new VBox(4, titulo, cuando);

        Label estado = badge(PortalClienteService.estadoCitaParaCliente(c.getEstadoCita()), c.getEstadoCita());
        HBox fila = fila(info, estado);

        String e = c.getEstadoCita() == null ? "" : c.getEstadoCita().toLowerCase();
        boolean futura = c.getFechaCita() != null && c.getHoraCita() != null
                && LocalDateTime.of(c.getFechaCita(), c.getHoraCita()).isAfter(LocalDateTime.now());
        if (futura && (e.startsWith("solicit") || e.startsWith("program") || e.startsWith("confirm"))) {
            Button cancelar = botonCancelar("Cancelar cita");
            cancelar.setOnAction(ev -> {
                if (Mensajes.confirmar("Cancelar cita", "¿Seguro que quieres cancelar tu cita del "
                        + ClienteCitaController.fechaLarga(c.getFechaCita()) + " a las "
                        + ClienteCitaController.horaTexto(c.getHoraCita()) + "?")) {
                    try {
                        service.cancelarCita(c.getIdCita(), cliente.getIdCliente());
                        Mensajes.info("Cita cancelada", "Listo, cancelamos tu cita. Puedes agendar otra cuando quieras.");
                        cargar();
                    } catch (SoptecException ex) {
                        AlertaUtil.errorControlado("Portal cliente", "CANCELAR_CITA", ex);
                    }
                }
            });
            fila.getChildren().add(cancelar);
        }
        return fila;
    }

    private Node filaPedido(PedidoCliente p, Cliente cliente) {
        Label titulo = new Label("Pedido " + p.getFolio()
                + (p.getFechaPedido() != null ? "  ·  " + p.getFechaPedido().format(FECHA_PEDIDO) : ""));
        titulo.getStyleClass().add("portal-texto-fuerte");
        Label productos = new Label(p.getResumenProductos() != null ? p.getResumenProductos() : "");
        productos.getStyleClass().add("portal-texto");
        productos.setWrapText(true);
        productos.setMaxWidth(560);
        VBox info = new VBox(4, titulo, productos);

        Label total = new Label(MONEDA.format(p.getTotal()));
        total.getStyleClass().add("producto-precio");
        total.setStyle("-fx-font-size: 20px;");
        Label estado = badge(PortalClienteService.estadoPedidoParaCliente(p.getEstado()), p.getEstado());
        VBox derecha = new VBox(6, total, estado);
        derecha.setAlignment(Pos.CENTER_RIGHT);
        HBox fila = fila(info, derecha);

        if (p.isCancelable()) {
            Button cancelar = botonCancelar("Cancelar pedido");
            cancelar.setOnAction(ev -> {
                if (Mensajes.confirmar("Cancelar pedido", "¿Seguro que quieres cancelar el pedido " + p.getFolio() + "?")) {
                    try {
                        service.cancelarPedidoDeCliente(p.getIdPedido(), cliente.getIdCliente());
                        Mensajes.info("Pedido cancelado", "Listo, cancelamos tu pedido " + p.getFolio() + ".");
                        cargar();
                    } catch (SoptecException ex) {
                        AlertaUtil.errorControlado("Portal cliente", "CANCELAR_PEDIDO", ex);
                    }
                }
            });
            fila.getChildren().add(cancelar);
        }
        return fila;
    }

    private Node filaProyecto(Proyecto p) {
        Label titulo = new Label(p.getNombreProyecto());
        titulo.getStyleClass().add("portal-texto-fuerte");
        String fechas = (p.getFechaInicio() != null ? "Inició: " + ClienteCitaController.fechaLarga(p.getFechaInicio()) : "")
                + (p.getFechaEstimadaFin() != null ? "   ·   Fecha estimada de entrega: "
                        + ClienteCitaController.fechaLarga(p.getFechaEstimadaFin()) : "");
        Label detalle = new Label(fechas);
        detalle.getStyleClass().add("portal-texto-tenue");
        int avance = InicioController.porcentajePorEstado(p.getEstado());
        ProgressBar barra = new ProgressBar(avance / 100.0);
        barra.setPrefWidth(320);
        barra.getStyleClass().add(avance >= 100 ? "progreso-exito" : "progreso-grande");
        Label porcentaje = new Label(avance + " % de avance");
        porcentaje.getStyleClass().add("portal-texto-tenue");
        HBox avanceBox = new HBox(10, barra, porcentaje);
        avanceBox.setAlignment(Pos.CENTER_LEFT);
        VBox info = new VBox(6, titulo, detalle, avanceBox);
        String estadoTxt = p.getEstado() == null ? "Planeado" : p.getEstado();
        return fila(info, badge(estadoTxt, estadoTxt));
    }

    // ------------------------------------------------------------------ utilidades

    private HBox fila(Node izquierda, Node derecha) {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox fila = new HBox(16, izquierda, espacio, derecha);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("servicio-fila");
        return fila;
    }

    private Label badge(String texto, String estadoOriginal) {
        Label b = new Label(texto);
        b.getStyleClass().addAll("badge", "badge-grande", PortalClienteService.claseBadge(estadoOriginal));
        return b;
    }

    private Button botonCancelar(String texto) {
        Button b = new Button(texto);
        b.getStyleClass().addAll("boton-peligro", "portal-boton-medio");
        return b;
    }

    private Label vacio(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("portal-texto-tenue");
        l.setWrapText(true);
        return l;
    }

    @FXML
    private void nuevaCita(ActionEvent event) {
        PortalClienteController.irA(PortalClienteController.VISTA_CITA);
    }

    @FXML
    private void irTienda(ActionEvent event) {
        PortalClienteController.irA(PortalClienteController.VISTA_TIENDA);
    }
}
