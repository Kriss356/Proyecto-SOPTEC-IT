# Práctica: Módulos CRUD con gestión de excepciones y tolerancia a fallos — SOPTEC-IT

Actividad colaborativa del proyecto cuatrimestral · 20/11/2026

**Resultado de las pruebas:** 144 pruebas JUnit 5 aprobadas y 0 fallidas, ejecutadas con MySQL real sobre `soptec_it_test`. Además, las 36 vistas FXML cargan con su controlador.

---

## 1. Base de datos

Las tablas nuevas **se crean solas** la primera vez que abres el sistema (`MigracionBD`, sin borrar información).
Si prefieres hacerlo a mano en phpMyAdmin, importa `database/soptec_it_ventas_servicios.sql`.

| Tabla | Tipo | Para qué |
|---|---|---|
| `servicio` | catálogo (con imagen) | Servicios que ofrece SOPTEC-IT: levantamiento, nodo Cat6, fusión de fibra, etc. |
| `cita_servicio` | **M:N** cita ↔ servicio | Servicios que se harán en cada cita (con cantidad y precio). |
| `venta` | encabezado | Venta o factura del cliente, con proyecto y cita opcionales; subtotal, IVA, total y estado. |
| `venta_servicio` | **M:N** venta ↔ servicio | Servicios vendidos en cada venta. |
| `pago` *(modificada)* | 1:N | Ahora tiene `id_venta` y `metodo_pago`: abonos de cada venta. |
| `orden_compra` *(modificada)* | 1:N | Ahora tiene `id_proveedor` y `estado` (Solicitada / Recibida / Cancelada). |

Las relaciones M:N que ya existían y ahora tienen formulario son `proyecto_empleado`, `material_proveedor`, `proyecto_material` y `orden_compra_material`.

`database/soptec_it_datos_prueba.sql` también carga servicios, citas con servicios, 2 ventas con pagos, una orden de compra y material asignado a proyectos.

---

## 2. Formularios: CRUD y relaciones muchos a muchos

En todos los formularios la información relacionada se **selecciona** de listas desplegables, sin volver a capturarla, y la parte muchos a muchos se guarda **de una sola vez**.

| Módulo › Pestaña | Relación | Qué se hace |
|---|---|---|
| **Citas** | cita + `cita_servicio` | Agendar, consultar, actualizar, desactivar o eliminar una cita **con sus servicios** (combo del catálogo + cantidad + tabla). La cita y sus servicios se guardan en una transacción. |
| **Ventas › Ventas y facturación** | venta + `venta_servicio` + `pago` | Se elige el cliente y se cargan solos sus proyectos y sus citas. Con **"Cargar"** se copian los servicios de una cita a la venta. Se registra la venta con IVA y un pago inicial opcional. También se puede actualizar, cancelar (baja lógica), eliminar y ver o imprimir la factura. |
| **Ventas › Catálogo de servicios** | `servicio` | CRUD completo con **imagen** (se guarda en `Documentos\SOPTEC-IT\imagenes\servicios`). |
| **Administración › Personal por proyecto** | `proyecto_empleado` | Se elige un proyecto, se marca con casillas a todo el personal y se guarda el equipo completo. |
| **Administración › Cuentas por cobrar** | `pago` ↔ `venta` | Saldos reales, historial de pagos por venta, registrar abono o liquidar y eliminar un pago. El estado se recalcula solo: Pendiente, Parcial o Pagada. |
| **Compras-Almacén › Órdenes de compra** | orden + `orden_compra_material` | Orden con varias partidas. **Registrar recepción** suma el material al almacén. También se puede cancelar, eliminar o editar mientras está *Solicitada*. Al elegir proveedor, primero aparece el material que surte. |
| **Compras-Almacén › Material por proveedor** | `material_proveedor` | Se marca con casillas el material que vende cada proveedor. |
| **Compras-Almacén › Material por proyecto** | `proyecto_material` | Se asigna material con cantidad a un proyecto. La diferencia se descuenta del almacén, o regresa si se quita. |
| **Compras-Almacén › Entradas y salidas** | `proyecto_material` | Una **salida a un proyecto** ahora queda registrada en el material del proyecto, en la misma transacción. |

---

## 3. Excepciones y tolerancia a fallos

**Transacciones con rollback.** Todas las operaciones que tocan varias tablas usan `setAutoCommit(false)` y luego `commit()`. Si algo falla, se ejecuta `rollback()`:

- **Cita + servicios:** si falla, no queda la cita ni ninguno de sus servicios (`CitaDao.crearConServicios` y `actualizarConServicios`).
- **Venta + servicios + pago:** si falla en cualquier punto, no queda la venta, ni sus servicios, ni el pago (`VentaDao.registrar`).
- **Abono:** el pago y el nuevo estado de la venta se guardan juntos. No se acepta un pago mayor al saldo.
- **Equipo del proyecto, material del proveedor, material del proyecto** (este último con existencias), **orden de compra con sus partidas** y **recepción de la orden** (con existencias).
- `Conexion.cerrarSilencioso` ahora revierte cualquier transacción que haya quedado abierta antes de cerrar la conexión. Antes, `setAutoCommit(true)` la confirmaba y podía dejar registros parciales.

**Errores diferenciados.** Cada tipo de error tiene su propia excepción, y la categoría queda en el campo `TipoExcepcion` del log:

| Tipo | Excepción | Título que ve el usuario |
|---|---|---|
| Validación (campos, formatos, duplicados, saldo, stock) | `ValidacionException` → `VALIDACION` | Datos inválidos o incompletos |
| Conexión con MySQL | `ConexionBDException` → `CONEXION_BD` | Sin conexión con la base de datos |
| Integridad referencial (registros relacionados, FK 1451/1452) | `IntegridadReferencialException` → `INTEGRIDAD_REFERENCIAL` | Operación bloqueada por registros relacionados |
| Almacenamiento de archivos / imágenes | `ArchivoException` → `ARCHIVO` | Problema con el archivo de imagen |
| Operaciones de base de datos / transacciones | `OperacionBDException` → `OPERACION_BD` | Error al procesar la operación |
| Rollback ejecutado | (aviso) → `TRANSACCION_REVERTIDA` | — (solo en el log) |

**Recuperación.**

- Cada botón tiene su propio `try/catch` y muestra un mensaje claro, sin SQL ni trazas, que además dice qué hacer.
- `App` tiene un manejador global: cualquier error no previsto queda en el log y el usuario ve un aviso, pero **la ventana no se cierra**.
- Si un módulo no carga, el menú principal muestra un aviso y el resto del sistema sigue funcionando.

**Log (`logs/soptec_errores.log`).** Una línea por evento con fecha y hora, nivel (`ERROR`, `WARN` o `INFO`), módulo, proceso, operación, tipo de excepción y detalle técnico (SQLState, código de MySQL y causa). Las contraseñas y los parámetros JDBC se enmascaran (`password=***`). Se puede consultar desde **Logs y pruebas** (Administrador general).

Ejemplo de lo que queda si falla una venta:

```
[2026-11-20 08:15:02] [WARN] | Modulo=Ventas | Proceso=Conexion | Operacion=REGISTRAR_VENTA | TipoExcepcion=TRANSACCION_REVERTIDA | Detalle=Se ejecuto ROLLBACK: ninguna de las tablas involucradas quedo con registros parciales.
[2026-11-20 08:15:02] [ERROR] | Modulo=Ventas | Proceso=VentaDao | Operacion=REGISTRAR_VENTA | TipoExcepcion=INTEGRIDAD_REFERENCIAL | Detalle=Restriccion FK al referenciar padre inexistente (error 1452) | Causa: SQLIntegrityConstraintViolationException[SQLState=23000, Code=1452]: ...
```

---

## 4. Pruebas (JUnit 5, carpeta `src/test/java/.../pruebas`)

Clases nuevas:

| Clase | Qué comprueba |
|---|---|
| `crud/ServicioCrudTest` | Registrar, consultar, buscar, actualizar, desactivar, reactivar y eliminar un servicio; que no haya duplicados ni precio 0; que no se pueda borrar un servicio en uso. |
| `crud/CitaServiciosCrudTest` | Cita con 2 servicios: alta, consulta, cambio de servicios y eliminación. Rechaza servicios repetidos, cantidades en 0 y servicios inexistentes. |
| `crud/VentaCrudTest` | Venta con servicios y pago: IVA, total, folio y estado; abonos hasta liquidar; que no se pueda pagar de más; actualizar servicios; venta desde una cita; cancelar y eliminar bloqueados si hay pagos. |
| `crud/RelacionesMuchosAMuchosTest` | Personal por proyecto, material por proveedor, material por proyecto (existencias) y órdenes de compra (recepción y reglas de borrado). |
| `relaciones/TransaccionesVentaCitaTest` | **Rollback** de cita + servicios y de venta + servicios + pago, tanto con fallo simulado como con un error **real** de MySQL. Comprueba que no quedan pagos huérfanos y que la operación se puede reintentar. |
| `excepciones/ErroresVentasCitasLogTest` | Los 4 tipos de error aparecen diferenciados en el log con todos los campos, la contraseña enmascarada y mensajes claros, y el sistema se recupera. |

Para ejecutarlas: enciende MySQL en XAMPP y en NetBeans haz clic derecho al proyecto → **Test**. Se generan `reportes/reporte_pruebas.html` y `logs/soptec_pruebas.log`.

### Guion sugerido para probar a mano (o para el video)
1. Importa `database/soptec_it_datos_prueba.sql`. Entra como **Administrador general**.
2. **Citas:** elige a *Sofía Ramírez*, agrega 2 servicios, guarda, y luego selecciónala, cambia una cantidad y actualiza.
3. **Ventas › Ventas y facturación:** elige a *Sofía*, carga los servicios de su cita, pon un pago inicial y registra. Ábrela con doble clic para ver la factura.
4. **Administración › Cuentas por cobrar:** registra un abono y después usa "Poner el saldo completo" para liquidarla.
5. **Errores controlados:**
   - Intenta pagar más del saldo (validación).
   - Elimina un servicio que ya está en una cita (integridad).
   - Apaga MySQL en XAMPP y pulsa *Refrescar* (conexión).
   - En *Material por proyecto*, pide más material del que hay (no se guarda nada).
   
   Después abre **Logs y pruebas** para ver cada error registrado.
