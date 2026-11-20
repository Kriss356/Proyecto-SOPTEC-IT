package com.mycompany.soptec.it.pruebas.soporte;

import java.util.Optional;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * Extensión JUnit 5 que registra el resultado y duración de cada prueba en el reporte de resultados.
 */
public class ReporteResultadosExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, TestWatcher {

    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(ReporteResultadosExtension.class);

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        context.getStore(NS).put("inicio", System.nanoTime());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        Long inicio = context.getStore(NS).get("inicio", Long.class);
        if (inicio != null) {
            context.getStore(NS).put("ms", (System.nanoTime() - inicio) / 1_000_000);
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        registrar(context, "APROBADA", null);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        registrar(context, "FALLIDA", cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        registrar(context, "OMITIDA", cause.getMessage());
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        registrar(context, "OMITIDA", reason.orElse("Deshabilitada"));
    }

    private void registrar(ExtensionContext context, String estado, String detalle) {
        Long ms = context.getStore(NS).get("ms", Long.class);
        String clase = context.getParent().map(ExtensionContext::getDisplayName)
                .orElse(context.getRequiredTestClass().getSimpleName());
        ReporteResultados.instancia().agregar(categoria(context.getRequiredTestClass()), clase,
                context.getDisplayName(), estado, ms == null ? 0 : ms, detalle);
    }

    static String categoria(Class<?> clase) {
        String paquete = clase.getPackageName();
        String ultimo = paquete.substring(paquete.lastIndexOf('.') + 1);
        switch (ultimo) {
            case "crud": return "1. Operaciones CRUD";
            case "relaciones": return "2. Relaciones y validaciones";
            case "imagenes": return "3. Gestión de imágenes";
            case "excepciones": return "4. Excepciones y logs";
            case "funcional": return "5. Funcionales / integración (JavaFX)";
            default: return "5. Funcionales / integración (JavaFX)";
        }
    }
}
