package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.util.AppLogger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Material que surte cada proveedor (tabla material_proveedor, relación muchos a muchos).
 */
public class MaterialProveedorDao {

    private static final String MODULO = "MaterialProveedor";
    private static final String PROCESO = "MaterialProveedorDao";

    public List<Material> listarMateriales(int idProveedor) throws SoptecException {
        String sql = "SELECT m.id_material, m.descripcion_material, m.precio_costo, m.existencias, m.activo "
                + "FROM material_proveedor mp INNER JOIN material m ON m.id_material = mp.id_material "
                + "WHERE mp.id_proveedor = ? ORDER BY m.descripcion_material";
        List<Material> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "LISTAR_MATERIAL_PROVEEDOR");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Material(rs.getInt(1), rs.getString(2), rs.getBigDecimal(3), rs.getInt(4),
                            rs.getBoolean(5)));
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "LISTAR_MATERIAL_PROVEEDOR",
                    "Error al consultar el material del proveedor.", ex);
        }
    }

    public List<Proveedor> listarProveedores(int idMaterial) throws SoptecException {
        String sql = "SELECT p.id_proveedor, p.nombre_proveedor, p.telefono_proveedor, p.correo_proveedor, "
                + "p.imagen, p.activo FROM material_proveedor mp "
                + "INNER JOIN proveedor p ON p.id_proveedor = mp.id_proveedor "
                + "WHERE mp.id_material = ? ORDER BY p.nombre_proveedor";
        List<Proveedor> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "LISTAR_PROVEEDORES_MATERIAL");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idMaterial);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Proveedor(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getBoolean(6)));
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "LISTAR_PROVEEDORES_MATERIAL",
                    "Error al consultar los proveedores del material.", ex);
        }
    }

    /** Reemplaza de una sola vez la lista de materiales que surte el proveedor (una transacción). */
    public void reemplazarMateriales(int idProveedor, List<Integer> idsMateriales, boolean simularFallo)
            throws SoptecException {
        String op = "GUARDAR_MATERIAL_PROVEEDOR";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);
            try (PreparedStatement del = con.prepareStatement("DELETE FROM material_proveedor WHERE id_proveedor = ?")) {
                del.setInt(1, idProveedor);
                del.executeUpdate();
            }
            if (simularFallo) {
                throw new SQLException("Fallo simulado al guardar el material del proveedor.", "45000", 9999);
            }
            try (PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO material_proveedor (id_material, id_proveedor) VALUES (?, ?)")) {
                for (Integer id : idsMateriales) {
                    ins.setInt(1, id);
                    ins.setInt(2, idProveedor);
                    ins.executeUpdate();
                }
            }
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Proveedor ID=" + idProveedor + " surte "
                    + idsMateriales.size() + " material(es)");
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo guardar el material del proveedor. Se revirtieron los cambios.", ex);
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }
}
