package com.mycompany.soptec.it.servicio;

import com.mycompany.soptec.it.dao.MaterialDao;
import com.mycompany.soptec.it.dao.OrdenCompraDao;
import com.mycompany.soptec.it.dao.ProveedorDao;
import com.mycompany.soptec.it.dao.ProyectoDao;
import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.OrdenCompra;
import com.mycompany.soptec.it.modelo.PartidaMaterial;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.util.AppLogger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas de las órdenes de compra y sus partidas.
 */
public class OrdenCompraService {

    private static final String MODULO = "OrdenesCompra";
    private static final String PROCESO = "OrdenCompraService";

    private final OrdenCompraDao dao = new OrdenCompraDao();
    private final ProveedorDao proveedorDao = new ProveedorDao();
    private final ProyectoDao proyectoDao = new ProyectoDao();
    private final MaterialDao materialDao = new MaterialDao();

    public OrdenCompra crear(OrdenCompra o) throws SoptecException {
        return crear(o, false);
    }

    public OrdenCompra crear(OrdenCompra o, boolean simularFallo) throws SoptecException {
        String op = "CREAR_ORDEN_COMPRA";
        try {
            validar(o, op);
            return dao.crear(o, simularFallo);
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    public void actualizar(OrdenCompra o) throws SoptecException {
        String op = "ACTUALIZAR_ORDEN_COMPRA";
        try {
            if (o == null || o.getIdOrdenCompra() <= 0) {
                throw new ValidacionException(MODULO, op, "Selecciona primero la orden que deseas modificar.");
            }
            validar(o, op);
            if (!dao.actualizar(o)) {
                throw new ValidacionException(MODULO, op, "La orden de compra ya no existe.");
            }
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    public void recibir(int idOrden) throws SoptecException {
        try {
            dao.recibir(idOrden);
        } catch (ValidacionException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, "RECIBIR_ORDEN_COMPRA", ex);
            throw ex;
        }
    }

    public void cancelar(int idOrden) throws SoptecException {
        if (!dao.cancelar(idOrden)) {
            ValidacionException ex = new ValidacionException(MODULO, "CANCELAR_ORDEN_COMPRA",
                    "Solo se pueden cancelar órdenes en estado 'Solicitada'.");
            AppLogger.registrarExcepcion(MODULO, PROCESO, "CANCELAR_ORDEN_COMPRA", ex);
            throw ex;
        }
        AppLogger.registrarInfo(MODULO, PROCESO, "CANCELAR_ORDEN_COMPRA", "Orden ID=" + idOrden + " cancelada");
    }

    public void eliminarFisico(int idOrden) throws SoptecException {
        String op = "ELIMINAR_ORDEN_COMPRA";
        try {
            OrdenCompra o = dao.obtenerPorId(idOrden);
            if (o == null) {
                throw new ValidacionException(MODULO, op, "La orden de compra ya no existe.");
            }
            if (OrdenCompra.RECIBIDA.equalsIgnoreCase(o.getEstado())) {
                throw new IntegridadReferencialException(MODULO, op, "La orden " + o.getFolio()
                        + " ya se recibió y su material entró al almacén; no se puede eliminar.");
            }
            dao.eliminarFisico(idOrden);
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Orden " + o.getFolio() + " eliminada con sus partidas");
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    public OrdenCompra consultarPorId(int idOrden) throws SoptecException {
        return dao.obtenerPorId(idOrden);
    }

    public List<OrdenCompra> listar(String criterio) throws SoptecException {
        return dao.listar(criterio);
    }

    private void validar(OrdenCompra o, String op) throws SoptecException {
        if (o == null) {
            throw new ValidacionException(MODULO, op, "Los datos de la orden son obligatorios.");
        }
        if (o.getIdProveedor() == null) {
            throw new ValidacionException(MODULO, op, "Selecciona el proveedor de la orden.");
        }
        Proveedor pr = proveedorDao.obtenerPorId(o.getIdProveedor());
        if (pr == null) {
            throw new IntegridadReferencialException(MODULO, op, "El proveedor seleccionado ya no existe.");
        }
        if (!pr.isActivo()) {
            throw new ValidacionException(MODULO, op, "El proveedor '" + pr.getNombreProveedor() + "' está inactivo.");
        }
        if (o.getIdProyecto() != null && proyectoDao.obtenerPorId(o.getIdProyecto()) == null) {
            throw new IntegridadReferencialException(MODULO, op, "El proyecto seleccionado ya no existe.");
        }
        if (o.getFechaOrden() == null) {
            throw new ValidacionException(MODULO, op, "La fecha de la orden es obligatoria.");
        }
        if (o.getPartidas().isEmpty()) {
            throw new ValidacionException(MODULO, op, "Agrega al menos un material a la orden.");
        }
        Set<Integer> vistos = new HashSet<>();
        for (PartidaMaterial p : o.getPartidas()) {
            if (!vistos.add(p.getIdMaterial())) {
                throw new ValidacionException(MODULO, op, "El material '" + p.getDescripcionMaterial()
                        + "' está repetido en la orden.");
            }
            if (p.getCantidad() <= 0) {
                throw new ValidacionException(MODULO, op, "La cantidad de '" + p.getDescripcionMaterial()
                        + "' debe ser mayor a cero.");
            }
            Material m = materialDao.obtenerPorId(p.getIdMaterial());
            if (m == null) {
                throw new IntegridadReferencialException(MODULO, op, "El material '" + p.getDescripcionMaterial()
                        + "' ya no existe.");
            }
            p.setPrecioUnitario(m.getPrecioCosto());
        }
        o.calcularTotal();
    }
}
