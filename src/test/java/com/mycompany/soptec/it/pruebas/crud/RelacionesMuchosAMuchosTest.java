package com.mycompany.soptec.it.pruebas.crud;

import com.mycompany.soptec.it.excepciones.SoptecException;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cliente;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.modelo.Material;
import com.mycompany.soptec.it.modelo.OrdenCompra;
import com.mycompany.soptec.it.modelo.PartidaMaterial;
import com.mycompany.soptec.it.modelo.Proveedor;
import com.mycompany.soptec.it.modelo.Proyecto;
import com.mycompany.soptec.it.excepciones.IntegridadReferencialException;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.ClienteService;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import com.mycompany.soptec.it.servicio.MaterialService;
import com.mycompany.soptec.it.servicio.OrdenCompraService;
import com.mycompany.soptec.it.servicio.ProveedorService;
import com.mycompany.soptec.it.servicio.ProyectoService;
import com.mycompany.soptec.it.servicio.RelacionesService;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD · Relaciones muchos a muchos (personal, proveedores, material y órdenes de compra)")
class RelacionesMuchosAMuchosTest extends PruebaBD {

    private final RelacionesService relaciones = new RelacionesService();

    private Proyecto proyecto() throws SoptecException {
        Cliente cli = new ClienteService().crear(DatosPrueba.nuevoCliente());
        return new ProyectoService().crear(DatosPrueba.nuevoProyecto(cli.getIdCliente()));
    }

    @Test
    @DisplayName("Proyecto ↔ Empleado: asignar, cambiar y quitar el equipo completo")
    void personalPorProyecto() throws Exception {
        Proyecto p = proyecto();
        EmpleadoService es = new EmpleadoService();
        Empleado a = es.crear(DatosPrueba.nuevoEmpleado("Técnico instalador"));
        Empleado b = es.crear(DatosPrueba.nuevoEmpleado("Técnico de redes"));

        relaciones.guardarPersonal(p.getIdProyecto(), List.of(a.getIdEmpleado(), b.getIdEmpleado()));
        assertEquals(2, relaciones.listarPersonal(p.getIdProyecto()).size());

        relaciones.guardarPersonal(p.getIdProyecto(), List.of(b.getIdEmpleado()));
        List<Empleado> equipo = relaciones.listarPersonal(p.getIdProyecto());
        assertEquals(1, equipo.size());
        assertEquals(b.getIdEmpleado(), equipo.get(0).getIdEmpleado());

        // Rollback: si falla a la mitad, el equipo anterior se conserva
        assertThrows(SoptecException.class, () -> relaciones.guardarPersonal(p.getIdProyecto(),
                List.of(a.getIdEmpleado()), true));
        assertEquals(b.getIdEmpleado(), relaciones.listarPersonal(p.getIdProyecto()).get(0).getIdEmpleado());

        es.desactivar(a.getIdEmpleado());
        assertThrows(ValidacionException.class,
                () -> relaciones.guardarPersonal(p.getIdProyecto(), List.of(a.getIdEmpleado())));

        relaciones.guardarPersonal(p.getIdProyecto(), List.of());
        assertTrue(relaciones.listarPersonal(p.getIdProyecto()).isEmpty());
    }

    @Test
    @DisplayName("Material ↔ Proveedor: guardar la lista de material que surte un proveedor")
    void materialPorProveedor() throws Exception {
        Proveedor pr = new ProveedorService().crear(DatosPrueba.nuevoProveedor());
        MaterialService ms = new MaterialService();
        Material m1 = ms.crear(DatosPrueba.nuevoMaterial());
        Material m2 = ms.crear(DatosPrueba.nuevoMaterial());

        relaciones.guardarMaterialDeProveedor(pr.getIdProveedor(), List.of(m1.getIdMaterial(), m2.getIdMaterial(), m1.getIdMaterial()));
        assertEquals(2, relaciones.listarMaterialDeProveedor(pr.getIdProveedor()).size(), "Los repetidos se ignoran");
        assertTrue(relaciones.listarProveedoresDeMaterial(m2.getIdMaterial()).stream()
                .anyMatch(x -> x.getIdProveedor() == pr.getIdProveedor()));

        relaciones.guardarMaterialDeProveedor(pr.getIdProveedor(), List.of(m2.getIdMaterial()));
        assertEquals(1, relaciones.listarMaterialDeProveedor(pr.getIdProveedor()).size());
    }

    @Test
    @DisplayName("Proyecto ↔ Material: asignar descuenta del almacén y quitar lo regresa")
    void materialPorProyecto() throws Exception {
        Proyecto p = proyecto();
        Material m = new MaterialService().crear(DatosPrueba.nuevoMaterial()); // 10 en existencia
        PartidaMaterial cuatro = new PartidaMaterial(m.getIdMaterial(), m.getDescripcionMaterial(), 4, m.getPrecioCosto());

        relaciones.guardarMaterialDeProyecto(p.getIdProyecto(), List.of(cuatro));
        assertEquals(6, new MaterialService().consultarPorId(m.getIdMaterial()).getExistencias());
        assertEquals(4, relaciones.listarMaterialDeProyecto(p.getIdProyecto()).get(0).getCantidad());

        PartidaMaterial demasiado = new PartidaMaterial(m.getIdMaterial(), m.getDescripcionMaterial(), 50, m.getPrecioCosto());
        assertThrows(ValidacionException.class, () -> relaciones.guardarMaterialDeProyecto(p.getIdProyecto(), List.of(demasiado)));
        assertEquals(6, new MaterialService().consultarPorId(m.getIdMaterial()).getExistencias(), "Sin cambios tras el error");

        relaciones.guardarMaterialDeProyecto(p.getIdProyecto(), List.of());
        assertEquals(10, new MaterialService().consultarPorId(m.getIdMaterial()).getExistencias());
    }

    @Test
    @DisplayName("Orden de compra + partidas: registrar, modificar, recibir (suma existencias) y reglas de borrado")
    void ordenesDeCompra() throws Exception {
        Proveedor pr = new ProveedorService().crear(DatosPrueba.nuevoProveedor());
        MaterialService ms = new MaterialService();
        Material m1 = ms.crear(DatosPrueba.nuevoMaterial());
        Material m2 = ms.crear(DatosPrueba.nuevoMaterial());
        OrdenCompraService oc = new OrdenCompraService();

        OrdenCompra o = new OrdenCompra();
        o.setIdProveedor(pr.getIdProveedor());
        o.setFechaOrden(LocalDate.now());
        o.setPartidas(List.of(PartidaMaterial.de(m1, 3)));
        o = oc.crear(o);
        assertEquals(OrdenCompra.SOLICITADA, oc.consultarPorId(o.getIdOrdenCompra()).getEstado());

        o.setPartidas(List.of(PartidaMaterial.de(m1, 5), PartidaMaterial.de(m2, 2)));
        oc.actualizar(o);
        OrdenCompra leida = oc.consultarPorId(o.getIdOrdenCompra());
        assertEquals(2, leida.getPartidas().size());
        assertEquals(0, m1.getPrecioCosto().multiply(java.math.BigDecimal.valueOf(5))
                .add(m2.getPrecioCosto().multiply(java.math.BigDecimal.valueOf(2))).compareTo(leida.getTotalCompra()));

        oc.recibir(o.getIdOrdenCompra());
        assertEquals(15, ms.consultarPorId(m1.getIdMaterial()).getExistencias());
        assertEquals(12, ms.consultarPorId(m2.getIdMaterial()).getExistencias());
        final int id = o.getIdOrdenCompra();
        assertThrows(ValidacionException.class, () -> oc.recibir(id), "No se recibe dos veces");
        assertThrows(IntegridadReferencialException.class, () -> oc.eliminarFisico(id));

        // Rollback de la orden: no quedan ni encabezado ni partidas
        OrdenCompra fallida = new OrdenCompra();
        fallida.setIdProveedor(pr.getIdProveedor());
        fallida.setFechaOrden(LocalDate.now());
        fallida.setPartidas(List.of(PartidaMaterial.de(m1, 1)));
        int antes = BaseDatosPrueba.contar("SELECT COUNT(*) FROM orden_compra WHERE id_proveedor = ?", pr.getIdProveedor());
        assertThrows(SoptecException.class, () -> oc.crear(fallida, true));
        assertEquals(antes, BaseDatosPrueba.contar("SELECT COUNT(*) FROM orden_compra WHERE id_proveedor = ?", pr.getIdProveedor()));

        // Una orden sin recibir se cancela y se elimina con sus partidas
        OrdenCompra otra = new OrdenCompra();
        otra.setIdProveedor(pr.getIdProveedor());
        otra.setFechaOrden(LocalDate.now());
        otra.setPartidas(List.of(PartidaMaterial.de(m2, 1)));
        otra = oc.crear(otra);
        oc.cancelar(otra.getIdOrdenCompra());
        oc.eliminarFisico(otra.getIdOrdenCompra());
        assertEquals(0, BaseDatosPrueba.contar("SELECT COUNT(*) FROM orden_compra_material WHERE id_orden_compra = ?",
                otra.getIdOrdenCompra()));
    }
}
