package soptecit.controladores;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import soptecit.util.Mensajes;

/**
 * Ventana principal: menú lateral + barra superior.
 * Cada módulo (Ventas, Citas, etc.) es un FXML independiente que se carga en el centro.
 */
public class PrincipalController implements Initializable {

    @FXML private ToggleGroup grupoMenu;
    @FXML private ToggleButton btnInicio;
    @FXML private ToggleButton btnSeguimiento;
    @FXML private ToggleButton btnVentas;
    @FXML private ToggleButton btnCitas;
    @FXML private ToggleButton btnLevantamientos;
    @FXML private ToggleButton btnAdministracion;
    @FXML private ToggleButton btnCompras;
    @FXML private ToggleButton btnInstalaciones;

    @FXML private Label lblIniciales;
    @FXML private Label lblUsuario;
    @FXML private Label lblArea;
    @FXML private Label lblTitulo;
    @FXML private Label lblRuta;
    @FXML private Label lblFecha;
    @FXML private TextField txtBuscarGlobal;
    @FXML private ScrollPane contenedor;

    /** Permite que otros módulos (por ejemplo Inicio) cambien de pantalla. */
    private static PrincipalController instancia;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instancia = this;

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, yyyy", new Locale("es", "MX"));
        String fecha = LocalDate.now().format(formato);
        lblFecha.setText(fecha.substring(0, 1).toUpperCase() + fecha.substring(1));

        // Evita que el menú quede sin ninguna opción seleccionada
        grupoMenu.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                grupoMenu.selectToggle(anterior);
            }
        });

        // Búsqueda global: abre Seguimiento de proyectos
        txtBuscarGlobal.setOnAction(e -> irA("Seguimiento"));

        cargarModulo("Inicio");
    }

    /** Lo llama el Login después de validar al usuario. */
    public void setUsuario(String usuario, String area) {
        lblUsuario.setText(usuario);
        lblIniciales.setText(usuario.length() >= 2 ? usuario.substring(0, 2).toUpperCase() : usuario.toUpperCase());
        boolean esAdmin = area == null || area.startsWith("Administrador");
        lblArea.setText(esAdmin ? "Acceso a todas las áreas" : "Área: " + area);

        if (!esAdmin) {
            // Cada área solo ve sus módulos (Inicio y Seguimiento siempre están visibles)
            mostrar(btnVentas, area.equals("Ventas"));
            mostrar(btnCitas, area.equals("Ventas"));
            mostrar(btnLevantamientos, area.equals("Levantamientos"));
            mostrar(btnAdministracion, area.equals("Administración"));
            mostrar(btnCompras, area.equals("Compras-Almacén"));
            mostrar(btnInstalaciones, area.equals("Instalaciones"));
        }
    }

    private void mostrar(ToggleButton boton, boolean visible) {
        boton.setVisible(visible);
        boton.setManaged(visible);
    }

    @FXML
    private void navegar(ActionEvent event) {
        ToggleButton boton = (ToggleButton) event.getSource();
        cargarModulo((String) boton.getUserData());
    }

    /** Cambia de módulo desde cualquier controlador: PrincipalController.irA("Citas"); */
    public static void irA(String modulo) {
        if (instancia != null) {
            instancia.cargarModulo(modulo);
        }
    }

    private void cargarModulo(String modulo) {
        try {
            Parent vista = FXMLLoader.load(getClass().getResource("/soptecit/vistas/" + modulo + ".fxml"));
            contenedor.setContent(vista);
            contenedor.setVvalue(0);
            actualizarEncabezado(modulo);
        } catch (IOException ex) {
            Mensajes.error("No se pudo abrir el módulo", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void actualizarEncabezado(String modulo) {
        switch (modulo) {
            case "Seguimiento":
                encabezado(btnSeguimiento, "Seguimiento de proyectos", "General  /  Consulta de avance para el cliente");
                break;
            case "Ventas":
                encabezado(btnVentas, "Ventas", "Áreas  /  Ventas  ·  Clientes, propuestas y facturación");
                break;
            case "Citas":
                encabezado(btnCitas, "Citas", "Áreas  /  Ventas  /  Agenda de citas");
                break;
            case "Levantamientos":
                encabezado(btnLevantamientos, "Levantamientos", "Áreas  /  Levantamientos  ·  Visitas y croquis de red");
                break;
            case "Administracion":
                encabezado(btnAdministracion, "Administración", "Áreas  /  Administración  ·  Expedientes, cobranza y cierre");
                break;
            case "ComprasAlmacen":
                encabezado(btnCompras, "Compras-Almacén", "Áreas  /  Compras-Almacén  ·  Material y órdenes de compra");
                break;
            case "Instalaciones":
                encabezado(btnInstalaciones, "Instalaciones", "Áreas  /  Instalaciones  ·  Avance de obra");
                break;
            default:
                encabezado(btnInicio, "Inicio", "SOPTEC-IT  /  Panel general");
        }
    }

    private void encabezado(ToggleButton boton, String titulo, String ruta) {
        boton.setSelected(true);
        lblTitulo.setText(titulo);
        lblRuta.setText(ruta);
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {
        if (!Mensajes.confirmar("Cerrar sesión", "¿Deseas salir del sistema?")) {
            return;
        }
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/soptecit/vistas/Login.fxml"));
            contenedor.getScene().setRoot(login);
            instancia = null;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** Cierra la aplicación completa (pide confirmación). */
    @FXML
    private void salirAplicacion(ActionEvent event) {
        if (Mensajes.confirmar("Cerrar la aplicación", "¿Deseas cerrar SOPTEC-IT?")) {
            Platform.exit();
        }
    }
}
