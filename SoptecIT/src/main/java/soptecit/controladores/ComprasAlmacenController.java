package soptecit.controladores;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import soptecit.util.Mensajes;
import soptecit.util.Tablas;

public class ComprasAlmacenController implements Initializable {

    // Material
    @FXML private TableView<String[]> tblInventario;
    @FXML private TextField txtClaveMaterial;
    @FXML private TextField txtNombreMaterial;
    @FXML private ComboBox<String> cmbCategoriaMaterial;
    @FXML private TextField txtMinimo;
    @FXML private TextField txtPrecioMaterial;

    // Movimientos
    @FXML private RadioButton rbEntrada;
    @FXML private RadioButton rbSalida;
    @FXML private ComboBox<String> cmbMaterialMov;
    @FXML private Spinner<Integer> spnCantidadMov;
    @FXML private DatePicker dpFechaMov;
    @FXML private ComboBox<String> cmbOrdenMov;
    @FXML private ComboBox<String> cmbProyectoMov;
    @FXML private ComboBox<String> cmbResponsableMov;
    @FXML private TableView<String[]> tblMovimientos;

    // Órdenes de compra
    @FXML private TableView<String[]> tblOrdenes;
    @FXML private ComboBox<String> cmbProveedorOrden;
    @FXML private ComboBox<String> cmbProyectoOrden;
    @FXML private ComboBox<String> cmbMaterialOrden;
    @FXML private TextField txtCantidadOrden;
    @FXML private TableView<String[]> tblPartidas;
    @FXML private Label lblTotalOrden;

    // Proveedores
    @FXML private TableView<String[]> tblProveedores;
    @FXML private TableView<String[]> tblPrecios;
    @FXML private Spinner<Integer> spnDiasEntrega;
    @FXML private TextField txtNombreProveedor;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        spnCantidadMov.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1));
        spnDiasEntrega.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90, 3));
        dpFechaMov.setValue(LocalDate.now());

        String[] materiales = {"Cable UTP Cat6 (bobina 305 m)", "Cable UTP Cat6A (bobina 305 m)", "Jack RJ45 Cat6",
            "Patch panel 24 puertos", "Rack 24U", "Fibra multimodo OM3", "Canaleta 40x25", "Patch cord Cat6 1 m"};
        cmbMaterialMov.getItems().addAll(materiales);
        cmbMaterialOrden.getItems().addAll(materiales);
        cmbOrdenMov.getItems().addAll("OC-0045", "OC-0044", "OC-0043");
        cmbProyectoMov.getItems().addAll("PRY-2026-014", "PRY-2026-013", "PRY-2026-012");
        cmbProyectoOrden.getItems().addAll("PRY-2026-014", "PRY-2026-013", "PRY-2026-012");
        cmbResponsableMov.getItems().addAll("Almacén - J. Torres", "Instalaciones - C. Méndez", "Instalaciones - R. Ramírez");
        cmbProveedorOrden.getItems().addAll("Redes y Cables GDL", "FiberMex", "Distribuidora Anixter");

        // Entrada usa orden de compra; salida usa proyecto destino
        rbSalida.selectedProperty().addListener((o, a, salida) -> {
            cmbProyectoMov.setDisable(!salida);
            cmbOrdenMov.setDisable(salida);
        });

        // Clave | Material | Categoría | Existencia | Mínimo | Precio | Proveedor | Estado
        Tablas.cargar(tblInventario, new String[][]{
            {"MAT-001", "Cable UTP Cat6 (bobina 305 m)", "Cable UTP", "14 bobinas", "6", "$2,850.00", "Redes y Cables GDL", "Disponible"},
            {"MAT-002", "Cable UTP Cat6A (bobina 305 m)", "Cable UTP", "2 bobinas", "10", "$4,390.00", "Redes y Cables GDL", "Bajo stock"},
            {"MAT-003", "Jack RJ45 Cat6", "Conectores", "40 piezas", "50", "$58.00", "Distribuidora Anixter", "Bajo stock"},
            {"MAT-004", "Patch panel 24 puertos", "Racks y gabinetes", "6 piezas", "2", "$1,250.00", "Distribuidora Anixter", "Disponible"},
            {"MAT-005", "Fibra multimodo OM3 (m)", "Fibra óptica", "0 metros", "200", "$24.00", "FiberMex", "Agotado"},
            {"MAT-006", "Rack 24U", "Racks y gabinetes", "3 piezas", "1", "$6,900.00", "Distribuidora Anixter", "Disponible"},
            {"MAT-007", "Canaleta 40x25 (2 m)", "Canalización", "85 piezas", "40", "$46.00", "Redes y Cables GDL", "Disponible"},
            {"MAT-008", "Patch cord Cat6 1 m", "Conectores", "120 piezas", "60", "$35.00", "Redes y Cables GDL", "Disponible"}
        });
        // Fecha | Tipo | Material | Cantidad | OC / Proyecto | Responsable
        Tablas.cargar(tblMovimientos, new String[][]{
            {"02/11/2026", "Salida", "Cable UTP Cat6 (bobina)", "4", "PRY-2026-014", "C. Méndez"},
            {"02/11/2026", "Salida", "Jack RJ45 Cat6", "48", "PRY-2026-014", "C. Méndez"},
            {"31/10/2026", "Entrada", "Cable UTP Cat6 (bobina)", "10", "OC-0044", "J. Torres"},
            {"30/10/2026", "Entrada", "Rack 24U", "2", "OC-0043", "J. Torres"},
            {"29/10/2026", "Salida", "Patch panel 24 puertos", "2", "PRY-2026-012", "R. Ramírez"}
        });
        // Orden | Proveedor | Proyecto | Entrega | Total | Estado
        Tablas.cargar(tblOrdenes, new String[][]{
            {"OC-0045", "FiberMex", "PRY-2026-013", "06/11/2026", "$18,400", "Solicitada"},
            {"OC-0044", "Redes y Cables GDL", "PRY-2026-014", "31/10/2026", "$28,500", "Recibida"},
            {"OC-0043", "Distribuidora Anixter", "PRY-2026-012", "30/10/2026", "$16,300", "Recibida"},
            {"OC-0042", "Redes y Cables GDL", "PRY-2026-011", "04/11/2026", "$9,870", "Parcial"}
        });
        Tablas.cargar(tblPartidas, new String[][]{});
        // Proveedor | Contacto | Teléfono | Entrega | Condiciones
        Tablas.cargar(tblProveedores, new String[][]{
            {"Redes y Cables GDL", "Ing. Mario Ruiz", "33 3615 2200", "2 días", "Crédito 30 días"},
            {"FiberMex", "Lic. Sofía Lara", "55 5280 1100", "5 días", "Contado"},
            {"Distribuidora Anixter", "Andrés Gil", "33 3880 4500", "3 días", "Crédito 15 días"}
        });
        // Material | Proveedor | Precio | Entrega | Actualizado
        Tablas.cargar(tblPrecios, new String[][]{
            {"Cable UTP Cat6 (bobina)", "Redes y Cables GDL", "$2,850.00", "2 días", "28/10/2026"},
            {"Cable UTP Cat6 (bobina)", "Distribuidora Anixter", "$2,990.00", "3 días", "20/10/2026"},
            {"Fibra multimodo OM3 (m)", "FiberMex", "$24.00", "5 días", "15/10/2026"},
            {"Jack RJ45 Cat6", "Distribuidora Anixter", "$58.00", "3 días", "20/10/2026"}
        });
    }

    @FXML
    private void agregarMaterial(ActionEvent event) {
        if (txtClaveMaterial.getText().trim().isEmpty() || txtNombreMaterial.getText().trim().isEmpty()) {
            Mensajes.error("Faltan datos", "Indica al menos la clave y el nombre del material.");
            return;
        }
        // TODO: INSERT INTO materiales (...)
        tblInventario.getItems().add(new String[]{txtClaveMaterial.getText(), txtNombreMaterial.getText(),
            cmbCategoriaMaterial.getValue() == null ? "" : cmbCategoriaMaterial.getValue(), "0", txtMinimo.getText(),
            txtPrecioMaterial.getText(), "", "Agotado"});
        txtClaveMaterial.clear();
        txtNombreMaterial.clear();
        txtMinimo.clear();
        txtPrecioMaterial.clear();
    }

    @FXML
    private void solicitarCompra(ActionEvent event) {
        String[] fila = tblInventario.getSelectionModel().getSelectedItem();
        if (fila != null) {
            cmbMaterialOrden.setValue(fila[1]);
        }
        Mensajes.info("Solicitar compra", "Completa la orden en la pestaña \"Órdenes de compra\".");
    }

    @FXML
    private void registrarMovimiento(ActionEvent event) {
        if (cmbMaterialMov.getEditor().getText().trim().isEmpty()) {
            Mensajes.error("Faltan datos", "Selecciona el material del movimiento.");
            return;
        }
        String tipo = rbEntrada.isSelected() ? "Entrada" : "Salida";
        String referencia = rbEntrada.isSelected() ? cmbOrdenMov.getValue() : cmbProyectoMov.getValue();
        // TODO: INSERT INTO movimientos (...) y actualizar existencia
        tblMovimientos.getItems().add(0, new String[]{dpFechaMov.getValue().toString(), tipo,
            cmbMaterialMov.getEditor().getText(), String.valueOf(spnCantidadMov.getValue()),
            referencia == null ? "" : referencia, cmbResponsableMov.getValue() == null ? "" : cmbResponsableMov.getValue()});
    }

    @FXML
    private void agregarPartida(ActionEvent event) {
        String material = cmbMaterialOrden.getEditor().getText().trim();
        if (material.isEmpty() || txtCantidadOrden.getText().trim().isEmpty()) {
            return;
        }
        // Precio de ejemplo; tomarlo de la lista de precios del proveedor
        tblPartidas.getItems().add(new String[]{material, txtCantidadOrden.getText().trim(), "$58.00", "-"});
        txtCantidadOrden.clear();
    }

    @FXML
    private void generarOrden(ActionEvent event) {
        if (cmbProveedorOrden.getValue() == null || tblPartidas.getItems().isEmpty()) {
            Mensajes.error("Faltan datos", "Selecciona el proveedor y agrega al menos una partida.");
            return;
        }
        Mensajes.info("Orden generada", "La orden de compra se envió al proveedor " + cmbProveedorOrden.getValue() + ".");
    }

    @FXML
    private void registrarRecepcion(ActionEvent event) {
        String[] orden = tblOrdenes.getSelectionModel().getSelectedItem();
        if (orden != null) {
            orden[5] = "Recibida";
            tblOrdenes.refresh();
        }
    }

    @FXML
    private void cancelarOrden(ActionEvent event) {
        String[] orden = tblOrdenes.getSelectionModel().getSelectedItem();
        if (orden != null && Mensajes.confirmar("Cancelar orden", "¿Cancelar la orden " + orden[0] + "?")) {
            orden[5] = "Cancelada";
            tblOrdenes.refresh();
        }
    }

    @FXML
    private void guardarProveedor(ActionEvent event) {
        if (txtNombreProveedor.getText().trim().isEmpty()) {
            Mensajes.error("Faltan datos", "Indica la razón social del proveedor.");
            return;
        }
        Mensajes.info("Proveedor registrado", "Se guardó " + txtNombreProveedor.getText() + ".");
    }
}
