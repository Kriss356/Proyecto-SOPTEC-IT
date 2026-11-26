package com.mycompany.soptec.it.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Avance de obra capturado en Instalaciones (tabla avance_proyecto, Proyecto 1:N Avance).
 */
public class AvanceProyecto {

    /** Separador de las rutas de las fotos de evidencia guardadas en una sola columna. */
    public static final String SEPARADOR_EVIDENCIAS = "|";

    private int idAvance;
    private int idProyecto;
    private LocalDate fecha;
    private int horas;
    private String tarea;
    private Integer idEmpleado;
    private String empleadoNombre;
    private int porcentaje;
    private String estado;
    private String materialUsado;
    private String observaciones;
    private String evidencias;

    public AvanceProyecto() {
    }

    /** Copia los datos de otro avance (se usa para "guardar como avance nuevo"). */
    public AvanceProyecto copiar() {
        AvanceProyecto c = new AvanceProyecto();
        c.idAvance = idAvance;
        c.idProyecto = idProyecto;
        c.fecha = fecha;
        c.horas = horas;
        c.tarea = tarea;
        c.idEmpleado = idEmpleado;
        c.empleadoNombre = empleadoNombre;
        c.porcentaje = porcentaje;
        c.estado = estado;
        c.materialUsado = materialUsado;
        c.observaciones = observaciones;
        c.evidencias = evidencias;
        return c;
    }

    public List<String> getListaEvidencias() {
        if (evidencias == null || evidencias.isBlank()) {
            return new ArrayList<>();
        }
        List<String> lista = new ArrayList<>();
        for (String r : Arrays.asList(evidencias.split("\\" + SEPARADOR_EVIDENCIAS))) {
            if (!r.isBlank()) {
                lista.add(r.trim());
            }
        }
        return lista;
    }

    public void setListaEvidencias(List<String> rutas) {
        evidencias = rutas == null || rutas.isEmpty() ? null : String.join(SEPARADOR_EVIDENCIAS, rutas);
    }

    public int getIdAvance() {
        return idAvance;
    }

    public void setIdAvance(int idAvance) {
        this.idAvance = idAvance;
    }

    public int getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(int idProyecto) {
        this.idProyecto = idProyecto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public int getHoras() {
        return horas;
    }

    public void setHoras(int horas) {
        this.horas = horas;
    }

    public String getTarea() {
        return tarea;
    }

    public void setTarea(String tarea) {
        this.tarea = tarea;
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

    public int getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMaterialUsado() {
        return materialUsado;
    }

    public void setMaterialUsado(String materialUsado) {
        this.materialUsado = materialUsado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getEvidencias() {
        return evidencias;
    }

    public void setEvidencias(String evidencias) {
        this.evidencias = evidencias;
    }
}
