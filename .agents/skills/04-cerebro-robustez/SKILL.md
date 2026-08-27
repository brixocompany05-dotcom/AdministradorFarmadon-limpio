---
name: cerebro-robustez
description: ¿Aguanta la vida real? Sin mezcla entre farmacias, guardados completos, cero errores ocultos, cero crashes, cero mala UX.
---

# CEREBRO 04: ROBUSTEZ (AGUANTA LA VIDA REAL)

> Robustez no es paranoia. Es que la app jamás mienta, jamás se caiga en silencio
> y jamás deje al usuario sin saber qué está pasando.

---

## 1. La única protección que importa: aislamiento entre farmacias (R1)
Cada farmacia es un cliente de BRIXO. Una farmacia jamás ve ni toca los datos de otra.
Toda consulta o guardado verifica explícitamente a qué farmacia pertenece el dato.
Esto NO es paranoia anti-hacker; es la regla de producto más importante del sistema.

---

## 2. Cero Crashes — Reglas de Código Android Kotlin

Estas reglas nacen de crashes reales. Violarlas produce cierres de app sin aviso:

### A. Constructores de ViewModel (CRÍTICO)
- `viewModel()` sin Factory **solo crea ViewModels con un único argumento: `Application`**.
- Los parámetros con valor por defecto de Kotlin (`= Algo()`) **NO existen en el bytecode Java**.
  El sistema los busca por reflexión y no los encuentra → crash.
- **Regla:** Si un `AndroidViewModel` necesita colaboradores, se instancian como propiedades
  internas (`private val repo = MiRepo()`), NO en el constructor.
- Si se necesita inyectar para testing, se usa el patrón `constructor(app) : this(app, MiRepo())`.

### B. Casteo de Context (CRÍTICO)
- `LocalContext.current as ComponentActivity` → crash cuando Compose envuelve el contexto
  con un `ContextThemeWrapper` (tema dinámico).
- **Regla:** Nunca hacer casteo manual de `Context` a `Activity`. Usar `viewModel()` directo,
  `LocalContext.current as? Activity` o `LocalLifecycleOwner.current`.

### C. Inicialización de Firebase en Application
- `FirebaseFirestore.getInstance().firestoreSettings = settings` falla con
  `IllegalStateException` si Firestore ya fue inicializado por un content provider.
- **Regla:** No reconfigurar Firestore después de `FirebaseApp.initializeApp()`. Si se
  necesita configuración especial, hacerla en el primer acceso real, no en `Application.onCreate()`.

### D. Source.SERVER en lecturas
- `.get(Source.SERVER)` falla si no hay red y congela la UI en red lenta.
- **Regla:** Usar `.get()` estándar (cache + red). Solo `Source.SERVER` si hay razón de
  negocio explícita documentada en el código. Siempre dentro de un `try/catch` con
  mensaje claro al usuario.

### E. Carrera de hilos en ciclo de vida
- Lanzar `SessionManager.getInstance()` en background (`appScope.launch`) al mismo tiempo
  que `MainActivity.onCreate()` lo llama en el hilo principal → condición de carrera.
- **Regla:** Los singletons de sesión se inicializan determinísticamente en el hilo que
  los necesita primero. Sin lanzamientos preventivos en `Application.onCreate()`.

### F. FieldValue.serverTimestamp() en Arreglos / arrayUnion (CRÍTICO)
- Regla definida en **R10 del AGENTS.md** (fuente única): `serverTimestamp()` SOLO va
  en campos de primer nivel o submapas directos. Prohibido dentro de listas o
  `arrayUnion()` — provoca `IllegalArgumentException` y aborta la transacción.
  En elementos de listas usar `Timestamp.now()` o `Date()`.

---

## 3. Cero Fallos Silenciosos ni Maquillaje de Errores

- **Prohibido `try/catch {}` vacíos.** Si se atrapa una excepción y no se muestra
  al usuario NI se registra en log, es un fallo silencioso. Mínimo: `Log.e(TAG, msg, e)`.
- **Prohibido el maquillaje de errores:** Toda falla de Firebase o del servidor debe
  traducirse con la **verdad real**. Prohibido poner textos genéricos como "Error inesperado"
  ocultando el motivo técnico real (`e.message` o código de error). El mensaje debe ser
  elegante y comprensible, pero veraz con la causa auténtica.
- **Prohibido `?.let { }` sin rama else cuando el nulo importa.** Si un campo puede
  ser nulo y afecta la operación, se maneja el caso nulo explícitamente.
- **Prohibido fingir éxito.** Si `onSuccess()` se llama sin verificar que Firestore
  confirmó la escritura, es mentira. Se usa `.await()` y se atrapa el error.
- **Prohibido estados de carga infinita.** Todo `isLoading = true` tiene su par
  `isLoading = false` en el `finally {}` del `try/catch`.

---

## 4. Cero Mala UX (Prevención Proactiva)

Estos problemas son tan graves como los crashes porque rompen la confianza del usuario:

### A. Estado residual entre sesiones
- Al cerrar sesión o ser expulsado, **todo el estado visual se limpia**: PIN vacío,
  formularios vacíos, errores borrados.
- **Regla:** `limpiarEstado()` se llama en `SessionState.LoggedOut` y `SessionState.Revoked`.
  Nunca confiar en que Compose lo descarte automáticamente por recomposición.

### B. Botones activos durante operación en vuelo
- Si el usuario puede tocar un botón mientras la operación ya está procesándose,
  puede duplicar la acción o corromper el estado.
- **Regla:** Todo botón que dispara una operación de red se deshabilita (`enabled = false`)
  mientras `isLoading = true`. Mostrar indicador visual de progreso.

### C. Primera vez vs. usuario activo
- Un solicitante que nunca fue aprobado **no debe ver "Acceso Suspendido"**.
- **Regla:** Antes de mostrar cualquier pantalla de error o suspensión, verificar que el
  usuario tiene un `clienteId` válido y aprobado. Sin clienteId → flujo de solicitud, no error.

### D. LaunchedEffect mal clave
- `LaunchedEffect(Unit)` se re-ejecuta en cada recomposición cuando el bloque cambia el
  estado que lo rodea → bucle de reseteo.
- **Regla:** Usar claves estables (`LaunchedEffect(sessionState)`, `LaunchedEffect(userId)`)
  que solo disparan cuando el dato que importa realmente cambia.

### E. Verdad en Tiempo Real (Cero Fotos Estáticas)
- Una vista interactiva o de consulta de estado (ej: consulta rápida de expediente) **jamás debe operar con un `.get()` estático desconectado**.
- **Regla:** Usar flujos reactivos (`callbackFlow` / `addSnapshotListener`) que actualicen la UI al instante (<50ms) cuando el servidor o BrixoPanel modifique los datos, cancelando la escucha al salir.

### F. Cero Datos de Relleno Manual ni Placeholders Falsos por Decoración
- Prohibido quemar datos falsos o nombres de fantasía en placeholders (`"Ej: Mi Empresa S.A.C."`, `"Ej: 20601234567"`) o inventar registros de prueba para 'llenar la pantalla'.
- Si la base de datos está vacía, la pantalla DEBE mostrar su estado vacío real (`emptyList()`, `""`, 0 elementos).
- **Regla:** Toda información en la interfaz proviene 100% de Firestore en tiempo real. La calidad se demuestra con diseño limpio y estados vacíos simétricos, jamás con maquillaje ni falsedad.

---

## 5. Todo o nada (Guardados completos)

Las operaciones críticas se hacen completas: verificar → validar → guardar todo junto.
Si algo falla, se revierte limpio y se puede reintentar sin duplicar ni romper datos.
Prohibido guardar a medias: un dato a medias es un dato mal guardado.

---

## 6. Red caída o lenta — Resiliencia y verdad de conexión

- **Firebase SDK decide la conectividad:** La app no debe abortar prematuramente porque el
  internet sea lento. Firebase gestiona reconexiones automáticas y persistencia.
- **No asumir "Sin conexión" antes de tiempo:** Solo emitir alerta de desconexión si Firebase
  o la verificación de red lo confirma fehacientemente tras los reintentos.

| Situación | Comportamiento correcto |
|---|---|
| Red lenta | Spinner + mensaje "Conectando con la central..." con tiempo prudente |
| Sin red confirmada | Mensaje claro: "Sin conexión a internet" + botón Reintentar |
| Error del servidor / Firebase | Mensaje claro con la causa real (`e.message`). Cero "Error inesperado" maquillaje |
| Éxito parcial | Informar qué se guardó y qué no. Nunca decir "guardado" si no se verificó |

---

## 7. Checklist de Robustez antes de entregar cualquier cambio

- [ ] ¿El ViewModel tiene solo `Application` en el constructor si extiende `AndroidViewModel`?
- [ ] ¿Hay algún `FieldValue.serverTimestamp()` adentro de una lista o `arrayUnion`? (Debe ser `Timestamp.now()`).
- [ ] ¿Hay algún `try/catch` que trague el error o muestre un mensaje genérico ("Error inesperado") en vez de la causa real?
- [ ] ¿Hay algún `Source.SERVER` sin manejo de error de red?
- [ ] ¿Todo `isLoading = true` tiene su `finally { isLoading = false }`?
- [ ] ¿Los botones se deshabilitan mientras hay operación en vuelo?
- [ ] ¿Al hacer logout el estado visual queda limpio?
- [ ] ¿Un usuario nuevo puede ver pantallas de error que no le corresponden?
- [ ] ¿Hay algún `LaunchedEffect(Unit)` que debería tener una clave más específica?

