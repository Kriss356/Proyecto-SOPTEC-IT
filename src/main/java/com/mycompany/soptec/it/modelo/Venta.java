package com.mycompany.soptec.it.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Venta / factura (tabla venta) con sus servicios (venta_servicio) y pagos (pago).
 */
public class Venta {

    public static final String PENDIENTE = "Pendiente";
    public static final String PARCIAL = "Parcial";
    public static final String PAGADA = "Pagada";
    public static final String CANCELADA = "Cancelada";
    public static final BigDecimal TASA_IVA = new BigDecimal("0.16");

    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    private int idVenta;
    private String folio;
    private int idCliente;
    private String clienteNombre;
    private String clienteRfc;
    private Integer idProyecto;
    private String proyectoNombre;
    private Integer idCita;
    private LocalDate fechaVenta;
    private LocalDate fechaVencimiento;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal iva = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal pagado = BigDecimal.ZERO;
    private String metodoPago;
    private String estado = PENDIENTE;
    private String observaciones;
    private boolean activo = true;
    private List<DetalleServicio> detalles = new ArrayList<>();

    /** Recalcula subtotal, IVA y total a partir de los servicios. */
    public void calcularTotales() {
        BigDecimal suma = BigDecimal.ZERO;
        for (DetalleServicio d : detalles) {
            suma = suma.add(d.getImporte());
        }
        subtotal = suma.setScale(2, RoundingMode.HALF_UP);
        iva = subtotal.multiply(TASA_IVA).setScale(2, RoundingMode.HALF_UP);
        total = subtotal.add(iva);
    }

    /** Estado de cobro según lo pagado (no cambia una venta cancelada). */
    public static String estadoPorPagos(BigDecimal total, BigDecimal pagado) {
        if (pagado == null || pagado.signum() <= 0) {
            return PENDIENTE;
        }
        return pagado.compareTo(total) >= 0 ? PAGADA : PARCIAL;
    }

    public BigDecimal getSaldo() {
        BigDecimal s = total.subtract(pagado);
        return s.signum() < 0 ? BigDecimal.ZERO : s;
    }

    public boolean isCancelada() {
        return CANCELADA.equalsIgnoreCase(estado);
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteRfc() {
        return clienteRfc;
    }

    public void setClienteRfc(String clienteRfc) {
        this.clienteRfc = clienteRfc;
    }

    public Integer getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(Integer idProyecto) {
        this.idProyecto = idProyecto != null && idProyecto > 0 ? idProyecto : null;
    }

    public String getProyectoNombre() {
        return proyectoNombre;
    }

    public void setProyectoNombre(String proyectoNombre) {
        this.proyectoNombre = proyectoNombre;
    }

    public Integer getIdCita() {
        return idCita;
    }

    public void setIdCita(Integer idCita) {
        this.idCita = idCita != null && idCita > 0 ? idCita : null;
    }

    public LocalDate getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDate fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public BigDecimal getIva() {
        return iva;
    }

    public void setIva(BigDecimal iva) {
        this.iva = iva != null ? iva : BigDecimal.ZERO;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getPagado() {
        return pagado;
    }

    public void setPagado(BigDecimal pagado) {
        this.pagado = pagado != null ? pagado : BigDecimal.ZERO;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public List<DetalleServicio> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleServicio> detalles) {
        this.detalles = detalles != null ? new ArrayList<>(detalles) : new ArrayList<>();
    }

    /** Resumen de los servicios para mostrar en la factura. */
    public String getConceptoResumen() {
        if (detalles.isEmpty()) {
            return observaciones != null && !observaciones.isBlank() ? observaciones : "Servicios";
        }
        StringBuilder sb = new StringBuilder();
        for (DetalleServicio d : detalles) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(d.getCantidad()).append(" × ").append(d.getNombreServicio());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return (folio != null ? folio : "Venta") + " · " + (clienteNombre != null ? clienteNombre : "")
                + " · Saldo " + MONEDA.format(getSaldo());
    }
}
