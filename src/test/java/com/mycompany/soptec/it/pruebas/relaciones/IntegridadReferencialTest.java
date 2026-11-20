package com.mycompany.soptec.it.pruebas.relaciones;

import com.mycompany.soptec.it.dao.CitaDao;
import com.mycompany.soptec.it.dao.ClienteDao;
import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.ClienteTelefono;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Levantamiento;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ClienteTelefonoService;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.servicio.LevantamientoService;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.servicio.ProveedorService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Relaciones · No se eliminan registros con dependientes")
class IntegridadReferencialTest extends PruebaBD {

    private final ClienteService clientes = new ClienteService();
    private final ProyectoService proyectos = new ProyectoService();
    private final CitaService citas = new CitaService();

    @Test
    @DisplayName("Cliente con teléfono no se puede desactivar ni eliminar")
    void clienteConTelefono() throws Exception {
        Cliente c = clientes.crear(DatosPrueba.nuevoCliente());
        new ClienteTelefonoService().crear(new ClienteTelefono(c.getIdCliente(), DatosPrueba.telefonoUnico(), true));

        assertThrows(IntegridadReferencialException.class, () -> clientes.desactivar(c.getIdCliente()));
        assertThrows(IntegridadReferencialException.class, () -> clientes.eliminarFisico(c.getIdCliente()));
        assertTrue(clientes.consultarPorId(c.getIdCliente()).isActivo(), "El cliente debe seguir intacto");
    }

    @Test
    @DisplayName("Cliente con citas o proyectos no se puede eliminar")
    void clienteConCitaYProyecto() throws Exception {
        Cliente conCita = clientes.crear(DatosPrueba.nuevoCliente());
        citas.crear(DatosPrueba.nuevaCita(conCita.getIdCliente()));
        assertThrows(IntegridadReferencialException.class, () -> clientes.eliminarFisico(conCita.getIdCliente()));

        Cliente conProyecto = clientes.crear(DatosPrueba.nuevoCliente());
        proyectos.crear(DatosPrueba.nuevoProyecto(conProyecto.getIdCliente()));
        assertThrows(IntegridadReferencialException.class, () -> clientes.eliminarFisico(conProyecto.getIdCliente()));
        assertNotNull(clientes.consultarPorId(conProyecto.getIdCliente()));
    }

    @Test
    @DisplayName("La llave foránea de MySQL bloquea el DELETE directo (error 1451)")
    void llaveForaneaEnBaseDeDatos() throws Exception {
        Cliente c = clientes.crear(DatosPrueba.nuevoCliente());
        new ClienteTelefonoService().crear(new ClienteTelefono(c.getIdCliente(), DatosPrueba.telefonoUnico(), true));
        // DAO directo: sin validación previa del servicio, la BD debe impedirlo (ON DELETE RESTRICT).
        assertThrows(IntegridadReferencialException.class, () -> new ClienteDao().eliminarFisico(c.getIdCliente()));
        assertEquals(1, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cliente WHERE id_cliente = ?", c.getIdCliente()));
    }

    @Test
    @DisplayName("Proyecto con levantamientos no se puede desactivar ni eliminar")
    void proyectoConLevantamiento() throws Exception {
        Cliente c = clientes.crear(DatosPrueba.nuevoCliente());
        Proyecto p = proyectos.crear(DatosPrueba.nuevoProyecto(c.getIdCliente()));
        new LevantamientoService().crear(DatosPrueba.nuevoLevantamiento(p.getIdProyecto(), null));

        assertThrows(IntegridadReferencialException.class, () -> proyectos.desactivar(p.getIdProyecto()));
        assertThrows(IntegridadReferencialException.class, () -> proyectos.eliminarFisico(p.getIdProyecto()));
    }

    @Test
    @DisplayName("Cita vinculada a un levantamiento no se puede eliminar")
    void citaConLevantamiento() throws Exception {
        Cliente c = clientes.crear(DatosPrueba.nuevoCliente());
        Proyecto p = proyectos.crear(DatosPrueba.nuevoProyecto(c.getIdCliente()));
        Cita cita = citas.crear(DatosPrueba.nuevaCita(c.getIdCliente()));
        Levantamiento l = new LevantamientoService().crear(DatosPrueba.nuevoLevantamiento(p.getIdProyecto(), cita.getIdCita()));

        assertThrows(IntegridadReferencialException.class, () -> citas.eliminarFisico(cita.getIdCita()));

        // Eliminando primero el dependiente, ya se permite (orden inverso correcto).
        new LevantamientoService().eliminarFisico(l.getIdLevantamiento());
        assertDoesNotThrow(() -> citas.eliminarFisico(cita.getIdCita()));
    }

    @Test
    @DisplayName("Material usado en un proyecto y proveedor con materiales no se eliminan")
    void materialYProveedorConRelaciones() throws Exception {
        Cliente c = clientes.crear(DatosPrueba.nuevoCliente());
        Proyecto p = proyectos.crear(DatosPrueba.nuevoProyecto(c.getIdCliente()));
        MaterialService materiales = new MaterialService();
        ProveedorService proveedores = new ProveedorService();
        Material m = materiales.crear(DatosPrueba.nuevoMaterial());
        Proveedor prov = proveedores.crear(DatosPrueba.nuevoProveedor());
        BaseDatosPrueba.ejecutar("INSERT INTO proyecto_material (id_proyecto, id_material, cantidad) VALUES (?, ?, 5)",
                p.getIdProyecto(), m.getIdMaterial());
        BaseDatosPrueba.ejecutar("INSERT INTO material_proveedor (id_material, id_proveedor) VALUES (?, ?)",
                m.getIdMaterial(), prov.getIdProveedor());

        assertThrows(IntegridadReferencialException.class, () -> materiales.eliminarFisico(m.getIdMaterial()));
        assertThrows(IntegridadReferencialException.class, () -> proveedores.eliminarFisico(prov.getIdProveedor()));
        assertThrows(IntegridadReferencialException.class, () -> proyectos.eliminarFisico(p.getIdProyecto()));
    }

    @Test
    @DisplayName("Empleado asignado a un proyecto no se elimina")
    void empleadoAsignado() throws Exception {
        Cliente c = clientes.crear(DatosPrueba.nuevoCliente());
        Proyecto p = proyectos.crear(DatosPrueba.nuevoProyecto(c.getIdCliente()));
        EmpleadoService empleados = new EmpleadoService();
        Empleado e = empleados.crear(DatosPrueba.nuevoEmpleado("Técnico Instalador"));
        BaseDatosPrueba.ejecutar("INSERT INTO proyecto_empleado (id_proyecto, id_empleado) VALUES (?, ?)",
                p.getIdProyecto(), e.getIdEmpleado());

        assertThrows(IntegridadReferencialException.class, () -> empleados.eliminarFisico(e.getIdEmpleado()));
    }

    @Test
    @DisplayName("No se puede registrar con un registro padre inexistente (error 1452)")
    void padreInexistente() {
        assertThrows(IntegridadReferencialException.class,
                () -> proyectos.crear(DatosPrueba.nuevoProyecto(999999)));
        assertThrows(IntegridadReferencialException.class,
                () -> new CitaDao().crear(DatosPrueba.nuevaCita(999999)));
    }
}
