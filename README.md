# CarniceriaApp 2.0 🥩

**CarniceriaApp 2.0** es una solución profesional de Punto de Venta (TPV) y despacho masivo, diseñada específicamente para carnicerías. Optimizada para su uso en tablets y con integración directa mediante Bluetooth para impresoras térmicas de 58mm (POS-58).

Esta versión 2.0 ha sido reconstruida desde cero utilizando tecnologías modernas de Android para garantizar fluidez, estabilidad y un manejo de hardware "blindado".

## ✨ Características Principales

### 🖥️ Estación de Despacho (TPV)
*   **Multiticket Simultáneo:** Permite gestionar varios tickets abiertos al mismo tiempo para no detener el flujo de atención.
*   **Gestión de Productos Inteligente:** Soporte para productos por **Unidad** y a **Granel (Peso)**.
*   **Teclado Inteligente (Smart Keyboard):** Interfaz híbrida con teclado numérico para cantidades/precios y QWERTY para búsqueda rápida de productos.
*   **Notas de Despacho Masivo:** Función exclusiva para anotar piezas estimadas en ventas por peso, facilitando el conteo físico en el área de despacho.

### 🖨️ Impresión Térmica Blindada
*   **Optimización para POS-58:** Protocolo de comunicación Bluetooth robustecido para evitar saturación de búfer y bloqueos físicos.
*   **Códigos de Barras CODE128:** Generación nativa de códigos de barras industriales para productos y control interno, eliminando la inestabilidad de los códigos QR en impresoras genéricas.
*   **EAN-13 Dinámico:** Genera automáticamente códigos EAN-13 que incluyen el precio del ítem para integración directa con cajas registradoras.
*   **Ahorro de Papel:** Diseño de ticket compacto y eficiente.

### 🏷️ Generador de Etiquetas
*   **Creación Rápida:** Formulario simplificado para generar etiquetas de productos con nombre, precio y código.
*   **Impresión en Lote:** Capacidad para imprimir múltiples copias en un solo flujo de datos Bluetooth.
*   **Historial de Etiquetas:** Guarda un registro de las etiquetas generadas para su reimpresión instantánea.

### 🛠️ Mantenimiento y Rendimiento
*   **Offline-First:** Funciona al 100% sin conexión a internet.
*   **Gestión de Folios Diarios:** Reinicio automático del contador de folios cada día.
*   **Reportes de Ventas:** Corte por fecha con resumen por departamento (piezas y kilos por separado), impresión de corte de caja y exportación a HTML.
*   **Historial Completo:** Los tickets se conservan; las migraciones de base de datos preservan productos y ventas entre versiones.

## 🚀 Stack Tecnológico

*   **Lenguaje:** Kotlin 2.0+
*   **UI:** Jetpack Compose (100%)
*   **Arquitectura:** MVVM (Model-View-ViewModel)
*   **Inyección de Dependencias:** Hilt / Dagger
*   **Base de Datos:** Room
*   **Almacenamiento de Preferencias:** DataStore
*   **Concurrencia:** Coroutines & Flow
*   **Hardware:** Bluetooth Classic (RFCOMM) con comandos ESC/POS

## 📦 Instalación y Requisitos

1.  **Dispositivo:** Tablet o Smartphone con Android 7.0 (API 24) o superior.
2.  **Hardware Externo:** Impresora térmica de 58mm con conexión Bluetooth.
3.  **Configuración:**
    *   Vincular la impresora en los ajustes de Bluetooth del dispositivo.
    *   Abrir la app y seleccionar la impresora desde el menú de ajustes de la TPV.

## 🛠️ Reglas de Oro del Proyecto

Las reglas críticas de desarrollo (base de datos, impresión Bluetooth, formatos, arquitectura) y la lista completa de funcionalidades están en [`CLAUDE.md`](CLAUDE.md).

---
Desarrollado con ❤️ para optimizar el despacho en carnicerías.
