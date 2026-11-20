package com.mycompany.soptec.it.pruebas.imagenes;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Levantamiento;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.EntornoPruebas;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.servicio.LevantamientoService;
import com.mycompany.soptec.it.servicio.ProveedorService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.util.GestorImagenes;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Imágenes · Ruta guardada en la base de datos")
class ImagenesBaseDatosTest extends PruebaBD {

    @TempDir
    Path temporal;
    private Path carpetaApp;
    private final ClienteService clientes = new ClienteService();

    @BeforeEach
    void carpetaSimulada() {
        carpetaApp = temporal.resolve("Documents").resolve("SOPTEC-IT").resolve("imagenes");
        GestorImagenes.configurarDirectorioBase(carpetaApp);
    }

    @AfterEach
    void restaurar() {
        GestorImagenes.configurarDirectorioBase(EntornoPruebas.DIR_IMAGENES_PRUEBA);
    }

    private String guardar(String nombre, String categoria) throws Exception {
        return GestorImagenes.guardarImagen(DatosPrueba.crearPng(temporal.resolve("origen"), nombre).toFile(),
                categoria, "PruebasImagenes");
    }

    @Test
    @DisplayName("La imagen del cliente se almacena, su ruta se registra y se recupera igual")
    void imagenCliente() throws Exception {
        Cliente c = DatosPrueba.nuevoCliente();
        c.setImagen(guardar("cliente.png", "clientes"));
        Cliente creado = clientes.crear(c);

        Cliente consultado = clientes.consultarPorId(creado.getIdCliente());
        assertEquals(c.getImagen(), consultado.getImagen());
        assertArrayEquals(DatosPrueba.PNG_1X1, GestorImagenes.leerImagen(consultado.getImagen(), "Clientes"));
    }

    @Test
    @DisplayName("Empleados, proveedores y croquis de levantamientos guardan y recuperan su imagen")
    void imagenesOtrosModulos() throws Exception {
        Empleado e = DatosPrueba.nuevoEmpleado("Técnico Instalador");
        e.setImagen(guardar("empleado.png", "empleados"));
        Empleado emp = new EmpleadoService().crear(e);
        assertTrue(GestorImagenes.existeImagen(new EmpleadoService().consultarPorId(emp.getIdEmpleado()).getImagen()));

        Proveedor p = DatosPrueba.nuevoProveedor();
        p.setImagen(guardar("logo.png", "proveedores"));
        Proveedor prov = new ProveedorService().crear(p);
        assertTrue(GestorImagenes.existeImagen(new ProveedorService().consultarPorId(prov.getIdProveedor()).getImagen()));

        Cliente cli = clientes.crear(DatosPrueba.nuevoCliente());
        Proyecto proy = new ProyectoService().crear(DatosPrueba.nuevoProyecto(cli.getIdCliente()));
        Levantamiento l = DatosPrueba.nuevoLevantamiento(proy.getIdProyecto(), null);
        l.setCroquis(guardar("croquis.png", "croquis"));
        Levantamiento lev = new LevantamientoService().crear(l);
        String croquis = new LevantamientoService().consultarPorId(lev.getIdLevantamiento()).getCroquis();
        assertArrayEquals(DatosPrueba.PNG_1X1, GestorImagenes.leerImagen(croquis, "Levantamientos"));
    }

    @Test
    @DisplayName("Si falla el almacenamiento de la nueva imagen, el registro conserva su imagen anterior")
    void falloDeAlmacenamientoNoAlteraRegistro() throws Exception {
        Cliente c = DatosPrueba.nuevoCliente();
        String imagenAnterior = guardar("anterior.png", "clientes");
        c.setImagen(imagenAnterior);
        Cliente creado = clientes.crear(c);

        // Flujo del formulario "Elegir imagen" + "Actualizar" con la carpeta de imágenes inaccesible.
        Path bloqueo = Files.write(temporal.resolve("disco-no-disponible"), new byte[]{1});
        GestorImagenes.configurarDirectorioBase(bloqueo.resolve("imagenes"));
        assertThrows(SoptecException.class, () -> {
            String nueva = GestorImagenes.guardarImagen(
                    DatosPrueba.crearPng(temporal.resolve("origen"), "nueva.png").toFile(), "clientes", "Clientes");
            creado.setImagen(nueva);
            clientes.actualizar(creado);
        });
        GestorImagenes.configurarDirectorioBase(carpetaApp);

        Cliente consultado = clientes.consultarPorId(creado.getIdCliente());
        assertEquals(imagenAnterior, consultado.getImagen(), "La BD no se modificó");
        assertTrue(GestorImagenes.existeImagen(consultado.getImagen()), "La imagen anterior sigue disponible");
    }

    @Test
    @DisplayName("Registro cuya imagen fue borrada del disco: se detecta sin afectar el registro")
    void imagenBorradaDelDisco() throws Exception {
        Cliente c = DatosPrueba.nuevoCliente();
        c.setImagen(guardar("temporal.png", "clientes"));
        Cliente creado = clientes.crear(c);
        Files.delete(GestorImagenes.resolverRuta(creado.getImagen()));

        Cliente consultado = clientes.consultarPorId(creado.getIdCliente());
        assertNotNull(consultado, "El cliente se sigue consultando con normalidad");
        assertFalse(GestorImagenes.existeImagen(consultado.getImagen()));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> GestorImagenes.leerImagen(consultado.getImagen(), "Clientes"));
        assertTrue(ex.getMessage().contains("ya no se encuentra"), ex.getMessage());
    }

    @Test
    @DisplayName("Una ruta de imagen demasiado larga para la columna se rechaza antes de guardar")
    void rutaDemasiadoLarga() {
        Cliente c = DatosPrueba.nuevoCliente();
        c.setImagen("clientes/" + "x".repeat(600) + ".png");
        assertThrows(ValidacionException.class, () -> clientes.crear(c));
    }
}
