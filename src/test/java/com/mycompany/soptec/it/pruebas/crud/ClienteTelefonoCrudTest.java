package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.ClienteTelefono;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ClienteTelefonoService;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Teléfonos de cliente")
class ClienteTelefonoCrudTest extends PruebaBD {

    private final ClienteTelefonoService servicio = new ClienteTelefonoService();
    private Cliente cliente;

    @BeforeEach
    void crearCliente() throws Exception {
        cliente = new ClienteService().crear(DatosPrueba.nuevoCliente());
    }

    @Test
    @DisplayName("Registrar teléfono y consultarlo por clave (cliente + número)")
    void registrarYConsultar() throws Exception {
        String numero = DatosPrueba.telefonoUnico();
        servicio.crear(new ClienteTelefono(cliente.getIdCliente(), numero, true));

        ClienteTelefono consultado = servicio.consultarPorClave(cliente.getIdCliente(), numero);
        assertNotNull(consultado);
        assertEquals(numero, consultado.getTelefono());
        assertEquals(cliente.getNombreCliente(), consultado.getNombreCliente(), "Trae el nombre del cliente por JOIN");
    }

    @Test
    @DisplayName("Listar teléfonos de un cliente y buscar por número")
    void listarYBuscar() throws Exception {
        String t1 = DatosPrueba.telefonoUnico();
        String t2 = DatosPrueba.telefonoUnico();
        servicio.crear(new ClienteTelefono(cliente.getIdCliente(), t1, true));
        servicio.crear(new ClienteTelefono(cliente.getIdCliente(), t2, true));

        assertEquals(2, servicio.listarPorCliente(cliente.getIdCliente(), false).size());
        assertTrue(servicio.buscar(t2, false).stream().anyMatch(t -> t.getTelefono().equals(t2)));
        assertTrue(servicio.listar(false).size() >= 2);
    }

    @Test
    @DisplayName("Actualizar el número de teléfono")
    void actualizar() throws Exception {
        String original = DatosPrueba.telefonoUnico();
        String nuevo = DatosPrueba.telefonoUnico();
        servicio.crear(new ClienteTelefono(cliente.getIdCliente(), original, true));

        servicio.actualizar(cliente.getIdCliente(), original, new ClienteTelefono(cliente.getIdCliente(), nuevo, true));

        assertNull(servicio.consultarPorClave(cliente.getIdCliente(), original));
        assertNotNull(servicio.consultarPorClave(cliente.getIdCliente(), nuevo));
    }

    @Test
    @DisplayName("Borrado lógico, reactivación y borrado físico del teléfono")
    void bajas() throws Exception {
        String numero = DatosPrueba.telefonoUnico();
        servicio.crear(new ClienteTelefono(cliente.getIdCliente(), numero, true));

        servicio.desactivar(cliente.getIdCliente(), numero);
        assertFalse(servicio.consultarPorClave(cliente.getIdCliente(), numero).isActivo());
        assertTrue(servicio.listarPorCliente(cliente.getIdCliente(), false).isEmpty());

        servicio.reactivar(cliente.getIdCliente(), numero);
        assertTrue(servicio.consultarPorClave(cliente.getIdCliente(), numero).isActivo());

        servicio.eliminarFisico(cliente.getIdCliente(), numero);
        assertNull(servicio.consultarPorClave(cliente.getIdCliente(), numero));
    }
}
