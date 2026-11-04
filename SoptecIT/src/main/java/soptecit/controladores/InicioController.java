package soptecit.controladores;

import java.net.URL;
import java.time.LocalTime;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import soptecit.util.Tablas;

public class InicioController implements Initializable {

    @FXML private Label lblSaludo;
    @FXML private Label lblProyectosActivos;
    @FXML private Label lblCitasHoy;
    @FXML private Label lblPorCobrar;
    @FXML private Label lblBajoStock;
    @FXML private TableView<String[]> tblProyectos;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        int hora = LocalTime.now().getHour();
        String saludo = hora < 12 ? "¡Buen día" : (hora < 19 ? "¡Buenas tardes" : "¡Buenas noches");
        lblSaludo.setText(saludo + "!");

        // Datos de ejemplo (sustituir por consulta a la BD)
        // Folio | Cliente | Servicio | Área actual | Avance (0-100) | Estado
        Tablas.cargar(tblProyectos, new String[][]{
            {"PRY-2026-014", "Corporativo Avante", "Cableado Cat6", "Instalaciones", "65", "En tiempo"},
            {"PRY-2026-013", "Clínica del Sol", "Fibra óptica", "Compras-Almacén", "30", "Por surtir"},
            {"PRY-2026-012", "Oficinas LTJO", "Voz y datos", "Instalaciones", "90", "En tiempo"},
            {"PRY-2026-011", "Sucursal Sur", "Cableado Cat6A", "Instalaciones", "40", "Retrasado"},
            {"PRY-2026-010", "Hotel Real Minerva", "Fibra óptica", "Administración", "15", "En revisión"},
            {"PRY-2026-009", "Despacho Ortega", "Mantenimiento", "Administración", "100", "Terminado"}
        });
    }

    @FXML
    private void irACitas(ActionEvent event) {
        PrincipalController.irA("Citas");
    }

    @FXML
    private void irASeguimiento(ActionEvent event) {
        PrincipalController.irA("Seguimiento");
    }
}
