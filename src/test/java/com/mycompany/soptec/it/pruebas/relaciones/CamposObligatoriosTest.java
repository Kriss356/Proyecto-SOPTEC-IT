package com.mycompany.soptec.it.pruebas.relaciones;

import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.ClienteTelefono;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Levantamiento;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaUnitaria;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ClienteTelefonoService;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.servicio.LevantamientoService;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.servicio.ProveedorService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Las validaciones de campos obligatorios se ejecutan ANTES de tocar la base de datos,
 * por eso estas pruebas no necesitan MySQL.
 */
@DisplayName("Validaciones · Campos obligatorios")
class CamposObligatoriosTest extends PruebaUnitaria {

    private static void rechazaConMensaje(Executable accion, String textoEsperado) {
        ValidacionException ex = assertThrows(ValidacionException.class, accion);
        assertTrue(ex.getMessage().toLowerCase().contains(textoEsperado.toLowerCase()),
                "Mensaje esperado con '" + textoEsperado + "' pero fue: " + ex.getMessage());
    }

    @Test
    @DisplayName("Cliente: RFC, nombre y correo son obligatorios")
    void cliente() {
        ClienteService s = new ClienteService();
        Cliente sinRfc = DatosPrueba.nuevoCliente();
        sinRfc.setRfc("  ");
        rechazaConMensaje(() -> s.crear(sinRfc), "RFC");

        Cliente sinNombre = DatosPrueba.nuevoCliente();
        sinNombre.setNombreCliente("");
        rechazaConMensaje(() -> s.crear(sinNombre), "Nombre del cliente");

        Cliente sinCorreo = DatosPrueba.nuevoCliente();
        sinCorreo.setCorreo(null);
        rechazaConMensaje(() -> s.crear(sinCorreo), "Correo");

        rechazaConMensaje(() -> s.crear(null), "requeridos");
    }

    @Test
    @DisplayName("Cliente: formatos inválidos de RFC y correo")
    void clienteFormatos() {
        ClienteService s = new ClienteService();
        Cliente rfcMal = DatosPrueba.nuevoCliente();
        rfcMal.setRfc("RFC_MAL");
        rechazaConMensaje(() -> s.crear(rfcMal), "RFC");

        Cliente correoMal = DatosPrueba.nuevoCliente();
        correoMal.setCorreo("correo-sin-arroba");
        rechazaConMensaje(() -> s.crear(correoMal), "correo");
    }

    @Test
    @DisplayName("Empleado: nombre, teléfono, puesto y correo son obligatorios")
    void empleado() {
        EmpleadoService s = new EmpleadoService();
        Empleado sinNombre = DatosPrueba.nuevoEmpleado("Agente de Ventas");
        sinNombre.setNombre(null);
        rechazaConMensaje(() -> s.crear(sinNombre), "Nombre");

        Empleado sinPuesto = DatosPrueba.nuevoEmpleado(" ");
        rechazaConMensaje(() -> s.crear(sinPuesto), "Puesto");

        Empleado telCorto = DatosPrueba.nuevoEmpleado("Agente de Ventas");
        telCorto.setTelefono("33123");
        rechazaConMensaje(() -> s.crear(telCorto), "10 dígitos");
    }

    @Test
    @DisplayName("Material: descripción obligatoria, precio y existencias no negativos")
    void material() {
        MaterialService s = new MaterialService();
        rechazaConMensaje(() -> s.crear(new Material(0, "", new BigDecimal("1"), 1, true)), "Descripción");
        rechazaConMensaje(() -> s.crear(new Material(0, "Cable", new BigDecimal("-5"), 1, true)), "negativo");
        rechazaConMensaje(() -> s.crear(new Material(0, "Cable", BigDecimal.ONE, -3, true)), "negativas");
    }

    @Test
    @DisplayName("Proveedor: nombre, teléfono y correo obligatorios")
    void proveedor() {
        ProveedorService s = new ProveedorService();
        Proveedor sinNombre = DatosPrueba.nuevoProveedor();
        sinNombre.setNombreProveedor("");
        rechazaConMensaje(() -> s.crear(sinNombre), "Nombre del proveedor");

        Proveedor sinTelefono = DatosPrueba.nuevoProveedor();
        sinTelefono.setTelefonoProveedor(null);
        rechazaConMensaje(() -> s.crear(sinTelefono), "Teléfono");
    }

    @Test
    @DisplayName("Proyecto: nombre, fechas coherentes, costo y cliente obligatorios")
    void proyecto() {
        ProyectoService s = new ProyectoService();
        Proyecto sinNombre = DatosPrueba.nuevoProyecto(1);
        sinNombre.setNombreProyecto(" ");
        rechazaConMensaje(() -> s.crear(sinNombre), "Nombre del proyecto");

        Proyecto sinFecha = DatosPrueba.nuevoProyecto(1);
        sinFecha.setFechaInicio(null);
        rechazaConMensaje(() -> s.crear(sinFecha), "fecha de inicio");

        Proyecto fechasInvertidas = DatosPrueba.nuevoProyecto(1);
        fechasInvertidas.setFechaEstimadaFin(LocalDate.now().minusDays(5));
        rechazaConMensaje(() -> s.crear(fechasInvertidas), "anterior");

        Proyecto costoNegativo = DatosPrueba.nuevoProyecto(1);
        costoNegativo.setCostoTotal(new BigDecimal("-1"));
        rechazaConMensaje(() -> s.crear(costoNegativo), "negativo");

        rechazaConMensaje(() -> s.crear(DatosPrueba.nuevoProyecto(0)), "cliente");
    }

    @Test
    @DisplayName("Cita y levantamiento: fecha, hora y relación obligatorias")
    void citaYLevantamiento() {
        CitaService citas = new CitaService();
        Cita sinFecha = DatosPrueba.nuevaCita(1);
        sinFecha.setFechaCita(null);
        rechazaConMensaje(() -> citas.crear(sinFecha), "fecha");

        Cita sinHora = DatosPrueba.nuevaCita(1);
        sinHora.setHoraCita(null);
        rechazaConMensaje(() -> citas.crear(sinHora), "hora");

        rechazaConMensaje(() -> citas.crear(DatosPrueba.nuevaCita(0)), "cliente");

        LevantamientoService lev = new LevantamientoService();
        Levantamiento sinFechaVisita = DatosPrueba.nuevoLevantamiento(1, null);
        sinFechaVisita.setFechaVisita(null);
        rechazaConMensaje(() -> lev.crear(sinFechaVisita), "fecha de visita");
        rechazaConMensaje(() -> lev.crear(DatosPrueba.nuevoLevantamiento(0, null)), "proyecto");
    }

    @Test
    @DisplayName("Teléfono de cliente: se debe elegir un cliente válido")
    void telefonoSinCliente() {
        rechazaConMensaje(() -> new ClienteTelefonoService().crear(new ClienteTelefono(0, "3312345678", true)),
                "cliente");
    }

    @Test
    @DisplayName("Operaciones sin seleccionar registro (ID 0) se rechazan con mensaje claro")
    void idsNoSeleccionados() {
        rechazaConMensaje(() -> new ClienteService().consultarPorId(0), "mayor a cero");
        rechazaConMensaje(() -> new EmpleadoService().desactivar(0), "Selecciona");
        rechazaConMensaje(() -> new ProyectoService().eliminarFisico(-1), "Selecciona");
    }
}
