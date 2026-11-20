package com.mycompany.soptec.it.pruebas.soporte;

import com.mycompany.soptec.it.dao.Conexion;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Clase base para pruebas que usan MySQL. Prepara soptec_it_test y verifica que
 * NUNCA se esté apuntando a la base de producción.
 * Si MySQL (XAMPP) está apagado, las pruebas de la clase se marcan como omitidas.
 */
@ExtendWith(ReporteResultadosExtension.class)
public abstract class PruebaBD {

    @BeforeAll
    static void prepararBaseDeDatosDePrueba(TestInfo info) {
        EntornoPruebas.inicializar();
        boolean lista = BaseDatosPrueba.preparar();
        if (!lista) {
            Class<?> clase = info.getTestClass().orElse(PruebaBD.class);
            ReporteResultados.instancia().agregar(ReporteResultadosExtension.categoria(clase), info.getDisplayName(),
                    "(todas las pruebas de la clase)", "OMITIDA", 0,
                    "MySQL no disponible: " + BaseDatosPrueba.getMotivoNoDisponible());
        }
        assumeTrue(lista, () -> "MySQL no disponible (enciende MySQL en XAMPP): "
                + BaseDatosPrueba.getMotivoNoDisponible());
        assertTrue(Conexion.getUrlActual().contains("/" + BaseDatosPrueba.NOMBRE_BD + "?"),
                "Las pruebas deben ejecutarse sobre la BD independiente " + BaseDatosPrueba.NOMBRE_BD);
    }

    @AfterEach
    void restaurarConexionDePrueba() {
        BaseDatosPrueba.configurarConexion();
    }
}
