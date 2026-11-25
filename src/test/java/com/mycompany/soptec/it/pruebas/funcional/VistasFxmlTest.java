package com.mycompany.soptec.it.pruebas.funcional;

import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba funcional / de integración: carga cada vista FXML con su controlador y CSS
 * (los controladores consultan la BD de pruebas al iniciar).
 */
@DisplayName("Funcional · Carga de vistas JavaFX")
class VistasFxmlTest extends PruebaBD {

    private static final String[] VISTAS = {
        "/soptecit/vistas/Login.fxml",
        "/soptecit/vistas/Principal.fxml",
        "/soptecit/vistas/Inicio.fxml",
        "/soptecit/vistas/Seguimiento.fxml",
        "/soptecit/vistas/Ventas.fxml",
        "/soptecit/vistas/Citas.fxml",
        "/soptecit/vistas/Levantamientos.fxml",
        "/soptecit/vistas/Administracion.fxml",
        "/soptecit/vistas/ComprasAlmacen.fxml",
        "/soptecit/vistas/Instalaciones.fxml",
        "/soptecit/vistas/DetalleFactura.fxml",
        "/soptecit/vistas/LogsPruebas.fxml",
        "/soptecit/vistas/SolicitudesClientes.fxml",
        // Práctica CRUD con relaciones muchos a muchos (también se cargan dentro de Ventas, Administración y Compras)
        "/soptecit/vistas/VentasFacturacion.fxml",
        "/soptecit/vistas/CatalogoServicios.fxml",
        "/soptecit/vistas/CuentasCobrar.fxml",
        "/soptecit/vistas/PersonalProyecto.fxml",
        "/soptecit/vistas/OrdenesCompra.fxml",
        "/soptecit/vistas/MaterialProveedor.fxml",
        "/soptecit/vistas/MaterialProyecto.fxml",
        // Portal del cliente
        "/soptecit/vistas/ClienteAcceso.fxml",
        "/soptecit/vistas/PortalCliente.fxml",
        "/soptecit/vistas/ClienteInicio.fxml",
        "/soptecit/vistas/ClienteCita.fxml",
        "/soptecit/vistas/ClienteTienda.fxml",
        "/soptecit/vistas/ClienteMisServicios.fxml",
        "/fxml/MainCrud.fxml",
        "/fxml/ClientesView.fxml",
        "/fxml/TelefonosClienteView.fxml",
        "/fxml/EmpleadosView.fxml",
        "/fxml/MaterialesView.fxml",
        "/fxml/ProveedoresView.fxml",
        "/fxml/ProyectosView.fxml",
        "/fxml/CitasView.fxml",
        "/fxml/LevantamientosView.fxml",
        "/fxml/LogsPruebasView.fxml"
    };

    @Test
    @DisplayName("Las 36 vistas FXML cargan con su controlador sin errores")
    void cargarTodasLasVistas() throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Platform.startup(inicio::countDown);
        } catch (IllegalStateException yaIniciado) {
            inicio.countDown();
        }
        assertTrue(inicio.await(10, TimeUnit.SECONDS), "JavaFX Toolkit no inició a tiempo");

        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<String> vistaConError = new AtomicReference<>();
        CountDownLatch fin = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                for (String ruta : VISTAS) {
                    vistaConError.set(ruta);
                    FXMLLoader loader = new FXMLLoader(VistasFxmlTest.class.getResource(ruta));
                    Parent root = loader.load();
                    assertNotNull(root, "Vista nula: " + ruta);
                    assertNotNull(loader.getController(), "Controlador nulo: " + ruta);
                }
                vistaConError.set(null);
            } catch (Throwable ex) {
                error.set(ex);
            } finally {
                fin.countDown();
            }
        });

        assertTrue(fin.await(30, TimeUnit.SECONDS), "Tiempo agotado al cargar las vistas FXML");
        if (error.get() != null) {
            fail("Error al cargar " + vistaConError.get() + ": " + error.get().getMessage(), error.get());
        }
    }
}
