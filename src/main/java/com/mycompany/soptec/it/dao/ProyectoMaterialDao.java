package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.PartidaMaterial;
import com.mycompany.soptec.it.util.AppLogger;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Material asignado a cada proyecto (tabla proyecto_material, relación muchos a muchos con cantidad).
 * Al asignar se descuenta del almacén; al quitar o reducir, regresa al almacén. Todo en una transacción.
 */
public class ProyectoMaterialDao {

    private static final String MODULO = "MaterialProyecto";
    private static final String PROCESO = "ProyectoMaterialDao";

    public List<PartidaMaterial> listar(int idProyecto) throws SoptecException {
        String sql = "SELECT pm.id_material, m.descripcion_material, pm.cantidad, m.precio_costo, m.existencias "
                + "FROM proyecto_material pm INNER JOIN material m ON m.id_material = pm.id_material "
                + "WHERE pm.id_proyecto = ? ORDER BY m.descripcion_material";
        List<PartidaMaterial> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "LISTAR_MATERIAL_PROYECTO");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProyecto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PartidaMaterial p = new PartidaMaterial(rs.getInt(1), rs.getString(2),
                            rs.getBigDecimal(3).intValue(), rs.getBigDecimal(4));
                    p.setExistencias(rs.getInt(5));
                    lista.add(p);
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "LISTAR_MATERIAL_PROYECTO",
                    "Error al consultar el material del proyecto.", ex);
        }
    }

    /**
     * Guarda la lista completa de material del proyecto y ajusta las existencias del almacén
     * con la diferencia. Si no alcanza el material, o falla cualquier paso, se revierte todo.
     */
    public void guardar(int idProyecto, List<PartidaMaterial> partidas, boolean simularFallo) throws SoptecException {
        String op = "GUARDAR_MATERIAL_PROYECTO";
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, op);
            con.setAutoCommit(false);

            Map<Integer, Integer> anteriores = new HashMap<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_material, cantidad FROM proyecto_material WHERE id_proyecto = ? FOR UPDATE")) {
                ps.setInt(1, idProyecto);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        anteriores.put(rs.getInt(1), rs.getBigDecimal(2).intValue());
                    }
                }
            }
            Map<Integer, Integer> nuevas = new HashMap<>();
            for (PartidaMaterial p : partidas) {
                nuevas.merge(p.getIdMaterial(), p.getCantidad(), Integer::sum);
            }
            Set<Integer> todos = new LinkedHashSet<>(anteriores.keySet());
            todos.addAll(nuevas.keySet());

            for (Integer idMaterial : todos) {
                int diferencia = nuevas.getOrDefault(idMaterial, 0) - anteriores.getOrDefault(idMaterial, 0);
                if (diferencia == 0) {
                    continue;
                }
                int existencias;
                String descripcion;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT existencias, descripcion_material FROM material WHERE id_material = ? FOR UPDATE")) {
                    ps.setInt(1, idMaterial);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Material inexistente id=" + idMaterial, "23000", 1452);
                        }
                        existencias = rs.getInt(1);
                        descripcion = rs.getString(2);
                    }
                }
                if (diferencia > existencias) {
                    throw new ValidacionException(MODULO, op, "No hay suficiente '" + descripcion
                            + "' en el almacén: se necesitan " + diferencia + " y hay " + existencias + ".");
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE material SET existencias = existencias - ? WHERE id_material = ?")) {
                    ps.setInt(1, diferencia);
                    ps.setInt(2, idMaterial);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement del = con.prepareStatement("DELETE FROM proyecto_material WHERE id_proyecto = ?")) {
                del.setInt(1, idProyecto);
                del.executeUpdate();
            }
            if (simularFallo) {
                throw new SQLException("Fallo simulado al guardar el material del proyecto.", "45000", 9999);
            }
            try (PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO proyecto_material (id_proyecto, id_material, cantidad) VALUES (?, ?, ?)")) {
                for (Map.Entry<Integer, Integer> e : nuevas.entrySet()) {
                    if (e.getValue() <= 0) {
                        continue;
                    }
                    ins.setInt(1, idProyecto);
                    ins.setInt(2, e.getKey());
                    ins.setBigDecimal(3, BigDecimal.valueOf(e.getValue()));
                    ins.executeUpdate();
                }
            }
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Proyecto ID=" + idProyecto + " con "
                    + nuevas.size() + " material(es); existencias del almacén ajustadas");
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, op,
                    "No se pudo guardar el material del proyecto. Se revirtieron los cambios "
                    + "(el almacén quedó como estaba).", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, op);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }
}
