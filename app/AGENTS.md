# Directivas de Desarrollo para Carniceriapp v2.0

Este documento establece las reglas, principios y funciones críticas para el desarrollo de `Carniceriapp v2.0`. El objetivo es garantizar un desarrollo consistente, mantenible y de alta calidad.

## 1. Reglas de Oro (No Negociables)

### 1.1. Arquitectura y Stack Tecnológico

1.  **UI 100% Jetpack Compose:** Todo el UI se construye con Jetpack Compose.
2.  **Arquitectura MVVM Estricta:** Vistas (Composables) -> ViewModels -> Repositorios.
3.  **Inyección de Dependencias con Hilt:** Todas las dependencias son gestionadas por Hilt.
4.  **Asincronía con Coroutines y Flow:** Todas las operaciones de BD, red o impresión se ejecutan en coroutines.

### 1.2. Regla de Oro: Sincronización de Hardware (Bluetooth) - [REVISADO PARA PRUEBAS]

**Se han reducido los tiempos para optimizar la velocidad en tablets de prueba:**
- **Handshake Inicial:** 0.5 segundos tras conectar.
- **Release Seguro:** Pausa de 1.0 segundo antes de cerrar el socket.
- **Ritmo de Datos:** Paquetes de 256 bytes con 20ms de delay.

### 1.3. Regla de Oro: Flujo de Despacho Profesional

**El sistema no es una caja registradora, es una estación de despacho:**
- El botón principal debe decir **"FINALIZAR"** o **"GENERAR TICKET"**.
- Antes de imprimir, es obligatorio mostrar el **Diálogo de Confirmación Centrado** para que el operario valide verbalmente los productos y el total con el cliente.

## 2. Funciones Críticas (Sagradas)

-   **`generarCodigoParaPOS`**: Genera el código EAN-13 para productos a granel. Formato: `"200" + [código de producto 4 dígitos] + [precio total 5 dígitos] + "5"`.
-   **`generarCodigoControlInterno`**: Genera el QR de auditoría. Formato: `HHMMSS-FFF-MMMM.CC`.
-   **`applyEstimatedPieces`**: Permite anotar unidades físicas para productos vendidos por peso, facilitando el despacho masivo.

## 3. Plan de Trabajo (Post-Reparación)

*   [x] **1. Reparar Compilación del Proyecto.**
*   [x] **2. Implementar Indicador de Carga en Botón de Impresión.**
*   [x] **3. Corregir Reimpresión en Historial.**
*   [x] **4. Ajustes de Impresión de Ticket (Logo y QR).**
*   [x] **5. Refactorización de UI y Teclado Inteligente (Smart Keyboard).**
*   [x] **6. Automatización y Mantenimiento (Offline-First).**
*   [x] **7. Blindaje de Impresión en Producción:**
    *   Solución al fallo de segunda impresión mediante limpieza de buffer.
    *   Inclusión de conteo de productos impreso (`PRODUCTOS: N`).
*   [x] **8. Notas de Despacho Masivo (Contador de Piezas).**

## 4. Gestión del Proyecto

-   **Checklist:** Este documento es el checklist oficial.
-   **Commits:** Se realizará un commit a Git después de completar cada punto.
