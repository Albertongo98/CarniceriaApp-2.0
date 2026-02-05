# Carniceriapp v2.0 - Especificación Funcional

## 1. Visión General del Proyecto

**Carniceriapp v2.0** es un sistema de punto de venta (TPV) especializado para carnicerías, construido 100% con **Jetpack Compose**. La app está diseñada para operar en tablets de producción, optimizando el flujo de venta, el pesaje de productos a granel y la gestión de impresión térmica Bluetooth.

## 2. Avances Recientes y Mejoras de Producción

### 2.1 UI Optimizada para Tablets
- **Rediseño de Teclados:** Tanto el teclado **QWERTY** como el **Keypad Numérico** han sido rediseñados con botones cuadrados de alto contraste (Verde Bosque) para una mejor respuesta táctil.
- **Elevación Anti-Barra de Navegación:** Se implementó un margen de seguridad de **65dp** en la parte inferior de los teclados para evitar que los botones de sistema de la tablet (Atrás, Inicio) obstruyan el uso de la app.
- **Panel Inferior Dinámico:** Se compactó el área de edición de productos para ganar altura vertical, permitiendo botones de teclado más grandes y fáciles de pulsar.

### 2.2 Robustez en la Impresión
- **Regla de Oro "Flush Print":** Implementación de una pausa obligatoria de **1.5 a 2 segundos** tras cada impresión para proteger el cabezal térmico y asegurar la integridad de los datos.
- **Finalización Limpia:** Se añadió un comando de avance de papel y corte automático (`CMD_FEED_AND_CUT`) para liberar el buffer de la impresora y evitar bloqueos.
- **Impresión Masiva Estable:** El historial de tickets ahora procesa reimpresiones de forma estrictamente secuencial, evitando la saturación del Bluetooth.
- **Resalte de Unidades:** Los productos vendidos por pieza ahora muestran una línea prominente: `>> 5 PIEZAS <<` en negrita y tamaño doble.

### 2.3 Gestión de Datos e Historial
- **Lector de CSV Híbrido:** Capacidad de importar bases de datos tanto en formato antiguo (separado por `;`) como en el nuevo formato de 14 columnas (separado por `,`), con limpieza automática de símbolos de moneda.
- **Vista Previa de Tickets:** Nueva funcionalidad en el historial que permite visualizar el contenido de un ticket en una simulación de papel térmico antes de imprimir.

## 3. Arquitectura Técnica

- **UI:** Jetpack Compose (Material 3).
- **MVVM:** Separación estricta de lógica y vista.
- **Room:** Persistencia local de productos y ventas.
- **Hilt:** Inyección de dependencias para ViewModels y Repositorios.
- **DataStore:** Manejo de preferencias (MAC de impresora, ajustes de logo).

## 4. Próximos Pasos (Checklist en AGENTS.md)
Consultar el archivo `AGENTS.md` para el seguimiento detallado de las tareas técnicas y el estado del desarrollo.
