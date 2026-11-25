package com.mycompany.soptec.it.dao;

import com.mycompany.soptec.it.excepciones.OperacionBDException;
import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.PedidoCliente;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.util.AppLogger;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Acceso a datos del Portal del cliente.
 * <ul>
 *   <li>Identificación del cliente solo con su número de teléfono (tabla cliente_telefono).</li>
 *   <li>Registro rápido (cliente + teléfono en una sola transacción).</li>
 *   <li>Solicitud de citas e instalaciones (tabla cita con estado "Solicitada").</li>
 *   <li>Tienda: apartado de material del almacén (tablas pedido_cliente y pedido_cliente_material).</li>
 * </ul>
 * Las tablas de pedidos se crean solas la primera vez (CREATE TABLE IF NOT EXISTS).
 */
public class PortalClienteDao {

    private static final String MODULO = "Portal cliente";
    private static final String PROCESO = "PortalClienteDao";

    /** Estado con el que llegan las citas que pide el cliente; el personal las confirma después. */
    public static final String ESTADO_SOLICITADA = "Solicitada";
    /** Prefijo que se guarda en observaciones para que el personal sepa que la cita viene del portal. */
    public static final String PREFIJO_PORTAL = "[Portal del cliente]";

    private static volatile boolean tablasVerificadas = false;

    // =====================================================================
    // Esquema
    // =====================================================================

    /** Crea las tablas de pedidos del portal si todavía no existen en la base de datos. */
    public static void asegurarTablas() throws SoptecException {
        if (tablasVerificadas) {
            return;
        }
        String sqlPedido = "CREATE TABLE IF NOT EXISTS pedido_cliente ("
                + " id_pedido      INT(11) AUTO_INCREMENT PRIMARY KEY,"
                + " id_cliente     INT(11) NOT NULL,"
                + " fecha_pedido   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + " estado         VARCHAR(30) NOT NULL DEFAULT 'Apartado',"
                + " total          DECIMAL(12,2) NOT NULL DEFAULT 0.00,"
                + " observaciones  TEXT NULL,"
                + " activo         TINYINT(1) NOT NULL DEFAULT 1,"
                + " CONSTRAINT fk_pedcli_cliente FOREIGN KEY (id_cliente)"
                + "   REFERENCES cliente(id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE"
                + ") ENGINE=InnoDB";
        String sqlDetalle = "CREATE TABLE IF NOT EXISTS pedido_cliente_material ("
                + " id_pedido        INT(11) NOT NULL,"
                + " id_material      INT(11) NOT NULL,"
                + " cantidad         INT NOT NULL DEFAULT 1,"
                + " precio_unitario  DECIMAL(10,2) NOT NULL DEFAULT 0.00,"
                + " PRIMARY KEY (id_pedido, id_material),"
                + " CONSTRAINT fk_pcm_pedido FOREIGN KEY (id_pedido)"
                + "   REFERENCES pedido_cliente(id_pedido) ON DELETE CASCADE ON UPDATE CASCADE,"
                + " CONSTRAINT fk_pcm_material FOREIGN KEY (id_material)"
                + "   REFERENCES material(id_material) ON DELETE RESTRICT ON UPDATE CASCADE"
                + ") ENGINE=InnoDB";
        try (Connection con = Conexion.conectarStrict(MODULO, "ASEGURAR_TABLAS");
             Statement st = con.createStatement()) {
            st.execute(sqlPedido);
            st.execute(sqlDetalle);
            tablasVerificadas = true;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "ASEGURAR_TABLAS",
                    "No se pudieron preparar las tablas de pedidos del portal.", ex);
        }
    }

    // =====================================================================
    // Identificación y registro del cliente
    // =====================================================================

    /** Deja solo los números de un teléfono: "(55) 1234-5678" → "5512345678". */
    public static String soloDigitos(String telefono) {
        return telefono == null ? "" : telefono.replaceAll("\\D", "");
    }

    /** Compara dos teléfonos por sus últimos 10 dígitos (ignora lada +52, espacios y guiones). */
    public static boolean mismoTelefono(String a, String b) {
        String da = soloDigitos(a);
        String db = soloDigitos(b);
        if (da.length() < 10 || db.length() < 10) {
            return !da.isEmpty() && da.equals(db);
        }
        return da.substring(da.length() - 10).equals(db.substring(db.length() - 10));
    }

    /**
     * Busca un cliente activo que tenga registrado el teléfono indicado.
     * @return el cliente o null si el número no está registrado.
     */
    public Cliente buscarPorTelefono(String telefono) throws SoptecException {
        String sql = "SELECT c.id_cliente, c.rfc, c.nombre_cliente, c.correo, c.calle, c.numero, c.colonia, "
                + "c.estado, c.imagen, c.activo, ct.telefono "
                + "FROM cliente c INNER JOIN cliente_telefono ct ON ct.id_cliente = c.id_cliente "
                + "WHERE c.activo = 1 AND ct.activo = 1 "
                + "ORDER BY c.id_cliente ASC";
        try (Connection con = Conexion.conectarStrict(MODULO, "BUSCAR_POR_TELEFONO");
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (mismoTelefono(telefono, rs.getString("telefono"))) {
                    Cliente c = mapearCliente(rs);
                    c.setTelefono(rs.getString("telefono"));
                    return c;
                }
            }
            return null;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "BUSCAR_POR_TELEFONO",
                    "No se pudo buscar tu número en este momento.", ex);
        }
    }

    /** Registra un cliente nuevo con su teléfono en una sola transacción (si algo falla no se guarda nada). */
    public Cliente registrar(Cliente cliente, String telefono) throws SoptecException {
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, "REGISTRAR_CLIENTE");
            con.setAutoCommit(false);
            cliente.setActivo(true);
            new ClienteDao().crear(con, cliente);
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO cliente_telefono (id_cliente, telefono, activo) VALUES (?, ?, 1)")) {
                ps.setInt(1, cliente.getIdCliente());
                ps.setString(2, telefono);
                ps.executeUpdate();
            }
            con.commit();
            cliente.setTelefono(telefono);
            AppLogger.registrarInfo(MODULO, PROCESO, "REGISTRAR_CLIENTE",
                    "Cliente registrado desde el portal ID=" + cliente.getIdCliente());
            return cliente;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, "REGISTRAR_CLIENTE");
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "REGISTRAR_CLIENTE",
                    "No se pudo crear tu registro. Intenta de nuevo en un momento.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, "REGISTRAR_CLIENTE");
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    /** Guarda el domicilio del cliente (se usa al pedir una instalación). */
    public void actualizarDomicilio(int idCliente, String calle, String numero, String colonia) throws SoptecException {
        String sql = "UPDATE cliente SET calle = ?, numero = ?, colonia = ? WHERE id_cliente = ?";
        try (Connection con = Conexion.conectarStrict(MODULO, "ACTUALIZAR_DOMICILIO");
             PreparedStatement ps = con.prepareStatement(sql)) {
            DaoSupport.setNullableString(ps, 1, calle);
            DaoSupport.setNullableString(ps, 2, numero);
            DaoSupport.setNullableString(ps, 3, colonia);
            ps.setInt(4, idCliente);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "ACTUALIZAR_DOMICILIO",
                    "No se pudo guardar tu domicilio.", ex);
        }
    }

    // =====================================================================
    // Citas e instalaciones
    // =====================================================================

    /** Horas que ya están ocupadas ese día (por cualquier cita no cancelada). */
    public Set<LocalTime> horasOcupadas(LocalDate fecha) throws SoptecException {
        String sql = "SELECT hora_cita FROM cita WHERE fecha_cita = ? AND activo = 1 "
                + "AND (estado_cita IS NULL OR estado_cita <> 'Cancelada')";
        Set<LocalTime> horas = new HashSet<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "HORAS_OCUPADAS");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time t = rs.getTime(1);
                    if (t != null) {
                        horas.add(t.toLocalTime().withSecond(0).withNano(0));
                    }
                }
            }
            return horas;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "HORAS_OCUPADAS",
                    "No se pudieron consultar los horarios disponibles.", ex);
        }
    }

    /**
     * Registra la cita que pidió el cliente con estado "Solicitada".
     * Vuelve a revisar que el horario siga libre justo antes de guardar.
     */
    public int solicitarCita(int idCliente, LocalDate fecha, LocalTime hora, String observaciones)
            throws SoptecException {
        if (horasOcupadas(fecha).contains(hora.withSecond(0).withNano(0))) {
            throw new ValidacionException(MODULO, "SOLICITAR_CITA",
                    "Ese horario se acaba de ocupar. Por favor elige otra hora.");
        }
        String sql = "INSERT INTO cita (fecha_cita, hora_cita, estado_cita, observaciones, id_cliente, activo) "
                + "VALUES (?, ?, ?, ?, ?, 1)";
        try (Connection con = Conexion.conectarStrict(MODULO, "SOLICITAR_CITA");
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(fecha));
            ps.setTime(2, Time.valueOf(hora));
            ps.setString(3, ESTADO_SOLICITADA);
            DaoSupport.setNullableString(ps, 4, observaciones);
            ps.setInt(5, idCliente);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : 0;
                AppLogger.registrarInfo(MODULO, PROCESO, "SOLICITAR_CITA",
                        "Cita solicitada desde el portal ID=" + id + " cliente=" + idCliente);
                return id;
            }
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "SOLICITAR_CITA",
                    "No se pudo guardar tu cita. Intenta de nuevo.", ex);
        }
    }

    /** Citas activas de un cliente, de la más reciente a la más antigua. */
    public List<Cita> citasDeCliente(int idCliente) throws SoptecException {
        String sql = "SELECT ci.id_cita, ci.fecha_cita, ci.hora_cita, ci.estado_cita, ci.observaciones, "
                + "ci.id_cliente, ci.activo, c.nombre_cliente, c.rfc "
                + "FROM cita ci INNER JOIN cliente c ON c.id_cliente = ci.id_cliente "
                + "WHERE ci.id_cliente = ? AND ci.activo = 1 "
                + "ORDER BY ci.fecha_cita DESC, ci.hora_cita DESC";
        return consultarCitas(sql, "CITAS_DE_CLIENTE", idCliente);
    }

    /** Citas que mandaron los clientes y todavía no confirma el personal. */
    public List<Cita> citasSolicitadas() throws SoptecException {
        String sql = "SELECT ci.id_cita, ci.fecha_cita, ci.hora_cita, ci.estado_cita, ci.observaciones, "
                + "ci.id_cliente, ci.activo, c.nombre_cliente, c.rfc "
                + "FROM cita ci INNER JOIN cliente c ON c.id_cliente = ci.id_cliente "
                + "WHERE ci.activo = 1 AND ci.estado_cita = ? "
                + "ORDER BY ci.fecha_cita ASC, ci.hora_cita ASC";
        List<Cita> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "CITAS_SOLICITADAS");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ESTADO_SOLICITADA);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCita(rs));
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CITAS_SOLICITADAS",
                    "No se pudieron consultar las citas solicitadas.", ex);
        }
    }

    /** El cliente cancela su propia cita (solo si todavía no se atiende). */
    public boolean cancelarCitaDeCliente(int idCita, int idCliente) throws SoptecException {
        String sql = "UPDATE cita SET estado_cita = 'Cancelada' "
                + "WHERE id_cita = ? AND id_cliente = ? AND activo = 1 "
                + "AND estado_cita IN ('Solicitada', 'Programada', 'Confirmada')";
        try (Connection con = Conexion.conectarStrict(MODULO, "CANCELAR_CITA_CLIENTE");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            ps.setInt(2, idCliente);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AppLogger.registrarInfo(MODULO, PROCESO, "CANCELAR_CITA_CLIENTE",
                        "El cliente " + idCliente + " canceló la cita ID=" + idCita);
            }
            return ok;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CANCELAR_CITA_CLIENTE",
                    "No se pudo cancelar la cita.", ex);
        }
    }

    /** El personal confirma o rechaza una cita solicitada. */
    public boolean cambiarEstadoCita(int idCita, String nuevoEstado) throws SoptecException {
        String sql = "UPDATE cita SET estado_cita = ? WHERE id_cita = ?";
        try (Connection con = Conexion.conectarStrict(MODULO, "CAMBIAR_ESTADO_CITA");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idCita);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CAMBIAR_ESTADO_CITA",
                    "No se pudo cambiar el estado de la cita.", ex);
        }
    }

    // =====================================================================
    // Proyectos del cliente (seguimiento)
    // =====================================================================

    public List<Proyecto> proyectosDeCliente(int idCliente) throws SoptecException {
        String sql = "SELECT p.id_proyecto, p.nombre_proyecto, p.fecha_inicio, p.fecha_estimada_fin, p.estado, "
                + "p.costo_total, p.id_cliente, p.activo, c.nombre_cliente, c.rfc "
                + "FROM proyecto p INNER JOIN cliente c ON c.id_cliente = p.id_cliente "
                + "WHERE p.id_cliente = ? AND p.activo = 1 ORDER BY p.id_proyecto DESC";
        List<Proyecto> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "PROYECTOS_DE_CLIENTE");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date ini = rs.getDate("fecha_inicio");
                    Date fin = rs.getDate("fecha_estimada_fin");
                    lista.add(new Proyecto(rs.getInt("id_proyecto"), rs.getString("nombre_proyecto"),
                            ini != null ? ini.toLocalDate() : null, fin != null ? fin.toLocalDate() : null,
                            rs.getString("estado"), rs.getBigDecimal("costo_total"), rs.getInt("id_cliente"),
                            rs.getBoolean("activo"), rs.getString("nombre_cliente"), rs.getString("rfc")));
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "PROYECTOS_DE_CLIENTE",
                    "No se pudieron consultar tus proyectos.", ex);
        }
    }

    // =====================================================================
    // Tienda (material del almacén)
    // =====================================================================

    /** Material activo con existencias para mostrar en la tienda. */
    public List<Material> materialesDisponibles() throws SoptecException {
        String sql = "SELECT id_material, descripcion_material, precio_costo, existencias, activo "
                + "FROM material WHERE activo = 1 AND existencias > 0 ORDER BY descripcion_material ASC";
        List<Material> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, "MATERIALES_TIENDA");
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Material(rs.getInt("id_material"), rs.getString("descripcion_material"),
                        rs.getBigDecimal("precio_costo"), rs.getInt("existencias"), rs.getBoolean("activo")));
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "MATERIALES_TIENDA",
                    "No se pudo cargar la tienda en este momento.", ex);
        }
    }

    /**
     * Crea el pedido del cliente y aparta las piezas del almacén en una sola transacción.
     * Si algún producto ya no tiene piezas suficientes, no se guarda nada.
     * @param carrito id_material → cantidad
     * @return id del pedido creado
     */
    public int crearPedido(int idCliente, Map<Integer, Integer> carrito) throws SoptecException {
        asegurarTablas();
        if (carrito == null || carrito.isEmpty()) {
            throw new ValidacionException(MODULO, "CREAR_PEDIDO", "Tu pedido está vacío. Agrega al menos un producto.");
        }
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, "CREAR_PEDIDO");
            con.setAutoCommit(false);

            int idPedido;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO pedido_cliente (id_cliente, fecha_pedido, estado, total, activo) VALUES (?, NOW(), ?, 0, 1)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idCliente);
                ps.setString(2, PedidoCliente.APARTADO);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new OperacionBDException(MODULO, "CREAR_PEDIDO", "No se obtuvo el folio del pedido.");
                    }
                    idPedido = keys.getInt(1);
                }
            }

            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> item : carrito.entrySet()) {
                int idMaterial = item.getKey();
                int cantidad = item.getValue() == null ? 0 : item.getValue();
                if (cantidad <= 0) {
                    continue;
                }
                String nombre;
                BigDecimal precio;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT descripcion_material, precio_costo FROM material WHERE id_material = ? AND activo = 1")) {
                    ps.setInt(1, idMaterial);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new ValidacionException(MODULO, "CREAR_PEDIDO",
                                    "Uno de los productos ya no está disponible. Revisa tu pedido.");
                        }
                        nombre = rs.getString(1);
                        precio = rs.getBigDecimal(2) != null ? rs.getBigDecimal(2) : BigDecimal.ZERO;
                    }
                }
                // Aparta las piezas solo si alcanzan (evita vender más de lo que hay).
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE material SET existencias = existencias - ? WHERE id_material = ? AND existencias >= ?")) {
                    ps.setInt(1, cantidad);
                    ps.setInt(2, idMaterial);
                    ps.setInt(3, cantidad);
                    if (ps.executeUpdate() == 0) {
                        throw new ValidacionException(MODULO, "CREAR_PEDIDO",
                                "Ya no hay suficientes piezas de \"" + nombre + "\". Baja la cantidad e intenta otra vez.");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO pedido_cliente_material (id_pedido, id_material, cantidad, precio_unitario) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, idPedido);
                    ps.setInt(2, idMaterial);
                    ps.setInt(3, cantidad);
                    ps.setBigDecimal(4, precio);
                    ps.executeUpdate();
                }
                total = total.add(precio.multiply(BigDecimal.valueOf(cantidad)));
            }

            try (PreparedStatement ps = con.prepareStatement("UPDATE pedido_cliente SET total = ? WHERE id_pedido = ?")) {
                ps.setBigDecimal(1, total);
                ps.setInt(2, idPedido);
                ps.executeUpdate();
            }
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, "CREAR_PEDIDO",
                    "Pedido del portal ID=" + idPedido + " cliente=" + idCliente + " total=" + total);
            return idPedido;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, "CREAR_PEDIDO");
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CREAR_PEDIDO",
                    "No se pudo guardar tu pedido. No se apartó ninguna pieza.", ex);
        } catch (SoptecException ex) {
            Conexion.revertirSilencioso(con, MODULO, "CREAR_PEDIDO");
            AppLogger.registrarExcepcion(MODULO, PROCESO, "CREAR_PEDIDO", ex);
            throw ex;
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    /** Pedidos de un cliente (más recientes primero). */
    public List<PedidoCliente> pedidosDeCliente(int idCliente) throws SoptecException {
        return consultarPedidos("WHERE pc.activo = 1 AND pc.id_cliente = ? ", idCliente, "PEDIDOS_DE_CLIENTE");
    }

    /** Pedidos para el personal: solo pendientes (apartados / listos) o todos. */
    public List<PedidoCliente> listarPedidos(boolean soloPendientes) throws SoptecException {
        String filtro = soloPendientes
                ? "WHERE pc.activo = 1 AND pc.estado IN ('" + PedidoCliente.APARTADO + "', '" + PedidoCliente.LISTO + "') "
                : "WHERE pc.activo = 1 ";
        return consultarPedidos(filtro, null, "LISTAR_PEDIDOS");
    }

    /** Cambia el estado del pedido (Listo para recoger / Entregado). Para cancelar usa cancelarPedido. */
    public boolean cambiarEstadoPedido(int idPedido, String nuevoEstado) throws SoptecException {
        asegurarTablas();
        String sql = "UPDATE pedido_cliente SET estado = ? WHERE id_pedido = ? AND estado IN (?, ?)";
        try (Connection con = Conexion.conectarStrict(MODULO, "CAMBIAR_ESTADO_PEDIDO");
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idPedido);
            ps.setString(3, PedidoCliente.APARTADO);
            ps.setString(4, PedidoCliente.LISTO);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AppLogger.registrarInfo(MODULO, PROCESO, "CAMBIAR_ESTADO_PEDIDO",
                        "Pedido ID=" + idPedido + " ahora está: " + nuevoEstado);
            }
            return ok;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CAMBIAR_ESTADO_PEDIDO",
                    "No se pudo actualizar el pedido.", ex);
        }
    }

    /**
     * Cancela un pedido y regresa las piezas al almacén (transacción).
     * @param idClienteDueno si no es null, solo cancela si el pedido es de ese cliente.
     */
    public boolean cancelarPedido(int idPedido, Integer idClienteDueno) throws SoptecException {
        asegurarTablas();
        Connection con = null;
        try {
            con = Conexion.conectarStrict(MODULO, "CANCELAR_PEDIDO");
            con.setAutoCommit(false);
            String sqlMarcar = "UPDATE pedido_cliente SET estado = ? WHERE id_pedido = ? AND estado IN (?, ?)"
                    + (idClienteDueno != null ? " AND id_cliente = ?" : "");
            try (PreparedStatement ps = con.prepareStatement(sqlMarcar)) {
                ps.setString(1, PedidoCliente.CANCELADO);
                ps.setInt(2, idPedido);
                ps.setString(3, PedidoCliente.APARTADO);
                ps.setString(4, PedidoCliente.LISTO);
                if (idClienteDueno != null) {
                    ps.setInt(5, idClienteDueno);
                }
                if (ps.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE material m INNER JOIN pedido_cliente_material d ON d.id_material = m.id_material "
                    + "SET m.existencias = m.existencias + d.cantidad WHERE d.id_pedido = ?")) {
                ps.setInt(1, idPedido);
                ps.executeUpdate();
            }
            con.commit();
            AppLogger.registrarInfo(MODULO, PROCESO, "CANCELAR_PEDIDO",
                    "Pedido ID=" + idPedido + " cancelado; piezas regresadas al almacén.");
            return true;
        } catch (SQLException ex) {
            Conexion.revertirSilencioso(con, MODULO, "CANCELAR_PEDIDO");
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, "CANCELAR_PEDIDO",
                    "No se pudo cancelar el pedido.", ex);
        } finally {
            Conexion.cerrarSilencioso(con);
        }
    }

    // =====================================================================
    // Utilidades internas
    // =====================================================================

    private List<Cita> consultarCitas(String sql, String operacion, int idCliente) throws SoptecException {
        List<Cita> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, operacion);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCita(rs));
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, operacion,
                    "No se pudieron consultar las citas.", ex);
        }
    }

    private List<PedidoCliente> consultarPedidos(String filtro, Integer idCliente, String operacion)
            throws SoptecException {
        asegurarTablas();
        String sql = "SELECT pc.id_pedido, pc.id_cliente, c.nombre_cliente, pc.fecha_pedido, pc.estado, pc.total, "
                + "GROUP_CONCAT(CONCAT(d.cantidad, ' x ', m.descripcion_material) ORDER BY m.descripcion_material SEPARATOR ', ') AS resumen, "
                + "COALESCE(SUM(d.cantidad), 0) AS piezas "
                + "FROM pedido_cliente pc "
                + "INNER JOIN cliente c ON c.id_cliente = pc.id_cliente "
                + "LEFT JOIN pedido_cliente_material d ON d.id_pedido = pc.id_pedido "
                + "LEFT JOIN material m ON m.id_material = d.id_material "
                + filtro
                + "GROUP BY pc.id_pedido, pc.id_cliente, c.nombre_cliente, pc.fecha_pedido, pc.estado, pc.total "
                + "ORDER BY pc.fecha_pedido DESC, pc.id_pedido DESC";
        List<PedidoCliente> lista = new ArrayList<>();
        try (Connection con = Conexion.conectarStrict(MODULO, operacion);
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (idCliente != null) {
                ps.setInt(1, idCliente);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp f = rs.getTimestamp("fecha_pedido");
                    lista.add(new PedidoCliente(rs.getInt("id_pedido"), rs.getInt("id_cliente"),
                            rs.getString("nombre_cliente"), f != null ? f.toLocalDateTime() : null,
                            rs.getString("estado"), rs.getBigDecimal("total"), rs.getString("resumen"),
                            rs.getInt("piezas")));
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw DaoSupport.traducirYRegistrar(MODULO, PROCESO, operacion,
                    "No se pudieron consultar los pedidos.", ex);
        }
    }

    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        return new Cliente(rs.getInt("id_cliente"), rs.getString("rfc"), rs.getString("nombre_cliente"),
                rs.getString("correo"), rs.getString("calle"), rs.getString("numero"), rs.getString("colonia"),
                rs.getString("estado"), rs.getString("imagen"), rs.getBoolean("activo"));
    }

    private Cita mapearCita(ResultSet rs) throws SQLException {
        Date f = rs.getDate("fecha_cita");
        Time h = rs.getTime("hora_cita");
        return new Cita(rs.getInt("id_cita"), f != null ? f.toLocalDate() : null,
                h != null ? h.toLocalTime() : null, rs.getString("estado_cita"), rs.getString("observaciones"),
                rs.getInt("id_cliente"), rs.getBoolean("activo"), rs.getString("nombre_cliente"), rs.getString("rfc"));
    }
}
