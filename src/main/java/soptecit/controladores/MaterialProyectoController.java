package soptecit.controladores;

import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.PartidaMaterial;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.servicio.RelacionesService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.ComboBuscadorUtil;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import soptecit.util.EditorDetalle;

/**
 * Material asignado a un proyecto (relación muchos a muchos proyecto_material con cantidad).
 * Guardar ajusta las existencias del almacén con la diferencia, todo en una transacción.
 */
public class MaterialProyectoController implements Initializable {

    private static final String MODULO = "MaterialProyecto";

    private final ProyectoService proyectoService = new ProyectoService();
    private final MaterialService materialService = new MaterialService();
    private final RelacionesService relaciones = new RelacionesService();

    @FXML private TextField txtFiltroProyecto;
    @FXML private ComboBox<Proyecto> cmbProyecto;
    @FXML private ComboBox<Material> cmbMaterial;
    @FXML private Spinner<Integer> spnCantidad;
    @FXML private TableView<PartidaMaterial> tblMaterialProyecto;
    @FXML private Label lblTotalMaterial;

    private EditorDetalle<Material, PartidaMaterial> editor;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        editor = new EditorDetalle<>(cmbMaterial, spnCantidad, tblMaterialProyecto, lblTotalMaterial,
                "Costo del material: ", "Elige un proyecto para ver su material",
                EditorDetalle.adaptador(PartidaMaterial::de, Material::getIdMaterial, PartidaMaterial::getIdMaterial,
                        PartidaMaterial::getDescripcionMaterial, PartidaMaterial::getCantidad, PartidaMaterial::setCantidad,
                        PartidaMaterial::getPrecioUnitario, PartidaMaterial::getImporte));
        cmbProyecto.valueProperty().addListener((o, a, p) -> cargarMaterialDelProyecto());
        refrescar(null);
    }

    @FXML
    private void refrescar(ActionEvent event) {
        try {
            Proyecto actual = cmbProyecto.getValue();
            ComboBuscadorUtil.configurar(cmbProyecto, txtFiltroProyecto, proyectoService.listar(false), Proyecto::toString);
            editor.setCatalogo(materialService.listar(false));
            if (actual != null) {
                ComboBuscadorUtil.seleccionarPorPredicado(cmbProyecto, p -> p.getIdProyecto() == actual.getIdProyecto());
            }
            cargarMaterialDelProyecto();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_MATERIAL_PROYECTO", ex);
        }
    }

    private void cargarMaterialDelProyecto() {
        Proyecto p = cmbProyecto.getValue();
        if (p == null) {
            editor.setRenglones(java.util.List.of());
            return;
        }
        try {
            editor.setRenglones(relaciones.listarMaterialDeProyecto(p.getIdProyecto()));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_MATERIAL_PROYECTO", ex);
        }
    }

    @FXML
    private void agregar(ActionEvent event) {
        if (cmbProyecto.getValue() == null) {
            AlertaUtil.advertencia("Selecciona un proyecto", "Elige primero el proyecto.");
            return;
        }
        String aviso = editor.agregarSeleccion();
        if (aviso != null) {
            AlertaUtil.advertencia("Agregar material", aviso);
        }
    }

    @FXML
    private void quitar(ActionEvent event) {
        String aviso = editor.quitarSeleccion();
        if (aviso != null) {
            AlertaUtil.advertencia("Quitar material", aviso);
        }
    }

    @FXML
    private void deshacer(ActionEvent event) {
        cargarMaterialDelProyecto();
    }

    @FXML
    private void guardar(ActionEvent event) {
        Proyecto p = cmbProyecto.getValue();
        if (p == null) {
            AlertaUtil.advertencia("Selecciona un proyecto", "Elige primero el proyecto.");
            return;
        }
        try {
            relaciones.guardarMaterialDeProyecto(p.getIdProyecto(), editor.getRenglones());
            AlertaUtil.info("Material guardado", "Se guardó el material de '" + p.getNombreProyecto()
                    + "' y se ajustaron las existencias del almacén.");
            refrescar(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "GUARDAR_MATERIAL_PROYECTO", ex);
            cargarMaterialDelProyecto();
        }
    }
}
