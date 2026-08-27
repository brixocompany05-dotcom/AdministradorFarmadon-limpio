---
name: cerebro-diseno
description: Lógica visual profesional, jerarquía natural y diseño sin estorbos para que el personal de la farmacia opere rápido, sin errores y con orgullo. Optimizado para SaaS en Tablet Horizontal.
---

1. Principio supremo

No preguntar "¿cómo adapto esta pantalla móvil a tablet?". Preguntar: "Si esta app hubiera nacido para esta pantalla, ¿cómo estaría organizada?"

Reconstruir la composición desde cero cuando haga falta. No conservar una estructura móvil solo porque ya existe el código.

Prueba del "móvil estirado": si al reducir mentalmente la pantalla a un teléfono la estructura sigue viéndose igual, es una UI móvil adaptada → rediseñar.

Señales de alerta (no convertir automáticamente):

columna → columna más ancha
card móvil → card gigante
formulario vertical → mismo formulario con más espacio
botón móvil → botón enorme
bottom sheet → panel gigante
FAB → acción principal
lista de cards → lista interminable
espacio vacío usado como decoración, no como estructura
2. Composición espacial

Pensar en zonas de trabajo, no en componentes sueltos:

┌────────────┬──────────────────────────────────────────┐
│ Navegación │             Área de trabajo               │
│            │  ┌────────────────┬───────────────┐       │
│            │  │ Lista / datos  │ Contexto/detalle│      │
│            │  └────────────────┴───────────────┘       │
└────────────┴──────────────────────────────────────────┘

Orden mental: espacio → relaciones → jerarquía → flujo → componentes (nunca al revés).

Toda pantalla necesita:

Punto de entrada — dónde estoy, qué veo, qué puedo hacer.
Área principal — el trabajo real.
Contexto — información relacionada visible cuando aporta.
Acciones — asociadas visualmente al objeto que modifican.
Respiración — espacio que separa jerarquías, no que rellena el monitor.
3. Jerarquía visual
Nivel 1  Qué estoy haciendo
Nivel 2  Qué información necesito
Nivel 3  Qué puedo modificar
Nivel 4  Qué acciones puedo ejecutar
Nivel 5  Información secundaria

Regla: si todo llama la atención, nada tiene jerarquía. Evitar botones/títulos/cards/colores todos con el mismo peso.

Una sola acción primaria por pantalla; el resto son secundarias o terciarias. Nunca [GUARDAR] [CANCELAR] [ELIMINAR] [EXPORTAR] con igual peso visual.

Regla de no competencia: en una misma zona no deben competir dos títulos principales, dos acciones principales, ni varios elementos de énfasis a la vez. Debe existir una lectura natural: primero → después → finalmente.

4. Estética profesional (referencia tipo Apple)

Buscar: precisión, calma, simplicidad, consistencia, profundidad sutil, buena proporción, controles discretos. No copiar literalmente macOS/iPadOS; usar como referencia conceptual.

Evitar exceso de: glassmorphism, sombras, bordes, gradientes, colores, iconos, cards, elementos gigantes, decoración sin función.

La sofisticación viene de: proporción + espaciado + tipografía + jerarquía + alineación + consistencia — no de efectos visuales.

Profundidad con propósito (no todo necesita sombra/borde/fondo):

Nivel 0  Fondo
Nivel 1  Área de trabajo
Nivel 2  Panel
Nivel 3  Control / elemento interactivo
Nivel 4  Popover / overlay
5. Alineación y proporción

Los elementos deben formar líneas visuales compartiendo ejes (títulos, campos, columnas, acciones). La alineación invisible es una de las principales fuentes de sensación profesional.

Nunca agrandar un componente solo porque hay más espacio. Un botón, campo o card no crece porque la pantalla es más grande. El espacio extra se usa para: mostrar más contexto, crear columnas, comparar información, separar zonas, reducir navegación — no para inflar controles.

Horizontalidad sobre verticalidad cuando la tarea se beneficie:

Móvil:                          Pantalla grande:
Proveedor                       Proveedor   Fecha      Factura   Estado
[________]                      [______]    [______]   [______]  [____]
Fecha
[________]

Pero las columnas deben representar relaciones reales entre datos, no aplicarse por estética.

6. Patrones de contenido

Formularios: agrupar por sección (ej. "Información general", "Condiciones"), no una card por campo ni una fila completa por input si cabe una composición más eficiente.

Tablas vs. cards: si el usuario necesita comparar registros, usar tabla. No convertir cada fila en una card.

Maestro + detalle: cuando el usuario selecciona algo y necesita revisar/editar, usar layout de lista + detalle en la misma pantalla en vez de forzar navegación completa (lista → abrir → detalle → volver → repetir).

Contexto continuo: antes de abrir una pantalla nueva, preguntar si el usuario necesita seguir viendo lo anterior. Si sí, preferir split view, panel lateral, edición inline o popover contextual en vez de navegación completa por costumbre móvil.

Acciones contextuales: deben vivir cerca de lo que modifican (ej. Producto   Stock   Precio   [Editar]), no aisladas al fondo de la pantalla.

Barra de acciones: para operaciones importantes o formularios extensos, usar una barra persistente (Cancelar | Guardar cambios) en vez de depender de un FAB solo porque hay espacio.

Modales: no usarlos como solución universal. Antes de abrir uno, preguntar si realmente necesita interrumpir al usuario. Preferir inline feedback, popover, banner o panel lateral cuando sea posible.

7. Navegación

Preferir: Navigation Rail, Permanent Navigation Drawer, Sidebar, Toolbar estructurada. Evitar: Bottom Navigation, navegación solo por gestos, menús escondidos, cualquier cosa que obligue a abandonar el contexto.

8. Densidad y responsive real

Densidad = tarea, no capricho:

Densa → comparar muchos datos
Media → lectura + edición
Aireada → concentración o decisión

Responsive real cambia la composición, no solo escala tamaños (width = screenWidth no es responsive):

Espacio reducido:   Lista → Detalle (navegación)
Espacio suficiente: Lista | Detalle (split)
Espacio amplio:     Navegación | Lista | Detalle
9. Interacción y estados

La UI debe funcionar bien con touch, mouse, teclado y stylus: hover, pressed, focus visible, selección, Tab/Enter/Escape, tooltips, targets adecuados. Nunca depender solo de swipe, long press o gestos invisibles.

Todo control importante necesita estados: default, hover, pressed, focused, disabled, loading, success, error.

Toda pantalla necesita estados: loading, vacío, error, sin resultados, guardando, guardado, datos incompletos, conflicto, selección, edición.

10. Consistencia del sistema

Toda pantalla comparte: escala tipográfica, radios, espacios, controles, estados, iconos, niveles de superficie, jerarquía de color. No diseñar cada pantalla como una app distinta.

Elementos equivalentes se comportan igual: misma escala, posición relativa, estados y jerarquía para botones o tablas similares. El usuario aprende el sistema una sola vez.

11. "Menos, pero mejor"

Antes de añadir algo, preguntar:

¿Este elemento ayuda al usuario?
¿Esta card agrupa información o solo la encierra?
¿Este botón es realmente necesario?
¿Esta sección tiene una responsabilidad clara?

Si la respuesta es no → eliminar. La interfaz profesional se logra quitando, no agregando.

12. Proceso de diseño (antes del código)

Antes de tocar Compose, definir mentalmente:

Qué intenta hacer el usuario.
Qué información necesita y cuál puede ocultarse.
Qué debe compararse.
Cuál es la acción principal y cuáles las secundarias.
Qué estructura espacial representa mejor ese trabajo.
Cómo se comporta en distintos tamaños de pantalla.
Qué estados tendrá.

Solo después: layout → componentes → implementación en Compose. Nunca empezar por "¿qué componente de Compose uso?".

13. Sistema de diseño: tipografía, color y botones

Definir esto una sola vez como tokens y reutilizarlo en toda la app. Nunca improvisar tamaños o colores pantalla por pantalla.

Tipografía (escala jerárquica)
Uso	Tamaño (sp)	Peso	Ejemplo Compose
Título de pantalla (H1)	28–32	Bold / SemiBold	titleLarge
Título de sección (H2)	20–22	SemiBold	titleMedium
Subtítulo / encabezado de card	16–18	Medium	titleSmall
Texto de cuerpo	14–16	Regular	bodyLarge / bodyMedium
Texto secundario / descripción	12–13	Regular	bodySmall
Etiqueta de campo (label)	12–13	Medium	labelMedium
Texto de botón	14–15	SemiBold	labelLarge
Hint / placeholder de textfield	14	Regular, color atenuado (~60% opacidad)	bodyMedium con onSurfaceVariant
Caption / metadatos (fecha, id)	11–12	Regular	labelSmall

Reglas:

Máximo 3 pesos de fuente en toda la app (Regular, Medium, SemiBold/Bold). No usar Light ni Black salvo casos muy puntuales.
La jerarquía se construye con tamaño + peso + color, no solo con tamaño.
El texto de cuerpo nunca baja de 13sp (legibilidad en escritorio/tablet).
Interlineado (line-height) generoso en párrafos: 1.4–1.6x el tamaño de fuente.
Botones (tamaño, geometría y simetría)
Tipo	Altura	Padding horizontal	Radio de esquina	Ancho
Primario (filled)	40–44dp	20–24dp	8–10dp	Ajustado al texto, no full-width salvo mobile/modal angosto
Secundario (outlined/tonal)	40–44dp	20–24dp	8–10dp	Igual altura que el primario, misma línea base
Terciario (text button)	36–40dp	12–16dp	8dp	Ajustado al texto
Icon button	36–40dp cuadrado	—	50% (circular) o 8dp	Simétrico (ancho = alto)
FAB (solo si aplica)	56dp	—	16dp	Cuadrado/circular simétrico

Reglas de geometría:

Botones en una misma fila comparten la misma altura exacta, aunque tengan distinto texto o icono.
El radio de esquina es el mismo en todos los botones del mismo tipo (no mezclar 4dp, 8dp y 12dp en la misma pantalla).
Icon buttons son siempre simétricos (mismo ancho y alto); nunca rectangulares.
El icono dentro de un botón mide 18–20dp y va separado del texto por 8dp.
Espaciado entre botones agrupados: 8–12dp.
Nunca estirar un botón a lo ancho completo en pantallas grandes solo porque hay espacio (ver regla de proporción, sección 5).
Color por rol (no por nombre de color)

Definir los colores como roles semánticos, no como "azul" o "rojo" sueltos, para que todo el sistema sea consistente:

Rol	Uso	Ejemplo (Material 3)
primary	Acción principal, botón primario, elementos de marca	Color de marca
onPrimary	Texto/icono sobre primary	Blanco o contraste alto
secondary	Acciones secundarias, chips, controles menos prioritarios	Tono neutro o complementario
success	Confirmaciones, estados "guardado", "activo"	Verde
warning	Alertas no críticas, datos por revisar	Ámbar/naranja
error	Errores, validaciones fallidas, acciones destructivas	Rojo
background	Fondo general de la pantalla	Neutro muy claro / oscuro según tema
surface	Fondo de cards, paneles, sheets	Un tono por encima del background
surfaceVariant	Fondo de textfields, filas alternas de tabla	Neutro sutil, distinto de surface
outline	Bordes de inputs, separadores	Gris bajo contraste
onSurface	Texto principal sobre superficies	Casi negro / casi blanco
onSurfaceVariant	Texto secundario, hints, placeholders	Gris medio

Reglas de uso:

Botón de acción primaria → fondo primary, texto onPrimary.
Botón secundario → borde o fondo tenue con secondary/surfaceVariant, texto onSurface.
Alerta/error (validación, banner de error) → fondo error al 10–15% de opacidad, texto/icono en error sólido. Nunca rojo saturado como fondo completo de un banner.
Éxito (guardado, confirmación) → mismo patrón que error pero con success: fondo tenue, icono/texto sólido.
Advertencia → mismo patrón con warning.
Cabeceras de pantalla/sección → fondo surface o background, texto onSurface con mayor peso tipográfico, sin necesidad de color de marca salvo que sea un elemento clave.
TextField → fondo surfaceVariant, borde outline (o sin borde si el fondo ya distingue el campo), texto onSurface, hint en onSurfaceVariant con opacidad reducida.
Estados de foco → borde o contorno con primary al enfocar un input.
Nunca usar más de 1 color de acento (primary) + colores semánticos (success/warning/error) en una misma pantalla. Todo lo demás son neutros.
Mantener el mismo mapeo de rol → color en modo claro y oscuro (cambia el valor, no el significado del rol).
14. Checklist de validación final
 ¿Se ve diseñada para pantalla grande desde cero, o es una UI móvil estirada?
 ¿Hay una acción primaria clara y el resto subordinadas?
 ¿El usuario compara información sin navegar de más?
 ¿Se usó tabla en vez de cards para comparar registros?
 ¿Las acciones están cerca de lo que modifican?
 ¿Funciona bien con mouse, teclado, touch y stylus?
 ¿Las alineaciones y proporciones son consistentes (nada agrandado sin motivo)?
 ¿Hay exceso de cards, bordes, sombras o colores?
 ¿El espacio vacío mejora la comprensión o solo llena la pantalla?
 ¿Se usan máximo 3 pesos tipográficos y la escala definida en la sección 13?
 ¿Los botones del mismo tipo comparten altura, radio y espaciado (simetría geométrica)?
 ¿Los colores se aplican por rol semántico (primary/success/warning/error) y no sueltos?
 ¿Alertas de éxito/error/advertencia usan fondo tenue + color sólido, no saturado completo?
 ¿Textfields, hints y cabeceras siguen el mismo mapeo de color en toda la app?

Pregunta final: ¿esto parece software profesional diseñado para una estación de trabajo, o una app móvil que hicieron más grande? Si parece móvil, rediseñar la composición.

Regla final

No diseñar componentes: diseñar experiencias. No llenar espacio: organizarlo. No escalar interfaces móviles: replantearlas. No decorar para parecer profesional: crear orden hasta que lo sea.

Objetivo: calma + precisión + contexto + productividad + sofisticación.