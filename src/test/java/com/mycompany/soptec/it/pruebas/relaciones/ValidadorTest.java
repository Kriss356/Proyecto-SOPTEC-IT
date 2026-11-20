package com.mycompany.soptec.it.pruebas.relaciones;

import com.mycompany.soptec.it.excepciones.ValidacionException;
import com.mycompany.soptec.it.pruebas.soporte.PruebaUnitaria;
import com.mycompany.soptec.it.util.Validador;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Validaciones · Clase Validador")
class ValidadorTest extends PruebaUnitaria {

    private static final String M = "Pruebas";
    private static final String O = "VALIDAR";

    @Test
    @DisplayName("RFC: acepta persona física y moral, rechaza formatos incorrectos")
    void rfc() throws Exception {
        assertEquals("XAXX010101000", Validador.validarRfc("xaxx010101000", M, O), "Se normaliza a mayúsculas");
        assertEquals("ABC150301AB2", Validador.validarRfc("ABC150301AB2", M, O));
        assertThrows(ValidacionException.class, () -> Validador.validarRfc("123", M, O));
        assertThrows(ValidacionException.class, () -> Validador.validarRfc("ABCD15AB01AB2", M, O));
        assertThrows(ValidacionException.class, () -> Validador.validarRfc(null, M, O));
    }

    @Test
    @DisplayName("Correo: formato válido y normalizado a minúsculas")
    void correo() throws Exception {
        assertEquals("ventas@soptec.com", Validador.validarCorreo(" Ventas@SOPTEC.com ", M, O));
        assertThrows(ValidacionException.class, () -> Validador.validarCorreo("ventas@soptec", M, O));
        assertThrows(ValidacionException.class, () -> Validador.validarCorreo("ventas soptec.com", M, O));
    }

    @Test
    @DisplayName("Teléfono: mínimo 10 dígitos y solo caracteres válidos")
    void telefono() throws Exception {
        assertEquals("33 1234 5678", Validador.validarTelefono("33 1234 5678", M, O));
        assertEquals("+52 (33) 1234-5678", Validador.validarTelefono("+52 (33) 1234-5678", M, O));
        assertThrows(ValidacionException.class, () -> Validador.validarTelefono("331234567", M, O));
        assertThrows(ValidacionException.class, () -> Validador.validarTelefono("33ABC45678", M, O));
    }

    @Test
    @DisplayName("Números: decimales y enteros no negativos, IDs positivos")
    void numeros() throws Exception {
        assertEquals(0, new BigDecimal("1250.50").compareTo(
                Validador.parsearDecimalNoNegativo("$1,250.50", "Precio", M, O)));
        assertThrows(ValidacionException.class, () -> Validador.parsearDecimalNoNegativo("-1", "Precio", M, O));
        assertThrows(ValidacionException.class, () -> Validador.parsearDecimalNoNegativo("abc", "Precio", M, O));
        assertEquals(7, Validador.parsearEnteroNoNegativo("7", "Existencias", M, O));
        assertThrows(ValidacionException.class, () -> Validador.parsearEnteroNoNegativo("2.5", "Existencias", M, O));
        assertThrows(ValidacionException.class, () -> Validador.parsearIdPositivo("0", "ID", M, O));
    }

    @Test
    @DisplayName("Fechas y horas: rango coherente y formato HH:mm")
    void fechasYHoras() throws Exception {
        LocalDate hoy = LocalDate.now();
        assertDoesNotThrow(() -> Validador.validarRangoFechas(hoy, hoy.plusDays(1), M, O));
        assertDoesNotThrow(() -> Validador.validarRangoFechas(hoy, null, M, O));
        assertThrows(ValidacionException.class, () -> Validador.validarRangoFechas(hoy, hoy.minusDays(1), M, O));
        assertEquals(LocalTime.of(9, 30), Validador.parsearHora("09:30", M, O));
        assertThrows(ValidacionException.class, () -> Validador.parsearHora("25:00", M, O));
        assertThrows(ValidacionException.class, () -> Validador.parsearHora("9.30", M, O));
    }

    @Test
    @DisplayName("Longitud máxima y campos opcionales")
    void longitudesYOpcionales() {
        assertThrows(ValidacionException.class,
                () -> Validador.longitudMaxima("x".repeat(101), 100, "Nombre", M, O));
        assertDoesNotThrow(() -> Validador.longitudMaxima("x".repeat(100), 100, "Nombre", M, O));
        assertNull(Validador.opcional("   "));
        assertEquals("Centro", Validador.opcional("  Centro "));
    }
}
