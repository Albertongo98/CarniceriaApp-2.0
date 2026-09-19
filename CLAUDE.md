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
Reporte de ventas por **fecha** (DatePicker): total del día, **resumen por departamento separando piezas y kilos** (nunca se suman entre sí), desglose por producto. Acciones: **imprimir corte de caja** (ticket térmico) y **descargar HTML** (SAF, `CreateDocument`).

### Productos (`ui/screens/products`)
Lista con búsqueda (nombre/código), chips de código, **departamento** y unidad; alta/edición/baja. En el formulario el departamento es un **combo autocompletable** con los departamentos ya existentes (también permite escribir uno nuevo). Un departamento no es entidad propia: es el texto `Product.department`.

### Etiquetas (`ui/screens/generador`)
Formulario nombre/precio/código → imprime N copias en un solo envío Bluetooth; historial de etiquetas para reimprimir.

### Actualizar base de datos por CSV (`ui/screens/update`)
- **Exportar** catálogo a CSV (`codigo,nombre,precio,departamento,unidad`).
- **Importar** reemplaza **todo** el catálogo (`deleteAllProducts` + `insertProducts`). Formatos reconocidos por el encabezado: *export* (el anterior, separado por comas) y *external* (11+ columnas del sistema del negocio: código en col. 1, nombre 2, precio 4 con `$`/comas, departamento 6, tipo de venta 10; ver `app/productos.csv`). Líneas inválidas se omiten.

## Ticket impreso (`util/BluetoothPrinterHelper.buildTicketData`)
32 columnas, charset **ISO-8859-1**. Orden: logo de texto "LA PALMA CARNICERIA" → fecha → por producto: nombre (máx. 20) + código de 4 dígitos, **banner `>> N PIEZAS <<` en tamaño doble si es UNIDAD con cantidad ≥ 2** (para que el cajero que escanea vea la cantidad al instante), línea cantidad/monto, **CODE128 del producto** (GRANEL: `generarCodigoParaPOS`; UNIDAD: el código) → TOTAL grande → Folio → **CODE128 de control `HHmmss-folio`** (sin texto HRI, alto 70; lo lee el sistema de control de tickets) → "¡GRACIAS POR SU COMPRA!".

## Reglas críticas (no negociables)

### Datos
1. **Nunca usar `fallbackToDestructiveMigration()`.** Cualquier cambio de esquema = subir `version` en `CarniceriaDatabase` + agregar `Migration(n, n+1)` en `data/local/DatabaseMigrations.kt` + registrarla en `DatabaseModule` + **commitear `app/schemas/**`** (`exportSchema = true`). Sin migración la app crashea al abrir a propósito: es mejor eso que borrar productos y ventas en silencio. Versión actual: **4** (migraciones 1→2 `daily_folio`, 2→3 `estimated_pieces`, 3→4 `product_department`).
2. **No borrar tickets automáticamente.** Reportes e Historial dependen de todo el historial. (Existió una purga a 48 h y era un bug de pérdida de datos.)
3. **Un solo `UserPreferencesRepository`** para DataStore (`settings`, clave `printer_mac_address`). No crear otro `preferencesDataStore(name = "settings")`: dos instancias sobre el mismo archivo pueden crashear.
4. **`ticket_items` no guarda la unidad de venta.** Los reportes la obtienen con `LEFT JOIN products` y puede venir `null` (producto ya no existe tras reimportar el CSV). Usar siempre `ProductSalesReport.effectiveUnit` (null → cantidad fraccionaria = GRANEL, entera = UNIDAD). **Nunca sumar piezas con kilos** en una sola cifra.
5. El importe de dinero sí se suma entre unidades; las cantidades físicas no.

### Formato y fechas
6. **Todo `String.format`/`"%.3f".format`/`NumberFormat` con `Locale` explícito** (`Locale.forLanguageTag("es-MX")`). El dispositivo real usa coma decimal y sin locale `30.567 kg` se ve como `30,567`.
7. **`DatePicker` de Material3 trabaja en UTC**; `Calendar`/`SimpleDateFormat` del resto de la app en zona local. Convertir explícitamente (`localMidnightToUtcPickerMillis` / `utcPickerMillisToLocalMidnight` en `ReportsScreen.kt`) o el día sale desfasado.

### Impresión Bluetooth (no tocar sin probar en la impresora física)
8. Todo pasa por `BluetoothPrinterHelper` (singleton, `Mutex`): una impresión a la vez. Parámetros probados: `connect` con timeout 10 s, espera 500 ms tras conectar, **chunks de 256 bytes con 20 ms** entre chunks, 1000 ms antes de cerrar el socket, **3 reintentos**. Sockets siempre cerrados en el `catch`.
9. Códigos de barras: comando `GS k` tipo **73 (CODE128)** con prefijo `{B`; sin HRI en el de control.
10. **Funciones sagradas** (`util/BarcodeHelper.kt`): `generarCodigoParaPOS` = `"200" + código(4, padStart '0') + centavos(5, padStart '0') + "5"`; los centavos se **redondean** (`roundToInt`), no se truncan. No duplicar esta función en otro archivo (hubo una copia privada con `.toInt()` que dejaba el precio 1 centavo abajo).
11. **No quitar del ticket**: el CODE128 de control `HHmmss-folio`, el banner de piezas ≥ 2 ni el CODE128 por producto — los usa el sistema externo de control. Después de imprimir un código de barras centrado hay que volver a `CMD_ALIGN_LEFT`.
12. El botón principal dice **FINALIZAR** y siempre pasa por el diálogo de confirmación antes de imprimir: esto es una estación de despacho, no una caja registradora.

### Arquitectura
13. UI 100% Compose; MVVM estricto (Composable → ViewModel → Repository → DAO); dependencias por Hilt; BD/impresión/IO siempre en coroutines. Los repositorios se enlazan en `RepositoryModule`.
14. En `ViewModel`s con `stateIn(..., WhileSubscribed(5000), ...)` el `.value` no se actualiza hasta que alguien colecciona. `TpvViewModel.uiState` además usa `flowOn(Dispatchers.Default)`: en tests esperar con `uiState.first { condición }`, nunca leer `.value` justo después de una acción.

## Compilar y probar
- Windows. En esta máquina el `JAVA_HOME` por defecto (`C:\Program Files\Android\Android Studio\jbr`) está corrupto (sin `java.exe`); usar `JAVA_HOME="C:\Program Files\Android\Android Studio1\jbr"`.
- Compilar: `./gradlew.bat :app:compileDebugKotlin` — Tests: `./gradlew.bat :app:testDebugUnitTest` (JUnit + mockito-kotlin + coroutines-test; deben pasar todos antes de un commit).
- No commitear cambios locales de `.idea/deploymentTargetSelector.xml` ni `local.properties`.

## Deuda técnica conocida
- Import CSV sin transacción (`deleteAll` + insert): si truena a mitad, el catálogo queda vacío; y el parser no soporta comillas/comas dentro de campos.
- `catch (e: Exception) {}` vacíos (impresión, import CSV) sin log.
- `SettingsScreen`/`SettingsViewModel`/`Routes.SETTINGS` **no son alcanzables** desde ningún menú (la MAC se configura en el diálogo del TPV); código sin uso: `ReportsViewModel.createDemoData/generateReportText`, `ReportExporter.saveAndShareFile`, `generarCodigoControlInterno` (formato distinto al que realmente se imprime).
- `isMinifyEnabled = false` en release; `app/productos.csv` (precios de costo del negocio) está versionado.
- Nombre del negocio ("LA PALMA CARNICERIA") escrito a mano en tickets y reportes.
