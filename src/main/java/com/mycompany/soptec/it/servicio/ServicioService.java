package com.mycompany.soptec.it.servicio;

import com.mycompany.soptec.it.dao.ServicioDao;
import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Servicio;
import com.mycompany.soptec.it.util.AppLogger;
import com.mycompany.soptec.it.util.Validador;
import java.util.List;

/**
 * Reglas del catálogo de servicios.
 */
public class ServicioService {

    private static final String MODULO = "Servicios";
    private static final String PROCESO = "ServicioService";
    private final ServicioDao dao = new ServicioDao();

    public Servicio crear(Servicio s) throws SoptecException {
        String op = "CREAR_SERVICIO";
        try {
            validar(s, op);
            if (dao.existeNombre(s.getNombreServicio(), 0)) {
                throw new ValidacionException(MODULO, op,
                        "Ya existe un servicio llamado '" + s.getNombreServicio() + "'.");
            }
            s.setActivo(true);
            Servicio creado = dao.crear(s);
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Servicio creado ID=" + creado.getIdServicio());
            return creado;
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    public Servicio consultarPorId(int id) throws SoptecException {
        if (id <= 0) {
            throw registrar(new ValidacionException(MODULO, "CONSULTAR_SERVICIO", "El ID del servicio debe ser mayor a cero."));
        }
        return dao.obtenerPorId(id);
    }

    public List<Servicio> listar(boolean incluirInactivos) throws SoptecException {
        return dao.listar(incluirInactivos);
    }

    public List<Servicio> buscar(String criterio, boolean incluirInactivos) throws SoptecException {
        return dao.buscar(criterio, incluirInactivos);
    }

    public void actualizar(Servicio s) throws SoptecException {
        String op = "ACTUALIZAR_SERVICIO";
        try {
            if (s == null || s.getIdServicio() <= 0) {
                throw new ValidacionException(MODULO, op, "Selecciona primero el servicio que deseas actualizar.");
            }
            validar(s, op);
            if (dao.existeNombre(s.getNombreServicio(), s.getIdServicio())) {
                throw new ValidacionException(MODULO, op,
                        "Ya existe otro servicio llamado '" + s.getNombreServicio() + "'.");
            }
            if (!dao.actualizar(s)) {
                throw new ValidacionException(MODULO, op, "El servicio ya no existe.");
            }
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Servicio actualizado ID=" + s.getIdServicio());
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    public void desactivar(int id) throws SoptecException {
        cambiarEstado(id, false);
    }

    public void reactivar(int id) throws SoptecException {
        cambiarEstado(id, true);
    }

    public void eliminarFisico(int id) throws SoptecException {
        String op = "ELIMINAR_FISICO_SERVICIO";
        try {
            if (id <= 0) {
                throw new ValidacionException(MODULO, op, "Selecciona un servicio.");
            }
            int usos = dao.contarUsos(id);
            if (usos > 0) {
                throw new IntegridadReferencialException(MODULO, op, "No se puede eliminar el servicio porque aparece en "
                        + usos + " cita(s) o venta(s). Puedes desactivarlo para que ya no se ofrezca.");
            }
            if (!dao.eliminarFisico(id)) {
                throw new ValidacionException(MODULO, op, "No se encontró el servicio.");
            }
            AppLogger.registrarInfo(MODULO, PROCESO, op, "Servicio eliminado ID=" + id);
        } catch (ValidacionException | IntegridadReferencialException ex) {
            AppLogger.registrarExcepcion(MODULO, PROCESO, op, ex);
            throw ex;
        }
    }

    private void cambiarEstado(int id, boolean activo) throws SoptecException {
        String op = activo ? "REACTIVAR_SERVICIO" : "DESACTIVAR_SERVICIO";
        if (id <= 0) {
            throw registrar(new ValidacionException(MODULO, op, "Selecciona un servicio."));
        }
        dao.cambiarEstadoActivo(id, activo);
        AppLogger.registrarInfo(MODULO, PROCESO, op, "Servicio ID=" + id + (activo ? " reactivado" : " desactivado"));
    }

    private void validar(Servicio s, String op) throws ValidacionException {
        if (s == null) {
            throw new ValidacionException(MODULO, op, "Los datos del servicio son obligatorios.");
        }
        s.setNombreServicio(Validador.requerido(s.getNombreServicio(), "Nombre del servicio", MODULO, op));
        Validador.longitudMaxima(s.getNombreServicio(), 100, "Nombre del servicio", MODULO, op);
        s.setDescripcion(Validador.opcional(s.getDescripcion()));
        Validador.longitudMaxima(s.getDescripcion(), 255, "Descripción", MODULO, op);
        if (s.getPrecio() == null || s.getPrecio().signum() <= 0) {
            throw new ValidacionException(MODULO, op, "El precio del servicio debe ser mayor a cero.");
        }
    }

    private SoptecException registrar(SoptecException ex) {
        AppLogger.registrarExcepcion(MODULO, PROCESO, ex.getOperacion(), ex);
        return ex;
    }
}
