package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.MovimientoAlmacen;
import com.mycompany.soptec.it.util.AppLogger;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Historial de movimientos del almacén (tabla movimiento_almacen).
 *
 * <p>Registrar, editar o eliminar un movimiento ajusta las existencias del material y, en las salidas
 * a un proyecto, el material asignado a ese proyecto (proyecto_material). Todo ocurre en UNA transacción:
 * si algo falla, el almacén queda exactamente como estaba.</p>
 */
public class MovimientoAlmacenDao {

    private static final String MODULO = "Materiales";
    private static final String PROCESO = "MovimientoAlmacenDao";

    private static final String SELECT_BASE = "SELECT mv.id_movimiento, mv.fecha, mv.tipo, mv.id_material, "
            + "m.descripcion_material, mv.cantidad, mv.id_proyecto, p.nombre_proyecto, mv.id_empleado, e.nombre, "
            + "mv.referencia, mv.observaciones FROM movimiento_almacen mv "
            + "INNER JOIN material m ON m.id_material = mv.id_material "
            + "LEFT JOIN proyecto p ON p.id_proyecto = mv.id_proyecto "
            + "LEFT JOIN empleado e ON e.id_empleado = mv.id_empleado ";

    /** Lista los movimientos (todos o solo los de una fecha), del más reciente al más antiguo. */
    public List<MovimientoAlmacen> listar(LocalDate fecha) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String sql = SELECT_BASE + (fecha != null ? "WHERE mv.fecha = ? " : "")
                + "ORDER BY mv.fecha DESC, mv.id_movimiento DESC";
        List<MovimientoAlmacen> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "LISTAR_MOVIMIENTOS");
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (fecha != null) {
                ps.setDate(1, Date.valueOf(fecha));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "LISTAR_MOVIMIENTOS",
                    "Error al consultar el historial de movimientos.", ex);
        }
    }

    public MovimientoAlmacen registrar(MovimientoAlmacen mv) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "REGISTRAR_MOVIMIENTO";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            aplicarEfecto(con, mv, +1, op);
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO movimiento_almacen (fecha, tipo, id_material, "
                    + "cantidad, id_proyecto, id_empleado, referencia, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                llenar(ps, mv);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        mv.setIdMovimiento(rs.getInt(1));
                    }
                }
            }
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, mv.getTipo() + " ID=" + mv.getIdMovimiento()
                    + " material ID=" + mv.getIdMaterial() + " x" + mv.getCantidad());
            return mv;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo registrar el movimiento. El almacén quedó como estaba.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    /**
     * Edita un movimiento: deshace el efecto del movimiento original y aplica el nuevo
     * (cambia material, cantidad, tipo o proyecto y el almacén se corrige solo).
     */
    public void actualizar(MovimientoAlmacen nuevo) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "ACTUALIZAR_MOVIMIENTO";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            MovimientoAlmacen anterior = obtenerParaActualizar(con, nuevo.getIdMovimiento());
            if (anterior == null) {
                throw new ValidacionException(MODULO, op, "El movimiento ya no existe. Refresca el historial.");
            }
            aplicarEfecto(con, anterior, -1, op);
            aplicarEfecto(con, nuevo, +1, op);
            try (PreparedStatement ps = con.prepareStatement("UPDATE movimiento_almacen SET fecha = ?, tipo = ?, "
                    + "id_material = ?, cantidad = ?, id_proyecto = ?, id_empleado = ?, referencia = ?, observaciones = ? "
                    + "WHERE id_movimiento = ?")) {
                llenar(ps, nuevo);
                ps.setInt(9, nuevo.getIdMovimiento());
                ps.executeUpdate();
            }
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Movimiento ID=" + nuevo.getIdMovimiento()
                    + " editado; existencias corregidas");
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudieron guardar los cambios del movimiento. El almacén quedó como estaba.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    /** Elimina el movimiento y regresa el almacén a como estaba antes de él. */
    public void eliminar(int idMovimiento) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "ELIMINAR_MOVIMIENTO";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            MovimientoAlmacen anterior = obtenerParaActualizar(con, idMovimiento);
            if (anterior == null) {
                throw new ValidacionException(MODULO, op, "El movimiento ya no existe. Refresca el historial.");
            }
            aplicarEfecto(con, anterior, -1, op);
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM movimiento_almacen WHERE id_movimiento = ?")) {
                ps.setInt(1, idMovimiento);
                ps.executeUpdate();
            }
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Movimiento ID=" + idMovimiento + " eliminado; existencias revertidas");
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo eliminar el movimiento. El almacén quedó como estaba.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    /**
     * Aplica (signo = +1) o deshace (signo = -1) el efecto de un movimiento sobre el almacén
     * y, si es salida a un proyecto, sobre el material del proyecto.
     */
    private void aplicarEfecto(Connection con, MovimientoAlmacen mv, int signo, String op)
            throws SQLException, SoptecException {
        int cambioAlmacen = (mv.isEntrada() ? mv.getCantidad() : -mv.getCantidad()) * signo;

        int existencias;
        String descripcion;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT existencias, descripcion_material FROM material WHERE id_material = ? FOR UPDATE")) {
            ps.setInt(1, mv.getIdMaterial());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Material inexistente id=" + mv.getIdMaterial(), "23000", 1452);
                }
                existencias = rs.getInt(1);
                descripcion = rs.getString(2);
            }
        }
        if (existencias + cambioAlmacen < 0) {
            throw new ValidacionException(MODULO, op, "No alcanzan las existencias de '" + descripcion
                    + "': hay " + existencias + " y el cambio necesita " + (-cambioAlmacen) + ".");
        }
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE material SET existencias = existencias + ? WHERE id_material = ?")) {
            ps.setInt(1, cambioAlmacen);
            ps.setInt(2, mv.getIdMaterial());
            ps.executeUpdate();
        }

        if (!mv.isEntrada() && mv.getIdProyecto() != null && mv.getIdProyecto() > 0) {
            int cambioProyecto = mv.getCantidad() * signo;
            if (cambioProyecto > 0) {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO proyecto_material (id_proyecto, id_material, "
                        + "cantidad) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE cantidad = cantidad + VALUES(cantidad)")) {
                    ps.setInt(1, mv.getIdProyecto());
                    ps.setInt(2, mv.getIdMaterial());
                    ps.setBigDecimal(3, BigDecimal.valueOf(cambioProyecto));
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement("UPDATE proyecto_material SET cantidad = "
                        + "GREATEST(cantidad - ?, 0) WHERE id_proyecto = ? AND id_material = ?")) {
                    ps.setBigDecimal(1, BigDecimal.valueOf(-cambioProyecto));
                    ps.setInt(2, mv.getIdProyecto());
                    ps.setInt(3, mv.getIdMaterial());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM proyecto_material WHERE id_proyecto = ? AND id_material = ? AND cantidad <= 0")) {
                    ps.setInt(1, mv.getIdProyecto());
                    ps.setInt(2, mv.getIdMaterial());
                    ps.executeUpdate();
                }
            }
        }
    }

    private MovimientoAlmacen obtenerParaActualizar(Connection con, int idMovimiento) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE mv.id_movimiento = ? FOR UPDATE")) {
            ps.setInt(1, idMovimiento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    private void llenar(PreparedStatement ps, MovimientoAlmacen mv) throws SQLException {
        ps.setDate(1, Date.valueOf(mv.getFecha()));
        ps.setString(2, mv.isEntrada() ? MovimientoAlmacen.ENTRADA : MovimientoAlmacen.SALIDA);
        ps.setInt(3, mv.getIdMaterial());
        ps.setInt(4, mv.getCantidad());
        DaoSupport.setNullableInt(ps, 5, mv.isEntrada() ? null : mv.getIdProyecto());
        DaoSupport.setNullableInt(ps, 6, mv.getIdEmpleado());
        DaoSupport.setNullableString(ps, 7, mv.getReferencia());
        DaoSupport.setNullableString(ps, 8, mv.getObservaciones());
    }

    private MovimientoAlmacen mapear(ResultSet rs) throws SQLException {
        MovimientoAlmacen mv = new MovimientoAlmacen();
        mv.setIdMovimiento(rs.getInt(1));
        Date f = rs.getDate(2);
        mv.setFecha(f != null ? f.toLocalDate() : null);
        mv.setTipo(rs.getString(3));
        mv.setIdMaterial(rs.getInt(4));
        mv.setMaterialDescripcion(rs.getString(5));
        mv.setCantidad(rs.getInt(6));
        int idProy = rs.getInt(7);
        mv.setIdProyecto(rs.wasNull() ? null : idProy);
        mv.setProyectoNombre(rs.getString(8));
        int idEmp = rs.getInt(9);
        mv.setIdEmpleado(rs.wasNull() ? null : idEmp);
        mv.setEmpleadoNombre(rs.getString(10));
        mv.setReferencia(rs.getString(11));
        mv.setObservaciones(rs.getString(12));
        return mv;
    }
}
