package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Empleados")
class EmpleadoCrudTest extends PruebaBD {

    private final EmpleadoService servicio = new EmpleadoService();

    @Test
    @DisplayName("Registrar empleado y consultarlo por ID")
    void registrarYConsultar() throws Exception {
        Empleado e = servicio.crear(DatosPrueba.nuevoEmpleado("Técnico Instalador"));
        assertTrue(e.getIdEmpleado() > 0);

        Empleado consultado = servicio.consultarPorId(e.getIdEmpleado());
        assertEquals(e.getNombre(), consultado.getNombre());
        assertEquals("Técnico Instalador", consultado.getPuesto());
        assertEquals(e.getCorreo(), consultado.getCorreo());
    }

    @Test
    @DisplayName("Listar y buscar empleados")
    void listarYBuscar() throws Exception {
        Empleado e = servicio.crear(DatosPrueba.nuevoEmpleado("Agente de Ventas"));
        assertTrue(servicio.listar(false).stream().anyMatch(x -> x.getIdEmpleado() == e.getIdEmpleado()));
        assertEquals(1, servicio.buscar(e.getCorreo(), false).size());
    }

    @Test
    @DisplayName("Editar y actualizar puesto y teléfono")
    void actualizar() throws Exception {
        Empleado e = servicio.crear(DatosPrueba.nuevoEmpleado("Técnico de Levantamiento"));
        e.setPuesto("Compras y Almacén");
        e.setTelefono("3399001122");
        servicio.actualizar(e);

        Empleado consultado = servicio.consultarPorId(e.getIdEmpleado());
        assertEquals("Compras y Almacén", consultado.getPuesto());
        assertEquals("3399001122", consultado.getTelefono());
    }

    @Test
    @DisplayName("Borrado lógico y reactivación")
    void borradoLogico() throws Exception {
        Empleado e = servicio.crear(DatosPrueba.nuevoEmpleado("Agente de Ventas"));
        servicio.desactivar(e.getIdEmpleado());
        assertFalse(servicio.consultarPorId(e.getIdEmpleado()).isActivo());
        assertFalse(servicio.listar(false).stream().anyMatch(x -> x.getIdEmpleado() == e.getIdEmpleado()));
        servicio.reactivar(e.getIdEmpleado());
        assertTrue(servicio.consultarPorId(e.getIdEmpleado()).isActivo());
    }

    @Test
    @DisplayName("Borrado físico")
    void borradoFisico() throws Exception {
        Empleado e = servicio.crear(DatosPrueba.nuevoEmpleado("Agente de Ventas"));
        servicio.eliminarFisico(e.getIdEmpleado());
        assertNull(servicio.consultarPorId(e.getIdEmpleado()));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM empleado WHERE id_empleado = ?", e.getIdEmpleado()));
    }
}
