package com.mycompany.soptec.it.servicio;

import com.mycompany.soptec.it.dao.CascadaDao;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.util.AppLogger;

/**
 * Activar / desactivar en cascada sin tener que ir a otras áreas: por ejemplo, desactivar un cliente
 * desactiva también sus teléfonos, citas, proyectos y levantamientos en un solo paso.
 * Los métodos simples de cada servicio (que bloquean si hay registros activos) se conservan para las pruebas.
 */
public class CascadaService {

    private static final String MODULO = "Cascada";
    private final CascadaDao dao = new CascadaDao();

    public CascadaDao.Resumen activosDeCliente(int idCliente) throws SoptecException {
        validarId(idCliente, "cliente");
        return dao.contarDeCliente(idCliente, true);
    }

    public CascadaDao.Resumen inactivosDeCliente(int idCliente) throws SoptecException {
        validarId(idCliente, "cliente");
        return dao.contarDeCliente(idCliente, false);
    }

    public CascadaDao.Resumen activosDeProyecto(int idProyecto) throws SoptecException {
        validarId(idProyecto, "proyecto");
        return dao.contarDeProyecto(idProyecto, true);
    }

    public CascadaDao.Resumen inactivosDeProyecto(int idProyecto) throws SoptecException {
        validarId(idProyecto, "proyecto");
        return dao.contarDeProyecto(idProyecto, false);
    }

    public int proyectosDeEmpleado(int idEmpleado) throws SoptecException {
        validarId(idEmpleado, "empleado");
        return dao.contarProyectosDeEmpleado(idEmpleado);
    }

    public boolean clienteInactivo(int idCliente) throws SoptecException {
        return Boolean.FALSE.equals(dao.clienteActivo(idCliente));
    }

    /** ¿El proyecto, o el cliente de ese proyecto, está inactivo? */
    public boolean proyectoOClienteInactivo(int idProyecto) throws SoptecException {
        if (Boolean.FALSE.equals(dao.proyectoActivo(idProyecto))) {
            return true;
        }
        int idCliente = dao.clienteDeProyecto(idProyecto);
        return idCliente > 0 && Boolean.FALSE.equals(dao.clienteActivo(idCliente));
    }

    public boolean proyectoDeClienteInactivo(int idProyecto) throws SoptecException {
        int idCliente = dao.clienteDeProyecto(idProyecto);
        return idCliente > 0 && Boolean.FALSE.equals(dao.clienteActivo(idCliente));
    }

    public void desactivarCliente(int idCliente, boolean conRelacionados) throws SoptecException {
        validarId(idCliente, "cliente");
        dao.cambiarCliente(idCliente, false, conRelacionados);
    }

    public void reactivarCliente(int idCliente, boolean conRelacionados) throws SoptecException {
        validarId(idCliente, "cliente");
        dao.cambiarCliente(idCliente, true, conRelacionados);
    }

    public void desactivarProyecto(int idProyecto, boolean conLevantamientos) throws SoptecException {
        validarId(idProyecto, "proyecto");
        dao.cambiarProyecto(idProyecto, false, conLevantamientos, false);
    }

    public void reactivarProyecto(int idProyecto, boolean conLevantamientos, boolean reactivarCliente) throws SoptecException {
        validarId(idProyecto, "proyecto");
        dao.cambiarProyecto(idProyecto, true, conLevantamientos, reactivarCliente);
    }

    public void reactivarTelefonoYCliente(int idCliente, String telefono) throws SoptecException {
        validarId(idCliente, "cliente");
        dao.reactivarTelefonoYCliente(idCliente, telefono);
    }

    public void reactivarCitaYCliente(int idCita) throws SoptecException {
        validarId(idCita, "cita");
        dao.reactivarCitaYCliente(idCita);
    }

    public void reactivarLevantamientoYProyecto(int idLevantamiento) throws SoptecException {
        validarId(idLevantamiento, "levantamiento");
        dao.reactivarLevantamientoYProyecto(idLevantamiento);
    }

    public void desactivarEmpleadoYQuitarDeProyectos(int idEmpleado) throws SoptecException {
        validarId(idEmpleado, "empleado");
        dao.desactivarEmpleadoYQuitarDeProyectos(idEmpleado);
    }

    private void validarId(int id, String que) throws ValidacionException {
        if (id <= 0) {
            ValidacionException ex = new ValidacionException(MODULO, "CASCADA", "Selecciona primero el " + que + ".");
            AppLogger.registrarExcepcion(MODULO, "CascadaService", "CASCADA", ex);
            throw ex;
        }
    }
}
