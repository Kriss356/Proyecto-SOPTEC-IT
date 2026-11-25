package com.mycompany.soptec.it.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pedido (apartado) que un cliente hace desde el Portal del cliente con material del almacén.
 * Tabla pedido_cliente + detalle en pedido_cliente_material.
 * El cliente paga al recoger; las piezas se apartan (se descuentan del almacén) al crear el pedido
 * y se regresan si el pedido se cancela.
 */
public class PedidoCliente {

    public static final String APARTADO = "Apartado";
    public static final String LISTO = "Listo para recoger";
    public static final String ENTREGADO = "Entregado";
    public static final String CANCELADO = "Cancelado";

    private int idPedido;
    private int idCliente;
    private String nombreCliente;
    private LocalDateTime fechaPedido;
    private String estado;
    private BigDecimal total = BigDecimal.ZERO;
    private String resumenProductos;
    private int totalPiezas;

    public PedidoCliente() {
    }

    public PedidoCliente(int idPedido, int idCliente, String nombreCliente, LocalDateTime fechaPedido,
            String estado, BigDecimal total, String resumenProductos, int totalPiezas) {
        this.idPedido = idPedido;
        this.idCliente = idCliente;
        this.nombreCliente = nombreCliente;
        this.fechaPedido = fechaPedido;
        this.estado = estado;
        this.total = total != null ? total : BigDecimal.ZERO;
        this.resumenProductos = resumenProductos;
        this.totalPiezas = totalPiezas;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public LocalDateTime getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(LocalDateTime fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total != null ? total : BigDecimal.ZERO;
    }

    public String getResumenProductos() {
        return resumenProductos;
    }

    public void setResumenProductos(String resumenProductos) {
        this.resumenProductos = resumenProductos;
    }

    public int getTotalPiezas() {
        return totalPiezas;
    }

    public void setTotalPiezas(int totalPiezas) {
        this.totalPiezas = totalPiezas;
    }

    /** Folio legible para el cliente, por ejemplo P-0007. */
    public String getFolio() {
        return String.format("P-%04d", idPedido);
    }

    /** El pedido todavía se puede cancelar (no se ha entregado ni cancelado). */
    public boolean isCancelable() {
        return APARTADO.equalsIgnoreCase(estado) || LISTO.equalsIgnoreCase(estado);
    }

    @Override
    public String toString() {
        return getFolio() + " · " + estado + " · $" + total;
    }
}
