package com.mycompany.soptec.it.pruebas.relaciones;

import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Empleado;
import com.mycompany.soptec.it.pruebas.soporte.BaseDatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.DatosPrueba;
import com.mycompany.soptec.it.pruebas.soporte.PruebaBD;
import com.mycompany.soptec.it.servicio.EmpleadoService;
import static com.mycompany.soptec.it.servicio.EmpleadoService.PUESTO_ADMIN_GENERAL;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Relaciones · Único Administrador general")
class AdministradorGeneralTest extends PruebaBD {

    private final EmpleadoService servicio = new EmpleadoService();

    @BeforeEach
    void sinAdministradores() throws Exception {
        BaseDatosPrueba.ejecutar("DELETE FROM empleado WHERE LOWER(TRIM(puesto)) = 'administrador general'");
    }

    private int administradoresActivos() throws Exception {
        return BaseDatosPrueba.contar(
                "SELECT COUNT(*) FROM empleado WHERE LOWER(TRIM(puesto)) = 'administrador general' AND activo = 1");
    }

    @Test
    @DisplayName("Se permite registrar el primer Administrador general")
    void primerAdministrador() throws Exception {
        Empleado admin = servicio.crear(DatosPrueba.nuevoEmpleado(PUESTO_ADMIN_GENERAL));
        assertTrue(admin.getIdEmpleado() > 0);
        assertEquals(1, administradoresActivos());
    }

    @Test
    @DisplayName("Un segundo Administrador general es rechazado")
    void segundoAdministradorRechazado() throws Exception {
        servicio.crear(DatosPrueba.nuevoEmpleado(PUESTO_ADMIN_GENERAL));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.crear(DatosPrueba.nuevoEmpleado(PUESTO_ADMIN_GENERAL)));
        assertTrue(ex.getMessage().contains("Administrador general"), ex.getMessage());
        assertEquals(1, administradoresActivos());
    }

    @Test
    @DisplayName("Tampoco se acepta escrito con otras mayúsculas o espacios")
    void variantesDeEscritura() throws Exception {
        servicio.crear(DatosPrueba.nuevoEmpleado(PUESTO_ADMIN_GENERAL));
        assertThrows(ValidacionException.class,
                () -> servicio.crear(DatosPrueba.nuevoEmpleado("  ADMINISTRADOR GENERAL ")));
        assertEquals(1, administradoresActivos());
    }

    @Test
    @DisplayName("No se puede cambiar el puesto de otro empleado a Administrador general")
    void actualizarOtroEmpleadoRechazado() throws Exception {
        servicio.crear(DatosPrueba.nuevoEmpleado(PUESTO_ADMIN_GENERAL));
        Empleado otro = servicio.crear(DatosPrueba.nuevoEmpleado("Agente de Ventas"));
        otro.setPuesto(PUESTO_ADMIN_GENERAL);
        assertThrows(ValidacionException.class, () -> servicio.actualizar(otro));
        assertEquals("Agente de Ventas", servicio.consultarPorId(otro.getIdEmpleado()).getPuesto());
    }

    @Test
    @DisplayName("El Administrador general sí puede editar sus propios datos")
    void adminEditaSusDatos() throws Exception {
        Empleado admin = servicio.crear(DatosPrueba.nuevoEmpleado(PUESTO_ADMIN_GENERAL));
        admin.setTelefono("3312341234");
        assertDoesNotThrow(() -> servicio.actualizar(admin));
    }

    @Test
    @DisplayName("Si el administrador se desactiva se asigna otro y el anterior no puede reactivarse")
    void reemplazoDeAdministrador() throws Exception {
        Empleado anterior = servicio.crear(DatosPrueba.nuevoEmpleado(PUESTO_ADMIN_GENERAL));
        servicio.desactivar(anterior.getIdEmpleado());

        Empleado nuevo = servicio.crear(DatosPrueba.nuevoEmpleado(PUESTO_ADMIN_GENERAL));
        assertTrue(nuevo.getIdEmpleado() > 0);

        assertThrows(ValidacionException.class, () -> servicio.reactivar(anterior.getIdEmpleado()));
        assertFalse(servicio.consultarPorId(anterior.getIdEmpleado()).isActivo());
        assertEquals(1, administradoresActivos());
    }
}
