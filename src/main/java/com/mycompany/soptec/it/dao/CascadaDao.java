package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.util.AppLogger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Activar / desactivar en cascada (borrado lógico) respetando las relaciones:
 * <pre>
 *   Cliente ─┬─ Teléfonos
 *            ├─ Citas
 *            └─ Proyectos ── Levantamientos
 *   Empleado ── asignaciones a proyectos (proyecto_empleado)
 * </pre>
 * Cada operación corre en UNA transacción: o cambian todos los registros o no cambia ninguno.
 */
public class CascadaDao {

    private static final String MODULO = "Cascada";
    private static final String PROCESO = "CascadaDao";

    /** Cuántos registros relacionados hay en el estado indicado (activos o inactivos). */
    public static final class Resumen {
        public int telefonos;
        public int citas;
        public int proyectos;
        public int levantamientos;

        public int total() {
            return telefonos + citas + proyectos + levantamientos;
        }

        /** Ej. "2 teléfonos, 1 cita y 3 levantamientos". */
        public String texto() {
            StringBuilder sb = new StringBuilder();
            agregar(sb, telefonos, "teléfono", "teléfonos");
            agregar(sb, citas, "cita", "citas");
            agregar(sb, proyectos, "proyecto", "proyectos");
            agregar(sb, levantamientos, "levantamiento", "levantamientos");
            int i = sb.lastIndexOf(", ");
            return i > 0 ? sb.replace(i, i + 2, " y ").toString() : sb.toString();
        }

        private static void agregar(StringBuilder sb, int n, String uno, String varios) {
            if (n > 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(n).append(' ').append(n == 1 ? uno : varios);
            }
        }
    }

    // =====================================================================
    // Conteos
    // =====================================================================
    public Resumen contarDeCliente(int idCliente, boolean activos) throws SoptecException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM cliente_telefono WHERE id_cliente = ? AND activo = ?), "
                + "(SELECT COUNT(*) FROM cita WHERE id_cliente = ? AND activo = ?), "
                + "(SELECT COUNT(*) FROM proyecto WHERE id_cliente = ? AND activo = ?), "
                + "(SELECT COUNT(*) FROM levantamiento l INNER JOIN proyecto p ON p.id_proyecto = l.id_proyecto "
                + "   WHERE p.id_cliente = ? AND l.activo = ?)";
        try (Connection con = Conexion.conectarStrict(MODULO, "CONTAR_RELACIONES_CLIENTE");
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < 4; i++) {
                ps.setInt(i * 2 + 1, idCliente);
                ps.setBoolean(i * 2 + 2, activos);
            }
            Resumen r = new Resumen();
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    r.telefonos = rs.getInt(1);
                    r.citas = rs.getInt(2);
                    r.proyectos = rs.getInt(3);
                    r.levantamientos = rs.getInt(4);
                }
            }
            return r;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CONTAR_RELACIONES_CLIENTE",
                    "No se pudieron revisar los registros relacionados del cliente.", ex);
        }
    }

    public Resumen contarDeProyecto(int idProyecto, boolean activos) throws SoptecException {
        Resumen r = new Resumen();
        r.levantamientos = contar("SELECT COUNT(*) FROM levantamiento WHERE id_proyecto = ? AND activo = ?",
                idProyecto, activos, "CONTAR_LEVANTAMIENTOS_PROYECTO");
        return r;
    }

    /** Proyectos activos a los que está asignado el empleado. */
    public int contarProyectosDeEmpleado(int idEmpleado) throws SoptecException {
        return contar("SELECT COUNT(*) FROM proyecto_empleado pe INNER JOIN proyecto p ON p.id_proyecto = pe.id_proyecto "
                + "WHERE pe.id_empleado = ? AND p.activo = ?", idEmpleado, true, "CONTAR_PROYECTOS_EMPLEADO");
    }

    /** ¿Está activo el cliente? (null si no existe). */
    public Boolean clienteActivo(int idCliente) throws SoptecException {
        return activo("SELECT activo FROM cliente WHERE id_cliente = ?", idCliente, "CLIENTE_ACTIVO");
    }

    /** ¿Está activo el proyecto? (null si no existe). */
    public Boolean proyectoActivo(int idProyecto) throws SoptecException {
        return activo("SELECT activo FROM proyecto WHERE id_proyecto = ?", idProyecto, "PROYECTO_ACTIVO");
    }

    /** Cliente dueño del proyecto (0 si no existe). */
    public int clienteDeProyecto(int idProyecto) throws SoptecException {
        try (Connection con = Conexion.conectarStrict(MODULO, "CLIENTE_DE_PROYECTO");
             PreparedStatement ps = con.prepareStatement("SELECT id_cliente FROM proyecto WHERE id_proyecto = ?")) {
            ps.setInt(1, idProyecto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CLIENTE_DE_PROYECTO",
                    "No se pudo consultar el cliente del proyecto.", ex);
        }
    }

    // =====================================================================
    // Cambios en cascada (una transacción cada uno)
    // =====================================================================
    /**
     * Activa o desactiva al cliente y, si se pide, también sus teléfonos, citas, proyectos y los
     * levantamientos de esos proyectos.
     */
    public void cambiarCliente(int idCliente, boolean activo, boolean incluirRelacionados) throws SoptecException {
        String op = activo ? "REACTIVAR_CLIENTE_CASCADA" : "DESACTIVAR_CLIENTE_CASCADA";
        ejecutarTransaccion(op, con -> {
            if (incluirRelacionados) {
                actualizar(con, "UPDATE levantamiento l INNER JOIN proyecto p ON p.id_proyecto = l.id_proyecto "
                        + "SET l.activo = ? WHERE p.id_cliente = ?", activo, idCliente);
                actualizar(con, "UPDATE proyecto SET activo = ? WHERE id_cliente = ?", activo, idCliente);
                actualizar(con, "UPDATE cita SET activo = ? WHERE id_cliente = ?", activo, idCliente);
                actualizar(con, "UPDATE cliente_telefono SET activo = ? WHERE id_cliente = ?", activo, idCliente);
            }
            if (actualizar(con, "UPDATE cliente SET activo = ? WHERE id_cliente = ?", activo, idCliente) == 0) {
                throw new ValidacionException(MODULO, op, "El cliente ya no existe. Refresca la pantalla.");
            }
        });
        AppLogger.registrarInfo(MODULO, PROCESO, op, "Cliente ID=" + idCliente + (activo ? " reactivado" : " desactivado")
                + (incluirRelacionados ? " junto con sus teléfonos, citas, proyectos y levantamientos" : ""));
    }

    /**
     * Activa o desactiva el proyecto; opcionalmente sus levantamientos. Al reactivar, si se pide,
     * también reactiva a su cliente (un proyecto activo no puede quedar con un cliente inactivo).
     */
    public void cambiarProyecto(int idProyecto, boolean activo, boolean incluirLevantamientos, boolean reactivarCliente)
            throws SoptecException {
        String op = activo ? "REACTIVAR_PROYECTO_CASCADA" : "DESACTIVAR_PROYECTO_CASCADA";
        ejecutarTransaccion(op, con -> {
            if (activo && reactivarCliente) {
                actualizar(con, "UPDATE cliente c INNER JOIN proyecto p ON p.id_cliente = c.id_cliente "
                        + "SET c.activo = 1 WHERE p.id_proyecto = ?", null, idProyecto);
            }
            if (incluirLevantamientos) {
                actualizar(con, "UPDATE levantamiento SET activo = ? WHERE id_proyecto = ?", activo, idProyecto);
            }
            if (actualizar(con, "UPDATE proyecto SET activo = ? WHERE id_proyecto = ?", activo, idProyecto) == 0) {
                throw new ValidacionException(MODULO, op, "El proyecto ya no existe. Refresca la pantalla.");
            }
        });
        AppLogger.registrarInfo(MODULO, PROCESO, op, "Proyecto ID=" + idProyecto + (activo ? " reactivado" : " desactivado")
                + (incluirLevantamientos ? " con sus levantamientos" : "") + (reactivarCliente ? " y su cliente" : ""));
    }

    /** Reactiva el teléfono y, a la vez, a su cliente inactivo. */
    public void reactivarTelefonoYCliente(int idCliente, String telefono) throws SoptecException {
        ejecutarTransaccion("REACTIVAR_TELEFONO_CASCADA", con -> {
            actualizar(con, "UPDATE cliente SET activo = 1 WHERE id_cliente = ?", null, idCliente);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE cliente_telefono SET activo = 1 WHERE id_cliente = ? AND telefono = ?")) {
                ps.setInt(1, idCliente);
                ps.setString(2, telefono);
                ps.executeUpdate();
            }
        });
    }

    /** Reactiva la cita y, a la vez, a su cliente inactivo. */
    public void reactivarCitaYCliente(int idCita) throws SoptecException {
        ejecutarTransaccion("REACTIVAR_CITA_CASCADA", con -> {
            actualizar(con, "UPDATE cliente c INNER JOIN cita ci ON ci.id_cliente = c.id_cliente "
                    + "SET c.activo = 1 WHERE ci.id_cita = ?", null, idCita);
            actualizar(con, "UPDATE cita SET activo = 1 WHERE id_cita = ?", null, idCita);
        });
    }

    /** Reactiva el levantamiento y, a la vez, su proyecto y el cliente de ese proyecto. */
    public void reactivarLevantamientoYProyecto(int idLevantamiento) throws SoptecException {
        ejecutarTransaccion("REACTIVAR_LEVANTAMIENTO_CASCADA", con -> {
            actualizar(con, "UPDATE cliente c INNER JOIN proyecto p ON p.id_cliente = c.id_cliente "
                    + "INNER JOIN levantamiento l ON l.id_proyecto = p.id_proyecto SET c.activo = 1 "
                    + "WHERE l.id_levantamiento = ?", null, idLevantamiento);
            actualizar(con, "UPDATE proyecto p INNER JOIN levantamiento l ON l.id_proyecto = p.id_proyecto "
                    + "SET p.activo = 1 WHERE l.id_levantamiento = ?", null, idLevantamiento);
            actualizar(con, "UPDATE levantamiento SET activo = 1 WHERE id_levantamiento = ?", null, idLevantamiento);
        });
    }

    /** Desactiva al empleado y lo quita de los proyectos a los que estaba asignado. */
    public void desactivarEmpleadoYQuitarDeProyectos(int idEmpleado) throws SoptecException {
        ejecutarTransaccion("DESACTIVAR_EMPLEADO_CASCADA", con -> {
            actualizar(con, "DELETE pe FROM proyecto_empleado pe INNER JOIN proyecto p ON p.id_proyecto = pe.id_proyecto "
                    + "WHERE pe.id_empleado = ? AND p.activo = 1", null, idEmpleado);
            if (actualizar(con, "UPDATE empleado SET activo = ? WHERE id_empleado = ?", false, idEmpleado) == 0) {
                throw new ValidacionException(MODULO, "DESACTIVAR_EMPLEADO_CASCADA", "El empleado ya no existe.");
            }
        });
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================
    private interface Pasos {
        void ejecutar(Connection con) throws SQLException, SoptecException;
    }

    private void ejecutarTransaccion(String op, Pasos pasos) throws SoptecException {
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            pasos.ejecutar(con);
            con.commit();
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo completar el cambio en cascada. No se modificó ningún registro.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    /** UPDATE/DELETE con (activo, id) o solo (id) cuando activo es null. */
    private int actualizar(Connection con, String sql, Boolean activo, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            if (activo != null) {
                ps.setBoolean(i++, activo);
            }
            ps.setInt(i, id);
            return ps.executeUpdate();
        }
    }

    private int contar(String sql, int id, boolean activos, String op) throws SoptecException {
        try (Connection con = Conexion.conectarStrict(MODULO, op);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setBoolean(2, activos);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op, "No se pudieron revisar los registros relacionados.", ex);
        }
    }

    private Boolean activo(String sql, int id, String op) throws SoptecException {
        try (Connection con = Conexion.conectarStrict(MODULO, op);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean(1) : null;
            }
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op, "No se pudo consultar el estado del registro.", ex);
        }
    }
}
