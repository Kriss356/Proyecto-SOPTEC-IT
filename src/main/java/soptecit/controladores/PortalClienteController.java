package soptecit.controladores;

import com.mycompany.soptec.it.util.AlertaUtil;
import com.mycompany.soptec.it.util.AppLogger;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import soptecit.util.Mensajes;
import soptecit.util.SesionCliente;

/**
 * Ventana principal del Portal del cliente: barra superior fija (Inicio, Ayuda, Salir)
 * y en el centro se carga la pantalla que el cliente eligió.
 */
public class PortalClienteController implements Initializable {

    /** Teléfono de atención que se muestra al cliente. Cámbialo por el número real de SOPTEC-IT. */
    public static final String TELEFONO_AYUDA = "55 1234 5678";
    /** Horario de atención que se muestra al cliente. */
    public static final String HORARIO_ATENCION = "lunes a sábado de 9:00 a 18:00";

    public static final String VISTA_INICIO = "ClienteInicio";
    public static final String VISTA_CITA = "ClienteCita";
    public static final String VISTA_TIENDA = "ClienteTienda";
    public static final String VISTA_MIS_SERVICIOS = "ClienteMisServicios";

    @FXML private Label lblSaludo;
    @FXML private Label lblSubtitulo;
    @FXML private Button btnInicio;
    @FXML private Button btnAyuda;
    @FXML private Button btnSalir;
    @FXML private ScrollPane contenedor;

    private static PortalClienteController instancia;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instancia = this;
        String nombre = SesionCliente.primerNombre();
        lblSaludo.setText(nombre.isEmpty() ? "Hola" : "Hola, " + nombre);
        cargar(VISTA_INICIO);
    }

    /**
     * Cambia la pantalla central desde cualquier controlador del portal.
     * @return el controlador de la pantalla cargada (o null si no se pudo abrir).
     */
    public static Object irA(String vista) {
        return instancia != null ? instancia.cargar(vista) : null;
    }

    private Object cargar(String vista) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/soptecit/vistas/" + vista + ".fxml"));
            Parent contenido = loader.load();
            contenedor.setContent(contenido);
            contenedor.setVvalue(0);
            btnInicio.setVisible(!VISTA_INICIO.equals(vista));
            btnInicio.setManaged(!VISTA_INICIO.equals(vista));
            lblSubtitulo.setText(subtituloPara(vista));
            return loader.getController();
        } catch (IOException ex) {
            AppLogger.registrarExcepcion("Portal cliente", "PortalClienteController", "CARGAR_" + vista, ex);
            AlertaUtil.error("No se pudo abrir esta opción",
                    "Intenta de nuevo. Si el problema sigue, llámanos al " + TELEFONO_AYUDA + ".");
            return null;
        }
    }

    private String subtituloPara(String vista) {
        switch (vista) {
            case VISTA_CITA:
                return "Agenda tu cita en 3 pasos";
            case VISTA_TIENDA:
                return "Tienda · aparta y paga al recoger";
            case VISTA_MIS_SERVICIOS:
                return "Tus citas, compras y proyectos";
            default:
                return "¿Qué quieres hacer hoy?";
        }
    }

    @FXML
    private void irInicio(ActionEvent event) {
        cargar(VISTA_INICIO);
    }

    @FXML
    private void mostrarAyuda(ActionEvent event) {
        Mensajes.info("¿Cómo te ayudamos?",
                "• Toca la opción que necesitas en la pantalla de inicio.\n"
                + "• Cada pantalla te guía paso a paso; siempre puedes regresar con «Regresar» o «Inicio».\n"
                + "• Tus citas quedan «Esperando confirmación» hasta que te llamemos.\n"
                + "• Lo que apartes en la tienda lo pagas al recogerlo.\n\n"
                + "Teléfono: " + TELEFONO_AYUDA + "\n"
                + "Horario: " + HORARIO_ATENCION);
    }

    @FXML
    private void salir(ActionEvent event) {
        if (!Mensajes.confirmar("Salir del portal", "¿Quieres cerrar tu sesión?")) {
            return;
        }
        SesionCliente.cerrar();
        instancia = null;
        try {
            Parent acceso = FXMLLoader.load(getClass().getResource("/soptecit/vistas/ClienteAcceso.fxml"));
            btnSalir.getScene().setRoot(acceso);
        } catch (IOException ex) {
            AppLogger.registrarExcepcion("Portal cliente", "PortalClienteController", "SALIR", ex);
        }
    }
}
