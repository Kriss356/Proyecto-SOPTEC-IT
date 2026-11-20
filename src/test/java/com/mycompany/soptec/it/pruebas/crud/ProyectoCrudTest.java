package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Proyectos")
class ProyectoCrudTest extends PruebaBD {

    private final ProyectoService servicio = new ProyectoService();
    private Cliente cliente;

    @BeforeEach
    void crearCliente() throws Exception {
        cliente = new ClienteService().crear(DatosPrueba.nuevoCliente());
    }

    @Test
    @DisplayName("Registrar proyecto ligado a un cliente y consultarlo")
    void registrarYConsultar() throws Exception {
        Proyecto p = servicio.crear(DatosPrueba.nuevoProyecto(cliente.getIdCliente()));
        Proyecto consultado = servicio.consultarPorId(p.getIdProyecto());

        assertEquals(p.getNombreProyecto(), consultado.getNombreProyecto());
        assertEquals(cliente.getIdCliente(), consultado.getIdCliente());
        assertEquals(cliente.getNombreCliente(), consultado.getClienteNombre(), "Relación proyecto → cliente");
        assertEquals(cliente.getRfc(), consultado.getClienteRfc());
    }

    @Test
    @DisplayName("Listar y buscar proyectos")
    void listarYBuscar() throws Exception {
        Proyecto p = servicio.crear(DatosPrueba.nuevoProyecto(cliente.getIdCliente()));
        assertTrue(servicio.listar(false).stream().anyMatch(x -> x.getIdProyecto() == p.getIdProyecto()));
        assertTrue(servicio.buscar(p.getNombreProyecto(), false).stream()
                .anyMatch(x -> x.getIdProyecto() == p.getIdProyecto()));
    }

    @Test
    @DisplayName("Actualizar estado y costo total")
    void actualizar() throws Exception {
        Proyecto p = servicio.crear(DatosPrueba.nuevoProyecto(cliente.getIdCliente()));
        p.setEstado("Terminado");
        p.setCostoTotal(new BigDecimal("31500.50"));
        servicio.actualizar(p);

        Proyecto consultado = servicio.consultarPorId(p.getIdProyecto());
        assertEquals("Terminado", consultado.getEstado());
        assertEquals(0, new BigDecimal("31500.50").compareTo(consultado.getCostoTotal()));
    }

    @Test
    @DisplayName("Borrado lógico, reactivación y borrado físico")
    void bajas() throws Exception {
        Proyecto p = servicio.crear(DatosPrueba.nuevoProyecto(cliente.getIdCliente()));
        servicio.desactivar(p.getIdProyecto());
        assertFalse(servicio.consultarPorId(p.getIdProyecto()).isActivo());
        servicio.reactivar(p.getIdProyecto());
        assertTrue(servicio.consultarPorId(p.getIdProyecto()).isActivo());
        servicio.eliminarFisico(p.getIdProyecto());
        assertNull(servicio.consultarPorId(p.getIdProyecto()));
    }

    @Test
    @DisplayName("Proceso: registrar proyecto con levantamiento inicial (transacción confirmada)")
    void proyectoConLevantamientoInicial() throws Exception {
        Proyecto p = servicio.crearConLevantamientoInicial(DatosPrueba.nuevoProyecto(cliente.getIdCliente()),
                DatosPrueba.nuevoLevantamiento(0, null), false);
        assertNotNull(servicio.consultarPorId(p.getIdProyecto()));
        assertEquals(1, com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba.contar(
                "SELECT COUNT(*) FROM levantamiento WHERE id_proyecto = ?", p.getIdProyecto()));
    }
}
