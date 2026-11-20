package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.ProveedorService;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Proveedores")
class ProveedorCrudTest extends PruebaBD {

    private final ProveedorService servicio = new ProveedorService();

    @Test
    @DisplayName("Registrar proveedor y consultarlo por ID")
    void registrarYConsultar() throws Exception {
        Proveedor p = servicio.crear(DatosPrueba.nuevoProveedor());
        Proveedor consultado = servicio.consultarPorId(p.getIdProveedor());
        assertEquals(p.getNombreProveedor(), consultado.getNombreProveedor());
        assertEquals(p.getCorreoProveedor(), consultado.getCorreoProveedor());
        assertEquals(p.getTelefonoProveedor(), consultado.getTelefonoProveedor());
    }

    @Test
    @DisplayName("Listar y buscar proveedores")
    void listarYBuscar() throws Exception {
        Proveedor p = servicio.crear(DatosPrueba.nuevoProveedor());
        assertTrue(servicio.listar(false).stream().anyMatch(x -> x.getIdProveedor() == p.getIdProveedor()));
        assertTrue(servicio.buscar(p.getNombreProveedor(), false).stream()
                .anyMatch(x -> x.getIdProveedor() == p.getIdProveedor()));
    }

    @Test
    @DisplayName("Actualizar teléfono y correo")
    void actualizar() throws Exception {
        Proveedor p = servicio.crear(DatosPrueba.nuevoProveedor());
        String correo = DatosPrueba.correoUnico("nuevo.proveedor");
        p.setCorreoProveedor(correo);
        p.setTelefonoProveedor("3344556677");
        servicio.actualizar(p);

        Proveedor consultado = servicio.consultarPorId(p.getIdProveedor());
        assertEquals(correo, consultado.getCorreoProveedor());
        assertEquals("3344556677", consultado.getTelefonoProveedor());
    }

    @Test
    @DisplayName("Borrado lógico, reactivación y borrado físico")
    void bajas() throws Exception {
        Proveedor p = servicio.crear(DatosPrueba.nuevoProveedor());
        servicio.desactivar(p.getIdProveedor());
        assertFalse(servicio.consultarPorId(p.getIdProveedor()).isActivo());
        servicio.reactivar(p.getIdProveedor());
        assertTrue(servicio.consultarPorId(p.getIdProveedor()).isActivo());
        servicio.eliminarFisico(p.getIdProveedor());
        assertNull(servicio.consultarPorId(p.getIdProveedor()));
    }
}
