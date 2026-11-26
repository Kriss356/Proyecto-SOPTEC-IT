package soptecit.controladores;

import com.mycompany.soptec.it.servicio.CascadaService;
import com.mycompany.soptec.it.util.GestorDocumentos;
import javafx.stage.FileChooser;
import java.io.File;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Levantamiento;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.servicio.LevantamientoService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.ComboBuscadorUtil;
import com.mycompany.soptec.it.util.GestorImagenes;
import com.mycompany.soptec.it.util.Validador;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import soptecit.util.Edicion;
import soptecit.util.Tablas;

/**
 * Controlador estilizado del módulo Levantamientos conectado a MySQL y GestorImagenes.
 */
public class LevantamientosController implements Initializable {

    private static final String MODULO = "Levantamientos";
    private final LevantamientoService service = new LevantamientoService();
    private final CascadaService cascada = new CascadaService();
    private final ProyectoService proyectoService = new ProyectoService();
    private final CitaService citaService = new CitaService();
    private final ClienteService clienteService = new ClienteService();

    // Botones del formulario de tblLevantamientos (modo nuevo / editando)
    @FXML private Button btnGuardarLev;
    @FXML private Button btnActualizarLev;
    @FXML private Button btnDesactivarLev;
    @FXML private Button btnReactivarLev;
    @FXML private Button btnEliminarLev;
    @FXML private CheckBox chkInactivos;
    @FXML private TextField txtBuscar;
    @FXML private TextField txtConsultarId;
    @FXML private TableView<Levantamiento> tblLevantamientos;
    @FXML private TableColumn<Levantamiento, String> colId;
    @FXML private TableColumn<Levantamiento, String> colFecha;
    @FXML private TableColumn<Levantamiento, String> colProyecto;
    @FXML private TableColumn<Levantamiento, String> colCita;
    @FXML private TableColumn<Levantamiento, String> colObs;
    @FXML private TableColumn<Levantamiento, String> colDoc;
    @FXML private Label lblDocumentoLev;
    @FXML private Button btnCargarDocumento;
    @FXML private Button btnAbrirDocumento;
    @FXML private Button btnQuitarDocumento;
    /** Ruta (relativa a Documentos/SOPTEC-IT/documentos) del documento del técnico. */
    private String rutaDocumentoActual;
    @FXML private TableColumn<Levantamiento, String> colActivo;

    @FXML private StackPane panelCroquis;
    @FXML private VBox vacioCroquis;
    @FXML private ImageView imgCroquis;
    @FXML private Label lblRutaCroquis;

    @FXML private Label lblModoFormulario;
    @FXML private Label lblInfoProyectoLev;
    @FXML private TextField txtFiltroProyecto;
    @FXML private ComboBox<Proyecto> cmbProyecto;
    @FXML private ComboBox<Cita> cmbCita;
    @FXML private DatePicker dpFechaVisita;
    @FXML private TextArea txtObservacionesLev;

    private Levantamiento seleccionado;
    private String rutaCroquisActual;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaVisita.setValue(LocalDate.now());

        panelCroquis.setMinSize(0, 180);
        imgCroquis.fitWidthProperty().bind(panelCroquis.widthProperty().subtract(20));
        imgCroquis.fitHeightProperty().bind(panelCroquis.heightProperty().subtract(20));

        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getIdLevantamiento())));
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaVisita() != null ? d.getValue().getFechaVisita().toString() : ""));
        colProyecto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProyectoDescriptivo()));
        colCita.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaHoraCita() != null && !d.getValue().getFechaHoraCita().isBlank()
                        ? d.getValue().getFechaHoraCita() : "Sin cita"));
        colObs.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getObservacionesTecnicas() != null ? d.getValue().getObservacionesTecnicas() : ""));
        colDoc.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDocumento() != null && !d.getValue().getDocumento().isBlank() ? "Sí" : "—"));
        colActivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getActivoTexto()));
        colActivo.setCellFactory(c -> Tablas.crearCeldaEstado());
        tblLevantamientos.setPlaceholder(new Label("No hay levantamientos registrados"));

        tblLevantamientos.getSelectionModel().selectedItemProperty().addListener((obs, a, nuevo) -> {
            if (nuevo != null) {
                cargarEnFormulario(nuevo);
            }
        });
        // Edición rápida: lápiz en cada fila, doble clic o Enter
        Edicion.habilitar(tblLevantamientos, this::cargarEnFormulario);
        Edicion.modo(false, lblModoFormulario, btnGuardarLev, btnActualizarLev, btnDesactivarLev, btnReactivarLev, btnEliminarLev);
        mostrarDocumento(null);

        // Autorrelleno: al elegir el proyecto se muestran los datos del cliente y se propone su cita más reciente;
        // al elegir primero la cita se propone el proyecto de ese cliente.
        cmbProyecto.valueProperty().addListener((o, a, p) -> alElegirProyecto(p));
        cmbCita.valueProperty().addListener((o, a, c) -> alElegirCita(c));

        cargarCombos();
        cargarTabla(null);
    }

    private boolean autollenando;

    private void alElegirProyecto(Proyecto p) {
        if (p == null) {
            lblInfoProyectoLev.setText("Al elegir el proyecto se llenan solos el cliente y su cita más reciente.");
            return;
        }
        StringBuilder info = new StringBuilder("Cliente: ")
                .append(p.getClienteNombre() != null ? p.getClienteNombre() : "—");
        try {
            Cliente cli = clienteService.consultarPorId(p.getIdCliente());
            if (cli != null) {
                try {
                    List<com.mycompany.soptec.it.modelo.ClienteTelefono> tels =
                            new com.mycompany.soptec.it.servicio.ClienteTelefonoService().listarPorCliente(cli.getIdCliente(), false);
                    if (!tels.isEmpty()) {
                        info.append("  ·  Tel. ").append(tels.get(0).getTelefono());
                    }
                } catch (Exception ex) {
                    // el teléfono es solo informativo
                }
                String dir = cli.getDireccionResumen();
                if (dir != null && !dir.isBlank()) {
                    info.append("  ·  ").append(dir);
                }
                if (cli.getEstado() != null && !cli.getEstado().isBlank() && (dir == null || !dir.contains(cli.getEstado()))) {
                    info.append(", ").append(cli.getEstado());
                }
            }
        } catch (Exception ex) {
            // Solo informativo
        }
        lblInfoProyectoLev.setText(info.toString());
        if (autollenando || seleccionado != null || cmbCita.getValue() != null) {
            return;
        }
        Cita reciente = null;
        for (Cita c : cmbCita.getItems()) {
            if (c != null && c.getIdCliente() == p.getIdCliente() && !"Cancelada".equalsIgnoreCase(c.getEstadoCita())
                    && (reciente == null || (c.getFechaCita() != null && reciente.getFechaCita() != null
                    && c.getFechaCita().isAfter(reciente.getFechaCita())))) {
                reciente = c;
            }
        }
        if (reciente != null) {
            autollenando = true;
            try {
                cmbCita.setValue(reciente);
            } finally {
                autollenando = false;
            }
        }
    }

    private void alElegirCita(Cita c) {
        if (c == null || autollenando || seleccionado != null || cmbProyecto.getValue() != null) {
            return;
        }
        Proyecto unico = null;
        int encontrados = 0;
        for (Proyecto p : cmbProyecto.getItems()) {
            if (p != null && p.getIdCliente() == c.getIdCliente()) {
                unico = p;
                encontrados++;
            }
        }
        if (encontrados == 1) {
            autollenando = true;
            try {
                cmbProyecto.setValue(unico);
            } finally {
                autollenando = false;
            }
        }
    }

    private void cargarCombos() {
        try {
            List<Proyecto> proyectos = proyectoService.listar(false);
            ComboBuscadorUtil.configurar(cmbProyecto, txtFiltroProyecto, proyectos, Proyecto::toString);

            List<Cita> citas = citaService.listar(false);
            ComboBuscadorUtil.configurar(cmbCita, null, citas, Cita::toString);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_COMBOS", ex);
        }
    }

    @FXML
    private void cargarTabla(ActionEvent event) {
        try {
            if (event != null) {
                cargarCombos();
            }
            tblLevantamientos.setItems(FXCollections.observableArrayList(
                    service.listar(chkInactivos.isSelected())));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_LEVANTAMIENTOS", ex);
        }
    }

    @FXML
    private void buscar(ActionEvent event) {
        try {
            tblLevantamientos.setItems(FXCollections.observableArrayList(
                    service.buscar(txtBuscar.getText(), chkInactivos.isSelected())));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "BUSCAR_LEVANTAMIENTOS", ex);
        }
    }

    @FXML
    private void consultarPorId(ActionEvent event) {
        try {
            int id = Validador.parsearIdPositivo(txtConsultarId.getText(), "ID de levantamiento", MODULO, "CONSULTAR_POR_ID");
            Levantamiento l = service.consultarPorId(id);
            if (l == null) {
                AlertaUtil.advertencia("No encontrado", "No existe un levantamiento con el ID " + id + ".");
                return;
            }
            cargarEnFormulario(l);
            AlertaUtil.info("Levantamiento encontrado",
                    "Proyecto: " + l.getProyectoDescriptivo()
                    + "\nFecha visita: " + l.getFechaVisita()
                    + "\nCroquis: " + (l.getCroquis() != null ? l.getCroquis() : "Sin croquis")
                    + "\nEstado: " + l.getActivoTexto());
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CONSULTAR_POR_ID", ex);
        }
    }

    @FXML
    private void cargarCroquis(ActionEvent event) {
        try {
            String ruta = GestorImagenes.elegirYGuardarImagen(
                    panelCroquis.getScene().getWindow(), "Seleccionar imagen de croquis", "croquis", MODULO);
            if (ruta != null) {
                mostrarCroquis(ruta);
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "SELECCIONAR_CROQUIS", ex);
        }
    }

    @FXML
    private void quitarCroquis(ActionEvent event) {
        rutaCroquisActual = null;
        lblRutaCroquis.setText("Sin croquis seleccionado");
        imgCroquis.setImage(null);
        vacioCroquis.setVisible(true);
    }

    private void mostrarCroquis(String ruta) {
        rutaCroquisActual = ruta;
        lblRutaCroquis.setText(ruta);
        Image img = GestorImagenes.cargarImagen(ruta);
        imgCroquis.setImage(img);
        vacioCroquis.setVisible(img == null);
    }

    @FXML
    private void guardarLevantamiento(ActionEvent event) {
        try {
            Levantamiento creado = service.crear(construirDesdeFormulario(new Levantamiento(), "CREAR_LEVANTAMIENTO"));
            AlertaUtil.info("Levantamiento registrado",
                    "Se guardó el levantamiento del proyecto '" + creado.getNombreProyecto() + "'.");
            limpiar(null);
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CREAR_LEVANTAMIENTO", ex);
        }
    }

    @FXML
    private void actualizarLevantamiento(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona un levantamiento", "Elige una fila de la tabla para actualizarla.");
            return;
        }
        try {
            Levantamiento editado = construirDesdeFormulario(seleccionado, "ACTUALIZAR_LEVANTAMIENTO");
            service.actualizar(editado);
            AlertaUtil.info("Levantamiento actualizado", "Se actualizaron los datos del levantamiento.");
            limpiar(null);
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ACTUALIZAR_LEVANTAMIENTO", ex);
        }
    }

    @FXML
    private void desactivarLevantamiento(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona un levantamiento", "Elige una fila de la tabla.");
            return;
        }
        try {
            service.desactivar(seleccionado.getIdLevantamiento());
            AlertaUtil.info("Levantamiento desactivado", "El levantamiento quedó inactivo.");
            limpiar(null);
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "DESACTIVAR_LEVANTAMIENTO", ex);
        }
    }

    @FXML
    private void reactivarLevantamiento(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona un levantamiento", "Marca 'Mostrar inactivos' y elige un registro.");
            return;
        }
        try {
            if (cascada.proyectoOClienteInactivo(seleccionado.getIdProyecto())) {
                if (!AlertaUtil.confirmar("Reactivar levantamiento", "El proyecto de este levantamiento (o su cliente) "
                        + "está inactivo.\nPara reactivarlo se reactivarán también el proyecto y su cliente. ¿Continuar?")) {
                    return;
                }
                cascada.reactivarLevantamientoYProyecto(seleccionado.getIdLevantamiento());
                AlertaUtil.info("Levantamiento reactivado", "El levantamiento, su proyecto y su cliente volvieron a estar activos.");
                cargarCombos();
            } else {
                service.reactivar(seleccionado.getIdLevantamiento());
                AlertaUtil.info("Levantamiento reactivado", "El levantamiento volvió a estar activo.");
            }
            limpiar(null);
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "REACTIVAR_LEVANTAMIENTO", ex);
        }
    }

    @FXML
    private void eliminarFisicoLevantamiento(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona un levantamiento", "Elige una fila de la tabla.");
            return;
        }
        if (!AlertaUtil.confirmar("Eliminar levantamiento definitivamente",
                "¿Eliminar permanentemente el levantamiento seleccionado?")) {
            return;
        }
        try {
            service.eliminarFisico(seleccionado.getIdLevantamiento());
            AlertaUtil.info("Levantamiento eliminado", "El registro fue eliminado de la base de datos.");
            limpiar(null);
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ELIMINAR_FISICO_LEVANTAMIENTO", ex);
        }
    }

    @FXML
    private void limpiar(ActionEvent event) {
        seleccionado = null;
        lblModoFormulario.setText("Registrar levantamiento");
        txtFiltroProyecto.clear();
        cmbProyecto.setValue(null);
        cmbCita.setValue(null);
        dpFechaVisita.setValue(LocalDate.now());
        txtObservacionesLev.clear();
        quitarCroquis(null);
        mostrarDocumento(null);
        tblLevantamientos.getSelectionModel().clearSelection();
        Edicion.modo(false, lblModoFormulario, btnGuardarLev, btnActualizarLev, btnDesactivarLev, btnReactivarLev, btnEliminarLev);
    }

    private void cargarEnFormulario(Levantamiento l) {
        seleccionado = l;
        lblModoFormulario.setText("Editando Levantamiento #" + l.getIdLevantamiento());
        txtFiltroProyecto.clear();
        ComboBuscadorUtil.seleccionarPorPredicado(cmbProyecto, p -> p.getIdProyecto() == l.getIdProyecto());
        if (l.getIdCita() != null) {
            ComboBuscadorUtil.seleccionarPorPredicado(cmbCita, c -> c.getIdCita() == l.getIdCita());
        } else {
            cmbCita.setValue(null);
        }
        dpFechaVisita.setValue(l.getFechaVisita());
        txtObservacionesLev.setText(l.getObservacionesTecnicas() == null ? "" : l.getObservacionesTecnicas());
        if (l.getCroquis() != null && !l.getCroquis().isBlank()) {
            mostrarCroquis(l.getCroquis());
        } else {
            quitarCroquis(null);
        }
        mostrarDocumento(l.getDocumento());
        Edicion.modo(true, lblModoFormulario, btnGuardarLev, btnActualizarLev, btnDesactivarLev, btnReactivarLev, btnEliminarLev);
    }

    // =========================================================================
    // DOCUMENTO DEL TÉCNICO (ficha en PDF, Word, Excel o foto)
    // =========================================================================
    @FXML
    private void cargarDocumento(ActionEvent event) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir el documento del levantamiento");
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documentos (PDF, Word, Excel, imágenes)",
                        "*.pdf", "*.doc", "*.docx", "*.xls", "*.xlsx", "*.txt", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("Solo PDF", "*.pdf"));
        File archivo = selector.showOpenDialog(lblDocumentoLev.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        try {
            mostrarDocumento(GestorDocumentos.guardar(archivo, "levantamientos", MODULO));
            if (seleccionado != null) {
                lblDocumentoLev.setText(lblDocumentoLev.getText() + "  (presiona «Guardar cambios»)");
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_DOCUMENTO", ex);
        }
    }

    @FXML
    private void abrirDocumento(ActionEvent event) {
        java.nio.file.Path ruta = GestorDocumentos.resolver(rutaDocumentoActual);
        if (ruta == null || !java.nio.file.Files.exists(ruta)) {
            AlertaUtil.advertencia("Documento no disponible", "No se encontró el archivo en la carpeta de SOPTEC-IT. "
                    + "Puede que se haya borrado; vuelve a cargarlo.");
            return;
        }
        if (!com.mycompany.soptec.it.App.abrirArchivo(ruta)) {
            AlertaUtil.info("Ubicación del documento", "Ábrelo desde:\n" + ruta);
        }
    }

    @FXML
    private void quitarDocumento(ActionEvent event) {
        mostrarDocumento(null);
    }

    private void mostrarDocumento(String ruta) {
        rutaDocumentoActual = ruta == null || ruta.isBlank() ? null : ruta;
        boolean hay = rutaDocumentoActual != null;
        lblDocumentoLev.setText(hay ? GestorDocumentos.nombreVisible(rutaDocumentoActual)
                : "Sin documento (PDF, Word, Excel o foto de su ficha)");
        btnAbrirDocumento.setDisable(!hay);
        btnQuitarDocumento.setDisable(!hay);
    }

    private Levantamiento construirDesdeFormulario(Levantamiento base, String op) throws ValidacionException {
        Proyecto proy = cmbProyecto.getValue();
        if (proy == null) {
            throw new ValidacionException(MODULO, op, "Debes seleccionar un proyecto de la lista desplegable.");
        }
        base.setIdProyecto(proy.getIdProyecto());
        base.setNombreProyecto(proy.getNombreProyecto());
        Cita cita = cmbCita.getValue();
        base.setIdCita(cita != null ? cita.getIdCita() : null);
        base.setFechaVisita(dpFechaVisita.getValue());
        base.setObservacionesTecnicas(txtObservacionesLev.getText());
        base.setCroquis(rutaCroquisActual);
        base.setDocumento(rutaDocumentoActual);
        return base;
    }
}
