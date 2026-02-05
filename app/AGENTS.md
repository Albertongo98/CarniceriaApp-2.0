# Directivas de Desarrollo para Carniceriapp v2.0

Este documento establece las reglas, principios y funciones críticas para el desarrollo de `Carniceriapp v2.0`. El objetivo es garantizar un desarrollo consistente, mantenible y de alta calidad.

## 1. Reglas de Oro (No Negociables)

### 1.1. Arquitectura y Stack Tecnológico

1.  **UI 100% Jetpack Compose:** Todo el UI se construye con Jetpack Compose.
2.  **Arquitectura MVVM Estricta:** Vistas (Composables) -> ViewModels -> Repositorios.
3.  **Inyección de Dependencias con Hilt:** Todas las dependencias son gestionadas por Hilt.
4.  **Asincronía con Coroutines y Flow:** Todas las operaciones de BD, red o impresión se ejecutan en coroutines.

### 1.2. Regla de Oro: El Flush Print

**El "Flush Print" es una pausa obligatoria de 1.5 segundos que debe realizarse *después* de cada impresión de ticket.** Esta regla previene el sobrecalentamiento del cabezal de la impresora y la corrupción de datos. Es una regla de hardware crítica.

### 1.3. Regla de Oro: Botón de Impresión Único

**El botón "Imprimir y Guardar" debe ser a prueba de múltiples clics.** Al presionarlo, debe mostrar inmediatamente un indicador de carga y deshabilitarse para prevenir la creación de tickets duplicados. Cualquier clic posterior mientras está en estado de "imprimiendo" será ignorado.

## 2. Funciones Críticas (Sagradas)

-   **`generarCodigoParaPOS`**: Genera el código EAN-13 para productos a granel. Formato: `"200" + [código de producto 4 dígitos] + [precio total 5 dígitos] + "5"`.
-   **`generarCodigoControlInterno`**: Genera el QR de auditoría. Formato: `HHMMSS-FFF-MMMM.CC`.

## 3. Plan de Trabajo (Post-Reparación)

*   [x] **1. Reparar Compilación del Proyecto:**
    *   Se actualizaron las dependencias de Gradle y se corrigieron errores de tipos.
*   [x] **2. Implementar Indicador de Carga en Botón de Impresión.**
*   [x] **3. Corregir Reimpresión en Historial.**
*   [x] **4. Ajustes de Impresión de Ticket (Logo y QR).**
*   [x] **5. Refactorización de UI y Teclado Inteligente (Smart Keyboard):**
    *   Rediseño visual con colores de marca y optimización de espacios para Tablet.
    *   Implementación de sugerencias dinámicas basadas en productos más vendidos (Top 10).
    *   Fila de importes rápidos ($5 a $100) para agilizar el pesado por dinero.
    *   Corrección de error fatal (crash) por duplicidad de llaves en la lista del ticket.

## 4. Gestión del Proyecto

-   **Checklist:** Este documento es el checklist oficial. Los puntos se marcan como completados `[x]`.
-   **Commits:** Se realizará un commit a Git después de completar cada punto para mantener puntos de restauración estables.
