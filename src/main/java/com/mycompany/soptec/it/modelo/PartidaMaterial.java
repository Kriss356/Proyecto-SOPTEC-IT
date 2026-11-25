package com.mycompany.soptec.it.modelo;

import java.math.BigDecimal;

/**
 * Renglón de material con cantidad: partidas de una orden de compra (orden_compra_material)
 * o material asignado a un proyecto (proyecto_material).
 */
public class PartidaMaterial {

    private int idMaterial;
    private String descripcionMaterial;
    private int cantidad;
    private BigDecimal precioUnitario = BigDecimal.ZERO;
    private int existencias;

    public PartidaMaterial() {
    }

    public PartidaMaterial(int idMaterial, String descripcionMaterial, int cantidad, BigDecimal precioUnitario) {
        this.idMaterial = idMaterial;
        this.descripcionMaterial = descripcionMaterial;
        this.cantidad = cantidad;
        setPrecioUnitario(precioUnitario);
    }

    public static PartidaMaterial de(Material m, int cantidad) {
        PartidaMaterial p = new PartidaMaterial(m.getIdMaterial(), m.getDescripcionMaterial(), cantidad, m.getPrecioCosto());
        p.setExistencias(m.getExistencias());
        return p;
    }

    public int getIdMaterial() {
        return idMaterial;
    }

    public void setIdMaterial(int idMaterial) {
        this.idMaterial = idMaterial;
    }

    public String getDescripcionMaterial() {
        return descripcionMaterial;
    }

    public void setDescripcionMaterial(String descripcionMaterial) {
        this.descripcionMaterial = descripcionMaterial;
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

    public int getExistencias() {
        return existencias;
    }

    public void setExistencias(int existencias) {
        this.existencias = existencias;
    }

    public BigDecimal getImporte() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    @Override
    public String toString() {
        return cantidad + " × " + descripcionMaterial;
    }
}
