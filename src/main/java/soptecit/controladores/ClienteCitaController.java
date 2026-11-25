package soptecit.controladores;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.servicio.PortalClienteService;
import com.mycompany.soptec.it.util.AlertaUtil;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import soptecit.util.SesionCliente;

/**
 * Asistente de 3 pasos para que el cliente agende una cita o pida una instalación:
 * 1) ¿Qué necesitas?  2) Día y hora (solo horarios libres)  3) Confirmar.
 */
public class ClienteCitaController implements Initializable {

    private static final Locale ES_MX = new Locale("es", "MX");

    @FXML private ToggleGroup grupoServicio;
    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private Label lblPaso1;
    @FXML private Label lblPaso2;
    @FXML private Label lblPaso3;

    @FXML private VBox paso1;
    @FXML private ToggleButton tgReparacion;
    @FXML private ToggleButton tgInstalacion;
    @FXML private ToggleButton tgMantenimiento;
    @FXML private ToggleButton tgCotizacion;
    @FXML private VBox panelDomicilio;
    @FXML private TextField txtCalle;
    @FXML private TextField txtNumero;
    @FXML private TextField txtColonia;
    @FXML private TextArea txtNota;

    @FXML private VBox paso2;
    @FXML private FlowPane flowDias;
    @FXML private Label lblTituloHoras;
    @FXML private FlowPane flowHoras;
    @FXML private Label lblSinHoras;

    @FXML private VBox paso3;
    @FXML private Label lblResServicio;
    @FXML private Label lblResDia;
    @FXML private Label lblResHora;
    @FXML private Label lblResDomicilio;
    @FXML private Label lblResNota;
    @FXML private Label lblResTelefono;

    @FXML private VBox pasoListo;
    @FXML private Label lblListoDetalle;

    @FXML private HBox barraNavegacion;
    @FXML private Button btnAtras;
    @FXML private Label lblError;
    @FXML private Button btnSiguiente;

    private final PortalClienteService service = new PortalClienteService();
    private final ToggleGroup grupoDias = new ToggleGroup();
    private final ToggleGroup grupoHoras = new ToggleGroup();
    private int paso = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tgReparacion.setUserData(PortalClienteService.SERV_REPARACION);
        tgInstalacion.setUserData(PortalClienteService.SERV_INSTALACION);
        tgMantenimiento.setUserData(PortalClienteService.SERV_MANTENIMIENTO);
        tgCotizacion.setUserData(PortalClienteService.SERV_COTIZACION);

        grupoServicio.selectedToggleProperty().addListener((o, anterior, nuevo) -> {
            boolean instalacion = nuevo == tgInstalacion;
            panelDomicilio.setVisible(instalacion);
            panelDomicilio.setManaged(instalacion);
            mostrarError(null);
        });
        mantenerSeleccion(grupoDias);
        mantenerSeleccion(grupoHoras);
        grupoDias.selectedToggleProperty().addListener((o, a, n) -> {
            if (n != null) {
                cargarHoras((LocalDate) n.getUserData());
            }
        });
        grupoHoras.selectedToggleProperty().addListener((o, a, n) -> mostrarError(null));

        Cliente c = SesionCliente.getCliente();
        if (c != null) {
            txtCalle.setText(c.getCalle());
            txtNumero.setText(c.getNumero());
            txtColonia.setText(c.getColonia());
        }
        crearBotonesDeDias();
        mostrarPaso(1);
    }

    /** Llega con un servicio ya elegido (por ejemplo desde «Pedir una instalación»). */
    public void preseleccionar(String servicio) {
        for (Toggle t : grupoServicio.getToggles()) {
            if (servicio.equals(t.getUserData())) {
                grupoServicio.selectToggle(t);
            }
        }
        if (PortalClienteService.SERV_INSTALACION.equals(servicio)) {
            lblTitulo.setText("Pedir una instalación");
            lblSubtitulo.setText("Dinos dónde y cuándo; vamos a tu domicilio.");
            txtCalle.requestFocus();
        }
    }

    // ------------------------------------------------------------------ paso 2

    private void crearBotonesDeDias() {
        flowDias.getChildren().clear();
        DateTimeFormatter diaSemana = DateTimeFormatter.ofPattern("EEE", ES_MX);
        DateTimeFormatter mes = DateTimeFormatter.ofPattern("MMM", ES_MX);
        LocalDate hoy = LocalDate.now();
        for (LocalDate d : service.diasDisponibles()) {
            String encabezado = d.equals(hoy) ? "Hoy"
                    : d.equals(hoy.plusDays(1)) ? "Mañana" : capitalizar(d.format(diaSemana).replace(".", ""));
            ToggleButton b = new ToggleButton(encabezado + "\n" + d.getDayOfMonth() + "\n" + d.format(mes).replace(".", ""));
            b.setUserData(d);
            b.setToggleGroup(grupoDias);
            b.getStyleClass().add("dia-boton");
            b.setTooltip(new Tooltip(capitalizar(fechaLarga(d))));
            flowDias.getChildren().add(b);
        }
    }

    private void cargarHoras(LocalDate fecha) {
        flowHoras.getChildren().clear();
        grupoHoras.getToggles().clear();
        lblTituloHoras.setText("2. Elige la hora del " + fechaLarga(fecha));
        Set<LocalTime> ocupadas;
        try {
            ocupadas = service.horasOcupadas(fecha);
        } catch (SoptecException ex) {
            mostrarError(AlertaUtil.mensajeUsuario(ex));
            return;
        }
        int libres = 0;
        for (LocalTime h : service.horasDelDia()) {
            ToggleButton b = new ToggleButton(horaTexto(h));
            b.setUserData(h);
            b.setToggleGroup(grupoHoras);
            b.getStyleClass().add("hora-boton");
            if (service.horaDisponible(fecha, h, ocupadas)) {
                libres++;
                b.setTooltip(new Tooltip("Disponible"));
            } else {
                b.setDisable(true);
                b.getStyleClass().add("hora-ocupada");
            }
            flowHoras.getChildren().add(b);
        }
        lblSinHoras.setVisible(libres == 0);
        lblSinHoras.setManaged(libres == 0);
    }

    // ------------------------------------------------------------------ navegación

    @FXML
    private void siguiente(ActionEvent event) {
        mostrarError(null);
        switch (paso) {
            case 1:
                if (grupoServicio.getSelectedToggle() == null) {
                    mostrarError("Toca una de las 4 opciones para decirnos qué necesitas.");
                    return;
                }
                if (esInstalacion() && (vacio(txtCalle) || vacio(txtNumero) || vacio(txtColonia))) {
                    mostrarError("Escribe la calle, el número y la colonia donde será la instalación.");
                    return;
                }
                mostrarPaso(2);
                break;
            case 2:
                if (grupoDias.getSelectedToggle() == null) {
                    mostrarError("Primero toca el día que prefieras.");
                    return;
                }
                if (grupoHoras.getSelectedToggle() == null) {
                    mostrarError("Ahora toca la hora que prefieras.");
                    return;
                }
                llenarResumen();
                mostrarPaso(3);
                break;
            case 3:
                confirmar();
                break;
            default:
                break;
        }
    }

    @FXML
    private void atras(ActionEvent event) {
        if (paso <= 1) {
            irInicio(event);
        } else {
            mostrarPaso(paso - 1);
        }
    }

    private void confirmar() {
        LocalDate fecha = (LocalDate) grupoDias.getSelectedToggle().getUserData();
        LocalTime hora = (LocalTime) grupoHoras.getSelectedToggle().getUserData();
        try {
            service.solicitarCita(SesionCliente.getCliente(), servicioElegido(), fecha, hora,
                    txtCalle.getText(), txtNumero.getText(), txtColonia.getText(), txtNota.getText());
            Cliente c = SesionCliente.getCliente();
            lblListoDetalle.setText(servicioElegido() + " para el " + fechaLarga(fecha) + " a las " + horaTexto(hora)
                    + "\nTe llamaremos al " + ClienteAccesoController.formatear(c.getTelefono())
                    + " para confirmarla. También puedes revisarla en «Mis citas y pedidos».");
            mostrarPaso(4);
        } catch (SoptecException ex) {
            mostrarError(AlertaUtil.mensajeUsuario(ex));
            if (ex.getMessage() != null && ex.getMessage().contains("horario")) {
                cargarHoras(fecha);
                mostrarPaso(2);
                mostrarError(AlertaUtil.mensajeUsuario(ex));
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado("Portal cliente", "SOLICITAR_CITA", ex);
        }
    }

    private void llenarResumen() {
        LocalDate fecha = (LocalDate) grupoDias.getSelectedToggle().getUserData();
        LocalTime hora = (LocalTime) grupoHoras.getSelectedToggle().getUserData();
        lblResServicio.setText(servicioElegido());
        lblResDia.setText(capitalizar(fechaLarga(fecha)));
        lblResHora.setText(horaTexto(hora));
        lblResDomicilio.setText(esInstalacion()
                ? txtCalle.getText().trim() + " #" + txtNumero.getText().trim() + ", Col. " + txtColonia.getText().trim()
                : "En nuestra tienda / lo confirmamos por teléfono");
        String nota = txtNota.getText() == null ? "" : txtNota.getText().trim();
        lblResNota.setText(nota.isEmpty() ? "(No escribiste nada, está bien)" : nota);
        Cliente c = SesionCliente.getCliente();
        lblResTelefono.setText(c != null ? ClienteAccesoController.formatear(c.getTelefono()) : "-");
    }

    private void mostrarPaso(int nuevo) {
        paso = nuevo;
        VBox[] paneles = {paso1, paso2, paso3, pasoListo};
        for (int i = 0; i < paneles.length; i++) {
            boolean ver = (i + 1) == nuevo;
            paneles[i].setVisible(ver);
            paneles[i].setManaged(ver);
        }
        Label[] chips = {lblPaso1, lblPaso2, lblPaso3};
        for (int i = 0; i < chips.length; i++) {
            chips[i].getStyleClass().removeAll("paso-chip-activo", "paso-chip-hecho");
            if (i + 1 == nuevo) {
                chips[i].getStyleClass().add("paso-chip-activo");
            } else if (i + 1 < nuevo) {
                chips[i].getStyleClass().add("paso-chip-hecho");
            }
        }
        boolean terminado = nuevo == 4;
        barraNavegacion.setVisible(!terminado);
        barraNavegacion.setManaged(!terminado);
        btnAtras.setText(nuevo == 1 ? "Volver al inicio" : "Regresar");
        btnSiguiente.getStyleClass().removeAll("boton-primario", "boton-exito");
        if (nuevo == 3) {
            btnSiguiente.setText("Confirmar mi cita");
            btnSiguiente.getStyleClass().add("boton-exito");
        } else {
            btnSiguiente.setText("Siguiente");
            btnSiguiente.getStyleClass().add("boton-primario");
        }
        mostrarError(null);
    }

    @FXML
    private void irInicio(ActionEvent event) {
        PortalClienteController.irA(PortalClienteController.VISTA_INICIO);
    }

    @FXML
    private void verMisCitas(ActionEvent event) {
        PortalClienteController.irA(PortalClienteController.VISTA_MIS_SERVICIOS);
    }

    // ------------------------------------------------------------------ utilidades

    private String servicioElegido() {
        Toggle t = grupoServicio.getSelectedToggle();
        return t != null ? (String) t.getUserData() : "";
    }

    private boolean esInstalacion() {
        return grupoServicio.getSelectedToggle() == tgInstalacion;
    }

    private boolean vacio(TextField campo) {
        return campo.getText() == null || campo.getText().trim().isEmpty();
    }

    private void mostrarError(String mensaje) {
        boolean hay = mensaje != null && !mensaje.isBlank();
        if (hay) {
            lblError.setText(mensaje);
        }
        lblError.setVisible(hay);
        lblError.setManaged(hay);
    }

    /** Evita que un botón de día/hora se “desmarque” al tocarlo dos veces. */
    private static void mantenerSeleccion(ToggleGroup grupo) {
        grupo.selectedToggleProperty().addListener((o, anterior, nuevo) -> {
            if (nuevo == null && anterior != null && grupo.getToggles().contains(anterior)) {
                grupo.selectToggle(anterior);
            }
        });
    }

    /** jueves 26 de noviembre */
    public static String fechaLarga(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        return fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES_MX));
    }

    /** 9:00 a.m. / 1:00 p.m. */
    public static String horaTexto(LocalTime hora) {
        if (hora == null) {
            return "";
        }
        int h = hora.getHour();
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return String.format("%d:%02d %s", h12, hora.getMinute(), h < 12 ? "a.m." : "p.m.");
    }

    static String capitalizar(String texto) {
        return texto == null || texto.isEmpty() ? "" : texto.substring(0, 1).toUpperCase(ES_MX) + texto.substring(1);
    }

    /** Lista de días como texto (útil para pruebas). */
    List<LocalDate> diasMostrados() {
        return service.diasDisponibles();
    }
}
