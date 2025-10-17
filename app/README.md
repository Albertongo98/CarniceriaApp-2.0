# Carniceriapp v2.0 - Especificación Funcional

## 1. Visión General del Proyecto

**Carniceriapp v2.0** será la reconstrucción completa de la aplicación de punto de venta (TPV) utilizando 100% **Jetpack Compose** para la interfaz de usuario y siguiendo las últimas mejores prácticas de desarrollo de Android. El objetivo es crear una aplicación más robusta, mantenible, escalable y con una experiencia de usuario mejorada, manteniendo toda la funcionalidad que ha demostrado ser crítica para el negocio.

Este documento sirve como la especificación funcional y la "fuente de verdad" para el desarrollo.

## 2. Arquitectura y Principios Técnicos

- **UI:** 100% Jetpack Compose.
- **Arquitectura:** MVVM (Model-View-ViewModel).
- **Asincronía:** Kotlin Coroutines & Flow.
- **Base de Datos:** Room.
- **Inyección de Dependencias:** Hilt.

## 3. Especificaciones por Pantalla

### 3.1 Pantalla Principal (TPV)

Es la pantalla central de la aplicación, optimizada para tablets y dividida en tres paneles principales.

**Panel Izquierdo: Catálogo de Productos**
- **Función:** Mostrar la lista completa de productos disponibles para la venta.
- **Componentes:**
    - **Campo de Búsqueda:** Un `TextField` en la parte superior que filtra la lista de productos en tiempo real a medida que el usuario escribe.
    - **Lista de Productos:** Un `LazyColumn` que muestra los productos agrupados por `departamento`. Cada grupo tiene una cabecera con el nombre del departamento. Estas cabeceras son colapsables/expandibles. Si el campo de búsqueda tiene texto, todos los grupos se muestran expandidos por defecto.
- **Interacción:**
    - Un clic corto en un producto lo añade al ticket activo.

**Panel Central: Gestión de Tickets**
- **Función:** Administrar el ticket de venta actual y permitir cambiar entre múltiples tickets.
- **Componentes:**
    - **Pestañas de Tickets:** En la parte superior, muestra una pestaña por cada ticket activo (`Ticket 1`, `Ticket 2`, etc.). Permite cerrar tickets individuales y añadir nuevos con un botón `+`.
    - **Lista de Items del Ticket:** `LazyColumn` que muestra los productos añadidos al ticket seleccionado. Cada item debe mostrar:
        - Nombre del producto.
        - Cantidad (editable).
        - Precio total del item (editable para productos a granel).
        - Botones para incrementar, decrementar o eliminar el item.
- **Interacción:**
    - **Editar Cantidad/Precio:** Al tocar el campo de cantidad o precio, se activa el teclado numérico del panel derecho para introducir un nuevo valor.

**Panel Derecho: Teclados y Acciones**
- **Función:** Proveer un método de entrada rápido y las acciones principales de la venta.
- **Componentes:**
    - **Teclado Numérico:** Para la edición de cantidades y precios en el ticket.
    - **Teclado Alfanumérico:** Se muestra cuando el foco está en el campo de búsqueda de productos.
    - **Botones de Acción:**
        - `Finalizar Venta`: Abre el diálogo de vista previa del ticket.
        - `Reimprimir Último Ticket`: Busca el último ticket guardado y lo manda a imprimir.
- **Kiosco Inteligente (Top Ventas):** Una fila (`LazyRow`) que muestra los productos más vendidos del día para un acceso rápido.

### 3.2 Diálogo de Vista Previa del Ticket
- **Función:** Mostrar un resumen final de la venta antes de imprimir.
- **Activación:** Se muestra al pulsar "Finalizar Venta".
- **Contenido:** Una lista no editable de los items del ticket y el monto total.
- **Acciones:**
    - `Imprimir`: Confirma la venta, la guarda en la base de datos y manda a imprimir el ticket.
    - `Cancelar`: Cierra el diálogo sin realizar la venta.

### 3.3 Pantalla de Gestión de Productos
- **Función:** CRUD (Crear, Leer, Actualizar, Eliminar) de los productos.
- **Componentes:**
    - `LazyColumn` con la lista de todos los productos de la base de datos.
    - Cada item muestra: Nombre, Código, Precio y botones de `Editar` y `Eliminar`.
    - Un `FloatingActionButton` (FAB) con un ícono `+` para navegar a la pantalla de "Añadir Producto".
- **Interacción:**
    - `Eliminar`: Muestra un diálogo de confirmación antes de borrar el producto de la base de datos.
    - `Editar`: Navega a la pantalla "Editar Producto", precargando los datos del producto seleccionado.

### 3.4 Pantalla de Añadir/Editar Producto
- **Función:** Formulario para crear un nuevo producto o modificar uno existente.
- **Componentes:**
    - `TextFields` para: Código, Nombre, Precio, Departamento.
    - `RadioButtons` para seleccionar la Unidad: `GRANEL` o `UNIDAD`.
    - Botones de `Guardar` y `Cancelar`.
- **Lógica de Validación:**
    - Todos los campos son obligatorios.
    - El precio debe ser un número válido mayor a cero.
    - **Crucial:** Al añadir un producto, el `código` no debe existir previamente en la base de datos. Al editar, el código no se puede modificar.

### 3.5 Pantalla de Historial de Tickets
- **Función:** Consultar todas las ventas finalizadas.
- **Componentes:**
    - `LazyColumn` que lista todos los tickets guardados en la BD, mostrando: Folio, Fecha/Hora, Monto Total.
- **Interacción:**
    - Un clic en un ticket lo manda a reimprimir.

### 3.6 Pantalla de Actualizar Base de Datos
- **Función:** Permitir la importación masiva de productos desde un archivo `CSV`.
- **Componentes:** Un simple botón "Importar desde CSV".
- **Lógica:**
    - Al pulsar, la app busca un archivo `8477.csv` en una ubicación predefinida.
    - Parsea el archivo (delimitador: punto y coma `;`) y actualiza la tabla de productos en la base de datos Room. **Importante:** La operación debe ser una limpieza y reinserción (borrar todos los productos existentes antes de insertar los nuevos).

