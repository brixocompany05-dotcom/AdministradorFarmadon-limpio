---
name: estado-post-mutacion
description: Protocolo de ingeniería para transiciones de UI post-mutación. Rige el ciclo de vida completo de cualquier acción que cambie datos (ventas, recepciones, anulaciones, pedidos, pagos, stock, cajas): bloqueo inmediato, confirmación real del servidor, purga de estado temporal, invalidación, avance de UI y blindaje del backstack.
---

# ESTADO POST-MUTACIÓN · PROTOCOLO DE INGENIERÍA FARMADON

> 🔒 **REGLA GLOBAL DE CONSISTENCIA DE BrixoPanel:**  
> **"Toda mutación exitosa invalida inmediatamente cualquier representación anterior de la entidad modificada."**  
> **"La interfaz nunca debe sobrevivir a una realidad que ya cambió."**  
> **"Después de una operación exitosa, la UI debe avanzar; nunca fingir que nada ocurrió."**

---

## 1. Definición de Mutación

Una **mutación** es cualquier acción disparada por el usuario o por el sistema que modifica el estado persistente del negocio en la base de datos (Firestore / Backend):

| Módulo | Acciones de Mutación Típicas |
| :--- | :--- |
| **Ventas / POS** | Registrar venta, cobrar ticket, anular venta, emitir nota de crédito / devolución. |
| **Compras / Reposición** | Crear orden a proveedor, enviar orden, cancelar orden, cerrar orden con ajuste, recibir mercadería física. |
| **Caja / Turno** | Apertura de caja, arqueo / cierre de caja, registro de ingreso o egreso de caja chica. |
| **Inventario / Stock** | Ajuste manual de stock, modificación de lote/vencimiento, edición de producto (precios, costos), dar de baja / merma, crear proveedor, vincular/desvincular producto. |
| **Cuentas por Pagar/Cobrar** | Registrar abono de factura, registrar pago mixto, aplicar saldo a favor, prorrogar vencimiento, registrar nota de crédito comercial, anular factura. |

---

## 2. Las 6 Preguntas Obligatorias de Toda Mutación

Antes de escribir una sola línea de código o dar por buena una pantalla con mutaciones, el ingeniero debe responder afirmativamente:

1. **¿Qué acción humana inició la mutación?** (Ej: el usuario presionó "Confirmar Recepción").
2. **¿Qué estado cambió en el servidor (Firestore)?** (Ej: el pedido pasó de `ENVIADO` a `RECIBIDO`, subió stock de 2 productos y se creó la factura con deuda).
3. **¿Qué representaciones locales de ese dato existen actualmente en memoria o pantalla?** (Ej: el diálogo modal de recepción, la tarjeta en "En camino", el panel de detalle abierto, los inputs de lotes).
4. **¿Cómo y cuándo se destruye o invalida cada una de esas representaciones?** (Ej: `mostrarDialogoRecepcion = false`, `pedidoParaRecepcionar = null`, `clearTransientState()`, la lista filtra excluyendo terminales).
5. **¿Qué ve el usuario inmediatamente después de confirmar?** (Ej: diálogo cerrado, banner de éxito veraz, la orden desaparece de "En camino" y aparece en "Historial").
6. **¿Qué ocurre si el usuario presiona "Atrás"?** (Ej: el back stack ya no tiene el diálogo consumido; el usuario permanece en la lista principal sin reabrir la operación ejecutada).

---

## 3. El Flujo Obligatorio Post-Mutación

Toda mutación debe seguir de manera estricta y secuencial este camino:

```
[ Usuario toca acción ]
         ↓
1. BLOQUEAR INMEDIATAMENTE
   • procesando = true en UI
   • Botón deshabilitado con spinner o indicador sutil
   • Previene doble clic, duplicados y llamadas concurrentes
         ↓
2. ESPERAR CONFIRMACIÓN REAL DEL SERVIDOR
   • Esperar el Result de Firestore (commit de transacción / batch)
   • Prohibido asumir éxito antes de tiempo
         ↓
┌───────────────────────────────┴───────────────────────────────┐
│                                                               │
▼ CASO FALLA (Servidor ❌)                                      ▼ CASO ÉXITO (Servidor ✅)
• Mantener al usuario exactamente donde está                    • Marcar contexto anterior como CONSUMIDO/OBSOLETO
• NO cerrar pantalla ni modal                                   • Cerrar vista/modal/sheet/diálogo de la operación
• Conservar lo que el usuario escribió                          • Limpiar estado temporal (clearTransientState)
• Mostrar mensaje veraz con la causa auténtica                  • Invalidar cualquier dato cacheado afectado
• Permitir corregir y reintentar                                • Refrescar las fuentes vivas (Source of Truth)
                                                                • Conducir la UI al contexto lógico siguiente
                                                                • Mostrar el nuevo estado real
                                                                • Retirar contexto del back stack (Atrás no reabre)
```

---

## 4. Regla de Oro

> **Una operación consumida no puede seguir pareciendo disponible.**

- **Prohibido:** Quedarse mirando la misma pantalla vieja con los botones todavía activos tras una operación exitosa.
- **Prohibido:** Que una venta anulada siga mostrando el botón "Anular" o "Editar".
- **Prohibido:** Que un pedido recibido siga mostrando "Pendiente de recepción" o el botón "Recibir mercadería".
- **Prohibido:** Que un formulario de pedido siga mostrando "Crear pedido" después de que la orden ya fue emitida.
- **Prohibido:** Que un diálogo de cobro permanezca abierto tras registrar el pago.

---

## 5. Separación Estricta: Estado Persistente vs. Estado Temporal

Para cumplir este principio, el código debe separar nítidamente dos clases de estado:

```
┌─────────────────────────────────────────────────────────────┐
│ 1. ESTADO PERSISTENTE DEL NEGOCIO (Source of Truth)         │
│ • Colecciones vivas en Firestore (addSnapshotListener).      │
│ • La base de datos es la dueña de la verdad, no la pantalla.│
│ • Se actualiza solo ante confirmaciones del servidor.       │
└─────────────────────────────────────────────────────────────┘
                              ≠
┌─────────────────────────────────────────────────────────────┐
│ 2. ESTADO TEMPORAL DE LA PANTALLA (Transient State)         │
│ • Inputs de texto, borradores, selecciones del formulario.  │
│ • Checkboxes, montos tipeados, lotes ingresados en memoria.  │
│ • Vive ÚNICAMENTE mientras la operación se prepara.         │
│ • DEBE DESTRUIRSE al completar la mutación con éxito.       │
└─────────────────────────────────────────────────────────────┘
```

### Protocolo de Limpieza (`clearTransientState`)

Al confirmar éxito:
1. Las variables temporales en el ViewModel deben resetearse a `null` o vacío (`pedidoParaRecepcionar = null`, `facturaParaAbonarId = null`, etc.).
2. El Composable del diálogo o formulario debe ser desmontado de la composición.
3. El ViewModel nunca conserva borradores de operaciones que ya fueron guardadas. Al abrir una nueva operación, el formulario nace limpio y fresco.

---

## 6. Blindaje Concurrente Multi-Usuario y Protocolo de Colisión SaaS

En un entorno SaaS Enterprise, múltiples usuarios pueden operar concurrentemente sobre los mismos datos desde diferentes terminales o tablets.

### Protocolo de Resolución de Colisiones:
1. **Verificación Transaccional en Servidor:**
   Toda mutación sensible (recibir pedido, cerrar con ajuste, anular factura, registrar abono) se ejecuta dentro de un `runTransaction` de Firestore. Antes de escribir, relee el estado vivo del documento y valida las transiciones legales mediante la **Máquina de Estados**.
   Si la entidad ya fue cerrada, recibida o cancelada por otro usuario, la transacción se aborta con un `IllegalStateException` específico y humano.
2. **Reconciliación Viva de Pantallas Abiertas:**
   Las escuchas en tiempo real (`observarPedidosRecientes`, `observarFacturas`, etc.) notifican inmediatamente a todas las terminales. Si una terminal tiene abierto un modal o formulario de una entidad y el snapshot revela que otra terminal ya la mutó a un estado incompatible o la eliminó, el ViewModel debe:
   - Desmontar y cerrar el modal inmediatamente.
   - Purgar el estado temporal (`clearTransientState()`).
   - Mostrar un mensaje informativo profesional alertando que la entidad fue actualizada en otra terminal.
3. **Manejo de Fallas Concurrentes en UI:**
   Si la transacción falla por colisión concurrente, el bloque `onFailure` del ViewModel detecta el conflicto, cierra el diálogo o formulario obsoleto y expone la verdad al usuario. Prohibido dejar una pantalla fantasma esperando que el usuario vuelva a presionar confirmar sobre una entidad que ya no existe.

---

## 7. Derivación Dinámica de Acciones (Invalidación de Botones)

La presencia o habilitación de acciones en la UI (botones como "Recibir Mercadería", "Cancelar Orden", "Cerrar con Ajuste", "Anular Factura") **jamás** se basa en variables booleanas locales desconectadas o en memoria casual.
- Se derivan directamente de la **Máquina de Estados Formal** (`MaquinaEstadosPedido.puedeRecibir(estado)`, `puedeCancelar(...)`, `puedeCerrarConAjuste(...)`).
- Si la orden está en estado terminal (`RECIBIDO`, `COMPLETADA_AJUSTE`, `CANCELADO`), todos los botones de mutación desaparecen y son reemplazados por insignias de estado informativas y legibles.

---

## 8. Blindaje de Navegación y Back Stack

El botón **Atrás** físico o por gestos de Android es una fuente frecuente de estados residuales.

### Reglas de Back Stack:
1. **Retiro inmediato del historial:** Cuando un diálogo, bottom sheet o pantalla de formulario completa su trabajo con éxito, ese contexto debe eliminarse del historial de navegación.
2. **Sin regreso a formularios consumidos:** Al presionar "Atrás", el usuario debe volver a la lista o pantalla principal del módulo, jamás a una pantalla de recepción, cobro o anulación que ya fue ejecutada.
3. **Auto-cierre por desaparición de condición (`LaunchedEffect`):** Si una orden o entidad abierta cambia de estado en el servidor (o desde otro terminal) y ya no cumple con el filtro de la vista activa, la pantalla o detalle debe replegarse automáticamente:
   ```kotlin
   // Ejemplo: si el pedido abierto ya fue recibido o no está en camino, el detalle se repliega solo
   LaunchedEffect(pedidosGuardados, abierto?.recepciones?.size) {
       if (pedidoAbiertoId != null) {
           val ordenEnServidor = pedidosGuardados.find { it.id == pedidoAbiertoId }
           val yaNoEstaEnCamino = ordenEnServidor == null || !MaquinaEstadosPedido.perteneceAEnCamino(ordenEnServidor.estado)
           if (yaNoEstaEnCamino) {
               pedidoAbiertoId = null
           }
       }
   }
   ```

---

## 9. Lista de Verificación (Checklist de 10 Puntos)

Antes de dar por concluida cualquier implementación que toque una mutación de datos, verificar:

- [ ] **1. Candado anti-doble clic:** ¿El botón se deshabilita inmediatamente al tocarlo (`enabled = !procesando`)?
- [ ] **2. Confirmación real:** ¿El éxito solo se anuncia cuando la transacción de Firestore retornó `Result.success`?
- [ ] **3. Preservación en error:** Si el servidor falla (ej. sin internet, error de validación), ¿el usuario se queda en su formulario con sus datos intactos para reintentar?
- [ ] **4. Mensaje veraz de error:** ¿Se muestra `e.message` auténtico en lugar de un comodín vago?
- [ ] **5. Cierre automático en éxito:** ¿Se cierra inmediatamente el diálogo/modal/formulario al tener éxito?
- [ ] **6. Purga de estado temporal:** ¿Se limpiaron todas las variables temporales en el ViewModel (`clearTransientState`)?
- [ ] **7. Avance al contexto lógico siguiente:** ¿La UI llevó al usuario a donde debe estar (ej. volver a la lista actualizada)?
- [ ] **8. Nuevo estado visible:** ¿El elemento modificado refleja inmediatamente su nuevo estado (ej. RECIBIDO, ANULADA, stock nuevo)?
- [ ] **9. Botones incompatibles destruidos:** ¿Desaparecieron por completo los botones que no tienen sentido sobre el nuevo estado (ej. "Recibir", "Anular")?
- [ ] **10. Blindaje del botón Atrás:** ¿Al presionar atrás el sistema permanece en el contexto actual sin reabrir la operación consumida?
