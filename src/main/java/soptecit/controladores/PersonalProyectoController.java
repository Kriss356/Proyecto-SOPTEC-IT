package soptecit.controladores;

import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.servicio.RelacionesService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.ComboBuscadorUtil;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
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
 * Personal por proyecto: administra en bloque la relación muchos a muchos proyecto_empleado.
 */
public class PersonalProyectoController implements Initializable {

    private static final String MODULO = "PersonalProyecto";

    private final ProyectoService proyectoService = new ProyectoService();
    private final EmpleadoService empleadoService = new EmpleadoService();
    private final RelacionesService relaciones = new RelacionesService();

    @FXML private TextField txtFiltroProyecto;
    @FXML private ComboBox<Proyecto> cmbProyecto;
    @FXML private Label lblInfoProyecto;
    @FXML private TableView<Empleado> tblAsignados;
    @FXML private TableColumn<Empleado, String> colAsigNombre;
    @FXML private TableColumn<Empleado, String> colAsigPuesto;
    @FXML private TableColumn<Empleado, String> colAsigTelefono;
    @FXML private TableColumn<Empleado, String> colAsigCorreo;
    @FXML private ListView<Empleado> lstEmpleados;
    @FXML private Label lblMarcados;

    private ListaCasillas<Empleado> casillas;
    private Set<Integer> asignadosGuardados = new HashSet<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colAsigNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colAsigPuesto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPuesto()));
        colAsigTelefono.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTelefono()));
        colAsigCorreo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCorreo()));
        tblAsignados.setPlaceholder(new Label("El proyecto no tiene personal asignado"));
        casillas = new ListaCasillas<>(lstEmpleados, Empleado::toString, lblMarcados, "empleado(s)");
        cmbProyecto.valueProperty().addListener((o, a, p) -> cargarAsignados());
        refrescar(null);
    }

    @FXML
    private void refrescar(ActionEvent event) {
        try {
            Proyecto actual = cmbProyecto.getValue();
            ComboBuscadorUtil.configurar(cmbProyecto, txtFiltroProyecto, proyectoService.listar(false), Proyecto::toString);
            casillas.setElementos(empleadoService.listar(false));
            if (actual != null) {
                ComboBuscadorUtil.seleccionarPorPredicado(cmbProyecto, p -> p.getIdProyecto() == actual.getIdProyecto());
            }
            cargarAsignados();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_PERSONAL", ex);
        }
    }

    private void cargarAsignados() {
        Proyecto p = cmbProyecto.getValue();
        if (p == null) {
            tblAsignados.setItems(FXCollections.observableArrayList());
            asignadosGuardados = new HashSet<>();
            casillas.marcarSolo(e -> false);
            lblInfoProyecto.setText("Sin proyecto seleccionado");
            return;
        }
        try {
            List<Empleado> equipo = relaciones.listarPersonal(p.getIdProyecto());
            tblAsignados.setItems(FXCollections.observableArrayList(equipo));
            asignadosGuardados = equipo.stream().map(Empleado::getIdEmpleado).collect(Collectors.toSet());
            casillas.marcarSolo(e -> asignadosGuardados.contains(e.getIdEmpleado()));
            lblInfoProyecto.setText("PRY-" + String.format("%04d", p.getIdProyecto()) + " · Cliente: "
                    + p.getClienteDescriptivo() + " · Estado: " + p.getEstado() + " · " + equipo.size() + " asignado(s)");
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_PERSONAL", ex);
        }
    }

    @FXML
    private void guardarEquipo(ActionEvent event) {
        Proyecto p = cmbProyecto.getValue();
        if (p == null) {
            AlertaUtil.advertencia("Selecciona un proyecto", "Elige primero el proyecto al que vas a asignar personal.");
            return;
        }
        try {
            List<Integer> ids = casillas.getMarcados().stream().map(Empleado::getIdEmpleado).collect(Collectors.toList());
            relaciones.guardarPersonal(p.getIdProyecto(), ids);
            AlertaUtil.info("Equipo guardado", "El proyecto '" + p.getNombreProyecto() + "' quedó con "
                    + ids.size() + " empleado(s) asignado(s).");
            cargarAsignados();
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "GUARDAR_PERSONAL_PROYECTO", ex);
            cargarAsignados();
        }
    }

    @FXML
    private void deshacer(ActionEvent event) {
        casillas.marcarSolo(e -> asignadosGuardados.contains(e.getIdEmpleado()));
    }

    @FXML
    private void quitarTodos(ActionEvent event) {
        casillas.marcarSolo(e -> false);
    }
}
