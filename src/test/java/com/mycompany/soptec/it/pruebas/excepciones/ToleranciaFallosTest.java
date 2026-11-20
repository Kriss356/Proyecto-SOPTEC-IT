package com.mycompany.soptec.it.pruebas.excepciones;

import com.mycompany.soptec.it.dao.ClienteDao;
import com.mycompany.soptec.it.dao.Conexion;
import com.mycompany.soptec.it.excepciones.ConexionBDException;
import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.ClienteTelefono;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ClienteTelefonoService;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Excepciones · Tolerancia a fallos y recuperación")
class ToleranciaFallosTest extends PruebaBD {

    private final ClienteService clientes = new ClienteService();

    @Test
    @DisplayName("Tras perder la conexión con MySQL la aplicación se recupera y sigue operando")
    void recuperacionDeConexion() throws Exception {
        Conexion.setSimularFalloConexion(true);
        assertThrows(ConexionBDException.class, () -> clientes.listar(false));
        assertThrows(ConexionBDException.class, () -> clientes.crear(DatosPrueba.nuevoCliente()));

        Conexion.setSimularFalloConexion(false); // el servidor vuelve
        assertTrue(Conexion.verificarConexion());
        Cliente creado = assertDoesNotThrow(() -> clientes.crear(DatosPrueba.nuevoCliente()));
        assertNotNull(clientes.consultarPorId(creado.getIdCliente()));
    }

    @Test
    @DisplayName("Varios errores recuperables seguidos no impiden la siguiente operación correcta")
    void erroresConsecutivos() throws Exception {
        Cliente base = clientes.crear(DatosPrueba.nuevoCliente());
        new ClienteTelefonoService().crear(new ClienteTelefono(base.getIdCliente(), DatosPrueba.telefonoUnico(), true));

        Cliente invalido = DatosPrueba.nuevoCliente();
        invalido.setCorreo("sin-arroba");
        assertThrows(ValidacionException.class, () -> clientes.crear(invalido));          // 1. validación
        Cliente duplicado = DatosPrueba.nuevoCliente();
        duplicado.setRfc(base.getRfc());
        assertThrows(ValidacionException.class, () -> clientes.crear(duplicado));         // 2. duplicado
        assertThrows(IntegridadReferencialException.class,
                () -> clientes.eliminarFisico(base.getIdCliente()));                        // 3. integridad
        assertThrows(SoptecException.class,
                () -> clientes.crearConTelefonoInicial(DatosPrueba.nuevoCliente(), DatosPrueba.telefonoUnico(), true)); // 4. rollback

        Cliente correcto = clientes.crear(DatosPrueba.nuevoCliente());
        assertTrue(correcto.getIdCliente() > 0, "El sistema sigue funcionando normalmente");
        assertNotNull(clientes.consultarPorId(base.getIdCliente()), "Los datos previos siguen íntegros");
    }

    @Test
    @DisplayName("Los errores que vienen de MySQL se traducen a mensajes comprensibles")
    void erroresDeMysqlTraducidos() throws Exception {
        Cliente original = clientes.crear(DatosPrueba.nuevoCliente());
        Cliente copia = DatosPrueba.nuevoCliente();
        copia.setRfc(original.getRfc());
        SoptecException duplicado = assertThrows(SoptecException.class, () -> new ClienteDao().crear(copia));
        MensajesUsuarioTest.sinDetalleTecnico(duplicado.getMessage());

        new ClienteTelefonoService().crear(new ClienteTelefono(original.getIdCliente(), DatosPrueba.telefonoUnico(), true));
        SoptecException fk = assertThrows(SoptecException.class,
                () -> new ClienteDao().eliminarFisico(original.getIdCliente()));
        MensajesUsuarioTest.sinDetalleTecnico(fk.getMessage());
    }

    @Test
    @DisplayName("Un dato que no cabe en la columna se reporta como validación, no como caída")
    void datoDemasiadoLargo() throws Exception {
        Cliente c = DatosPrueba.nuevoCliente();
        c.setNumero("1234567890123456789012345"); // la columna numero admite 20 caracteres
        // El DAO directo deja que MySQL lo rechace (error 1406) y se traduce a ValidacionException.
        SoptecException ex = assertThrows(SoptecException.class, () -> new ClienteDao().crear(c));
        assertEquals("VALIDACION", ex.getCategoria());
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cliente WHERE rfc = ?", c.getRfc()));
    }
}
