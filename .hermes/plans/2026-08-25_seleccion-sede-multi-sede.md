# Plan · Elegir sede activa para usuarios multi-sede (dueño / admin / rol con capacidad)

> **For Hermes:** Plan de diseño pendiente de aprobación del dueño. NO ejecutar hasta su OK.

**Goal:** Que cualquier usuario con capacidad multi-sede (dueño, administrador u otro rol autorizado) pueda ELEGIR en qué sede opera, de forma clara, verificada y sin estados fantasma — reutilizando lo que ya existe.

---

## 1. Lo que YA existe (auditado en código real)

| Pieza | Dónde | Estado |
|---|---|---|
| Selector de sede en el menú lateral (dropdown) | `SidebarHeader.kt:99-153` | ✅ Funciona — pero SOLO visible si `esItinerante && sucursales.size > 1` |
| Acción de cambio de sede | `SidebarViewModel.cambiarSucursalActiva()` (:82-87) | ✅ Actualiza estado + SessionManager + aviso flotante |
| Lista de sedes vivas escuchada en tiempo real | `escucharSucursales()` (:317-343) | ✅ Con bandera `activa`; el menú filtra `it.activa` |
| Definición de "itinerante" (= puede elegir) | `SidebarViewModel:203`: `sucursalId=="todas" \|\| blank \|\| rolId blank` | ⚠️ Es LA regla actual de capacidad multi-sede |
| Recarga de datos al cambiar sede | `InventarioViewModel` valida `sucursalEsperada != SessionManager.sucursalIdEfectiva` | ✅ El contexto muere y renace limpio (frescura §5) |

**Regla de negocio vigente:** la capacidad multi-sede HOY se otorga de una sola forma — el módulo Personal asigna `sucursalId="todas"` (o el usuario es dueño, `rolId` vacío). No existe aún un permiso granular "puede operar N sedes específicas".

## 2. Huecos encontrados (reales, con evidencia)

### H1 — La elección no sobrevive un reinicio (el importante)
El dueño elige "San Miguel" en el menú → trabaja ahí. Cierra la app. Al reabrir:
- `_sucursalIdEfectiva` nace en `"principal"` (default :66).
- En la primera emisión de la ficha, siendo itinerante NO se restaura nada (:208-210 solo marca la carga inicial).
- Peor: `ContenedorAplicacion.kt:101-104` copia ese `"principal"` fresco **encima** de la elección guardada en SessionManager → la borra.
- Resultado: el dueño vuelve a operar sobre Principal SIN saberlo. **Estado fantasma de sesión.**

### H2 — Al elegir una sede del menú no se verifica nada en el momento
`cambiarSucursalActiva` confía en la lista ya filtrada. Carrera menor: la sede pudo desactivarse hace segundos y el menú aún la muestra. Coherente con la lección de hoy: verificar al decidir.

### H3 — Rol con capacidad multi-sede pero ficha con sede específica no tiene camino
Si mañana se quiere dar a un administrador "estas 2 sedes", no existe el modelo. **Decisión pendiente del dueño:** ¿se necesita pronto o seguimos con la regla única "todas"? (YAGNI: no construir hasta que el negocio lo pida.)

## 3. Camino propuesto

**Principio:** un solo lugar para elegir (el header del sidebar, ya construido), una sola regla de quién puede (la de hoy), y verdad verificada en cada paso.

```
╔══════════════════════════════════════════════════╗
║  Dueño/Admin itinerante abre la app              ║
║   → la app RESTAURA la última sede elegida       ║
║     (verificándola viva antes de usarla)         ║
║                                                  ║
║  Toca el nombre de sede en el menú               ║
║   → lista de sedes ACTIVAS en vivo               ║
║   → elige → aviso "Operando en X"                ║
║     → inventario/compras recargan en esa sede    ║
║     → la elección QUEDA guardada para mañana     ║
╚══════════════════════════════════════════════════╝
```

## 4. Tareas (mordiscos pequeños, en orden)

### Tarea 1 · Restaurar la elección al arrancar (cierra H1)
- **Archivo:** `SidebarViewModel.kt`
- En la primera emisión de la ficha, cuando `esItinerante==true`: leer `SessionManager.sucursalId`; si apunta a una sede real (≠ principal/todas/blank) y esa sede está viva en `sucursalesDisponibles`, fijarla como efectiva. Si está muerta → caer a "principal" con el aviso flotante verdadero.
- **Verificación:** elegir San Miguel → cerrar app → reabrir → sigue en San Miguel. Desactivar San Miguel → reabrir → cae a Principal con aviso.

### Tarea 2 · Verificar al elegir (cierra H2)
- **Archivo:** `SidebarViewModel.cambiarSucursalActiva()`
- Antes de aplicar: lectura puntual del documento de la sede (decisión del momento, foto puntual legítima). Muerta → aviso claro y NO cambiar; refrescar lista.
- **Verificación:** dos tablets; desactivar sede en una, intentar elegir en la otra → bloqueo con mensaje.

### Tarea 3 · Aviso de continuidad
- Ya existe la notificación flotante (:87). Solo asegurar que también dispare al restaurar (Tarea 1) cuando la sede restaurada ≠ principal.

### Fuera de alcance (explícito)
- Permisos granulares multi-sede por rol (H3): diferido hasta que el negocio lo pida.
- Cierre de sedes con acta: ya diferido por decisión del dueño.

## 5. Riesgos / abiertos
- Tarea 1 depende de que `sucursalesDisponibles` llegue antes que el usuario opere (es listener vivo; la ventana inicial usa default principal — aceptable, se declara).
- Ningún permiso/regla Firestore nuevo (regla del dueño).
