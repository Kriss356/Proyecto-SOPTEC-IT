package com.mycompany.soptec.it.modelo;

import java.math.BigDecimal;

/**
 * Renglón de servicio dentro de una cita (cita_servicio) o de una venta (venta_servicio).
 */
public class DetalleServicio {

    private int idServicio;
    private String nombreServicio;
    private int cantidad = 1;
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    public DetalleServicio() {
    }

    public DetalleServicio(int idServicio, String nombreServicio, int cantidad, BigDecimal precioUnitario) {
        this.idServicio = idServicio;
        this.nombreServicio = nombreServicio;
        this.cantidad = cantidad;
        setPrecioUnitario(precioUnitario);
    }

    /** Crea el renglón con el precio vigente del catálogo. */
    public static DetalleServicio de(Servicio s, int cantidad) {
        return new DetalleServicio(s.getIdServicio(), s.getNombreServicio(), cantidad, s.getPrecio());
    }

    public int getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(int idServicio) {
        this.idServicio = idServicio;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
    }

    public BigDecimal getImporte() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    @Override
    public String toString() {
        return cantidad + " × " + nombreServicio;
    }
}
