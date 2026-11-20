package com.mycompany.soptec.it.pruebas.soporte;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Clase base para pruebas que NO necesitan MySQL (validaciones, imágenes, logs).
 */
@ExtendWith(ReporteResultadosExtension.class)
public abstract class PruebaUnitaria {

    @BeforeAll
    static void prepararEntorno() {
        EntornoPruebas.inicializar();
    }

    @AfterEach
    void restaurarConexionDePrueba() {
        BaseDatosPrueba.configurarConexion();
    }
}
