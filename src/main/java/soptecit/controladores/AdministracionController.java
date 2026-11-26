package soptecit.controladores;

import com.mycompany.soptec.it.servicio.CascadaService;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.AppLogger;
import com.mycompany.soptec.it.util.GestorImagenes;
import com.mycompany.soptec.it.util.Validador;
import java.net.URL;
import java.text.NumberFormat;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import soptecit.util.Edicion;
import soptecit.util.Mensajes;
import soptecit.util.Tablas;

/**
 * Controlador estilizado de Administración.
 * Integra CRUD completo de Empleados y gestión de expedientes/cierre de Proyectos en MySQL.
 */
public class AdministracionController implements Initializable {

    private static final String MOD_EMP = "Empleados";
    private static final String MOD_ADM = "Administracion";
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    private final EmpleadoService empleadoService = new EmpleadoService();
    private final CascadaService cascada = new CascadaService();
    private final ProyectoService proyectoService = new ProyectoService();

    // ---- 1. Empleados ----
    // Botones del formulario de tblEmpleados (modo nuevo / editando)
    @FXML private Button btnGuardarEmpleado;
    @FXML private Button btnActualizarEmpleado;
    @FXML private Button btnDesactivarEmpleado;
    @FXML private Button btnReactivarEmpleado;
    @FXML private Button btnEliminarEmpleado;
    @FXML private CheckBox chkInactivosEmp;
    @FXML private TextField txtBuscarEmp;
    @FXML private TextField txtConsultarIdEmp;
    @FXML private TableView<Empleado> tblEmpleados;
    @FXML private TableColumn<Empleado, String> colEmpId;
    @FXML private TableColumn<Empleado, String> colEmpNombre;
    @FXML private TableColumn<Empleado, String> colEmpPuesto;
    @FXML private TableColumn<Empleado, String> colEmpTelefono;
    @FXML private TableColumn<Empleado, String> colEmpCorreo;
    @FXML private TableColumn<Empleado, String> colEmpEstado;

    @FXML private Label lblModoEmpleado;
    @FXML private TextField txtNombreEmp;
    @FXML private ComboBox<String> cmbPuestoEmp;
    @FXML private TextField txtTelefonoEmp;
    @FXML private TextField txtCorreoEmp;
    @FXML private ImageView imgEmpleado;
    @FXML private Label lblRutaImgEmp;

    private Empleado empleadoSeleccionado;
    private String rutaImgEmpleado;

    // ---- 2. Revisión de propuestas ----
    @FXML private TableView<Proyecto> tblExpedientes;
    @FXML private TableColumn<Proyecto, String> colExpId;
    @FXML private TableColumn<Proyecto, String> colExpCliente;
    @FXML private TableColumn<Proyecto, String> colExpNombre;
    @FXML private TableColumn<Proyecto, String> colExpCosto;
    @FXML private TableColumn<Proyecto, String> colExpFecha;
    @FXML private TableColumn<Proyecto, String> colExpEstado;
    @FXML private Label lblFolioExp;
    @FXML private Label lblClienteExp;
    @FXML private Label lblServicioExp;
    @FXML private Label lblMontoExp;
    @FXML private TextArea txtComentariosExp;

    // ---- 3. Cuentas por cobrar: ahora en CuentasCobrarController (pestaña incluida con fx:include) ----

    // ---- 4. Revisión final ----
    @FXML private TableView<Proyecto> tblRevisionFinal;
    @FXML private TableColumn<Proyecto, String> colRevId;
    @FXML private TableColumn<Proyecto, String> colRevProyecto;
    @FXML private TableColumn<Proyecto, String> colRevCliente;
    @FXML private TableColumn<Proyecto, String> colRevFin;
    @FXML private TableColumn<Proyecto, String> colRevEstado;
    @FXML private Label lblProyectoRevision;
    @FXML private CheckBox chkPruebas;
    @FXML private CheckBox chkCertificacion;
    @FXML private CheckBox chkEtiquetado;
    @FXML private CheckBox chkLimpieza;
    @FXML private CheckBox chkSobrante;
    @FXML private CheckBox chkActa;
    @FXML private CheckBox chkPagos;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        inicializarEmpleados();
        inicializarExpedientesYRevision();
        cargarTablaEmpleados(null);
        cargarExpedientes(null);
    }

    // =========================================================================
    // 1. EMPLEADOS (CRUD COMPLETO)
    // =========================================================================
    private void inicializarEmpleados() {
        cmbPuestoEmp.getItems().addAll("Administrador general", "Administrador", "Agente de Ventas", "Técnico de Levantamiento",
                "Técnico Instalador", "Compras y Almacén");

        colEmpId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getIdEmpleado())));
        colEmpNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colEmpPuesto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPuesto()));
        colEmpTelefono.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTelefono()));
        colEmpCorreo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCorreo()));
        colEmpEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstadoTexto()));
        colEmpEstado.setCellFactory(c -> Tablas.crearCeldaEstado());
        tblEmpleados.setPlaceholder(new Label("No hay empleados registrados"));

        tblEmpleados.getSelectionModel().selectedItemProperty().addListener((obs, a, nuevo) -> {
            if (nuevo != null) {
                cargarEmpleadoEnFormulario(nuevo);
            }
        });
        // Edición rápida: lápiz en cada fila, doble clic o Enter
        Edicion.habilitar(tblEmpleados, this::cargarEmpleadoEnFormulario);
        Edicion.modo(false, lblModoEmpleado, btnGuardarEmpleado, btnActualizarEmpleado, btnDesactivarEmpleado, btnReactivarEmpleado, btnEliminarEmpleado);
    }

    @FXML
    private void cargarTablaEmpleados(ActionEvent event) {
        try {
            tblEmpleados.setItems(FXCollections.observableArrayList(
                    empleadoService.listar(chkInactivosEmp.isSelected())));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_EMP, "LISTAR_EMPLEADOS", ex);
        }
    }

    @FXML
    private void buscarEmpleados(ActionEvent event) {
        try {
            tblEmpleados.setItems(FXCollections.observableArrayList(
                    empleadoService.buscar(txtBuscarEmp.getText(), chkInactivosEmp.isSelected())));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_EMP, "BUSCAR_EMPLEADOS", ex);
        }
    }

    @FXML
    private void consultarEmpleadoPorId(ActionEvent event) {
        try {
            int id = Validador.parsearIdPositivo(txtConsultarIdEmp.getText(), "ID de empleado", MOD_EMP, "CONSULTAR_POR_ID");
            Empleado emp = empleadoService.consultarPorId(id);
            if (emp == null) {
                AlertaUtil.advertencia("No encontrado", "No existe un empleado con el ID " + id + ".");
                return;
            }
            cargarEmpleadoEnFormulario(emp);
            AlertaUtil.info("Empleado encontrado",
                    "Nombre: " + emp.getNombre()
                    + "\nPuesto: " + emp.getPuesto()
                    + "\nTeléfono: " + emp.getTelefono()
                    + "\nCorreo: " + emp.getCorreo()
                    + "\nEstado: " + emp.getEstadoTexto());
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_EMP, "CONSULTAR_POR_ID", ex);
        }
    }

    @FXML
    private void seleccionarImagenEmpleado(ActionEvent event) {
        try {
            String ruta = GestorImagenes.elegirYGuardarImagen(
                    imgEmpleado.getScene().getWindow(), "Seleccionar fotografía de empleado", "empleados", MOD_EMP);
            if (ruta != null) {
                rutaImgEmpleado = ruta;
                lblRutaImgEmp.setText(ruta);
                imgEmpleado.setImage(GestorImagenes.cargarImagen(ruta));
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_EMP, "SELECCIONAR_IMAGEN", ex);
        }
    }

    @FXML
    private void quitarImagenEmpleado(ActionEvent event) {
        rutaImgEmpleado = null;
        lblRutaImgEmp.setText("Sin imagen seleccionada");
        imgEmpleado.setImage(null);
    }

    @FXML
    private void guardarEmpleado(ActionEvent event) {
        try {
            Empleado creado = empleadoService.crear(construirEmpleadoDesdeFormulario(new Empleado()));
            AlertaUtil.info("Empleado registrado", "Se guardó a " + creado.getNombre() + ".");
            limpiarEmpleado(null);
            cargarTablaEmpleados(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_EMP, "CREAR_EMPLEADO", ex);
        }
    }

    @FXML
    private void actualizarEmpleado(ActionEvent event) {
        if (empleadoSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un empleado", "Elige un empleado de la tabla para actualizarlo.");
            return;
        }
        try {
            Empleado editado = construirEmpleadoDesdeFormulario(empleadoSeleccionado);
            empleadoService.actualizar(editado);
            AlertaUtil.info("Empleado actualizado", "Se actualizó la información de " + editado.getNombre() + ".");
            limpiarEmpleado(null);
            cargarTablaEmpleados(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_EMP, "ACTUALIZAR_EMPLEADO", ex);
        }
    }

    @FXML
    private void desactivarEmpleado(ActionEvent event) {
        if (empleadoSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un empleado", "Elige un empleado de la tabla.");
            return;
        }
        String nombre = empleadoSeleccionado.getNombre();
        try {
            int proyectos = cascada.proyectosDeEmpleado(empleadoSeleccionado.getIdEmpleado());
            if (proyectos > 0) {
                int r = AlertaUtil.elegir("Desactivar empleado", nombre + " está asignado a " + proyectos
                        + (proyectos == 1 ? " proyecto activo" : " proyectos activos")
                        + ".\n¿Quieres quitarlo también de esos proyectos?",
                        "Desactivar y quitar de proyectos", "Solo desactivar", "Cancelar");
                if (r < 0 || r == 2) {
                    return;
                }
                if (r == 0) {
                    // Respeta la regla del Administrador general único
                    empleadoService.desactivar(empleadoSeleccionado.getIdEmpleado());
                    cascada.desactivarEmpleadoYQuitarDeProyectos(empleadoSeleccionado.getIdEmpleado());
                    AlertaUtil.info("Empleado desactivado", nombre + " quedó inactivo y se quitó de sus proyectos.");
                    limpiarEmpleado(null);
                    cargarTablaEmpleados(null);
                    return;
                }
            }
            empleadoService.desactivar(empleadoSeleccionado.getIdEmpleado());
            AlertaUtil.info("Empleado desactivado", "El empleado quedó inactivo.");
            limpiarEmpleado(null);
            cargarTablaEmpleados(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_EMP, "DESACTIVAR_EMPLEADO", ex);
        }
    }

    @FXML
    private void reactivarEmpleado(ActionEvent event) {
        if (empleadoSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un empleado", "Marca 'Mostrar inactivos' y elige un empleado.");
            return;
        }
        try {
            empleadoService.reactivar(empleadoSeleccionado.getIdEmpleado());
            AlertaUtil.info("Empleado reactivado", "El empleado volvió a estar activo.");
            limpiarEmpleado(null);
            cargarTablaEmpleados(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_EMP, "REACTIVAR_EMPLEADO", ex);
        }
    }

    @FXML
    private void eliminarFisicoEmpleado(ActionEvent event) {
        if (empleadoSeleccionado == null) {
            AlertaUtil.advertencia("Selecciona un empleado", "Elige un empleado de la tabla.");
            return;
        }
        if (!AlertaUtil.confirmar("Eliminar empleado definitivamente",
                "¿Eliminar permanentemente a " + empleadoSeleccionado.getNombre() + "?")) {
            return;
        }
        try {
            empleadoService.eliminarFisico(empleadoSeleccionado.getIdEmpleado());
            AlertaUtil.info("Empleado eliminado", "El registro fue eliminado de la base de datos.");
            limpiarEmpleado(null);
            cargarTablaEmpleados(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_EMP, "ELIMINAR_FISICO_EMPLEADO", ex);
        }
    }

    @FXML
    private void limpiarEmpleado(ActionEvent event) {
        empleadoSeleccionado = null;
        lblModoEmpleado.setText("Registrar empleado");
        txtNombreEmp.clear();
        txtTelefonoEmp.clear();
        cmbPuestoEmp.setValue(null);
        cmbPuestoEmp.getEditor().clear();
        txtCorreoEmp.clear();
        quitarImagenEmpleado(null);
        tblEmpleados.getSelectionModel().clearSelection();
        Edicion.modo(false, lblModoEmpleado, btnGuardarEmpleado, btnActualizarEmpleado, btnDesactivarEmpleado, btnReactivarEmpleado, btnEliminarEmpleado);
    }

    private void cargarEmpleadoEnFormulario(Empleado e) {
        empleadoSeleccionado = e;
        lblModoEmpleado.setText("Editando Empleado #" + e.getIdEmpleado());
        txtNombreEmp.setText(e.getNombre());
        txtTelefonoEmp.setText(e.getTelefono());
        cmbPuestoEmp.setValue(e.getPuesto());
        cmbPuestoEmp.getEditor().setText(e.getPuesto());
        txtCorreoEmp.setText(e.getCorreo());
        rutaImgEmpleado = e.getImagen();
        if (rutaImgEmpleado != null && !rutaImgEmpleado.isBlank()) {
            lblRutaImgEmp.setText(rutaImgEmpleado);
            imgEmpleado.setImage(GestorImagenes.cargarImagen(rutaImgEmpleado));
        } else {
            quitarImagenEmpleado(null);
        }
        Edicion.modo(true, lblModoEmpleado, btnGuardarEmpleado, btnActualizarEmpleado, btnDesactivarEmpleado, btnReactivarEmpleado, btnEliminarEmpleado);
    }

    private Empleado construirEmpleadoDesdeFormulario(Empleado base) {
        base.setNombre(txtNombreEmp.getText());
        base.setTelefono(txtTelefonoEmp.getText());
        String puesto = cmbPuestoEmp.getEditor().getText();
        if ((puesto == null || puesto.isBlank()) && cmbPuestoEmp.getValue() != null) {
            puesto = cmbPuestoEmp.getValue();
        }
        base.setPuesto(puesto);
        base.setCorreo(txtCorreoEmp.getText());
        base.setImagen(rutaImgEmpleado);
        return base;
    }

    // =========================================================================
    // 2 y 4. EXPEDIENTES Y REVISIÓN FINAL (CONECTADOS A PROYECTOS)
    // =========================================================================
    private void inicializarExpedientesYRevision() {
        colExpId.setCellValueFactory(d -> new SimpleStringProperty("PRY-" + String.format("%04d", d.getValue().getIdProyecto())));
        colExpCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClienteDescriptivo()));
        colExpNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreProyecto()));
        colExpCosto.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getCostoTotal())));
        colExpFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaInicio() != null ? d.getValue().getFechaInicio().toString() : ""));
        colExpEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        colExpEstado.setCellFactory(c -> Tablas.crearCeldaEstado());

        tblExpedientes.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> {
            if (p != null) {
                lblFolioExp.setText("PRY-" + String.format("%04d", p.getIdProyecto()));
                lblClienteExp.setText(p.getClienteDescriptivo());
                lblServicioExp.setText(p.getNombreProyecto());
                lblMontoExp.setText(MONEDA.format(p.getCostoTotal()) + " + IVA");
            }
        });

        colRevId.setCellValueFactory(d -> new SimpleStringProperty("PRY-" + String.format("%04d", d.getValue().getIdProyecto())));
        colRevProyecto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreProyecto()));
        colRevCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClienteDescriptivo()));
        colRevFin.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaEstimadaFin() != null ? d.getValue().getFechaEstimadaFin().toString() : "—"));
        colRevEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        colRevEstado.setCellFactory(c -> Tablas.crearCeldaEstado());

        tblRevisionFinal.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> {
            if (p != null) {
                lblProyectoRevision.setText("PRY-" + String.format("%04d", p.getIdProyecto()) + " · " + p.getNombreProyecto());
            }
        });
    }

    @FXML
    private void cargarExpedientes(ActionEvent event) {
        try {
            List<Proyecto> proyectos = proyectoService.listar(false);
            tblExpedientes.setItems(FXCollections.observableArrayList(proyectos));
            tblRevisionFinal.setItems(FXCollections.observableArrayList(proyectos));
        } catch (Exception ex) {
            AppLogger.registrarExcepcion(MOD_ADM, "AdministracionController", "CARGAR_EXPEDIENTES", ex);
        }
    }

    @FXML
    private void aprobarPropuesta(ActionEvent event) {
        Proyecto p = tblExpedientes.getSelectionModel().getSelectedItem();
        if (p == null) {
            Mensajes.info("Selecciona un expediente", "Elige el proyecto que deseas aprobar.");
            return;
        }
        try {
            p.setEstado("En proceso");
            proyectoService.actualizar(p);
            Mensajes.info("Proyecto aprobado",
                    "El proyecto '" + p.getNombreProyecto() + "' pasó a estado 'En proceso' en MySQL.");
            cargarExpedientes(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_ADM, "APROBAR_PROYECTO", ex);
        }
    }

    @FXML
    private void rechazarPropuesta(ActionEvent event) {
        Proyecto p = tblExpedientes.getSelectionModel().getSelectedItem();
        if (p == null) {
            Mensajes.info("Selecciona un expediente", "Elige el proyecto que deseas rechazar.");
            return;
        }
        if (!Mensajes.confirmar("Rechazar proyecto", "¿Marcar como 'Cancelado' el proyecto '" + p.getNombreProyecto() + "'?")) {
            return;
        }
        try {
            p.setEstado("Cancelado");
            proyectoService.actualizar(p);
            Mensajes.info("Proyecto rechazado", "El proyecto cambió a estado 'Cancelado' en MySQL.");
            cargarExpedientes(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_ADM, "RECHAZAR_PROYECTO", ex);
        }
    }

    @FXML
    private void cerrarProyecto(ActionEvent event) {
        Proyecto p = tblRevisionFinal.getSelectionModel().getSelectedItem();
        if (p == null) {
            Mensajes.advertencia("Selecciona un proyecto", "Elige un proyecto de la tabla de revisión final.");
            return;
        }
        CheckBox[] lista = {chkPruebas, chkCertificacion, chkEtiquetado, chkLimpieza, chkSobrante, chkActa, chkPagos};
        for (CheckBox c : lista) {
            if (!c.isSelected()) {
                Mensajes.error("Revisión incompleta", "Falta validar: " + c.getText());
                return;
            }
        }
        try {
            p.setEstado("Finalizado");
            proyectoService.actualizar(p);
            Mensajes.info("Proyecto cerrado", "El proyecto '" + p.getNombreProyecto() + "' se marcó como Finalizado en MySQL.");
            for (CheckBox c : lista) {
                c.setSelected(false);
            }
            cargarExpedientes(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_ADM, "CERRAR_PROYECTO", ex);
        }
    }

    @FXML
    private void regresarAInstalaciones(ActionEvent event) {
        Proyecto p = tblRevisionFinal.getSelectionModel().getSelectedItem();
        if (p == null) {
            Mensajes.advertencia("Selecciona un proyecto", "Elige un proyecto de la tabla.");
            return;
        }
        try {
            p.setEstado("En proceso");
            proyectoService.actualizar(p);
            Mensajes.info("Regresado a Instalaciones", "El proyecto '" + p.getNombreProyecto() + "' volvió a estado 'En proceso'.");
            cargarExpedientes(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MOD_ADM, "REGRESAR_INSTALACIONES", ex);
        }
    }
}
