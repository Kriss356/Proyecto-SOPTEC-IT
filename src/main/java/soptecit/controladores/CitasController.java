package soptecit.controladores;

import com.mycompany.soptec.it.servicio.CascadaService;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.DetalleServicio;
import com.mycompany.soptec.it.modelo.Servicio;
import com.mycompany.soptec.it.servicio.ServicioService;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ClienteTelefonoService;
import com.mycompany.soptec.it.modelo.ClienteTelefono;
import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.ComboBuscadorUtil;
import com.mycompany.soptec.it.util.Validador;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
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
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import soptecit.util.Edicion;
import soptecit.util.EditorDetalle;
import soptecit.util.Tablas;

/**
 * Controlador estilizado de Agenda y CRUD de Citas conectado a CitaService y ClienteService.
 */
public class CitasController implements Initializable {

    private static final String MODULO = "Citas";
    private final CitaService service = new CitaService();
    private final CascadaService cascada = new CascadaService();
    private final ClienteService clienteService = new ClienteService();
    private final ClienteTelefonoService telefonoService = new ClienteTelefonoService();

    // Botones del formulario de tblCitas (modo nuevo / editando)
    @FXML private Button btnGuardarCita;
    @FXML private Button btnActualizarCita;
    @FXML private Button btnDesactivarCita;
    @FXML private Button btnReactivarCita;
    @FXML private Button btnEliminarCita;
    @FXML private CheckBox chkInactivos;
    @FXML private TextField txtBuscarCita;
    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private TextField txtConsultarId;
    @FXML private Label lblResumen;
    @FXML private Label lblConteo;

    @FXML private TableView<Cita> tblCitas;
    @FXML private TableColumn<Cita, String> colId;
    @FXML private TableColumn<Cita, String> colFecha;
    @FXML private TableColumn<Cita, String> colHora;
    @FXML private TableColumn<Cita, String> colCliente;
    @FXML private TableColumn<Cita, String> colEstadoCita;
    @FXML private TableColumn<Cita, String> colObs;
    @FXML private TableColumn<Cita, String> colActivo;

    @FXML private Label lblModoFormulario;
    @FXML private TextField txtFiltroCliente;
    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private Label lblInfoCliente;
    @FXML private TextField txtCliTelefono;
    @FXML private TextField txtCliRfc;
    @FXML private TextField txtCliCorreo;
    @FXML private TextField txtCliEstado;
    @FXML private TextField txtCliDireccion;
    @FXML private DatePicker dpFechaCita;
    @FXML private ComboBox<String> cmbHora;
    @FXML private ComboBox<String> cmbEstadoCita;
    @FXML private TextArea txtObservaciones;

    // Servicios de la cita (M:N cita_servicio)
    @FXML private ComboBox<Servicio> cmbServicioCita;
    @FXML private Spinner<Integer> spnCantidadServicio;
    @FXML private TableView<DetalleServicio> tblServiciosCita;
    @FXML private Label lblTotalServiciosCita;

    private final ServicioService servicioService = new ServicioService();
    private EditorDetalle<Servicio, DetalleServicio> editorServicios;

    private Cita seleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        for (int h = 8; h <= 19; h++) {
            cmbHora.getItems().add(String.format("%02d:00", h));
            cmbHora.getItems().add(String.format("%02d:30", h));
        }
        cmbHora.setValue("09:00");
        cmbEstadoCita.getItems().addAll("Solicitada", "Programada", "Confirmada", "Atendida", "Cancelada");
        cmbEstadoCita.setValue("Programada");
        cmbFiltroEstado.setValue("Todas");
        dpFechaCita.setValue(LocalDate.now());

        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getIdCita())));
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaCita() != null ? d.getValue().getFechaCita().toString() : ""));
        colHora.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getHoraCita() != null ? d.getValue().getHoraCita().toString() : ""));
        colCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClienteDescriptivo()));
        colEstadoCita.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstadoCita()));
        colEstadoCita.setCellFactory(c -> Tablas.crearCeldaEstado());
        colObs.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getObservaciones() != null ? d.getValue().getObservaciones() : ""));
        colActivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getActivoTexto()));
        colActivo.setCellFactory(c -> Tablas.crearCeldaEstado());
        tblCitas.setPlaceholder(new Label("No hay citas para mostrar"));

        cmbFiltroEstado.valueProperty().addListener((obs, a, n) -> buscar(null));

        // Al elegir el cliente, sus datos (teléfono, RFC, correo, estado y dirección) se llenan solos
        cmbCliente.valueProperty().addListener((obs, a, cli) -> mostrarDatosCliente(cli));

        tblCitas.getSelectionModel().selectedItemProperty().addListener((obs, oldV, nuevo) -> {
            if (nuevo != null) {
                cargarEnFormulario(nuevo);
            }
        });
        // Edición rápida: lápiz en cada fila, doble clic o Enter
        Edicion.habilitar(tblCitas, this::cargarEnFormulario);
        Edicion.modo(false, lblModoFormulario, btnGuardarCita, btnActualizarCita, btnDesactivarCita, btnReactivarCita, btnEliminarCita);

        editorServicios = new EditorDetalle<>(cmbServicioCita, spnCantidadServicio, tblServiciosCita,
                lblTotalServiciosCita, "Total estimado: ", "Sin servicios: agrega al menos uno",
                EditorDetalle.adaptador(DetalleServicio::de, Servicio::getIdServicio, DetalleServicio::getIdServicio,
                        DetalleServicio::getNombreServicio, DetalleServicio::getCantidad, DetalleServicio::setCantidad,
                        DetalleServicio::getPrecioUnitario, DetalleServicio::getImporte));
        editorServicios.setTextoCatalogoVacio("No hay servicios en el catálogo. Agrégalos en Ventas › Catálogo de servicios.");
        // Cada vez que se abre la lista se vuelve a leer el catálogo (así aparecen los servicios nuevos)
        editorServicios.setFuenteCatalogo(() -> servicioService.listar(false));

        cargarComboClientes();
        cargarCatalogoServicios();
        cargarTabla(null);
    }

    private void cargarCatalogoServicios() {
        try {
            editorServicios.setCatalogo(servicioService.listar(false));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_CATALOGO_SERVICIOS", ex);
        }
    }

    @FXML
    private void agregarServicio(ActionEvent event) {
        String aviso = editorServicios.agregarSeleccion();
        if (aviso != null) {
            AlertaUtil.advertencia("Agregar servicio", aviso);
        }
    }

    @FXML
    private void quitarServicio(ActionEvent event) {
        String aviso = editorServicios.quitarSeleccion();
        if (aviso != null) {
            AlertaUtil.advertencia("Quitar servicio", aviso);
        }
    }

    private void cargarComboClientes() {
        try {
            List<Cliente> activos = clienteService.listar(false);
            ComboBuscadorUtil.configurar(cmbCliente, txtFiltroCliente, activos, Cliente::toString);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CARGAR_COMBO_CLIENTES", ex);
        }
    }

    @FXML
    private void cargarTabla(ActionEvent event) {
        try {
            if (event != null) {
                cargarComboClientes();
                cargarCatalogoServicios();
            }
            List<Cita> lista = aplicarFiltroEstado(service.listar(chkInactivos.isSelected()));
            mostrarEnTabla(lista);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_CITAS", ex);
        }
    }

    @FXML
    private void buscar(ActionEvent event) {
        try {
            String criterio = txtBuscarCita.getText();
            List<Cita> base = (criterio == null || criterio.isBlank())
                    ? service.listar(chkInactivos.isSelected())
                    : service.buscar(criterio, chkInactivos.isSelected());
            mostrarEnTabla(aplicarFiltroEstado(base));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "BUSCAR_CITAS", ex);
        }
    }

    private List<Cita> aplicarFiltroEstado(List<Cita> citas) {
        String filtro = cmbFiltroEstado.getValue();
        if (filtro == null || "Todas".equals(filtro)) {
            return citas;
        }
        return citas.stream()
                .filter(c -> filtro.equalsIgnoreCase(c.getEstadoCita()))
                .collect(Collectors.toList());
    }

    private void mostrarEnTabla(List<Cita> citas) {
        tblCitas.setItems(FXCollections.observableArrayList(citas));
        long confirmadas = citas.stream().filter(c -> "Confirmada".equalsIgnoreCase(c.getEstadoCita())).count();
        long atendidas = citas.stream().filter(c -> "Atendida".equalsIgnoreCase(c.getEstadoCita())).count();
        lblConteo.setText(citas.size() + " citas  ·  " + confirmadas + " confirmadas  ·  " + atendidas + " atendidas");
    }

    @FXML
    private void consultarPorId(ActionEvent event) {
        try {
            int id = Validador.parsearIdPositivo(txtConsultarId.getText(), "ID de cita", MODULO, "CONSULTAR_POR_ID");
            Cita c = service.consultarPorId(id);
            if (c == null) {
                AlertaUtil.advertencia("No encontrada", "No existe una cita con el ID " + id + ".");
                return;
            }
            cargarEnFormulario(c);
            AlertaUtil.info("Cita encontrada",
                    "Cliente: " + c.getClienteDescriptivo()
                    + "\nFecha y hora: " + c.getFechaCita() + " " + c.getHoraCita()
                    + "\nEstado cita: " + c.getEstadoCita()
                    + "\nRegistro: " + c.getActivoTexto());
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CONSULTAR_POR_ID", ex);
        }
    }

    @FXML
    private void guardarCita(ActionEvent event) {
        try {
            List<DetalleServicio> servicios = editorServicios.getRenglones();
            Cita creada = service.crearConServicios(construirDesdeFormulario(new Cita(), "CREAR_CITA_CON_SERVICIOS"),
                    servicios);
            AlertaUtil.info("Cita agendada",
                    "Se registró la cita #" + creada.getIdCita() + " para " + creada.getClienteDescriptivo()
                    + " el " + creada.getFechaCita() + " a las " + creada.getHoraCita()
                    + (servicios.isEmpty() ? "." : " con " + servicios.size() + " servicio(s)."));
            limpiar(null);
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CREAR_CITA", ex);
        }
    }

    @FXML
    private void actualizarCita(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona una cita", "Elige una cita de la tabla para actualizarla.");
            return;
        }
        try {
            Cita editada = construirDesdeFormulario(seleccionado, "ACTUALIZAR_CITA_CON_SERVICIOS");
            service.actualizarConServicios(editada, editorServicios.getRenglones());
            AlertaUtil.info("Cita actualizada", "Se actualizó la cita y sus servicios correctamente.");
            limpiar(null);
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ACTUALIZAR_CITA", ex);
        }
    }

    @FXML
    private void cancelarCita(ActionEvent event) {
        Cita cita = tblCitas.getSelectionModel().getSelectedItem();
        if (cita == null) {
            AlertaUtil.advertencia("Selecciona una cita", "Elige la cita que deseas cancelar en la tabla.");
            return;
        }
        if (!AlertaUtil.confirmar("Cancelar cita", "¿Seguro que deseas marcar como Cancelada la cita #" + cita.getIdCita() + "?")) {
            return;
        }
        try {
            cita.setEstadoCita("Cancelada");
            service.actualizar(cita);
            AlertaUtil.info("Cita cancelada", "El estado de la cita cambió a Cancelada en MySQL.");
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CANCELAR_CITA", ex);
        }
    }

    @FXML
    private void marcarAtendida(ActionEvent event) {
        Cita cita = tblCitas.getSelectionModel().getSelectedItem();
        if (cita == null) {
            AlertaUtil.advertencia("Selecciona una cita", "Elige la cita que deseas marcar como Atendida.");
            return;
        }
        try {
            cita.setEstadoCita("Atendida");
            service.actualizar(cita);
            AlertaUtil.info("Cita atendida", "La cita #" + cita.getIdCita() + " se marcó como Atendida en MySQL.");
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "MARCAR_ATENDIDA", ex);
        }
    }

    @FXML
    private void desactivarCita(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona una cita", "Elige una cita de la tabla.");
            return;
        }
        try {
            service.desactivar(seleccionado.getIdCita());
            AlertaUtil.info("Cita desactivada", "La cita quedó inactiva.");
            limpiar(null);
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "DESACTIVAR_CITA", ex);
        }
    }

    @FXML
    private void reactivarCita(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona una cita", "Marca 'Mostrar inactivos' y elige una cita.");
            return;
        }
        try {
            if (cascada.clienteInactivo(seleccionado.getIdCliente())) {
                if (!AlertaUtil.confirmar("Reactivar cita", "El cliente de esta cita está inactivo.\n"
                        + "Para reactivar la cita se reactivará también al cliente. ¿Continuar?")) {
                    return;
                }
                cascada.reactivarCitaYCliente(seleccionado.getIdCita());
                AlertaUtil.info("Cita reactivada", "La cita y su cliente volvieron a estar activos.");
                cargarComboClientes();
            } else {
                service.reactivar(seleccionado.getIdCita());
                AlertaUtil.info("Cita reactivada", "La cita volvió a estar activa.");
            }
            limpiar(null);
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "REACTIVAR_CITA", ex);
        }
    }

    @FXML
    private void eliminarFisicoCita(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona una cita", "Elige una cita de la tabla.");
            return;
        }
        if (!AlertaUtil.confirmar("Eliminar cita definitivamente",
                "¿Eliminar permanentemente la cita del " + seleccionado.getFechaCita() + "?")) {
            return;
        }
        try {
            service.eliminarFisico(seleccionado.getIdCita());
            AlertaUtil.info("Cita eliminada", "El registro fue eliminado de la base de datos.");
            limpiar(null);
            cargarTabla(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ELIMINAR_FISICO_CITA", ex);
        }
    }

    @FXML
    private void nuevoCliente(ActionEvent event) {
        PrincipalController.irA("Ventas");
    }

    @FXML
    private void limpiar(ActionEvent event) {
        seleccionado = null;
        lblModoFormulario.setText("Agendar nueva cita");
        txtFiltroCliente.clear();
        cmbCliente.setValue(null);
        dpFechaCita.setValue(LocalDate.now());
        cmbHora.setValue("09:00");
        cmbHora.getEditor().setText("09:00");
        cmbEstadoCita.setValue("Programada");
        txtObservaciones.clear();
        editorServicios.limpiar();
        tblCitas.getSelectionModel().clearSelection();
        Edicion.modo(false, lblModoFormulario, btnGuardarCita, btnActualizarCita, btnDesactivarCita, btnReactivarCita, btnEliminarCita);
    }

    private void cargarEnFormulario(Cita c) {
        seleccionado = c;
        lblModoFormulario.setText("Editando Cita #" + c.getIdCita());
        txtFiltroCliente.clear();
        ComboBuscadorUtil.seleccionarPorPredicado(cmbCliente, cli -> cli.getIdCliente() == c.getIdCliente());
        dpFechaCita.setValue(c.getFechaCita());
        String horaStr = c.getHoraCita() != null ? c.getHoraCita().toString() : "09:00";
        if (horaStr.length() > 5) {
            horaStr = horaStr.substring(0, 5);
        }
        cmbHora.setValue(horaStr);
        cmbHora.getEditor().setText(horaStr);
        cmbEstadoCita.setValue(c.getEstadoCita());
        txtObservaciones.setText(c.getObservaciones() == null ? "" : c.getObservaciones());
        try {
            editorServicios.setRenglones(service.listarServicios(c.getIdCita()));
        } catch (Exception ex) {
            editorServicios.setRenglones(List.of());
            AlertaUtil.errorControlado(MODULO, "LISTAR_SERVICIOS_CITA", ex);
        }
        Edicion.modo(true, lblModoFormulario, btnGuardarCita, btnActualizarCita, btnDesactivarCita, btnReactivarCita, btnEliminarCita);
    }

    /** Llena la ficha del cliente con sus datos guardados (solo lectura). */
    private void mostrarDatosCliente(Cliente cli) {
        TextField[] campos = {txtCliTelefono, txtCliRfc, txtCliCorreo, txtCliEstado, txtCliDireccion};
        if (cli == null) {
            for (TextField t : campos) {
                t.clear();
                t.getStyleClass().remove("campo-auto-lleno");
            }
            lblInfoCliente.setText("Al elegir el cliente, sus datos se llenan solos.");
            return;
        }
        String telefono = "";
        try {
            List<ClienteTelefono> tels = telefonoService.listarPorCliente(cli.getIdCliente(), false);
            telefono = tels.stream().map(ClienteTelefono::getTelefono).collect(Collectors.joining(", "));
        } catch (Exception ex) {
            // Sin teléfono no se detiene la captura de la cita
        }
        txtCliTelefono.setText(telefono.isEmpty() ? "Sin teléfono" : telefono);
        txtCliRfc.setText(texto(cli.getRfc()));
        txtCliCorreo.setText(texto(cli.getCorreo()));
        txtCliEstado.setText(texto(cli.getEstado()));
        String dir = cli.getDireccionResumen();
        txtCliDireccion.setText(dir == null || dir.isBlank() ? "Sin dirección registrada" : dir);
        for (TextField t : campos) {
            t.positionCaret(0);
            if (!t.getStyleClass().contains("campo-auto-lleno")) {
                t.getStyleClass().add("campo-auto-lleno");
            }
        }
        lblInfoCliente.setText("Datos de " + cli.getNombreCliente() + " (para cambiarlos ve a Ventas › Clientes).");
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private Cita construirDesdeFormulario(Cita base, String op) throws Exception {
        Cliente cli = cmbCliente.getValue();
        if (cli == null) {
            throw new ValidacionException(MODULO, op, "Selecciona un cliente de la lista desplegable.");
        }
        base.setIdCliente(cli.getIdCliente());
        base.setNombreCliente(cli.getNombreCliente());
        base.setRfcCliente(cli.getRfc());
        base.setFechaCita(dpFechaCita.getValue());
        String horaTxt = cmbHora.getEditor().getText();
        if ((horaTxt == null || horaTxt.isBlank()) && cmbHora.getValue() != null) {
            horaTxt = cmbHora.getValue();
        }
        base.setHoraCita(Validador.parsearHora(horaTxt, MODULO, op));
        base.setEstadoCita(cmbEstadoCita.getValue());
        base.setObservaciones(txtObservaciones.getText());
        return base;
    }
}
