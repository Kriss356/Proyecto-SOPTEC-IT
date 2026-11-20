package com.mycompany.soptec.it.pruebas.excepciones;

import com.mycompany.soptec.it.dao.Conexion;
import com.mycompany.soptec.it.excepciones.ArchivoException;
import com.mycompany.soptec.it.excepciones.ConexionBDException;
import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.pruebas.soporte.PruebaUnitaria;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.util.AlertaUtil;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Excepciones · Mensajes descriptivos para el usuario")
class MensajesUsuarioTest extends PruebaUnitaria {

    /** Lo que nunca debe ver un usuario final en una alerta. */
    static void sinDetalleTecnico(String mensaje) {
        assertNotNull(mensaje);
        assertFalse(mensaje.isBlank());
        for (String prohibido : new String[]{"SQLException", "jdbc:", "Exception:", "at com.", "SELECT ", "INSERT ",
            "Duplicate entry", "foreign key", "password"}) {
            assertFalse(mensaje.contains(prohibido), "El mensaje expone detalle técnico (" + prohibido + "): " + mensaje);
        }
    }

    @Test
    @DisplayName("Error de validación → título y mensaje claros")
    void validacion() {
        Cliente invalido = new Cliente(0, "RFC_MAL", "", "correo-invalido", null, null, null, null, null, true);
        SoptecException ex = assertThrows(ValidacionException.class, () -> new ClienteService().crear(invalido));

        assertEquals("Datos inválidos o incompletos", AlertaUtil.tituloPara(ex));
        sinDetalleTecnico(ex.getMessage());
        assertTrue(AlertaUtil.mensajeUsuario(ex).contains("registrado en el archivo de log"));
    }

    @Test
    @DisplayName("Pérdida de conexión con MySQL → mensaje que indica qué revisar")
    void conexion() {
        Conexion.setSimularFalloConexion(true);
        ConexionBDException ex = assertThrows(ConexionBDException.class, () -> new EmpleadoService().listar(false));

        assertEquals("Sin conexión con la base de datos", AlertaUtil.tituloPara(ex));
        assertTrue(ex.getMessage().contains("MySQL"), ex.getMessage());
        sinDetalleTecnico(ex.getMessage());
    }

    @Test
    @DisplayName("Integridad referencial y archivos → títulos específicos")
    void otrasCategorias() {
        assertEquals("Operación bloqueada por registros relacionados", AlertaUtil.tituloPara(
                new IntegridadReferencialException("Clientes", "ELIMINAR", "Tiene registros relacionados.")));
        assertEquals("Problema con el archivo de imagen", AlertaUtil.tituloPara(
                new ArchivoException("Clientes", "GESTION_IMAGEN", "No se pudo almacenar la imagen.")));
    }

    @Test
    @DisplayName("Error inesperado (no controlado) → mensaje genérico sin la traza técnica")
    void errorInesperado() {
        RuntimeException tecnico = new IllegalStateException("NullPointer en at com.mycompany.Dao.linea(42) jdbc:mysql");
        assertEquals("Error inesperado", AlertaUtil.tituloPara(tecnico));
        assertEquals(AlertaUtil.MENSAJE_ERROR_INESPERADO, AlertaUtil.mensajeUsuario(tecnico));
        assertFalse(AlertaUtil.mensajeUsuario(tecnico).contains("jdbc"));
        assertTrue(AlertaUtil.mensajeUsuario(tecnico).contains("continúa funcionando"));
    }
}
