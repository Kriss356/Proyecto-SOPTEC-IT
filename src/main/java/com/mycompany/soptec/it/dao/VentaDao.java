package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.DetalleServicio;
import com.mycompany.soptec.it.modelo.Pago;
import com.mycompany.soptec.it.modelo.Venta;
import com.mycompany.soptec.it.util.AppLogger;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de ventas / facturas (tabla venta) con sus servicios (venta_servicio, relación M:N)
 * y sus pagos (tabla pago). Todas las operaciones que tocan varias tablas usan una transacción:
 * si algo falla se hace ROLLBACK y no quedan registros parciales.
 */
public class VentaDao {

    private static final String MODULO = "Ventas";
    private static final String PROCESO = "VentaDao";

    private static final String SELECT_BASE = "SELECT v.id_venta, v.folio, v.id_cliente, v.id_proyecto, v.id_cita, "
            + "v.fecha_venta, v.fecha_vencimiento, v.subtotal, v.iva, v.total, v.metodo_pago, v.estado, "
            + "v.observaciones, v.activo, c.nombre_cliente, c.rfc, p.nombre_proyecto, "
            + "IFNULL((SELECT SUM(pg.monto) FROM pago pg WHERE pg.id_venta = v.id_venta), 0) AS pagado "
            + "FROM venta v "
            + "INNER JOIN cliente c ON c.id_cliente = v.id_cliente "
            + "LEFT JOIN proyecto p ON p.id_proyecto = v.id_proyecto ";

    /** Punto de la transacción donde se puede provocar un fallo controlado (pruebas). */
    public enum FalloSimulado { NINGUNO, DESPUES_DE_SERVICIOS, DESPUES_DEL_PAGO }

    // =====================================================================
    // Registrar venta + servicios + pago inicial (UNA transacción)
    // =====================================================================
    public Venta registrar(Venta v, Pago pagoInicial, FalloSimulado fallo) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "REGISTRAR_VENTA";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);

            String sql = "INSERT INTO venta (id_cliente, id_proyecto, id_cita, fecha_venta, fecha_vencimiento, "
                    + "subtotal, iva, total, metodo_pago, estado, observaciones, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
            int idVenta;
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, v.getIdCliente());
                DaoSupport.setNullableInt(ps, 2, v.getIdProyecto());
                DaoSupport.setNullableInt(ps, 3, v.getIdCita());
                ps.setDate(4, Date.valueOf(v.getFechaVenta()));
                DaoSupport.setNullableDate(ps, 5, v.getFechaVencimiento());
                ps.setBigDecimal(6, v.getSubtotal());
                ps.setBigDecimal(7, v.getIva());
                ps.setBigDecimal(8, v.getTotal());
                DaoSupport.setNullableString(ps, 9, v.getMetodoPago());
                ps.setString(10, Venta.PENDIENTE);
                DaoSupport.setNullableString(ps, 11, v.getObservaciones());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("No se obtuvo el ID de la venta", "45000", 9998);
                    }
                    idVenta = keys.getInt(1);
                }
            }
            String folio = String.format("F-%04d", idVenta);
            try (PreparedStatement ps = con.prepareStatement("UPDATE venta SET folio = ? WHERE id_venta = ?")) {
                ps.setString(1, folio);
                ps.setInt(2, idVenta);
                ps.executeUpdate();
            }

            insertarServicios(con, idVenta, v.getDetalles());
            if (fallo == FalloSimulado.DESPUES_DE_SERVICIOS) {
                throw new SQLException("Fallo simulado despues de guardar los servicios de la venta.", "45000", 9999);
            }

            if (pagoInicial != null && pagoInicial.getMonto().signum() > 0) {
                pagoInicial.setIdVenta(idVenta);
                pagoInicial.setIdProyecto(v.getIdProyecto());
                if (pagoInicial.getConcepto() == null || pagoInicial.getConcepto().isBlank()) {
                    pagoInicial.setConcepto("Pago inicial " + folio);
                }
                insertarPago(con, pagoInicial, v.getTotal());
            }
            if (fallo == FalloSimulado.DESPUES_DEL_PAGO) {
                throw new SQLException("Fallo simulado despues de guardar el pago de la venta.", "45000", 9999);
            }
            recalcularEstado(con, idVenta);

            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Transaccion confirmada: venta " + folio + " con "
                    + v.getDetalles().size() + " servicio(s)"
                    + (pagoInicial != null && pagoInicial.getMonto().signum() > 0 ? " y pago inicial" : ""));
            v.setIdVenta(idVenta);
            v.setFolio(folio);
            return v;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo registrar la venta. Se revirtieron los cambios: no se guardó la venta, "
                    + "ni sus servicios, ni el pago.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    // =====================================================================
    // Actualizar venta y reemplazar sus servicios (UNA transacción)
    // =====================================================================
    public boolean actualizar(Venta v, boolean simularFallo) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "ACTUALIZAR_VENTA";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            BigDecimal pagado = bloquearYObtenerPagado(con, v.getIdVenta());
            if (pagado == null) {
                con.rollback();
                return false;
            }
            if (v.getTotal().compareTo(pagado) < 0) {
                throw new ValidacionException(MODULO, op, "El nuevo total de la venta (" + v.getTotal()
                        + ") no puede ser menor a lo que el cliente ya pagó (" + pagado + ").");
            }
            String sql = "UPDATE venta SET id_cliente = ?, id_proyecto = ?, id_cita = ?, fecha_venta = ?, "
                    + "fecha_vencimiento = ?, subtotal = ?, iva = ?, total = ?, metodo_pago = ?, observaciones = ? "
                    + "WHERE id_venta = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, v.getIdCliente());
                DaoSupport.setNullableInt(ps, 2, v.getIdProyecto());
                DaoSupport.setNullableInt(ps, 3, v.getIdCita());
                ps.setDate(4, Date.valueOf(v.getFechaVenta()));
                DaoSupport.setNullableDate(ps, 5, v.getFechaVencimiento());
                ps.setBigDecimal(6, v.getSubtotal());
                ps.setBigDecimal(7, v.getIva());
                ps.setBigDecimal(8, v.getTotal());
                DaoSupport.setNullableString(ps, 9, v.getMetodoPago());
                DaoSupport.setNullableString(ps, 10, v.getObservaciones());
                ps.setInt(11, v.getIdVenta());
                ps.executeUpdate();
            }
            try (PreparedStatement del = con.prepareStatement("DELETE FROM venta_servicio WHERE id_venta = ?")) {
                del.setInt(1, v.getIdVenta());
                del.executeUpdate();
            }
            if (simularFallo) {
                throw new SQLException("Fallo simulado al reemplazar los servicios de la venta.", "45000", 9999);
            }
            insertarServicios(con, v.getIdVenta(), v.getDetalles());
            // Los pagos de la venta siguen al proyecto elegido
            try (PreparedStatement ps = con.prepareStatement("UPDATE pago SET id_proyecto = ? WHERE id_venta = ?")) {
                DaoSupport.setNullableInt(ps, 1, v.getIdProyecto());
                ps.setInt(2, v.getIdVenta());
                ps.executeUpdate();
            }
            recalcularEstado(con, v.getIdVenta());
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Transaccion confirmada: venta ID=" + v.getIdVenta()
                    + " con " + v.getDetalles().size() + " servicio(s)");
            return true;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo actualizar la venta y sus servicios. Se revirtieron los cambios.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    // =====================================================================
    // Abonos (pago + estado de la venta en UNA transacción)
    // =====================================================================
    public Pago registrarAbono(int idVenta, Pago pago) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "REGISTRAR_ABONO";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            BigDecimal total;
            String estado;
            Integer idProyecto;
            String folio;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT total, estado, id_proyecto, folio FROM venta WHERE id_venta = ? FOR UPDATE")) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new ValidacionException(MODULO, op, "La venta seleccionada ya no existe.");
                    }
                    total = rs.getBigDecimal(1);
                    estado = rs.getString(2);
                    int p = rs.getInt(3);
                    idProyecto = rs.wasNull() ? null : p;
                    folio = rs.getString(4);
                }
            }
            if (Venta.CANCELADA.equalsIgnoreCase(estado)) {
                throw new ValidacionException(MODULO, op, "La venta " + folio + " está cancelada; no se le pueden registrar pagos.");
            }
            pago.setIdVenta(idVenta);
            pago.setIdProyecto(idProyecto);
            if (pago.getConcepto() == null || pago.getConcepto().isBlank()) {
                pago.setConcepto("Abono " + folio);
            }
            insertarPago(con, pago, total);
            recalcularEstado(con, idVenta);
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Abono registrado a la venta " + folio
                    + " por $" + pago.getMonto());
            return pago;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo registrar el pago. Se revirtieron los cambios.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    public boolean eliminarPago(int idPago) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "ELIMINAR_PAGO";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            Integer idVenta = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT id_venta FROM pago WHERE id_pago = ? FOR UPDATE")) {
                ps.setInt(1, idPago);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    int v = rs.getInt(1);
                    idVenta = rs.wasNull() ? null : v;
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM pago WHERE id_pago = ?")) {
                ps.setInt(1, idPago);
                ps.executeUpdate();
            }
            if (idVenta != null) {
                recalcularEstado(con, idVenta);
            }
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Pago ID=" + idPago + " eliminado");
            return true;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo eliminar el pago. Se revirtieron los cambios.", ex);
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    /**
     * Corrige un pago ya registrado (monto, fecha, método y concepto). Verifica que el nuevo monto
     * no supere el saldo de la venta y recalcula el estado; todo en UNA transacción.
     */
    public void actualizarPago(Pago pago) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "ACTUALIZAR_PAGO";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            Integer idVenta;
            try (PreparedStatement ps = con.prepareStatement("SELECT id_venta FROM pago WHERE id_pago = ? FOR UPDATE")) {
                ps.setInt(1, pago.getIdPago());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new ValidacionException(MODULO, op, "El pago ya no existe. Refresca la pantalla.");
                    }
                    int v = rs.getInt(1);
                    idVenta = rs.wasNull() ? null : v;
                }
            }
            String estadoCuenta = "Abono";
            if (idVenta != null) {
                BigDecimal total;
                String estado;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT total, estado FROM venta WHERE id_venta = ? FOR UPDATE")) {
                    ps.setInt(1, idVenta);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new ValidacionException(MODULO, op, "La venta del pago ya no existe.");
                        }
                        total = rs.getBigDecimal(1);
                        estado = rs.getString(2);
                    }
                }
                if (Venta.CANCELADA.equalsIgnoreCase(estado)) {
                    throw new ValidacionException(MODULO, op, "La venta está cancelada; sus pagos ya no se pueden modificar.");
                }
                BigDecimal otrosPagos = BigDecimal.ZERO;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT IFNULL(SUM(monto), 0) FROM pago WHERE id_venta = ? AND id_pago <> ?")) {
                    ps.setInt(1, idVenta);
                    ps.setInt(2, pago.getIdPago());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            otrosPagos = rs.getBigDecimal(1);
                        }
                    }
                }
                BigDecimal saldoDisponible = total.subtract(otrosPagos);
                if (pago.getMonto().compareTo(saldoDisponible) > 0) {
                    throw new ValidacionException(MODULO, op, "El pago ($" + pago.getMonto()
                            + ") es mayor a lo que falta por pagar de la venta ($" + saldoDisponible + ").");
                }
                estadoCuenta = pago.getMonto().compareTo(saldoDisponible) == 0 ? "Liquidado" : "Abono";
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE pago SET monto = ?, fecha_pago = ?, metodo_pago = ?, "
                    + "concepto = IFNULL(?, concepto), estado_cuenta = ? WHERE id_pago = ?")) {
                ps.setBigDecimal(1, pago.getMonto());
                ps.setDate(2, Date.valueOf(pago.getFechaPago()));
                DaoSupport.setNullableString(ps, 3, pago.getMetodoPago());
                DaoSupport.setNullableString(ps, 4, pago.getConcepto());
                ps.setString(5, estadoCuenta);
                ps.setInt(6, pago.getIdPago());
                ps.executeUpdate();
            }
            if (idVenta != null) {
                recalcularEstado(con, idVenta);
            }
            con.commit();
            pago.setEstadoCuenta(estadoCuenta);
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Pago ID=" + pago.getIdPago() + " corregido a $" + pago.getMonto());
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudieron guardar los cambios del pago. Se revirtieron los cambios.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    // =====================================================================
    // Consultas
    // =====================================================================
    public Venta obtenerPorId(int idVenta) throws SoptecException {
        MigracionBD.asegurarEsquema();
        try (Connection con = Conexion.conectarStrict(MODULO, "CONSULTAR_VENTA");
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE v.id_venta = ?")) {
            ps.setInt(1, idVenta);
            Venta v;
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                v = mapear(rs);
            }
            v.setDetalles(listarServicios(con, idVenta));
            return v;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CONSULTAR_VENTA",
                    "Error al consultar la venta.", ex);
        }
    }

    public List<Venta> buscar(String criterio, boolean incluirCanceladas) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String like = "%" + (criterio == null ? "" : criterio.trim()) + "%";
        String sql = SELECT_BASE
                + "WHERE (IFNULL(v.folio, '') LIKE ? OR c.nombre_cliente LIKE ? OR IFNULL(c.rfc, '') LIKE ? "
                + "OR IFNULL(p.nombre_proyecto, '') LIKE ?) "
                + (incluirCanceladas ? "" : "AND v.estado <> 'Cancelada' ")
                + "ORDER BY v.fecha_venta DESC, v.id_venta DESC";
        List<Venta> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "LISTAR_VENTAS");
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            for (Venta v : lista) {
                v.setDetalles(listarServicios(con, v.getIdVenta()));
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "LISTAR_VENTAS",
                    "Error al consultar las ventas.", ex);
        }
    }

    public List<Pago> listarPagos(int idVenta) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String sql = "SELECT pg.id_pago, pg.id_proyecto, pg.id_venta, v.folio, pg.concepto, pg.monto, pg.fecha_pago, "
                + "pg.metodo_pago, pg.estado_cuenta FROM pago pg LEFT JOIN venta v ON v.id_venta = pg.id_venta "
                + "WHERE pg.id_venta = ? ORDER BY pg.fecha_pago, pg.id_pago";
        List<Pago> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "LISTAR_PAGOS");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pago p = new Pago();
                    p.setIdPago(rs.getInt(1));
                    int pr = rs.getInt(2);
                    p.setIdProyecto(rs.wasNull() ? null : pr);
                    p.setIdVenta(rs.getInt(3));
                    p.setFolioVenta(rs.getString(4));
                    p.setConcepto(rs.getString(5));
                    p.setMonto(rs.getBigDecimal(6));
                    Date f = rs.getDate(7);
                    p.setFechaPago(f != null ? f.toLocalDate() : null);
                    p.setMetodoPago(rs.getString(8));
                    p.setEstadoCuenta(rs.getString(9));
                    lista.add(p);
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "LISTAR_PAGOS",
                    "Error al consultar los pagos de la venta.", ex);
        }
    }

    // =====================================================================
    // Baja lógica (cancelar) y eliminación física
    // =====================================================================
    public boolean cancelar(int idVenta) throws SoptecException {
        MigracionBD.asegurarEsquema();
        try (Connection con = Conexion.conectarStrict(MODULO, "CANCELAR_VENTA");
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE venta SET estado = 'Cancelada', activo = 0 WHERE id_venta = ?")) {
            ps.setInt(1, idVenta);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CANCELAR_VENTA",
                    "No se pudo cancelar la venta.", ex);
        }
    }

    /** Elimina la venta y sus servicios. Si tiene pagos, MySQL lo impide (FK pago → venta). */
    public boolean eliminarFisico(int idVenta) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = "ELIMINAR_FISICO_VENTA";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM venta_servicio WHERE id_venta = ?")) {
                ps.setInt(1, idVenta);
                ps.executeUpdate();
            }
            int filas;
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM venta WHERE id_venta = ?")) {
                ps.setInt(1, idVenta);
                filas = ps.executeUpdate();
            }
            con.commit();
            return filas > 0;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo eliminar la venta. Se revirtieron los cambios.", ex);
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    // =====================================================================
    // Auxiliares (siempre dentro de la conexión/transacción recibida)
    // =====================================================================
    private void insertarServicios(Connection con, int idVenta, List<DetalleServicio> detalles) throws SQLException {
        String sql = "INSERT INTO venta_servicio (id_venta, id_servicio, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (DetalleServicio d : detalles) {
                ps.setInt(1, idVenta);
                ps.setInt(2, d.getIdServicio());
                ps.setInt(3, d.getCantidad());
                ps.setBigDecimal(4, d.getPrecioUnitario());
                ps.executeUpdate();
            }
        }
    }

    private void insertarPago(Connection con, Pago p, BigDecimal totalVenta) throws SQLException, SoptecException {
        BigDecimal pagado = BigDecimal.ZERO;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT IFNULL(SUM(monto), 0) FROM pago WHERE id_venta = ?")) {
            ps.setInt(1, p.getIdVenta());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pagado = rs.getBigDecimal(1);
                }
            }
        }
        BigDecimal saldo = totalVenta.subtract(pagado);
        if (p.getMonto().compareTo(saldo) > 0) {
            throw new ValidacionException(MODULO, "REGISTRAR_PAGO", "El pago ($" + p.getMonto()
                    + ") es mayor al saldo pendiente de la venta ($" + saldo + ").");
        }
        String estadoCuenta = p.getMonto().compareTo(saldo) == 0 ? "Liquidado" : "Abono";
        String sql = "INSERT INTO pago (id_proyecto, id_venta, concepto, monto, fecha_pago, metodo_pago, estado_cuenta) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (p.getIdProyecto() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, p.getIdProyecto());
            }
            ps.setInt(2, p.getIdVenta());
            DaoSupport.setNullableString(ps, 3, p.getConcepto());
            ps.setBigDecimal(4, p.getMonto());
            ps.setDate(5, Date.valueOf(p.getFechaPago()));
            DaoSupport.setNullableString(ps, 6, p.getMetodoPago());
            ps.setString(7, estadoCuenta);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setIdPago(keys.getInt(1));
                }
            }
        }
        p.setEstadoCuenta(estadoCuenta);
    }

    private BigDecimal bloquearYObtenerPagado(Connection con, int idVenta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id_venta FROM venta WHERE id_venta = ? FOR UPDATE")) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT IFNULL(SUM(monto), 0) FROM pago WHERE id_venta = ?")) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /** Pone Pendiente / Parcial / Pagada según los pagos (una venta cancelada no cambia). */
    private void recalcularEstado(Connection con, int idVenta) throws SQLException {
        String sql = "UPDATE venta v SET v.estado = CASE "
                + " WHEN v.estado = 'Cancelada' THEN 'Cancelada' "
                + " WHEN (SELECT IFNULL(SUM(monto), 0) FROM pago WHERE id_venta = v.id_venta) <= 0 THEN 'Pendiente' "
                + " WHEN (SELECT IFNULL(SUM(monto), 0) FROM pago WHERE id_venta = v.id_venta) >= v.total THEN 'Pagada' "
                + " ELSE 'Parcial' END "
                + "WHERE v.id_venta = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }
    }

    private List<DetalleServicio> listarServicios(Connection con, int idVenta) throws SQLException {
        String sql = "SELECT vs.id_servicio, s.nombre_servicio, vs.cantidad, vs.precio_unitario "
                + "FROM venta_servicio vs INNER JOIN servicio s ON s.id_servicio = vs.id_servicio "
                + "WHERE vs.id_venta = ? ORDER BY s.nombre_servicio";
        List<DetalleServicio> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new DetalleServicio(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getBigDecimal(4)));
                }
            }
        }
        return lista;
    }

    private Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setIdVenta(rs.getInt("id_venta"));
        v.setFolio(rs.getString("folio"));
        v.setIdCliente(rs.getInt("id_cliente"));
        int pr = rs.getInt("id_proyecto");
        v.setIdProyecto(rs.wasNull() ? null : pr);
        int ci = rs.getInt("id_cita");
        v.setIdCita(rs.wasNull() ? null : ci);
        Date fv = rs.getDate("fecha_venta");
        v.setFechaVenta(fv != null ? fv.toLocalDate() : null);
        Date fven = rs.getDate("fecha_vencimiento");
        v.setFechaVencimiento(fven != null ? fven.toLocalDate() : null);
        v.setSubtotal(rs.getBigDecimal("subtotal"));
        v.setIva(rs.getBigDecimal("iva"));
        v.setTotal(rs.getBigDecimal("total"));
        v.setMetodoPago(rs.getString("metodo_pago"));
        v.setEstado(rs.getString("estado"));
        v.setObservaciones(rs.getString("observaciones"));
        v.setActivo(rs.getBoolean("activo"));
        v.setClienteNombre(rs.getString("nombre_cliente"));
        v.setClienteRfc(rs.getString("rfc"));
        v.setProyectoNombre(rs.getString("nombre_proyecto"));
        v.setPagado(rs.getBigDecimal("pagado"));
        return v;
    }
}
