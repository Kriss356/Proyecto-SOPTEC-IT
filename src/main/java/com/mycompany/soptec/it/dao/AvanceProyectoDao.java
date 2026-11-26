package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.modelo.AvanceProyecto;
import com.mycompany.soptec.it.util.AppLogger;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a la tabla avance_proyecto: historial de avances de obra de cada proyecto.
 */
public class AvanceProyectoDao {

    private static final String MODULO = "Instalaciones";
    private static final String PROCESO = "AvanceProyectoDao";

    private static final String SELECT_BASE = "SELECT a.id_avance, a.id_proyecto, a.fecha, a.horas, a.tarea, "
            + "a.id_empleado, e.nombre, a.porcentaje, a.estado, a.material_usado, a.observaciones, a.evidencias "
            + "FROM avance_proyecto a LEFT JOIN empleado e ON e.id_empleado = a.id_empleado ";

    /** Avances del proyecto, del más reciente al más antiguo. */
    public List<AvanceProyecto> listarPorProyecto(int idProyecto) throws SoptecException {
        MigracionBD.asegurarEsquema();
        List<AvanceProyecto> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "LISTAR_AVANCES");
             PreparedStatement ps = con.prepareStatement(SELECT_BASE
                     + "WHERE a.id_proyecto = ? ORDER BY a.fecha DESC, a.id_avance DESC")) {
            ps.setInt(1, idProyecto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "LISTAR_AVANCES",
                    "Error al consultar el historial de avances.", ex);
        }
    }

    /** Porcentaje del avance más reciente de cada proyecto (id_proyecto → porcentaje). */
    public Map<Integer, Integer> ultimoPorcentajePorProyecto() throws SoptecException {
        MigracionBD.asegurarEsquema();
        String sql = "SELECT a.id_proyecto, a.porcentaje FROM avance_proyecto a "
                + "WHERE a.id_avance = (SELECT a2.id_avance FROM avance_proyecto a2 WHERE a2.id_proyecto = a.id_proyecto "
                + "ORDER BY a2.fecha DESC, a2.id_avance DESC LIMIT 1)";
        Map<Integer, Integer> mapa = new HashMap<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "ULTIMO_AVANCE");
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getInt(1), rs.getInt(2));
            }
            return mapa;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "ULTIMO_AVANCE",
                    "Error al consultar el avance de los proyectos.", ex);
        }
    }

    public AvanceProyecto crear(AvanceProyecto a) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String sql = "INSERT INTO avance_proyecto (id_proyecto, fecha, horas, tarea, id_empleado, porcentaje, "
                + "estado, material_usado, observaciones, evidencias) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Conexion.conectarStrict(MODULO, "CREAR_AVANCE");
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenar(ps, a);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    a.setIdAvance(rs.getInt(1));
                }
            }
            AppLogger.registrarInfo(MODULO, PROCESO, "CREAR_AVANCE",
                    "Avance ID=" + a.getIdAvance() + " del proyecto ID=" + a.getIdProyecto() + " (" + a.getPorcentaje() + " %)");
            return a;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CREAR_AVANCE",
                    "No se pudo guardar el avance.", ex);
        }
    }

    public boolean actualizar(AvanceProyecto a) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String sql = "UPDATE avance_proyecto SET id_proyecto = ?, fecha = ?, horas = ?, tarea = ?, id_empleado = ?, "
                + "porcentaje = ?, estado = ?, material_usado = ?, observaciones = ?, evidencias = ? WHERE id_avance = ?";
        try (Connection con = Conexion.conectarStrict(MODULO, "ACTUALIZAR_AVANCE");
             PreparedStatement ps = con.prepareStatement(sql)) {
            llenar(ps, a);
            ps.setInt(11, a.getIdAvance());
            boolean ok = ps.executeUpdate() > 0;
            AppLogger.registrarInfo(MODULO, PROCESO, "ACTUALIZAR_AVANCE", "Avance ID=" + a.getIdAvance() + " editado");
            return ok;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "ACTUALIZAR_AVANCE",
                    "No se pudieron guardar los cambios del avance.", ex);
        }
    }

    public boolean eliminar(int idAvance) throws SoptecException {
        MigracionBD.asegurarEsquema();
        try (Connection con = Conexion.conectarStrict(MODULO, "ELIMINAR_AVANCE");
             PreparedStatement ps = con.prepareStatement("DELETE FROM avance_proyecto WHERE id_avance = ?")) {
            ps.setInt(1, idAvance);
            boolean ok = ps.executeUpdate() > 0;
            AppLogger.registrarInfo(MODULO, PROCESO, "ELIMINAR_AVANCE", "Avance ID=" + idAvance + " eliminado");
            return ok;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "ELIMINAR_AVANCE",
                    "No se pudo eliminar el avance.", ex);
        }
    }

    private void llenar(PreparedStatement ps, AvanceProyecto a) throws SQLException {
        ps.setInt(1, a.getIdProyecto());
        ps.setDate(2, Date.valueOf(a.getFecha()));
        ps.setInt(3, a.getHoras());
        ps.setString(4, a.getTarea().trim());
        DaoSupport.setNullableInt(ps, 5, a.getIdEmpleado());
        ps.setInt(6, a.getPorcentaje());
        DaoSupport.setNullableString(ps, 7, a.getEstado());
        DaoSupport.setNullableString(ps, 8, a.getMaterialUsado());
        DaoSupport.setNullableString(ps, 9, a.getObservaciones());
        DaoSupport.setNullableString(ps, 10, a.getEvidencias());
    }

    private AvanceProyecto mapear(ResultSet rs) throws SQLException {
        AvanceProyecto a = new AvanceProyecto();
        a.setIdAvance(rs.getInt(1));
        a.setIdProyecto(rs.getInt(2));
        Date f = rs.getDate(3);
        a.setFecha(f != null ? f.toLocalDate() : null);
        a.setHoras(rs.getInt(4));
        a.setTarea(rs.getString(5));
        int idEmp = rs.getInt(6);
        a.setIdEmpleado(rs.wasNull() ? null : idEmp);
        a.setEmpleadoNombre(rs.getString(7));
        a.setPorcentaje(rs.getInt(8));
        a.setEstado(rs.getString(9));
        a.setMaterialUsado(rs.getString(10));
        a.setObservaciones(rs.getString(11));
        a.setEvidencias(rs.getString(12));
        return a;
    }
}
