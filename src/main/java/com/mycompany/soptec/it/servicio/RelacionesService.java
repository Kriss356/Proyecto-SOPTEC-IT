package com.mycompany.soptec.it.servicio;

import com.mycompany.soptec.it.dao.MaterialDao;
import com.mycompany.soptec.it.dao.MaterialProveedorDao;
import com.mycompany.soptec.it.dao.ProveedorDao;
import com.mycompany.soptec.it.dao.ProyectoDao;
import com.mycompany.soptec.it.dao.ProyectoEmpleadoDao;
import com.mycompany.soptec.it.dao.ProyectoMaterialDao;
import com.mycompany.soptec.it.dao.EmpleadoDao;
import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.PartidaMaterial;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.util.AppLogger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Reglas de las relaciones muchos a muchos que se administran "en bloque" desde un formulario:
 * <ul>
 *   <li>proyecto_empleado: personal asignado a un proyecto.</li>
 *   <li>material_proveedor: material que surte un proveedor.</li>
 *   <li>proyecto_material: material que se usa en un proyecto (descuenta del almacén).</li>
 * </ul>
 */
public class RelacionesService {

    private static final String PROCESO = "RelacionesService";

    private final ProyectoEmpleadoDao proyectoEmpleadoDao = new ProyectoEmpleadoDao();
    private final MaterialProveedorDao materialProveedorDao = new MaterialProveedorDao();
    private final ProyectoMaterialDao proyectoMaterialDao = new ProyectoMaterialDao();
    private final ProyectoDao proyectoDao = new ProyectoDao();
    private final EmpleadoDao empleadoDao = new EmpleadoDao();
    private final ProveedorDao proveedorDao = new ProveedorDao();
    private final MaterialDao materialDao = new MaterialDao();

    // ---------------------------------------------------------------- personal por proyecto
    public List<Empleado> listarPersonal(int idProyecto) throws SoptecException {
        return proyectoEmpleadoDao.listarEmpleados(idProyecto);
    }

    public void guardarPersonal(int idProyecto, List<Integer> idsEmpleados) throws SoptecException {
        guardarPersonal(idProyecto, idsEmpleados, false);
    }

    public void guardarPersonal(int idProyecto, List<Integer> idsEmpleados, boolean simularFallo) throws SoptecException {
        String modulo = "PersonalProyecto";
        String op = "GUARDAR_PERSONAL_PROYECTO";
        try {
            Proyecto p = proyectoActivo(idProyecto, modulo, op);
            List<Integer> ids = sinRepetidos(idsEmpleados);
            for (Integer id : ids) {
                Empleado e = empleadoDao.obtenerPorId(id);
                if (e == null) {
                    throw new IntegridadReferencialException(modulo, op, "Uno de los empleados seleccionados ya no existe.");
                }
                if (!e.isActivo()) {
                    throw new ValidacionException(modulo, op, "El empleado '" + e.getNombre()
                            + "' está inactivo y no se puede asignar al proyecto.");
                }
            }
            proyectoEmpleadoDao.reemplazarEquipo(p.getIdProyecto(), ids, simularFallo);
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(modulo, PROCESO, op, ex);
            throw ex;
        }
    }

    // ---------------------------------------------------------------- material por proveedor
    public List<Material> listarMaterialDeProveedor(int idProveedor) throws SoptecException {
        return materialProveedorDao.listarMateriales(idProveedor);
    }

    public List<Proveedor> listarProveedoresDeMaterial(int idMaterial) throws SoptecException {
        return materialProveedorDao.listarProveedores(idMaterial);
    }

    public void guardarMaterialDeProveedor(int idProveedor, List<Integer> idsMateriales) throws SoptecException {
        guardarMaterialDeProveedor(idProveedor, idsMateriales, false);
    }

    public void guardarMaterialDeProveedor(int idProveedor, List<Integer> idsMateriales, boolean simularFallo)
            throws SoptecException {
        String modulo = "MaterialProveedor";
        String op = "GUARDAR_MATERIAL_PROVEEDOR";
        try {
            if (idProveedor <= 0) {
                throw new ValidacionException(modulo, op, "Selecciona el proveedor.");
            }
            Proveedor pr = proveedorDao.obtenerPorId(idProveedor);
            if (pr == null) {
                throw new IntegridadReferencialException(modulo, op, "El proveedor seleccionado ya no existe.");
            }
            if (!pr.isActivo()) {
                throw new ValidacionException(modulo, op, "El proveedor '" + pr.getNombreProveedor() + "' está inactivo.");
            }
            List<Integer> ids = sinRepetidos(idsMateriales);
            for (Integer id : ids) {
                if (materialDao.obtenerPorId(id) == null) {
                    throw new IntegridadReferencialException(modulo, op, "Uno de los materiales seleccionados ya no existe.");
                }
            }
            materialProveedorDao.reemplazarMateriales(idProveedor, ids, simularFallo);
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(modulo, PROCESO, op, ex);
            throw ex;
        }
    }

    // ---------------------------------------------------------------- material por proyecto
    public List<PartidaMaterial> listarMaterialDeProyecto(int idProyecto) throws SoptecException {
        return proyectoMaterialDao.listar(idProyecto);
    }

    public void guardarMaterialDeProyecto(int idProyecto, List<PartidaMaterial> partidas) throws SoptecException {
        guardarMaterialDeProyecto(idProyecto, partidas, false);
    }

    public void guardarMaterialDeProyecto(int idProyecto, List<PartidaMaterial> partidas, boolean simularFallo)
            throws SoptecException {
        String modulo = "MaterialProyecto";
        String op = "GUARDAR_MATERIAL_PROYECTO";
        try {
            proyectoActivo(idProyecto, modulo, op);
            List<PartidaMaterial> lista = partidas == null ? new ArrayList<>() : partidas;
            for (PartidaMaterial p : lista) {
                if (p.getIdMaterial() <= 0) {
                    throw new ValidacionException(modulo, op, "Uno de los materiales no es válido.");
                }
                if (p.getCantidad() <= 0) {
                    throw new ValidacionException(modulo, op, "La cantidad de '" + p.getDescripcionMaterial()
                            + "' debe ser mayor a cero.");
                }
            }
            proyectoMaterialDao.guardar(idProyecto, lista, simularFallo);
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(modulo, PROCESO, op, ex);
            throw ex;
        }
    }

    // ---------------------------------------------------------------- auxiliares
    private Proyecto proyectoActivo(int idProyecto, String modulo, String op) throws SoptecException {
        if (idProyecto <= 0) {
            throw new ValidacionException(modulo, op, "Selecciona el proyecto.");
        }
        Proyecto p = proyectoDao.obtenerPorId(idProyecto);
        if (p == null) {
            throw new IntegridadReferencialException(modulo, op, "El proyecto seleccionado ya no existe.");
        }
        if (!p.isActivo()) {
            throw new ValidacionException(modulo, op, "El proyecto '" + p.getNombreProyecto() + "' está inactivo.");
        }
        return p;
    }

    private static List<Integer> sinRepetidos(List<Integer> ids) {
        return ids == null ? new ArrayList<>() : new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
