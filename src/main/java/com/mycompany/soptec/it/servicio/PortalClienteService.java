package com.mycompany.soptec.it.servicio;

import com.mycompany.soptec.it.dao.PortalClienteDao;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.PedidoCliente;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.util.AppLogger;
import com.mycompany.soptec.it.util.Validador;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reglas del Portal del cliente. Todo está pensado para que una persona que casi no usa
 * computadora pueda agendar, comprar y revisar sus servicios: pocos datos y mensajes claros.
 */
public class PortalClienteService {

    private static final String MODULO = "Portal cliente";

    /** Tipos de servicio que el cliente puede pedir. */
    public static final String SERV_REPARACION = "Reparación de equipo";
    public static final String SERV_INSTALACION = "Instalación";
    public static final String SERV_MANTENIMIENTO = "Mantenimiento";
    public static final String SERV_COTIZACION = "Cotización / asesoría";

    /** Horario de atención: lunes a sábado, de 9:00 a 17:00 (una cita por hora). */
    public static final int HORA_INICIO = 9;
    public static final int HORA_FIN = 17;

    private static final Pattern PATRON_SERVICIO = Pattern.compile("Servicio: ([^·]+)");

    private final PortalClienteDao dao = new PortalClienteDao();

    // ---------------------------------------------------------------- acceso

    /** Valida que el teléfono tenga 10 números y lo regresa limpio (solo dígitos). */
    public String normalizarTelefono(String telefono) throws ValidacionException {
        String digitos = PortalClienteDao.soloDigitos(telefono);
        if (digitos.length() > 10 && digitos.startsWith("52")) {
            digitos = digitos.substring(digitos.length() - 10);
        }
        if (digitos.length() != 10) {
            throw new ValidacionException(MODULO, "VALIDAR_TELEFONO",
                    "Tu número debe tener 10 dígitos. Escribiste " + digitos.length() + ".");
        }
        return digitos;
    }

    public Cliente buscarPorTelefono(String telefono) throws SoptecException {
        return dao.buscarPorTelefono(normalizarTelefono(telefono));
    }

    public Cliente registrar(String nombre, String telefono, String correo) throws SoptecException {
        String op = "REGISTRAR_CLIENTE";
        try {
            String tel = normalizarTelefono(telefono);
            String nom = Validador.requerido(nombre, "Tu nombre", MODULO, op);
            Validador.longitudMaxima(nom, 100, "Tu nombre", MODULO, op);
            if (nom.length() < 3) {
                throw new ValidacionException(MODULO, op, "Escribe tu nombre completo, por favor.");
            }
            String cor = Validador.opcional(correo);
            if (cor != null) {
                cor = Validador.validarCorreo(cor, MODULO, op);
            }
            if (dao.buscarPorTelefono(tel) != null) {
                throw new ValidacionException(MODULO, op,
                        "Ese número ya está registrado. Regresa y entra solo con tu número.");
            }
            Cliente c = new Cliente();
            c.setNombreCliente(nom);
            c.setNombre(nom);
            c.setCorreo(cor);
            return dao.registrar(c, tel);
        } catch (ValidacionException ex) {
            AppLogger.registrarExcepcion(MODULO, "PortalClienteService", op, ex);
            throw ex;
        }
    }

    // ---------------------------------------------------------------- citas

    /** Días que se muestran para elegir: los próximos 10 días hábiles (sin domingos). */
    public List<LocalDate> diasDisponibles() {
        List<LocalDate> dias = new ArrayList<>();
        LocalDate d = LocalDate.now();
        // Si ya pasó la última hora de hoy, empezamos mañana.
        if (LocalTime.now().getHour() >= HORA_FIN) {
            d = d.plusDays(1);
        }
        while (dias.size() < 10) {
            if (d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                dias.add(d);
            }
            d = d.plusDays(1);
        }
        return dias;
    }

    /** Todas las horas del horario de atención. */
    public List<LocalTime> horasDelDia() {
        List<LocalTime> horas = new ArrayList<>();
        for (int h = HORA_INICIO; h <= HORA_FIN; h++) {
            horas.add(LocalTime.of(h, 0));
        }
        return horas;
    }

    public Set<LocalTime> horasOcupadas(LocalDate fecha) throws SoptecException {
        return dao.horasOcupadas(fecha);
    }

    /** Una hora ya no se puede elegir si está ocupada o si ya pasó (cuando es hoy). */
    public boolean horaDisponible(LocalDate fecha, LocalTime hora, Set<LocalTime> ocupadas) {
        if (ocupadas.contains(hora)) {
            return false;
        }
        return LocalDateTime.of(fecha, hora).isAfter(LocalDateTime.now().plusMinutes(30));
    }

    /**
     * Guarda la cita/instalación del cliente con estado "Solicitada".
     * Si es instalación, el domicilio es obligatorio y se guarda también en su ficha.
     */
    public int solicitarCita(Cliente cliente, String servicio, LocalDate fecha, LocalTime hora,
            String calle, String numero, String colonia, String nota) throws SoptecException {
        String op = "SOLICITAR_CITA";
        try {
            if (cliente == null) {
                throw new ValidacionException(MODULO, op, "Vuelve a entrar con tu número de teléfono.");
            }
            if (servicio == null || servicio.isBlank()) {
                throw new ValidacionException(MODULO, op, "Elige qué servicio necesitas.");
            }
            if (fecha == null || hora == null) {
                throw new ValidacionException(MODULO, op, "Elige el día y la hora de tu cita.");
            }
            if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
                throw new ValidacionException(MODULO, op, "Los domingos no damos servicio. Elige otro día.");
            }
            String cal = Validador.opcional(calle);
            String num = Validador.opcional(numero);
            String col = Validador.opcional(colonia);
            Validador.longitudMaxima(cal, 100, "Calle", MODULO, op);
            Validador.longitudMaxima(num, 20, "Número", MODULO, op);
            Validador.longitudMaxima(col, 100, "Colonia", MODULO, op);
            boolean esInstalacion = SERV_INSTALACION.equals(servicio);
            if (esInstalacion && (cal == null || num == null || col == null)) {
                throw new ValidacionException(MODULO, op,
                        "Para la instalación necesitamos tu domicilio: calle, número y colonia.");
            }
            String not = Validador.opcional(nota);
            Validador.longitudMaxima(not, 500, "Nota", MODULO, op);

            StringBuilder obs = new StringBuilder(PortalClienteDao.PREFIJO_PORTAL)
                    .append(" Servicio: ").append(servicio)
                    .append(" · Tel: ").append(cliente.getTelefono() != null ? cliente.getTelefono() : "-");
            if (cal != null || col != null) {
                obs.append(" · Domicilio: ").append(cal != null ? cal : "").append(num != null ? " #" + num : "")
                        .append(col != null ? ", Col. " + col : "");
            }
            if (not != null) {
                obs.append(" · Nota: ").append(not);
            }
            if (cal != null && num != null && col != null) {
                dao.actualizarDomicilio(cliente.getIdCliente(), cal, num, col);
                cliente.setCalle(cal);
                cliente.setNumero(num);
                cliente.setColonia(col);
            }
            return dao.solicitarCita(cliente.getIdCliente(), fecha, hora, obs.toString());
        } catch (ValidacionException ex) {
            AppLogger.registrarExcepcion(MODULO, "PortalClienteService", op, ex);
            throw ex;
        }
    }

    public List<Cita> citasDeCliente(int idCliente) throws SoptecException {
        return dao.citasDeCliente(idCliente);
    }

    public boolean cancelarCita(int idCita, int idCliente) throws SoptecException {
        return dao.cancelarCitaDeCliente(idCita, idCliente);
    }

    public List<Cita> citasSolicitadas() throws SoptecException {
        return dao.citasSolicitadas();
    }

    public boolean cambiarEstadoCita(int idCita, String estado) throws SoptecException {
        boolean ok = dao.cambiarEstadoCita(idCita, estado);
        AppLogger.registrarInfo(MODULO, "PortalClienteService", "CAMBIAR_ESTADO_CITA",
                "Cita ID=" + idCita + " ahora está: " + estado);
        return ok;
    }

    /** Próxima cita vigente del cliente (para mostrarla en su inicio) o null. */
    public Cita proximaCita(int idCliente) throws SoptecException {
        Cita proxima = null;
        LocalDateTime ahora = LocalDateTime.now();
        for (Cita c : dao.citasDeCliente(idCliente)) {
            if (c.getFechaCita() == null || c.getHoraCita() == null) {
                continue;
            }
            String e = c.getEstadoCita() == null ? "" : c.getEstadoCita();
            if (e.equalsIgnoreCase("Cancelada") || e.equalsIgnoreCase("Atendida")) {
                continue;
            }
            LocalDateTime cuando = LocalDateTime.of(c.getFechaCita(), c.getHoraCita());
            if (cuando.isAfter(ahora) && (proxima == null
                    || cuando.isBefore(LocalDateTime.of(proxima.getFechaCita(), proxima.getHoraCita())))) {
                proxima = c;
            }
        }
        return proxima;
    }

    /** Saca el tipo de servicio de las observaciones de la cita ("Cita" si no viene del portal). */
    public static String servicioDeCita(Cita c) {
        if (c == null || c.getObservaciones() == null) {
            return "Cita";
        }
        Matcher m = PATRON_SERVICIO.matcher(c.getObservaciones());
        return m.find() ? m.group(1).trim() : "Cita";
    }

    // ---------------------------------------------------------------- tienda

    public List<Material> materialesDisponibles() throws SoptecException {
        return dao.materialesDisponibles();
    }

    public int crearPedido(Cliente cliente, Map<Integer, Integer> carrito) throws SoptecException {
        if (cliente == null) {
            throw new ValidacionException(MODULO, "CREAR_PEDIDO", "Vuelve a entrar con tu número de teléfono.");
        }
        return dao.crearPedido(cliente.getIdCliente(), carrito);
    }

    public List<PedidoCliente> pedidosDeCliente(int idCliente) throws SoptecException {
        return dao.pedidosDeCliente(idCliente);
    }

    public boolean cancelarPedidoDeCliente(int idPedido, int idCliente) throws SoptecException {
        return dao.cancelarPedido(idPedido, idCliente);
    }

    public List<PedidoCliente> listarPedidos(boolean soloPendientes) throws SoptecException {
        return dao.listarPedidos(soloPendientes);
    }

    public boolean cambiarEstadoPedido(int idPedido, String estado) throws SoptecException {
        if (PedidoCliente.CANCELADO.equals(estado)) {
            return dao.cancelarPedido(idPedido, null);
        }
        return dao.cambiarEstadoPedido(idPedido, estado);
    }

    // ---------------------------------------------------------------- seguimiento

    public List<Proyecto> proyectosDeCliente(int idCliente) throws SoptecException {
        return dao.proyectosDeCliente(idCliente);
    }

    // ---------------------------------------------------------------- textos amigables

    /** Estado de una cita explicado en palabras sencillas para el cliente. */
    public static String estadoCitaParaCliente(String estado) {
        String e = estado == null ? "" : estado.toLowerCase();
        if (e.startsWith("solicit")) {
            return "Esperando confirmación";
        }
        if (e.startsWith("program") || e.startsWith("confirm")) {
            return "Confirmada";
        }
        if (e.startsWith("atend")) {
            return "Ya atendida";
        }
        if (e.startsWith("cancel")) {
            return "Cancelada";
        }
        return estado == null ? "Sin estado" : estado;
    }

    /** Estado de un pedido explicado en palabras sencillas para el cliente. */
    public static String estadoPedidoParaCliente(String estado) {
        if (PedidoCliente.APARTADO.equalsIgnoreCase(estado)) {
            return "Lo estamos preparando";
        }
        if (PedidoCliente.LISTO.equalsIgnoreCase(estado)) {
            return "¡Listo para recoger!";
        }
        if (PedidoCliente.ENTREGADO.equalsIgnoreCase(estado)) {
            return "Entregado";
        }
        if (PedidoCliente.CANCELADO.equalsIgnoreCase(estado)) {
            return "Cancelado";
        }
        return estado == null ? "Sin estado" : estado;
    }

    /** Clase CSS del badge según el estado (verde = bien, amarillo = en espera, rojo = cancelado). */
    public static String claseBadge(String estado) {
        String e = estado == null ? "" : estado.toLowerCase();
        if (e.contains("cancel")) {
            return "badge-peligro";
        }
        if (e.contains("solicit") || e.contains("apartad") || e.contains("planead") || e.contains("revisi")) {
            return "badge-alerta";
        }
        if (e.contains("listo") || e.contains("confirm") || e.contains("program") || e.contains("finaliz")) {
            return "badge-exito";
        }
        if (e.contains("proceso") || e.contains("curso")) {
            return "badge-info";
        }
        return "badge-neutro";
    }
}
