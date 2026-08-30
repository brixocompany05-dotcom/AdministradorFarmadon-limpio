---
name: cerebro-pensamiento
description: Razona sobre producto y software como un sistema dentro del mundo real. Úsala para cuestionar planes, descubrir trabajo innecesario, anticipar consecuencias y elegir soluciones simples que prevengan problemas.
---

# RAZONAMIENTO DE SISTEMA EN EL MUNDO REAL

## Propósito

No evaluar una funcionalidad únicamente desde el código, la pantalla o la implementación
técnica. Antes de aceptar un diseño, cambio o solución, analizar cómo existe dentro de un
entorno real con personas, procesos, datos, tiempo, responsabilidades y consecuencias.

El objetivo no es solamente que el sistema funcione. Es que tenga sentido, reduzca esfuerzo
humano, prevenga problemas y mantenga coherencia con la realidad operativa.

## Regla principal

Nunca analizar solo:

```text
acción → resultado esperado
```

Analizar:

```text
intención humana
        ↓
acción
        ↓
estado del sistema
        ↓
cambios generados
        ↓
consecuencias
        ↓
personas afectadas
        ↓
nuevas decisiones
        ↓
resultado final
```

## Preguntas universales

### Realidad humana

- ¿Qué intenta conseguir realmente la persona?
- ¿La solución facilita ese objetivo o agrega pasos?
- ¿La persona entiende qué está ocurriendo?
- ¿El sistema ayuda o traslada trabajo al usuario?

### Trabajo innecesario

Buscar tareas repetitivas, datos que se vuelven a introducir, decisiones simples que el
sistema podría resolver y procesos donde la persona funciona como conexión manual entre
partes que ya conocen la misma información.

Preguntar: ¿por qué una persona debe hacer esto si el sistema tiene suficiente información
para ayudar?

### Consecuencias completas

Después de cada cambio preguntar qué cambia, quién se afecta, qué depende de ello, qué debe
actualizarse, qué nuevas acciones aparecen y qué responsabilidades genera.

### Caminos alternativos

Analizar información incompleta o incorrecta, personas distintas, cambios inesperados,
interrupciones, abandono, repetición, simultaneidad, estados intermedios y respuestas que
llegan tarde.

### Verdad del sistema

Comprobar si algo ocurrió realmente o solo se intentó, si existe confirmación real, si la
información está vigente y si un mensaje bonito está ocultando incertidumbre.

### Estados y tiempo

Todo estado necesita inicio, duración, cambios posibles, condición de término y siguiente
paso. No crear estados que no tengan ciclo de vida completo.

### Personas y fuentes de verdad

Preguntar qué pasa si otra persona modifica el dato, si dos acciones llegan juntas o si el
contexto cambia mientras alguien trabaja. Definir quién manda cuando existen versiones
distintas y evitar que una respuesta vieja pinte información en un contexto nuevo.

### Prevención

No diseñar solamente “la persona se equivoca y mostramos un error”. Buscar cómo impedir que
llegue al error: mostrar consecuencias antes de decidir, pedir solo lo necesario, detectar
contradicciones a tiempo y guiar el siguiente paso.

### Valor real

No aceptar una función porque se vea moderna, tenga más opciones o use tecnología nueva.
Medirla por el problema que elimina, el tiempo que ahorra, los errores que evita, las
decisiones que facilita y la carga mental que quita.

### Permanencia

Comprobar que siga siendo entendible con pocos datos, con crecimiento, con historial
acumulado y después de meses de uso. Esto no autoriza a construir arquitectura hipotética:
la solución debe ser simple hoy y no encerrar el cambio mañana.

## Modo crítico y profundidad

Antes de aceptar una propuesta, asumir temporalmente que puede estar mal. No defender la
primera idea; intentar mejorarla o reemplazarla por una alternativa más simple y poderosa.

La profundidad se ajusta al impacto:

- cambio pequeño y local: intención, riesgo principal y resultado;
- funcionalidad nueva: preguntas universales y flujo completo;
- cambio que afecta datos, dinero, inventario, usuarios o módulos: análisis profundo y
  auditoría de verdad.

Así se piensa siempre en consecuencias sin convertir cada ajuste visual en un trámite.

## Pregunta de cierre

> Si esto estuviera funcionando en un negocio real durante un día completo, ¿seguiría siendo
> lógico, útil, vigente y fácil para las personas que lo usan?

Si no es claramente sí, seguir cuestionando.
