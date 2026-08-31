---
name: prevencion-errores
description: Diseña flujos que previenen errores, guían a la persona y dejan recuperación clara. Úsala cuando una acción pueda duplicarse, perder trabajo, producir una decisión equivocada o dejar estados confusos.
---

# PREVENCIÓN DE ERRORES Y GUÍA DEL SISTEMA

## Propósito

El sistema no debe esperar a que la persona se equivoque para mostrar un mensaje. Debe
ayudar antes de la decisión, reducir la memoria y escritura que exige, mostrar el efecto
real de la acción y conducir al siguiente paso sin confusión.

## Antes del error

Para cada acción identificar:

- qué condición vuelve peligrosa o inválida la decisión;
- qué información ya conoce el sistema para detectarla;
- qué puede mostrar antes de confirmar;
- qué dato o paso puede eliminarse porque es repetido;
- cómo se evita que una repetición ejecute dos veces la misma consecuencia.

Prevenir no significa añadir bloqueos por miedo. Significa poner la información correcta
en el momento correcto y reservar las interrupciones para consecuencias realmente importantes.

## Flujo guiado

La persona debe saber:

1. qué está haciendo;
2. qué necesita completar;
3. qué va a cambiar;
4. cómo confirmar;
5. cómo cancelar o volver;
6. qué ocurrió después.

El sistema debe conservar lo que ya se escribió cuando un paso posterior falla, siempre que
conservarlo no genere una operación incompleta. Si el conjunto debe ser indivisible, no
mostrar éxito ni dejar una mitad presentada como terminada.

## Estados y feedback

Diseñar estados reales de carga, validación, guardado, éxito, error, cancelación y salida.
Mientras una acción está en vuelo, evitar duplicados y devolver el control en todos los
caminos: éxito, error, cancelación, cierre e interrupción.

Los mensajes deben decir qué pasó y cuál es el siguiente paso. Nunca ocultar el motivo con
un texto genérico ni convertir incertidumbre en confirmación.

### Regla de bloqueo y éxito visible

Si la acción queda invalidada por una regla de negocio o un conflicto real (duplicado,
estado no permitido, falta crítica, condición de carrera), la persona debe verlo en la UI
exactamente en el campo, panel o botón afectado. El bloqueo visual debe ser claro, concreto
y profesional, no un log del programador ni un mensaje silencioso.

Cuando la operación se completa, la pantalla debe reflejar ese éxito de forma visible y
honesta: toast, banner, estado actualizado o confirmación clara. No se puede anunciar un
éxito solo porque el código ejecutó una línea; debe existir resultado visible para la persona.

### Regla del catch

El `catch` existe para diagnóstico del desarrollador: log, trazabilidad, contexto técnico y
depuración. No se usa para ocultarle al usuario un problema, ni para maquillar una falla con
mensaje genérico, ni para convertir una operación fallida en un falso éxito. El usuario debe
recibir la verdad del bloqueo o del éxito; el programador recibe la excepción en el log.

## Datos y decisiones

Los números, fechas, límites, disponibilidad y consecuencias deben venir de fuentes
verificadas. La persona elige o confirma; no debería hacer cálculos que el sistema puede
hacer. Si una decisión depende de un dato que puede cambiar, verificarlo justo antes de
aplicarla.

## Después

Una acción no termina con un cartel. Confirmar qué quedó actualizado, quién debe verlo,
qué nueva acción se habilita, qué rastro queda y qué verá la persona al volver más tarde.
No crear tareas, alertas o pendientes si la acción física que las justificaría nunca
ocurrió.

## Criterio de cierre

El mejor flujo es aquel donde la persona entiende más, recuerda menos, se equivoca menos y
puede recuperarse sin perder trabajo. Si para proteger una acción se añadieron pasos que no
reducen un riesgo real, simplificarla.
