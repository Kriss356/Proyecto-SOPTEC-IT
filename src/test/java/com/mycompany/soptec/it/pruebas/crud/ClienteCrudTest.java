package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.ClienteTelefono;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ClienteTelefonoService;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Clientes")
class ClienteCrudTest extends PruebaBD {

    private final ClienteService servicio = new ClienteService();

    @Test
    @DisplayName("Registrar cliente y consultarlo por ID")
    void registrarYConsultarPorId() throws Exception {
        Cliente nuevo = DatosPrueba.nuevoCliente();
        Cliente creado = servicio.crear(nuevo);

        assertTrue(creado.getIdCliente() > 0, "Debe asignarse un ID autoincremental");
        Cliente consultado = servicio.consultarPorId(creado.getIdCliente());
        assertNotNull(consultado);
        assertEquals(nuevo.getRfc(), consultado.getRfc());
        assertEquals(nuevo.getNombreCliente(), consultado.getNombreCliente());
        assertEquals(nuevo.getCorreo(), consultado.getCorreo());
        assertEquals("Jalisco", consultado.getEstado());
        assertTrue(consultado.isActivo());
    }

    @Test
    @DisplayName("Consultar un ID que no existe devuelve null")
    void consultarInexistente() throws Exception {
        assertNull(servicio.consultarPorId(999999));
    }

    @Test
    @DisplayName("Listar clientes activos e inactivos")
    void listar() throws Exception {
        Cliente activo = servicio.crear(DatosPrueba.nuevoCliente());
        Cliente inactivo = servicio.crear(DatosPrueba.nuevoCliente());
        servicio.desactivar(inactivo.getIdCliente());

        List<Cliente> soloActivos = servicio.listar(false);
        List<Cliente> todos = servicio.listar(true);

        assertTrue(contiene(soloActivos, activo.getIdCliente()));
        assertFalse(contiene(soloActivos, inactivo.getIdCliente()), "El listado normal no muestra inactivos");
        assertTrue(contiene(todos, inactivo.getIdCliente()), "Con 'mostrar inactivos' sí aparece");
    }

    @Test
    @DisplayName("Buscar cliente por RFC y por nombre")
    void buscar() throws Exception {
        Cliente creado = servicio.crear(DatosPrueba.nuevoCliente());
        assertEquals(1, servicio.buscar(creado.getRfc(), false).size());
        assertTrue(contiene(servicio.buscar(creado.getNombreCliente(), false), creado.getIdCliente()));
    }

    @Test
    @DisplayName("Editar y actualizar los datos de un cliente")
    void actualizar() throws Exception {
        Cliente c = servicio.crear(DatosPrueba.nuevoCliente());
        c.setNombreCliente("Cliente Actualizado");
        c.setColonia("Providencia");
        c.setCorreo(DatosPrueba.correoUnico("actualizado"));
        servicio.actualizar(c);

        Cliente consultado = servicio.consultarPorId(c.getIdCliente());
        assertEquals("Cliente Actualizado", consultado.getNombreCliente());
        assertEquals("Providencia", consultado.getColonia());
        assertEquals(c.getCorreo(), consultado.getCorreo());
    }

    @Test
    @DisplayName("Borrado lógico (desactivar) y reactivación")
    void borradoLogicoYReactivacion() throws Exception {
        Cliente c = servicio.crear(DatosPrueba.nuevoCliente());
        servicio.desactivar(c.getIdCliente());
        assertFalse(servicio.consultarPorId(c.getIdCliente()).isActivo());
        assertEquals(1, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cliente WHERE id_cliente = ?", c.getIdCliente()),
                "El borrado lógico conserva el registro en la tabla");

        servicio.reactivar(c.getIdCliente());
        assertTrue(servicio.consultarPorId(c.getIdCliente()).isActivo());
    }

    @Test
    @DisplayName("Borrado físico elimina el registro de la tabla")
    void borradoFisico() throws Exception {
        Cliente c = servicio.crear(DatosPrueba.nuevoCliente());
        servicio.eliminarFisico(c.getIdCliente());
        assertNull(servicio.consultarPorId(c.getIdCliente()));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cliente WHERE id_cliente = ?", c.getIdCliente()));
    }

    @Test
    @DisplayName("Proceso: registrar cliente con teléfono inicial en una sola transacción")
    void registrarConTelefonoInicial() throws Exception {
        String telefono = DatosPrueba.telefonoUnico();
        Cliente c = servicio.crearConTelefonoInicial(DatosPrueba.nuevoCliente(), telefono, false);

        ClienteTelefono tel = new ClienteTelefonoService().consultarPorClave(c.getIdCliente(), telefono);
        assertNotNull(tel, "El teléfono inicial se guarda junto con el cliente");
        assertEquals(c.getNombreCliente(), tel.getNombreCliente());
    }

    private static boolean contiene(List<Cliente> lista, int id) {
        return lista.stream().anyMatch(x -> x.getIdCliente() == id);
    }
}
