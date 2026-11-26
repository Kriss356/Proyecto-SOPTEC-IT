package soptecit.controladores;

import com.mycompany.soptec.it.dao.Conexion;
import com.mycompany.soptec.it.util.AppLogger;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Levantamiento;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.modelo.Venta;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.servicio.LevantamientoService;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.servicio.ProveedorService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.servicio.VentaService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;
import soptecit.util.BusquedaTabla;
import soptecit.util.MejorasPantalla;
import soptecit.util.Mensajes;

/**
 * Ventana principal estilizada de SOPTEC-IT.
 * Carga cada módulo en el panel central y controla la visibilidad por área y estado de MySQL.
 */
public class PrincipalController implements Initializable {

    @FXML private ToggleGroup grupoMenu;
    @FXML private ToggleButton btnInicio;
    @FXML private ToggleButton btnSeguimiento;
    @FXML private ToggleButton btnVentas;
    @FXML private ToggleButton btnCitas;
    @FXML private ToggleButton btnSolicitudes;
    @FXML private ToggleButton btnLevantamientos;
    @FXML private ToggleButton btnAdministracion;
    @FXML private ToggleButton btnCompras;
    @FXML private ToggleButton btnInstalaciones;
    @FXML private ToggleButton btnLogs;
    @FXML private Label lblSeccionSoporte;

    @FXML private Label lblIniciales;
    @FXML private Label lblUsuario;
    @FXML private Label lblArea;
    @FXML private Label lblTitulo;
    @FXML private Label lblRuta;
    @FXML private Label lblFecha;
    @FXML private Button btnEstadoConexion;
    @FXML private TextField txtBuscarGlobal;
    @FXML private ScrollPane contenedor;

    private static PrincipalController instancia;
    private String usuarioActual = "Administrador";
    /** Solo el Administrador general puede ver y abrir Logs. */
    private boolean esAdminGeneral;
    /** Módulo abierto (para regresar el menú si el usuario decide quedarse a guardar). */
    private String moduloActual = "Inicio";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instancia = this;

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, yyyy", new Locale("es", "MX"));
        String fecha = LocalDate.now().format(formato);
        lblFecha.setText(fecha.substring(0, 1).toUpperCase() + fecha.substring(1));

        grupoMenu.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                grupoMenu.selectToggle(anterior);
            }
        });

        configurarBusquedaGlobal();
        // Si al guardar aparece un error, lo capturado sigue contando como "sin guardar"
        com.mycompany.soptec.it.util.AlertaUtil.setAlMostrarAviso(soptecit.util.CambiosPendientes::alMostrarError);
        // Al cerrar la ventana con la X también se avisa si hay cambios sin guardar
        contenedor.sceneProperty().addListener((o, a, escena) -> {
            if (escena != null) {
                escena.windowProperty().addListener((ow, aw, ventana) -> {
                    if (ventana != null) {
                        ventana.setOnCloseRequest(e -> {
                            if (!confirmarSalida()) {
                                e.consume();
                            }
                        });
                    }
                });
                if (escena.getWindow() != null) {
                    escena.getWindow().setOnCloseRequest(e -> {
                        if (!confirmarSalida()) {
                            e.consume();
                        }
                    });
                }
            }
        });
        // Si al guardar falta un dato obligatorio, se marca en rojo en la pantalla abierta
        com.mycompany.soptec.it.util.AlertaUtil.setMarcarCamposVacios(
                () -> soptecit.util.Requeridos.marcarVacios(contenedor.getContent()));
        // Ctrl+F: pone el cursor en el buscador de la pantalla abierta (o en la barra de arriba)
        contenedor.sceneProperty().addListener((o, a, escena) -> {
            if (escena != null) {
                escena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(KeyCode.F,
                        javafx.scene.input.KeyCombination.SHORTCUT_DOWN), this::enfocarBusqueda);
            }
        });
        // Crea las tablas nuevas (avances, movimientos, documentos) en cuanto se entra al sistema
        try {
            com.mycompany.soptec.it.dao.MigracionBD.asegurarEsquema();
        } catch (Exception ex) {
            AppLogger.registrarExcepcion("Sistema", "PrincipalController", "ASEGURAR_ESQUEMA", ex);
        }

        actualizarBadgeConexion();
        cargarModulo("Inicio", null);
    }

    /** Configura usuario en sesión y aplica el filtro de módulos por área. */
    public void setUsuario(String usuario, String area) {
        this.usuarioActual = usuario;
        lblUsuario.setText(usuario);
        lblIniciales.setText(usuario.length() >= 2 ? usuario.substring(0, 2).toUpperCase() : usuario.toUpperCase());
        boolean esAdmin = area == null || area.startsWith("Administrador");
        lblArea.setText(esAdmin ? "Acceso a todas las áreas" : "Área: " + area);

        mostrar(btnVentas, esAdmin || "Ventas".equals(area));
        mostrar(btnCitas, esAdmin || "Ventas".equals(area));
        // Lo que piden los clientes desde el Portal del cliente (citas y pedidos de la tienda).
        mostrar(btnSolicitudes, esAdmin || "Ventas".equals(area) || "Compras-Almacén".equals(area));
        mostrar(btnLevantamientos, esAdmin || "Levantamientos".equals(area));
        mostrar(btnAdministracion, esAdmin || "Administración".equals(area));
        mostrar(btnCompras, esAdmin || "Compras-Almacén".equals(area));
        mostrar(btnInstalaciones, esAdmin || "Instalaciones".equals(area));
        // Logs (bitácora de errores y pruebas de fallos) es exclusivo del Administrador general.
        esAdminGeneral = "Administrador general".equals(area);
        mostrar(btnLogs, esAdminGeneral);
        lblSeccionSoporte.setVisible(esAdminGeneral);
        lblSeccionSoporte.setManaged(esAdminGeneral);

        cargarModulo("Inicio", null);
    }

    public static String getUsuarioActual() {
        return instancia != null ? instancia.usuarioActual : "Administrador";
    }

    public static void refrescarConexionGlobal() {
        if (instancia != null) {
            instancia.actualizarBadgeConexion();
        }
    }

    private void mostrar(ToggleButton boton, boolean visible) {
        boton.setVisible(visible);
        boton.setManaged(visible);
    }

    @FXML
    private void navegar(ActionEvent event) {
        ToggleButton boton = (ToggleButton) event.getSource();
        if (!confirmarSalida()) {
            actualizarEncabezado(moduloActual);
            return;
        }
        cargarModulo((String) boton.getUserData(), null);
        actualizarBadgeConexion();
    }

    @FXML
    private void verificarConexion(ActionEvent event) {
        boolean ok = actualizarBadgeConexion();
        if (ok) {
            Mensajes.info("Conexión activa", "Conexión verificada correctamente con la base de datos soptec_it.");
        } else {
            Mensajes.advertencia("Sin conexión con MySQL",
                    "No se pudo conectar con la base de datos soptec_it.\nRevisa que el servicio MySQL de XAMPP esté activo."
                    + (Conexion.isSimularFalloConexion()
                            ? "\nLa simulación de caída está activa en 'Logs'; desactívala para reconectar."
                            : ""));
        }
    }

    public boolean actualizarBadgeConexion() {
        boolean ok = Conexion.verificarConexion();
        btnEstadoConexion.getStyleClass().removeAll("chip-conexion-ok", "chip-conexion-error");
        if (ok) {
            btnEstadoConexion.setText("MySQL: Conectado");
            btnEstadoConexion.getStyleClass().add("chip-conexion-ok");
        } else {
            btnEstadoConexion.setText("MySQL: Sin conexión");
            btnEstadoConexion.getStyleClass().add("chip-conexion-error");
        }
        return ok;
    }

    /** Cambia de módulo desde cualquier controlador: PrincipalController.irA("Citas"); */
    public static void irA(String modulo) {
        if (instancia != null && instancia.confirmarSalida()) {
            instancia.cargarModulo(modulo, null);
        }
    }

    /** ¿El usuario en sesión puede abrir ese módulo? */
    public static boolean puedeVer(String modulo) {
        if (instancia == null) {
            return false;
        }
        switch (modulo) {
            case "ComprasAlmacen":
                return instancia.btnCompras.isVisible();
            case "Ventas":
                return instancia.btnVentas.isVisible();
            case "Instalaciones":
                return instancia.btnInstalaciones.isVisible();
            default:
                return true;
        }
    }

    /** Abre Compras-Almacén › Órdenes de compra con el material ya cargado para pedirlo. */
    public static void pedirMaterial(int idMaterial) {
        if (instancia == null) {
            return;
        }
        if (!puedeVer("ComprasAlmacen")) {
            Mensajes.info("Material con pocas existencias",
                    "Avisa al área de Compras-Almacén para que haga el pedido de este material.");
            return;
        }
        if (!instancia.confirmarSalida()) {
            return;
        }
        OrdenesCompraController.prepararPedido(idMaterial);
        instancia.cargarModulo("ComprasAlmacen", null, "Órdenes de compra", null);
    }

    public static void irASeguimientoConFiltro(String filtro) {
        if (instancia != null) {
            instancia.cargarModulo("Seguimiento", filtro);
        }
    }

    private void cargarModulo(String modulo, String parametro) {
        cargarModulo(modulo, parametro, null, null);
    }

    /**
     * Abre el módulo; si se indica, selecciona la pestaña y escribe el texto en su buscador
     * (la tabla se filtra y se abre el primer resultado para editarlo).
     */
    private void cargarModulo(String modulo, String parametro, String pestana, String filtro) {
        if ("LogsPruebas".equals(modulo) && !esAdminGeneral) {
            Mensajes.advertencia("Acceso restringido", "Logs solo está disponible para el Administrador general.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/soptecit/vistas/" + modulo + ".fxml"));
            Parent vista = loader.load();
            // Búsqueda en vivo, combos con autocompletado y observaciones ampliables en toda la pantalla
            MejorasPantalla.aplicar(vista);
            soptecit.util.CambiosPendientes.vigilar(vista);
            if (filtro != null || pestana != null) {
                abrirResultado(vista, pestana, filtro);
            }
            if ("Seguimiento".equals(modulo) && parametro != null && !parametro.isBlank()) {
                Object ctrl = loader.getController();
                if (ctrl instanceof SeguimientoController) {
                    ((SeguimientoController) ctrl).buscarDesdeExterno(parametro);
                }
            }
            contenedor.setContent(vista);
            contenedor.setVvalue(0);
            moduloActual = modulo;
            actualizarEncabezado(modulo);
        } catch (IOException | RuntimeException ex) {
            AppLogger.registrarExcepcion("Navegacion", "PrincipalController", "CARGAR_MODULO_" + modulo, ex);
            Mensajes.error("No se pudo abrir el módulo",
                    "Ocurrió un problema al cargar " + modulo + ". El detalle fue guardado en soptec_errores.log.");
        }
    }

    // =========================================================================
    // BÚSQUEDA GLOBAL (barra de arriba)
    // =========================================================================
    /** Un resultado de la barra de búsqueda: qué es y a dónde lleva. */
    private static final class Resultado {
        final String grupo;
        final String titulo;
        final String detalle;
        final String modulo;
        final String pestana;
        final String filtro;
        final String texto;

        Resultado(String grupo, String titulo, String detalle, String modulo, String pestana, String filtro) {
            this.grupo = grupo;
            this.titulo = titulo == null ? "" : titulo;
            this.detalle = detalle == null ? "" : detalle;
            this.modulo = modulo;
            this.pestana = pestana;
            this.filtro = filtro;
            this.texto = sinAcentos(this.titulo + " " + this.detalle);
        }
    }

    private final Popup popupBusqueda = new Popup();
    private final VBox listaResultados = new VBox(2);
    private final PauseTransition esperaTecleo = new PauseTransition(Duration.millis(180));
    private List<Resultado> indiceGlobal = new ArrayList<>();
    private long indiceCargadoEn;
    private boolean cargandoIndice;
    private Resultado primerResultado;

    private void configurarBusquedaGlobal() {
        txtBuscarGlobal.setPromptText("Buscar cliente, proyecto, cita, material, folio...");
        txtBuscarGlobal.setTooltip(new javafx.scene.control.Tooltip(
                "Escribe para buscar en todo el sistema. Ctrl+F lleva al buscador de la pantalla abierta."));
        listaResultados.setPadding(new Insets(8));
        listaResultados.getStyleClass().add("busqueda-global-popup");
        listaResultados.setPrefWidth(460);
        ScrollPane scroll = new ScrollPane(listaResultados);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(430);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.getStylesheets().add(getClass().getResource("/soptecit/css/estilos.css").toExternalForm());
        popupBusqueda.getContent().add(scroll);
        popupBusqueda.setAutoHide(true);

        esperaTecleo.setOnFinished(e -> buscarGlobal());
        txtBuscarGlobal.textProperty().addListener((o, a, n) -> esperaTecleo.playFromStart());
        txtBuscarGlobal.focusedProperty().addListener((o, a, enfocado) -> {
            if (enfocado) {
                prepararIndice(false);
                if (txtBuscarGlobal.getText() != null && txtBuscarGlobal.getText().trim().length() >= 2) {
                    buscarGlobal();
                }
            }
        });
        txtBuscarGlobal.setOnAction(e -> {
            if (primerResultado != null) {
                irAResultado(primerResultado);
            } else if (txtBuscarGlobal.getText() != null && !txtBuscarGlobal.getText().isBlank()) {
                cargarModulo("Seguimiento", txtBuscarGlobal.getText());
            }
        });
        txtBuscarGlobal.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.DOWN && popupBusqueda.isShowing()) {
                listaResultados.getChildren().stream().filter(n -> n instanceof Button).findFirst()
                        .ifPresent(Node::requestFocus);
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popupBusqueda.hide();
                txtBuscarGlobal.clear();
            }
        });
    }

    /** Lee (en segundo plano) clientes, proyectos, citas, materiales, etc. para buscar sin esperar. */
    private void prepararIndice(boolean forzar) {
        if (cargandoIndice || (!forzar && System.currentTimeMillis() - indiceCargadoEn < 20000)) {
            return;
        }
        cargandoIndice = true;
        boolean verVentas = btnVentas.isVisible();
        boolean verCitas = btnCitas.isVisible();
        boolean verLev = btnLevantamientos.isVisible();
        boolean verAdmin = btnAdministracion.isVisible();
        boolean verCompras = btnCompras.isVisible();
        boolean verInst = btnInstalaciones.isVisible();
        Thread hilo = new Thread(() -> {
            List<Resultado> r = new ArrayList<>();
            try {
                if (verVentas) {
                    for (Cliente c : new ClienteService().listar(false)) {
                        r.add(new Resultado("Clientes", c.getNombreCliente(),
                                unir(c.getRfc(), c.getCorreo(), c.getColonia(), c.getEstado()),
                                "Ventas", "Clientes", c.getNombreCliente()));
                    }
                    for (Venta v : new VentaService().listar(false)) {
                        r.add(new Resultado("Ventas / facturas", v.getFolio() + " · " + v.getClienteNombre(),
                                unir(v.getProyectoNombre(), v.getEstado()), "Ventas", "Ventas y facturación", v.getFolio()));
                    }
                }
                if (verVentas || verInst) {
                    for (Proyecto p : new ProyectoService().listar(false)) {
                        String folio = "PRY-" + String.format("%04d", p.getIdProyecto());
                        r.add(verVentas
                                ? new Resultado("Proyectos", p.getNombreProyecto(), unir(folio, p.getClienteNombre(), p.getEstado()),
                                        "Ventas", "Proyectos y propuestas", p.getNombreProyecto())
                                : new Resultado("Proyectos", p.getNombreProyecto(), unir(folio, p.getClienteNombre(), p.getEstado()),
                                        "Instalaciones", null, folio));
                    }
                }
                if (verCitas) {
                    for (Cita c : new CitaService().listar(false)) {
                        r.add(new Resultado("Citas", c.getNombreCliente(),
                                unir(c.getFechaCita() != null ? c.getFechaCita().toString() : null,
                                        c.getHoraCita() != null ? c.getHoraCita().toString() : null, c.getEstadoCita()),
                                "Citas", null, c.getNombreCliente()));
                    }
                }
                if (verLev) {
                    for (Levantamiento l : new LevantamientoService().listar(false)) {
                        r.add(new Resultado("Levantamientos", l.getNombreProyecto(),
                                unir(l.getNombreCliente(), l.getFechaVisita() != null ? l.getFechaVisita().toString() : null),
                                "Levantamientos", null, l.getNombreProyecto()));
                    }
                }
                if (verCompras) {
                    for (Material m : new MaterialService().listar(false)) {
                        r.add(new Resultado("Materiales", m.getDescripcionMaterial(), "Existencias: " + m.getExistencias(),
                                "ComprasAlmacen", "Material", m.getDescripcionMaterial()));
                    }
                    for (Proveedor pr : new ProveedorService().listar(false)) {
                        r.add(new Resultado("Proveedores", pr.getNombreProveedor(),
                                unir(pr.getTelefonoProveedor(), pr.getCorreoProveedor()),
                                "ComprasAlmacen", "Proveedores y precios", pr.getNombreProveedor()));
                    }
                }
                if (verAdmin) {
                    for (Empleado em : new EmpleadoService().listar(false)) {
                        r.add(new Resultado("Empleados", em.getNombre(), unir(em.getPuesto(), em.getTelefono()),
                                "Administracion", "Personal / Empleados", em.getNombre()));
                    }
                }
            } catch (Exception ex) {
                AppLogger.registrarExcepcion("Navegacion", "PrincipalController", "INDICE_BUSQUEDA_GLOBAL", ex);
            }
            Platform.runLater(() -> {
                indiceGlobal = r;
                indiceCargadoEn = System.currentTimeMillis();
                cargandoIndice = false;
                if (txtBuscarGlobal.isFocused()) {
                    buscarGlobal();
                }
            });
        }, "indice-busqueda-global");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void buscarGlobal() {
        String q = txtBuscarGlobal.getText() == null ? "" : txtBuscarGlobal.getText().trim();
        primerResultado = null;
        if (q.length() < 2) {
            popupBusqueda.hide();
            return;
        }
        String[] palabras = sinAcentos(q).split("\\s+");
        Map<String, List<Resultado>> grupos = new LinkedHashMap<>();
        int total = 0;
        for (Resultado r : indiceGlobal) {
            boolean ok = true;
            for (String p : palabras) {
                if (!r.texto.contains(p)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                List<Resultado> g = grupos.computeIfAbsent(r.grupo, k -> new ArrayList<>());
                if (g.size() < 5) {
                    g.add(r);
                    total++;
                }
            }
        }
        listaResultados.getChildren().clear();
        if (total == 0) {
            Label vacio = new Label(cargandoIndice ? "Buscando..." : "Sin resultados para \"" + q + "\"  ·  Enter busca en Seguimiento");
            vacio.getStyleClass().add("busqueda-global-detalle");
            vacio.setPadding(new Insets(8, 12, 8, 12));
            listaResultados.getChildren().add(vacio);
        }
        for (Map.Entry<String, List<Resultado>> g : grupos.entrySet()) {
            Label titulo = new Label(g.getKey().toUpperCase());
            titulo.getStyleClass().add("busqueda-global-grupo");
            listaResultados.getChildren().add(titulo);
            for (Resultado r : g.getValue()) {
                if (primerResultado == null) {
                    primerResultado = r;
                }
                Label t = new Label(r.titulo);
                t.getStyleClass().add("busqueda-global-titulo");
                Label d = new Label(r.detalle);
                d.getStyleClass().add("busqueda-global-detalle");
                VBox caja = new VBox(1, t, d);
                Button item = new Button();
                item.setGraphic(caja);
                item.setMaxWidth(Double.MAX_VALUE);
                item.setAlignment(Pos.CENTER_LEFT);
                item.getStyleClass().setAll("busqueda-global-item");
                item.setOnAction(e -> irAResultado(r));
                listaResultados.getChildren().add(item);
            }
        }
        if (!popupBusqueda.isShowing() && txtBuscarGlobal.getScene() != null) {
            Bounds b = txtBuscarGlobal.localToScreen(txtBuscarGlobal.getBoundsInLocal());
            if (b != null) {
                popupBusqueda.show(txtBuscarGlobal, b.getMinX(), b.getMaxY() + 6);
            }
        }
    }

    private void enfocarBusqueda() {
        if (contenedor.getScene() == null || contenedor.getContent() == null) {
            return;
        }
        for (Node n : contenedor.getContent().lookupAll(".campo-busqueda")) {
            if (visibleEnPantalla(n)) {
                n.requestFocus();
                ((TextField) n).selectAll();
                return;
            }
        }
        txtBuscarGlobal.requestFocus();
    }

    private static boolean visibleEnPantalla(Node n) {
        for (Node p = n; p != null; p = p.getParent()) {
            if (!p.isVisible()) {
                return false;
            }
        }
        return n.getScene() != null;
    }

    private void irAResultado(Resultado r) {
        popupBusqueda.hide();
        if (!confirmarSalida()) {
            return;
        }
        cargarModulo(r.modulo, null, r.pestana, r.filtro);
        actualizarBadgeConexion();
    }

    /** En el módulo recién abierto: elige la pestaña, escribe en su buscador y abre el primer resultado. */
    private void abrirResultado(Parent vista, String pestana, String filtro) {
        Node area = vista;
        List<Node> nodos = new ArrayList<>();
        MejorasPantalla.recorrer(vista, nodos);
        if (pestana != null) {
            for (Node n : nodos) {
                if (n instanceof TabPane) {
                    for (Tab t : ((TabPane) n).getTabs()) {
                        if (pestana.equals(t.getText())) {
                            ((TabPane) n).getSelectionModel().select(t);
                            area = t.getContent();
                        }
                    }
                }
            }
        }
        if (filtro == null) {
            return;
        }
        List<Node> enArea = new ArrayList<>();
        MejorasPantalla.recorrer(area, enArea);
        for (Node n : enArea) {
            if (n instanceof TextField && BusquedaTabla.tablaDe((TextField) n) != null) {
                TextField campo = (TextField) n;
                campo.setText(filtro);
                TableView<?> tabla = BusquedaTabla.tablaDe(campo);
                if (!tabla.getItems().isEmpty()) {
                    tabla.getSelectionModel().selectFirst();
                    tabla.scrollTo(0);
                }
                return;
            }
        }
    }

    private static String unir(String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p != null && !p.isBlank()) {
                if (sb.length() > 0) {
                    sb.append("  ·  ");
                }
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }

    private static String sinAcentos(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private void actualizarEncabezado(String modulo) {
        switch (modulo) {
            case "Seguimiento":
                encabezado(btnSeguimiento, "Seguimiento de proyectos", "General  /  Consulta de avance en base de datos");
                break;
            case "Ventas":
                encabezado(btnVentas, "Ventas", "Áreas  /  Ventas  ·  Clientes, teléfonos, proyectos y facturación");
                break;
            case "Citas":
                encabezado(btnCitas, "Citas", "Áreas  /  Ventas  /  Agenda y CRUD de citas");
                break;
            case "SolicitudesClientes":
                encabezado(btnSolicitudes, "Solicitudes de clientes", "Áreas  /  Ventas  ·  Citas y pedidos que llegan desde el Portal del cliente");
                break;
            case "Levantamientos":
                encabezado(btnLevantamientos, "Levantamientos", "Áreas  /  Levantamientos  ·  Visitas, croquis y relación con proyectos");
                break;
            case "Administracion":
                encabezado(btnAdministracion, "Administración", "Áreas  /  Administración  ·  Expedientes, cobranza, cierre y personal");
                break;
            case "ComprasAlmacen":
                encabezado(btnCompras, "Compras-Almacén", "Áreas  /  Compras-Almacén  ·  Materiales, movimientos, órdenes y proveedores");
                break;
            case "Instalaciones":
                encabezado(btnInstalaciones, "Instalaciones", "Áreas  /  Instalaciones  ·  Avance de obra y técnicos");
                break;
            case "LogsPruebas":
                encabezado(btnLogs, "Logs", "Soporte  /  Bitácora de errores y pruebas de fallos (solo Administrador general)");
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
        if (soptecit.util.CambiosPendientes.haySinGuardar(contenedor.getContent())) {
            if (!confirmarSalida()) {
                return;
            }
        } else if (!Mensajes.confirmar("Cerrar sesión", "¿Deseas salir del sistema y volver al acceso?")) {
            return;
        }
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/soptecit/vistas/Login.fxml"));
            contenedor.getScene().setRoot(login);
            instancia = null;
        } catch (IOException ex) {
            AppLogger.registrarExcepcion("Autenticacion", "PrincipalController", "CERRAR_SESION", ex);
        }
    }

    @FXML
    private void salirAplicacion(ActionEvent event) {
        if (soptecit.util.CambiosPendientes.haySinGuardar(contenedor.getContent())) {
            if (confirmarSalida()) {
                Platform.exit();
            }
        } else if (Mensajes.confirmar("Cerrar la aplicación", "¿Deseas cerrar SOPTEC-IT?")) {
            Platform.exit();
        }
    }

    /**
     * Si en la pantalla abierta hay datos escritos sin guardar, pregunta antes de salir de ella.
     * @return true si se puede salir
     */
    boolean confirmarSalida() {
        if (!soptecit.util.CambiosPendientes.haySinGuardar(contenedor.getContent())) {
            return true;
        }
        int r = com.mycompany.soptec.it.util.AlertaUtil.elegir("Cambios sin guardar",
                "Escribiste datos en esta pantalla que todavía no se han guardado.\n"
                + "Si sales ahora, se perderán.", "Salir sin guardar", "Quedarme para guardar");
        if (r == 0) {
            soptecit.util.CambiosPendientes.descartar(contenedor.getContent()); // ya no vuelve a preguntar
            return true;
        }
        return false;
    }
}
