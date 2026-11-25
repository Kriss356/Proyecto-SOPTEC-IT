package soptecit.controladores;

import com.mycompany.soptec.it.modelo.Servicio;
import com.mycompany.soptec.it.servicio.ServicioService;
import com.mycompany.soptec.it.util.AlertaUtil;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import soptecit.util.Tablas;

/**
 * CRUD del catálogo de servicios (tabla servicio) con gestión de imagen.
 */
public class CatalogoServiciosController implements Initializable {

    private static final String MODULO = "Servicios";
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    private final ServicioService service = new ServicioService();

    @FXML private CheckBox chkInactivosServ;
    @FXML private TextField txtBuscarServ;
    @FXML private TextField txtConsultarIdServ;
    @FXML private TableView<Servicio> tblServicios;
    @FXML private TableColumn<Servicio, String> colServId;
    @FXML private TableColumn<Servicio, String> colServNombre;
    @FXML private TableColumn<Servicio, String> colServDesc;
    @FXML private TableColumn<Servicio, String> colServPrecio;
    @FXML private TableColumn<Servicio, String> colServEstado;
    @FXML private Label lblConteoServ;

    @FXML private Label lblModoServicio;
    @FXML private TextField txtNombreServ;
    @FXML private TextField txtPrecioServ;
    @FXML private TextArea txtDescServ;
    @FXML private ImageView imgServicio;
    @FXML private Label lblRutaImgServ;

    private Servicio seleccionado;
    private String rutaImagen;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colServId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getIdServicio())));
        colServNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreServicio()));
        colServDesc.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDescripcion() != null ? d.getValue().getDescripcion() : ""));
        colServPrecio.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getPrecio())));
        colServEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstadoTexto()));
        colServEstado.setCellFactory(c -> Tablas.crearCeldaEstado());
        tblServicios.setPlaceholder(new Label("No hay servicios en el catálogo"));
        tblServicios.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            if (s != null) {
                cargarEnFormulario(s);
            }
        });
        cargarServicios(null);
    }

    @FXML
    private void cargarServicios(ActionEvent event) {
        try {
            List<Servicio> lista = service.buscar(txtBuscarServ.getText(), chkInactivosServ.isSelected());
            tblServicios.setItems(FXCollections.observableArrayList(lista));
            lblConteoServ.setText(lista.size() + (lista.size() == 1 ? " servicio" : " servicios"));
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "LISTAR_SERVICIOS", ex);
        }
    }

    @FXML
    private void consultarPorId(ActionEvent event) {
        try {
            int id = Validador.parsearIdPositivo(txtConsultarIdServ.getText(), "ID de servicio", MODULO, "CONSULTAR_SERVICIO");
            Servicio s = service.consultarPorId(id);
            if (s == null) {
                AlertaUtil.advertencia("No encontrado", "No existe un servicio con el ID " + id + ".");
                return;
            }
            cargarEnFormulario(s);
            AlertaUtil.info("Servicio encontrado", s.getNombreServicio() + "\nPrecio: " + MONEDA.format(s.getPrecio())
                    + "\nRegistro: " + s.getEstadoTexto());
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CONSULTAR_SERVICIO", ex);
        }
    }

    @FXML
    private void seleccionarImagen(ActionEvent event) {
        try {
            String ruta = GestorImagenes.elegirYGuardarImagen(imgServicio.getScene().getWindow(),
                    "Seleccionar imagen del servicio", "servicios", MODULO);
            if (ruta != null) {
                rutaImagen = ruta;
                lblRutaImgServ.setText(ruta);
                imgServicio.setImage(GestorImagenes.cargarImagen(ruta));
            }
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "SELECCIONAR_IMAGEN", ex);
        }
    }

    @FXML
    private void quitarImagen(ActionEvent event) {
        rutaImagen = null;
        lblRutaImgServ.setText("Sin imagen seleccionada");
        imgServicio.setImage(null);
    }

    @FXML
    private void guardar(ActionEvent event) {
        try {
            Servicio s = service.crear(construir(new Servicio(), "CREAR_SERVICIO"));
            AlertaUtil.info("Servicio registrado", "Se agregó '" + s.getNombreServicio() + "' al catálogo.");
            limpiar(null);
            cargarServicios(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "CREAR_SERVICIO", ex);
        }
    }

    @FXML
    private void actualizar(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona un servicio", "Elige un servicio de la tabla para actualizarlo.");
            return;
        }
        try {
            Servicio s = construir(seleccionado, "ACTUALIZAR_SERVICIO");
            service.actualizar(s);
            AlertaUtil.info("Servicio actualizado", "Se actualizó '" + s.getNombreServicio()
                    + "'. Las citas y ventas ya registradas conservan su precio original.");
            limpiar(null);
            cargarServicios(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ACTUALIZAR_SERVICIO", ex);
        }
    }

    @FXML
    private void desactivar(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona un servicio", "Elige un servicio de la tabla.");
            return;
        }
        try {
            service.desactivar(seleccionado.getIdServicio());
            AlertaUtil.info("Servicio desactivado", "El servicio ya no aparecerá en citas ni ventas nuevas.");
            limpiar(null);
            cargarServicios(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "DESACTIVAR_SERVICIO", ex);
        }
    }

    @FXML
    private void reactivar(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona un servicio", "Marca 'Mostrar inactivos' y elige un servicio.");
            return;
        }
        try {
            service.reactivar(seleccionado.getIdServicio());
            AlertaUtil.info("Servicio reactivado", "El servicio vuelve a estar disponible.");
            limpiar(null);
            cargarServicios(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "REACTIVAR_SERVICIO", ex);
        }
    }

    @FXML
    private void eliminar(ActionEvent event) {
        if (seleccionado == null) {
            AlertaUtil.advertencia("Selecciona un servicio", "Elige un servicio de la tabla.");
            return;
        }
        if (!AlertaUtil.confirmar("Eliminar servicio", "¿Eliminar definitivamente '" + seleccionado.getNombreServicio() + "'?")) {
            return;
        }
        try {
            service.eliminarFisico(seleccionado.getIdServicio());
            AlertaUtil.info("Servicio eliminado", "El servicio se eliminó del catálogo.");
            limpiar(null);
            cargarServicios(null);
        } catch (Exception ex) {
            AlertaUtil.errorControlado(MODULO, "ELIMINAR_FISICO_SERVICIO", ex);
        }
    }

    @FXML
    private void limpiar(ActionEvent event) {
        seleccionado = null;
        lblModoServicio.setText("Registrar servicio");
        txtNombreServ.clear();
        txtPrecioServ.clear();
        txtDescServ.clear();
        quitarImagen(null);
        tblServicios.getSelectionModel().clearSelection();
    }

    private void cargarEnFormulario(Servicio s) {
        seleccionado = s;
        lblModoServicio.setText("Editando servicio #" + s.getIdServicio());
        txtNombreServ.setText(s.getNombreServicio());
        txtPrecioServ.setText(s.getPrecio().toPlainString());
        txtDescServ.setText(s.getDescripcion() != null ? s.getDescripcion() : "");
        rutaImagen = s.getImagen();
        if (rutaImagen != null && !rutaImagen.isBlank()) {
            lblRutaImgServ.setText(GestorImagenes.existeImagen(rutaImagen) ? rutaImagen
                    : "La imagen guardada ya no se encuentra en la carpeta de la aplicación");
            imgServicio.setImage(GestorImagenes.cargarImagen(rutaImagen));
        } else {
            quitarImagen(null);
        }
    }

    private Servicio construir(Servicio base, String op) throws Exception {
        base.setNombreServicio(txtNombreServ.getText());
        base.setDescripcion(txtDescServ.getText());
        base.setPrecio(Validador.parsearDecimalNoNegativo(txtPrecioServ.getText(), "Precio", MODULO, op));
        base.setImagen(rutaImagen);
        return base;
    }
}
