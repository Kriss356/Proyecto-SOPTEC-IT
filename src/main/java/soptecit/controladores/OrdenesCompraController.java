package soptecit.controladores;

import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.OrdenCompra;
import com.mycompany.soptec.it.modelo.PartidaMaterial;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.servicio.OrdenCompraService;
import com.mycompany.soptec.it.servicio.ProveedorService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.servicio.RelacionesService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.ComboBuscadorUtil;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import soptecit.util.EditorDetalle;
import soptecit.util.Tablas;

/**
 * Órdenes de compra con sus partidas (relación muchos a muchos orden_compra_material) guardadas en MySQL.
 */
public class OrdenesCompraController implements Initializable {

    private static final String MODULO = "OrdenesCompra";
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrdenCompraService ordenService = new OrdenCompraService();
    private final ProveedorService proveedorService = new ProveedorService();
    private final ProyectoService proyectoService = new ProyectoService();
    private final MaterialService materialService = new MaterialService();
    private final RelacionesService relaciones = new RelacionesService();

    @FXML private TextField txtBuscarOrden;
    @FXML private TableView<OrdenCompra> tblOrdenes;
    @FXML private TableColumn<OrdenCompra, String> colOrdFolio;
    @FXML private TableColumn<OrdenCompra, String> colOrdProveedor;
    @FXML private TableColumn<OrdenCompra, String> colOrdProyecto;
    @FXML private TableColumn<OrdenCompra, String> colOrdFecha;
    @FXML private TableColumn<OrdenCompra, String> colOrdEntrega;
    @FXML private TableColumn<OrdenCompra, String> colOrdTotal;
    @FXML private TableColumn<OrdenCompra, String> colOrdEstado;
    @FXML private Label lblResumenOrdenes;

    @FXML private Label lblModoOrden;
    @FXML private ComboBox<Proveedor> cmbProveedorOrden;
    @FXML private ComboBox<Proyecto> cmbProyectoOrden;
    @FXML private DatePicker dpFechaOrden;
    @FXML private DatePicker dpEntregaOrden;
    @FXML private Label lblAyudaProveedor;
    @FXML private ComboBox<Material> cmbMaterialOrden;
    @FXML private Spinner<Integer> spnCantidadOrden;
    @FXML private TableView<PartidaMaterial> tblPartidas;
    @FXML private Label lblTotalOrden;

    private EditorDetalle<Material, PartidaMaterial> editorPartidas;
    private List<Material> materiales = new ArrayList<>();
    private OrdenCompra seleccionada;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaOrden.setValue(LocalDate.now());
        dpEntregaOrden.setValue(LocalDate.now().plusDays(5));

        colOrdFolio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFolio()));
        colOrdProveedor.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProveedorNombre() != null ? d.getValue().getProveedorNombre() : "—"));
        colOrdProyecto.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProyectoNombre() != null ? d.getValue().getProyectoNombre() : "Stock general"));
        colOrdFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaOrden() != null ? d.getValue().getFechaOrden().format(FECHA) : ""));
        colOrdEntrega.setCellValueFactory(d -> new SimpleStringProperty(formatoEntrega(d.getValue().getTiempoEntrega())));
        colOrdTotal.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getTotalCompra())));
        colOrdEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        colOrdEstado.setCellFactory(c -> Tablas.crearCeldaEstado());
        tblOrdenes.setPlaceholder(new Label("No hay órdenes de compra"));
        tblOrdenes.getSelectionModel().selectedItemProperty().addListener((o, a, oc) -> {
            if (oc != null) {
                cargarEnFormulario(oc);
            }
        });

        editorPartidas = new EditorDetalle<>(cmbMaterialOrden, spnCantidadOrden, tblPartidas, lblTotalOrden,
                "Total: ", "Agrega el material a comprar",
                EditorDetalle.adaptador(PartidaMaterial::de, Material::getIdMaterial, PartidaMaterial::getIdMaterial,
                        PartidaMaterial::getDescripcionMaterial, PartidaMaterial::getCantidad, PartidaMaterial::setCantidad,
                        PartidaMaterial::getPrecioUnitario, PartidaMaterial::getImporte));
        cmbProveedorOrden.valueProperty().addListener((o, a, p) -> ordenarMaterialPorProveedor(p));
        cargarCatalogos();
        cargarOrdenes(null);
    }

    private void cargarCatalogos() {
        try {
            cmbProveedorOrden.setItems(FXCollections.observableArrayList(proveedorService.listar(false)));
            List<Proyecto> proyectos = new ArrayList<>();
            proyectos.add(null);
            proyectos.addAll(proyectoService.listar(false));
            cmbProyectoOrden.setItems(FXCollections.observableArrayList(proyectos));
            materiales = materialService.listar(false);
            editorPartidas.setCatalogo(materiales);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_CATALOGOS_ORDEN", ex);
        }
    }

    /** Pone primero el material que surte el proveedor elegido (material_proveedor). */
    private void ordenarMaterialPorProveedor(Proveedor p) {
        if (p == null) {
            editorPartidas.setCatalogo(materiales);
            lblAyudaProveedor.setText("Al elegir proveedor se listan primero los materiales que surte.");
            return;
        }
        try {
            Set<Integer> surte = relaciones.listarMaterialDeProveedor(p.getIdProveedor()).stream()
                    .map(Material::getIdMaterial).collect(Collectors.toSet());
            List<Material> ordenados = new ArrayList<>();
            materiales.stream().filter(m -> surte.contains(m.getIdMaterial())).forEach(ordenados::add);
            materiales.stream().filter(m -> !surte.contains(m.getIdMaterial())).forEach(ordenados::add);
            editorPartidas.setCatalogo(ordenados);
            lblAyudaProveedor.setText(surte.isEmpty()
                    ? p.getNombreProveedor() + " aún no tiene material asignado (pestaña 'Material por proveedor')."
                    : p.getNombreProveedor() + " surte " + surte.size() + " material(es); aparecen primero en la lista.");
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_MATERIAL_PROVEEDOR", ex);
        }
    }

    @FXML
    private void cargarOrdenes(ActionEvent event) {
        try {
            List<OrdenCompra> ordenes = ordenService.listar(txtBuscarOrden.getText());
            tblOrdenes.setItems(FXCollections.observableArrayList(ordenes));
            long pendientes = ordenes.stream().filter(o -> OrdenCompra.SOLICITADA.equals(o.getEstado())).count();
            lblResumenOrdenes.setText(ordenes.size() + " órdenes  ·  " + pendientes + " por recibir");
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_ORDENES_COMPRA", ex);
        }
    }

    @FXML
    private void agregarPartida(ActionEvent event) {
        String aviso = editorPartidas.agregarSeleccion();
        if (aviso != null) {
            AlertaUtil.advertencia("Agregar material", aviso);
        }
    }

    @FXML
    private void quitarPartida(ActionEvent event) {
        String aviso = editorPartidas.quitarSeleccion();
        if (aviso != null) {
            AlertaUtil.advertencia("Quitar material", aviso);
        }
    }

    @FXML
    private void generarOrden(ActionEvent event) {
        try {
            OrdenCompra o = ordenService.crear(construir(new OrdenCompra(), "CREAR_ORDEN_COMPRA"));
            AlertaUtil.info("Orden generada", "Se registró la orden " + o.getFolio() + " por "
                    + MONEDA.format(o.getTotalCompra()) + " con " + o.getPartidas().size() + " partida(s).");
            limpiarOrden(null);
            cargarOrdenes(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CREAR_ORDEN_COMPRA", ex);
        }
    }

    @FXML
    private void actualizarOrden(ActionEvent event) {
        if (seleccionada == null) {
            AlertaUtil.advertencia("Selecciona una orden", "Elige en la tabla la orden que deseas modificar.");
            return;
        }
        try {
            OrdenCompra o = construir(seleccionada, "ACTUALIZAR_ORDEN_COMPRA");
            ordenService.actualizar(o);
            AlertaUtil.info("Orden actualizada", "Se actualizaron la orden " + o.getFolio() + " y sus partidas.");
            limpiarOrden(null);
            cargarOrdenes(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ACTUALIZAR_ORDEN_COMPRA", ex);
        }
    }

    @FXML
    private void recibirOrden(ActionEvent event) {
        OrdenCompra o = tblOrdenes.getSelectionModel().getSelectedItem();
        if (o == null) {
            AlertaUtil.advertencia("Selecciona una orden", "Elige la orden que llegó al almacén.");
            return;
        }
        if (!AlertaUtil.confirmar("Registrar recepción", "¿Confirmas que llegó todo el material de la orden "
                + o.getFolio() + "? Se sumará a las existencias del almacén.")) {
            return;
        }
        try {
            ordenService.recibir(o.getIdOrdenCompra());
            AlertaUtil.info("Orden recibida", "La orden " + o.getFolio() + " quedó Recibida y el material entró al almacén.");
            limpiarOrden(null);
            cargarCatalogos();
            cargarOrdenes(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "RECIBIR_ORDEN_COMPRA", ex);
        }
    }

    @FXML
    private void cancelarOrden(ActionEvent event) {
        OrdenCompra o = tblOrdenes.getSelectionModel().getSelectedItem();
        if (o == null) {
            AlertaUtil.advertencia("Selecciona una orden", "Elige la orden que deseas cancelar.");
            return;
        }
        if (!AlertaUtil.confirmar("Cancelar orden", "¿Cancelar la orden " + o.getFolio() + "?")) {
            return;
        }
        try {
            ordenService.cancelar(o.getIdOrdenCompra());
            AlertaUtil.info("Orden cancelada", "La orden " + o.getFolio() + " quedó cancelada.");
            limpiarOrden(null);
            cargarOrdenes(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CANCELAR_ORDEN_COMPRA", ex);
        }
    }

    @FXML
    private void eliminarOrden(ActionEvent event) {
        OrdenCompra o = tblOrdenes.getSelectionModel().getSelectedItem();
        if (o == null) {
            AlertaUtil.advertencia("Selecciona una orden", "Elige la orden que deseas eliminar.");
            return;
        }
        if (!AlertaUtil.confirmar("Eliminar orden", "¿Eliminar definitivamente la orden " + o.getFolio()
                + " y sus partidas?")) {
            return;
        }
        try {
            ordenService.eliminarFisico(o.getIdOrdenCompra());
            AlertaUtil.info("Orden eliminada", "La orden y sus partidas se eliminaron.");
            limpiarOrden(null);
            cargarOrdenes(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ELIMINAR_ORDEN_COMPRA", ex);
        }
    }

    @FXML
    private void limpiarOrden(ActionEvent event) {
        seleccionada = null;
        lblModoOrden.setText("Nueva orden de compra");
        cmbProveedorOrden.setValue(null);
        cmbProyectoOrden.setValue(null);
        dpFechaOrden.setValue(LocalDate.now());
        dpEntregaOrden.setValue(LocalDate.now().plusDays(5));
        editorPartidas.limpiar();
        tblOrdenes.getSelectionModel().clearSelection();
    }

    private void cargarEnFormulario(OrdenCompra o) {
        seleccionada = o;
        lblModoOrden.setText("Orden " + o.getFolio() + " · " + o.getEstado());
        ComboBuscadorUtil.seleccionarPorPredicado(cmbProveedorOrden,
                p -> o.getIdProveedor() != null && p.getIdProveedor() == o.getIdProveedor());
        if (o.getIdProyecto() != null) {
            ComboBuscadorUtil.seleccionarPorPredicado(cmbProyectoOrden, p -> p != null && p.getIdProyecto() == o.getIdProyecto());
        } else {
            cmbProyectoOrden.setValue(null);
        }
        dpFechaOrden.setValue(o.getFechaOrden());
        try {
            dpEntregaOrden.setValue(o.getTiempoEntrega() != null ? LocalDate.parse(o.getTiempoEntrega()) : null);
        } catch (Exception ex) {
            dpEntregaOrden.setValue(null);
        }
        editorPartidas.setRenglones(o.getPartidas());
    }

    private OrdenCompra construir(OrdenCompra base, String op) throws ValidacionException {
        Proveedor p = cmbProveedorOrden.getValue();
        if (p == null) {
            throw new ValidacionException(MODULO, op, "Selecciona el proveedor de la orden.");
        }
        base.setIdProveedor(p.getIdProveedor());
        base.setProveedorNombre(p.getNombreProveedor());
        Proyecto pr = cmbProyectoOrden.getValue();
        base.setIdProyecto(pr != null ? pr.getIdProyecto() : null);
        base.setFechaOrden(dpFechaOrden.getValue());
        base.setTiempoEntrega(dpEntregaOrden.getValue() != null ? dpEntregaOrden.getValue().toString() : null);
        base.setPartidas(editorPartidas.getRenglones());
        return base;
    }

    private static String formatoEntrega(String valor) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        try {
            return LocalDate.parse(valor).format(FECHA);
        } catch (Exception ex) {
            return valor;
        }
    }
}
