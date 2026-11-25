package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.OperacionBDException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.modelo.Servicio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO del catálogo de servicios (tabla servicio).
 */
public class ServicioDao {

    private static final String MODULO = "Servicios";
    private static final String PROCESO = "ServicioDao";
    private static final String COLUMNAS = "id_servicio, nombre_servicio, descripcion, precio, imagen, activo";

    public Servicio crear(Servicio s) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String sql = "INSERT INTO servicio (nombre_servicio, descripcion, precio, imagen, activo) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Conexion.conectarStrict(MODULO, "CREAR_SERVICIO");
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getNombreServicio());
            DaoSupport.setNullableString(ps, 2, s.getDescripcion());
            ps.setBigDecimal(3, s.getPrecio());
            DaoSupport.setNullableString(ps, 4, s.getImagen());
            ps.setBoolean(5, s.isActivo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setIdServicio(keys.getInt(1));
                    return s;
                }
            }
            throw new OperacionBDException(MODULO, "CREAR_SERVICIO",
                    "El servicio se guardó pero no se obtuvo su identificador.");
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CREAR_SERVICIO",
                    "No se pudo registrar el servicio.", ex);
        }
    }

    public Servicio obtenerPorId(int id) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String sql = "SELECT " + COLUMNAS + " FROM servicio WHERE id_servicio = ?";
        try (Connection con = Conexion.conectarStrict(MODULO, "CONSULTAR_SERVICIO");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CONSULTAR_SERVICIO",
                    "Error al consultar el servicio.", ex);
        }
    }

    public List<Servicio> listar(boolean incluirInactivos) throws SoptecException {
        return buscar(null, incluirInactivos);
    }

    public List<Servicio> buscar(String criterio, boolean incluirInactivos) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String like = "%" + (criterio == null ? "" : criterio.trim()) + "%";
        String sql = "SELECT " + COLUMNAS + " FROM servicio "
                + "WHERE (nombre_servicio LIKE ? OR IFNULL(descripcion, '') LIKE ?) "
                + (incluirInactivos ? "" : "AND activo = 1 ")
                + "ORDER BY nombre_servicio";
        List<Servicio> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "LISTAR_SERVICIOS");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "LISTAR_SERVICIOS",
                    "Error al consultar el catálogo de servicios.", ex);
        }
    }

    public boolean actualizar(Servicio s) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String sql = "UPDATE servicio SET nombre_servicio = ?, descripcion = ?, precio = ?, imagen = ?, activo = ? "
                + "WHERE id_servicio = ?";
        try (Connection con = Conexion.conectarStrict(MODULO, "ACTUALIZAR_SERVICIO");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getNombreServicio());
            DaoSupport.setNullableString(ps, 2, s.getDescripcion());
            ps.setBigDecimal(3, s.getPrecio());
            DaoSupport.setNullableString(ps, 4, s.getImagen());
            ps.setBoolean(5, s.isActivo());
            ps.setInt(6, s.getIdServicio());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "ACTUALIZAR_SERVICIO",
                    "No se pudo actualizar el servicio.", ex);
        }
    }

    public boolean cambiarEstadoActivo(int id, boolean activo) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String op = activo ? "REACTIVAR_SERVICIO" : "DESACTIVAR_SERVICIO";
        try (Connection con = Conexion.conectarStrict(MODULO, op);
             PreparedStatement ps = con.prepareStatement("UPDATE servicio SET activo = ? WHERE id_servicio = ?")) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo cambiar el estado del servicio.", ex);
        }
    }

    public boolean eliminarFisico(int id) throws SoptecException {
        MigracionBD.asegurarEsquema();
        try (Connection con = Conexion.conectarStrict(MODULO, "ELIMINAR_FISICO_SERVICIO");
             PreparedStatement ps = con.prepareStatement("DELETE FROM servicio WHERE id_servicio = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "ELIMINAR_FISICO_SERVICIO",
                    "No se pudo eliminar el servicio.", ex);
        }
    }

    /** Cuántas citas y ventas usan el servicio (para bloquear su eliminación). */
    public int contarUsos(int id) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String sql = "SELECT (SELECT COUNT(*) FROM cita_servicio WHERE id_servicio = ?)"
                + " + (SELECT COUNT(*) FROM venta_servicio WHERE id_servicio = ?)";
        try (Connection con = Conexion.conectarStrict(MODULO, "VERIFICAR_RELACIONES");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "VERIFICAR_RELACIONES",
                    "Error al verificar si el servicio está en uso.", ex);
        }
    }

    public boolean existeNombre(String nombre, int excluirId) throws SoptecException {
        MigracionBD.asegurarEsquema();
        String sql = "SELECT COUNT(*) FROM servicio WHERE LOWER(nombre_servicio) = LOWER(?) AND id_servicio <> ?";
        try (Connection con = Conexion.conectarStrict(MODULO, "VERIFICAR_DUPLICADO");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre == null ? "" : nombre.trim());
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "VERIFICAR_DUPLICADO",
                    "Error al verificar el nombre del servicio.", ex);
        }
    }

    private Servicio mapear(ResultSet rs) throws SQLException {
        return new Servicio(rs.getInt("id_servicio"), rs.getString("nombre_servicio"), rs.getString("descripcion"),
                rs.getBigDecimal("precio"), rs.getString("imagen"), rs.getBoolean("activo"));
    }
}
