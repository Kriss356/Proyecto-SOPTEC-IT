package com.mycompany.soptec.it.pruebas.funcional;

import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.PedidoCliente;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.servicio.PortalClienteService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Funcional · Portal del cliente (acceso, citas y tienda)")
class PortalClienteTest extends PruebaBD {

    private final PortalClienteService portal = new PortalClienteService();
    private final MaterialService materiales = new MaterialService();

    private Cliente clienteNuevo() throws Exception {
        String tel = DatosPrueba.telefonoUnico();
        return portal.registrar("Cliente Portal " + DatosPrueba.siguiente(), tel, null);
    }

    private static LocalDate diaHabil(int diasAdelante) {
        LocalDate d = LocalDate.now().plusDays(diasAdelante);
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.plusDays(1) : d;
    }

    @Test
    @DisplayName("Registro rápido solo con nombre y teléfono; después entra solo con el teléfono")
    void registroYAcceso() throws Exception {
        Cliente c = clienteNuevo();
        assertTrue(c.getIdCliente() > 0);
        Cliente encontrado = portal.buscarPorTelefono(c.getTelefono());
        assertNotNull(encontrado, "Debe encontrarlo por su número");
        assertEquals(c.getIdCliente(), encontrado.getIdCliente());
        assertThrows(ValidacionException.class, () -> portal.registrar("Otra Persona", c.getTelefono(), null),
                "No se puede registrar dos veces el mismo número");
    }

    @Test
    @DisplayName("Solicitar cita: queda 'Solicitada' y ese horario ya no se puede volver a tomar")
    void solicitarCita() throws Exception {
        Cliente c = clienteNuevo();
        LocalDate dia = diaHabil(5);
        LocalTime hora = LocalTime.of(16, 0);
        int id = portal.solicitarCita(c, PortalClienteService.SERV_REPARACION, dia, hora, null, null, null, "No enciende");
        assertTrue(id > 0);
        List<Cita> citas = portal.citasDeCliente(c.getIdCliente());
        assertEquals("Solicitada", citas.get(0).getEstadoCita());
        assertTrue(portal.horasOcupadas(dia).contains(hora));
        assertThrows(ValidacionException.class, () ->
                portal.solicitarCita(c, PortalClienteService.SERV_MANTENIMIENTO, dia, hora, null, null, null, null));
        assertTrue(portal.cancelarCita(id, c.getIdCliente()));
        assertFalse(portal.horasOcupadas(dia).contains(hora), "Al cancelar se libera el horario");
    }

    @Test
    @DisplayName("Instalación sin domicilio se rechaza")
    void instalacionSinDomicilio() throws Exception {
        Cliente c = clienteNuevo();
        assertThrows(ValidacionException.class, () -> portal.solicitarCita(c, PortalClienteService.SERV_INSTALACION,
                diaHabil(6), LocalTime.of(9, 0), "", "", "", null));
    }

    @Test
    @DisplayName("Tienda: apartar descuenta piezas del almacén y cancelar las regresa")
    void pedidoDescuentaYRegresaStock() throws Exception {
        Cliente c = clienteNuevo();
        Material m = materiales.crear(DatosPrueba.nuevoMaterial()); // 10 piezas a $150.75
        Map<Integer, Integer> carrito = new LinkedHashMap<>();
        carrito.put(m.getIdMaterial(), 3);

        int idPedido = portal.crearPedido(c, carrito);
        assertEquals(7, materiales.consultarPorId(m.getIdMaterial()).getExistencias());
        PedidoCliente p = portal.pedidosDeCliente(c.getIdCliente()).get(0);
        assertEquals(idPedido, p.getIdPedido());
        assertEquals(PedidoCliente.APARTADO, p.getEstado());
        assertEquals(0, new BigDecimal("452.25").compareTo(p.getTotal()));

        assertTrue(portal.cancelarPedidoDeCliente(idPedido, c.getIdCliente()));
        assertEquals(10, materiales.consultarPorId(m.getIdMaterial()).getExistencias());
        assertFalse(portal.cancelarPedidoDeCliente(idPedido, c.getIdCliente()), "No se cancela dos veces");
    }

    @Test
    @DisplayName("Tienda: si no alcanzan las piezas no se guarda nada (rollback)")
    void stockInsuficiente() throws Exception {
        Cliente c = clienteNuevo();
        Material m = materiales.crear(DatosPrueba.nuevoMaterial()); // 10 piezas
        Map<Integer, Integer> carrito = new LinkedHashMap<>();
        carrito.put(m.getIdMaterial(), 50);
        assertThrows(ValidacionException.class, () -> portal.crearPedido(c, carrito));
        assertEquals(10, materiales.consultarPorId(m.getIdMaterial()).getExistencias());
        assertTrue(portal.pedidosDeCliente(c.getIdCliente()).isEmpty());
    }

    @Test
    @DisplayName("Personal: marca un pedido listo y luego entregado")
    void flujoPedidoPersonal() throws Exception {
        Cliente c = clienteNuevo();
        Material m = materiales.crear(DatosPrueba.nuevoMaterial());
        Map<Integer, Integer> carrito = new LinkedHashMap<>();
        carrito.put(m.getIdMaterial(), 1);
        int idPedido = portal.crearPedido(c, carrito);
        assertTrue(portal.listarPedidos(true).stream().anyMatch(p -> p.getIdPedido() == idPedido));
        assertTrue(portal.cambiarEstadoPedido(idPedido, PedidoCliente.LISTO));
        assertTrue(portal.cambiarEstadoPedido(idPedido, PedidoCliente.ENTREGADO));
        assertFalse(portal.listarPedidos(true).stream().anyMatch(p -> p.getIdPedido() == idPedido));
        assertEquals(9, materiales.consultarPorId(m.getIdMaterial()).getExistencias(), "Entregado no regresa piezas");
    }
}
