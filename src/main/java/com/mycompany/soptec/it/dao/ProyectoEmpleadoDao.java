package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.util.AppLogger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Personal asignado a cada proyecto (tabla proyecto_empleado, relación muchos a muchos).
 */
public class ProyectoEmpleadoDao {

    private static final String MODULO = "PersonalProyecto";
    private static final String PROCESO = "ProyectoEmpleadoDao";

    public List<Empleado> listarEmpleados(int idProyecto) throws SoptecException {
        String sql = "SELECT e.id_empleado, e.nombre, e.telefono, e.puesto, e.correo, e.imagen, e.activo "
                + "FROM proyecto_empleado pe INNER JOIN empleado e ON e.id_empleado = pe.id_empleado "
                + "WHERE pe.id_proyecto = ? ORDER BY e.nombre";
        List<Empleado> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "LISTAR_PERSONAL");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProyecto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Empleado(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), rs.getBoolean(7)));
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "LISTAR_PERSONAL",
                    "Error al consultar el personal del proyecto.", ex);
        }
    }

    /** Cuántos proyectos tiene asignados el empleado. */
    public int contarProyectosDeEmpleado(int idEmpleado) throws SoptecException {
        try (Connection con = Conexion.conectarStrict(MODULO, "CONTAR_PROYECTOS");
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM proyecto_empleado WHERE id_empleado = ?")) {
            ps.setInt(1, idEmpleado);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CONTAR_PROYECTOS",
                    "Error al consultar los proyectos del empleado.", ex);
        }
    }

    /**
     * Guarda de una sola vez el equipo del proyecto: quita a quien ya no está y agrega a los nuevos.
     * Todo en una transacción; si un empleado no existe se revierte y el equipo queda como estaba.
     */
    public void reemplazarEquipo(int idProyecto, List<Integer> idsEmpleados, boolean simularFallo)
            throws SoptecException {
        String op = "GUARDAR_PERSONAL_PROYECTO";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            try (PreparedStatement del = con.prepareStatement("DELETE FROM proyecto_empleado WHERE id_proyecto = ?")) {
                del.setInt(1, idProyecto);
                del.executeUpdate();
            }
            if (simularFallo) {
                throw new SQLException("Fallo simulado al guardar el personal del proyecto.", "45000", 9999);
            }
            try (PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO proyecto_empleado (id_proyecto, id_empleado) VALUES (?, ?)")) {
                for (Integer id : idsEmpleados) {
                    ins.setInt(1, idProyecto);
                    ins.setInt(2, id);
                    ins.executeUpdate();
                }
            }
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Proyecto ID=" + idProyecto + " con "
                    + idsEmpleados.size() + " empleado(s) asignado(s)");
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo guardar el personal del proyecto. Se revirtieron los cambios.", ex);
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }
}
