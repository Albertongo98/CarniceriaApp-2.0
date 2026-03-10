# Carniceriapp v2.0 - Especificación Funcional

## 1. Visión General del Proyecto

**Carniceriapp v2.0** es un sistema de punto de venta (TPV) especializado para carnicerías, construido 100% con **Jetpack Compose**. La app está diseñada para operar en tablets de producción, optimizando el flujo de venta, el pesaje de productos a granel y la gestión de impresión térmica Bluetooth.

## 2. Avances Recientes y Mejoras de Producción

### 2.1 UI Optimizada para Despacho Rápido
- **Smart Keyboard v2:** Teclado QWERTY y Numérico optimizados para una altura de **380dp**, maximizando la visibilidad del ticket. Incluye indicadores de scroll visual (flechas parpadeantes y gradientes) para navegación lateral intuitiva.
- **Moda Dinámica Diaria:** El teclado inteligente ahora sugiere los 10 productos más vendidos **específicamente del día actual**, adaptándose al ritmo de venta de cada jornada.
- **Ayuda Visual de Precios:** Los precios por kilo/pieza se muestran permanentemente debajo de cada ítem en el ticket para validación inmediata del operario.

### 2.2 Gestión de Notas de Despacho (Nuevo)
- **Contador de Piezas (+PZ):** Función especializada para productos a granel que permite anotar cuántas unidades físicas pidió el cliente (ej: 3 chiles, 2 cebollas) independientemente del peso, facilitando el surtido de órdenes masivas.
- **Resumen Centrado para el Trabajador:** Diálogo de confirmación rediseñado como una "hoja de verificación" interna con nombres en mayúsculas, negritas y total destacado.

### 2.3 Robustez en la Impresión (En Desarrollo)
- **Sincronización de Hardware:** Implementación de protocolos de **Handshake (1.2s)** y **Release Seguro (2.5s)** para estabilizar la comunicación Bluetooth. 
- **Nota Técnica:** Persiste un problema intermitente en la segunda impresión consecutiva; se utiliza el botón de "Reimprimir" como mecanismo de rescate mientras se optimiza el vaciado del buffer de hardware.
- **Tickets Informativos:** Inclusión de conteo de productos impreso (`PRODUCTOS: N`) y avisos gigantes de `>> PIEZAS <<` para unidades múltiples.

## 3. Arquitectura Técnica (v3)
- **Base de Datos Room v3:** Esquema actualizado para soportar `estimatedPieces` y moda dinámica por timestamp.
- **Comunicación:** Chunks de 64 bytes para evitar saturación de buffer en impresoras de 57mm.

## 4. Próximos Pasos (Checklist en AGENTS.md)
Consultar el archivo `AGENTS.md` para el seguimiento detallado de las tareas técnicas.
