package soptecit.controladores;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.servicio.PortalClienteService;
import com.mycompany.soptec.it.util.AlertaUtil;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import soptecit.util.Mensajes;
import soptecit.util.SesionCliente;

/**
 * Tienda del Portal del cliente: muestra el material del almacén con existencias,
 * el cliente elige cantidades con botones + / − y aparta su pedido (paga al recoger).
 */
public class ClienteTiendaController implements Initializable {

    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private static final String ICONO_CAJA =
            "M3.5 7.5 L12 3.5 L20.5 7.5 V16.5 L12 20.5 L3.5 16.5 Z M3.5 7.5 L12 11.5 L20.5 7.5 M12 11.5 V20.5";
    private static final int POCAS_PIEZAS = 5;

    @FXML private TextField txtBuscar;
    @FXML private Button btnVerTodo;
    @FXML private Label lblResultados;
    @FXML private Label lblSinProductos;
    @FXML private FlowPane flowProductos;
    @FXML private Label lblPiezas;
    @FXML private VBox listaCarrito;
    @FXML private Label lblTotal;
    @FXML private Button btnApartar;
    @FXML private Button btnVaciar;

    private final PortalClienteService service = new PortalClienteService();
    /** id_material → material (con las existencias actuales). */
    private final Map<Integer, Material> materiales = new LinkedHashMap<>();
    /** id_material → cantidad en el pedido. */
    private final Map<Integer, Integer> carrito = new LinkedHashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        txtBuscar.textProperty().addListener((o, a, n) -> pintarCatalogo());
        cargarMateriales();
        pintarCarrito();
    }

    // ------------------------------------------------------------------ catálogo

    private void cargarMateriales() {
        materiales.clear();
        try {
            for (Material m : service.materialesDisponibles()) {
                materiales.put(m.getIdMaterial(), m);
            }
        } catch (SoptecException ex) {
            AlertaUtil.errorControlado("Portal cliente", "CARGAR_TIENDA", ex);
        }
        // Si algo del pedido ya no hay o hay menos, se ajusta.
        carrito.entrySet().removeIf(e -> !materiales.containsKey(e.getKey()));
        carrito.replaceAll((id, cant) -> Math.min(cant, materiales.get(id).getExistencias()));
        carrito.values().removeIf(cant -> cant <= 0);
        pintarCatalogo();
    }

    private void pintarCatalogo() {
        flowProductos.getChildren().clear();
        String filtro = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        int mostrados = 0;
        for (Material m : materiales.values()) {
            String nombre = m.getDescripcionMaterial() == null ? "" : m.getDescripcionMaterial();
            if (!filtro.isEmpty() && !nombre.toLowerCase().contains(filtro)) {
                continue;
            }
            flowProductos.getChildren().add(crearTarjeta(m));
            mostrados++;
        }
        lblResultados.setText(mostrados == 1 ? "1 producto disponible" : mostrados + " productos disponibles");
        boolean vacio = mostrados == 0;
        lblSinProductos.setText(materiales.isEmpty()
                ? "Por ahora no tenemos productos disponibles en la tienda. Llámanos al "
                  + PortalClienteController.TELEFONO_AYUDA + " y te ayudamos."
                : "No encontramos productos con esa palabra. Prueba con otra o toca «Ver todo».");
        lblSinProductos.setVisible(vacio);
        lblSinProductos.setManaged(vacio);
    }

    private VBox crearTarjeta(Material m) {
        int disponibles = disponiblesPara(m.getIdMaterial());

        StackPane icono = new StackPane(new Circle(26), new SVGPath());
        icono.getStyleClass().add("icono-azul");
        icono.getChildren().get(0).getStyleClass().add("portal-icono-fondo");
        ((SVGPath) icono.getChildren().get(1)).setContent(ICONO_CAJA);
        icono.getChildren().get(1).getStyleClass().add("portal-icono");
        icono.setMaxSize(52, 52);
        icono.setMinSize(52, 52);

        Label nombre = new Label(m.getDescripcionMaterial());
        nombre.getStyleClass().add("producto-nombre");
        nombre.setWrapText(true);
        nombre.setMinHeight(48);

        Label precio = new Label(MONEDA.format(m.getPrecioCosto()));
        precio.getStyleClass().add("producto-precio");

        Label stock = new Label(m.getExistencias() <= POCAS_PIEZAS
                ? "¡Solo quedan " + m.getExistencias() + "!"
                : "Hay " + m.getExistencias() + " disponibles");
        stock.getStyleClass().add(m.getExistencias() <= POCAS_PIEZAS ? "producto-stock-bajo" : "producto-stock");

        Label cantidad = new Label("1");
        cantidad.getStyleClass().add("contador-valor");
        Button menos = botonContador("−", "Quitar uno");
        Button mas = botonContador("+", "Agregar uno más");
        menos.setOnAction(e -> cambiarContador(cantidad, -1, disponiblesPara(m.getIdMaterial())));
        mas.setOnAction(e -> cambiarContador(cantidad, +1, disponiblesPara(m.getIdMaterial())));
        HBox contador = new HBox(8, menos, cantidad, mas);
        contador.setAlignment(Pos.CENTER_LEFT);

        Button agregar = new Button(carrito.containsKey(m.getIdMaterial()) ? "Agregar más" : "Agregar");
        agregar.getStyleClass().addAll("boton-primario", "portal-boton-medio");
        agregar.setMaxWidth(Double.MAX_VALUE);
        agregar.setOnAction(e -> agregarAlCarrito(m, Integer.parseInt(cantidad.getText())));

        if (disponibles <= 0) {
            cantidad.setText("0");
            menos.setDisable(true);
            mas.setDisable(true);
            agregar.setDisable(true);
            agregar.setText("Ya está todo en tu pedido");
        }

        VBox tarjeta = new VBox(10, icono, nombre, precio, stock, contador, agregar);
        tarjeta.getStyleClass().add("producto-tarjeta");
        tarjeta.setPrefWidth(236);
        tarjeta.setMaxWidth(236);
        return tarjeta;
    }

    private Button botonContador(String texto, String ayuda) {
        Button b = new Button(texto);
        b.getStyleClass().add("contador-boton");
        b.setTooltip(new Tooltip(ayuda));
        b.setFocusTraversable(true);
        return b;
    }

    private void cambiarContador(Label etiqueta, int delta, int maximo) {
        int valor = Integer.parseInt(etiqueta.getText()) + delta;
        etiqueta.setText(String.valueOf(Math.max(1, Math.min(valor, Math.max(1, maximo)))));
    }

    private int disponiblesPara(int idMaterial) {
        Material m = materiales.get(idMaterial);
        return m == null ? 0 : m.getExistencias() - carrito.getOrDefault(idMaterial, 0);
    }

    @FXML
    private void verTodo(ActionEvent event) {
        txtBuscar.clear();
        cargarMateriales();
    }

    // ------------------------------------------------------------------ pedido

    private void agregarAlCarrito(Material m, int cantidad) {
        int maximo = disponiblesPara(m.getIdMaterial());
        int agregar = Math.min(cantidad, maximo);
        if (agregar <= 0) {
            return;
        }
        carrito.merge(m.getIdMaterial(), agregar, Integer::sum);
        pintarCatalogo();
        pintarCarrito();
    }

    private void pintarCarrito() {
        listaCarrito.getChildren().clear();
        BigDecimal total = BigDecimal.ZERO;
        int piezas = 0;
        for (Map.Entry<Integer, Integer> item : new ArrayList<>(carrito.entrySet())) {
            Material m = materiales.get(item.getKey());
            if (m == null) {
                continue;
            }
            int cant = item.getValue();
            BigDecimal subtotal = m.getPrecioCosto().multiply(BigDecimal.valueOf(cant));
            total = total.add(subtotal);
            piezas += cant;
            listaCarrito.getChildren().add(crearFilaCarrito(m, cant, subtotal));
        }
        lblTotal.setText(MONEDA.format(total));
        lblPiezas.setText(piezas == 0 ? "Aún no agregas productos. Elige del catálogo de la izquierda."
                : piezas == 1 ? "1 pieza en tu pedido" : piezas + " piezas en tu pedido");
        btnApartar.setDisable(piezas == 0);
        btnVaciar.setDisable(piezas == 0);
    }

    private VBox crearFilaCarrito(Material m, int cantidad, BigDecimal subtotal) {
        Label nombre = new Label(m.getDescripcionMaterial());
        nombre.getStyleClass().add("portal-texto");
        nombre.setWrapText(true);

        Label cant = new Label(String.valueOf(cantidad));
        cant.getStyleClass().add("contador-valor");
        Button menos = botonContador("−", "Quitar uno");
        Button mas = botonContador("+", "Agregar uno más");
        menos.setOnAction(e -> {
            if (cantidad <= 1) {
                carrito.remove(m.getIdMaterial());
            } else {
                carrito.put(m.getIdMaterial(), cantidad - 1);
            }
            pintarCatalogo();
            pintarCarrito();
        });
        mas.setDisable(cantidad >= m.getExistencias());
        mas.setOnAction(e -> {
            carrito.put(m.getIdMaterial(), Math.min(cantidad + 1, m.getExistencias()));
            pintarCatalogo();
            pintarCarrito();
        });
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label sub = new Label(MONEDA.format(subtotal));
        sub.getStyleClass().add("portal-texto-fuerte");
        HBox controles = new HBox(8, menos, cant, mas, espacio, sub);
        controles.setAlignment(Pos.CENTER_LEFT);

        Button quitar = new Button("Quitar de mi pedido");
        quitar.getStyleClass().add("boton-quitar");
        quitar.setOnAction(e -> {
            carrito.remove(m.getIdMaterial());
            pintarCatalogo();
            pintarCarrito();
        });

        VBox fila = new VBox(8, nombre, controles, quitar);
        fila.getStyleClass().add("carrito-item");
        return fila;
    }

    @FXML
    private void vaciarPedido(ActionEvent event) {
        if (carrito.isEmpty() || !Mensajes.confirmar("Vaciar pedido", "¿Quieres quitar todos los productos de tu pedido?")) {
            return;
        }
        carrito.clear();
        pintarCatalogo();
        pintarCarrito();
    }

    @FXML
    private void apartarPedido(ActionEvent event) {
        if (carrito.isEmpty()) {
            return;
        }
        StringBuilder resumen = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        List<Integer> ids = new ArrayList<>(carrito.keySet());
        for (Integer id : ids) {
            Material m = materiales.get(id);
            int cant = carrito.get(id);
            resumen.append("• ").append(cant).append(" x ").append(m.getDescripcionMaterial()).append('\n');
            total = total.add(m.getPrecioCosto().multiply(BigDecimal.valueOf(cant)));
        }
        resumen.append("\nTotal a pagar al recoger: ").append(MONEDA.format(total));
        if (!Mensajes.confirmar("¿Apartamos tu pedido?", resumen.toString())) {
            return;
        }
        try {
            int idPedido = service.crearPedido(SesionCliente.getCliente(), new LinkedHashMap<>(carrito));
            carrito.clear();
            cargarMateriales();
            pintarCarrito();
            Mensajes.info("¡Pedido apartado!",
                    "Tu folio es " + String.format("P-%04d", idPedido) + ".\n"
                    + "Te avisaremos cuando esté listo para recoger. Puedes ver cómo va en «Mis citas y pedidos».");
        } catch (SoptecException ex) {
            AlertaUtil.errorControlado("Portal cliente", "CREAR_PEDIDO", ex);
            cargarMateriales();
            pintarCarrito();
        } catch (Exception ex) {
            AlertaUtil.errorControlado("Portal cliente", "CREAR_PEDIDO", ex);
        }
    }
}
