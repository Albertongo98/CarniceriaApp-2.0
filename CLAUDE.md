# CarniceriaApp 2.0 — La Palma Carnicería

App Android (Kotlin + Jetpack Compose) de **punto de venta / estación de despacho** para una carnicería. Corre en tablet (también en teléfono), 100% offline, e imprime en una impresora térmica **POS-58 (58 mm) por Bluetooth Classic (RFCOMM, ESC/POS)**.

Stack: Kotlin 2.0, Compose (Material3), MVVM, Hilt, Room, DataStore, Coroutines/Flow; procesadores con **KSP** (no kapt). `minSdk 24`, `compileSdk/targetSdk 34`, JVM target 11. Namespace/paquete `com.example.carniceriaapp20` (solo el código); **`applicationId` = `com.lapalma.carniceria`** (es la identidad de la app instalada: cambiarlo crea otra app con datos separados). `versionCode`/`versionName` en `app/build.gradle.kts`: **súbelos en cada versión que se instale**.

## Qué puede hacer la app

Navegación (`ui/navigation/AppNavigation.kt`): la app arranca **directo en el TPV** (sin pantalla de carga propia; solo la pantalla de arranque del sistema, `installSplashScreen()` + `Theme.App.Starting` con el logo, que no agrega espera); todo lo demás sale del menú ⋮ del TPV: Gestionar Productos, Historial, Reportes 🔒, Etiquetas, Actualizar Base de Datos 🔒, Configurar Impresora, Respaldo y diagnóstico 🔒, Seguridad (PIN) 🔒. (🔒 = pide el PIN si hay uno configurado.)

### TPV / Despacho (`ui/screens/tpv`, partido por responsabilidad: `TpvScreen`, `TpvCatalogPanel`, `TpvTicketPanel`, `TpvKeyboardPanels`, `TpvDialogs`)
- **Multiticket**: varias pestañas de ticket abiertas a la vez (sin límite fijo); botón `+` para abrir, `X` para cerrar (botón grande a propósito, para no cerrar por accidente).
- **Catálogo** agrupado por departamento (colapsable), búsqueda por nombre o código. Al buscar/agregar se usa el teclado QWERTY; al seleccionar un renglón, el numérico (**teclado inteligente** dinámico, alto fijo 380 dp en tablet). Los productos con existencia controlada muestran **BAJO / SIN EXISTENCIA** (solo aviso, no bloquea la venta).
- **Productos más vendidos del día** (top 10 desde las 00:00 locales, sin contar anulados) como acceso rápido.
- Dos tipos de venta (`ProductUnit`): **UNIDAD** (piezas, +/− en el renglón) y **GRANEL** (peso en kg). En GRANEL se puede teclear la **cantidad (kg)** o el **importe ($)**; con importe manual la cantidad se calcula `importe / precio` y el renglón muestra kg y total.
- **Montos rápidos** y **piezas estimadas (+PZ)**: en productos a granel se anota cuántas piezas físicas pidió el cliente (ej. 3 chiles) para el conteo en despacho.
- **Diálogo de confirmación** centrado (hoja de verificación para validar con el cliente) → **FINALIZAR** guarda el ticket **y luego** imprime. Todos los tickets se entregan impresos; si la impresión falla el ticket ya quedó guardado y se reimprime (botón "reimprimir último" o Historial). Guardar = ticket + renglones + descuento de existencias en **una transacción**.
- **Folio diario** (`001`, `002`… se reinicia cada día local), calculado con `countTicketsOfDay + 1` (cuenta también los anulados).
- **Aviso de respaldo**: al abrir el TPV avisa si nunca se respaldó o si hace ≥ 7 días.
- Configurar impresora (diálogo: dispositivos Bluetooth emparejados; la MAC se guarda en DataStore).

### Historial (`ui/screens/history`)
Lista de todos los tickets (los anulados se ven marcados); tocar = vista previa; mantener presionado = modo selección; **imprimir selección en lote** (auditoría, 1 s entre tickets); **reimprimir** un ticket lo imprime exactamente como se vendió (`TicketItem.toCartItem()`). **Anular ticket**: NO pide PIN (decisión del dueño), solo un **motivo obligatorio**; el ticket NO se borra, queda `voided_at`/`void_reason`, sale de reportes y "más vendidos", lo vendido regresa a existencias, y si se reimprime lleva `*** ANULADO ***` y el motivo.

### Reportes (`ui/screens/reports`) 🔒
Reporte de ventas por **rango de fechas de hasta 31 días** (`MAX_RANGE_DAYS`, cubre cualquier mes calendario). Filtros: chips **Hoy / 7 días / Este mes / Mes anterior** y botones **Desde / Hasta** (el "Hasta" solo permite hasta 31 días después del "Desde" y nunca fechas futuras; si el rango queda inválido el ViewModel lo ajusta). Muestra: total, **tickets, ticket promedio, promedio por día con venta**, **ventas por día**, **resumen por departamento separando piezas y kilos** (nunca se suman entre sí) y desglose por producto. Excluye tickets anulados. Acciones: **imprimir corte de caja** y **descargar HTML** (`reporte_yyyyMMdd_yyyyMMdd.html`) con la misma información.

### Productos (`ui/screens/products`)
Lista con búsqueda (nombre/código), chips de código, **departamento** y unidad, **existencia** (con "BAJO INVENTARIO" en rojo) filtro **Bajo inventario (N)** y botón **Imprimir lista de resurtido** (ticket con todos los productos en o bajo su mínimo, por departamento, con casilla `[ ]`, lo que hay, el mínimo y cuánto falta; `TicketPrintFormatter.buildRestockList`); alta/edición/baja (**borrar pide PIN** 🔒). El formulario: departamento con **combo autocompletable** (al guardar se reutiliza la escritura de un departamento existente sin importar mayúsculas/acentos y es obligatorio); precio con coma o punto decimal; **existencia** y **inventario mínimo** opcionales (existencia vacía = no se controla). Un departamento no es entidad propia: es el texto `Product.department`.

### Etiquetas (`ui/screens/generador`)
Formulario nombre/precio/código → imprime N copias en un solo envío Bluetooth; historial de etiquetas para reimprimir.

### Actualizar base de datos por CSV (`ui/screens/update`) 🔒
- **Exportar** catálogo a CSV (`codigo,nombre,precio,departamento,unidad,existencia,minimo`).
- **Importar** reemplaza el catálogo en **una transacción** (`ProductRepository.replaceAllProducts`: *upsert* por código y borra solo los códigos que ya no vienen; si algo falla no cambia nada). Formatos por encabezado: *export* (el anterior; existencia/mínimo opcionales) y *external* (11+ columnas del sistema del negocio: código col. 1, nombre 2, precio 4 con `$`/comas, departamento 6, existencia 7, inv. mínimo 8, tipo de venta 10). Soporta campos entre comillas y el BOM de Excel; unifica departamentos escritos distinto (la primera escritura gana; vacío → "Sin departamento"); informa cuántas líneas se **omitieron**.

### Respaldo y diagnóstico (`ui/screens/backup`) 🔒
- **Respaldar ahora**: exporta TODA la base (catálogo, ventas, etiquetas) como `.db` (`respaldo_carniceria_yyyyMMdd_HHmm.db`) por el selector de archivos. Guarda la fecha del último respaldo.
- **Restaurar un respaldo**: valida que sea SQLite de esta app (tablas y versión ≤ actual), guarda antes una copia de lo actual en `filesDir/backups/` (las últimas 3), reemplaza la base y **reinicia la app**. Respaldos de versiones anteriores se migran solos al abrir.
- **Exportar registro de errores** (`AppLog`): archivo local `filesDir/logs/app.log` (rota a 256 KB) con fallos de impresión, importación, respaldo y cierres inesperados.

### Seguridad (PIN)
PIN de 4–8 dígitos guardado como SHA-256 con sal (`PinManager`, DataStore). Sin PIN, todo está permitido. 5 fallos seguidos bloquean 30 s. Si se olvida, la única salida es borrar los datos de la app.

## Ticket impreso (`util/TicketPrintFormatter` — formato; `BluetoothPrinterHelper` — solo transporte)
32 columnas, charset **ISO-8859-1**. Orden: nombre del negocio (`AppConfig.BUSINESS_NAME`) → (si está anulado: `*** ANULADO ***` + motivo) → fecha → por producto: nombre (máx. 20) + código de 4 dígitos, **banner `>> N PIEZAS <<` en tamaño doble si es UNIDAD con cantidad ≥ 2** (para que el cajero que escanea vea la cantidad al instante), línea cantidad/monto, **CODE128 del producto** (GRANEL: `generarCodigoParaPOS`; UNIDAD: el código) → TOTAL grande → Folio → **CODE128 de control `HHmmss-folio`** (sin texto HRI, alto 70; lo lee el sistema de control de tickets) → "¡GRACIAS POR SU COMPRA!". El corte de caja y las etiquetas se arman en la misma clase.

## Reglas críticas (no negociables)

### Datos
1. **Nunca usar `fallbackToDestructiveMigration()`.** Cualquier cambio de esquema = subir `CARNICERIA_DB_VERSION` + agregar la migración en `data/local/DatabaseMigrations.kt` (como **lista de sentencias SQL**, ver `MIGRATION_4_5_STATEMENTS`) + registrarla en `DatabaseModule` + **commitear `app/schemas/**`** + **agregar/actualizar `MigrationV5Test`-style**: el test corre las sentencias contra SQLite real (sqlite-jdbc) y verifica que la estructura migrada sea IDÉNTICA a la que Room crea desde cero (si no, Room se niega a abrir la base en la tablet). Sin migración la app crashea al abrir a propósito. Versión actual: **5** (1→2 `daily_folio`; 2→3 `estimated_pieces`; 3→4 `product_department`; 4→5 unidad en `ticket_items`, `voided_at`/`void_reason` en `tickets`, `stock`/`min_stock` en `products`, índice `tickets.timestamp` y rescate de códigos de producto/departamento/unidad de ventas viejas).
2. **No borrar tickets** (ni automáticamente ni desde la UI): anular = marcar (`TicketRepository.voidTicket`). Reportes, Historial y el folio diario dependen del historial completo.
3. **Un solo `UserPreferencesRepository`** para DataStore (`settings`). No crear otro `preferencesDataStore(name = "settings")`.
4. **`ticket_items.unit` guarda la unidad con la que se vendió.** Reportes y reimpresiones usan ese dato; no se infiere ni se hace `JOIN` con el catálogo. **Nunca sumar piezas con kilos** en una sola cifra; el dinero sí se suma.
5. **Nunca `DELETE FROM products` ni `INSERT OR REPLACE` masivo sobre `products`.** Room activa `PRAGMA foreign_keys = ON` y `ticket_items.product_code` tiene `ON DELETE SET_NULL`: borrar/reemplazar un producto deja en `NULL` el código de sus ventas (verificado en `MigrationV5Test`). Usar `@Upsert` y borrar solo lo que sobra (`replaceAllProducts`).
6. **Las existencias solo se mueven dentro de las transacciones de `TicketRepositoryImpl`** (`saveTicket` resta, `voidTicket` devuelve) o desde el formulario del producto / la importación. `adjustStock` redondea a 3 decimales (kg).
7. **Todo cambio de catálogo o ventas que deba sobrevivir a un fallo va en `database.withTransaction`.**

### Formato y fechas
8. **Todo `String.format`/`"%.3f".format`/`NumberFormat` con `Locale` explícito** (`Locale.forLanguageTag("es-MX")`). El dispositivo real usa coma decimal. Para leer lo que teclea el usuario usar `parseDecimal` (acepta coma o punto); para mostrar cantidades `formatPlain`.
9. **`DatePicker` de Material3 trabaja en UTC**; el resto de la app en zona local. Convertir explícitamente (`localMidnightToUtcPickerMillis` / `utcPickerMillisToLocalMidnight`, también en los límites de `SelectableDates`). Los rangos son medianoche local y la consulta usa `[inicio, fin + 1 día)` (fin **exclusivo**).

### Impresión Bluetooth (no tocar sin probar en la impresora física)
10. Todo pasa por `BluetoothPrinterHelper` (singleton, `Mutex`): una impresión a la vez. Parámetros probados: `connect` con timeout 10 s, espera 500 ms tras conectar, **chunks de 256 bytes con 20 ms** entre chunks, 1000 ms antes de cerrar el socket, **3 reintentos**; cada fallo se registra con `AppLog`.
11. **Todo cambio al formato del ticket va en `TicketPrintFormatter` y debe mantener verdes los tests de `TicketPrintFormatterTest`** (verifican en bytes el código de control, códigos por producto, banner de piezas, alineación, kilos con punto y aviso de anulado). No quitar del ticket: el CODE128 de control `HHmmss-folio`, el banner de piezas ≥ 2 ni el CODE128 por producto (los usa el sistema externo de control); tras un código de barras centrado volver a `CMD_ALIGN_LEFT`.
12. **No envolver el armado de códigos de barras en `try/catch` que ignore el error**: si falla, `PrintResult.Error`. Solo se omite el código de un renglón cuando el producto no tiene código.
13. `generarCodigoParaPOS` (`util/BarcodeHelper.kt`) = `"200" + código(4) + centavos(5) + "5"` con centavos **redondeados** (`roundToInt`); no duplicarla.
14. El botón principal dice **FINALIZAR** y siempre pasa por el diálogo de confirmación antes de imprimir.

### Seguridad y errores
15. Las acciones sensibles pasan por `rememberPinGate().require { ... }` (y `pinGate.Dialog()` en la pantalla). No agregar acciones destructivas sin PIN (excepción decidida por el dueño: anular ticket, que no borra nada y exige motivo).
16. **No tragarse errores**: usar `AppLog.e(tag, mensaje, e)` (o mostrar el error al usuario). `catch` vacíos solo para limpieza de recursos, con comentario.

### Arquitectura
17. UI 100% Compose; MVVM estricto (Composable → ViewModel → Repository → DAO); Hilt; BD/impresión/IO en coroutines. Repositorios en `RepositoryModule`; BD y DAOs en `DatabaseModule`. Las clases `@Singleton @Inject constructor` (`BluetoothPrinterHelper`, `UserPreferencesRepository`, `PinManager`, `BackupManager`) **no necesitan módulo**; el calificador va en el *parámetro* (`@ApplicationContext private val context: Context`), **no** `@field:ApplicationContext`.
18. En `ViewModel`s con `stateIn(..., WhileSubscribed(5000), ...)` el `.value` no se actualiza hasta que alguien colecciona. `TpvViewModel.uiState` además usa `flowOn(Dispatchers.Default)`: en tests esperar con `uiState.first { condición }`.

## Compilar y probar
- Windows. En esta máquina el `JAVA_HOME` por defecto (`C:\Program Files\Android\Android Studio\jbr`) está corrupto (sin `java.exe`); usar `JAVA_HOME="C:\Program Files\Android\Android Studio1\jbr"`.
- Compilar: `./gradlew.bat :app:compileDebugKotlin` — Tests unitarios: `./gradlew.bat :app:testDebugUnitTest` (JUnit + mockito-kotlin + coroutines-test + sqlite-jdbc; **deben pasar todos antes de un commit**) — APK debug: `./gradlew.bat :app:assembleDebug`.
- **Pruebas en dispositivo** (Room real: migración 4→5 validada contra `5.json`, transacciones de venta/anulación, reemplazo de catálogo con claves foráneas): `./gradlew.bat :app:connectedDebugAndroidTest` con un emulador/tablet conectado. **Al terminar desinstala la app del dispositivo (se pierden sus datos)**; correrlas antes de tocar `DatabaseMigrations.kt`, `TicketRepositoryImpl` o `ProductRepositoryImpl`. El CI solo corre las unitarias (no tiene emulador).
- **APK release** (`./gradlew.bat :app:assembleRelease`, ~1 s de arranque, ~3 MB): usa R8 (`minify` + `shrinkResources`). Se firma con la clave de `keystore.properties` (raíz, en `.gitignore`, con `storeFile`, `storePassword`, `keyAlias`, `keyPassword`); si no existe, cae a la clave de depuración y el APK se instala igual (y actualiza sobre un debug). Para publicar o entregar de forma definitiva, crear una clave propia y **respaldarla** (sin ella no se pueden actualizar las apps instaladas). Tras cambiar dependencias o reglas, probar el release en el emulador/tablet: R8 puede romper reflexión.
- **CI**: `.github/workflows/ci.yml` corre tests + APK debug en cada push/PR a `main` y deja el APK como artefacto.
- Al escribir scripts de edición (Python) desde el shell, los `\n` de código Kotlin se corrompen: escribir el script con la herramienta de archivos, no con heredoc.
- No commitear cambios locales de `.idea/` ni `local.properties`.

## Arranque y rendimiento
- **No agregar pantallas de carga con `delay`.** Hubo una pantalla de Compose con `delay(2000)` fijo: sumaba 2 s siempre (medido en emulador: TPV listo en 5.6 s con ella vs 3.7 s sin ella con APK debug; ~1.0 s en APK release). La marca se mantiene con la pantalla de arranque del sistema, que dura solo lo que tarda la app.
- Medición: `TpvScreen` llama `ReportDrawnWhen { catálogo visible }`, así que logcat registra `ActivityTaskManager: Fully drawn …: +Xms` = tiempo hasta TPV con datos. Comando: `adb shell am force-stop <pkg>`, `adb logcat -c`, `adb shell am start -W -n com.lapalma.carniceria/com.example.carniceriaapp20.MainActivity`, esperar unos segundos y `adb logcat -d -s ActivityTaskManager:I | grep "Fully drawn"`.
- El volumen de datos casi no afecta el arranque (1,000 productos + ~65 mil renglones de venta: +0.1–0.2 s). El costo dominante es el arranque fijo de la app y **el APK debug es ~4 veces más lento que el release** (3.8 s vs 1.0 s en emulador): para uso real en la tablet instalar un build **release**, no el debug de Android Studio.
- Con `adb`/Git Bash en Windows usar `MSYS_NO_PATHCONV=1` o las rutas `/data/...` se reescriben. `run-as com.lapalma.carniceria` solo funciona con el APK debug.

## Deuda técnica conocida
- No hay tests de UI/Compose. Los flujos de PIN, respaldo/restauración, DatePicker y la impresión física solo se han verificado a mano (emulador; la impresora real, en la tablet).
- La migración se prueba únicamente de 4→5: no existen los esquemas exportados de las versiones 1–3 (se exportaron desde la v4). Toda migración nueva debe llevar su prueba en `MigrationInstrumentedTest` y en `MigrationV5Test`.
- `targetSdk 34` (subirlo exige revisar comportamientos de Android 15, p. ej. edge-to-edge; la tablet es Android 7 y no lo necesita).
- Sin firma de release propia configurada (ver arriba): crear la clave cuando se decida distribuir.
- `app/productos.csv` (catálogo real con costos) es solo local, en `.gitignore`; sigue en el historial git anterior a `81a83de` (decisión del dueño: no es problema).
