package com.mycompany.soptec.it.pruebas.soporte;

import com.mycompany.soptec.it.util.AppLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acumula los resultados de todas las pruebas y al terminar la ejecución genera
 * {@code reportes/reporte_pruebas.html} y {@code reportes/reporte_pruebas.csv}.
 */
public final class ReporteResultados {

    public static final Path CARPETA = Paths.get("reportes");
    private static ReporteResultados instancia;

    private final List<String[]> filas = new ArrayList<>();
    private final LocalDateTime inicio = LocalDateTime.now();

    private ReporteResultados() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::escribir, "reporte-pruebas"));
    }

    public static synchronized ReporteResultados instancia() {
        if (instancia == null) {
            instancia = new ReporteResultados();
        }
        return instancia;
    }

    /**
     * @param categoria criterio de evaluación (CRUD, Relaciones, Imágenes...)
     * @param clase nombre descriptivo de la clase de prueba
     * @param prueba nombre descriptivo de la prueba
     * @param estado APROBADA, FALLIDA u OMITIDA
     */
    public synchronized void agregar(String categoria, String clase, String prueba, String estado,
            long milisegundos, String detalle) {
        filas.add(new String[]{categoria, clase, prueba, estado, String.valueOf(milisegundos),
            detalle == null ? "" : detalle.replaceAll("[\\r\\n]+", " ")});
    }

    private synchronized void escribir() {
        if (filas.isEmpty()) {
            return;
        }
        filas.sort(java.util.Comparator.comparing((String[] f) -> f[0]).thenComparing(f -> f[1]));
        try {
            Files.createDirectories(CARPETA);
            Files.write(CARPETA.resolve("reporte_pruebas.csv"), csv().getBytes(StandardCharsets.UTF_8));
            Files.write(CARPETA.resolve("reporte_pruebas.html"), html().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            System.err.println("No se pudo escribir el reporte de pruebas: " + ex.getMessage());
        }
    }

    private String csv() {
        StringBuilder sb = new StringBuilder("﻿Categoria,Clase,Prueba,Estado,Duracion_ms,Detalle\n");
        for (String[] f : filas) {
            for (int i = 0; i < f.length; i++) {
                sb.append('"').append(f[i].replace("\"", "\"\"")).append('"').append(i < f.length - 1 ? "," : "\n");
            }
        }
        return sb.toString();
    }

    private String html() {
        int ok = 0, fallo = 0, omit = 0;
        Map<String, int[]> porCategoria = new LinkedHashMap<>();
        for (String[] f : filas) {
            int[] c = porCategoria.computeIfAbsent(f[0], k -> new int[3]);
            switch (f[3]) {
                case "APROBADA": ok++; c[0]++; break;
                case "FALLIDA": fallo++; c[1]++; break;
                default: omit++; c[2]++; break;
            }
        }
        String fecha = inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><title>Reporte de pruebas SOPTEC-IT</title>")
          .append("<style>body{font-family:Segoe UI,Arial,sans-serif;margin:32px;color:#1d2433}h1{color:#0b4f8a;margin-bottom:4px}")
          .append(".meta{color:#555;margin-bottom:24px}.cards{display:flex;gap:16px;margin-bottom:24px}")
          .append(".card{padding:14px 22px;border-radius:10px;background:#f2f5f9;min-width:120px}.card b{font-size:26px;display:block}")
          .append(".ok b{color:#1e7d32}.fail b{color:#c62828}.skip b{color:#8a6d00}")
          .append("table{border-collapse:collapse;width:100%;margin-bottom:28px;font-size:14px}th{background:#0b4f8a;color:#fff;text-align:left}")
          .append("th,td{padding:7px 10px;border-bottom:1px solid #dde3ea;vertical-align:top}")
          .append(".APROBADA{color:#1e7d32;font-weight:600}.FALLIDA{color:#c62828;font-weight:600}.OMITIDA{color:#8a6d00;font-weight:600}")
          .append("</style></head><body>");
        sb.append("<h1>Reporte de resultados de pruebas unitarias — SOPTEC-IT</h1>")
          .append("<div class='meta'>Ejecución: ").append(fecha)
          .append(" · Framework: JUnit 5 · Base de datos: ").append(BaseDatosPrueba.NOMBRE_BD)
          .append(" · Log: ").append(esc(AppLogger.getRutaLog().toString())).append("</div>");
        sb.append("<div class='cards'><div class='card'><b>").append(filas.size()).append("</b>Total</div>")
          .append("<div class='card ok'><b>").append(ok).append("</b>Aprobadas</div>")
          .append("<div class='card fail'><b>").append(fallo).append("</b>Fallidas</div>")
          .append("<div class='card skip'><b>").append(omit).append("</b>Omitidas</div></div>");
        sb.append("<h2>Resumen por criterio</h2><table><tr><th>Criterio</th><th>Aprobadas</th><th>Fallidas</th><th>Omitidas</th></tr>");
        for (Map.Entry<String, int[]> e : porCategoria.entrySet()) {
            sb.append("<tr><td>").append(esc(e.getKey())).append("</td><td>").append(e.getValue()[0])
              .append("</td><td>").append(e.getValue()[1]).append("</td><td>").append(e.getValue()[2]).append("</td></tr>");
        }
        sb.append("</table><h2>Detalle</h2><table><tr><th>#</th><th>Criterio</th><th>Clase</th><th>Prueba</th>")
          .append("<th>Resultado</th><th>ms</th><th>Detalle</th></tr>");
        int i = 1;
        for (String[] f : filas) {
            sb.append("<tr><td>").append(i++).append("</td><td>").append(esc(f[0])).append("</td><td>").append(esc(f[1]))
              .append("</td><td>").append(esc(f[2])).append("</td><td class='").append(f[3]).append("'>").append(f[3])
              .append("</td><td>").append(f[4]).append("</td><td>").append(esc(f[5])).append("</td></tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
