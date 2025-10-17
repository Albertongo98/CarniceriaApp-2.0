u# Directivas de Desarrollo para Carniceriapp v2.0

Este documento establece las reglas, principios y funciones críticas para el desarrollo de `Carniceriapp v2.0`. El objetivo es garantizar un desarrollo consistente, mantenible y de alta calidad, evitando los problemas de la versión anterior.

## 1. Reglas de Oro (No Negociables)

### 1.1. Arquitectura y Stack Tecnológico

1.  **UI 100% Jetpack Compose:** Todo el UI, desde la pantalla principal hasta el último diálogo, se construirá con Jetpack Compose. **No se utilizará XML para layouts bajo ninguna circunstancia.**
2.  **Arquitectura MVVM Estricta:**
    -   **Vistas (Composables):** Solo deben observar el estado del ViewModel y notificarle las acciones del usuario. No deben contener lógica de negocio.
    -   **ViewModels:** Exponen el estado de la UI a través de `StateFlow` y contienen la lógica de presentación. Se obtienen en los Composables con `hiltViewModel()`.
    -   **Repositorios:** Abstraen el origen de los datos (base de datos, red, etc.). Los ViewModels se comunican exclusivamente con los Repositorios, nunca directamente con los DAOs.
    -   **Modelos (Entidades Room):** Representan los datos de la aplicación.
3.  **Inyección de Dependencias con Hilt:** Todas las dependencias (ViewModels, Repositorios, DAOs, Database) serán gestionadas y provistas por Hilt. No se permite la instanciación manual de estas clases.
4.  **Coroutines y Flow para Asincronía:** Todas las operaciones de larga duración (lectura/escritura de BD, impresión) deben ejecutarse en coroutines, utilizando el despachador adecuado (`Dispatchers.IO` para BD, `Dispatchers.Main` para UI).

## 2. Funciones Críticas (Sagradas e Intocables)

Las siguientes funciones contienen lógica de negocio esencial que ha sido depurada y validada. **No deben ser modificadas sin un análisis de impacto exhaustivo y aprobación explícita.** Deben ser aisladas en clases `Helper` o `UseCase` y ser probadas unitariamente.

### 2.1. Lógica de Generación de Código de Barras para TPV (Productos a Granel)

Esta función es la responsable de crear el código de barras que lee el sistema de caja. Su formato es vital.

**Especificación:** `200` + `código de producto (4 dígitos)` + `precio total (5 dígitos)` + `5`

```kotlin
/**
 * Genera el código de barras en formato EAN-13 para ser leído por el TPV.
 * Especificación: "200" + código de producto (4 dígitos) + precio total (5 dígitos) + "5"
 *
 * @param productoCodigo El código del producto (ej. "1090").
 * @param montoVenta El precio total del item (ej. 50.50).
 * @return El código de barras formateado como String (ej. "2001090050505").
 */
fun generarCodigoParaPOS(productoCodigo: String, montoVenta: Double): String {
    val codigoProductoStr = productoCodigo.padStart(4, '0')
    val montoVentaStr = (montoVenta * 100).roundToInt().toString().padStart(5, '0')
    return "200${codigoProductoStr}${montoVentaStr}5"
}
```

### 2.2. Lógica de Generación de Código QR para Control Interno

Este código es para uso interno y su formato debe ser consistente para futuras herramientas de análisis.

**Especificación:** `HHMMSS-FFF-MMMM.CC`

```kotlin
/**
 * Genera la cadena de datos para el código QR de control interno.
 * Especificación: HHMMSS-FFF-MMMM.CC
 *
 * @param timestamp La fecha y hora de la venta en milisegundos.
 * @param folio El folio de la venta (ej. "1").
 * @param montoTotal El monto total del ticket (ej. 125.50).
 * @return La cadena formateada para el QR (ej. "143025-001-0125.50").
 */
fun generarCodigoControlInterno(timestamp: Long, folio: String, montoTotal: Double): String {
    val dateFormat = SimpleDateFormat("HHmmss", Locale.US)
    val timeStr = dateFormat.format(Date(timestamp))
    val folioStr = folio.padStart(3, '0')
    val montoStr = String.format(Locale.US, "%07.2f", montoTotal) // Crucial: Mantiene el punto decimal.
    
    return "$timeStr-$folioStr-$montoStr"
}
```

## 3. Plan de Desarrollo Incremental

El desarrollo se realizará por fases para minimizar riesgos y tener entregables funcionales en cada etapa.

1.  **Fase 1: Cimientos.**
    -   Configurar Gradle, Hilt, Room y Compose Navigation.
    -   Definir las entidades de Room y sus DAOs.
    -   Crear los Repositorios y los módulos de Hilt.
2.  **Fase 2: Gestión de Productos (CRUD).**
    -   Implementar las pantallas de "Lista de Productos" y "Añadir/Editar Producto" usando Jetpack Compose. Validar el stack completo (UI -> ViewModel -> Repository -> DB).
3.  **Fase 3: Corazón del TPV.**
    -   Construir la pantalla principal de 3 paneles.
    -   Implementar el catálogo, la búsqueda, la gestión de múltiples tickets y la edición de items.
4.  **Fase 4: Hardware y Finalización.**
    -   Reutilizar y adaptar el `BluetoothPrinterHelper`.
    -   Conectar la lógica de impresión a los botones de "Finalizar Venta" y "Reimprimir".
    -   Implementar las pantallas de "Historial" y "Actualizar BD desde CSV".

## 4. Sugerencias de Mejora (A implementar en v2.0)

-   **Pantalla de Configuración:** En lugar de opciones en el menú, crear una pantalla de "Ajustes" dedicada. Usar `Jetpack DataStore` para persistir la dirección MAC de la impresora y, potencialmente, la ruta del archivo CSV.
-   **Mejorar la Experiencia de Edición:** Al editar un item en el ticket, en lugar de un `EditText`, mostrar un diálogo o un popup más amigable que use el teclado numérico para evitar cambios de foco indeseados.
-   **Pruebas Unitarias:** Añadir pruebas unitarias para los ViewModels y, especialmente, para las funciones críticas definidas en la sección 2. Esto nos protegerá de regresiones en el futuro.
