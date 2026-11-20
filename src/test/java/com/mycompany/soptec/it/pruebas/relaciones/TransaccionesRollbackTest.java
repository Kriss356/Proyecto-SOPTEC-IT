package com.mycompany.soptec.it.pruebas.relaciones;

import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.LogPruebas;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Relaciones · Transacciones con rollback")
class TransaccionesRollbackTest extends PruebaBD {

    @Test
    @DisplayName("Cliente + teléfono: si falla el segundo INSERT no queda ni el cliente ni el teléfono")
    void rollbackClienteConTelefono() throws Exception {
        Cliente c = DatosPrueba.nuevoCliente();
        String telefono = DatosPrueba.telefonoUnico();
        int marca = LogPruebas.marca();

        SoptecException ex = assertThrows(SoptecException.class,
                () -> new ClienteService().crearConTelefonoInicial(c, telefono, true));

        assertTrue(ex.getMessage().contains("revirtieron"), "El usuario es informado del rollback: " + ex.getMessage());
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cliente WHERE rfc = ?", c.getRfc()),
                "El INSERT de cliente debe revertirse");
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cliente_telefono WHERE telefono = ?", telefono));
        assertTrue(LogPruebas.textoDesde(marca).contains("Operacion=CREAR_CLIENTE_CON_TELEFONO"));
    }

    @Test
    @DisplayName("Proyecto + levantamiento: fallo simulado revierte el proyecto")
    void rollbackProyectoSimulado() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Proyecto p = DatosPrueba.nuevoProyecto(cli.getIdCliente());

        assertThrows(SoptecException.class, () -> new ProyectoService()
                .crearConLevantamientoInicial(p, DatosPrueba.nuevoLevantamiento(0, null), true));

        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM proyecto WHERE nombre_proyecto = ?",
                p.getNombreProyecto()));
    }

    @Test
    @DisplayName("Proyecto + levantamiento: un error REAL de MySQL (cita inexistente) también revierte")
    void rollbackPorErrorReal() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Proyecto p = DatosPrueba.nuevoProyecto(cli.getIdCliente());

        // El levantamiento apunta a una cita que no existe: el segundo INSERT viola la FK (error 1452).
        assertThrows(IntegridadReferencialException.class, () -> new ProyectoService()
                .crearConLevantamientoInicial(p, DatosPrueba.nuevoLevantamiento(0, 999999), false));

        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM proyecto WHERE nombre_proyecto = ?",
                p.getNombreProyecto()), "El proyecto insertado antes del error debe revertirse");
    }

    @Test
    @DisplayName("Después de un rollback la misma operación se puede completar con éxito")
    void reintentoDespuesDeRollback() throws Exception {
        Cliente c = DatosPrueba.nuevoCliente();
        String telefono = DatosPrueba.telefonoUnico();
        ClienteService servicio = new ClienteService();

        assertThrows(SoptecException.class, () -> servicio.crearConTelefonoInicial(c, telefono, true));
        Cliente creado = servicio.crearConTelefonoInicial(c, telefono, false);

        assertTrue(creado.getIdCliente() > 0);
        assertEquals(1, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cliente WHERE rfc = ?", c.getRfc()));
        assertEquals(1, BaseDatosPrueba.contar(
                "SELECT COUNT(*) FROM cliente_telefono WHERE id_cliente = ? AND telefono = ?",
                creado.getIdCliente(), telefono));
    }
}
