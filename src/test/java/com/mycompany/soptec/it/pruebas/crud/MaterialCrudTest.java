package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.MaterialService;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Materiales")
class MaterialCrudTest extends PruebaBD {

    private final MaterialService servicio = new MaterialService();

    @Test
    @DisplayName("Registrar material y consultarlo por ID")
    void registrarYConsultar() throws Exception {
        Material m = servicio.crear(DatosPrueba.nuevoMaterial());
        Material consultado = servicio.consultarPorId(m.getIdMaterial());
        assertEquals(m.getDescripcionMaterial(), consultado.getDescripcionMaterial());
        assertEquals(0, new BigDecimal("150.75").compareTo(consultado.getPrecioCosto()));
        assertEquals(10, consultado.getExistencias());
    }

    @Test
    @DisplayName("Listar y buscar materiales")
    void listarYBuscar() throws Exception {
        Material m = servicio.crear(DatosPrueba.nuevoMaterial());
        assertTrue(servicio.listar(false).stream().anyMatch(x -> x.getIdMaterial() == m.getIdMaterial()));
        assertTrue(servicio.buscar(m.getDescripcionMaterial(), false).stream()
                .anyMatch(x -> x.getIdMaterial() == m.getIdMaterial()));
    }

    @Test
    @DisplayName("Actualizar precio y existencias")
    void actualizar() throws Exception {
        Material m = servicio.crear(DatosPrueba.nuevoMaterial());
        m.setPrecioCosto(new BigDecimal("199.90"));
        m.setExistencias(55);
        servicio.actualizar(m);

        Material consultado = servicio.consultarPorId(m.getIdMaterial());
        assertEquals(0, new BigDecimal("199.90").compareTo(consultado.getPrecioCosto()));
        assertEquals(55, consultado.getExistencias());
    }

    @Test
    @DisplayName("Borrado lógico, reactivación y borrado físico")
    void bajas() throws Exception {
        Material m = servicio.crear(DatosPrueba.nuevoMaterial());
        servicio.desactivar(m.getIdMaterial());
        assertFalse(servicio.consultarPorId(m.getIdMaterial()).isActivo());
        servicio.reactivar(m.getIdMaterial());
        assertTrue(servicio.consultarPorId(m.getIdMaterial()).isActivo());
        servicio.eliminarFisico(m.getIdMaterial());
        assertNull(servicio.consultarPorId(m.getIdMaterial()));
    }
}
