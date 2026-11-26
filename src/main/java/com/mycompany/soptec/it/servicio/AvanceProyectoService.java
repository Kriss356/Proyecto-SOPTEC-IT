package com.mycompany.soptec.it.servicio;

import com.mycompany.soptec.it.dao.AvanceProyectoDao;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.AvanceProyecto;
import com.mycompany.soptec.it.util.AppLogger;
import com.mycompany.soptec.it.util.Validador;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reglas de negocio del historial de avances de obra (Instalaciones).
 */
public class AvanceProyectoService {

    private static final String MODULO = "Instalaciones";
    private final AvanceProyectoDao dao = new AvanceProyectoDao();

    public List<AvanceProyecto> listarPorProyecto(int idProyecto) throws SoptecException {
        return dao.listarPorProyecto(idProyecto);
    }

    public Map<Integer, Integer> ultimoPorcentajePorProyecto() throws SoptecException {
        return dao.ultimoPorcentajePorProyecto();
    }

    public AvanceProyecto crear(AvanceProyecto a) throws SoptecException {
        validar(a, "CREAR_AVANCE");
        validarQueNoRetroceda(a, "CREAR_AVANCE");
        return dao.crear(a);
    }

    public void actualizar(AvanceProyecto a) throws SoptecException {
        validar(a, "ACTUALIZAR_AVANCE");
        validarQueNoRetroceda(a, "ACTUALIZAR_AVANCE");
        if (a.getIdAvance() <= 0) {
            throw registrar(new ValidacionException(MODULO, "ACTUALIZAR_AVANCE",
                    "Selecciona primero el avance que quieres modificar."));
        }
        if (!dao.actualizar(a)) {
            throw registrar(new ValidacionException(MODULO, "ACTUALIZAR_AVANCE",
                    "Ese avance ya no existe. Refresca la pantalla."));
        }
    }

    public void eliminar(int idAvance) throws SoptecException {
        if (idAvance <= 0) {
            throw registrar(new ValidacionException(MODULO, "ELIMINAR_AVANCE",
                    "Selecciona primero el avance que quieres eliminar."));
        }
        dao.eliminar(idAvance);
    }

    /**
     * Límites del porcentaje para un avance según su lugar en el historial (por fecha):
     * no puede ser menor que el avance anterior ni mayor que el siguiente. Así la obra nunca "retrocede".
     *
     * @param historial avances del proyecto
     * @param idAvance  id del avance que se edita (0 si es nuevo)
     * @param fecha     fecha del avance
     * @return {mínimo, máximo, índice del anterior o -1, índice del siguiente o -1}
     */
    public static int[] limites(List<AvanceProyecto> historial, int idAvance, LocalDate fecha) {
        int minimo = 0;
        int maximo = 100;
        int anterior = -1;
        int siguiente = -1;
        int idPropio = idAvance > 0 ? idAvance : Integer.MAX_VALUE;
        LocalDate f = fecha != null ? fecha : LocalDate.now();
        for (int i = 0; i < historial.size(); i++) {
            AvanceProyecto h = historial.get(i);
            if (h.getIdAvance() == idAvance || h.getFecha() == null) {
                continue;
            }
            boolean antes = h.getFecha().isBefore(f) || (h.getFecha().isEqual(f) && h.getIdAvance() < idPropio);
            if (antes && h.getPorcentaje() >= minimo) {
                minimo = h.getPorcentaje();
                anterior = i;
            } else if (!antes && h.getPorcentaje() <= maximo) {
                maximo = h.getPorcentaje();
                siguiente = i;
            }
        }
        return new int[]{minimo, Math.max(minimo, maximo), anterior, siguiente};
    }

    private void validarQueNoRetroceda(AvanceProyecto a, String op) throws SoptecException {
        List<AvanceProyecto> historial = dao.listarPorProyecto(a.getIdProyecto());
        int[] lim = limites(historial, a.getIdAvance(), a.getFecha());
        java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (a.getPorcentaje() < lim[0]) {
            AvanceProyecto ant = historial.get(lim[2]);
            throw registrar(new ValidacionException(MODULO, op, "El avance no puede ir hacia atrás: el avance del "
                    + ant.getFecha().format(df) + " ya iba en " + lim[0] + " %. Captura " + lim[0] + " % o más."));
        }
        if (a.getPorcentaje() > lim[1]) {
            AvanceProyecto sig = historial.get(lim[3]);
            throw registrar(new ValidacionException(MODULO, op, "Este avance no puede ser mayor que el avance siguiente ("
                    + sig.getFecha().format(df) + ", " + lim[1] + " %). Captura " + lim[1] + " % o menos."));
        }
    }

    private void validar(AvanceProyecto a, String op) throws ValidacionException {
        try {
            if (a == null || a.getIdProyecto() <= 0) {
                throw new ValidacionException(MODULO, op, "Selecciona el proyecto del avance.");
            }
            if (a.getFecha() == null) {
                throw new ValidacionException(MODULO, op, "Indica la fecha del avance.");
            }
            if (a.getFecha().isAfter(LocalDate.now())) {
                throw new ValidacionException(MODULO, op, "La fecha del avance no puede ser futura.");
            }
            a.setTarea(Validador.requerido(a.getTarea(), "Tarea realizada", MODULO, op));
            Validador.longitudMaxima(a.getTarea(), 100, "Tarea realizada", MODULO, op);
            Validador.longitudMaxima(a.getMaterialUsado(), 255, "Material utilizado", MODULO, op);
            if (a.getHoras() < 0 || a.getHoras() > 24) {
                throw new ValidacionException(MODULO, op, "Las horas trabajadas deben estar entre 0 y 24.");
            }
            if (a.getPorcentaje() < 0 || a.getPorcentaje() > 100) {
                throw new ValidacionException(MODULO, op, "El porcentaje de avance debe estar entre 0 y 100.");
            }
        } catch (ValidacionException ex) {
            throw registrar(ex);
        }
    }

    private ValidacionException registrar(ValidacionException ex) {
        AppLogger.registrarExcepcion(MODULO, "AvanceProyectoService", ex.getOperacion(), ex);
        return ex;
    }
}
