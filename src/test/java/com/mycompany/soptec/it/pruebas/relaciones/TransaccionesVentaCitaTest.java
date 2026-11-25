package com.mycompany.soptec.it.pruebas.relaciones;

import com.mycompany.soptec.it.dao.VentaDao;
import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.DetalleServicio;
import com.mycompany.soptec.it.modelo.Pago;
import com.mycompany.soptec.it.modelo.Servicio;
import com.mycompany.soptec.it.modelo.Venta;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.LogPruebas;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ServicioService;
import com.mycompany.soptec.it.servicio.VentaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Relaciones · Rollback de cita + servicios y de venta + servicios + pago")
class TransaccionesVentaCitaTest extends PruebaBD {

    @Test
    @DisplayName("Si falla el registro de una cita, sus servicios NO quedan registrados")
    void rollbackCitaConServicios() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Servicio s = new ServicioService().crear(DatosPrueba.nuevoServicio("850.00"));
        int marca = LogPruebas.marca();

        SoptecException ex = assertThrows(SoptecException.class, () -> new CitaService().crearConServicios(
                DatosPrueba.nuevaCita(cli.getIdCliente()), List.of(DetalleServicio.de(s, 1)), true));

        assertTrue(ex.getMessage().contains("revirtieron"), ex.getMessage());
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cita WHERE id_cliente = ?", cli.getIdCliente()));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cita_servicio WHERE id_servicio = ?", s.getIdServicio()));
        String log = LogPruebas.textoDesde(marca);
        assertTrue(log.contains("TipoExcepcion=TRANSACCION_REVERTIDA"), "El rollback queda en el log");
        assertTrue(log.contains("Operacion=CREAR_CITA_CON_SERVICIOS"));
    }

    @Test
    @DisplayName("Error REAL de MySQL en el 2.º servicio (FK inexistente): la cita y el 1.er servicio se revierten")
    void rollbackCitaPorErrorReal() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Servicio s = new ServicioService().crear(DatosPrueba.nuevoServicio("850.00"));
        Cita cita = DatosPrueba.nuevaCita(cli.getIdCliente());
        // Se va directo al DAO para saltar la validación previa y forzar el error 1452 de MySQL
        DetalleServicio inexistente = new DetalleServicio(987654, "No existe", 1, BigDecimal.TEN);

        assertThrows(IntegridadReferencialException.class, () -> new com.mycompany.soptec.it.dao.CitaDao()
                .crearConServicios(cita, List.of(DetalleServicio.de(s, 1), inexistente), false));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cita WHERE id_cliente = ?", cli.getIdCliente()));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM cita_servicio WHERE id_servicio = ?", s.getIdServicio()));
    }

    @Test
    @DisplayName("Si falla una venta después de guardar sus servicios y su pago, no queda NADA registrado")
    void rollbackVentaServiciosPago() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Servicio s = new ServicioService().crear(DatosPrueba.nuevoServicio("1000.00"));
        Venta v = new Venta();
        v.setIdCliente(cli.getIdCliente());
        v.setFechaVenta(LocalDate.now());
        v.setDetalles(List.of(DetalleServicio.de(s, 2)));
        int marca = LogPruebas.marca();

        for (VentaDao.FalloSimulado fallo : new VentaDao.FalloSimulado[]{
            VentaDao.FalloSimulado.DESPUES_DE_SERVICIOS, VentaDao.FalloSimulado.DESPUES_DEL_PAGO}) {
            SoptecException ex = assertThrows(SoptecException.class, () -> new VentaService().registrar(v,
                    new Pago(new BigDecimal("500.00"), LocalDate.now(), "Efectivo", null), fallo));
            assertTrue(ex.getMessage().contains("no se guardó la venta"), ex.getMessage());
        }
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM venta WHERE id_cliente = ?", cli.getIdCliente()));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM venta_servicio WHERE id_servicio = ?", s.getIdServicio()));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM pago p INNER JOIN venta v ON v.id_venta = p.id_venta "
                + "WHERE v.id_cliente = ?", cli.getIdCliente()));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM pago WHERE id_venta IS NOT NULL AND id_venta NOT IN "
                + "(SELECT id_venta FROM venta)"), "No hay pagos huérfanos");
        String log = LogPruebas.textoDesde(marca);
        assertTrue(log.contains("Operacion=REGISTRAR_VENTA"));
        assertTrue(log.contains("TRANSACCION_REVERTIDA"));

        // Después del fallo, la misma venta se registra bien (el sistema se recupera)
        Venta ok = new VentaService().registrar(v, new Pago(new BigDecimal("500.00"), LocalDate.now(), "Efectivo", null));
        assertEquals(1, BaseDatosPrueba.contar("SELECT COUNT(*) FROM venta WHERE id_venta = ?", ok.getIdVenta()));
        assertEquals(1, BaseDatosPrueba.contar("SELECT COUNT(*) FROM venta_servicio WHERE id_venta = ?", ok.getIdVenta()));
        assertEquals(1, BaseDatosPrueba.contar("SELECT COUNT(*) FROM pago WHERE id_venta = ?", ok.getIdVenta()));
    }

    @Test
    @DisplayName("Error REAL de MySQL en un servicio de la venta: no queda la venta ni el pago")
    void rollbackVentaPorErrorReal() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Servicio s = new ServicioService().crear(DatosPrueba.nuevoServicio("1000.00"));
        Venta v = new Venta();
        v.setIdCliente(cli.getIdCliente());
        v.setFechaVenta(LocalDate.now());
        v.setDetalles(List.of(DetalleServicio.de(s, 1), new DetalleServicio(987654, "No existe", 1, BigDecimal.ONE)));
        v.calcularTotales();

        assertThrows(IntegridadReferencialException.class, () -> new VentaDao().registrar(v,
                new Pago(new BigDecimal("100.00"), LocalDate.now(), "Efectivo", null), VentaDao.FalloSimulado.NINGUNO));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM venta WHERE id_cliente = ?", cli.getIdCliente()));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM venta_servicio WHERE id_servicio = ?", s.getIdServicio()));
    }

    @Test
    @DisplayName("Actualizar una cita con fallo a la mitad conserva los servicios anteriores")
    void rollbackActualizarCita() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        ServicioService ss = new ServicioService();
        Servicio a = ss.crear(DatosPrueba.nuevoServicio("100.00"));
        Servicio b = ss.crear(DatosPrueba.nuevoServicio("200.00"));
        CitaService citas = new CitaService();
        Cita cita = citas.crearConServicios(DatosPrueba.nuevaCita(cli.getIdCliente()), List.of(DetalleServicio.de(a, 1)));

        assertThrows(SoptecException.class,
                () -> citas.actualizarConServicios(cita, List.of(DetalleServicio.de(b, 5)), true));
        List<DetalleServicio> actuales = citas.listarServicios(cita.getIdCita());
        assertEquals(1, actuales.size());
        assertEquals(a.getIdServicio(), actuales.get(0).getIdServicio());
    }
}
