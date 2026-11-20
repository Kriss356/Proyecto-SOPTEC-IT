package com.mycompany.soptec.it.pruebas.relaciones;

import com.mycompany.soptec.it.dao.ClienteDao;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.ClienteTelefono;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ClienteTelefonoService;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.servicio.ProveedorService;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Relaciones · No se permiten registros duplicados")
class DuplicadosTest extends PruebaBD {

    private final ClienteService clientes = new ClienteService();

    @Test
    @DisplayName("Cliente con RFC repetido es rechazado")
    void clienteRfcDuplicado() throws Exception {
        Cliente original = clientes.crear(DatosPrueba.nuevoCliente());
        Cliente copia = DatosPrueba.nuevoCliente();
        copia.setRfc(original.getRfc());

        ValidacionException ex = assertThrows(ValidacionException.class, () -> clientes.crear(copia));
        assertTrue(ex.getMessage().contains("RFC"), ex.getMessage());
        assertEquals(1, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cliente WHERE rfc = ?", original.getRfc()));
    }

    @Test
    @DisplayName("Cliente con correo repetido (sin importar mayúsculas) es rechazado")
    void clienteCorreoDuplicado() throws Exception {
        Cliente original = clientes.crear(DatosPrueba.nuevoCliente());
        Cliente copia = DatosPrueba.nuevoCliente();
        copia.setCorreo(original.getCorreo().toUpperCase());
        assertThrows(ValidacionException.class, () -> clientes.crear(copia));
    }

    @Test
    @DisplayName("Actualizar un cliente con el RFC de otro es rechazado")
    void actualizarConRfcDeOtro() throws Exception {
        Cliente a = clientes.crear(DatosPrueba.nuevoCliente());
        Cliente b = clientes.crear(DatosPrueba.nuevoCliente());
        b.setRfc(a.getRfc());
        assertThrows(ValidacionException.class, () -> clientes.actualizar(b));
    }

    @Test
    @DisplayName("La restricción UNIQUE de MySQL también bloquea el duplicado (error 1062)")
    void restriccionUniqueEnBaseDeDatos() throws Exception {
        Cliente original = clientes.crear(DatosPrueba.nuevoCliente());
        Cliente copia = DatosPrueba.nuevoCliente();
        copia.setRfc(original.getRfc());
        // Se usa el DAO directamente (sin la validación del servicio) para comprobar la restricción de la tabla.
        ValidacionException ex = assertThrows(ValidacionException.class, () -> new ClienteDao().crear(copia));
        assertTrue(ex.getMessage().toLowerCase().contains("duplicado"));
    }

    @Test
    @DisplayName("El mismo teléfono no se registra dos veces para el mismo cliente")
    void telefonoDuplicado() throws Exception {
        Cliente c = clientes.crear(DatosPrueba.nuevoCliente());
        String numero = DatosPrueba.telefonoUnico();
        ClienteTelefonoService telefonos = new ClienteTelefonoService();
        telefonos.crear(new ClienteTelefono(c.getIdCliente(), numero, true));
        assertThrows(ValidacionException.class,
                () -> telefonos.crear(new ClienteTelefono(c.getIdCliente(), numero, true)));
    }

    @Test
    @DisplayName("Empleado con correo repetido es rechazado")
    void empleadoCorreoDuplicado() throws Exception {
        EmpleadoService empleados = new EmpleadoService();
        Empleado e = empleados.crear(DatosPrueba.nuevoEmpleado("Agente de Ventas"));
        Empleado copia = DatosPrueba.nuevoEmpleado("Técnico Instalador");
        copia.setCorreo(e.getCorreo());
        assertThrows(ValidacionException.class, () -> empleados.crear(copia));
    }

    @Test
    @DisplayName("Material con la misma descripción es rechazado")
    void materialDuplicado() throws Exception {
        MaterialService materiales = new MaterialService();
        Material m = materiales.crear(DatosPrueba.nuevoMaterial());
        assertThrows(ValidacionException.class, () -> materiales.crear(
                new Material(0, m.getDescripcionMaterial(), new BigDecimal("10.00"), 1, true)));
    }

    @Test
    @DisplayName("Proveedor con nombre o correo repetido es rechazado")
    void proveedorDuplicado() throws Exception {
        ProveedorService proveedores = new ProveedorService();
        Proveedor p = proveedores.crear(DatosPrueba.nuevoProveedor());

        Proveedor mismoNombre = DatosPrueba.nuevoProveedor();
        mismoNombre.setNombreProveedor(p.getNombreProveedor());
        assertThrows(ValidacionException.class, () -> proveedores.crear(mismoNombre));

        Proveedor mismoCorreo = DatosPrueba.nuevoProveedor();
        mismoCorreo.setCorreoProveedor(p.getCorreoProveedor());
        assertThrows(ValidacionException.class, () -> proveedores.crear(mismoCorreo));
    }
}
