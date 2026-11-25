package com.mycompany.soptec.it.pruebas.excepciones;

import com.mycompany.soptec.it.dao.Conexion;
import com.mycompany.soptec.it.excepciones.ConexionBDException;
import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.DetalleServicio;
import com.mycompany.soptec.it.modelo.Pago;
import com.mycompany.soptec.it.modelo.Servicio;
import com.mycompany.soptec.it.modelo.Venta;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.LogPruebas;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ServicioService;
import com.mycompany.soptec.it.servicio.VentaService;
import com.mycompany.soptec.it.util.AlertaUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Excepciones · Ventas, citas y servicios: tipos de error, mensajes y log")
class ErroresVentasCitasLogTest extends PruebaBD {

    @Test
    @DisplayName("Cada tipo de error queda diferenciado en el log con fecha, módulo, proceso, operación y tipo")
    void tiposDeErrorDiferenciados() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Servicio s = new ServicioService().crear(DatosPrueba.nuevoServicio("500.00"));
        new CitaService().crearConServicios(DatosPrueba.nuevaCita(cli.getIdCliente()), List.of(DetalleServicio.de(s, 1)));
        int marca = LogPruebas.marca();

        // 1. Validación: venta sin servicios
        Venta vacia = new Venta();
        vacia.setIdCliente(cli.getIdCliente());
        vacia.setFechaVenta(LocalDate.now());
        ValidacionException val = assertThrows(ValidacionException.class, () -> new VentaService().registrar(vacia, null));
        assertEquals("Datos inválidos o incompletos", AlertaUtil.tituloPara(val));

        // 2. Integridad referencial: servicio usado en una cita
        IntegridadReferencialException ir = assertThrows(IntegridadReferencialException.class,
                () -> new ServicioService().eliminarFisico(s.getIdServicio()));
        assertEquals("Operación bloqueada por registros relacionados", AlertaUtil.tituloPara(ir));

        // 3. Conexión: MySQL "apagado" al registrar una venta
        Venta v = new Venta();
        v.setIdCliente(cli.getIdCliente());
        v.setFechaVenta(LocalDate.now());
        v.setDetalles(List.of(DetalleServicio.de(s, 1)));
        Conexion.setSimularFalloConexion(true);
        ConexionBDException con = assertThrows(ConexionBDException.class, () -> new VentaService().registrar(v, null));
        Conexion.setSimularFalloConexion(false);
        assertEquals("Sin conexión con la base de datos", AlertaUtil.tituloPara(con));

        // 4. Operación de BD con rollback (fallo a la mitad de la transacción)
        SoptecException tx = assertThrows(SoptecException.class, () -> new CitaService()
                .crearConServicios(DatosPrueba.nuevaCita(cli.getIdCliente()), List.of(DetalleServicio.de(s, 1)), true));
        assertEquals("OPERACION_BD", tx.getCategoria());

        String log = LogPruebas.textoDesde(marca);
        assertTrue(log.matches("(?s).*\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*"), "Fecha y hora");
        assertTrue(log.contains("Modulo=Ventas | Proceso=VentaService | Operacion=REGISTRAR_VENTA | TipoExcepcion=VALIDACION"));
        assertTrue(log.contains("Modulo=Servicios | Proceso=ServicioService | Operacion=ELIMINAR_FISICO_SERVICIO | TipoExcepcion=INTEGRIDAD_REFERENCIAL"));
        assertTrue(log.contains("TipoExcepcion=CONEXION_BD"));
        assertTrue(log.contains("Operacion=CREAR_CITA_CON_SERVICIOS | TipoExcepcion=TRANSACCION_REVERTIDA"));
        assertTrue(log.contains("Operacion=CREAR_CITA_CON_SERVICIOS | TipoExcepcion=OPERACION_BD"));
        assertTrue(log.contains("password=***"), "La contraseña se enmascara");
        assertFalse(log.matches("(?s).*password=(?!\\*\\*\\*)[^\\s,;)]+.*"), "Nunca aparece una contraseña en claro");
    }

    @Test
    @DisplayName("El usuario recibe mensajes claros, sin SQL ni trazas, y la siguiente operación funciona")
    void mensajesClarosYRecuperacion() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Servicio s = new ServicioService().crear(DatosPrueba.nuevoServicio("1000.00"));
        Venta v = new Venta();
        v.setIdCliente(cli.getIdCliente());
        v.setFechaVenta(LocalDate.now());
        v.setDetalles(List.of(DetalleServicio.de(s, 1)));

        SoptecException ex = assertThrows(SoptecException.class, () -> new VentaService().registrar(v,
                new Pago(new BigDecimal("99999"), LocalDate.now(), "Efectivo", null)));
        MensajesUsuarioTest.sinDetalleTecnico(AlertaUtil.mensajeUsuario(ex));
        assertTrue(ex.getMessage().contains("pago inicial"), ex.getMessage());

        SoptecException rollback = assertThrows(SoptecException.class, () -> new VentaService().registrar(v,
                null, com.mycompany.soptec.it.dao.VentaDao.FalloSimulado.DESPUES_DE_SERVICIOS));
        MensajesUsuarioTest.sinDetalleTecnico(AlertaUtil.mensajeUsuario(rollback));

        Venta ok = new VentaService().registrar(v, new Pago(new BigDecimal("1160.00"), LocalDate.now(), "Efectivo", null));
        assertEquals(Venta.PAGADA, new VentaService().consultarPorId(ok.getIdVenta()).getEstado());
    }
}
