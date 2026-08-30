---
name: cerebro-diseno
description: Software Enterprise/SaaS diseñado para pantallas grandes (Tablet/Desktop). Prioriza patrones Anti-Mobile, navegación lateral, workspaces, densidad útil y productividad con mouse/teclado.
---

# SKILL — ENTERPRISE LARGE-SCREEN UI

## Anti-Mobile · Tablet · Desktop · SaaS · Jetpack Compose

---

# 1. MISIÓN

Esta aplicación es un **software Enterprise/SaaS diseñado para pantallas grandes**.

La interfaz debe sentirse como un sistema profesional de gestión empresarial, no como una aplicación móvil ampliada.

Jetpack Compose es únicamente la tecnología de implementación.

**Android NO define el patrón UX.**

La interfaz debe priorizar:

1. Contexto continuo
2. Productividad
3. Densidad de información útil
4. Claridad
5. Velocidad operativa
6. Navegación visible
7. Mouse
8. Teclado
9. Touch
10. Accesibilidad
11. Consistencia
12. Escalabilidad

---

# 2. REGLA ABSOLUTA ANTI-MOBILE

Cuando exista un patrón típico de aplicaciones móviles y exista un patrón equivalente para pantalla grande:

> **EL PATRÓN LARGE-SCREEN TIENE PRIORIDAD.**

No se debe implementar un patrón móvil simplemente porque exista un componente Material/Compose para hacerlo.

### Flujo obligatorio

```text
NECESIDAD DEL USUARIO
        ↓
PATRÓN UX
        ↓
PATRÓN LARGE-SCREEN
        ↓
COMPONENTE COMPOSE
```

Nunca:

```text
"Existe AlertDialog()"
        ↓
"Entonces uso AlertDialog()"
```

---

# 3. TABLA MAESTRA DE PROHIBICIONES Y SUSTITUTOS

Esta tabla es normativa.

| ❌ Patrón móvil que NO debe utilizarse como patrón principal | ✅ Sustituto Large-Screen                |
| ----------------------------------------------------------- | --------------------------------------- |
| `BottomNavigation`                                          | **Sidebar / Navigation Rail**           |
| `NavigationBar` inferior                                    | **Sidebar persistente**                 |
| `ModalNavigationDrawer`                                     | **Persistent Sidebar**                  |
| Hamburger menu como navegación principal                    | **Sidebar visible**                     |
| `ModalBottomSheet`                                          | **Side Sheet / Side Panel**             |
| `BottomSheet` para edición                                  | **Edit Side Panel**                     |
| `AlertDialog` para formularios                              | **Side Panel / Workspace**              |
| `Dialog` para detalles complejos                            | **Detail Pane**                         |
| `Dialog` para filtros                                       | **Filter Side Panel**                   |
| `Dialog` para selección grande                              | **Searchable Selection Panel**          |
| `Dialog` para crear entidades complejas                     | **Create Workspace / Side Panel**       |
| `FAB` para acción empresarial principal                     | **Primary Action Button**               |
| FAB con múltiples acciones                                  | **Toolbar / Action Group**              |
| `Toast` para feedback importante                            | **Inline Feedback / Notification**      |
| `Snackbar` como sistema de notificaciones                   | **Notification Center / Inline Status** |
| Snackbar para error importante                              | **Error State / Inline Error**          |
| Spinner global bloqueando pantalla                          | **Skeleton / Local Loading**            |
| ProgressBar sin contexto                                    | **Operation Status**                    |
| `LazyColumn` como lista visual de registros empresariales   | **Data Table / Structured List**        |
| Card para cada registro                                     | **Table Row / Data Row**                |
| Lista → pantalla detalle                                    | **Master–Detail**                       |
| Pantalla completa de detalle                                | **Detail Pane**                         |
| Pantalla completa para editar algo contextual               | **Side Panel**                          |
| Formulario de una columna                                   | **Responsive Form Grid**                |
| Dropdown con cientos de elementos                           | **Searchable ComboBox**                 |
| Botón "Filtros" como única forma de filtrar                 | **Filter Bar**                          |
| `FilterBottomSheet`                                         | **Filter Side Panel**                   |
| Swipe como única forma de acción                            | **Row Actions / Context Menu**          |
| Long Press como única interacción                           | **Right Click / Context Menu**          |
| Icon-only para acciones poco obvias                         | **Icon + Label / Tooltip**              |
| Full-width button innecesario                               | **Compact Action Group**                |
| Pull-to-refresh como única actualización                    | **Refresh Action**                      |
| Carrusel para información importante                        | **Grid / Multi-column Layout**          |
| Wizard móvil                                                | **Stepper / Workflow Workspace**        |
| Pantallas apiladas innecesariamente                         | **Workspace / Split View**              |
| Bottom action buttons                                       | **Toolbar / Sticky Action Bar**         |
| Una sola columna en pantalla amplia                         | **Responsive Multi-column Layout**      |
| Modal para propiedades                                      | **Inspector Panel**                     |
| Modal para información relacionada                          | **Detail Panel / Tabs**                 |
| Menú escondido para acciones frecuentes                     | **Toolbar**                             |
| Loading de toda la aplicación                               | **Section-level Loading**               |
| Empty screen sin explicación                                | **Actionable Empty State**              |
| "Error" sin recuperación                                    | **Recovery Error State**                |
| Información temporal importante                             | **Persistent Activity / Notification**  |
| Touch como única interacción                                | **Touch + Mouse + Keyboard**            |
| Navegación sin focus                                        | **Keyboard Focus Navigation**           |

---

# 4. NAVEGACIÓN

## ❌ PROHIBIDO

Usar navegación inferior como arquitectura principal:

```text
┌───────────────────────────┐
│                           │
│       CONTENIDO           │
│                           │
├───────────────────────────┤
│ Inicio Inventario Caja    │
└───────────────────────────┘
```

## ✅ OBLIGATORIO

Utilizar navegación lateral.

```text
┌──────────────┬─────────────────────────────────────┐
│              │                                     │
│ Inicio       │                                     │
│ Inventario   │             WORKSPACE               │
│ Compras      │                                     │
│ Proveedores  │                                     │
│ Caja         │                                     │
│ Reportes     │                                     │
│ Configuración│                                     │
└──────────────┴─────────────────────────────────────┘
```

La navegación debe ser:

* persistente
* visible
* jerárquica
* consistente

---

# 5. SIDEBAR

El Sidebar debe poder contener:

* logo
* organización
* módulos
* submódulos
* sección activa
* badges
* indicadores
* configuración
* cuenta

Debe permitir estado:

```text
Expanded
Collapsed
```

Pero el colapso no debe destruir la usabilidad.

---

# 6. DIALOG

## ❌ PROHIBIDO COMO CONTENEDOR GENERAL

No utilizar Dialog para:

* formularios largos
* edición compleja
* creación compleja
* detalle completo
* gestión de lotes
* filtros complejos
* selección masiva
* workflows

Ejemplo prohibido:

```text
┌─────────────────────────────┐
│ Editar producto             │
│                             │
│ Nombre                      │
│ Categoría                   │
│ Proveedor                   │
│ Presentación                │
│ Precio                      │
│ Stock                       │
│ Lote                        │
│ ...                         │
│                             │
│ Cancelar       Guardar      │
└─────────────────────────────┘
```

## ✅ SUSTITUTO

Utilizar:

* Side Panel
* Detail Pane
* Inspector
* Workspace
* Split View

---

# 7. EXCEPCIÓN DEL DIALOG

Dialog está permitido únicamente cuando:

* la interacción es corta
* requiere atención inmediata
* es una confirmación crítica
* el contenido es pequeño
* mantenerlo como superficie modal mejora la seguridad

Ejemplo válido:

```text
Eliminar lote

¿Eliminar el lote A-123?

[Cancelar] [Eliminar]
```

No convertir una excepción en el patrón general.

---

# 8. SIDE PANEL

Usar Side Panel para:

* edición contextual
* creación sencilla
* filtros
* propiedades
* configuración
* detalles

Debe mantener visible el contexto principal.

---

# 9. DETAIL PANE

Usar Detail Pane cuando el usuario selecciona un elemento.

```text
┌─────────────────────┬──────────────────────────────┐
│ Productos           │ Paracetamol 500 mg           │
│                     │                              │
│ Paracetamol ←       │ Información                  │
│ Amoxicilina         │ Lotes                        │
│ Ibuprofeno          │ Movimientos                  │
└─────────────────────┴──────────────────────────────┘
```

---

# 10. MASTER–DETAIL

Cuando exista:

```text
Lista + detalle
```

considerar Master–Detail antes de crear navegación a otra pantalla.

Debe preservar:

* búsqueda
* filtros
* selección
* contexto

---

# 11. SPLIT VIEW

Usar cuando dos o más áreas deben permanecer visibles simultáneamente.

Ejemplo:

```text
Sidebar
   +
Lista
   +
Detalle
```

No introducir Split View simplemente para llenar espacio.

---

# 12. FAB

## ❌ PROHIBIDO COMO ACCIÓN PRINCIPAL EMPRESARIAL

No:

```text
                         (+)
```

para:

* nuevo producto
* nueva compra
* nueva orden
* nuevo proveedor

## ✅ SUSTITUTO

```text
Inventario

[+ Nuevo producto]
```

El usuario debe saber exactamente qué hará.

---

# 13. TOOLBAR

Las acciones principales y secundarias deben organizarse.

```text
Inventario

🔍 Buscar...     Filtros     Importar     Exportar     + Nuevo
```

Jerarquía:

```text
Primary
Secondary
Tertiary
Destructive
```

No dar la misma importancia visual a todo.

---

# 14. TABLE

Para información empresarial estructurada:

```text
Producto       Código     Stock    Precio     Estado
──────────────────────────────────────────────────────
Paracetamol    PAR500     120      S/2.50     Activo
Amoxicilina    AMX500      42      S/4.20     Activo
Ibuprofeno     IBU400      18      S/3.80     Bajo
```

Debe poder soportar cuando corresponda:

* selección
* multi-select
* ordenamiento
* filtros
* paginación
* acciones
* estados
* columnas configurables
* sticky header

---

# 15. CARDS

## ❌ PROHIBIDO

Convertir todos los registros en Cards.

```text
┌──────────────┐
│ Producto     │
│ Stock        │
│ Precio       │
└──────────────┘
```

repetido 50 veces.

## ✅ UTILIZAR CARD PARA

* KPI
* resumen
* agrupación
* sección
* información destacada

Para datos repetitivos y comparables:

**Table / Row.**

---

# 16. FORMULARIOS

## ❌ PROHIBIDO

Formularios enormes de una sola columna cuando existe espacio.

## ✅ OBLIGATORIO

Responsive Grid.

```text
Nombre                Código
[____________]        [___________]

Categoría             Presentación
[____________]        [___________]

Proveedor             Laboratorio
[____________]        [___________]
```

Utilizar 2–4 columnas cuando sea apropiado.

No dividir campos que deberían permanecer juntos.

---

# 17. COMBOBOX

Para pocas opciones puede utilizarse Select.

Para grandes cantidades:

```text
Proveedor

🔍 Buscar...

Droguería ABC
Distribuidora XYZ
Farmacéutica Lima
```

Nunca obligar al usuario a recorrer listas enormes.

---

# 18. FILTER BAR

Filtros frecuentes visibles:

```text
Categoría [Todas]
Stock [Todos]
Proveedor [Todos]
Vencimiento [Todos]
```

Los filtros avanzados pueden abrir un:

**Filter Side Panel.**

---

# 19. SEARCH

La búsqueda debe estar integrada en el Workspace cuando sea una función frecuente.

```text
🔍 Buscar producto, código o laboratorio...
```

No obligar a abrir otra pantalla únicamente para buscar.

---

# 20. GLOBAL SEARCH

Para sistemas grandes puede existir búsqueda global.

Debe permitir localizar:

* productos
* proveedores
* compras
* clientes
* documentos
* lotes
* órdenes

Los resultados deben estar agrupados por tipo.

---

# 21. TABS

Usar Tabs para información relacionada.

```text
[Información] [Lotes] [Movimientos] [Compras]
```

No usar Tabs como navegación principal.

---

# 22. ACCORDION

Usar para información secundaria o avanzada.

No esconder información crítica dentro de múltiples niveles de expansión.

---

# 23. CONTEXT MENU

Acciones secundarias:

```text
⋮

Ver
Editar
Duplicar
Reabastecer
Historial
────────────
Desactivar
```

No utilizar Context Menu para acciones que el usuario necesita constantemente.

---

# 24. RIGHT CLICK

El sistema debe aprovechar mouse.

Right click puede mostrar:

* acciones contextuales
* navegación
* copiar
* editar
* abrir detalle
* acciones de fila

Debe existir una alternativa equivalente para touch/teclado.

---

# 25. HOVER

Hover puede revelar:

* acciones secundarias
* tooltip
* información auxiliar
* estado de fila

Pero:

> Nunca esconder información esencial únicamente mediante hover.

---

# 26. KEYBOARD

Toda interfaz Enterprise debe considerar teclado.

Soportar cuando corresponda:

```text
Tab
Shift + Tab
Enter
Escape
Arrow Keys
Space
Ctrl + K
Ctrl + N
```

No introducir shortcuts arbitrarios.

---

# 27. FOCUS

Todos los elementos interactivos deben tener estado Focus visible.

El usuario debe saber dónde está el foco del teclado.

---

# 28. LOADING

## ❌ PROHIBIDO

Bloquear toda la aplicación:

```text
████████████████
    Loading...
████████████████
```

para una operación local.

## ✅ SUSTITUTOS

* Skeleton
* Section Loading
* Table Loading
* Panel Loading
* Button Loading
* Operation Status

La aplicación debe conservar contexto.

---

# 29. SKELETON

Cuando se conoce la estructura del contenido:

```text
████████████████████
████████
████████████████
```

Debe aproximarse a la estructura real.

---

# 30. OPERATION STATUS

Operaciones largas deben mostrar:

```text
Importando productos

342 / 1,200

██████████████░░░░

[Cancelar]
```

Después:

```text
✓ Importación completada

1,200 procesados
1,184 correctos
16 requieren revisión
```

---

# 31. TOAST

## ❌ PROHIBIDO

Usar Toast para:

* errores importantes
* operaciones críticas
* información que debe revisarse posteriormente
* resultados empresariales

## ✅ SUSTITUTO

* Inline Feedback
* Status
* Notification Center
* Activity
---

# 32. SNACKBAR

Snackbar puede existir para feedback breve, pero no debe ser el sistema principal de comunicación.

Para información importante utilizar:

**Inline Status / Notification / Activity.**

---

# 33. NOTIFICATION CENTER

Información que debe permanecer consultable:

```text
🔔 3

Notificaciones

● Stock bajo
● Compra recibida
● Lote próximo a vencer
```

---

# 34. EMPTY STATE

Nunca dejar una pantalla vacía.

Debe explicar:

```text
No hay productos

Todavía no has registrado productos.

[+ Nuevo producto]
```

---

# 35. ERROR STATE

Nunca:

```text
Error
```

Debe existir recuperación:

```text
No pudimos cargar el inventario.

Comprueba la conexión e inténtalo nuevamente.

[Reintentar]
```

---

# 36. CONFIRMATION

Las confirmaciones deben explicar consecuencias.

Debe responder:

* ¿qué estoy haciendo?
* ¿qué será afectado?
* ¿es reversible?
* ¿qué ocurrirá?

---

# 37. INLINE EDITING

Para modificaciones pequeñas:

```text
Precio: S/ 2.50   ✎
```

Puede convertirse en:

```text
[ S/ 2.50 ] [✓] [×]
```

No abrir un formulario completo para una modificación trivial.

---

# 38. BULK ACTIONS

Para múltiples registros:

```text
24 seleccionados

[Exportar] [Actualizar] [Asignar] [Desactivar]
```

Las acciones deben ser contextuales a la selección.

---

# 39. PAGINATION

Para datasets grandes:

```text
1–50 de 1,284

‹ 1 2 3 ... 26 ›
```

Permitir tamaño de página cuando sea útil.

---

# 40. STICKY TOOLBAR

Cuando el usuario trabaja con formularios largos o tablas:

```text
┌──────────────────────────────────────┐
│ Acciones / estado                    │
└──────────────────────────────────────┘
```

puede permanecer visible.

No usar sticky únicamente por estética.

---

# 41. STICKY TABLE HEADER

En tablas largas, conservar los encabezados visibles cuando el contexto lo requiera.

---

# 42. DENSIDAD

La interfaz Enterprise debe tener densidad configurable cuando sea útil:

```text
Cómoda
Estándar
Compacta
```

Nunca sacrificar legibilidad por densidad.

---

# 43. GRID RESPONSIVE

La UI debe reaccionar al espacio disponible.

No utilizar dimensiones fijas basadas en teléfonos.

No asumir:

```text
412dp
```

como referencia de composición.

El layout debe calcular:

* columnas
* paneles
* sidebar
* detalle
* inspector
* densidad

según el espacio disponible.

---

# 44. PANEL INSPECTOR

Para propiedades de un elemento seleccionado:

```text
┌─────────────────────────────┐
│ Inspector                   │
│                             │
│ Paracetamol 500 mg          │
│                             │
│ Código                      │
│ PAR500                      │
│                             │
│ Categoría                   │
│ Analgésicos                 │
│                             │
│ Estado                      │
│ ● Activo                    │
│                             │
│ [Editar]                    │
└─────────────────────────────┘
```

Ideal para herramientas administrativas.

---

# 45. ACTIVITY / HISTORY

Las entidades relevantes deben poder mostrar:

```text
Actividad

10:42  Stock actualizado
10:35  Lote agregado
09:51  Precio modificado
```

Cuando corresponda incluir:

* usuario
* fecha
* hora
* acción
* cambio

---

# 46. AUDIT

Las operaciones empresariales importantes deben conservar trazabilidad cuando el dominio lo requiera.

Ejemplo:

```text
Precio

Antes: S/ 2.30
Después: S/ 2.50
Usuario: Administrador
Fecha: 29/08/2026 10:42
```

---

# 47. STEPPER

Para workflows complejos:

```text
① Producto
② Presentación
③ Inventario
④ Confirmación
```

En pantalla grande preferir Stepper lateral cuando aporte contexto.

---

# 48. WORKSPACE

Una aplicación Enterprise debe pensar en términos de Workspaces.

Ejemplo:

```text
Sidebar
   ↓
Inventario Workspace
   ↓
Lista + Detalle + Acciones
```

El Workspace mantiene:

* contexto
* filtros
* navegación
* estado
* selección

---

# 49. SCROLL

Evitar una única superficie de scroll gigante.

Cuando corresponda, separar:

```text
Sidebar scroll
Lista scroll
Detalle scroll
```

Cada área debe desplazarse independientemente cuando tenga sentido.

---

# 50. RESIZABLE PANELS

Cuando exista una necesidad real:

```text
Lista │ Detalle
      ↕
```

permitir ajustar tamaños.

No introducir resize solo por apariencia.

---

# 51. DASHBOARD

Un dashboard debe utilizar el espacio grande para mostrar información simultánea.

Ejemplo:

```text
┌─────────────┬─────────────┬─────────────┐
│ Ventas      │ Compras     │ Stock       │
│ S/24,320    │ S/12,430    │ 8,240       │
└─────────────┴─────────────┴─────────────┘

┌──────────────────────────┬────────────────┐
│ Ventas                   │ Alertas        │
│                          │                │
│        gráfico           │ Stock bajo     │
│                          │ Vencimientos   │
└──────────────────────────┴────────────────┘
```

---

# 52. KPI

Un KPI debe mostrar:

* valor
* significado
* periodo
* comparación cuando aporte valor

No utilizar tarjetas gigantes.

---

# 53. CAROUSEL

No utilizar Carousel para información administrativa importante.

Preferir:

* Grid
* columnas
* tabla
* paneles simultáneos

El usuario no debería deslizar para descubrir información esencial cuando existe espacio suficiente para mostrarla.

---

# 54. FULL-WIDTH

No utilizar botones gigantes ocupando toda la pantalla.

Incorrecto:

```text
┌───────────────────────────────┐
│          GUARDAR              │
└───────────────────────────────┘
```

Preferido:

```text
[Cancelar] [Guardar producto]
```

---

# 55. ICONOS

Los iconos complementan acciones.

No convertir toda la aplicación en icon-only UI.

Cuando una acción no sea obvia:

```text
✎ Editar
```

o utilizar Tooltip.

---

# 56. RESPONSIVE

Responsive significa:

> **Adaptar la composición al espacio.**

No significa:

> Convertir la aplicación en Mobile UI.

Ejemplo:

```text
Espacio amplio:
Sidebar + Lista + Detalle + Inspector

Espacio medio:
Sidebar + Lista + Detalle

Espacio menor:
Sidebar + Workspace
```

La estructura debe seguir siendo Large-Screen.

---

# 57. TOUCH + MOUSE + KEYBOARD

Toda interacción importante debe tener equivalencia:

```text
Touch
Mouse
Keyboard
```

Ejemplo:

```text
Touch → seleccionar
Mouse → click
Keyboard → Enter
```

No depender de:

* swipe
* long press
* gestos ocultos

como única interacción.

---

# 58. ACCESIBILIDAD

Todos los componentes deben contemplar:

* contenido semántico
* focus
* navegación por teclado
* contraste
* tamaño razonable
* labels
* estados
* feedback

No depender exclusivamente del color.

---

# 59. NO ABUSAR DEL ESPACIO

Large Screen no significa:

```text
████████████████████████
                        

        UNA CARD
        
                        

████████████████████████
```

El espacio debe servir para mostrar:

* más contexto
* más información relacionada
* mejor comparación
* más acciones visibles
* mejor organización

---

# 60. NO ABUSAR DE COMPONENTES

Enterprise no significa agregar componentes por todas partes.

No introducir:

* Card sin propósito
* Panel sin contenido
* Tab innecesario
* Modal innecesario
* Tooltip innecesario
* Accordion innecesario
* Animación innecesaria

Cada componente debe resolver una necesidad.

---

# 61. REGLA DE DECISIÓN

Antes de implementar una interacción:

```text
¿Es navegación principal?
→ Sidebar

¿Es navegación secundaria?
→ Tabs / local navigation

¿Es una lista de datos?
→ Data Table / Structured List

¿Es lista + detalle?
→ Master–Detail

¿Es detalle?
→ Detail Pane

¿Son propiedades?
→ Inspector

¿Es edición contextual?
→ Side Panel

¿Es formulario complejo?
→ Workspace / Side Panel amplio

¿Es confirmación crítica pequeña?
→ Dialog

¿Son filtros frecuentes?
→ Filter Bar

¿Son filtros avanzados?
→ Filter Panel

¿Es una acción principal?
→ Primary Button

¿Son acciones secundarias?
→ Toolbar / Context Menu

¿Es una operación masiva?
→ Multi-select + Bulk Actions

¿Es una operación larga?
→ Operation Status

¿Está cargando contenido?
→ Skeleton / Local Loading

¿Es un error?
→ Error State + Recovery

¿Está vacío?
→ Empty State

¿Es información persistente?
→ Notification / Activity

¿Es información relacionada?
→ Tabs / Sections

¿Es selección de muchas entidades?
→ Searchable ComboBox

¿Es una modificación pequeña?
→ Inline Editing
```

---

# 62. CHECKLIST OBLIGATORIO ANTES DE CREAR UI

Antes de implementar cualquier pantalla, comprobar:

```text
[ ] ¿La navegación principal es lateral?
[ ] ¿El usuario conserva contexto?
[ ] ¿Se aprovecha correctamente el espacio horizontal?
[ ] ¿Existe una razón para usar un Dialog?
[ ] ¿Podría ser Side Panel?
[ ] ¿Podría ser Detail Pane?
[ ] ¿Podría ser Master–Detail?
[ ] ¿Los datos deberían ser una Table?
[ ] ¿El formulario aprovecha Grid?
[ ] ¿Los filtros frecuentes están visibles?
[ ] ¿Las acciones principales están en Toolbar?
[ ] ¿El usuario puede trabajar con mouse?
[ ] ¿El usuario puede trabajar con teclado?
[ ] ¿Touch también funciona?
[ ] ¿Los estados tienen feedback claro?
[ ] ¿Los errores permiten recuperación?
[ ] ¿Las operaciones largas muestran progreso contextual?
[ ] ¿El Empty State explica qué hacer?
[ ] ¿Se conserva búsqueda/filtro/selección?
[ ] ¿Se evitó navegación innecesaria?
[ ] ¿Se evitó un componente móvil por comodidad?
```

---

# 63. REGLA PARA EL AGENTE DE IA

Cuando el agente esté a punto de utilizar:

```text
AlertDialog
Dialog
ModalBottomSheet
BottomNavigation
NavigationBar
ModalNavigationDrawer
FloatingActionButton
Toast
Snackbar
Swipe
LongPress
Card list
Single-column form
Full-screen detail
PullRefresh
Global spinner
```

debe **detenerse y evaluar la tabla Anti-Mobile de esta skill**.

No debe asumir que el componente es apropiado simplemente porque funciona técnicamente.

Debe seleccionar primero el patrón Large-Screen equivalente.

---

# 64. PRINCIPIO DE PRIORIDAD

Cuando exista conflicto entre:

```text
Material default
```

y:

```text
Enterprise Large-Screen UX
```

debe prevalecer:

**Enterprise Large-Screen UX.**

Cuando exista conflicto entre:

```text
facilidad de implementación
```

y:

```text
claridad/productividad/consistencia
```

no elegir la solución más fácil automáticamente.

La arquitectura UX debe permanecer correcta.

---

# 65. DEFINICIÓN DE ÉXITO

La aplicación terminada debe sentirse como:

```text
Software empresarial
        +
SaaS moderno
        +
Workspace profesional
        +
Alta densidad útil
        +
Contexto continuo
        +
Mouse
        +
Teclado
        +
Touch
```

No como:

```text
Aplicación móvil
        +
más ancho
        +
componentes gigantes
```

---

# 66. PRINCIPIO FINAL

**NO DISEÑAR PARA EL COMPONENTE.**

Diseñar para la tarea.

La secuencia correcta siempre es:

```text
USUARIO
   ↓
TAREA
   ↓
CONTEXTO
   ↓
INFORMACIÓN NECESARIA
   ↓
PATRÓN UX
   ↓
COMPONENTE LARGE-SCREEN
   ↓
JETPACK COMPOSE
```

La tecnología debe adaptarse al diseño.

El diseño no debe degradarse para acomodarse a los componentes disponibles.
