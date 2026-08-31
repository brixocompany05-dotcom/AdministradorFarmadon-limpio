SKILL — ENTERPRISE LARGE-SCREEN UI
Anti-Mobile · Tablet · Desktop · SaaS · Jetpack Compose
0. PROBLEMA QUE ESTA SKILL DEBE RESOLVER SIEMPRE

Un diseño que se ve bien solo en UN tamaño de pantalla está mal hecho, aunque sea "large-screen".

Prohibido:

Medir/diseñar contra un solo dispositivo (tu tablet) y asumir que "ya funciona".
Anchos, paddings o número de columnas hardcodeados en dp fijos que asumen un ancho exacto.
Que un layout se vea "apretado", con texto cortado, overlap o scroll roto en cualquier ventana entre 600dp y el ancho máximo soportado.

Obligatorio: todo layout se prueba mentalmente (o realmente) en 3 anchos mínimo — compacto grande, mediano, expandido — antes de darse por terminado. Ver sección 43.

1. MISIÓN

Esta aplicación es un software Enterprise/SaaS diseñado para pantallas grandes.

La interfaz debe sentirse como un sistema profesional de gestión empresarial, no como una aplicación móvil ampliada.

Jetpack Compose es únicamente la tecnología de implementación.

Android NO define el patrón UX. El ancho de ventana SÍ define cuánto se muestra, nunca si se rompe.

La interfaz debe priorizar:

Contexto continuo
Productividad
Densidad de información útil
Claridad
Velocidad operativa
Navegación visible
Mouse
Teclado
Touch
Accesibilidad
Consistencia
Escalabilidad
Estabilidad en cualquier ancho soportado (no solo el tuyo)
2. REGLA ABSOLUTA ANTI-MOBILE

Cuando exista un patrón típico de aplicaciones móviles y exista un patrón equivalente para pantalla grande:

EL PATRÓN LARGE-SCREEN TIENE PRIORIDAD — EN TODOS LOS ANCHOS SOPORTADOS.

No se debe implementar un patrón móvil simplemente porque exista un componente Material/Compose para hacerlo, y tampoco como "solución" cuando el espacio se reduce. Si un panel no cabe, se colapsa según la sección 43 — nunca se reemplaza por bottom nav, FAB, bottom sheet, etc.

Flujo obligatorio
text
NECESIDAD DEL USUARIO
        ↓
PATRÓN UX
        ↓
PATRÓN LARGE-SCREEN
        ↓
¿CABE EN EL ANCHO ACTUAL?
   NO → COLAPSAR SEGÚN SECCIÓN 43 (sigue siendo large-screen)
   SÍ → COMPONENTE COMPOSE

Nunca:

text
"Existe AlertDialog()"
        ↓
"Entonces uso AlertDialog()"

Nunca:

text
"No cabe en esta tablet"
        ↓
"Entonces uso un patrón mobile"
3. TABLA MAESTRA DE PROHIBICIONES Y SUSTITUTOS

Esta tabla es normativa.

❌ Patrón móvil que NO debe utilizarse como patrón principal	✅ Sustituto Large-Screen
BottomNavigation	Sidebar / Navigation Rail
NavigationBar inferior	Sidebar persistente
ModalNavigationDrawer	Persistent Sidebar
Hamburger menu como navegación principal	Sidebar visible (o Rail colapsado)
ModalBottomSheet	Side Sheet / Side Panel
BottomSheet para edición	Edit Side Panel
AlertDialog para formularios	Side Panel / Workspace
Dialog para detalles complejos	Detail Pane
Dialog para filtros	Filter Side Panel
Dialog para selección grande	Searchable Selection Panel
Dialog para crear entidades complejas	Create Workspace / Side Panel
FAB para acción empresarial principal	Primary Action Button
FAB con múltiples acciones	Toolbar / Action Group
Toast para feedback importante	Inline Feedback / Notification
Snackbar como sistema de notificaciones	Notification Center / Inline Status
Snackbar para error importante	Error State / Inline Error
Spinner global bloqueando pantalla	Skeleton / Local Loading
ProgressBar sin contexto	Operation Status
LazyColumn como lista visual de registros empresariales	Data Table / Structured List
Card para cada registro	Table Row / Data Row
Lista → pantalla detalle	Master–Detail
Pantalla completa de detalle	Detail Pane
Pantalla completa para editar algo contextual	Side Panel
Formulario de una columna	Responsive Form Grid
Dropdown con cientos de elementos	Searchable ComboBox
Botón "Filtros" como única forma de filtrar	Filter Bar
FilterBottomSheet	Filter Side Panel
Swipe como única forma de acción	Row Actions / Context Menu
Long Press como única interacción	Right Click / Context Menu
Icon-only para acciones poco obvias	Icon + Label / Tooltip
Full-width button innecesario	Compact Action Group
Pull-to-refresh como única actualización	Refresh Action
Carrusel para información importante	Grid / Multi-column Layout
Wizard móvil	Stepper / Workflow Workspace
Pantallas apiladas innecesariamente	Workspace / Split View
Bottom action buttons	Toolbar / Sticky Action Bar
Una sola columna en pantalla amplia	Responsive Multi-column Layout
Modal para propiedades	Inspector Panel
Modal para información relacionada	Detail Panel / Tabs
Menú escondido para acciones frecuentes	Toolbar
Loading de toda la aplicación	Section-level Loading
Empty screen sin explicación	Actionable Empty State
"Error" sin recuperación	Recovery Error State
Información temporal importante	Persistent Activity / Notification
Touch como única interacción	Touch + Mouse + Keyboard
Navegación sin focus	Keyboard Focus Navigation
Ancho fijo en dp copiado de un solo dispositivo	Breakpoints por ventana (sección 43)
Elementos que se cortan/overlapean si la ventana es menor	Colapso ordenado (sección 43)
4. NAVEGACIÓN
❌ PROHIBIDO

Usar navegación inferior como arquitectura principal:

text
┌───────────────────────────┐
│                           │
│       CONTENIDO           │
│                           │
├───────────────────────────┤
│ Inicio Inventario Caja    │
└───────────────────────────┘
✅ OBLIGATORIO

Utilizar navegación lateral. En ancho reducido, colapsa a Rail (íconos), nunca a barra inferior.

text
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

La navegación debe ser: persistente, visible, jerárquica, consistente en cualquier ancho soportado.

5. SIDEBAR

El Sidebar debe poder contener: logo, organización, módulos, submódulos, sección activa, badges, indicadores, configuración, cuenta.

Debe permitir estado:

text
Expanded
Collapsed (Rail: solo íconos, con tooltip)

El colapso no debe destruir la usabilidad ni ocultar módulos: solo reduce el label a ícono + tooltip. El breakpoint que decide expandido vs colapsado está en la sección 43, no un valor arbitrario elegido a ojo.

6. DIALOG
❌ PROHIBIDO COMO CONTENEDOR GENERAL

No utilizar Dialog para: formularios largos, edición compleja, creación compleja, detalle completo, gestión de lotes, filtros complejos, selección masiva, workflows.

✅ SUSTITUTO

Side Panel, Detail Pane, Inspector, Workspace, Split View.

7. EXCEPCIÓN DEL DIALOG

Dialog está permitido únicamente cuando: la interacción es corta, requiere atención inmediata, es una confirmación crítica, el contenido es pequeño, mantenerlo como superficie modal mejora la seguridad.

No convertir una excepción en el patrón general.

8. SIDE PANEL

Usar Side Panel para: edición contextual, creación sencilla, filtros, propiedades, configuración, detalles.

Debe mantener visible el contexto principal, y su ancho se ajusta con breakpoints (sección 43) — nunca ocupa toda la pantalla salvo en el ancho más reducido soportado.

9. DETAIL PANE

Usar Detail Pane cuando el usuario selecciona un elemento.

text
┌─────────────────────┬──────────────────────────────┐
│ Productos           │ Paracetamol 500 mg           │
│                     │                              │
│ Paracetamol ←       │ Información                  │
│ Amoxicilina         │ Lotes                        │
│ Ibuprofeno          │ Movimientos                  │
└─────────────────────┴──────────────────────────────┘
10. MASTER–DETAIL

Cuando exista lista + detalle, considerar Master–Detail antes de crear navegación a otra pantalla. Debe preservar búsqueda, filtros, selección, contexto en todos los anchos (en el más angosto soportado, el detalle puede ocupar todo el ancho pero se navega con "volver", nunca se corta a la mitad).

11. SPLIT VIEW

Usar cuando dos o más áreas deben permanecer visibles simultáneamente, y el ancho disponible las soporta según sección 43. No introducir Split View simplemente para llenar espacio, ni forzarlo cuando el ancho no alcanza (eso rompe el layout — mejor colapsar un panel).

12. FAB
❌ PROHIBIDO COMO ACCIÓN PRINCIPAL EMPRESARIAL

No usar (+) flotante para: nuevo producto, nueva compra, nueva orden, nuevo proveedor.

✅ SUSTITUTO
text
Inventario

[+ Nuevo producto]
13. TOOLBAR
text
Inventario

🔍 Buscar...     Filtros     Importar     Exportar     + Nuevo

Jerarquía: Primary, Secondary, Tertiary, Destructive. En ancho reducido, las acciones secundarias se agrupan en un menú "más" — nunca desaparecen ni se cortan visualmente.

14. TABLE
text
Producto       Código     Stock    Precio     Estado
──────────────────────────────────────────────────────
Paracetamol    PAR500     120      S/2.50     Activo
Amoxicilina    AMX500      42      S/4.20     Activo
Ibuprofeno     IBU400      18      S/3.80     Bajo

Debe soportar: selección, multi-select, ordenamiento, filtros, paginación, acciones, estados, columnas configurables, sticky header. En ancho reducido, las columnas de menor prioridad se ocultan primero (nunca se comprimen hasta ser ilegibles) — ver orden de prioridad en sección 43.

15. CARDS
❌ PROHIBIDO

Convertir todos los registros en Cards repetidas.

✅ UTILIZAR CARD PARA

KPI, resumen, agrupación, sección, información destacada. Para datos repetitivos y comparables: Table / Row.

16. FORMULARIOS
❌ PROHIBIDO

Formularios enormes de una sola columna cuando existe espacio, o columnas fijas que no se ajustan y terminan cortando campos en ventanas menores.

✅ OBLIGATORIO

Responsive Grid: 2–4 columnas según el ancho disponible (sección 43), nunca un número fijo de columnas sin importar el ancho. No dividir campos que deberían permanecer juntos.

17. COMBOBOX

Para pocas opciones puede utilizarse Select. Para grandes cantidades, ComboBox con búsqueda. Nunca obligar al usuario a recorrer listas enormes.

18. FILTER BAR

Filtros frecuentes visibles. Si no caben todos en el ancho actual, se agrupan en "Más filtros" (sigue siendo un patrón desktop, no un ícono de hamburguesa mobile).

Los filtros avanzados pueden abrir un Filter Side Panel.

19. SEARCH

La búsqueda debe estar integrada en el Workspace cuando sea una función frecuente. No obligar a abrir otra pantalla únicamente para buscar.

20. GLOBAL SEARCH

Debe permitir localizar productos, proveedores, compras, clientes, documentos, lotes, órdenes. Resultados agrupados por tipo.

21. TABS

Usar Tabs para información relacionada, no como navegación principal.

22. ACCORDION

Usar para información secundaria o avanzada. No esconder información crítica dentro de múltiples niveles de expansión.

23. CONTEXT MENU

Acciones secundarias mediante ⋮ o click derecho. No usar Context Menu para acciones que el usuario necesita constantemente.

24. RIGHT CLICK

Right click puede mostrar acciones contextuales, navegación, copiar, editar, abrir detalle. Debe existir una alternativa equivalente para touch/teclado.

25. HOVER

Hover puede revelar acciones secundarias, tooltip, información auxiliar, estado de fila. Nunca esconder información esencial únicamente mediante hover (en tablet sin mouse, hover no existe).

26. KEYBOARD

Soportar cuando corresponda: Tab, Shift+Tab, Enter, Escape, Arrow Keys, Space, Ctrl+K, Ctrl+N. No introducir shortcuts arbitrarios.

27. FOCUS

Todos los elementos interactivos deben tener estado Focus visible.

28. LOADING
❌ PROHIBIDO

Bloquear toda la aplicación para una operación local.

✅ SUSTITUTOS

Skeleton, Section Loading, Table Loading, Panel Loading, Button Loading, Operation Status.

29. SKELETON

Cuando se conoce la estructura del contenido, el skeleton debe aproximarse a esa estructura y respetar el mismo grid/breakpoint que el contenido real (para que no "salte" al cargar).

30. OPERATION STATUS
text
Importando productos
342 / 1,200
██████████████░░░░
[Cancelar]

Luego:

text
✓ Importación completada
1,200 procesados · 1,184 correctos · 16 requieren revisión
31. TOAST
❌ PROHIBIDO

Usar Toast para errores importantes, operaciones críticas, información que debe revisarse después, resultados empresariales.

✅ SUSTITUTO

Inline Feedback, Status, Notification Center, Activity.

32. SNACKBAR

Puede existir para feedback breve, pero no como sistema principal de comunicación.

33. NOTIFICATION CENTER
text
🔔 3
Notificaciones
● Stock bajo
● Compra recibida
● Lote próximo a vencer
34. EMPTY STATE
text
No hay productos
Todavía no has registrado productos.
[+ Nuevo producto]
35. ERROR STATE
text
No pudimos cargar el inventario.
Comprueba la conexión e inténtalo nuevamente.
[Reintentar]
36. CONFIRMATION

Debe responder: ¿qué estoy haciendo?, ¿qué será afectado?, ¿es reversible?, ¿qué ocurrirá?

37. INLINE EDITING
text
Precio: S/ 2.50   ✎  →  [ S/ 2.50 ] [✓] [×]

No abrir un formulario completo para una modificación trivial.

38. BULK ACTIONS
text
24 seleccionados
[Exportar] [Actualizar] [Asignar] [Desactivar]
39. PAGINATION
text
1–50 de 1,284
‹ 1 2 3 ... 26 ›
40. STICKY TOOLBAR

Puede permanecer visible en formularios largos o tablas. No usar sticky únicamente por estética.

41. STICKY TABLE HEADER

En tablas largas, conservar los encabezados visibles cuando el contexto lo requiera.

42. DENSIDAD
text
Cómoda
Estándar
Compacta

Nunca sacrificar legibilidad por densidad.

43. BREAKPOINTS Y COLAPSO ORDENADO (OBLIGATORIO — resuelve "se ve roto en otra tablet")

Esta es la sección que evita que el diseño solo funcione en tu dispositivo.

Clases de ventana (ancho disponible, no el tamaño físico del dispositivo)
text
COMPACT-LARGE   600dp – 839dp    (tablets chicas, ventanas divididas)
MEDIUM          840dp – 1199dp   (tablets grandes en vertical, ventanas medianas)
EXPANDED        1200dp+          (tablets grandes en horizontal, desktop)

Nunca diseñar contra un ancho exacto (ej. "1280dp porque mi tablet mide eso"). Diseñar contra estas 3 clases.

Qué cambia en cada clase (nunca cambia el PATRÓN, solo la densidad de paneles)
Elemento	COMPACT-LARGE (600–839dp)	MEDIUM (840–1199dp)	EXPANDED (1200dp+)
Sidebar	Rail (solo íconos)	Rail o expandido según toggle	Expandido por defecto
Master–Detail	Uno visible a la vez (con volver)	Ambos visibles	Master + Detail + Inspector
Formulario	1–2 columnas	2–3 columnas	3–4 columnas
Tabla	Columnas esenciales solamente	Columnas esenciales + medias	Todas las columnas
Inspector Panel	Oculto (se abre como panel full)	Visible si hay espacio	Siempre visible
Toolbar	Acciones agrupadas en "Más"	Mayoría visible	Todas visibles
Dashboard KPIs	1–2 por fila	2–3 por fila	3–4 por fila
Orden de prioridad al reducirse el ancho (qué se colapsa primero)
text
1º Inspector Panel (se oculta primero)
2º Columnas secundarias de tablas
3º Sidebar (expandido → rail)
4º Panel de detalle (se apila detrás del master, con navegación "volver")
5º Columnas del formulario (de 4 → 3 → 2 → 1)

Nunca se corta texto, nunca se sobreponen elementos, nunca aparece scroll horizontal no intencional. Si algo no cabe, se colapsa según este orden — no se "achica" hasta romperse.

Regla de verificación obligatoria

Antes de dar por terminada una pantalla, confirmar mentalmente (o probando) que se ve correcta y sin overlap en los tres anchos: 600dp, ~900dp y ~1280dp+. Si solo se probó en un dispositivo, la pantalla NO está terminada.

44. PANEL INSPECTOR
text
┌─────────────────────────────┐
│ Inspector                   │
│ Paracetamol 500 mg          │
│ Código: PAR500               │
│ Categoría: Analgésicos       │
│ Estado: ● Activo             │
│ [Editar]                    │
└─────────────────────────────┘

Visible según la clase de ventana (sección 43).

45. ACTIVITY / HISTORY
text
Actividad
10:42  Stock actualizado
10:35  Lote agregado
09:51  Precio modificado
46. AUDIT
text
Precio
Antes: S/ 2.30
Después: S/ 2.50
Usuario: Administrador
Fecha: 29/08/2026 10:42
47. STEPPER
text
① Producto  ② Presentación  ③ Inventario  ④ Confirmación

En pantalla grande preferir Stepper lateral cuando aporte contexto.

48. WORKSPACE
text
Sidebar → Inventario Workspace → Lista + Detalle + Acciones

El Workspace mantiene contexto, filtros, navegación, estado, selección — en cualquier clase de ventana.

49. SCROLL

Evitar una única superficie de scroll gigante. Separar scroll de Sidebar, Lista y Detalle cuando tenga sentido.

50. RESIZABLE PANELS

Cuando exista necesidad real, permitir ajustar tamaños (Lista ↕ Detalle). No introducir resize solo por apariencia, y el tamaño mínimo de cada panel respeta los breakpoints de la sección 43.

51. DASHBOARD
text
┌─────────────┬─────────────┬─────────────┐
│ Ventas      │ Compras     │ Stock       │
│ S/24,320    │ S/12,430    │ 8,240       │
└─────────────┴─────────────┴─────────────┘

Número de columnas de KPI según sección 43, nunca fijo.

52. KPI

Debe mostrar valor, significado, periodo, comparación cuando aporte valor. No utilizar tarjetas gigantes.

53. CAROUSEL

No utilizar para información administrativa importante. Preferir Grid/columnas/tabla/paneles simultáneos.

54. FULL-WIDTH

No utilizar botones gigantes ocupando toda la pantalla.

text
[Cancelar] [Guardar producto]
55. ICONOS

No convertir toda la aplicación en icon-only UI. Cuando una acción no sea obvia: ✎ Editar o Tooltip.

56. RESPONSIVE

Responsive significa adaptar la composición al espacio, según la sección 43 — no convertir la aplicación en Mobile UI ni dejar que se rompa entre breakpoints.

text
Expanded: Sidebar + Lista + Detalle + Inspector
Medium:   Sidebar (rail/expandido) + Lista + Detalle
Compact-Large: Sidebar rail + Workspace (uno a la vez)

La estructura debe seguir siendo Large-Screen en los tres casos.

57. TOUCH + MOUSE + KEYBOARD

Toda interacción importante debe tener equivalencia: Touch, Mouse, Keyboard. No depender de swipe, long press o gestos ocultos como única interacción.

58. ACCESIBILIDAD

Contenido semántico, focus, navegación por teclado, contraste, tamaño razonable, labels, estados, feedback. No depender exclusivamente del color.

59. NO ABUSAR DEL ESPACIO

El espacio debe servir para mostrar más contexto, más información relacionada, mejor comparación, más acciones visibles, mejor organización — no para dejar una sola card flotando en un mar de espacio vacío.

60. NO ABUSAR DE COMPONENTES

No introducir Card sin propósito, Panel sin contenido, Tab innecesario, Modal innecesario, Tooltip innecesario, Accordion innecesario, Animación innecesaria. Cada componente debe resolver una necesidad.

61. REGLA DE DECISIÓN
text
¿Es navegación principal? → Sidebar
¿Es navegación secundaria? → Tabs / local navigation
¿Es una lista de datos? → Data Table / Structured List
¿Es lista + detalle? → Master–Detail
¿Es detalle? → Detail Pane
¿Son propiedades? → Inspector
¿Es edición contextual? → Side Panel
¿Es formulario complejo? → Workspace / Side Panel amplio
¿Es confirmación crítica pequeña? → Dialog
¿Son filtros frecuentes? → Filter Bar
¿Son filtros avanzados? → Filter Panel
¿Es una acción principal? → Primary Button
¿Son acciones secundarias? → Toolbar / Context Menu
¿Es una operación masiva? → Multi-select + Bulk Actions
¿Es una operación larga? → Operation Status
¿Está cargando contenido? → Skeleton / Local Loading
¿Es un error? → Error State + Recovery
¿Está vacío? → Empty State
¿Es información persistente? → Notification / Activity
¿Es información relacionada? → Tabs / Sections
¿Es selección de muchas entidades? → Searchable ComboBox
¿Es una modificación pequeña? → Inline Editing
¿No cabe en el ancho actual? → Colapsar según sección 43, nunca patrón mobile
62. CHECKLIST OBLIGATORIO ANTES DE CREAR UI
text
[ ] ¿La navegación principal es lateral?
[ ] ¿El usuario conserva contexto?
[ ] ¿Se aprovecha correctamente el espacio horizontal?
[ ] ¿Existe una razón para usar un Dialog?
[ ] ¿Podría ser Side Panel / Detail Pane / Master–Detail?
[ ] ¿Los datos deberían ser una Table?
[ ] ¿El formulario aprovecha Grid según el breakpoint?
[ ] ¿Los filtros frecuentes están visibles?
[ ] ¿Las acciones principales están en Toolbar?
[ ] ¿El usuario puede trabajar con mouse, teclado y touch?
[ ] ¿Los estados tienen feedback claro?
[ ] ¿Los errores permiten recuperación?
[ ] ¿Las operaciones largas muestran progreso contextual?
[ ] ¿El Empty State explica qué hacer?
[ ] ¿Se conserva búsqueda/filtro/selección al colapsar?
[ ] ¿Se evitó navegación innecesaria?
[ ] ¿Se evitó un componente móvil por comodidad, incluso al reducirse el espacio?
[ ] ¿SE PROBÓ EN LOS TRES BREAKPOINTS (600dp / ~900dp / 1200dp+) SIN OVERLAP NI CORTES?
63. REGLA PARA EL AGENTE DE IA

Cuando el agente esté a punto de utilizar AlertDialog, Dialog, ModalBottomSheet, BottomNavigation, NavigationBar, ModalNavigationDrawer, FloatingActionButton, Toast, Snackbar, Swipe, LongPress, Card list, formulario de una sola columna, detalle full-screen, PullRefresh o spinner global — o cuando esté a punto de fijar un dp exacto en vez de usar breakpoints — debe detenerse y evaluar la tabla Anti-Mobile y la sección 43 de esta skill.

No debe asumir que el componente o el ancho es apropiado simplemente porque funciona en el dispositivo donde se está probando. Debe seleccionar primero el patrón Large-Screen equivalente y verificarlo en los tres breakpoints.

64. PRINCIPIO DE PRIORIDAD

Cuando exista conflicto entre Material default y Enterprise Large-Screen UX, prevalece Enterprise Large-Screen UX.

Cuando exista conflicto entre facilidad de implementación (ej. un ancho fijo que "ya funciona en mi tablet") y claridad/productividad/consistencia en todos los anchos, no elegir la solución más fácil automáticamente. La arquitectura UX debe permanecer correcta en cualquier dispositivo soportado, no solo el de prueba.

65. DEFINICIÓN DE ÉXITO
text
Software empresarial + SaaS moderno + Workspace profesional
+ Alta densidad útil + Contexto continuo
+ Mouse + Teclado + Touch
+ Se ve correcto en CUALQUIER tablet/desktop soportado, no solo en una

No como: aplicación móvil + más ancho + componentes gigantes + roto en otros tamaños.

66. PRINCIPIO FINAL

NO DISEÑAR PARA EL COMPONENTE, NI PARA TU DISPOSITIVO.

Diseñar para la tarea, en cualquier ancho soportado.

text
USUARIO → TAREA → CONTEXTO → INFORMACIÓN NECESARIA
→ PATRÓN UX → BREAKPOINT ACTUAL (sección 43)
→ COMPONENTE LARGE-SCREEN → JETPACK COMPOSE

La tecnología debe adaptarse al diseño, y el diseño debe adaptarse al ancho real disponible — nunca romperse ni degradarse a un patrón mobile.
