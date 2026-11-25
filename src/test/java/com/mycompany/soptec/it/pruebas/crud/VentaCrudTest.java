package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.DetalleServicio;
import com.mycompany.soptec.it.modelo.Pago;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.modelo.Servicio;
import com.mycompany.soptec.it.modelo.Venta;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.CitaService;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.servicio.ServicioService;
import com.mycompany.soptec.it.servicio.VentaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Ventas con servicios (M:N venta_servicio) y pagos")
class VentaCrudTest extends PruebaBD {

    private final VentaService ventas = new VentaService();

    private Venta nuevaVenta(int idCliente, DetalleServicio... detalles) {
        Venta v = new Venta();
        v.setIdCliente(idCliente);
        v.setFechaVenta(LocalDate.now());
        v.setFechaVencimiento(LocalDate.now().plusDays(30));
        v.setMetodoPago("Transferencia");
        v.setDetalles(List.of(detalles));
        return v;
    }

    @Test
    @DisplayName("Registrar venta con 2 servicios y pago inicial: calcula IVA, total, folio y estado Parcial")
    void registrarVentaConPago() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        ServicioService ss = new ServicioService();
        Servicio a = ss.crear(DatosPrueba.nuevoServicio("1000.00"));
        Servicio b = ss.crear(DatosPrueba.nuevoServicio("500.00"));

        Venta v = ventas.registrar(nuevaVenta(cli.getIdCliente(), DetalleServicio.de(a, 2), DetalleServicio.de(b, 1)),
                new Pago(new BigDecimal("1000.00"), LocalDate.now(), "Efectivo", null));

        Venta leida = ventas.consultarPorId(v.getIdVenta());
        assertEquals(String.format("F-%04d", v.getIdVenta()), leida.getFolio());
        assertEquals(0, new BigDecimal("2500.00").compareTo(leida.getSubtotal()));
        assertEquals(0, new BigDecimal("400.00").compareTo(leida.getIva()));
        assertEquals(0, new BigDecimal("2900.00").compareTo(leida.getTotal()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(leida.getPagado()));
        assertEquals(Venta.PARCIAL, leida.getEstado());
        assertEquals(2, leida.getDetalles().size());
        assertEquals(1, ventas.listarPagos(v.getIdVenta()).size());
    }

    @Test
    @DisplayName("Abonos: el estado pasa a Pagada y no se acepta pagar más del saldo")
    void abonosHastaLiquidar() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Servicio a = new ServicioService().crear(DatosPrueba.nuevoServicio("1000.00"));
        Venta v = ventas.registrar(nuevaVenta(cli.getIdCliente(), DetalleServicio.de(a, 1)), null);
        assertEquals(Venta.PENDIENTE, ventas.consultarPorId(v.getIdVenta()).getEstado());

        assertThrows(ValidacionException.class, () -> ventas.registrarAbono(v.getIdVenta(),
                new Pago(new BigDecimal("5000.00"), LocalDate.now(), "Efectivo", null)));

        ventas.registrarAbono(v.getIdVenta(), new Pago(new BigDecimal("160.00"), LocalDate.now(), "Efectivo", null));
        assertEquals(Venta.PARCIAL, ventas.consultarPorId(v.getIdVenta()).getEstado());
        Pago ultimo = ventas.registrarAbono(v.getIdVenta(),
                new Pago(new BigDecimal("1000.00"), LocalDate.now(), "Tarjeta", null));
        assertEquals("Liquidado", ultimo.getEstadoCuenta());
        Venta pagada = ventas.consultarPorId(v.getIdVenta());
        assertEquals(Venta.PAGADA, pagada.getEstado());
        assertEquals(0, BigDecimal.ZERO.compareTo(pagada.getSaldo()));

        // Eliminar un pago regresa la venta a Parcial
        ventas.eliminarPago(ultimo.getIdPago());
        assertEquals(Venta.PARCIAL, ventas.consultarPorId(v.getIdVenta()).getEstado());
    }

    @Test
    @DisplayName("Actualizar la venta reemplaza sus servicios; no puede quedar por debajo de lo pagado")
    void actualizarVenta() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        ServicioService ss = new ServicioService();
        Servicio a = ss.crear(DatosPrueba.nuevoServicio("1000.00"));
        Servicio b = ss.crear(DatosPrueba.nuevoServicio("200.00"));
        Venta v = ventas.registrar(nuevaVenta(cli.getIdCliente(), DetalleServicio.de(a, 1)),
                new Pago(new BigDecimal("500.00"), LocalDate.now(), "Efectivo", null));

        Venta editada = ventas.consultarPorId(v.getIdVenta());
        editada.setDetalles(List.of(DetalleServicio.de(a, 2), DetalleServicio.de(b, 1)));
        ventas.actualizar(editada);
        Venta leida = ventas.consultarPorId(v.getIdVenta());
        assertEquals(2, leida.getDetalles().size());
        assertEquals(0, new BigDecimal("2552.00").compareTo(leida.getTotal()));

        editada.setDetalles(List.of(DetalleServicio.de(b, 1)));   // total 232 < 500 pagado
        assertThrows(ValidacionException.class, () -> ventas.actualizar(editada));
        assertEquals(2, ventas.consultarPorId(v.getIdVenta()).getDetalles().size(), "Se conservan los servicios");
    }

    @Test
    @DisplayName("Venta desde una cita: valida que cita y proyecto sean del mismo cliente")
    void ventaDesdeCita() throws Exception {
        ClienteService cs = new ClienteService();
        Cliente cli = cs.crear(DatosPrueba.nuevoCliente());
        Cliente otro = cs.crear(DatosPrueba.nuevoCliente());
        Servicio a = new ServicioService().crear(DatosPrueba.nuevoServicio("850.00"));
        Cita cita = new CitaService().crearConServicios(DatosPrueba.nuevaCita(cli.getIdCliente()),
                List.of(DetalleServicio.de(a, 1)));
        Proyecto proyOtro = new ProyectoService().crear(DatosPrueba.nuevoProyecto(otro.getIdCliente()));

        Venta v = nuevaVenta(cli.getIdCliente(), new CitaService().listarServicios(cita.getIdCita()).toArray(new DetalleServicio[0]));
        v.setIdCita(cita.getIdCita());
        v.setIdProyecto(proyOtro.getIdProyecto());
        assertThrows(ValidacionException.class, () -> ventas.registrar(v, null));

        v.setIdProyecto(null);
        Venta creada = ventas.registrar(v, null);
        assertEquals(cita.getIdCita(), ventas.consultarPorId(creada.getIdVenta()).getIdCita());
        // La cita ya facturada no se puede borrar
        assertThrows(IntegridadReferencialException.class, () -> new CitaService().eliminarFisico(cita.getIdCita()));
    }

    @Test
    @DisplayName("Cancelar y eliminar: bloqueados si hay pagos; sin pagos sí se puede")
    void cancelarYEliminar() throws Exception {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        Servicio a = new ServicioService().crear(DatosPrueba.nuevoServicio("300.00"));
        Venta conPago = ventas.registrar(nuevaVenta(cli.getIdCliente(), DetalleServicio.de(a, 1)),
                new Pago(new BigDecimal("100.00"), LocalDate.now(), "Efectivo", null));
        assertThrows(IntegridadReferencialException.class, () -> ventas.cancelar(conPago.getIdVenta()));
        assertThrows(IntegridadReferencialException.class, () -> ventas.eliminarFisico(conPago.getIdVenta()));

        Venta sinPago = ventas.registrar(nuevaVenta(cli.getIdCliente(), DetalleServicio.de(a, 1)), null);
        ventas.cancelar(sinPago.getIdVenta());
        assertEquals(Venta.CANCELADA, ventas.consultarPorId(sinPago.getIdVenta()).getEstado());
        assertFalse(ventas.listar(false).stream().anyMatch(x -> x.getIdVenta() == sinPago.getIdVenta()));
        assertThrows(ValidacionException.class, () -> ventas.registrarAbono(sinPago.getIdVenta(),
                new Pago(new BigDecimal("10.00"), LocalDate.now(), "Efectivo", null)));

        ventas.eliminarFisico(sinPago.getIdVenta());
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM venta WHERE id_venta = ?", sinPago.getIdVenta()));
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM venta_servicio WHERE id_venta = ?", sinPago.getIdVenta()));
    }
}
