# Pruebas unitarias JUnit 5 — SOPTEC-IT

Práctica: *Implementación de pruebas unitarias para la aplicación JavaFX* (actividad colaborativa del proyecto cuatrimestral).

> Actualizado el 20/11/2026: con la práctica de CRUD y excepciones ya son **144 pruebas** (ver `PRACTICA_CRUD_EXCEPCIONES.md`).

**Resultado de referencia (19/11/2026):** 111 pruebas aprobadas, 0 fallidas (corrida con MySQL real sobre `soptec_it_test`). La prueba funcional de vistas JavaFX (`VistasFxmlTest`) se ejecuta en Windows desde NetBeans.

---

## 1. Cómo ejecutarlas

1. Enciende **MySQL** en el panel de XAMPP (no hace falta importar nada).
2. En NetBeans: clic derecho en el proyecto → **Test** (o `Alt + F6`).
   - Para una sola clase: abre la clase de prueba → clic derecho → **Test File** (`Ctrl + F6`).
3. Revisa los resultados en la ventana **Test Results** de NetBeans.

Al terminar se generan automáticamente:

| Entregable | Ubicación |
|---|---|
| Reporte de resultados (para abrir en el navegador) | `reportes/reporte_pruebas.html` |
| Reporte en tabla (Excel) | `reportes/reporte_pruebas.csv` |
| Archivo log generado durante las pruebas | `logs/soptec_pruebas.log` |
| Reportes estándar de Maven/JUnit | `target/surefire-reports/` |

> Si MySQL está apagado, las pruebas que usan la base de datos aparecen como **omitidas** (no fallan) y el reporte indica el motivo. Las de validaciones, imágenes y logs se ejecutan igual.

### Base de datos independiente
Las pruebas crean solas la base **`soptec_it_test`** (misma estructura que `soptec_it`) a partir de `database/soptec_it_test.sql` y la recrean vacía en cada ejecución. Antes de cada clase se verifica que la conexión apunte a `soptec_it_test`; **nunca se modifica la base de producción `soptec_it`**.

Si tu MySQL tiene contraseña: en NetBeans → Propiedades del proyecto → *Actions* → *Test project* → agrega `soptec.test.password=TU_CLAVE` (también existen `soptec.test.usuario` y `soptec.test.host`).

---

## 2. Clases de prueba organizadas por módulo

Paquete `src/test/java/com/mycompany/soptec/it/pruebas/`

| Criterio (peso) | Paquete | Clases | Qué comprueban |
|---|---|---|---|
| **Pruebas CRUD (30%)** | `crud` | `ClienteCrudTest`, `ClienteTelefonoCrudTest`, `EmpleadoCrudTest`, `MaterialCrudTest`, `ProveedorCrudTest`, `ProyectoCrudTest`, `CitaCrudTest`, `LevantamientoCrudTest` | Registro, consulta por ID, listado (activos/inactivos), búsqueda, edición/actualización, borrado lógico, reactivación y borrado físico de las 8 tablas; procesos transaccionales *cliente + teléfono* y *proyecto + levantamiento*. |
| **Relaciones y validaciones (20%)** | `relaciones` | `DuplicadosTest` | RFC, correo, teléfono, descripción de material y proveedor duplicados (validación del servicio y restricción `UNIQUE` de MySQL, error 1062). |
| | | `AdministradorGeneralTest` | Solo puede existir **un Administrador general activo** (alta, edición, mayúsculas/espacios, reactivación). |
| | | `IntegridadReferencialTest` | No se eliminan clientes, proyectos, citas, materiales, proveedores ni empleados con registros dependientes; FK de MySQL (errores 1451 y 1452). |
| | | `CamposObligatoriosTest`, `ValidadorTest` | Campos obligatorios de todos los módulos, formatos de RFC, correo, teléfono, números, fechas y horas. |
| | | `TransaccionesRollbackTest` | Rollback ante fallo simulado y ante error **real** de MySQL; reintento exitoso después del rollback. |
| **Gestión de imágenes (15%)** | `imagenes` | `GestorImagenesTest` | Carpeta `Documentos/SOPTEC-IT/imagenes` creada automáticamente, guardar/recuperar con el mismo contenido, imagen inexistente, extensión incorrecta, archivo falso con extensión `.png`, error de acceso, sin archivos a medias tras un fallo. |
| | | `ImagenesBaseDatosTest` | La ruta queda en la BD y se recupera la imagen (clientes, empleados, proveedores, croquis); si falla el almacenamiento el registro conserva su imagen anterior; imagen borrada del disco. |
| **Excepciones y tolerancia a fallos (15%)** | `excepciones` | `MensajesUsuarioTest`, `ToleranciaFallosTest` | Mensajes descriptivos sin SQL ni trazas; recuperación tras caída de MySQL; errores consecutivos sin afectar la siguiente operación; errores de MySQL traducidos a mensajes comprensibles. |
| **Registro de errores en logs (10%)** | `excepciones` | `RegistroLogTest` | Cada línea tiene fecha, hora, módulo, proceso, operación y tipo de excepción; no se registran contraseñas, tokens ni parámetros JDBC; una línea por evento. |
| Funcionales / integración | `funcional` | `VistasFxmlTest` | Carga de las 22 vistas FXML con su controlador y CSS. |
| (soporte) | `soporte` | `BaseDatosPrueba`, `PruebaBD`, `PruebaUnitaria`, `DatosPrueba`, `EntornoPruebas`, `LogPruebas`, `ReporteResultados*` | BD de pruebas, datos únicos, log de pruebas y generación del reporte. |

---

## 3. Cambios hechos en la aplicación para cumplir la práctica

- **`GestorImagenes`**: las imágenes ahora se guardan en `C:\Users\<usuario>\Documents\SOPTEC-IT\imagenes\{clientes, empleados, proveedores, croquis}` y la carpeta se crea sola si no existe. Se valida el **contenido** del archivo (no solo la extensión), el tamaño máximo (10 MB) y la copia es atómica (no deja archivos a medias). En la BD se guarda la ruta relativa (`clientes/archivo.png`). Las imágenes antiguas en `uploads/` se siguen mostrando. Formatos: PNG, JPG, JPEG, GIF y BMP (se quitó WEBP porque JavaFX no lo puede mostrar).
- **Nueva excepción `ArchivoException`** (categoría `ARCHIVO`) para errores de acceso o almacenamiento de archivos.
- **`EmpleadoService` / `EmpleadoDao`**: regla de **único Administrador general activo**. Se agregó la opción *"Administrador general"* al combo de puestos (módulos Empleados y Administración).
- **`AlertaUtil`**: el título y el texto que ve el usuario se calculan en `tituloPara()` y `mensajeUsuario()` (se pueden probar sin abrir ventanas); nuevo título para errores de archivo.
- **`AppLogger`**: ruta de log configurable (las pruebas escriben en `logs/soptec_pruebas.log` y no ensucian `logs/soptec_errores.log`).
- **`Conexion`**: método `getUrlActual()` para verificar que las pruebas usen `soptec_it_test`.
- **`pom.xml`**: configuración de Surefire para que JUnit pueda ejecutar las clases de prueba con `module-info.java` (también al correr una sola clase).
- Las pruebas anteriores `CrudBaseIntegrationTest` y `FxmlSmokeTest` (usaban la BD de producción) se movieron a `_to_delete/`; su contenido quedó cubierto por las nuevas clases. Se puede borrar esa carpeta.

---

## 4. Guion sugerido para el video demostrativo

1. **Preparación (≈30 s):** XAMPP con MySQL encendido; en phpMyAdmin mostrar que existen `soptec_it` (producción) y `soptec_it_test` (pruebas).
2. **Ejecución (≈1 min):** en NetBeans, clic derecho al proyecto → *Test*. Mostrar la ventana *Test Results* con todas las pruebas en verde y la estructura de paquetes `crud`, `relaciones`, `imagenes`, `excepciones`, `funcional`.
3. **Reporte (≈30 s):** abrir `reportes/reporte_pruebas.html` en el navegador: totales y resumen por criterio.
4. **Log (≈30 s):** abrir `logs/soptec_pruebas.log` y señalar una línea `[ERROR]` con fecha, hora, `Modulo=`, `Operacion=`, `TipoExcepcion=` y `password=***`.
5. **Escenarios de errores controlados (mínimo 3):**
   - **A. Validación:** en la app, módulo *Logs y Pruebas* → *Simular error de validación* → mensaje claro y nueva línea `TipoExcepcion=VALIDACION` en el log.
   - **B. Rollback:** *Simular rollback multi-tabla* → mensaje de que se revirtieron los cambios; en phpMyAdmin comprobar que el cliente no se guardó.
   - **C. Caída de MySQL:** marcar *Simular caída de conexión*, navegar a otro módulo (la app no se cierra), desmarcar y *Verificar conexión* → la app se recupera.
   - **D. Imagen con formato incorrecto:** en *Clientes* → *Elegir imagen*, elegir un `.txt` renombrado a `.png` → mensaje *"no es una imagen válida"*.
   - **E. Carpeta en Documentos:** borrar `Documentos\SOPTEC-IT`, elegir una imagen válida en la app y mostrar que la carpeta se volvió a crear.
   - **F. (Opcional) MySQL apagado en las pruebas:** detener MySQL en XAMPP y volver a correr *Test* → las pruebas de BD aparecen omitidas con el motivo, el resto pasa.
6. **Cierre (≈15 s):** resumen de criterios cubiertos.
