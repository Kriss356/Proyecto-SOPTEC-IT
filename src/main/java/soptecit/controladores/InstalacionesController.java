package soptecit.controladores;

import com.mycompany.soptec.it.modelo.AvanceProyecto;
import com.mycompany.soptec.it.servicio.ClienteTelefonoService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.modelo.PartidaMaterial;
import com.mycompany.soptec.it.modelo.ClienteTelefono;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.servicio.AvanceProyectoService;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.servicio.RelacionesService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.AppLogger;
import com.mycompany.soptec.it.util.GestorImagenes;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import soptecit.util.Edicion;
import soptecit.util.Mensajes;
import soptecit.util.Tablas;

/**
 * Instalaciones: proyectos en obra y su historial de avances (tabla avance_proyecto).
 *
 * <p>Edición rápida: al hacer clic en un proyecto se abre su último avance ya cargado en el formulario,
 * listo para modificar (avance, estado, técnico, fotos...). También se puede corregir cualquier avance
 * del historial con el lápiz o con doble clic, o registrar uno nuevo partiendo de los datos actuales.</p>
 */
public class InstalacionesController implements Initializable {

    private static final String MODULO = "Instalaciones";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProyectoService proyectoService = new ProyectoService();
    private final EmpleadoService empleadoService = new EmpleadoService();
    private final AvanceProyectoService avanceService = new AvanceProyectoService();
    private final RelacionesService relacionesService = new RelacionesService();

    @FXML private TableView<String[]> tblInstalaciones;
    @FXML private TableView<AvanceProyecto> tblHistorial;
    @FXML private TableColumn<AvanceProyecto, String> colHisFecha;
    @FXML private TableColumn<AvanceProyecto, String> colHisTarea;
    @FXML private TableColumn<AvanceProyecto, String> colHisAvance;
    @FXML private TableColumn<AvanceProyecto, String> colHisHoras;
    @FXML private TableColumn<AvanceProyecto, String> colHisTecnico;
    @FXML private TableColumn<AvanceProyecto, String> colHisObs;
    @FXML private Label lblProyectoHistorial;

    @FXML private Label lblTituloAvance;
    @FXML private Label lblModoAvance;
    @FXML private Label lblSubtituloAvance;
    @FXML private ComboBox<Proyecto> cmbProyectoAvance;
    @FXML private DatePicker dpFechaAvance;
    @FXML private Spinner<Integer> spnHoras;
    @FXML private ComboBox<String> cmbTarea;
    @FXML private ComboBox<Empleado> cmbTecnico;
    @FXML private Slider sldAvance;
    @FXML private Label lblPorcentaje;
    @FXML private Label lblLimiteAvance;
    @FXML private ComboBox<String> cmbEstadoAvance;
    @FXML private DatePicker dpEntregaEstimada;
    @FXML private TextField txtMaterialUsado;
    @FXML private TextArea txtObservacionesAvance;
    @FXML private Label lblEvidencias;
    @FXML private CheckBox chkTerminado;
    @FXML private Button btnNuevoAvance;
    @FXML private Button btnGuardarComoNuevo;
    @FXML private Button btnEliminarAvance;
    @FXML private Button btnGuardarAvance;
    @FXML private Button btnFichaProyecto;

    /** Proyectos de la tabla por folio (así el clic funciona aunque se ordene la tabla). */
    private final Map<String, Proyecto> proyectosPorFolio = new LinkedHashMap<>();
    private Map<Integer, Integer> ultimoPorcentaje = new HashMap<>();

    /** Avance cargado en el formulario; null cuando se está capturando uno nuevo. */
    private AvanceProyecto avanceEditando;
    private final List<String> evidenciasGuardadas = new ArrayList<>();
    private final List<File> fotosNuevas = new ArrayList<>();
    private List<AvanceProyecto> historialProyecto = new ArrayList<>();
    /** Evita que los listeners se disparen mientras el código llena el formulario. */
    private boolean llenando;
    /** Límites del porcentaje para que el avance nunca vaya hacia atrás. */
    private int minimoAvance = 0;
    private int maximoAvance = 100;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaAvance.setValue(LocalDate.now());
        spnHoras.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 24, 8));
        cmbEstadoAvance.setValue("En proceso");

        sldAvance.valueProperty().addListener((o, a, n) -> {
            lblPorcentaje.setText(Math.round(n.doubleValue()) + " %");
            // El avance no se puede regresar: si se mueve por debajo del mínimo, vuelve al mínimo
            if (!llenando) {
                if (n.doubleValue() < minimoAvance) {
                    sldAvance.setValue(minimoAvance);
                    resaltarLimite();
                } else if (n.doubleValue() > maximoAvance) {
                    sldAvance.setValue(maximoAvance);
                    resaltarLimite();
                }
            }
        });
        dpFechaAvance.valueProperty().addListener((o, a, f) -> {
            if (!llenando) {
                calcularLimites();
            }
        });
        chkTerminado.selectedProperty().addListener((o, a, terminado) -> {
            if (terminado && !llenando) {
                sldAvance.setValue(100);
                cmbEstadoAvance.setValue("En revisión");
            }
        });

        inicializarHistorial();

        tblInstalaciones.getSelectionModel().selectedItemProperty().addListener((o, a, fila) -> {
            if (fila != null && !llenando) {
                abrirProyecto(proyectosPorFolio.get(fila[0]));
            }
        });
        cmbProyectoAvance.valueProperty().addListener((o, a, p) -> {
            if (p != null && !llenando) {
                abrirProyecto(p);
            }
        });

        modoNuevo();
        cargarDatos(null);
    }

    private void inicializarHistorial() {
        colHisFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null ? d.getValue().getFecha().format(FECHA) : ""));
        colHisTarea.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTarea()));
        colHisAvance.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPorcentaje() + " %"));
        colHisHoras.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getHoras())));
        colHisTecnico.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEmpleadoNombre() != null ? d.getValue().getEmpleadoNombre() : "—"));
        colHisObs.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getObservaciones() != null ? d.getValue().getObservaciones() : ""));
        tblHistorial.setPlaceholder(new Label("Este proyecto todavía no tiene avances capturados"));
        Edicion.habilitar(tblHistorial, this::cargarAvance, a -> {
            cargarAvance(a);
            eliminarAvance(null);
        });
    }

    // =========================================================================
    // CARGA DE DATOS
    // =========================================================================
    @FXML
    private void cargarDatos(ActionEvent event) {
        Proyecto actual = cmbProyectoAvance.getValue();
        try {
            List<Proyecto> proyectos = proyectoService.listar(false);
            try {
                ultimoPorcentaje = avanceService.ultimoPorcentajePorProyecto();
            } catch (Exception ex) {
                ultimoPorcentaje = new HashMap<>();
            }
            llenando = true;
            cmbProyectoAvance.setItems(FXCollections.observableArrayList(proyectos));
            cmbTecnico.setItems(FXCollections.observableArrayList(empleadoService.listar(false)));

            proyectosPorFolio.clear();
            String[][] filas = new String[proyectos.size()][6];
            for (int i = 0; i < proyectos.size(); i++) {
                Proyecto p = proyectos.get(i);
                String folio = folio(p);
                proyectosPorFolio.put(folio, p);
                filas[i] = new String[]{
                    folio,
                    p.getClienteNombre() != null ? p.getClienteNombre() : "Sin cliente",
                    p.getNombreProyecto(),
                    String.valueOf(porcentajeDe(p)),
                    p.getFechaEstimadaFin() != null ? p.getFechaEstimadaFin().format(FECHA) : "—",
                    p.getEstado() != null ? p.getEstado() : "Planeado"
                };
            }
            Tablas.cargar(tblInstalaciones, filas);
        } catch (Exception ex) {
            AppLogger.registrarExcepcion(MODULO, "InstalacionesController", "CARGAR_DATOS", ex);
            Tablas.cargar(tblInstalaciones, new String[0][0]);
        } finally {
            llenando = false;
        }
        // Con "Refrescar", el proyecto que estaba abierto se vuelve a abrir con los datos recién leídos
        if (actual != null && event != null) {
            Proyecto recargado = proyectosPorFolio.get(folio(actual));
            if (recargado != null) {
                abrirProyecto(recargado);
            }
        }
    }

    /** Abre el proyecto: carga su historial y deja su último avance listo para editar. */
    private void abrirProyecto(Proyecto p) {
        if (p == null) {
            return;
        }
        llenando = true;
        try {
            cmbProyectoAvance.setValue(p);
            seleccionarFilaProyecto(p);
            cmbEstadoAvance.setValue(p.getEstado() != null ? p.getEstado() : "En proceso");
            dpEntregaEstimada.setValue(p.getFechaEstimadaFin());
        } finally {
            llenando = false;
        }
        lblProyectoHistorial.setText(folio(p) + " · " + p.getNombreProyecto()
                + (p.getClienteNombre() != null ? " (" + p.getClienteNombre() + ")" : ""));

        List<AvanceProyecto> historial = cargarHistorial(p);
        if (!historial.isEmpty()) {
            cargarAvance(historial.get(0));
        } else {
            limpiarCamposAvance(porcentajeDe(p));
            calcularLimites();
            proponerTecnico(p);
            modoNuevo();
        }
    }

    /** Proyecto sin avances: se propone como técnico al primer empleado asignado al proyecto. */
    private void proponerTecnico(Proyecto p) {
        try {
            List<Empleado> equipo = relacionesService.listarPersonal(p.getIdProyecto());
            if (!equipo.isEmpty()) {
                cmbTecnico.setValue(buscarTecnico(equipo.get(0).getIdEmpleado()));
            }
        } catch (Exception ex) {
            // Si no se puede consultar el equipo, el técnico se elige a mano
        }
    }

    private List<AvanceProyecto> cargarHistorial(Proyecto p) {
        try {
            List<AvanceProyecto> lista = avanceService.listarPorProyecto(p.getIdProyecto());
            historialProyecto = lista;
            tblHistorial.setItems(FXCollections.observableArrayList(lista));
            return lista;
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_AVANCES", ex);
            historialProyecto = new ArrayList<>();
            tblHistorial.setItems(FXCollections.observableArrayList());
            return new ArrayList<>();
        }
    }

    /** Pone un avance guardado en el formulario para modificarlo. */
    private void cargarAvance(AvanceProyecto a) {
        if (a == null) {
            return;
        }
        avanceEditando = a;
        llenando = true;
        try {
            dpFechaAvance.setValue(a.getFecha());
            spnHoras.getValueFactory().setValue(a.getHoras());
            cmbTarea.setValue(a.getTarea());
            cmbTarea.getEditor().setText(a.getTarea());
            cmbTecnico.setValue(buscarTecnico(a.getIdEmpleado()));
            sldAvance.setValue(a.getPorcentaje());
            if (a.getEstado() != null) {
                cmbEstadoAvance.setValue(a.getEstado());
            }
            txtMaterialUsado.setText(a.getMaterialUsado());
            txtObservacionesAvance.setText(a.getObservaciones());
            chkTerminado.setSelected(false);
            evidenciasGuardadas.clear();
            evidenciasGuardadas.addAll(a.getListaEvidencias());
            fotosNuevas.clear();
            actualizarTextoEvidencias();
        } finally {
            llenando = false;
        }
        tblHistorial.getSelectionModel().select(a);
        calcularLimites();
        modoEdicion(a);
    }

    // =========================================================================
    // ACCIONES DEL FORMULARIO
    // =========================================================================
    /** Pasa a capturar un avance nuevo conservando proyecto, tarea, técnico y porcentaje actuales. */
    @FXML
    private void nuevoAvance(ActionEvent event) {
        avanceEditando = null;
        dpFechaAvance.setValue(LocalDate.now());
        spnHoras.getValueFactory().setValue(8);
        txtMaterialUsado.clear();
        txtObservacionesAvance.clear();
        chkTerminado.setSelected(false);
        evidenciasGuardadas.clear();
        fotosNuevas.clear();
        actualizarTextoEvidencias();
        tblHistorial.getSelectionModel().clearSelection();
        calcularLimites();
        if (sldAvance.getValue() < minimoAvance) {
            sldAvance.setValue(minimoAvance);
        }
        modoNuevo();
        dpFechaAvance.requestFocus();
    }

    @FXML
    private void guardarComoNuevo(ActionEvent event) {
        guardar(true);
    }

    @FXML
    private void guardarAvance(ActionEvent event) {
        guardar(avanceEditando == null);
    }

    private void guardar(boolean esNuevo) {
        Proyecto proyecto = cmbProyectoAvance.getValue();
        Empleado tecnico = cmbTecnico.getValue();
        String tarea = cmbTarea.getEditor().getText() != null ? cmbTarea.getEditor().getText().trim() : "";
        if (proyecto == null || tarea.isEmpty() || tecnico == null) {
            Mensajes.error("Faltan datos", "Selecciona proyecto, tarea realizada y técnico responsable.");
            return;
        }
        String estado = chkTerminado.isSelected()
                ? "En revisión"
                : (cmbEstadoAvance.getValue() != null ? cmbEstadoAvance.getValue() : "En proceso");

        AvanceProyecto a = esNuevo || avanceEditando == null ? new AvanceProyecto() : avanceEditando.copiar();
        a.setIdProyecto(proyecto.getIdProyecto());
        a.setFecha(dpFechaAvance.getValue() != null ? dpFechaAvance.getValue() : LocalDate.now());
        a.setHoras(spnHoras.getValue() != null ? spnHoras.getValue() : 0);
        a.setTarea(tarea);
        a.setIdEmpleado(tecnico.getIdEmpleado());
        a.setEmpleadoNombre(tecnico.getNombre());
        a.setPorcentaje((int) Math.round(sldAvance.getValue()));
        a.setEstado(estado);
        a.setMaterialUsado(textoOpcional(txtMaterialUsado.getText()));
        a.setObservaciones(textoOpcional(txtObservacionesAvance.getText()));

        try {
            List<String> rutas = new ArrayList<>(evidenciasGuardadas);
            for (File foto : fotosNuevas) {
                rutas.add(GestorImagenes.guardarImagen(foto, "avances", MODULO));
            }
            a.setListaEvidencias(rutas);

            if (esNuevo) {
                a.setIdAvance(0);
                avanceService.crear(a);
            } else {
                avanceService.actualizar(a);
            }

            // Estado y fecha de entrega del proyecto (solo si cambiaron)
            boolean cambioProyecto = !Objects.equals(estado, proyecto.getEstado())
                    || !Objects.equals(dpEntregaEstimada.getValue(), proyecto.getFechaEstimadaFin());
            if (cambioProyecto) {
                proyecto.setEstado(estado);
                proyecto.setFechaEstimadaFin(dpEntregaEstimada.getValue());
                proyectoService.actualizar(proyecto);
            }

            String mensaje = esNuevo
                    ? "Se registró el avance del " + a.getPorcentaje() + " % del proyecto " + folio(proyecto) + "."
                    : "Se guardaron los cambios del avance del " + a.getFecha().format(FECHA) + ".";
            if (chkTerminado.isSelected()) {
                mensaje += "\nEl proyecto pasó a 'En revisión' para que Administración lo cierre.";
            }
            Mensajes.info(esNuevo ? "Avance guardado" : "Cambios guardados", mensaje);

            int idGuardado = a.getIdAvance();
            cargarDatos(null);
            Proyecto recargado = proyectosPorFolio.getOrDefault(folio(proyecto), proyecto);
            abrirProyecto(recargado);
            // Deja abierto justamente el avance que se acaba de guardar
            for (AvanceProyecto h : tblHistorial.getItems()) {
                if (h.getIdAvance() == idGuardado) {
                    cargarAvance(h);
                    break;
                }
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, esNuevo ? "CREAR_AVANCE" : "ACTUALIZAR_AVANCE", ex);
        }
    }

    @FXML
    private void eliminarAvance(ActionEvent event) {
        AvanceProyecto a = avanceEditando != null ? avanceEditando : tblHistorial.getSelectionModel().getSelectedItem();
        if (a == null) {
            Mensajes.advertencia("Selecciona un avance", "Elige en el historial el avance que quieres eliminar.");
            return;
        }
        if (!Mensajes.confirmar("Eliminar avance",
                "¿Eliminar el avance del " + (a.getFecha() != null ? a.getFecha().format(FECHA) : "")
                + " (" + a.getTarea() + ", " + a.getPorcentaje() + " %)?\nEsta acción no se puede deshacer.")) {
            return;
        }
        try {
            avanceService.eliminar(a.getIdAvance());
            Mensajes.info("Avance eliminado", "El avance se quitó del historial.");
            Proyecto p = cmbProyectoAvance.getValue();
            cargarDatos(null);
            if (p != null) {
                abrirProyecto(proyectosPorFolio.getOrDefault(folio(p), p));
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ELIMINAR_AVANCE", ex);
        }
    }

    @FXML
    private void adjuntarEvidencia(ActionEvent event) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar fotos de evidencia");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        List<File> fotos = selector.showOpenMultipleDialog(lblEvidencias.getScene().getWindow());
        if (fotos != null && !fotos.isEmpty()) {
            fotosNuevas.addAll(fotos);
            actualizarTextoEvidencias();
        }
    }

    /** Todo lo que el técnico necesita del proyecto en un solo lugar (sin ir a otras áreas). */
    @FXML
    private void verFichaProyecto(ActionEvent event) {
        Proyecto p = cmbProyectoAvance.getValue();
        if (p == null) {
            Mensajes.advertencia("Elige un proyecto", "Haz clic en un proyecto de la lista para ver su ficha.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Proyecto: ").append(p.getNombreProyecto()).append(" (").append(folio(p)).append(")\n");
        sb.append("Estado: ").append(p.getEstado() != null ? p.getEstado() : "—");
        if (p.getFechaEstimadaFin() != null) {
            sb.append("   ·   Entrega estimada: ").append(p.getFechaEstimadaFin().format(FECHA));
        }
        sb.append("\n\nCLIENTE\n").append(p.getClienteNombre() != null ? p.getClienteNombre() : "—");
        try {
            Cliente cli = new ClienteService().consultarPorId(p.getIdCliente());
            if (cli != null) {
                List<ClienteTelefono> tels = new ClienteTelefonoService().listarPorCliente(cli.getIdCliente(), false);
                StringBuilder t = new StringBuilder();
                for (ClienteTelefono ct : tels) {
                    t.append(t.length() > 0 ? ", " : "").append(ct.getTelefono());
                }
                sb.append("\nTeléfono: ").append(t.length() > 0 ? t : "sin teléfono registrado");
                String dir = cli.getDireccionResumen();
                sb.append("\nDirección: ").append(dir != null && !dir.isBlank() ? dir : "sin dirección registrada");
                if (cli.getEstado() != null && !cli.getEstado().isBlank()) {
                    sb.append(", ").append(cli.getEstado());
                }
            }
        } catch (Exception ex) {
            sb.append("\n(No se pudieron leer los datos de contacto)");
        }
        try {
            List<Empleado> equipo = relacionesService.listarPersonal(p.getIdProyecto());
            sb.append("\n\nEQUIPO ASIGNADO\n");
            if (equipo.isEmpty()) {
                sb.append("Sin personal asignado (se asigna en Administración › Personal por proyecto)");
            }
            for (Empleado e : equipo) {
                sb.append("• ").append(e.toString()).append("\n");
            }
        } catch (Exception ex) {
            sb.append("\n(No se pudo leer el equipo asignado)");
        }
        try {
            List<PartidaMaterial> material = relacionesService.listarMaterialDeProyecto(p.getIdProyecto());
            sb.append("\nMATERIAL ENTREGADO AL PROYECTO\n");
            if (material.isEmpty()) {
                sb.append("Todavía no se ha entregado material (se registra en Compras-Almacén › Entradas y salidas)");
            }
            for (PartidaMaterial m : material) {
                sb.append("• ").append(m.getCantidad()).append(" × ").append(m.getDescripcionMaterial()).append("\n");
            }
        } catch (Exception ex) {
            sb.append("\n(No se pudo leer el material del proyecto)");
        }
        Mensajes.info("Ficha del proyecto " + folio(p), sb.toString().trim());
    }

    // =========================================================================
    // AUXILIARES
    // =========================================================================
    private void modoNuevo() {
        lblModoAvance.setText("Nuevo avance");
        lblModoAvance.getStyleClass().remove("chip-modo-editando");
        lblTituloAvance.setText("Capturar avance");
        lblSubtituloAvance.setText(cmbProyectoAvance.getValue() == null
                ? "Selecciona un proyecto de la lista para empezar"
                : "Registra el trabajo realizado en el día");
        btnGuardarAvance.setText("Guardar avance");
        Edicion.modo(false, lblTituloAvance, null, null, btnEliminarAvance);
        Edicion.mostrar(btnGuardarComoNuevo, false);
    }

    private void modoEdicion(AvanceProyecto a) {
        String fecha = a.getFecha() != null ? a.getFecha().format(FECHA) : "";
        boolean esUltimo = !tblHistorial.getItems().isEmpty()
                && tblHistorial.getItems().get(0).getIdAvance() == a.getIdAvance();
        lblModoAvance.setText("Editando · " + fecha);
        if (!lblModoAvance.getStyleClass().contains("chip-modo-editando")) {
            lblModoAvance.getStyleClass().add("chip-modo-editando");
        }
        lblTituloAvance.setText(esUltimo ? "Último avance" : "Editar avance");
        lblSubtituloAvance.setText("Cambia lo que necesites y presiona \"Guardar cambios\". "
                + "Para capturar el trabajo de otro día usa \"Nuevo avance\".");
        btnGuardarAvance.setText("Guardar cambios");
        Edicion.modo(true, lblTituloAvance, null, null, btnEliminarAvance);
        Edicion.mostrar(btnGuardarComoNuevo, true);
    }

    /** Limpia el formulario para un proyecto sin avances (porcentaje inicial según su estado). */
    private void limpiarCamposAvance(int porcentajeInicial) {
        avanceEditando = null;
        llenando = true;
        try {
            dpFechaAvance.setValue(LocalDate.now());
            spnHoras.getValueFactory().setValue(8);
            cmbTarea.setValue(null);
            cmbTarea.getEditor().clear();
            cmbTecnico.setValue(null);
            sldAvance.setValue(porcentajeInicial);
            txtMaterialUsado.clear();
            txtObservacionesAvance.clear();
            chkTerminado.setSelected(false);
            evidenciasGuardadas.clear();
            fotosNuevas.clear();
            actualizarTextoEvidencias();
        } finally {
            llenando = false;
        }
    }

    /** Calcula entre qué porcentajes puede quedar el avance según los avances anteriores y siguientes. */
    private void calcularLimites() {
        int id = avanceEditando != null ? avanceEditando.getIdAvance() : 0;
        int[] lim = AvanceProyectoService.limites(new ArrayList<>(historialCompleto()), id, dpFechaAvance.getValue());
        minimoAvance = lim[0];
        maximoAvance = lim[1];
        if (minimoAvance == 0 && maximoAvance == 100) {
            lblLimiteAvance.setText("El avance solo puede subir: no puede ser menor al último capturado.");
        } else if (maximoAvance == 100) {
            lblLimiteAvance.setText("Mínimo " + minimoAvance + " %: el avance anterior ya iba en " + minimoAvance
                    + " % y no puede ir hacia atrás.");
        } else {
            lblLimiteAvance.setText("Debe quedar entre " + minimoAvance + " % y " + maximoAvance
                    + " % (avance anterior y avance siguiente).");
        }
        lblLimiteAvance.getStyleClass().remove("texto-alerta");
    }

    private void resaltarLimite() {
        if (!lblLimiteAvance.getStyleClass().contains("texto-alerta")) {
            lblLimiteAvance.getStyleClass().add("texto-alerta");
        }
    }

    /** Todos los avances del proyecto abierto (aunque la tabla esté filtrada). */
    private List<AvanceProyecto> historialCompleto() {
        return historialProyecto;
    }

    private void actualizarTextoEvidencias() {
        int total = evidenciasGuardadas.size() + fotosNuevas.size();
        lblEvidencias.setText(total == 0 ? "Sin evidencias adjuntas"
                : total + (total == 1 ? " foto adjunta" : " fotos adjuntas"));
        List<String> nombres = new ArrayList<>();
        for (String r : evidenciasGuardadas) {
            nombres.add(new File(r).getName());
        }
        for (File f : fotosNuevas) {
            nombres.add(f.getName() + " (nueva)");
        }
        lblEvidencias.setTooltip(nombres.isEmpty() ? null : new Tooltip(String.join("\n", nombres)));
    }

    private void seleccionarFilaProyecto(Proyecto p) {
        String folio = folio(p);
        for (String[] fila : tblInstalaciones.getItems()) {
            if (fila.length > 0 && folio.equals(fila[0])) {
                tblInstalaciones.getSelectionModel().select(fila);
                return;
            }
        }
    }

    private Empleado buscarTecnico(Integer idEmpleado) {
        if (idEmpleado == null) {
            return null;
        }
        for (Empleado e : cmbTecnico.getItems()) {
            if (e.getIdEmpleado() == idEmpleado) {
                return e;
            }
        }
        return null;
    }

    private int porcentajeDe(Proyecto p) {
        Integer pct = ultimoPorcentaje.get(p.getIdProyecto());
        return pct != null ? pct : InicioController.porcentajePorEstado(p.getEstado());
    }

    private static String folio(Proyecto p) {
        return "PRY-" + String.format("%04d", p.getIdProyecto());
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
