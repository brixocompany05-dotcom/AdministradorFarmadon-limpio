---
name: ssot-derived-state-udf
description: >-
  Una sola fuente de verdad, estado derivado y flujo unidireccional. Evita estados
  duplicados, contadores desincronizados, sincronizaciones manuales, lógica circular
  y múltiples dueños del mismo dato.
---

# SKILL — SINGLE SOURCE OF TRUTH + ESTADO DERIVADO + UDF

> **Una sola verdad. Todo lo demás se deriva. Los datos fluyen en una sola dirección.**
> Menos estados = menos sincronización = menos errores = menos lugares que mantener.

## 1. EL PRINCIPIO

Cada dato tiene UN lugar autoritativo, UN dueño y UNA forma válida de modificarse.
Todo lo demás (contadores, filtrados, ordenados, selecciones, flags derivados) es una
**PROYECCIÓN que se calcula** — jamás una copia que se mantiene sincronizada a mano.

```text
FUENTE DE VERDAD → ESTADO → DERIVACIÓN → UI
Firestore → snapshot → Repository → StateFlow → ViewModel → UI Compose
```

## 2. REGLA CENTRAL — "DERIVE, DON'T SYNCHRONIZE"

Prohibido tener simultáneamente `items`, `totalItems`, `pendingItems`, `completedItems`
si todos salen de `items`. La verdad es `items`; los números son cálculo:

```kotlin
val totalItems     = items.size
val pendingItems   = items.count { it.status == PENDING }
val completedItems = items.count { it.status == COMPLETED }
```

La consistencia queda garantizada **POR CONSTRUCCIÓN**: si el número se calcula desde la
lista, es imposible que discrepen en el mismo instante. Mantenerlos por separado
(`contador++` junto a `items.add(...)`) crea dos verdades que alguien debe sincronizar
a mano — y esa sincronización fallará algún día (lista = 10, contador = 9).

Antes de crear una variable de estado, preguntar:

1. ¿Este dato ya existe en otro lugar?
2. ¿Puede calcularse desde el estado existente?
3. ¿Cambiaría independientemente de la fuente?
4. ¿Tiene significado propio o solo representa otro dato?
5. ¿Lo creo solo para ahorrar un cálculo?
6. ¿Es una segunda copia de la misma información?

Si puede derivarse correctamente: **NO almacenarlo**. Aplica a contadores, totales,
pedidos, usuarios, notificaciones, tareas, documentos, seleccionados — cualquier
subconjunto o resumen de una colección.

## 3. VERDAD vs CONSECUENCIA (regla de oro)

Antes de escribir `var x = ...`: **¿X es una verdad o una consecuencia?**

| Tipo | Ejemplos | Trato |
|---|---|---|
| **Estado real** (existencia propia) | `orders`, `selectedOrderId`, `isLoading`, `error`, `searchQuery` | Puede ser estado |
| **Derivado** (consecuencia) | `orderCount`, `hasOrders`, `pendingCount`, `selectedOrder`, lista filtrada/ordenada | Se deriva, nunca se almacena |
| **UI temporal** (interacción real) | `isFilterSheetOpen`, `expandedItemId` | Legítimo si no duplica negocio |

No forzar SSOT en absurdo: `products` y `searchQuery` representan conceptos DISTINTOS y
coexisten bien. La regla es **una sola verdad por concepto**, no "una sola variable para
toda la app". Nota: `isLoading` es estado legítimo aunque exista `items` — representa una
condición independiente del contenido.

## 4. FILTRO, ORDEN Y SELECCIÓN TAMBIÉN SE DERIVAN

- **Filtrado:** la verdad es `products + searchQuery + filtros`; el resultado es
  proyección (`products.filter { ... }`). Prohibido almacenar `allProducts` y
  `filteredProducts` a la vez.
- **Orden:** `products.sortedBy { it.name }` — prohibido guardar `sortedProducts`.
- **Selección:** guardar la REFERENCIA (`selectedProductId: String?`), no el objeto
  completo; resolver con `products.firstOrNull { it.id == selectedProductId }`.
  Dos versiones del mismo objeto viviendo en paralelo = dos verdades peleándose.

## 5. UDF — FLUJO UNIDIRECCIONAL (Firestore · ViewModel · Compose)

```text
DATOS:      Firestore → snapshot → Repository → StateFlow → UI
INTENCIONES: UI → evento → ViewModel → Repository → Firestore
```

- El `StateFlow` ES el reflejo del backend observado — no una copia paralela con vida propia.
- La UI es función del estado: `UI = f(state)`. Observa con `collectAsStateWithLifecycle()`;
  prohibido `remember { mutableStateOf(...) }` sosteniendo datos de negocio.
- El `UiState` no lleva campos derivados sin razón independiente: incluir `productCount`
  dentro del data class cuando ya está `products` es un campo muerto esperando mentir.
- Prohibida la lógica circular (`lista → contador → lista`) y los estados actualizados
  desde múltiples lugares. El flujo se sigue de punta a punta en una sola dirección.
- Si el backend guarda un contador por rendimiento/escala, es un **dato del modelo con su
  propia política** (dueña: `cerebro-frescura`), no una copia casual que la UI mantiene aparte.

## 6. CACHÉ NO ES SEGUNDA VERDAD

Un caché es válido por rendimiento SOLO si: tiene política clara (qué tan viejo puede
estar — lo decide `cerebro-frescura`), se invalida determinísticamente y jamás contradice
a la fuente. Un caché que exige sincronización manual arbitraria ya es un segundo dueño
disfrazado.

## 7. DETECCIÓN DE SMELL (checklist única de revisión)

🔴 **Alto riesgo:**
- Dos variables representan el mismo dato; contadores con `++`/`--` junto a colecciones.
- `allX` + `filteredX`/`sortedX` almacenadas simultáneamente; objeto completo donde basta un ID.
- UI manteniendo copias del estado del ViewModel; un estado modificado desde varios lugares.
- Backend y estado local tratados como verdades simultáneas.
- Agregar/modificar un dato obliga a actualizar manualmente N estados relacionados.

🟡 **Revisar:** caches, snapshots locales, valores precalculados — ¿tienen política o son
una segunda verdad accidental?

🟢 **Correcto:** fuente única, proyecciones puras, `StateFlow` reactivo, eventos
unidireccionales, transformaciones reproducibles.

Nombres sospechosos: `totalX`, `countX`, `hasX`, `filteredX`, `sortedX`, `selectedX` →
preguntar siempre: ¿estado independiente o proyección?

## 8. PROCEDIMIENTO AL TOCAR CÓDIGO

1. **Encontrar la verdad:** ¿dónde vive este dato hoy?
2. **Buscar duplicados:** variables gemelas, contadores, listas secundarias, cachés.
3. **¿Derivable?** → derivarlo y eliminar el estado redundante.
4. **Un dueño único** por dato.
5. **Verificar el flujo:** `Backend → Estado → UI` y `UI → evento → Datos`, sin caminos laterales.
6. **Prueba final:** eliminar una variable derivada no rompe nada; agregar un dato no
   obliga a actualizar cinco estados a mano. Si lo obliga, hay duplicación — extirpar la raíz.

Las auditorías (`auditoria-verdad`) usan la sección 7 como gate de estados.

---

> **FRONTERAS (un solo camino entre skills, cero contradicciones):**
> - **Escrituras seguras, concurrencia, integridad** → skill de ROBUSTEZ/INGENIERÍA.
>   Esta skill define QUIÉN es dueño del estado; no cómo se protege una escritura.
> - **Si un conteo/caché servidor DEBE existir por escala** → la POLÍTICA la decide
>   `cerebro-frescura` (escalera de frescura); aquí solo manda que no nazca como segunda
>   verdad accidental ni pida sincronización manual.
> - **Duplicación de CONOCIMIENTO en código** (misma regla en dos rutas) → skill de
>   SIMPLICIDAD/MANTENIBILIDAD. **Duplicación de ESTADO** (mismo dato en dos variables)
>   → esta skill.
> - **Carpetas y capas** donde vive cada pieza → `organizacion-modular`.
> - **Dueño del dato ENTRE apps** (la central manda catálogos y reglas; el producto
>   consume) → lo define la constitución de cada repo. Esta skill manda DENTRO de cada app.
