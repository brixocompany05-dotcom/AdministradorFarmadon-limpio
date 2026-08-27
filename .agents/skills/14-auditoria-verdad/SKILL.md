---
name: auditoria-verdad
description: >-
  Auditoría de VERDAD (cero pérdida) — revisión profunda de un módulo completo,
  pie a cabeza: que ninguna acción apunte mal a Firebase (farmaciaId/sucursalId
  correctos), que ningún dato se pierda o sobrescriba en silencio, que nada nazca
  falso o por defecto, y que cada acción sea coherente con el resto del negocio
  (efecto dominó sobre stock/kardex/comprobantes/suscripción).
  Se invoca con: "hazle auditoría de verdad a [módulo]" o "revisión cero-pérdida".
---

# AUDITORÍA DE VERDAD (CERO PÉRDIDA · v1.0)

> **PROPÓSITO:** responder con evidencia línea-en-mano, nunca con suposición:
> ¿cada acción escribe DONDE DEBE, CONSERVA lo que debe conservar, BORRA solo lo
> nombrado, NACE sin falsedades, y es COHERENTE con el resto del ecosistema?
> Frase de cierre esperada: *bien o nada — jamás inventar, jamás perder en silencio.*
> Aquí el dinero es doble: los SOLES de la venta Y el STOCK/KARDEX que la venta mueve.

---

## LAS 6 FASES OBLIGATORIAS (en orden, sin saltos)

### FASE 0 · MAPA (antes de juzgar)
Inventario completo del módulo: archivos + líneas, TODAS las rutas Firestore
(vía `FarmadonPaths` y paths propios), estados UI, escuchas (nacen/mueren),
multi-sucursal (¿dónde entra `sucursalIdEfectiva`?), tests existentes.
Prohibido opinar sobre código no leído.

### FASE 1 · FALSIDAD
Barrido de mentiras clásicas:
- Defaults inventados (`?: "Producto general"`, `"SIN CATEGORÍA"`, precios 0.0 por defecto).
- Reloj local (`System.currentTimeMillis`, `Calendar.getInstance`, `nowIso`)
  tocando DINERO real: comprobantes/IGV, kardex, lotes (vencimientos), cuenta corriente.
- Etiquetas guardadas usadas como verdad cuando existe calculador en vivo.
- Errores tragados que convierten "falló" en "vacío", "cero" o "sin movimientos".

### FASE 2 · PUNTERÍA
Para cada acción de negocio: ¿escribe en las colecciones EXACTAS correctas y de
la SUCURSAL correcta? ¿Los espejos atómicos se cumplen (ej.: producto + kardex +
lote en la misma transacción)? ¿Locks de idempotencia presentes y con TTL?
¿El aislamiento por farmacia (R7/Límite 1) queda implícito en cada ruta?

### FASE 3 · PÉRDIDA
Campo por campo por acción:
- ¿Qué SOBREESCRIBE? ¿Usa set() completo donde debía update() nombrado?
- ¿El KARDEX y los historiales son APPEND desde snapshot fresco de la tx (no caché)?
- ¿Los borrados son campo-nombre-por-campo-nombre, jamás documentos enteros sin rastro
  (el patrón del proyecto: papelera/`listaeliminado` + auditoría)?
- ¿Alguna decisión lee de StateFlow/caché de pantalla donde debía leer del SERVIDOR?
- Toda acción deja auditoría permanente cuando su documento puede morir u overwritearse.

### FASE 4 · NACIMIENTO
El pipeline de creación del recurso central del módulo (un producto con su lote
inicial, una venta con su comprobante, el registro de un postulante hacia BRIXO):
ningún dato falso ni por defecto; fuentes verificadas en jerarquía explícita;
lo obligatorio obliga (aborta con mensaje humano nombrando qué falta);
nada se pierde en silencio; imposible sobrescribir algo ya existente.

### FASE 5 · COHERENCIA CRUZADA (efecto dominó)
Matriz acción × entidades ajenas: ¿debería tocarlas? Ejemplos reales de dominó
aquí: vender mueve stock + kardex + comprobante JUNTOS; recibir mercadería ajusta
lote y kardex sin tocar precio pactado; cambiar régimen de precios no reescribe
comprobantes históricos. Regla de oro: **una regla de negocio, todos sus caminos,
mismo candado** (si una vía bloquea stock negativo, TODAS la bloquean).
Incluye despertar limpio (sin campos residuales) y decisiones deliberadas
documentadas con su lógica.

---

## LOS 4 CAMINOS + DATOS HOSTILES (gate obligatorio POR CADA ACCIÓN nueva)

Toda acción importante se valida al menos por:

| Camino | Pregunta |
|---|---|
| **Feliz** | ¿Funciona de punta a punta? |
| **Error** | ¿Algo falla — qué ve el usuario y qué queda intacto? |
| **Repetición** | ¿Vuelve a ejecutar (doble toque, reintento tras corte)? ¿Duplica o llega a "ya procesado"? |
| **Interrupción** | ¿Muere la app/red a mitad? ¿Qué estado queda y cómo se recupera? |

**DATOS MANIPULADOS — jamás asumir que la UI envía verdad:** valores modificados,
IDs incorrectos o inexistentes, documentos eliminados, referencias rotas, datos
antiguos, campos faltantes, tipos inesperados. El sistema debe fallar de forma
controlada con mensaje humano.

**MISIÓN ANTI-FALSO-POSITIVO:** una función puede compilar, guardar correcto y
mostrar la UI correcta… y estar MAL DISEÑADA (ej.: notificación que reaparece al
volver a la pantalla; venta que descuenta stock dos veces). El trabajo es descubrir
cómo comportarse mal aunque aparentemente funcione. Si alguno de los 4 caminos
queda sin comportamiento definido, la funcionalidad está INCOMPLETA.

---

## PUERTA DE SALIDA (sin esto NO está terminada)

- [ ] Compila según `gradle-windows-execution`: `.\gradlew.bat :app:compileDebugKotlin` (protocolo anti-bloqueo, salida a `build.log`)
- [ ] Suite verde de los módulos tocados (tests nuevos SOLO si nació lógica pura crítica)
- [ ] Barrido anti-residuos final (grep de defaults/relojes eliminados = cero matches)
- [ ] **LAS 8 PREGUNTAS DE LA VIDA REAL respondidas por cada acción nueva**
      (viven en `cerebro-pensamiento` §6: quién no puede · ya tiene/tuvo · resultado
      exacto antes de confirmar · salida visible · carrera/reintento · rastro
      permanente · números dictados por el sistema · inmunidad al futuro del catálogo)
- [ ] Veredicto de socio: hallazgos por severidad + qué quedó protegido + honestidad del después
      (incluye lo que Farmadon NO controla —contrato con BrixoPanel/BRIXO— y deudas conscientes del dueño)

## FRONTERAS

- Los FLUJOS de usuario y bocetos siguen siendo de `09-cerebro-comunicacion`.
- La robustez genérica sigue siendo de `04-cerebro-robustez`; la frescura de `cerebro-frescura`;
  Firebase mecánico de `firebase-android`.
- Esta skill manda en el ORDEN DE AUDITORÍA y en el estándar cero-pérdida/cero-falsedad.
