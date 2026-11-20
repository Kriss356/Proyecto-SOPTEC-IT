package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Levantamiento;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.LevantamientoService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Levantamientos")
class LevantamientoCrudTest extends PruebaBD {

    private final LevantamientoService servicio = new LevantamientoService();
    private Proyecto proyecto;
    private Cita cita;

    @BeforeEach
    void crearRelaciones() throws Exception {
        Cliente cliente = new ClienteService().crear(DatosPrueba.nuevoCliente());
        proyecto = new ProyectoService().crear(DatosPrueba.nuevoProyecto(cliente.getIdCliente()));
        cita = new CitaService().crear(DatosPrueba.nuevaCita(cliente.getIdCliente()));
    }

    @Test
    @DisplayName("Registrar levantamiento con proyecto y cita, y consultarlo")
    void registrarYConsultar() throws Exception {
        Levantamiento l = servicio.crear(DatosPrueba.nuevoLevantamiento(proyecto.getIdProyecto(), cita.getIdCita()));
        Levantamiento consultado = servicio.consultarPorId(l.getIdLevantamiento());

        assertEquals(proyecto.getIdProyecto(), consultado.getIdProyecto());
        assertEquals(cita.getIdCita(), consultado.getIdCita());
        assertEquals(proyecto.getNombreProyecto(), consultado.getNombreProyecto(), "Relación levantamiento → proyecto");
    }

    @Test
    @DisplayName("Registrar levantamiento sin cita (la cita es opcional)")
    void registrarSinCita() throws Exception {
        Levantamiento l = servicio.crear(DatosPrueba.nuevoLevantamiento(proyecto.getIdProyecto(), null));
        Levantamiento consultado = servicio.consultarPorId(l.getIdLevantamiento());
        assertTrue(consultado.getIdCita() == null || consultado.getIdCita() == 0);
    }

    @Test
    @DisplayName("Listar, buscar y actualizar observaciones técnicas")
    void listarBuscarActualizar() throws Exception {
        Levantamiento l = servicio.crear(DatosPrueba.nuevoLevantamiento(proyecto.getIdProyecto(), null));
        assertTrue(servicio.listar(false).stream().anyMatch(x -> x.getIdLevantamiento() == l.getIdLevantamiento()));
        assertTrue(servicio.buscar(proyecto.getNombreProyecto(), false).stream()
                .anyMatch(x -> x.getIdLevantamiento() == l.getIdLevantamiento()));

        l.setObservacionesTecnicas("48 nodos Cat6A y 2 racks");
        servicio.actualizar(l);
        assertEquals("48 nodos Cat6A y 2 racks",
                servicio.consultarPorId(l.getIdLevantamiento()).getObservacionesTecnicas());
    }

    @Test
    @DisplayName("Borrado lógico, reactivación y borrado físico")
    void bajas() throws Exception {
        Levantamiento l = servicio.crear(DatosPrueba.nuevoLevantamiento(proyecto.getIdProyecto(), null));
        servicio.desactivar(l.getIdLevantamiento());
        assertFalse(servicio.consultarPorId(l.getIdLevantamiento()).isActivo());
        servicio.reactivar(l.getIdLevantamiento());
        assertTrue(servicio.consultarPorId(l.getIdLevantamiento()).isActivo());
        servicio.eliminarFisico(l.getIdLevantamiento());
        assertNull(servicio.consultarPorId(l.getIdLevantamiento()));
    }
}
