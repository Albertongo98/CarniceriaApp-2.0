# CarniceriaApp 2.0 — La Palma Carnicería

App Android (Kotlin + Jetpack Compose) de **punto de venta / estación de despacho** para una carnicería. Corre en tablet (también en teléfono), 100% offline, e imprime en una impresora térmica **POS-58 (58 mm) por Bluetooth Classic (RFCOMM, ESC/POS)**.

Stack: Kotlin 2.0, Compose (Material3), MVVM, Hilt, Room, DataStore, Coroutines/Flow. `minSdk 24`, `compileSdk/targetSdk 34`, JVM target 11. Paquete `com.example.carniceriaapp20`.

## Qué puede hacer la app

Navegación (`ui/navigation/AppNavigation.kt`): Splash → TPV; todo lo demás sale del menú ⋮ del TPV.

### TPV / Despacho (`ui/screens/tpv`)
- **Multiticket**: varias pestañas de ticket abiertas a la vez (sin límite fijo); botón `+` para abrir, `X` para cerrar (botón grande a propósito, para no cerrar por accidente).
- **Catálogo** agrupado por departamento (colapsable), búsqueda por nombre o código. Al buscar/agregar se usa el teclado QWERTY; al seleccionar un renglón, el numérico (**teclado inteligente** dinámico, alto fijo 380 dp en tablet).
- **Productos más vendidos del día** (top 10 desde las 00:00 locales) como acceso rápido.
- Dos tipos de venta (`ProductUnit`): **UNIDAD** (piezas, +/− en el renglón) y **GRANEL** (peso en kg). En GRANEL se puede teclear la **cantidad (kg)** o el **importe ($)**; con importe manual la cantidad se calcula `importe / precio` y el renglón muestra kg y total (no la palabra "Manual").
- **Montos rápidos** y **piezas estimadas (+PZ)**: en productos a granel se anota cuántas piezas físicas pidió el cliente (ej. 3 chiles) para el conteo en despacho.
- **Diálogo de confirmación** centrado (hoja de verificación para validar con el cliente) → **FINALIZAR** guarda el ticket **y luego** imprime. Si la impresión falla, el ticket ya quedó guardado (se reimprime desde Historial o con el botón de reimprimir último).
- **Folio diario** (`001`, `002`… se reinicia cada día local), calculado con `countTicketsOfDay + 1`.
- Configurar impresora (diálogo en el menú del TPV: lista de dispositivos Bluetooth emparejados; la MAC se guarda en DataStore).

### Historial (`ui/screens/history`)
Lista de todos los tickets guardados; tocar = vista previa; mantener presionado = modo selección; **imprimir selección en lote** (para auditoría, con 1 s entre tickets).

### Reportes (`ui/screens/reports`)
Reporte de ventas por **rango de fechas de hasta 31 días** (`MAX_RANGE_DAYS`, cubre cualquier mes calendario). Filtros: chips **Hoy / 7 días / Este mes / Mes anterior** y botones **Desde / Hasta** (el "Hasta" solo permite hasta 31 días después del "Desde" y nunca fechas futuras; si el rango queda inválido el ViewModel lo ajusta). Muestra: total, **tickets, ticket promedio, promedio por día con venta**, **ventas por día** (tickets y total de cada día), **resumen por departamento separando piezas y kilos** (nunca se suman entre sí) y desglose por producto. Acciones: **imprimir corte de caja** (ticket térmico; con periodo, tickets y ventas por día si el rango es de varios días) y **descargar HTML** (SAF, `CreateDocument`; `reporte_yyyyMMdd_yyyyMMdd.html`) con la misma información, pensado como reporte mensual.

### Productos (`ui/screens/products`)
Lista con búsqueda (nombre/código), chips de código, **departamento** y unidad; alta/edición/baja. En el formulario el departamento es un **combo autocompletable** con los departamentos ya existentes (también permite escribir uno nuevo). Un departamento no es entidad propia: es el texto `Product.department`.

### Etiquetas (`ui/screens/generador`)
Formulario nombre/precio/código → imprime N copias en un solo envío Bluetooth; historial de etiquetas para reimprimir.

### Actualizar base de datos por CSV (`ui/screens/update`)
- **Exportar** catálogo a CSV (`codigo,nombre,precio,departamento,unidad`).
- **Importar** reemplaza el catálogo en **una transacción** (`ProductRepository.replaceAllProducts`: *upsert* por código y borra solo los códigos que ya no vienen; si algo falla no cambia nada). Formatos reconocidos por el encabezado: *export* (el anterior, separado por comas) y *external* (11+ columnas del sistema del negocio: código en col. 1, nombre 2, precio 4 con `$`/comas, departamento 6, tipo de venta 10). Soporta campos entre comillas (nombres con coma) y el BOM de Excel; la pantalla informa cuántas líneas se **omitieron** (precio/unidad inválidos, incompletas o código repetido). La exportación escapa comillas/comas, así que exportar→importar no pierde datos.

## Ticket impreso (`util/BluetoothPrinterHelper.buildTicketData`)
32 columnas, charset **ISO-8859-1**. Orden: logo de texto "LA PALMA CARNICERIA" → fecha → por producto: nombre (máx. 20) + código de 4 dígitos, **banner `>> N PIEZAS <<` en tamaño doble si es UNIDAD con cantidad ≥ 2** (para que el cajero que escanea vea la cantidad al instante), línea cantidad/monto, **CODE128 del producto** (GRANEL: `generarCodigoParaPOS`; UNIDAD: el código) → TOTAL grande → Folio → **CODE128 de control `HHmmss-folio`** (sin texto HRI, alto 70; lo lee el sistema de control de tickets) → "¡GRACIAS POR SU COMPRA!".

## Reglas críticas (no negociables)

### Datos
1. **Nunca usar `fallbackToDestructiveMigration()`.** Cualquier cambio de esquema = subir `version` en `CarniceriaDatabase` + agregar `Migration(n, n+1)` en `data/local/DatabaseMigrations.kt` + registrarla en `DatabaseModule` + **commitear `app/schemas/**`** (`exportSchema = true`). Sin migración la app crashea al abrir a propósito: es mejor eso que borrar productos y ventas en silencio. Versión actual: **4** (migraciones 1→2 `daily_folio`, 2→3 `estimated_pieces`, 3→4 `product_department`).
2. **No borrar tickets automáticamente.** Reportes e Historial dependen de todo el historial. (Existió una purga a 48 h y era un bug de pérdida de datos.)
3. **Un solo `UserPreferencesRepository`** para DataStore (`settings`, clave `printer_mac_address`). No crear otro `preferencesDataStore(name = "settings")`: dos instancias sobre el mismo archivo pueden crashear.
4. **`ticket_items` no guarda la unidad de venta.** Los reportes la obtienen con `LEFT JOIN products` y puede venir `null` (producto ya no existe tras reimportar el CSV). Usar siempre `ProductSalesReport.effectiveUnit` (null → cantidad fraccionaria = GRANEL, entera = UNIDAD). **Nunca sumar piezas con kilos** en una sola cifra.
5. El importe de dinero sí se suma entre unidades; las cantidades físicas no.
5b. **Nunca hacer `DELETE FROM products` ni `INSERT OR REPLACE` masivo sobre `products`.** Room activa `PRAGMA foreign_keys = ON` y `ticket_items.product_code` tiene `ON DELETE SET_NULL`: borrar/reemplazar un producto deja en `NULL` el código de todas sus ventas históricas (rompe la unidad en reportes y los códigos de barras al reimprimir tickets viejos). Para cambiar el catálogo usar `@Upsert` y borrar solo lo que sobra (ver `replaceAllProducts`). Las ventas anteriores a este arreglo pueden tener `product_code = NULL` (irrecuperable salvo por nombre).

### Formato y fechas
6. **Todo `String.format`/`"%.3f".format`/`NumberFormat` con `Locale` explícito** (`Locale.forLanguageTag("es-MX")`). El dispositivo real usa coma decimal y sin locale `30.567 kg` se ve como `30,567`.
7. **`DatePicker` de Material3 trabaja en UTC**; `Calendar`/`SimpleDateFormat` del resto de la app en zona local. Convertir explícitamente (`localMidnightToUtcPickerMillis` / `utcPickerMillisToLocalMidnight` en `ReportsScreen.kt`) o el día sale desfasado. Lo mismo aplica a los límites de `SelectableDates`. Los rangos se guardan como medianoche local y la consulta usa `[inicio, fin + 1 día)` (fin **exclusivo**, `<`).

### Impresión Bluetooth (no tocar sin probar en la impresora física)
8. Todo pasa por `BluetoothPrinterHelper` (singleton, `Mutex`): una impresión a la vez. Parámetros probados: `connect` con timeout 10 s, espera 500 ms tras conectar, **chunks de 256 bytes con 20 ms** entre chunks, 1000 ms antes de cerrar el socket, **3 reintentos**. Sockets siempre cerrados en el `catch`; cada intento fallido se registra con `Log.e`.
9. Códigos de barras: comando `GS k` tipo **73 (CODE128)** con prefijo `{B`; sin HRI en el de control.
10. **Funciones sagradas** (`util/BarcodeHelper.kt`): `generarCodigoParaPOS` = `"200" + código(4, padStart '0') + centavos(5, padStart '0') + "5"`; los centavos se **redondean** (`roundToInt`), no se truncan. No duplicar esta función en otro archivo (hubo una copia privada con `.toInt()` que dejaba el precio 1 centavo abajo).
11. **No quitar del ticket**: el CODE128 de control `HHmmss-folio`, el banner de piezas ≥ 2 ni el CODE128 por producto — los usa el sistema externo de control. Después de imprimir un código de barras centrado hay que volver a `CMD_ALIGN_LEFT`. **No envolver el armado de códigos de barras en `try/catch` que ignore el error**: si falla, la impresión debe devolver `PrintResult.Error` (un ticket sin código de control es peor que un error). Solo se omite el código de un renglón cuando el producto no tiene código (ventas viejas).
12. El botón principal dice **FINALIZAR** y siempre pasa por el diálogo de confirmación antes de imprimir: esto es una estación de despacho, no una caja registradora.

### Arquitectura
13. UI 100% Compose; MVVM estricto (Composable → ViewModel → Repository → DAO); dependencias por Hilt; BD/impresión/IO siempre en coroutines. Los repositorios se enlazan en `RepositoryModule`; la base de datos y los DAOs en `DatabaseModule`. Las clases con `@Singleton @Inject constructor` (`BluetoothPrinterHelper`, `UserPreferencesRepository`) **no necesitan módulo**; el calificador va en el *parámetro* del constructor (`@ApplicationContext private val context: Context`), **no** `@field:ApplicationContext` (con `@field:` Hilt no encuentra el `Context`).
14. En `ViewModel`s con `stateIn(..., WhileSubscribed(5000), ...)` el `.value` no se actualiza hasta que alguien colecciona. `TpvViewModel.uiState` además usa `flowOn(Dispatchers.Default)`: en tests esperar con `uiState.first { condición }`, nunca leer `.value` justo después de una acción.

## Compilar y probar
- Windows. En esta máquina el `JAVA_HOME` por defecto (`C:\Program Files\Android\Android Studio\jbr`) está corrupto (sin `java.exe`); usar `JAVA_HOME="C:\Program Files\Android\Android Studio1\jbr"`.
- Compilar: `./gradlew.bat :app:compileDebugKotlin` — Tests: `./gradlew.bat :app:testDebugUnitTest` (JUnit + mockito-kotlin + coroutines-test; deben pasar todos antes de un commit).
- No commitear cambios locales de `.idea/deploymentTargetSelector.xml` ni `local.properties`.

## Deuda técnica conocida
- **`ticket_items` no guarda la unidad de venta**: se infiere en 3 lugares (`ProductSalesReport.effectiveUnit`, `HistoryViewModel`, reimpresión del TPV). Arreglo de raíz: columna `unit` (Migration 4→5 con relleno de ventas viejas) y borrar las heurísticas y el `LEFT JOIN`. El reimpreso desde Historial también pierde el departamento.
- El armado del ticket (`buildTicketData`) es privado y no tiene tests: extraerlo a una clase pura con tests (código de control, banner de piezas, alineación).
- No hay CI (GitHub Actions con `compileDebugKotlin` + `testDebugUnitTest`). No hay test de migraciones de Room (requiere `androidTest` con dispositivo) ni del `replaceAllProducts` contra SQLite real.
- `TpvScreen.kt` es un archivo enorme con líneas larguísimas; partirlo en composables.
- `isMinifyEnabled = false` en release. Nombre del negocio ("LA PALMA CARNICERIA") escrito a mano en tickets y reportes.
- `app/productos.csv` (catálogo real con costos) es solo local y está en `.gitignore`; sigue en el historial git anterior a `81a83de` (decisión del dueño: no es problema).
