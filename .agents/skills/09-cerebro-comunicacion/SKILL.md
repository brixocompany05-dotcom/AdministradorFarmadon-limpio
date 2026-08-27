---
name: cerebro-comunicacion
description: Protocolo obligatorio de comunicación humana. PRIORIDAD MÁXIMA: todo se explica con bocetos de pantalla y ejemplos de la vida real, jamás con código.
---

# CEREBRO 09: COMUNICACIÓN HUMANA (EL LENGUAJE DEL SOCIO)

> **PRIORIDAD MÁXIMA.** Esta skill está por encima de todas las demás.
> Si la explicación no la entiende alguien que trabaja en la farmacia sin saber
> programar, está mal hecha y se rehace antes de continuar.

## 1. El marco del negocio (para no confundirse nunca)

- **BRIXO** es la empresa: la central de control.
- **BrixoPanel** es la sala de máquinas de BRIXO (agentes que aprueban altas,
  monitorean suscripciones y cobros).
- **Farmadon** es el producto que BRIXO les vende a las farmacias. Cada farmacia es un
  CLIENTE de BRIXO; el personal de la farmacia usa Farmadon en su día a día (ventas,
  inventario, stock).
- Los problemas operativos de una farmacia viven en Farmadon; la central solo debe
  hacer que todo funcione.

## 2. El principio único

Toda comunicación — problema, plan, bug, revisión, riesgo — se cuenta primero como un
**flujo de usuario real**, corto y visual:

```
¿Quién? → ¿Qué toca? → ¿Qué espera que pase? → ¿Qué pasa en realidad? → ¿Cuál es el problema?
```

## 3. El formato obligatorio: BOCETO DE PANTALLA (mockup en la terminal)

Todo problema o solución se explica con un dibujo simple de la pantalla. Es la forma
de hablar con alguien que no programa: se ve qué ve el usuario, qué toca y qué pasa.

```
╔═══════════════════════════════════════════════╗
║  EJEMPLO — Cómo explicar un problema          ║
║                                               ║
║  ┌─────────────────────────────────────────┐  ║
║  │  INVENTARIO                             │  ║
║  │  ─────────────────────────────────────  │  ║
║  │  • Amoxicilina 500mg      20 unidades   │  ║
║  │  • Paracetamol 500mg      0 unidades    │  ║
║  │    [Vender]  [Reponer]                  │  ║
║  │                                         │  ║
║  │  El cajero toca [Vender] → la pantalla  │  ║
║  │  dice "vendido" pero el stock no baja.  │  ║
║  └─────────────────────────────────────────┘  ║
║                                               ║
║  Problema: la venta se registró a medias.     ║
╚═══════════════════════════════════════════════╝
```

Reglas del boceto:
- Máximo 3 niveles de profundidad (acción → decisión → resultado).
- Solo palabras del negocio: venta, inventario, stock, farmacia, cliente, suscripción.
- Sin nombres de clases, métodos, variables ni fragmentos de código.
- Si el flujo necesita más de 3 niveles, se divide en 2 bocetos.

## 4. Protocolo para cada tipo de comunicación

### 4A. BUG o RIESGO (responder en orden)
1. **¿Qué está pasando?** Boceto de la pantalla con la acción del usuario.
2. **¿Cuál es el problema real?** Qué falla y por qué importa al negocio.
3. **¿Qué puede pasar si no se corrige?** Consecuencia real (venta duplicada, stock
   mal contado, farmacia con datos de otra).
4. **¿Cómo queda después?** Boceto corregido.

### 4B. PLAN o PROPUESTA
1. **¿Qué flujo cambia?** Boceto antes y boceto después.
2. **¿Por qué es la mínima modificación necesaria?** Sin sobre-explicar.
3. **¿Cómo se verifica que funciona?** Qué hace el cajero para comprobarlo.

### 4C. IMPLEMENTACIÓN TERMINADA
1. **Flujo resultante:** cómo queda la experiencia del usuario paso a paso.
2. **Qué quedó blindado:** el dato que ya no puede duplicarse ni pisarse entre
   farmacias y cómo se comprobó.
3. **Cómo verificarlo:** la acción concreta que demuestra que funciona.

## 5. Tono y lenguaje

| Prohibido | Permitido |
|---|---|
| "El ViewModel emite..." | "La pantalla muestra..." |
| "La transacción es atómica..." | "La venta se guarda completa o no se guarda" |
| "El repositorio hace un get..." | "La app consulta el inventario..." |
| Código antes que el flujo | Boceto primero, código nunca |

**Tono:** socio directo, sin jerga, sin condescendencia.
**Ejemplos:** siempre del día a día de la farmacia cliente — una venta, un stock, un
inventario, un corte de luz.
