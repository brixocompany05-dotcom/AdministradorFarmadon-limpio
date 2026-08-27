---
name: cerebro-frescura
description: Contrato de frescura inteligente por dato y por pantalla. Ni "todo en tiempo real" ni "una foto al entrar": cada dato define su vejez máxima tolerable, su mecanismo, su dueño y su condición de fin. Incluye estados de carga honestos, política de datos viejos (A/B/C/D), cero carga infinita, botones siempre recuperables, actualizar sin parpadeo, economía de lecturas y medición antes de optimizar. Complementa R8 — no la anula.
---

# CEREBRO-FRESCURA (cada dato tiene un contrato)

> **EL OBJETIVO (Principio de Equilibrio SaaS):**
> No es *"todo siempre en tiempo real"* — demasiados listeners = lecturas de más,
> batería, costo y pantallas complejas.
> Tampoco es *"cargo una vez y confío para siempre"* — pantallas que mienten.
> Es: **la información correcta, con la frescura correcta, en el momento correcto,
> usando el mínimo trabajo necesario.**

Para cada dato se decide EXPLÍCITAMENTE: ¿qué tan rápido puede quedar viejo antes de
convertirse en un problema? Nunca se asume "todo en vivo" ni "una lectura basta".

---

## 1. Las 6 preguntas del contrato (obligatorias ANTES de cualquier escucha o recarga)

1. **Fuente de verdad:** ¿de dónde proviene realmente?
2. **Importancia:** ¿qué ocurre si está desactualizado?
3. **Vejez máxima tolerable:** ¿segundos, minutos, o "al entrar basta"?
4. **Cuándo refrescar:** al cambiar / al entrar / al volver a primer plano / después de una acción relacionada / bajo demanda / manualmente.
5. **Cómo detectar el cambio:** listener en vivo / invalidación / recarga / sincronización / dato derivado.
6. **Cuándo DEJA de observarse:** al salir de pantalla / al cambiar sede, tenant o recurso. Toda observación tiene **dueño, contexto, recurso, inicio y fin**.

---

## 2. Los datos críticos no envejecen en silencio

Pregunta filtro: *"si esto cambia mientras el usuario mira la pantalla, ¿seguir mostrando
el valor antiguo puede provocar una decisión incorrecta?"* Si sí → alta frescura.

Candidatos típicos en Farmadon: acceso/suspensión, estado de cuenta, permisos,
stock operativo, límites del plan, estado de una orden, alertas compartidas,
cambios hechos por otros usuarios. **No todos usan el mismo mecanismo**: el riesgo
concreto decide, no la etiqueta de "importante".

---

## 3. Fresco ≠ listener permanente

El listener vivo es el mecanismo **más caro** (lectura por cada evento, trabajo y
recomposición aunque nadie mire, duplicados posibles). Preguntar:
*"¿realmente necesito saber el cambio en el instante exacto en que ocurre?"*
Si no, usar el mecanismo más económico: recarga al entrar, al volver a primer plano,
tras la acción propia, bajo demanda.

---

## 4. Toda observación tiene dueño, inicio y fin — cero duplicados accidentales

Antes de abrir una escucha preguntar: *¿quién está viendo esto ahora?* y
*"¿cuántas escuchas activas pueden existir para este mismo dato?"*

Prohibido duplicar por: recomposición · navegación · retorno a pantalla · escucha
vieja + nueva · varios collectors del mismo evento. Verificar limpieza:
`awaitClose` que remueve TODO lo registrado; `onCleared` del ViewModel igual.

**Solape acotado ≠ pecado (pero se declara):** dos observaciones del mismo documento
son aceptables SOLO si ambas tienen dueño visible y vida limitada (ej.: Sidebar
permanente + pantalla abierta un rato) y unificarlas obligaría a acoplar módulos.
El solape se deja POR ESCRITO en la tabla de contratos (§15). Lo prohibido es el
solape accidental, eterno o sin dueño.

---

## 5. Cambiar de contexto borra lo anterior

Nueva sede, tenant, usuario, producto, pantalla o filtro principal → los datos del
contexto anterior jamás se pintan como si fueran del nuevo. Preguntar siempre:
*"¿qué pasa con la respuesta o listener anterior cuando cambia el contexto?"*
Una respuesta tardía del contexto A jamás debe pintarse en la UI del contexto B.
En Farmadon esto ya vive así: cambiar de sede dispara recarga limpia del inventario.

---

## 6. Invalidación y actualización en cadena

Después de modificar un dato preguntar: *"¿qué información relacionada acaba de quedar
potencialmente vieja?"* — detalle, lista, contador, badge, resumen, límite disponible,
dato derivado, caché. Sin actualizar "toda la app" a ciegas; sin dejar dependientes
viejos. Revisar la cadena completa **A → B → C → D** (dato original → contador →
resumen → alerta → permiso de acción). Una funcionalidad no está completa hasta
verificar qué otros estados dependen de ella.

---

## 7. Fresco AL MOMENTO DE DECIDIR

La pantalla puede mostrar un valor ligeramente viejo si no causa daño. Pero toda
decisión crítica (vender, descontar, validar límite, permitir acción, confirmar
estado) se ejecuta leyendo la verdad atómica dentro de la transacción (`tx.get`),
nunca confiando en la foto de la pantalla. En Farmadon esto YA aplica en ventas,
stock y anulaciones: **la foto vieja informa; la transacción decide.**

---

## 8. Estados de carga honestos (cuatro estados que jamás se confunden)

Toda pantalla define qué muestra mientras obtiene sus primeros datos:

| Estado | Significado | En pantalla |
|---|---|---|
| **Todavía no cargó** | La primera lectura no llegó | Cargando real (nunca vacío inventado) |
| **Cargó y está vacío** | El servidor confirmó que no hay datos | Estado vacío real (R12) |
| **Falló** | Error real | Mensaje verdadero + siguiente paso (R9) |
| **Con datos anteriores mientras actualiza** | Refresco sobre datos previos | Decidir política §9 |

Prohibido confundir "no ha cargado" con "está vacío": es mentirle al usuario dos veces.

---

## 9. Datos viejos mientras se actualizan: política explícita A/B/C/D

Si se mantienen datos antiguos durante un refresco, la política se DECLARA, no se
improvisa:

- **A. Seguro** — se muestran mientras se refrescan. Ej.: la lista acumulada del inventario crece lote a lote sin borrar lo visible.
- **B. Seguro para VER, no para DECIDIR** — se ven, pero las acciones críticas esperan verdad actual. Ej.: badge de alertas viejo se ve; al vender, la transacción lee stock real (`tx.get`).
- **C. No seguro** — hay que refrescar antes de continuar. Ej.: crear una sede exige límite vigente del plan.
- **D. Ya no válido** — se elimina de la vista de inmediato. Ej.: cambio de sede → los datos de la sede anterior salen del contexto.

No existe UNA política correcta para todo; existe la política correcta POR DATO.

---

## 10. Toda operación termina y restaura

Un único contrato con dos caras — se verifican SIEMPRE juntas:

**La carga sabe terminar.** NINGUNA operación puede quedarse en "cargando" para
siempre. Para cada carga definir: qué la inicia · qué la termina · éxito · error ·
cancelación · timeout · pérdida de contexto · abandono de pantalla.
Pregunta obligatoria: *"¿existe algún camino donde isLoading pueda quedar en true
para siempre?"* Todo `isLoading = true` tiene su `finally { isLoading = false }`
(cerebro-robustez §3).

**El botón sabe volver.** Toda acción que se deshabilita durante una operación
analiza TODOS los caminos de restauración: éxito · error · excepción · timeout ·
cancelación · salida de pantalla · respuesta inesperada. Prohibido el usuario que
pulsa, la operación falla en silencio y el botón queda muerto: eso convierte un
error pequeño en un usuario atrapado.

---

## 11. Actualizar sin romper la pantalla

Una actualización correcta puede sentirse mala si provoca: lista que desaparece,
spinner completo, scroll que salta, selección perdida. Reglas:

- **Feedback proporcional (latencia percibida):** nada de spinners agresivos para operaciones casi instantáneas (parpadeo). El usuario siempre sabe que su acción fue recibida, si sigue esperando, si falló y qué puede hacer.
- **Actualización selectiva:** si cambió una parte, solo esa parte se reprocesa. Un cambio pequeño jamás dispara recarga total + pérdida de scroll + reconstrucción de listas.
- **Carga progresiva:** mostrar primero lo que el usuario necesita ahora; lo secundario y diferible llega después. No retrasar toda la pantalla por información que aún no se necesita. Ej.: inventario muestra la primera página y crece en lotes de 50.

---

## 12. Economía y verdad del rendimiento

- **No sobreleer:** antes de cada lectura preguntar *"¿ya tengo este dato y sigue siendo válido según su contrato?"*. Prohibido reconsultar al recomponer o por un cambio visual irrelevante; prohibido cargar datos que aún no se necesitan. Pero tampoco reutilizar un dato viejo sin verificar que su vejez siga dentro del contrato.
- **Costo invisible:** preguntar *"¿qué pasará con 10x usuarios, productos, alertas u órdenes?"* — documentos, tamaño de respuesta, frecuencia de eventos, número de listeners, recomposiciones, listas grandes, paginación. Lo que hoy vuela puede arrastrarse mañana.
- **Medir ANTES de optimizar:** no asumir lentitud. Identificar qué operación tarda, cuándo, cuánto, qué la dispara, cuántas veces ocurre. Prohibido agregar caché, debounce o trabajo en segundo plano sin un problema medido.
- **Rendimiento ≠ quitar seguridad:** jamás resolver lentitud quitando validaciones, `tx.get`, listeners necesarios o actualizaciones críticas. Se optimiza la causa (lo que sobra, lo que se lee de más, lo que se recompone de más), nunca la protección.

---

## 13. La matriz antes de implementar (datos críticos)

No se implementa la política de un dato crítico sin poder responder:

| Pregunta | Decisión |
|---|---|
| ¿Qué dato es? | Recurso definido |
| ¿Quién es dueño? | Fuente de verdad única |
| ¿Qué tan crítico es? | Impacto de estar viejo |
| ¿Puede cambiar mientras se ve? | Sí / No |
| ¿Cuánto puede envejecer? | Política de frescura |
| ¿Cómo se actualiza? | Listener / recarga / demanda |
| ¿Cuándo inicia la observación? | Condición explícita |
| ¿Cuándo termina? | Condición explícita |
| ¿Puede duplicarse la escucha? | Debe prevenirse |
| ¿Puede llegar tarde? | Debe validarse (cambio de contexto) |
| ¿Qué invalida el dato? | Evento definido |
| ¿Qué acciones dependen de él? | Consecuencias en cadena |
| ¿Qué pasa si falla? | Estado real y recuperación |
| ¿Qué pasa si tarda? | Feedback proporcional |
| ¿Qué pasa si crece 10x? | Escalabilidad |

---

## 14. Relación con las demás reglas

- **R8** manda el QUÉ (ninguna vista congelada indefinidamente); este cerebro define el CÓMO según el contrato de cada dato.
- **R3/R9** exigen estados honestos y errores verdaderos: §8 y §10 son su aplicación a cargas y botones.
- **R12** prohíbe vacíos inventados: "no ha cargado" nunca se pinta como "vacío".
- **cerebro-robustez §4E** es el caso particular de escuchas vivas; aquí se generaliza a todo mecanismo de frescura.
- **cerebro-pensamiento criterios 4 y 5**: la película completa y el "después" del Éxito son quienes revelan qué datos quedaron viejos tras una acción.

---

## 15. Tabla de contratos vigente en Farmadon (decisión del dueño — 2026-08-25)

| Dato | Vejez máx. | Mecanismo | Dueño | Fin |
|---|---|---|---|---|
| Acceso / suspensión / revocación | Instantes | Lectura única en la puerta + vigilante vivo | PantallaPrincipal | Cerrar sesión |
| Stock operativo, inventario, ventas | Instantes | Listener vivo; lista acumulada (política A) | Repositorio del módulo | Salir de pantalla |
| Estado de cuenta, saldo, plan (Mi Plan) | Instantes | Escuchas vivas combinadas (suscripción + farmacia + sedes + catálogo re-anclado) | PlanFacturacionRepository mientras la pantalla/diálogo lo colecciona | Salir de pantalla / cerrar diálogo |
| Límite de sedes (banner capacidad) | Instantes | Escuchas vivas farmacia + catálogo re-anclado (política C al crear sede) | SucursalesRepository (pantalla Sedes visible) | Salir de pantalla |
| Catálogo de planes BRIXO | Minutos ok | Escucha solo visible | Pantalla que lo muestra | Salir de pantalla |
| Badge alertas leídas (compartido por sede) | Minutos ok | Recarga: al entrar + al volver a primer plano + tras marcar + regreso por navegación. SIN listener | InventarioViewModel + observer ON_RESUME de InventarioScreen | N/A (no hay escucha) |
| Historial de pagos | Al entrar basta | Listener ligado a pantalla | Pantalla Mi Plan | Salir |
| Solicitud de pago reportada | Instantes (diálogo abierto) | Listener propio del diálogo | ReportarPagoDialog | Cerrar diálogo |
| Búsqueda de inventario | Momento de decidir | Consulta fresca por tipeo (foto puntual legítima) | Acción del usuario | Inmediato |
| Duplicados/códigos, facturas, unicidad | Momento de decidir | Consulta puntual pre-escritura + verdad atómica `tx.get` al guardar (política B universal) | Acción de guardar | Inmediato |
| Perfil usuario, moneda, sedes del sidebar | Instantes | Listeners permanentes del shell | SidebarViewModel (`onCleared` limpia) | Logout |

**Solapes aceptados y declarados (§4):** mientras Mi Plan, Sedes o el diálogo de pago
están abiertos, sus escuchas solapan temporalmente con las permanentes del Sidebar
(farmacia, suscripción, catálogo). Ambos dueños están vivos y acotados; unificarlos
obligaría a convertir al Sidebar en hub de datos de módulos ajenos (acoplamiento peor
que el costo). Si algún día se deduplica, el dueño único debe ser un objeto de la capa
de datos compartido por todos — no el ViewModel de navegación.
