package com.mycompany.soptec.it.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago o abono (tabla pago). Pertenece a una venta y, si la venta tiene proyecto, también al proyecto.
 */
public class Pago {

    private int idPago;
    private Integer idProyecto;
    private Integer idVenta;
    private String folioVenta;
    private String concepto;
    private BigDecimal monto = BigDecimal.ZERO;
    private LocalDate fechaPago;
    private String metodoPago;
    private String estadoCuenta;

    public Pago() {
    }

    public Pago(BigDecimal monto, LocalDate fechaPago, String metodoPago, String concepto) {
        setMonto(monto);
        this.fechaPago = fechaPago;
        this.metodoPago = metodoPago;
        this.concepto = concepto;
    }

    public int getIdPago() {
        return idPago;
    }

    public void setIdPago(int idPago) {
        this.idPago = idPago;
    }

    public Integer getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(Integer idProyecto) {
        this.idProyecto = idProyecto;
    }

    public Integer getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(Integer idVenta) {
        this.idVenta = idVenta;
    }

    public String getFolioVenta() {
        return folioVenta;
    }

    public void setFolioVenta(String folioVenta) {
        this.folioVenta = folioVenta;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto != null ? monto : BigDecimal.ZERO;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(String estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }
}
