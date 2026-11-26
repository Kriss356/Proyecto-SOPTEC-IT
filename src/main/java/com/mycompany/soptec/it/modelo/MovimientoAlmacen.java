package com.mycompany.soptec.it.modelo;

import java.time.LocalDate;

/**
 * Entrada o salida de material del almacén (tabla movimiento_almacen).
 * Una entrada suma existencias; una salida las resta y, si va a un proyecto,
 * se suma al material asignado a ese proyecto (proyecto_material).
 */
public class MovimientoAlmacen {

    public static final String ENTRADA = "Entrada";
    public static final String SALIDA = "Salida";

    private int idMovimiento;
    private LocalDate fecha;
    private String tipo;
    private int idMaterial;
    private String materialDescripcion;
    private int cantidad;
    private Integer idProyecto;
    private String proyectoNombre;
    private Integer idEmpleado;
    private String empleadoNombre;
    private String referencia;
    private String observaciones;

    public MovimientoAlmacen() {
    }

    public boolean isEntrada() {
        return ENTRADA.equalsIgnoreCase(tipo);
    }

    /** Texto para la columna "OC / Proyecto". */
    public String getDestinoTexto() {
        if (proyectoNombre != null && !proyectoNombre.isBlank()) {
            return (idProyecto != null ? "PRY-" + String.format("%04d", idProyecto) + " · " : "") + proyectoNombre;
        }
        if (referencia != null && !referencia.isBlank()) {
            return referencia;
        }
        return isEntrada() ? "Recepción almacén" : "Uso interno";
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getIdMaterial() {
        return idMaterial;
    }

    public void setIdMaterial(int idMaterial) {
        this.idMaterial = idMaterial;
    }

    public String getMaterialDescripcion() {
        return materialDescripcion;
    }

    public void setMaterialDescripcion(String materialDescripcion) {
        this.materialDescripcion = materialDescripcion;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Integer getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(Integer idProyecto) {
        this.idProyecto = idProyecto;
    }

    public String getProyectoNombre() {
        return proyectoNombre;
    }

    public void setProyectoNombre(String proyectoNombre) {
        this.proyectoNombre = proyectoNombre;
    }

    public Integer getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(Integer idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public String getEmpleadoNombre() {
        return empleadoNombre;
    }

    public void setEmpleadoNombre(String empleadoNombre) {
        this.empleadoNombre = empleadoNombre;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
