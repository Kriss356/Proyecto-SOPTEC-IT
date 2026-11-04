package soptecit.controladores;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import soptecit.util.Mensajes;
import soptecit.util.Tablas;

/**
 * Ventana con el detalle de una factura (se abre desde el historial de Ventas).
 *
 * La factura llega como String[] en este orden:
 * 0 Folio | 1 Proyecto | 2 Cliente | 3 Emisión | 4 Total | 5 Estado |
 * 6 RFC | 7 Vencimiento | 8 Concepto | 9 Subtotal | 10 IVA | 11 Método de pago
 */
public class DetalleFacturaController {

    @FXML private VBox hojaFactura;
    @FXML private Label lblFolio;
    @FXML private Label lblCliente;
    @FXML private Label lblRfc;
    @FXML private Label lblEstado;
    @FXML private Label lblProyecto;
    @FXML private Label lblEmision;
    @FXML private Label lblVencimiento;
    @FXML private Label lblMetodo;
    @FXML private Label lblConcepto;
    @FXML private Label lblImporte;
    @FXML private Label lblSubtotal;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;

    /** Abre la ventana de la factura encima de la ventana indicada. */
    public static void abrir(String[] factura, Window ventanaPadre) {
        try {
            FXMLLoader loader = new FXMLLoader(DetalleFacturaController.class.getResource("/soptecit/vistas/DetalleFactura.fxml"));
            Parent root = loader.load();
            DetalleFacturaController controlador = loader.getController();
            controlador.setFactura(factura);

            Stage ventana = new Stage();
            ventana.setTitle("Factura " + factura[0] + " · SOPTEC-IT");
            ventana.getIcons().add(new Image(DetalleFacturaController.class.getResourceAsStream("/soptecit/img/icono.png")));
            ventana.initOwner(ventanaPadre);
            ventana.initModality(Modality.WINDOW_MODAL);
            ventana.setScene(new Scene(root));
            ventana.show();
        } catch (IOException ex) {
            Mensajes.error("No se pudo abrir la factura", ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void setFactura(String[] f) {
        lblFolio.setText(f[0]);
        lblProyecto.setText(f[1]);
        lblCliente.setText(f[2]);
        lblEmision.setText(f[3]);
        lblTotal.setText(f[4]);
        lblEstado.setText(f[5]);
        lblEstado.getStyleClass().setAll("label", "badge", Tablas.claseEstado(f[5]));
        lblRfc.setText("RFC: " + dato(f, 6));
        lblVencimiento.setText(dato(f, 7));
        lblConcepto.setText(dato(f, 8));
        lblImporte.setText(dato(f, 9));
        lblSubtotal.setText(dato(f, 9));
        lblIva.setText(dato(f, 10));
        lblMetodo.setText(dato(f, 11));
    }

    private static String dato(String[] f, int i) {
        return i < f.length && f[i] != null && !f[i].isEmpty() ? f[i] : "—";
    }

    @FXML
    private void imprimir(ActionEvent event) {
        PrinterJob trabajo = PrinterJob.createPrinterJob();
        if (trabajo == null) {
            Mensajes.error("Sin impresora", "No se encontró una impresora instalada.");
            return;
        }
        if (!trabajo.showPrintDialog(hojaFactura.getScene().getWindow())) {
            return; // el usuario canceló
        }
        // Ajusta la hoja al tamaño de la página
        PageLayout pagina = trabajo.getJobSettings().getPageLayout();
        double escala = Math.min(pagina.getPrintableWidth() / hojaFactura.getBoundsInParent().getWidth(),
                pagina.getPrintableHeight() / hojaFactura.getBoundsInParent().getHeight());
        Scale ajuste = new Scale(Math.min(escala, 1.0), Math.min(escala, 1.0));
        hojaFactura.getTransforms().add(ajuste);
        boolean impreso = trabajo.printPage(hojaFactura);
        hojaFactura.getTransforms().remove(ajuste);
        if (impreso) {
            trabajo.endJob();
        }
    }

    @FXML
    private void cerrar(ActionEvent event) {
        ((Stage) hojaFactura.getScene().getWindow()).close();
    }
}
