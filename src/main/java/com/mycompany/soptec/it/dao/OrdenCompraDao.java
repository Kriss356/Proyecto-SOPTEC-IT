package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.OrdenCompra;
import com.mycompany.soptec.it.modelo.PartidaMaterial;
import com.mycompany.soptec.it.util.AppLogger;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Órdenes de compra (orden_compra) con sus partidas de material (orden_compra_material, M:N).
 */
public class OrdenCompraDao {

    private static final String MODULO = "OrdenesCompra";
    private static final String PROCESO = "OrdenCompraDao";

    private static final String SELECT_BASE = "SELECT oc.id_orden_compra, oc.id_proyecto, oc.id_proveedor, "
            + "oc.fecha_orden, oc.total_compra, oc.tiempo_entrega, oc.estado, p.nombre_proyecto, pr.nombre_proveedor "
            + "FROM orden_compra oc "
            + "LEFT JOIN proyecto p ON p.id_proyecto = oc.id_proyecto "
            + "LEFT JOIN proveedor pr ON pr.id_proveedor = oc.id_proveedor ";

    /** Registra la orden y todas sus partidas en una transacción. */
    public OrdenCompra crear(OrdenCompra o, boolean simularFallo) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "CREAR_ORDEN_COMPRA";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            String sql = "INSERT INTO orden_compra (id_proyecto, id_proveedor, fecha_orden, total_compra, "
                    + "tiempo_entrega, estado) VALUES (?, ?, ?, ?, ?, 'Solicitada')";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                DaoSupport.setNullableInt(ps, 1, o.getIdProyecto());
                DaoSupport.setNullableInt(ps, 2, o.getIdProveedor());
                ps.setDate(3, Date.valueOf(o.getFechaOrden()));
                ps.setBigDecimal(4, o.getTotalCompra());
                DaoSupport.setNullableString(ps, 5, o.getTiempoEntrega());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("No se obtuvo el ID de la orden", "45000", 9998);
                    }
                    o.setIdOrdenCompra(keys.getInt(1));
                }
            }
            if (simularFallo) {
                throw new SQLException("Fallo simulado despues del INSERT de la orden de compra.", "45000", 9999);
            }
            insertarPartidas(con, o);
            con.commit();
            o.setEstado(OrdenCompra.SOLICITADA);
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Transaccion confirmada: " + o.getFolio() + " con "
                    + o.getPartidas().size() + " partida(s)");
            return o;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo registrar la orden de compra. Se revirtieron los cambios: no se guardó ni la orden "
                    + "ni sus partidas.", ex);
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    /** Actualiza encabezado y partidas (solo órdenes Solicitadas). */
    public boolean actualizar(OrdenCompra o) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "ACTUALIZAR_ORDEN_COMPRA";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            String estado = bloquearEstado(con, o.getIdOrdenCompra());
            if (estado == null) {
                con.rollback();
                return false;
            }
            if (!OrdenCompra.SOLICITADA.equalsIgnoreCase(estado)) {
                throw new ValidacionException(MODULO, op, "Solo se pueden modificar órdenes en estado 'Solicitada'. "
                        + "Esta orden está '" + estado + "'.");
            }
            String sql = "UPDATE orden_compra SET id_proyecto = ?, id_proveedor = ?, fecha_orden = ?, total_compra = ?, "
                    + "tiempo_entrega = ? WHERE id_orden_compra = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                DaoSupport.setNullableInt(ps, 1, o.getIdProyecto());
                DaoSupport.setNullableInt(ps, 2, o.getIdProveedor());
                ps.setDate(3, Date.valueOf(o.getFechaOrden()));
                ps.setBigDecimal(4, o.getTotalCompra());
                DaoSupport.setNullableString(ps, 5, o.getTiempoEntrega());
                ps.setInt(6, o.getIdOrdenCompra());
                ps.executeUpdate();
            }
            try (PreparedStatement del = con.prepareStatement(
                    "DELETE FROM orden_compra_material WHERE id_orden_compra = ?")) {
                del.setInt(1, o.getIdOrdenCompra());
                del.executeUpdate();
            }
            insertarPartidas(con, o);
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Orden " + o.getFolio() + " actualizada");
            return true;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo actualizar la orden de compra. Se revirtieron los cambios.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    /** Marca la orden como Recibida y suma sus partidas a las existencias, en una transacción. */
    public void recibir(int idOrden) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "RECIBIR_ORDEN_COMPRA";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            String estado = bloquearEstado(con, idOrden);
            if (estado == null) {
                throw new ValidacionException(MODULO, op, "La orden de compra ya no existe.");
            }
            if (!OrdenCompra.SOLICITADA.equalsIgnoreCase(estado)) {
                throw new ValidacionException(MODULO, op, "Solo se pueden recibir órdenes 'Solicitadas'. "
                        + "Esta orden está '" + estado + "'.");
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE material m INNER JOIN orden_compra_material ocm "
                    + "ON ocm.id_material = m.id_material SET m.existencias = m.existencias + ocm.cantidad "
                    + "WHERE ocm.id_orden_compra = ?")) {
                ps.setInt(1, idOrden);
                ps.executeUpdate();
            }
            cambiarEstado(con, idOrden, OrdenCompra.RECIBIDA);
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Orden ID=" + idOrden + " recibida; existencias actualizadas");
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo registrar la recepción. Se revirtieron los cambios (el almacén quedó como estaba).", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    public boolean cancelar(int idOrden) throws SoptecException {
        MigracionBD.asegurarEsquema();
        try (Connection con = Conexion.conectarStrict(MODULO, "CANCELAR_ORDEN_COMPRA");
             PreparedStatement ps = con.prepareStatement("UPDATE orden_compra SET estado = 'Cancelada' "
                     + "WHERE id_orden_compra = ? AND estado = 'Solicitada'")) {
            ps.setInt(1, idOrden);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CANCELAR_ORDEN_COMPRA",
                    "No se pudo cancelar la orden de compra.", ex);
        }
    }

    /** Elimina la orden y sus partidas (las partidas se borran en cascada). */
    public boolean eliminarFisico(int idOrden) throws SoptecException {
        MigracionBD.asegurarEsquema();
        try (Connection con = Conexion.conectarStrict(MODULO, "ELIMINAR_ORDEN_COMPRA");
             PreparedStatement ps = con.prepareStatement("DELETE FROM orden_compra WHERE id_orden_compra = ?")) {
            ps.setInt(1, idOrden);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "ELIMINAR_ORDEN_COMPRA",
                    "No se pudo eliminar la orden de compra.", ex);
        }
    }

    public OrdenCompra obtenerPorId(int idOrden) throws SoptecException {
        MigracionBD.asegurarEsquema();
        try (Connection con = Conexion.conectarStrict(MODULO, "CONSULTAR_ORDEN_COMPRA");
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE oc.id_orden_compra = ?")) {
            ps.setInt(1, idOrden);
            OrdenCompra o;
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                o = mapear(rs);
            }
            o.setPartidas(listarPartidas(con, idOrden));
            return o;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CONSULTAR_ORDEN_COMPRA",
                    "Error al consultar la orden de compra.", ex);
        }
    }

    public List<OrdenCompra> listar(String criterio) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String like = "%" + (criterio == null ? "" : criterio.trim()) + "%";
        String sql = SELECT_BASE + "WHERE (IFNULL(pr.nombre_proveedor, '') LIKE ? OR IFNULL(p.nombre_proyecto, '') LIKE ? "
                + "OR oc.estado LIKE ?) ORDER BY oc.fecha_orden DESC, oc.id_orden_compra DESC";
        List<OrdenCompra> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "LISTAR_ORDENES_COMPRA");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            for (OrdenCompra o : lista) {
                o.setPartidas(listarPartidas(con, o.getIdOrdenCompra()));
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "LISTAR_ORDENES_COMPRA",
                    "Error al consultar las órdenes de compra.", ex);
        }
    }

    private void insertarPartidas(Connection con, OrdenCompra o) throws SQLException {
        String sql = "INSERT INTO orden_compra_material (id_orden_compra, id_material, cantidad) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (PartidaMaterial p : o.getPartidas()) {
                ps.setInt(1, o.getIdOrdenCompra());
                ps.setInt(2, p.getIdMaterial());
                ps.setBigDecimal(3, BigDecimal.valueOf(p.getCantidad()));
                ps.executeUpdate();
            }
        }
    }

    private List<PartidaMaterial> listarPartidas(Connection con, int idOrden) throws SQLException {
        String sql = "SELECT ocm.id_material, m.descripcion_material, ocm.cantidad, m.precio_costo, m.existencias "
                + "FROM orden_compra_material ocm INNER JOIN material m ON m.id_material = ocm.id_material "
                + "WHERE ocm.id_orden_compra = ? ORDER BY m.descripcion_material";
        List<PartidaMaterial> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idOrden);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PartidaMaterial p = new PartidaMaterial(rs.getInt(1), rs.getString(2),
                            rs.getBigDecimal(3).intValue(), rs.getBigDecimal(4));
                    p.setExistencias(rs.getInt(5));
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    private String bloquearEstado(Connection con, int idOrden) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT estado FROM orden_compra WHERE id_orden_compra = ? FOR UPDATE")) {
            ps.setInt(1, idOrden);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private void cambiarEstado(Connection con, int idOrden, String estado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE orden_compra SET estado = ? WHERE id_orden_compra = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, idOrden);
            ps.executeUpdate();
        }
    }

    private OrdenCompra mapear(ResultSet rs) throws SQLException {
        OrdenCompra o = new OrdenCompra();
        o.setIdOrdenCompra(rs.getInt("id_orden_compra"));
        int pr = rs.getInt("id_proyecto");
        o.setIdProyecto(rs.wasNull() ? null : pr);
        int pv = rs.getInt("id_proveedor");
        o.setIdProveedor(rs.wasNull() ? null : pv);
        Date f = rs.getDate("fecha_orden");
        o.setFechaOrden(f != null ? f.toLocalDate() : null);
        o.setTotalCompra(rs.getBigDecimal("total_compra"));
        o.setTiempoEntrega(rs.getString("tiempo_entrega"));
        o.setEstado(rs.getString("estado"));
        o.setProyectoNombre(rs.getString("nombre_proyecto"));
        o.setProveedorNombre(rs.getString("nombre_proveedor"));
        return o;
    }
}
