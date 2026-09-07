# AGENTS.md — FARMADON

## Manifiesto · El producto que BRIXO le vende a las farmacias

**Rol:** Socio de producto senior.  
**Misión:** Farmadon es el software que el personal de una farmacia usa en su día a día
(ventas, inventario, stock). Farmadon es un producto de BRIXO: **BRIXO es el dueño y la
central de control** (BrixoPanel es la sala de máquinas de BRIXO, donde se aprueba el alta
de una farmacia, se vigilan sus suscripciones y sus cobros). Cada farmacia es un CLIENTE
de BRIXO que opera con Farmadon; las reglas, planes y catálogos los define la central
(BrixoPanel) y Farmadon los consume y ejecuta. **Nada importante nace solo en Farmadon:
lo define BRIXO y Farmadon lo hace funcionar de forma limpia, directa y robusta.**

---

## REGLAS OBLIGATORIAS (Cualquier IA que trabaje aquí las sigue, sin excepción)

### R1. El único enemigo: que una farmacia pise o rompa los datos de otra
La única "protección" que importa es que cada farmacia vea y toque SOLO sus propios datos
(ventas, inventario, stock, clientes). Prohibido mezclar, duplicar o pisar datos entre
farmacias, y prohibido dejar datos a medias o mal guardados.

### R2. Prohibido el gasto en paranoia anti-hacker
Prohibido agregar defensas floridas o paranoia de seguridad que atrase el desarrollo:
límites de intentos de contraseña, candados anti fuerza-bruta, verificación de identidad
de intrusos, escenarios "alguien quiere robar o hackear", etc. Ese escenario NO es
objetivo. Toda la energía va a que la app FUNCIONE sin errores y a que las farmacias no
se pisen entre sí.

**ACLARACIÓN (no contradice R1):** Esto NO prohíbe ni reemplaza las reglas mínimas que
garantizan el aislamiento entre farmacias (R1). Verificar a qué farmacia pertenece cada
dato y que una farmacia jamás acceda a datos de otra SÍ es obligatorio, y se hace tanto en
la app como en las reglas del servidor (Firestore Security Rules). El aislamiento es una
regla de producto (R1); lo que se veta es la paranoia anti-hacker, no la protección de R1.

> **REGLAS SOBRE PERMISOS Y SEGURIDAD (decisión del dueño — 2026-08-21):**
> - **Cero paranoia anti-hacker:** Prohibido agregar permisos, candados, verificaciones de
>   identidad, límites de intentos o defensas "por si alguien quiere hackear". Ese escenario
>   NO es objetivo y atrasa el desarrollo (R2).
> - **Solo lo esencial de aislamiento:** Lo único obligatorio es que cada farmacia vea y toque
>   SOLO sus datos (R1) — verificado en la app y en Firestore Security Rules.
> - **Ningún permiso/regla de seguridad nueva sin tu aprobación:** Cualquier cambio de
>   permisos, regla de Firestore, rol o restricción se propone y se implementa **SOLO** si tú
>   lo apruebas explícitamente. La IA no inventa ni anticipa defensas.
> - `firebase-security-rules-auditor` queda como referencia técnica **solo bajo pedido
>   explícito**: jamás se aplica por defecto ni dicta diseño (choca con R2).

### R3. Todo debe funcionar, siempre (Cero Fallos Silenciosos ni Ilógicas)
- Toda acción del usuario termina en éxito visible o en un error claro con el siguiente
  paso. Prohibido fingir éxito, tragarse errores o mostrar "guardado" sin verificar que
  se guardó.
- **Bloqueos visibles para la persona:** si una acción está invalidada por reglas de negocio,
  la persona debe verlo en la UI exactamente donde ocurre (campo, panel o botón), no como
  aviso oculto ni como excepción del programador. El sistema bloquea la acción con feedback
  humano, profesional y concreto.
- **Éxito visible para la persona:** cuando la operación se completa, la UI debe mostrar
  confirmación real y clara (toast, banner, estado o mensaje de éxito) y no dejar la pantalla
  en un estado ambiguo. El éxito no puede ser solo un log en consola.
- **`catch` solo sirve al programador:** el bloque `catch` es para log, trazabilidad y
  diagnóstico técnico; nunca para ocultar un error al usuario, inventar un éxito o reemplazar
  la verdad del negocio por un mensaje genérico.
- **Prohibido fallos silenciosos:** Prohibido dejar `try/catch` vacíos o selectores incompletos
  que devuelvan valores nulos o por defecto engañosos que oculten que un dato, lote, fecha o
  cálculo no se pudo procesar.
- **Prohibido tareas o alertas ficticias:** Si una acción física nunca ocurrió (ej: nunca se
  imprimieron etiquetas para un producto), está prohibido generar alertas o tareas pendientes falsas.
- Prohibido datos falsos o desactualizados en pantalla.
- Las operaciones que tocan varios datos se guardan completas o no se guardan (nada a
  medias).

### R4. REGLA DE SIMPLICIDAD ARQUITECTÓNICA
- **La arquitectura mínima es la correcta:** Antes de crear una clase, capa, interfaz,
  mapper, use case, manager, helper o abstracción, se debe demostrar qué problema
  concreto de la implementación ACTUAL resuelve. Si no existe un problema real, no se crea.
- **Responsabilidad clara ≠ arquitectura fragmentada:** Menos piezas con
  responsabilidades claras es mejor que muchas piezas con divisiones artificiales. Si
  UI -> Lógica -> Datos funciona limpiamente, esa es la mejor solución.

### R5. NO ANTICIPAR ARQUITECTURA (Cero "Por si acaso")
- **Prohibido el diseño preventivo:** No crear estructuras "por si mañana" necesitamos
  escalabilidad, múltiples fuentes de datos o cambios de plataforma. Se diseña para el
  hoy, asegurando que sea fácil de cambiar, pero sin construir infraestructura para
  problemas hipotéticos.
- **Cero parches sobre parches:** Si algo está mal, se elimina y se hace bien de raíz.
  Si el plano (el código) se volvió un laberinto, se dibuja el plano simple desde cero.

### R6. Cada responsabilidad vive en su carpeta y su clase
- El código se organiza por módulo (una responsabilidad = una carpeta) y dentro de cada
  módulo por capas (pantalla, lógica, datos) en clases pequeñas y legibles.
- Prohibido clases gigantes que lo hacen todo. Si una clase ya no cumple su función o
  estorba, se elimina y se escribe la correcta.
- El resultado: código mantenible, legible y fácil de seguir por cualquier persona o IA.

### R7. Los datos operativos son de cada farmacia
Cada farmacia es dueña de sus datos operativos (ventas, inventario, stock, clientes). La
central (BrixoPanel/BRIXO) no lee ni toca los datos operativos de una farmacia salvo que
sea estrictamente necesario para operar (cobros, suscripciones, soporte). Los datos de los
clientes de una farmacia jamás se mezclan ni se exponen a otras farmacias ni a la central.
Solo la protección mínima que cumple R1; sin sobre-ingeniería de seguridad.

### R8. Verdad Vigente (Cero Datos Congelados)
Toda vista operativa, de consulta o de estado (ej: consulta de expediente, estado de cuenta,
solicitudes) mantiene su verdad VIGENTE según el contrato de frescura de cada dato
(skill `firebase-android`): escucha viva (`addSnapshotListener` / `callbackFlow`) cuando el
instante exacto importa; recarga por contrato (entrar, volver a primer plano, tras la
acción) cuando el dato tolera vejez breve; foto puntual (`.get()`) solo cuando es decisión
del momento y la operación final se verifica atómica (`tx.get`). Prohibida toda foto
estática que deje la pantalla congelada indefinidamente obligando al usuario a refrescar
o salir para enterarse de una actualización del servidor. Al salir de la pantalla, los
listeners se cancelan limpiamente.

### R9. Cero Errores Maquillados: Traducir la Falla Real
- Toda falla del servidor o de Firebase (Auth, Firestore, Storage) debe comunicarse con la **verdad real**.
- Prohibido reemplazar errores técnicos por comodines genéricos ("Error inesperado", "Intente más tarde").
  El diseño debe ser profesional y humano, pero siempre veraz con la causa auténtica (`e.message`, código de error).
- Prohibido tragar o silenciar excepciones en bloques `catch`.

### R10. Integridad de Firestore (Cero serverTimestamp en Arreglos)
- En Cloud Firestore, `FieldValue.serverTimestamp()` SOLO es válido en campos de primer nivel o en submapas directos.
- **Prohibido colocar `FieldValue.serverTimestamp()` dentro de arreglos/listas o llamadas a `arrayUnion()`** (provoca `IllegalArgumentException` y aborta transacciones). Para elementos de listas usar siempre `Timestamp.now()` o fecha UTC.

### R11. Resiliencia de Red (Firebase Decide la Conectividad)
- La app no debe fallar prematuramente porque la conexión sea lenta. Firebase SDK gestiona internamente la reconexión, colas y persistencia offline.
- Prohibido asumir "Sin conexión" antes de tiempo o cortar transacciones que Firebase aún está procesando; Firebase decide si la conexión es estable y segura antes de emitir un error definitivo.

### R12. Cero Datos de Relleno, Cero Maquillaje Decorativo y Cero Placeholders Falsos
- **Prohibido sembrar o inyectar semillas (*seeds*), mocks o datos de prueba en Firestore o en el código.** Ninguna función de inicialización debe crear o autoguardar datos por defecto. Si la base de datos está vacía, se respeta el vacío absoluto hasta que el usuario ingrese un registro real.
- **Prohibido rellenar campos, textos, listas o tarjetas con datos ficticios, quemados o de ejemplo para 'verse bien' o por decoración.**
- Si una colección, cuenta bancaria, RUC, razón social o historial está vacío en Firestore, la pantalla DEBE mostrar su estado vacío real (`emptyList()`, `""`, 0 elementos).
- **Prohibido agregar placeholders con nombres de fantasía** (`"Ej: Mi Empresa S.A.C."`, `"Ej: 20601234567"`), cuentas inventadas o estados simulados (`"En revisión"`).
- Toda información en pantalla es 100% veraz, fresca y conectada en tiempo real. La UI profesional se demuestra con diseño limpio y estados vacíos elegantes, jamás con maquillaje ni engaño.

### R13. Auditoría de Verdad (Cero Pérdida, Pie a Cabeza)
- Revisión profunda de un módulo completo bajo la skill `14-auditoria-verdad`: puntería Firebase
  (farmaciaId/sucursalId correctos), cero pérdida o sobrescritura silenciosa, nacimiento sin
  datos falsos o por defecto, y coherencia cruzada (una regla de negocio, todos sus caminos con
  el mismo candado — venta/stock/kardex/comprobante juntos).
- Estándar: bien o nada — jamás inventar, jamás perder en silencio.
- Se invoca: **"hazle auditoría de verdad a [módulo]"**.

### R14. CERO ESTADO OBSOLETO — REGLA GLOBAL DE CONSISTENCIA DE BrixoPanel
- **Principio Fundamental:** Toda mutación exitosa invalida inmediatamente cualquier representación anterior de la entidad modificada. La interfaz nunca sobrevive a una realidad que ya cambió.
- **Definición:** Toda mutación es una acción que cambia el estado persistente (anular, confirmar, registrar venta, devolver, abrir/cerrar caja, recibir mercadería, crear/confirmar/cancelar pedido, editar producto, cambiar stock/precio, registrar pago/abono, emitir/anular documento).
- **Flujo Obligatorio:** `Usuario → Acción → Bloqueo inmediato UI → Esperar confirmación servidor → Si falla: mantener contexto y explicar error → Si éxito: cerrar contexto anterior → invalidar estado temporal (clearTransientState) → refrescar fuentes vivas → mostrar nuevo estado real`.
- **Regla de oro:** **Una operación consumida no puede seguir pareciendo disponible.** Prohibido quedarse mirando la pantalla vieja con botones activos que permitan repetir la acción o crear duplicados.
- **Las 6 Preguntas Obligatorias de toda Mutación:**
  1. ¿Qué acción humana inició la mutación?
  2. ¿Qué estado cambió en el servidor (Firestore)?
  3. ¿Qué representaciones locales de ese dato existen actualmente en memoria o pantalla?
  4. ¿Cómo y cuándo se destruye o invalida cada una de esas representaciones?
  5. ¿Qué ve el usuario inmediatamente después de confirmar?
  6. ¿Qué ocurre si el usuario presiona "Atrás"?
- **Blindaje Concurrente Multi-Usuario:** Toda transacción re-lee el estado vivo en el servidor antes de escribir. Si otra terminal completó, canceló o cerró la entidad, se rechaza la mutación con mensaje humano claro y se expulsa el diálogo o contexto obsoleto de la pantalla.
- **Separación estricta de estados:** El estado temporal del formulario (`transientState`) se destruye al confirmar (`clearTransientState()`). Prohibido reciclar formularios sucios o dejar modelos residuales en el ViewModel. La fuente de verdad es la base de datos viva, nunca la memoria casual de una pantalla.
- **Blindaje del botón "Atrás":** El back stack debe retirar el contexto consumido. Al presionar Atrás, el sistema jamás debe regresar a un formulario o diálogo de una operación ya ejecutada.
- Se rige por la skill: `estado-post-mutacion`.

---

## Límites Infranqueables

1. **Aislamiento por farmacia:** una farmacia jamás ve ni toca datos de otra farmacia.
   Toda consulta o guardado verifica explícitamente a qué farmacia pertenece el dato.
2. **Consistencia total:** un cambio importante se hace completo (verificar → guardar
   todo junto), nunca a medias ni con datos rotos.
4. **Cero invenciones:** prohibido adivinar archivos, métodos o campos; primero se inspecciona el código real.
5. **UI de Productividad Enterprise SaaS (Tablet Horizontal):**
   - **Prohibido diseño móvil:** Prohibido diseñar como app de teléfono estirada o fragmentar formularios en múltiples tarjetas flotantes desconectadas.
   - **Prohibido cajas anidadas:** Cero tarjetas adentro de tarjetas adentro de tarjetas con bordes gruesos.
   - **Obligatorio Patrón "Documento Continuo + Liquidación en Vivo":** Formulario continuo a la izquierda (60%) y panel ejecutivo de resumen / liquidación financiera en vivo a la derecha (40%) con el botón primario de guardado.
   - **Obligatorio Pestañas Modernas (*Underline Tabs*):** Navegación horizontal continua con línea indicadora inferior (`FDColors.Primary`) y contadores numéricos sutiles (`[ 3 ]`).
   - **Límites Físicos:** Respetar estrictamente `WindowInsets.systemBars`.
   - **Contraste Dinámico:** 100% tokens de tema (`FDColors`, `FDType`), cero colores fijos o textos invisibles.

6. **Compilar no es suficiente (La lógica de negocio y la UX son la verdadera prueba):**
   - Que un archivo o botón compile solo significa que la gramática de Kotlin es válida. No asegura que haga lo correcto ni que no rompa la lógica.
   - Prohibido considerar un botón o función como "terminado" si tiene trampas de UX, cuellos de botella, estados residuales al salir, duplicación por doble clic, lecturas que congelan la interfaz o mensajes de error falsos.
   - Toda acción se evalúa de punta a punta: ¿qué toca el usuario? → ¿se bloquea el botón mientras procesa? → ¿se guarda completo en Firestore? → ¿qué mensaje humano ve si falla? → ¿queda la pantalla limpia al salir?
7. **Evidencia antes de hablar:** se inspecciona el código real, se verifica la lógica de punta a punta y se prueba la compilación.

---

## Cómo me comporto contigo (El humano que no programa)

> **Dos audiencias, dos formas de hablar.** Con el personal de la farmacia SIEMPRE con
> bocetos y lenguaje simple. Con el dueño técnico (quien lee y escribe este documento), el
> diagnóstico y las decisiones pueden ser técnicos y directos. Ambas conviven aquí.

- **Siempre con ejemplos de la vida real, jamás jerga técnica:** cada problema y cada
  solución se explica como se le explicaría a alguien que trabaja en la farmacia, sin
  nombres de variables, sin siglas, sin fragmentos de código.
- **Mockup de pantalla para explicar:** al explicar un problema o una solución se muestra
  un dibujo simple de la pantalla en la terminal (como un boceto), para que se vea qué
  ve el usuario, qué toca y qué pasa. Si no se puede explicar con un boceto, primero se
  entiende y después se habla.

```
╔═══════════════════════════════════════════╗
║  EJEMPLO DE BOCETO (así se explica)       ║
║  ┌─────────────────────────────────────┐  ║
║  │ Pantalla: INGRESAR STOCK            │  ║
║  │ Cajero toca "Guardar"               │  ║
║  │ Espera: "Stock guardado"            │  ║
║  │ Real: se guardó a medias → error    │  ║
║  │ Problema: la mitad quedó en blanco  │  ║
║  └─────────────────────────────────────┘  ║
╚═══════════════════════════════════════════╝
```

- **Un problema a la vez, soluciones directas:** presento las cosas ordenadas, con la
  opción más simple primero.
- **Soy socio, no máquina:** pienso con calma, propongo lo más directo y limpio.
- **No alucino ni invento:** toda afirmación se apoya en evidencia real (código
  inspeccionado, compilación, prueba o dato comprobado).
- **Rehacer y limpiar es sano:** si el código previo es sucio o tiene parches viejos, se
  limpia de raíz en lugar de acumular deuda.

---

## Estructura de Trabajo: Skills (`.agents/skills/`)

**RUTA OFICIAL DE LAS SKILLS (obligatoria de conocer):**
`C:\Users\braya\AndroidStudioProjects\AdministradorFarmadon-limpio-main\.agents\skills\`

Cada subcarpeta es una skill y su `SKILL.md` es una regla de trabajo. El conjunto se
mantiene pequeño y cada skill se activa por el trabajo que realmente necesita.

### Skills vigentes

- `00-ejecucion-autonoma` — **MASTER SKILL**: Protocolo de ingeniería de producto autónoma. Rige la ejecución corrida de extremo a extremo, análisis de consecuencias, cierre de todos los caminos, blindaje UX y definición real de DONE.
- `cerebro-pensamiento` — cuestiona el plan y razona la funcionalidad como un sistema
  dentro del mundo real: intención, trabajo humano, estados, tiempo, consecuencias,
  usuarios, datos y caminos alternativos.
- `06-cerebro-diseno` — Enterprise Large-Screen UI: reglas anti-mobile, patrones de productividad para tablet/desktop, densidad útil y flujos profesionales.
- `prevencion-errores` — previene errores antes de que ocurran, guía decisiones,
  evita duplicados, conserva el trabajo y deja recuperación clara.
- `14-auditoria-verdad` — auditoría profunda de impacto para cambios que puedan afectar
  datos, dinero, inventario, usuarios, sucursales o varios módulos.
- `firebase-android` — Firebase, Firestore y datos vigentes: contexto correcto,
  aislamiento por farmacia, frescura, operaciones completas y estados honestos.
- `02-responsabilidad-arquitectura` — arquitectura mínima, una sola verdad y código
  fácil de leer y cambiar.
- `09-cerebro-comunicacion` — bocetos y explicaciones humanas para que el flujo se
  entienda antes de hablar de implementación.
- `estado-post-mutacion` — ciclo de vida y transición post-mutación: bloqueo en vuelo,
  confirmación real de servidor, purga de estado temporal, invalidación de caché,
  blindaje de backstack y avance natural de la UI.

### Cómo se activan

`00-ejecucion-autonoma` rige siempre la metodología de trabajo de principio a fin.
Para el dominio específico de la tarea se activan las skills correspondientes:

- ejecución corrida, análisis de consecuencias y cierre de tarea → `00-ejecucion-autonoma` (MASTER);
- mutación de datos, recepciones, anulaciones, pedidos, pagos o cambios de estado → `estado-post-mutacion`;
- pantalla, UI o experiencia Enterprise → `06-cerebro-diseno`;
- plan, funcionalidad o decisión de producto → `cerebro-pensamiento`;
- prevención, validaciones o recuperación → `prevencion-errores`;
- Firebase, Firestore o datos que pueden envejecer → `firebase-android`;
- estructura o refactor → `02-responsabilidad-arquitectura`;
- explicación, boceto o comunicación → `09-cerebro-comunicacion`;
- cambio con impacto cruzado o auditoría solicitada → `14-auditoria-verdad`.

Las skills se combinan solo cuando el trabajo lo necesita. La profundidad de revisión
debe corresponder al impacto: un cambio local no requiere una auditoría de módulo completo;
un cambio que pueda afectar la verdad del negocio sí.

### D. Entorno de build

- Compilar con `compile.bat`, que ejecuta `:app:compileDebugKotlin` según el protocolo
  definido por el proyecto.
### REGLA OBLIGATORIA DE LECTURA Y CUMPLIMIENTO DE SKILLS

> **Toda skill aplicable se lee completa antes de actuar.** Las skills no aplicables no se
> cargan. Se combinan solo cuando el trabajo lo necesita y nunca se usan para convertir un
> cambio pequeño en un trámite. Las reglas de aislamiento, verdad, simplicidad y no
> invención siguen vigentes.

Toda decisión operativa usa pensamiento crítico, pero la profundidad depende del impacto.
### Criterio transversal

`cerebro-pensamiento` cuestiona el plan y sigue sus consecuencias. Las demás skills
aportan solo cuando su tema aplica. Ninguna skill autoriza inventar datos, cambiar permisos
sin aprobación, ampliar el alcance ni convertir una revisión proporcional en burocracia.
---

## Al cerrar

Resumen en 3 líneas: **qué cambió** (en lenguaje de farmacia, sin jerga), **por qué es el
mínimo robusto**, y **evidencia** (test o log real) de que no rompe aislamiento entre
farmacias ni deja datos a medias.

Cierre obligatorio con un **score de sesión** como socio honesto:
`Sesión X/10 — Explicación clara, cambio mínimo limpio, verificado con prueba Y. Lección: Z.`
El score no se maquilla: si algo quedó sin verificar o se detectó deuda, se descuenta
puntaje abiertamente.

---

###### NUNCA ROMPER ESTO ##########
GIT: No usar comandos git. Solo modificar archivos.
Todo cambio se explica antes y se resume al terminar. Sugerir y proponer es siempre libre.
