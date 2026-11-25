package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.DetalleServicio;
import com.mycompany.soptec.it.modelo.Servicio;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ServicioService;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Catálogo de servicios")
class ServicioCrudTest extends PruebaBD {

    private final ServicioService servicio = new ServicioService();

    @Test
    @DisplayName("Registrar, consultar, buscar, actualizar, desactivar, reactivar y eliminar un servicio")
    void cicloCompleto() throws Exception {
        Servicio s = servicio.crear(DatosPrueba.nuevoServicio("1450.00"));
        assertTrue(s.getIdServicio() > 0);

        Servicio leido = servicio.consultarPorId(s.getIdServicio());
        assertEquals(0, new BigDecimal("1450.00").compareTo(leido.getPrecio()));
        assertTrue(servicio.buscar(s.getNombreServicio(), false).stream()
                .anyMatch(x -> x.getIdServicio() == s.getIdServicio()));

        leido.setPrecio(new BigDecimal("1500.00"));
        servicio.actualizar(leido);
        assertEquals(0, new BigDecimal("1500.00").compareTo(servicio.consultarPorId(s.getIdServicio()).getPrecio()));

        servicio.desactivar(s.getIdServicio());
        assertFalse(servicio.listar(false).stream().anyMatch(x -> x.getIdServicio() == s.getIdServicio()));
        servicio.reactivar(s.getIdServicio());
        assertTrue(servicio.consultarPorId(s.getIdServicio()).isActivo());

        servicio.eliminarFisico(s.getIdServicio());
        assertNull(servicio.consultarPorId(s.getIdServicio()));
    }

    @Test
    @DisplayName("No se permiten servicios duplicados ni con precio cero")
    void validaciones() throws Exception {
        Servicio s = servicio.crear(DatosPrueba.nuevoServicio("900.00"));
        Servicio repetido = new Servicio(0, s.getNombreServicio().toUpperCase(), null, new BigDecimal("100"), null, true);
        assertThrows(ValidacionException.class, () -> servicio.crear(repetido));
        assertThrows(ValidacionException.class, () -> servicio.crear(DatosPrueba.nuevoServicio("0")));
    }

    @Test
    @DisplayName("No se elimina un servicio que ya está en una cita")
    void noEliminarServicioEnUso() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Servicio s = servicio.crear(DatosPrueba.nuevoServicio("650.00"));
        new CitaService().crearConServicios(DatosPrueba.nuevaCita(cli.getIdCliente()),
                List.of(DetalleServicio.de(s, 2)));

        assertThrows(IntegridadReferencialException.class, () -> servicio.eliminarFisico(s.getIdServicio()));
        assertEquals(1, BaseDatosPrueba.contar("SELECT COUNT(*) FROM servicio WHERE id_servicio = ?", s.getIdServicio()));
    }
}
