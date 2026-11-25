package soptecit.controladores;

import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.servicio.ProveedorService;
import com.mycompany.soptec.it.servicio.RelacionesService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.ComboBuscadorUtil;
import java.net.URL;
import java.text.NumberFormat;
import java.util.HashSet;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import soptecit.util.ListaCasillas;

/**
 * Material que surte cada proveedor: administra en bloque la relación muchos a muchos material_proveedor.
 */
public class MaterialProveedorController implements Initializable {

    private static final String MODULO = "MaterialProveedor";
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    private final ProveedorService proveedorService = new ProveedorService();
    private final MaterialService materialService = new MaterialService();
    private final RelacionesService relaciones = new RelacionesService();

    @FXML private TextField txtFiltroProveedor;
    @FXML private ComboBox<Proveedor> cmbProveedor;
    @FXML private Label lblInfoProveedor;
    @FXML private TableView<Material> tblSurte;
    @FXML private TableColumn<Material, String> colSurteMaterial;
    @FXML private TableColumn<Material, String> colSurtePrecio;
    @FXML private TableColumn<Material, String> colSurteExistencias;
    @FXML private TableColumn<Material, String> colSurteOtros;
    @FXML private ListView<Material> lstMateriales;
    @FXML private Label lblMarcados;

    private ListaCasillas<Material> casillas;
    private Set<Integer> guardados = new HashSet<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colSurteMaterial.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescripcionMaterial()));
        colSurtePrecio.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getPrecioCosto())));
        colSurteExistencias.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getExistencias())));
        colSurteOtros.setCellValueFactory(d -> new SimpleStringProperty(otrosProveedores(d.getValue())));
        tblSurte.setPlaceholder(new Label("Este proveedor aún no tiene material asignado"));
        casillas = new ListaCasillas<>(lstMateriales, Material::getDescripcionMaterial, lblMarcados, "material(es)");
        cmbProveedor.valueProperty().addListener((o, a, p) -> cargarAsignados());
        refrescar(null);
    }

    @FXML
    private void refrescar(ActionEvent event) {
        try {
            Proveedor actual = cmbProveedor.getValue();
            ComboBuscadorUtil.configurar(cmbProveedor, txtFiltroProveedor, proveedorService.listar(false), Proveedor::toString);
            casillas.setElementos(materialService.listar(false));
            if (actual != null) {
                ComboBuscadorUtil.seleccionarPorPredicado(cmbProveedor, p -> p.getIdProveedor() == actual.getIdProveedor());
            }
            cargarAsignados();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_MATERIAL_PROVEEDOR", ex);
        }
    }

    private void cargarAsignados() {
        Proveedor p = cmbProveedor.getValue();
        if (p == null) {
            tblSurte.setItems(FXCollections.observableArrayList());
            guardados = new HashSet<>();
            casillas.marcarSolo(m -> false);
            lblInfoProveedor.setText("Sin proveedor seleccionado");
            return;
        }
        try {
            List<Material> lista = relaciones.listarMaterialDeProveedor(p.getIdProveedor());
            tblSurte.setItems(FXCollections.observableArrayList(lista));
            guardados = lista.stream().map(Material::getIdMaterial).collect(Collectors.toSet());
            casillas.marcarSolo(m -> guardados.contains(m.getIdMaterial()));
            lblInfoProveedor.setText(p.getNombreProveedor() + " · Tel. " + p.getTelefonoProveedor()
                    + " · " + lista.size() + " material(es)");
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_MATERIAL_PROVEEDOR", ex);
        }
    }

    private String otrosProveedores(Material m) {
        Proveedor actual = cmbProveedor.getValue();
        try {
            String otros = relaciones.listarProveedoresDeMaterial(m.getIdMaterial()).stream()
                    .filter(p -> actual == null || p.getIdProveedor() != actual.getIdProveedor())
                    .map(Proveedor::getNombreProveedor).collect(Collectors.joining(", "));
            return otros.isEmpty() ? "Solo este proveedor" : otros;
        } catch (Exception ex) {
            return "—";
        }
    }

    @FXML
    private void guardar(ActionEvent event) {
        Proveedor p = cmbProveedor.getValue();
        if (p == null) {
            AlertaUtil.advertencia("Selecciona un proveedor", "Elige primero el proveedor.");
            return;
        }
        try {
            List<Integer> ids = casillas.getMarcados().stream().map(Material::getIdMaterial).collect(Collectors.toList());
            relaciones.guardarMaterialDeProveedor(p.getIdProveedor(), ids);
            AlertaUtil.info("Material guardado", p.getNombreProveedor() + " quedó con " + ids.size() + " material(es).");
            cargarAsignados();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "GUARDAR_MATERIAL_PROVEEDOR", ex);
            cargarAsignados();
        }
    }

    @FXML
    private void deshacer(ActionEvent event) {
        casillas.marcarSolo(m -> guardados.contains(m.getIdMaterial()));
    }

    @FXML
    private void quitarTodos(ActionEvent event) {
        casillas.marcarSolo(m -> false);
    }
}
