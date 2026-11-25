package soptecit.controladores;

import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.PedidoCliente;
import com.mycompany.soptec.it.servicio.PortalClienteService;
import com.mycompany.soptec.it.util.AppLogger;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import soptecit.util.SesionCliente;

/**
 * Inicio del Portal del cliente: cuatro opciones grandes y, si existe, la próxima cita.
 */
public class ClienteInicioController implements Initializable {

    @FXML private Label lblBienvenida;
    @FXML private VBox panelProximaCita;
    @FXML private Label lblProximaCita;
    @FXML private Label lblProximaCitaEstado;
    @FXML private Button btnAgendar;
    @FXML private Button btnInstalacion;
    @FXML private Button btnTienda;
    @FXML private Button btnMisServicios;
    @FXML private Label lblAvisoPedido;
    @FXML private Label lblAyudaInicio;

    private final PortalClienteService service = new PortalClienteService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblAyudaInicio.setText("¿Tienes dudas? Toca el botón naranja «Ayuda» que está arriba a la derecha "
                + "o llámanos al " + PortalClienteController.TELEFONO_AYUDA + ".");
        Cliente cliente = SesionCliente.getCliente();
        if (cliente == null) {
            return;
        }
        lblBienvenida.setText("¿Qué necesitas hoy, " + SesionCliente.primerNombre() + "?");
        try {
            Cita proxima = service.proximaCita(cliente.getIdCliente());
            if (proxima != null) {
                lblProximaCita.setText(ClienteCitaController.capitalizar(ClienteCitaController.fechaLarga(proxima.getFechaCita()))
                        + " · " + ClienteCitaController.horaTexto(proxima.getHoraCita()));
                lblProximaCitaEstado.setText(PortalClienteService.servicioDeCita(proxima) + " · "
                        + PortalClienteService.estadoCitaParaCliente(proxima.getEstadoCita()));
                panelProximaCita.setVisible(true);
                panelProximaCita.setManaged(true);
            }
            long listos = service.pedidosDeCliente(cliente.getIdCliente()).stream()
                    .filter(p -> PedidoCliente.LISTO.equalsIgnoreCase(p.getEstado())).count();
            if (listos > 0) {
                lblAvisoPedido.setText(listos == 1
                        ? "¡Buenas noticias! Tienes un pedido listo para recoger. Revísalo en «Mis citas y pedidos»."
                        : "¡Buenas noticias! Tienes " + listos + " pedidos listos para recoger.");
                lblAvisoPedido.setVisible(true);
                lblAvisoPedido.setManaged(true);
            }
        } catch (Exception ex) {
            // El inicio debe abrir aunque la base de datos falle; solo se registra en el log.
            AppLogger.registrarExcepcion("Portal cliente", "ClienteInicioController", "RESUMEN_INICIO", ex);
        }
    }

    @FXML
    private void abrirCita(ActionEvent event) {
        PortalClienteController.irA(PortalClienteController.VISTA_CITA);
    }

    @FXML
    private void abrirInstalacion(ActionEvent event) {
        Object ctrl = PortalClienteController.irA(PortalClienteController.VISTA_CITA);
        if (ctrl instanceof ClienteCitaController) {
            ((ClienteCitaController) ctrl).preseleccionar(PortalClienteService.SERV_INSTALACION);
        }
    }

    @FXML
    private void abrirTienda(ActionEvent event) {
        PortalClienteController.irA(PortalClienteController.VISTA_TIENDA);
    }

    @FXML
    private void abrirMisServicios(ActionEvent event) {
        PortalClienteController.irA(PortalClienteController.VISTA_MIS_SERVICIOS);
    }
}
