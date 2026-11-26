package com.mycompany.soptec.it.servicio;

import com.mycompany.soptec.it.dao.MovimientoAlmacenDao;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.MovimientoAlmacen;
import com.mycompany.soptec.it.util.AppLogger;
import com.mycompany.soptec.it.util.Validador;
import java.time.LocalDate;
import java.util.List;

/**
 * Reglas de negocio del historial de movimientos del almacén.
 */
public class MovimientoAlmacenService {

    private static final String MODULO = "Materiales";
    private final MovimientoAlmacenDao dao = new MovimientoAlmacenDao();

    public List<MovimientoAlmacen> listar(LocalDate fecha) throws SoptecException {
        return dao.listar(fecha);
    }

    public MovimientoAlmacen registrar(MovimientoAlmacen mv) throws SoptecException {
        validar(mv, "REGISTRAR_MOVIMIENTO");
        return dao.registrar(mv);
    }

    public void actualizar(MovimientoAlmacen mv) throws SoptecException {
        validar(mv, "ACTUALIZAR_MOVIMIENTO");
        if (mv.getIdMovimiento() <= 0) {
            throw registrar(new ValidacionException(MODULO, "ACTUALIZAR_MOVIMIENTO",
                    "Selecciona primero el movimiento que quieres modificar."));
        }
        dao.actualizar(mv);
    }

    public void eliminar(int idMovimiento) throws SoptecException {
        if (idMovimiento <= 0) {
            throw registrar(new ValidacionException(MODULO, "ELIMINAR_MOVIMIENTO",
                    "Selecciona primero el movimiento que quieres eliminar."));
        }
        dao.eliminar(idMovimiento);
    }

    private void validar(MovimientoAlmacen mv, String op) throws ValidacionException {
        try {
            if (mv == null || mv.getIdMaterial() <= 0) {
                throw new ValidacionException(MODULO, op, "Selecciona el material del movimiento.");
            }
            if (mv.getCantidad() <= 0) {
                throw new ValidacionException(MODULO, op, "La cantidad debe ser mayor a cero.");
            }
            if (mv.getFecha() == null) {
                throw new ValidacionException(MODULO, op, "Indica la fecha del movimiento.");
            }
            if (mv.getFecha().isAfter(LocalDate.now())) {
                throw new ValidacionException(MODULO, op, "La fecha del movimiento no puede ser futura.");
            }
            if (!MovimientoAlmacen.ENTRADA.equalsIgnoreCase(mv.getTipo())
                    && !MovimientoAlmacen.SALIDA.equalsIgnoreCase(mv.getTipo())) {
                throw new ValidacionException(MODULO, op, "Indica si el movimiento es Entrada o Salida.");
            }
            Validador.longitudMaxima(mv.getReferencia(), 100, "Referencia", MODULO, op);
        } catch (ValidacionException ex) {
            throw registrar(ex);
        }
    }

    private ValidacionException registrar(ValidacionException ex) {
        AppLogger.registrarExcepcion(MODULO, "MovimientoAlmacenService", ex.getOperacion(), ex);
        return ex;
    }
}
