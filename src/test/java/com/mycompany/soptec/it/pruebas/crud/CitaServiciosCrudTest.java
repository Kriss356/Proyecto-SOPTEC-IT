package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.DetalleServicio;
import com.mycompany.soptec.it.modelo.Servicio;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ServicioService;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Cita con servicios (M:N cita_servicio)")
class CitaServiciosCrudTest extends PruebaBD {

    private final CitaService citas = new CitaService();

    @Test
    @DisplayName("Registrar una cita con 2 servicios, consultarlos, cambiarlos y eliminar la cita")
    void cicloCitaConServicios() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        ServicioService ss = new ServicioService();
        Servicio a = ss.crear(DatosPrueba.nuevoServicio("850.00"));
        Servicio b = ss.crear(DatosPrueba.nuevoServicio("1450.00"));
        Servicio c = ss.crear(DatosPrueba.nuevoServicio("650.00"));

        Cita cita = citas.crearConServicios(DatosPrueba.nuevaCita(cli.getIdCliente()),
                List.of(DetalleServicio.de(a, 1), DetalleServicio.de(b, 3)));
        assertTrue(cita.getIdCita() > 0);
        List<DetalleServicio> guardados = citas.listarServicios(cita.getIdCita());
        assertEquals(2, guardados.size());
        assertEquals(3, guardados.stream().filter(d -> d.getIdServicio() == b.getIdServicio())
                .findFirst().orElseThrow().getCantidad());

        // Actualizar: se quita "a", se cambia la cantidad de "b" y se agrega "c"
        cita.setObservaciones("Cambio de servicios");
        citas.actualizarConServicios(cita, List.of(DetalleServicio.de(b, 1), DetalleServicio.de(c, 12)));
        guardados = citas.listarServicios(cita.getIdCita());
        assertEquals(2, guardados.size());
        assertFalse(guardados.stream().anyMatch(d -> d.getIdServicio() == a.getIdServicio()));
        assertEquals("Cambio de servicios", citas.consultarPorId(cita.getIdCita()).getObservaciones());

        // Eliminar la cita elimina también sus renglones de cita_servicio
        citas.eliminarFisico(cita.getIdCita());
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cita_servicio WHERE id_cita = ?", cita.getIdCita()));
    }

    @Test
    @DisplayName("No se aceptan servicios repetidos, cantidades en cero ni servicios inexistentes")
    void validacionesDeServicios() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Servicio a = new ServicioService().crear(DatosPrueba.nuevoServicio("850.00"));

        assertThrows(ValidacionException.class, () -> citas.crearConServicios(DatosPrueba.nuevaCita(cli.getIdCliente()),
                List.of(DetalleServicio.de(a, 1), DetalleServicio.de(a, 2))));
        assertThrows(ValidacionException.class, () -> citas.crearConServicios(DatosPrueba.nuevaCita(cli.getIdCliente()),
                List.of(DetalleServicio.de(a, 0))));
        DetalleServicio fantasma = new DetalleServicio(999999, "Fantasma", 1, a.getPrecio());
        assertThrows(IntegridadReferencialException.class,
                () -> citas.crearConServicios(DatosPrueba.nuevaCita(cli.getIdCliente()), List.of(fantasma)));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cita WHERE id_cliente = ?", cli.getIdCliente()));
    }
}
