package soptecit.util;

import com.mycompany.soptec.it.modelo.Cliente;

/**
 * Guarda al cliente que entró al Portal del cliente con su número de teléfono.
 * Solo existe mientras la aplicación está abierta; al salir se borra.
 */
public final class SesionCliente {

    private static Cliente clienteActual;

    private SesionCliente() {
    }

    public static void iniciar(Cliente cliente) {
        clienteActual = cliente;
    }

    public static Cliente getCliente() {
        return clienteActual;
    }

    public static boolean hayCliente() {
        return clienteActual != null;
    }

    /** Primer nombre del cliente para saludarlo ("Hola, Juan"). */
    public static String primerNombre() {
        if (clienteActual == null || clienteActual.getNombreCliente() == null) {
            return "";
        }
        String nombre = clienteActual.getNombreCliente().trim();
        int espacio = nombre.indexOf(' ');
        return espacio > 0 ? nombre.substring(0, espacio) : nombre;
    }

    public static void cerrar() {
        clienteActual = null;
    }
}
