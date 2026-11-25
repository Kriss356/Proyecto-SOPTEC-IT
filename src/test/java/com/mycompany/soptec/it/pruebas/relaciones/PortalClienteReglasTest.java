package com.mycompany.soptec.it.pruebas.relaciones;

import com.mycompany.soptec.it.dao.PortalClienteDao;
import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.modelo.Cita;
import com.mycompany.soptec.it.pruebas.soporte.PruebaUnitaria;
import com.mycompany.soptec.it.servicio.PortalClienteService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Validaciones · Reglas del Portal del cliente")
class PortalClienteReglasTest extends PruebaUnitaria {

    private final PortalClienteService servicio = new PortalClienteService();

    @Test
    @DisplayName("Teléfono: se aceptan 10 dígitos (con o sin espacios / +52) y se rechaza lo demás")
    void telefono() throws Exception {
        assertEquals("3312345678", servicio.normalizarTelefono("33 1234 5678"));
        assertEquals("3312345678", servicio.normalizarTelefono("(33) 1234-5678"));
        assertEquals("3312345678", servicio.normalizarTelefono("+52 33 1234 5678"));
        assertThrows(ValidacionException.class, () -> servicio.normalizarTelefono("12345"));
        assertThrows(ValidacionException.class, () -> servicio.normalizarTelefono(""));
        assertThrows(ValidacionException.class, () -> servicio.normalizarTelefono(null));
    }

    @Test
    @DisplayName("Teléfono: se compara por los últimos 10 dígitos")
    void compararTelefonos() {
        assertTrue(PortalClienteDao.mismoTelefono("3312345678", "33 1234 5678"));
        assertTrue(PortalClienteDao.mismoTelefono("523312345678", "3312345678"));
        assertFalse(PortalClienteDao.mismoTelefono("3312345678", "3312345679"));
        assertFalse(PortalClienteDao.mismoTelefono("", "3312345678"));
    }

    @Test
    @DisplayName("Agenda: no se ofrecen domingos y las horas van de 9:00 a 17:00")
    void diasYHoras() {
        List<LocalDate> dias = servicio.diasDisponibles();
        assertFalse(dias.isEmpty());
        assertTrue(dias.stream().noneMatch(d -> d.getDayOfWeek() == DayOfWeek.SUNDAY));
        assertTrue(dias.stream().noneMatch(d -> d.isBefore(LocalDate.now())));
        List<LocalTime> horas = servicio.horasDelDia();
        assertEquals(LocalTime.of(9, 0), horas.get(0));
        assertEquals(LocalTime.of(17, 0), horas.get(horas.size() - 1));
    }

    @Test
    @DisplayName("Agenda: una hora ocupada o ya pasada no se puede elegir")
    void horaDisponible() {
        LocalDate manana = LocalDate.now().plusDays(1);
        Set<LocalTime> ocupadas = Set.of(LocalTime.of(10, 0));
        assertFalse(servicio.horaDisponible(manana, LocalTime.of(10, 0), ocupadas));
        assertTrue(servicio.horaDisponible(manana, LocalTime.of(11, 0), ocupadas));
        assertFalse(servicio.horaDisponible(LocalDate.now().minusDays(1), LocalTime.of(11, 0), Collections.emptySet()));
    }

    @Test
    @DisplayName("Textos amigables: estados y tipo de servicio que ve el cliente")
    void textosAmigables() {
        assertEquals("Esperando confirmación", PortalClienteService.estadoCitaParaCliente("Solicitada"));
        assertEquals("Confirmada", PortalClienteService.estadoCitaParaCliente("Programada"));
        assertEquals("¡Listo para recoger!", PortalClienteService.estadoPedidoParaCliente("Listo para recoger"));
        assertEquals("badge-peligro", PortalClienteService.claseBadge("Cancelado"));
        Cita c = new Cita();
        c.setObservaciones(PortalClienteDao.PREFIJO_PORTAL + " Servicio: Instalación · Tel: 3312345678");
        assertEquals("Instalación", PortalClienteService.servicioDeCita(c));
        c.setObservaciones("Cita capturada por Ventas");
        assertEquals("Cita", PortalClienteService.servicioDeCita(c));
    }
}
