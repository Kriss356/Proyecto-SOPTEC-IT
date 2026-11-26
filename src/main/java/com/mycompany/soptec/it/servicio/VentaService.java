package com.mycompany.soptec.it.servicio;

import com.mycompany.soptec.it.dao.CitaDao;
import com.mycompany.soptec.it.dao.ClienteDao;
import com.mycompany.soptec.it.dao.ProyectoDao;
import com.mycompany.soptec.it.dao.ServicioDao;
import com.mycompany.soptec.it.dao.VentaDao;
import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.DetalleServicio;
import com.mycompany.soptec.it.modelo.Pago;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.modelo.Servicio;
import com.mycompany.soptec.it.modelo.Venta;
import com.mycompany.soptec.it.util.AppLogger;
import com.mycompany.soptec.it.util.Validador;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas de negocio de ventas / facturación: venta + servicios + pagos.
 */
public class VentaService {

    private static final String MODULO = "Ventas";
    private static final String PROCESO = "VentaService";

    private final VentaDao dao = new VentaDao();
    private final ClienteDao clienteDao = new ClienteDao();
    private final ProyectoDao proyectoDao = new ProyectoDao();
    private final CitaDao citaDao = new CitaDao();
    private final ServicioDao servicioDao = new ServicioDao();

    /** Registra venta, servicios y (opcional) pago inicial en una sola transacción. */
    public Venta registrar(Venta v, Pago pagoInicial) throws SoptecException {
        return registrar(v, pagoInicial, VentaDao.FalloSimulado.NINGUNO);
    }

    public Venta registrar(Venta v, Pago pagoInicial, VentaDao.FalloSimulado fallo) throws SoptecException {
        String op = "REGISTRAR_VENTA";
        try {
            validar(v, op);
            if (pagoInicial != null && pagoInicial.getMonto().signum() > 0) {
                validarPago(pagoInicial, op);
                if (pagoInicial.getMonto().compareTo(v.getTotal()) > 0) {
                    throw new ValidacionException(MODULO, op, "El pago inicial no puede ser mayor al total de la venta.");
                }
            }
            return dao.registrar(v, pagoInicial, fallo);
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    public void actualizar(Venta v) throws SoptecException {
        actualizar(v, false);
    }

    public void actualizar(Venta v, boolean simularFallo) throws SoptecException {
        String op = "ACTUALIZAR_VENTA";
        try {
            if (v == null || v.getIdVenta() <= 0) {
                throw new ValidacionException(MODULO, op, "Selecciona primero la venta que deseas actualizar.");
            }
            Venta actual = dao.obtenerPorId(v.getIdVenta());
            if (actual == null) {
                throw new ValidacionException(MODULO, op, "La venta ya no existe.");
            }
            if (actual.isCancelada()) {
                throw new ValidacionException(MODULO, op, "La venta " + actual.getFolio() + " está cancelada y no se puede modificar.");
            }
            validar(v, op);
            if (!dao.actualizar(v, simularFallo)) {
                throw new ValidacionException(MODULO, op, "La venta ya no existe.");
            }
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    public Pago registrarAbono(int idVenta, Pago pago) throws SoptecException {
        String op = "REGISTRAR_ABONO";
        try {
            if (idVenta <= 0) {
                throw new ValidacionException(MODULO, op, "Selecciona la venta a la que se aplicará el pago.");
            }
            validarPago(pago, op);
            return dao.registrarAbono(idVenta, pago);
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    public void actualizarPago(Pago pago) throws SoptecException {
        String op = "ACTUALIZAR_PAGO";
        try {
            if (pago == null || pago.getIdPago() <= 0) {
                throw new ValidacionException(MODULO, op, "Selecciona el pago que quieres corregir.");
            }
            validarPago(pago, op);
            dao.actualizarPago(pago);
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    public void eliminarPago(int idPago) throws SoptecException {
        if (idPago <= 0) {
            throw registrar(new ValidacionException(MODULO, "ELIMINAR_PAGO", "Selecciona el pago que deseas eliminar."));
        }
        if (!dao.eliminarPago(idPago)) {
            throw registrar(new ValidacionException(MODULO, "ELIMINAR_PAGO", "El pago ya no existe."));
        }
    }

    public Venta consultarPorId(int idVenta) throws SoptecException {
        if (idVenta <= 0) {
            throw registrar(new ValidacionException(MODULO, "CONSULTAR_VENTA", "El ID de la venta debe ser mayor a cero."));
        }
        return dao.obtenerPorId(idVenta);
    }

    public List<Venta> listar(boolean incluirCanceladas) throws SoptecException {
        return dao.buscar(null, incluirCanceladas);
    }

    public List<Venta> buscar(String criterio, boolean incluirCanceladas) throws SoptecException {
        return dao.buscar(criterio, incluirCanceladas);
    }

    public List<Pago> listarPagos(int idVenta) throws SoptecException {
        return dao.listarPagos(idVenta);
    }

    /** Baja lógica: la venta queda como "Cancelada". Solo si no tiene pagos. */
    public void cancelar(int idVenta) throws SoptecException {
        String op = "CANCELAR_VENTA";
        try {
            Venta v = consultarPorId(idVenta);
            if (v == null) {
                throw new ValidacionException(MODULO, op, "La venta ya no existe.");
            }
            if (v.getPagado().signum() > 0) {
                throw new IntegridadReferencialException(MODULO, op, "La venta " + v.getFolio()
                        + " ya tiene pagos registrados. Elimina primero los pagos para poder cancelarla.");
            }
            dao.cancelar(idVenta);
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Venta " + v.getFolio() + " cancelada");
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    public void eliminarFisico(int idVenta) throws SoptecException {
        String op = "ELIMINAR_FISICO_VENTA";
        try {
            Venta v = consultarPorId(idVenta);
            if (v == null) {
                throw new ValidacionException(MODULO, op, "La venta ya no existe.");
            }
            if (v.getPagado().signum() > 0) {
                throw new IntegridadReferencialException(MODULO, op, "No se puede eliminar la venta " + v.getFolio()
                        + " porque tiene pagos registrados.");
            }
            dao.eliminarFisico(idVenta);
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Venta " + v.getFolio() + " eliminada con sus servicios");
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    // ------------------------------------------------------------------ validaciones
    private void validar(Venta v, String op) throws SoptecException {
        if (v == null) {
            throw new ValidacionException(MODULO, op, "Los datos de la venta son obligatorios.");
        }
        if (v.getIdCliente() <= 0) {
            throw new ValidacionException(MODULO, op, "Selecciona el cliente de la venta.");
        }
        Cliente cli = clienteDao.obtenerPorId(v.getIdCliente());
        if (cli == null) {
            throw new IntegridadReferencialException(MODULO, op, "El cliente seleccionado no existe.");
        }
        if (!cli.isActivo()) {
            throw new ValidacionException(MODULO, op, "El cliente '" + cli.getNombreCliente() + "' está inactivo.");
        }
        if (v.getIdProyecto() != null) {
            Proyecto p = proyectoDao.obtenerPorId(v.getIdProyecto());
            if (p == null) {
                throw new IntegridadReferencialException(MODULO, op, "El proyecto seleccionado no existe.");
            }
            if (p.getIdCliente() != v.getIdCliente()) {
                throw new ValidacionException(MODULO, op, "El proyecto '" + p.getNombreProyecto()
                        + "' pertenece a otro cliente.");
            }
        }
        if (v.getIdCita() != null) {
            Cita c = citaDao.obtenerPorId(v.getIdCita());
            if (c == null) {
                throw new IntegridadReferencialException(MODULO, op, "La cita seleccionada no existe.");
            }
            if (c.getIdCliente() != v.getIdCliente()) {
                throw new ValidacionException(MODULO, op, "La cita seleccionada pertenece a otro cliente.");
            }
        }
        if (v.getFechaVenta() == null) {
            throw new ValidacionException(MODULO, op, "La fecha de la venta es obligatoria.");
        }
        if (v.getFechaVencimiento() != null && v.getFechaVencimiento().isBefore(v.getFechaVenta())) {
            throw new ValidacionException(MODULO, op, "La fecha de vencimiento no puede ser anterior a la fecha de la venta.");
        }
        validarDetalles(v.getDetalles(), op);
        v.setObservaciones(Validador.opcional(v.getObservaciones()));
        v.setMetodoPago(Validador.opcional(v.getMetodoPago()));
        v.calcularTotales();
    }

    /** Valida la lista de servicios (compartido con citas). */
    void validarDetalles(List<DetalleServicio> detalles, String op) throws SoptecException {
        validarDetalles(detalles, op, MODULO, true, servicioDao);
    }

    static void validarDetalles(List<DetalleServicio> detalles, String op, String modulo, boolean obligatorio,
            ServicioDao servicioDao) throws SoptecException {
        if (detalles == null || detalles.isEmpty()) {
            if (obligatorio) {
                throw new ValidacionException(modulo, op, "Agrega al menos un servicio.");
            }
            return;
        }
        Set<Integer> vistos = new HashSet<>();
        for (DetalleServicio d : detalles) {
            if (d.getIdServicio() <= 0) {
                throw new ValidacionException(modulo, op, "Uno de los servicios no es válido.");
            }
            if (!vistos.add(d.getIdServicio())) {
                throw new ValidacionException(modulo, op, "El servicio '" + d.getNombreServicio()
                        + "' está repetido; cambia la cantidad en lugar de agregarlo dos veces.");
            }
            if (d.getCantidad() <= 0) {
                throw new ValidacionException(modulo, op, "La cantidad de '" + d.getNombreServicio() + "' debe ser mayor a cero.");
            }
            if (d.getPrecioUnitario().signum() < 0) {
                throw new ValidacionException(modulo, op, "El precio de '" + d.getNombreServicio() + "' no puede ser negativo.");
            }
            Servicio s = servicioDao.obtenerPorId(d.getIdServicio());
            if (s == null) {
                throw new IntegridadReferencialException(modulo, op, "El servicio '" + d.getNombreServicio()
                        + "' ya no existe en el catálogo.");
            }
        }
    }

    private void validarPago(Pago p, String op) throws ValidacionException {
        if (p == null || p.getMonto() == null || p.getMonto().signum() <= 0) {
            throw new ValidacionException(MODULO, op, "El monto del pago debe ser mayor a cero.");
        }
        if (p.getFechaPago() == null) {
            throw new ValidacionException(MODULO, op, "La fecha del pago es obligatoria.");
        }
        p.setMonto(p.getMonto().setScale(2, java.math.RoundingMode.HALF_UP));
        p.setMetodoPago(Validador.opcional(p.getMetodoPago()));
        p.setConcepto(Validador.opcional(p.getConcepto()));
        Validador.longitudMaxima(p.getConcepto(), 150, "Concepto del pago", MODULO, op);
    }

    private SoptecException registrar(SoptecException ex) {
        AppLogger.registrarExcepcion(MODULO, PROCESO, ex.getOperacion(), ex);
        return ex;
    }

    /** Total de lo pendiente por cobrar de una lista de ventas. */
    public static BigDecimal saldoTotal(List<Venta> ventas) {
        BigDecimal total = BigDecimal.ZERO;
        for (Venta v : ventas) {
            if (!v.isCancelada()) {
                total = total.add(v.getSaldo());
            }
        }
        return total;
    }
}
