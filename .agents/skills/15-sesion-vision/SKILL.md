---
name: sesion-vision
description: >-
  Sesión de VISIÓN — pensar como dueño qué le falta a la app. NO es auditar (lo
  roto) ni revisar (lo hecho): es descubrir innovación, automatización, mejor
  trabajo y menos estrés para el día real de la farmacia. Se invoca con:
  "piensa qué le falta a la app" o "sesión de visión sobre [módulo]".
---

# SESIÓN DE VISIÓN (PENSAMIENTO DE PRODUCTO · v1.0)

> **PROPÓSITO:** encontrar lo que Farmadon NECESITA y aún no tiene — cosas que
> aporten innovación, automatización, mejor trabajo o menor estrés al personal
> de la farmacia y a su dueño. No habla de seguridad ni de deuda técnica: eso
> viven en auditorías. Aquí manda una sola pregunta: **"¿qué trabajo humano
> tonto, lento o invisible queda en el mostrador, el inventario o la caja,
> que el sistema podría eliminar, automatizar o sugerir?"**
>
> Piensa la PELÍCULA completa del día (8am apertura → mercadería → ventas →
> cierre): ¿en qué momento el sistema ayuda de menos?

---

## LAS 3 FASES

### FASE A · RECORRER CON LENTE DE OPORTUNIDAD
Recorrer los módulos (empezando por los nunca visitados) leyendo código REAL con
estas preguntas:
- ¿Qué se memoriza o se hace a mano que el sistema ya sabe (stock mínimo,
  vencimientos próximos, productos que venden juntos)?
- ¿Dónde el sistema DETECTA algo pero se queda mirando sin ofrecer la acción?
- ¿Qué espera pasivamente cuando podría empujar (vencimiento de lote, quiebre
  de stock, cuadre de caja)?
- ¿Qué reporte del negocio hoy no existe y cambiaría decisiones (lo más
  vendido, lo rentable, lo fiado, lo vencido)?
- ¿Dónde el usuario repite pasos en lote sin ayuda (carga de mercadería,
  precios por laboratorio)?
- ¿El cierre del día deja trabajo manual que podría ser un botón?

Prohibido proponer sin citar el dolor observado (archivo/flujo/operación real).

### FASE B · MATRIZ DE OPORTUNIDADES
Cada idea validada entra a la tabla:

| Dolor real (evidencia) | Oportunidad | Valor para la farmacia | Esfuerzo | Fase sugerida |
|---|---|---|---|---|

Clasificación de valor válida: pérdida evitada (vencidos, quiebres) · velocidad
de mostrador · prevención de error legal/sanitario · menos horas de inventario ·
dato nuevo que cambia decisiones del dueño.

### FASE C · MENÚ AL DUEÑO
Entregar el menú priorizado. El dueño elige; lo aprobado entra al flujo normal
(Encuadra → Implementa → Verifica → Cierra). Esta skill NUNCA implementa:
produce decisiones, no código de producto.

---

## FILTROS ANTI-MODA (rechazar ideas)

- ❌ "Estaría bonito" / feature porque otra app de farmacia la tiene.
- ❌ IA/automatización sin dolor concreto detrás.
- ❌ Datos que nadie en la farmacia consumirá ("dato muerto").
- ✅ SÍ: elimina memorización del cajero/químico, convierte detección en acción,
  anticipa (vencimientos, quiebres), cierra el día sin planillas manuales.

## PUERTA DE SALIDA

- [ ] Menú con ≥1 evidencia por idea (módulo/flujo donde duele)
- [ ] Valor expresado en lenguaje de negocio (no técnico)
- [ ] Esfuerzo estimado honesto (S/M/L + si requiere infraestructura nueva → marcado)
- [ ] El dueño decidió qué entra; lo elegido queda listo para Encuadre normal

## FRONTERAS

- El filtro de valor base vive en `01-cerebro-valor`; los 5 criterios de razonamiento
  en `cerebro-pensamiento` (esta skill es su aplicación de producto, corrida como sesión).
- Hallazgos de datos rotos durante el recorrido NO se arreglan aquí: se anotan
  para `14-auditoria-verdad` o flujo normal.
