package com.mycompany.soptec.it.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de compra a un proveedor (tabla orden_compra) con sus partidas (orden_compra_material).
 */
public class OrdenCompra {

    public static final String SOLICITADA = "Solicitada";
    public static final String RECIBIDA = "Recibida";
    public static final String CANCELADA = "Cancelada";

    private int idOrdenCompra;
    private Integer idProyecto;
    private String proyectoNombre;
    private Integer idProveedor;
    private String proveedorNombre;
    private LocalDate fechaOrden;
    private BigDecimal totalCompra = BigDecimal.ZERO;
    private String tiempoEntrega;
    private String estado = SOLICITADA;
    private List<PartidaMaterial> partidas = new ArrayList<>();

    public void calcularTotal() {
        BigDecimal suma = BigDecimal.ZERO;
        for (PartidaMaterial p : partidas) {
            suma = suma.add(p.getImporte());
        }
        totalCompra = suma.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public String getFolio() {
        return String.format("OC-%04d", idOrdenCompra);
    }

    public int getIdOrdenCompra() {
        return idOrdenCompra;
    }

    public void setIdOrdenCompra(int idOrdenCompra) {
        this.idOrdenCompra = idOrdenCompra;
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

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Integer idProveedor) {
        this.idProveedor = idProveedor != null && idProveedor > 0 ? idProveedor : null;
    }

    public String getProveedorNombre() {
        return proveedorNombre;
    }

    public void setProveedorNombre(String proveedorNombre) {
        this.proveedorNombre = proveedorNombre;
    }

    public LocalDate getFechaOrden() {
        return fechaOrden;
    }

    public void setFechaOrden(LocalDate fechaOrden) {
        this.fechaOrden = fechaOrden;
    }

    public BigDecimal getTotalCompra() {
        return totalCompra;
    }

    public void setTotalCompra(BigDecimal totalCompra) {
        this.totalCompra = totalCompra != null ? totalCompra : BigDecimal.ZERO;
    }

    public String getTiempoEntrega() {
        return tiempoEntrega;
    }

    public void setTiempoEntrega(String tiempoEntrega) {
        this.tiempoEntrega = tiempoEntrega;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<PartidaMaterial> getPartidas() {
        return partidas;
    }

    public void setPartidas(List<PartidaMaterial> partidas) {
        this.partidas = partidas != null ? new ArrayList<>(partidas) : new ArrayList<>();
    }

    @Override
    public String toString() {
        return getFolio() + " · " + (proveedorNombre != null ? proveedorNombre : "Sin proveedor") + " · " + estado;
    }
}
