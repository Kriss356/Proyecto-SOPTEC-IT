package soptecit.controladores;

import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.MovimientoAlmacen;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.servicio.MovimientoAlmacenService;
import com.mycompany.soptec.it.servicio.ProveedorService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.servicio.RelacionesService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.GestorImagenes;
import com.mycompany.soptec.it.util.Validador;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import soptecit.util.Edicion;
import soptecit.util.Mensajes;
import soptecit.util.Tablas;

/**
 * Controlador estilizado de Compras-Almacén.
 * Integra CRUD completo de Materiales y Proveedores, KPIs en vivo y ajustes reales de existencias.
 */
public class ComprasAlmacenController implements Initializable {

    private static final String MOD_MAT = "Materiales";
    private static final String MOD_PROV = "Proveedores";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    private final MaterialService materialService = new MaterialService();
    private final ProveedorService proveedorService = new ProveedorService();
    private final ProyectoService proyectoService = new ProyectoService();
    private final EmpleadoService empleadoService = new EmpleadoService();

    // KPIs
    // Botones del formulario de tblMateriales (modo nuevo / editando)
    // Botones del formulario de tblProveedores (modo nuevo / editando)
    @FXML private Button btnGuardarProveedor;
    @FXML private Button btnActualizarProveedor;
    @FXML private Button btnDesactivarProveedor;
    @FXML private Button btnReactivarProveedor;
    @FXML private Button btnEliminarProveedor;
    @FXML private Button btnGuardarMaterial;
    @FXML private Button btnActualizarMaterial;
    @FXML private Button btnDesactivarMaterial;
    @FXML private Button btnReactivarMaterial;
    @FXML private Button btnEliminarMaterial;
    @FXML private Button btnPedirMaterial;
    @FXML private Label lblTotalMateriales;
    @FXML private Label lblBajoStockAlm;
    @FXML private Label lblTotalProveedores;
    @FXML private Label lblValorInventario;

    // 1. Materiales
    @FXML private CheckBox chkInactivosMat;
    @FXML private TextField txtBuscarMaterial;
    @FXML private TextField txtConsultarIdMat;
    @FXML private TableView<Material> tblMateriales;
    @FXML private TableColumn<Material, String> colMatId;
    @FXML private TableColumn<Material, String> colMatDesc;
    @FXML private TableColumn<Material, String> colMatPrecio;
    @FXML private TableColumn<Material, String> colMatExistencias;
    @FXML private TableColumn<Material, String> colMatStock;
    @FXML private TableColumn<Material, String> colMatEstado;

    @FXML private Label lblModoMaterial;
    @FXML private TextField txtDescMaterial;
    @FXML private TextField txtPrecioMaterial;
    @FXML private TextField txtExistenciasMaterial;

    private Material materialSeleccionado;

    // 2. Proveedores
    @FXML private CheckBox chkInactivosProv;
    @FXML private TextField txtBuscarProv;
    @FXML private TextField txtConsultarIdProv;
    @FXML private TableView<Proveedor> tblProveedores;
    @FXML private TableColumn<Proveedor, String> colProvId;
    @FXML private TableColumn<Proveedor, String> colProvNombre;
    @FXML private TableColumn<Proveedor, String> colProvTelefono;
    @FXML private TableColumn<Proveedor, String> colProvCorreo;
    @FXML private TableColumn<Proveedor, String> colProvEstado;

    @FXML private Label lblModoProveedor;
    @FXML private TextField txtNombreProveedor;
    @FXML private TextField txtTelefonoProveedor;
    @FXML private TextField txtCorreoProveedor;
    @FXML private ImageView imgProveedor;
    @FXML private Label lblRutaImgProv;

    private Proveedor proveedorSeleccionado;
    private String rutaImgProveedor;

    // 3. Movimientos
    @FXML private RadioButton rbEntrada;
    @FXML private RadioButton rbSalida;
    @FXML private ComboBox<Material> cmbMaterialMov;
    @FXML private Spinner<Integer> spnCantidadMov;
    @FXML private DatePicker dpFechaMov;
    @FXML private ComboBox<Proyecto> cmbProyectoMov;
    @FXML private ComboBox<Empleado> cmbResponsableMov;
    @FXML private TextArea txtObservacionesMov;
    @FXML private TextField txtReferenciaMov;
    @FXML private Label lblModoMovimiento;
    @FXML private Label lblChipMovimiento;
    @FXML private Label lblAyudaMovimiento;
    @FXML private Button btnRegistrarMov;
    @FXML private Button btnEliminarMov;
    @FXML private DatePicker dpFiltroMov;
    @FXML private TableView<MovimientoAlmacen> tblMovimientos;
    @FXML private TableColumn<MovimientoAlmacen, String> colMovFecha;
    @FXML private TableColumn<MovimientoAlmacen, String> colMovTipo;
    @FXML private TableColumn<MovimientoAlmacen, String> colMovMaterial;
    @FXML private TableColumn<MovimientoAlmacen, String> colMovCantidad;
    @FXML private TableColumn<MovimientoAlmacen, String> colMovDestino;
    @FXML private TableColumn<MovimientoAlmacen, String> colMovResponsable;

    private final MovimientoAlmacenService movimientoService = new MovimientoAlmacenService();
    /** Movimiento cargado en el formulario para corregirlo; null cuando se registra uno nuevo. */
    private MovimientoAlmacen movimientoEditando;

    // 4. Órdenes de compra, material por proveedor y material por proyecto: controladores propios
    //    (OrdenesCompraController, MaterialProveedorController, MaterialProyectoController) con fx:include.


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        inicializarMateriales();
        inicializarProveedores();
        inicializarMovimientos();
        refrescarTodo();
    }

    private void refrescarTodo() {
        cargarTablaMateriales(null);
        cargarTablaProveedores(null);
        cargarCombosAuxiliares();
        cargarMovimientos();
        actualizarKpis();
    }

    private void actualizarKpis() {
        try {
            List<Material> materiales = materialService.listar(false);
            List<Proveedor> proveedores = proveedorService.listar(false);

            lblTotalMateriales.setText(String.valueOf(materiales.size()));
            long bajoStock = materiales.stream().filter(m -> m.getExistencias() <= 10).count();
            lblBajoStockAlm.setText(String.valueOf(bajoStock));
            lblTotalProveedores.setText(String.valueOf(proveedores.size()));

            BigDecimal valorTotal = materiales.stream()
                    .map(m -> m.getPrecioCosto().multiply(BigDecimal.valueOf(m.getExistencias())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblValorInventario.setText(MONEDA.format(valorTotal));
        } catch (Exception ex) {
            // Ya reportado por los listados
        }
    }

    // =========================================================================
    // 1. MATERIALES (CRUD)
    // =========================================================================
    private void inicializarMateriales() {
        colMatId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getIdMaterial())));
        colMatDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescripcionMaterial()));
        colMatPrecio.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getPrecioCosto())));
        colMatExistencias.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getExistencias())));
        colMatStock.setCellValueFactory(d -> {
            int ex = d.getValue().getExistencias();
            return new SimpleStringProperty(ex == 0 ? "Agotado" : (ex <= 10 ? "Bajo stock" : "Disponible"));
        });
        colMatStock.setCellFactory(c -> Tablas.crearCeldaEstado());
        colMatEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstadoTexto()));
        colMatEstado.setCellFactory(c -> Tablas.crearCeldaEstado());
        tblMateriales.setPlaceholder(new Label("No hay materiales registrados"));

        tblMateriales.getSelectionModel().selectedItemProperty().addListener((obs, a, nuevo) -> {
            if (nuevo != null) {
                cargarMaterialEnFormulario(nuevo);
            }
        });
        // Edición rápida: lápiz en cada fila, doble clic o Enter
        Edicion.habilitar(tblMateriales, this::cargarMaterialEnFormulario);
        Edicion.modo(false, lblModoMaterial, btnGuardarMaterial, btnActualizarMaterial, btnDesactivarMaterial, btnReactivarMaterial, btnEliminarMaterial, btnPedirMaterial);
    }

    @FXML
    private void cargarTablaMateriales(ActionEvent event) {
        try {
            List<Material> lista = materialService.listar(chkInactivosMat.isSelected());
            tblMateriales.setItems(FXCollections.observableArrayList(lista));
            if (event != null) {
                cargarCombosAuxiliares();
                actualizarKpis();
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, "LISTAR_MATERIALES", ex);
        }
    }

    @FXML
    private void buscarMateriales(ActionEvent event) {
        try {
            tblMateriales.setItems(FXCollections.observableArrayList(
                    materialService.buscar(txtBuscarMaterial.getText(), chkInactivosMat.isSelected())));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, "BUSCAR_MATERIALES", ex);
        }
    }

    @FXML
    private void consultarMaterialPorId(ActionEvent event) {
        try {
            int id = Validador.parsearIdPositivo(txtConsultarIdMat.getText(), "ID de material", MOD_MAT, "CONSULTAR_POR_ID");
            Material m = materialService.consultarPorId(id);
            if (m == null) {
                AlertaUtil.advertencia("No encontrado", "No existe un material con el ID " + id + ".");
                return;
            }
            cargarMaterialEnFormulario(m);
            AlertaUtil.info("Material encontrado",
                    "Descripción: " + m.getDescripcionMaterial()
                    + "\nPrecio costo: " + MONEDA.format(m.getPrecioCosto())
                    + "\nExistencias: " + m.getExistencias()
                    + "\nEstado: " + m.getEstadoTexto());
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, "CONSULTAR_POR_ID", ex);
        }
    }

    @FXML
    private void guardarMaterial(ActionEvent event) {
        try {
            Material creado = materialService.crear(construirMaterialDesdeFormulario(new Material(), "CREAR_MATERIAL"));
            AlertaUtil.info("Material registrado", "Se guardó '" + creado.getDescripcionMaterial() + "'.");
            limpiarMaterial(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, "CREAR_MATERIAL", ex);
        }
    }

    @FXML
    private void actualizarMaterial(ActionEvent event) {
        if (materialSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un material", "Elige un material de la tabla para actualizarlo.");
            return;
        }
        try {
            Material editado = construirMaterialDesdeFormulario(materialSeleccionado, "ACTUALIZAR_MATERIAL");
            materialService.actualizar(editado);
            AlertaUtil.info("Material actualizado", "Se actualizó '" + editado.getDescripcionMaterial() + "'.");
            limpiarMaterial(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, "ACTUALIZAR_MATERIAL", ex);
        }
    }

    @FXML
    private void desactivarMaterial(ActionEvent event) {
        if (materialSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un material", "Elige un material de la tabla.");
            return;
        }
        try {
            materialService.desactivar(materialSeleccionado.getIdMaterial());
            AlertaUtil.info("Material desactivado", "El material quedó inactivo.");
            limpiarMaterial(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, "DESACTIVAR_MATERIAL", ex);
        }
    }

    @FXML
    private void reactivarMaterial(ActionEvent event) {
        if (materialSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un material", "Marca 'Mostrar inactivos' y elige un material.");
            return;
        }
        try {
            materialService.reactivar(materialSeleccionado.getIdMaterial());
            AlertaUtil.info("Material reactivado", "El material volvió a estar activo.");
            limpiarMaterial(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, "REACTIVAR_MATERIAL", ex);
        }
    }

    @FXML
    private void eliminarFisicoMaterial(ActionEvent event) {
        if (materialSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un material", "Elige un material de la tabla.");
            return;
        }
        if (!AlertaUtil.confirmar("Eliminar material definitivamente",
                "¿Eliminar permanentemente '" + materialSeleccionado.getDescripcionMaterial() + "'?")) {
            return;
        }
        try {
            materialService.eliminarFisico(materialSeleccionado.getIdMaterial());
            AlertaUtil.info("Material eliminado", "El registro fue eliminado de la base de datos.");
            limpiarMaterial(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, "ELIMINAR_FISICO_MATERIAL", ex);
        }
    }

    @FXML
    private void limpiarMaterial(ActionEvent event) {
        materialSeleccionado = null;
        lblModoMaterial.setText("Registrar material");
        txtDescMaterial.clear();
        txtPrecioMaterial.clear();
        txtExistenciasMaterial.clear();
        tblMateriales.getSelectionModel().clearSelection();
        Edicion.modo(false, lblModoMaterial, btnGuardarMaterial, btnActualizarMaterial, btnDesactivarMaterial, btnReactivarMaterial, btnEliminarMaterial, btnPedirMaterial);
    }

    private void cargarMaterialEnFormulario(Material m) {
        materialSeleccionado = m;
        lblModoMaterial.setText("Editando Material #" + m.getIdMaterial());
        txtDescMaterial.setText(m.getDescripcionMaterial());
        txtPrecioMaterial.setText(m.getPrecioCosto().toPlainString());
        txtExistenciasMaterial.setText(String.valueOf(m.getExistencias()));
        Edicion.modo(true, lblModoMaterial, btnGuardarMaterial, btnActualizarMaterial, btnDesactivarMaterial, btnReactivarMaterial, btnEliminarMaterial, btnPedirMaterial);
    }

    /** Abre una orden de compra ya llenada con este material, la cantidad sugerida y su proveedor. */
    @FXML
    private void pedirMaterial(ActionEvent event) {
        if (materialSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un material", "Elige en la tabla el material que quieres pedir.");
            return;
        }
        PrincipalController.pedirMaterial(materialSeleccionado.getIdMaterial());
    }

    private Material construirMaterialDesdeFormulario(Material base, String op) throws Exception {
        base.setDescripcionMaterial(txtDescMaterial.getText());
        base.setPrecioCosto(Validador.parsearDecimalNoNegativo(txtPrecioMaterial.getText(), "Precio costo", MOD_MAT, op));
        base.setExistencias(Validador.parsearEnteroNoNegativo(txtExistenciasMaterial.getText(), "Existencias", MOD_MAT, op));
        return base;
    }

    // =========================================================================
    // 2. PROVEEDORES (CRUD)
    // =========================================================================
    private void inicializarProveedores() {
        colProvId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getIdProveedor())));
        colProvNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreProveedor()));
        colProvTelefono.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTelefonoProveedor()));
        colProvCorreo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCorreoProveedor()));
        colProvEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstadoTexto()));
        colProvEstado.setCellFactory(c -> Tablas.crearCeldaEstado());
        tblProveedores.setPlaceholder(new Label("No hay proveedores registrados"));

        tblProveedores.getSelectionModel().selectedItemProperty().addListener((obs, a, nuevo) -> {
            if (nuevo != null) {
                cargarProveedorEnFormulario(nuevo);
            }
        });
        // Edición rápida: lápiz en cada fila, doble clic o Enter
        Edicion.habilitar(tblProveedores, this::cargarProveedorEnFormulario);
        Edicion.modo(false, lblModoProveedor, btnGuardarProveedor, btnActualizarProveedor, btnDesactivarProveedor, btnReactivarProveedor, btnEliminarProveedor);
    }

    @FXML
    private void cargarTablaProveedores(ActionEvent event) {
        try {
            tblProveedores.setItems(FXCollections.observableArrayList(
                    proveedorService.listar(chkInactivosProv.isSelected())));
            if (event != null) {
                cargarCombosAuxiliares();
                actualizarKpis();
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_PROV, "LISTAR_PROVEEDORES", ex);
        }
    }

    @FXML
    private void buscarProveedores(ActionEvent event) {
        try {
            tblProveedores.setItems(FXCollections.observableArrayList(
                    proveedorService.buscar(txtBuscarProv.getText(), chkInactivosProv.isSelected())));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_PROV, "BUSCAR_PROVEEDORES", ex);
        }
    }

    @FXML
    private void consultarProveedorPorId(ActionEvent event) {
        try {
            int id = Validador.parsearIdPositivo(txtConsultarIdProv.getText(), "ID de proveedor", MOD_PROV, "CONSULTAR_POR_ID");
            Proveedor p = proveedorService.consultarPorId(id);
            if (p == null) {
                AlertaUtil.advertencia("No encontrado", "No existe un proveedor con el ID " + id + ".");
                return;
            }
            cargarProveedorEnFormulario(p);
            AlertaUtil.info("Proveedor encontrado",
                    "Nombre: " + p.getNombreProveedor()
                    + "\nTeléfono: " + p.getTelefonoProveedor()
                    + "\nCorreo: " + p.getCorreoProveedor()
                    + "\nEstado: " + p.getEstadoTexto());
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_PROV, "CONSULTAR_POR_ID", ex);
        }
    }

    @FXML
    private void seleccionarImagenProveedor(ActionEvent event) {
        try {
            String ruta = GestorImagenes.elegirYGuardarImagen(
                    imgProveedor.getScene().getWindow(), "Seleccionar imagen del proveedor", "proveedores", MOD_PROV);
            if (ruta != null) {
                rutaImgProveedor = ruta;
                lblRutaImgProv.setText(ruta);
                imgProveedor.setImage(GestorImagenes.cargarImagen(ruta));
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_PROV, "SELECCIONAR_IMAGEN", ex);
        }
    }

    @FXML
    private void quitarImagenProveedor(ActionEvent event) {
        rutaImgProveedor = null;
        lblRutaImgProv.setText("Sin imagen seleccionada");
        imgProveedor.setImage(null);
    }

    @FXML
    private void guardarProveedor(ActionEvent event) {
        try {
            Proveedor creado = proveedorService.crear(construirProveedorDesdeFormulario(new Proveedor()));
            AlertaUtil.info("Proveedor registrado", "Se guardó a " + creado.getNombreProveedor() + ".");
            limpiarProveedor(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_PROV, "CREAR_PROVEEDOR", ex);
        }
    }

    @FXML
    private void actualizarProveedor(ActionEvent event) {
        if (proveedorSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un proveedor", "Elige un proveedor de la tabla para actualizarlo.");
            return;
        }
        try {
            Proveedor editado = construirProveedorDesdeFormulario(proveedorSeleccionado);
            proveedorService.actualizar(editado);
            AlertaUtil.info("Proveedor actualizado", "Se actualizó a " + editado.getNombreProveedor() + ".");
            limpiarProveedor(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_PROV, "ACTUALIZAR_PROVEEDOR", ex);
        }
    }

    @FXML
    private void desactivarProveedor(ActionEvent event) {
        if (proveedorSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un proveedor", "Elige un proveedor de la tabla.");
            return;
        }
        try {
            proveedorService.desactivar(proveedorSeleccionado.getIdProveedor());
            AlertaUtil.info("Proveedor desactivado", "El proveedor quedó inactivo.");
            limpiarProveedor(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_PROV, "DESACTIVAR_PROVEEDOR", ex);
        }
    }

    @FXML
    private void reactivarProveedor(ActionEvent event) {
        if (proveedorSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un proveedor", "Marca 'Mostrar inactivos' y elige un proveedor.");
            return;
        }
        try {
            proveedorService.reactivar(proveedorSeleccionado.getIdProveedor());
            AlertaUtil.info("Proveedor reactivado", "El proveedor volvió a estar activo.");
            limpiarProveedor(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_PROV, "REACTIVAR_PROVEEDOR", ex);
        }
    }

    @FXML
    private void eliminarFisicoProveedor(ActionEvent event) {
        if (proveedorSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un proveedor", "Elige un proveedor de la tabla.");
            return;
        }
        if (!AlertaUtil.confirmar("Eliminar proveedor definitivamente",
                "¿Eliminar permanentemente a " + proveedorSeleccionado.getNombreProveedor() + "?")) {
            return;
        }
        try {
            proveedorService.eliminarFisico(proveedorSeleccionado.getIdProveedor());
            AlertaUtil.info("Proveedor eliminado", "El registro fue eliminado de la base de datos.");
            limpiarProveedor(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_PROV, "ELIMINAR_FISICO_PROVEEDOR", ex);
        }
    }

    @FXML
    private void limpiarProveedor(ActionEvent event) {
        proveedorSeleccionado = null;
        lblModoProveedor.setText("Registrar proveedor");
        txtNombreProveedor.clear();
        txtTelefonoProveedor.clear();
        txtCorreoProveedor.clear();
        quitarImagenProveedor(null);
        tblProveedores.getSelectionModel().clearSelection();
        Edicion.modo(false, lblModoProveedor, btnGuardarProveedor, btnActualizarProveedor, btnDesactivarProveedor, btnReactivarProveedor, btnEliminarProveedor);
    }

    private void cargarProveedorEnFormulario(Proveedor p) {
        proveedorSeleccionado = p;
        lblModoProveedor.setText("Editando Proveedor #" + p.getIdProveedor());
        txtNombreProveedor.setText(p.getNombreProveedor());
        txtTelefonoProveedor.setText(p.getTelefonoProveedor());
        txtCorreoProveedor.setText(p.getCorreoProveedor());
        rutaImgProveedor = p.getImagen();
        if (rutaImgProveedor != null && !rutaImgProveedor.isBlank()) {
            lblRutaImgProv.setText(rutaImgProveedor);
            imgProveedor.setImage(GestorImagenes.cargarImagen(rutaImgProveedor));
        } else {
            quitarImagenProveedor(null);
        }
        Edicion.modo(true, lblModoProveedor, btnGuardarProveedor, btnActualizarProveedor, btnDesactivarProveedor, btnReactivarProveedor, btnEliminarProveedor);
    }

    private Proveedor construirProveedorDesdeFormulario(Proveedor base) {
        base.setNombreProveedor(txtNombreProveedor.getText());
        base.setTelefonoProveedor(txtTelefonoProveedor.getText());
        base.setCorreoProveedor(txtCorreoProveedor.getText());
        base.setImagen(rutaImgProveedor);
        return base;
    }

    // =========================================================================
    // 3 y 4. MOVIMIENTOS (ACTUALIZA STOCK EN MYSQL) Y ÓRDENES DE COMPRA
    // =========================================================================
    private void inicializarMovimientos() {
        spnCantidadMov.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100000, 1));
        dpFechaMov.setValue(LocalDate.now());

        // Autorrelleno: salida a un proyecto → se propone como responsable al primer empleado del proyecto
        cmbProyectoMov.valueProperty().addListener((o, a, p) -> {
            if (p != null && movimientoEditando == null && cmbResponsableMov.getValue() == null) {
                try {
                    List<Empleado> equipo = new RelacionesService().listarPersonal(p.getIdProyecto());
                    if (!equipo.isEmpty()) {
                        int id = equipo.get(0).getIdEmpleado();
                        cmbResponsableMov.getItems().stream().filter(e -> e.getIdEmpleado() == id).findFirst()
                                .ifPresent(cmbResponsableMov::setValue);
                    }
                } catch (Exception ex) {
                    // El responsable se elige a mano
                }
            }
        });
        rbSalida.selectedProperty().addListener((o, a, salida) -> {
            cmbProyectoMov.setDisable(!salida);
            if (!salida) {
                cmbProyectoMov.setValue(null);
            }
        });

        colMovFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null ? d.getValue().getFecha().format(FECHA) : ""));
        colMovTipo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTipo()));
        colMovTipo.setCellFactory(c -> Tablas.crearCeldaEstado());
        colMovMaterial.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMaterialDescripcion()));
        colMovCantidad.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCantidad())));
        colMovDestino.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDestinoTexto()));
        colMovResponsable.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEmpleadoNombre() != null ? d.getValue().getEmpleadoNombre() : "Almacén"));
        tblMovimientos.setPlaceholder(new Label("No hay movimientos registrados"));
        Edicion.habilitar(tblMovimientos, this::cargarMovimientoEnFormulario, mv -> {
            cargarMovimientoEnFormulario(mv);
            eliminarMovimiento(null);
        });

        dpFiltroMov.valueProperty().addListener((o, a, f) -> cargarMovimientos());
        modoMovimiento(false);
    }

    private void cargarCombosAuxiliares() {
        try {
            List<Material> mats = materialService.listar(false);
            cmbMaterialMov.setItems(FXCollections.observableArrayList(mats));

            List<Proyecto> proys = proyectoService.listar(false);
            cmbProyectoMov.setItems(FXCollections.observableArrayList(proys));

            List<Empleado> emps = empleadoService.listar(false);
            cmbResponsableMov.setItems(FXCollections.observableArrayList(emps));
        } catch (Exception ex) {
            // El error ya quedó registrado en el log por el DAO; se avisa sin cerrar la ventana.
            AlertaUtil.errorControlado(MOD_MAT, "CARGAR_COMBOS_ALMACEN", ex);
        }
    }

    private void cargarMovimientos() {
        try {
            tblMovimientos.setItems(FXCollections.observableArrayList(movimientoService.listar(dpFiltroMov.getValue())));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, "LISTAR_MOVIMIENTOS", ex);
        }
    }

    @FXML
    private void verTodosMovimientos(ActionEvent event) {
        if (dpFiltroMov.getValue() != null) {
            dpFiltroMov.setValue(null); // el listener recarga la tabla
        } else {
            cargarMovimientos();
        }
    }

    /** Guarda un movimiento nuevo o, si hay uno cargado, sus cambios (el almacén se corrige solo). */
    @FXML
    private void registrarMovimiento(ActionEvent event) {
        Material mat = cmbMaterialMov.getValue();
        if (mat == null) {
            Mensajes.error("Faltan datos", "Selecciona el material del movimiento.");
            return;
        }
        boolean esEntrada = rbEntrada.isSelected();
        MovimientoAlmacen mv = new MovimientoAlmacen();
        mv.setIdMovimiento(movimientoEditando != null ? movimientoEditando.getIdMovimiento() : 0);
        mv.setTipo(esEntrada ? MovimientoAlmacen.ENTRADA : MovimientoAlmacen.SALIDA);
        mv.setIdMaterial(mat.getIdMaterial());
        mv.setCantidad(spnCantidadMov.getValue() != null ? spnCantidadMov.getValue() : 0);
        mv.setFecha(dpFechaMov.getValue() != null ? dpFechaMov.getValue() : LocalDate.now());
        mv.setIdProyecto(!esEntrada && cmbProyectoMov.getValue() != null ? cmbProyectoMov.getValue().getIdProyecto() : null);
        mv.setIdEmpleado(cmbResponsableMov.getValue() != null ? cmbResponsableMov.getValue().getIdEmpleado() : null);
        mv.setReferencia(Validador.opcional(txtReferenciaMov.getText()));
        mv.setObservaciones(Validador.opcional(txtObservacionesMov.getText()));
        try {
            if (movimientoEditando == null) {
                movimientoService.registrar(mv);
                Mensajes.info("Movimiento aplicado", mv.getTipo() + " de " + mv.getCantidad() + " × '"
                        + mat.getDescripcionMaterial() + "' registrada. Las existencias ya se actualizaron.");
            } else {
                movimientoService.actualizar(mv);
                Mensajes.info("Cambios guardados", "Se corrigió el movimiento y las existencias del almacén "
                        + "se ajustaron con la diferencia.");
            }
            nuevoMovimiento(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, movimientoEditando == null ? "REGISTRAR_MOVIMIENTO" : "ACTUALIZAR_MOVIMIENTO", ex);
        }
    }

    @FXML
    private void eliminarMovimiento(ActionEvent event) {
        MovimientoAlmacen mv = movimientoEditando != null ? movimientoEditando
                : tblMovimientos.getSelectionModel().getSelectedItem();
        if (mv == null) {
            Mensajes.advertencia("Selecciona un movimiento", "Elige en el historial el movimiento que quieres eliminar.");
            return;
        }
        String efecto = mv.isEntrada()
                ? "Se restarán " + mv.getCantidad() + " unidades del almacén."
                : "Se regresarán " + mv.getCantidad() + " unidades al almacén"
                + (mv.getIdProyecto() != null ? " y se quitarán del material del proyecto." : ".");
        if (!Mensajes.confirmar("Eliminar movimiento", "¿Eliminar la " + mv.getTipo().toLowerCase() + " de "
                + mv.getCantidad() + " × '" + mv.getMaterialDescripcion() + "'?\n" + efecto)) {
            return;
        }
        try {
            movimientoService.eliminar(mv.getIdMovimiento());
            Mensajes.info("Movimiento eliminado", "El movimiento se quitó y el almacén quedó como estaba antes de él.");
            nuevoMovimiento(null);
            refrescarTodo();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_MAT, "ELIMINAR_MOVIMIENTO", ex);
        }
    }

    @FXML
    private void nuevoMovimiento(ActionEvent event) {
        movimientoEditando = null;
        rbEntrada.setSelected(true);
        cmbMaterialMov.setValue(null);
        spnCantidadMov.getValueFactory().setValue(1);
        dpFechaMov.setValue(LocalDate.now());
        cmbProyectoMov.setValue(null);
        cmbResponsableMov.setValue(null);
        txtReferenciaMov.clear();
        txtObservacionesMov.clear();
        tblMovimientos.getSelectionModel().clearSelection();
        modoMovimiento(false);
    }

    private void cargarMovimientoEnFormulario(MovimientoAlmacen mv) {
        movimientoEditando = mv;
        if (mv.isEntrada()) {
            rbEntrada.setSelected(true);
        } else {
            rbSalida.setSelected(true);
        }
        cmbMaterialMov.setValue(buscarPorId(cmbMaterialMov.getItems(), mv.getIdMaterial()));
        spnCantidadMov.getValueFactory().setValue(mv.getCantidad());
        dpFechaMov.setValue(mv.getFecha());
        cmbProyectoMov.setValue(mv.getIdProyecto() == null ? null
                : cmbProyectoMov.getItems().stream().filter(p -> p.getIdProyecto() == mv.getIdProyecto()).findFirst().orElse(null));
        cmbResponsableMov.setValue(mv.getIdEmpleado() == null ? null
                : cmbResponsableMov.getItems().stream().filter(e -> e.getIdEmpleado() == mv.getIdEmpleado()).findFirst().orElse(null));
        txtReferenciaMov.setText(mv.getReferencia());
        txtObservacionesMov.setText(mv.getObservaciones());
        modoMovimiento(true);
        cmbMaterialMov.requestFocus();
    }

    private static Material buscarPorId(List<Material> materiales, int idMaterial) {
        for (Material m : materiales) {
            if (m.getIdMaterial() == idMaterial) {
                return m;
            }
        }
        return null;
    }

    private void modoMovimiento(boolean editando) {
        lblModoMovimiento.setText(editando ? "Corregir movimiento" : "Registrar movimiento");
        lblChipMovimiento.setText(editando ? "Editando #" + movimientoEditando.getIdMovimiento() : "Nuevo");
        lblChipMovimiento.getStyleClass().remove("chip-modo-editando");
        if (editando) {
            lblChipMovimiento.getStyleClass().add("chip-modo-editando");
        }
        lblAyudaMovimiento.setText(editando
                ? "Al guardar, el almacén se corrige con la diferencia entre el movimiento original y el nuevo"
                : "Las existencias del almacén se actualizan solas");
        btnRegistrarMov.setText(editando ? "Guardar cambios" : "Aplicar movimiento");
        Edicion.modo(editando, lblModoMovimiento, null, null, btnEliminarMov);
    }
}
