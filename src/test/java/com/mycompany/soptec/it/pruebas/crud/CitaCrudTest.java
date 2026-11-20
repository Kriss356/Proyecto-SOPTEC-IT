package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Citas")
class CitaCrudTest extends PruebaBD {

    private final CitaService servicio = new CitaService();
    private Cliente cliente;

    @BeforeEach
    void crearCliente() throws Exception {
        cliente = new ClienteService().crear(DatosPrueba.nuevoCliente());
    }

    @Test
    @DisplayName("Registrar cita y consultarla por ID")
    void registrarYConsultar() throws Exception {
        Cita c = servicio.crear(DatosPrueba.nuevaCita(cliente.getIdCliente()));
        Cita consultada = servicio.consultarPorId(c.getIdCita());
        assertEquals(LocalTime.of(10, 30), consultada.getHoraCita());
        assertEquals("Programada", consultada.getEstadoCita());
        assertEquals(cliente.getIdCliente(), consultada.getIdCliente());
    }

    @Test
    @DisplayName("Listar y buscar citas")
    void listarYBuscar() throws Exception {
        Cita c = servicio.crear(DatosPrueba.nuevaCita(cliente.getIdCliente()));
        assertTrue(servicio.listar(false).stream().anyMatch(x -> x.getIdCita() == c.getIdCita()));
        assertTrue(servicio.buscar(cliente.getNombreCliente(), false).stream()
                .anyMatch(x -> x.getIdCita() == c.getIdCita()));
    }

    @Test
    @DisplayName("Actualizar estado y hora de la cita")
    void actualizar() throws Exception {
        Cita c = servicio.crear(DatosPrueba.nuevaCita(cliente.getIdCliente()));
        c.setEstadoCita("Confirmada");
        c.setHoraCita(LocalTime.of(16, 0));
        servicio.actualizar(c);

        Cita consultada = servicio.consultarPorId(c.getIdCita());
        assertEquals("Confirmada", consultada.getEstadoCita());
        assertEquals(LocalTime.of(16, 0), consultada.getHoraCita());
    }

    @Test
    @DisplayName("Borrado lógico, reactivación y borrado físico")
    void bajas() throws Exception {
        Cita c = servicio.crear(DatosPrueba.nuevaCita(cliente.getIdCliente()));
        servicio.desactivar(c.getIdCita());
        assertFalse(servicio.consultarPorId(c.getIdCita()).isActivo());
        servicio.reactivar(c.getIdCita());
        assertTrue(servicio.consultarPorId(c.getIdCita()).isActivo());
        servicio.eliminarFisico(c.getIdCita());
        assertNull(servicio.consultarPorId(c.getIdCita()));
    }
}
