package com.mycompany.soptec.it.modelo;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Servicio del catálogo de SOPTEC-IT (tabla servicio). Se relaciona M:N con cita y con venta.
 */
public class Servicio {

    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    private int idServicio;
    private String nombreServicio;
    private String descripcion;
    private BigDecimal precio = BigDecimal.ZERO;
    private String imagen;
    private boolean activo = true;

    public Servicio() {
    }

    public Servicio(int idServicio, String nombreServicio, String descripcion, BigDecimal precio,
            String imagen, boolean activo) {
        this.idServicio = idServicio;
        this.nombreServicio = nombreServicio;
        this.descripcion = descripcion;
        setPrecio(precio);
        this.imagen = imagen;
        this.activo = activo;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio != null ? precio : BigDecimal.ZERO;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getEstadoTexto() {
        return activo ? "Activo" : "Inactivo";
    }

    @Override
    public String toString() {
        return nombreServicio + " · " + MONEDA.format(precio) + (activo ? "" : " [Inactivo]");
    }
}
