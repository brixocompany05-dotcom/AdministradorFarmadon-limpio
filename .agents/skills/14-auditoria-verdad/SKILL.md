---
name: auditoria-verdad
description: Audita una funcionalidad o módulo de extremo a extremo para descubrir huecos, datos falsos, pérdidas, consecuencias cruzadas y caminos no cubiertos. Úsala cuando el cambio tenga impacto real o se solicite una auditoría profunda.
---

# AUDITORÍA DE VERDAD E IMPACTO

## Propósito

No aceptar una solución solo porque compila, se ve bien o funciona en el camino feliz.
Reconstruir cómo vive dentro del sistema real y buscar activamente qué podría estar mal,
qué se puede perder, qué queda viejo y qué otros procesos o personas se ven afectados.

La auditoría no es un ritual para cada cambio pequeño. Su profundidad debe corresponder
al impacto: una modificación local puede tener una revisión breve; una acción que toca
datos, dinero, inventario, permisos, sucursales o varios módulos exige auditoría completa.

## Fases de análisis

### 1. Mapa real

Leer los archivos y seguir la acción completa: entrada, pantalla, lógica, persistencia,
escuchas, rutas, estados y resultado. Identificar fuentes de verdad, actores, módulos
afectados y qué ocurre al cambiar de contexto.

### 2. Supuestos y falsedad

Buscar valores por defecto que inventan información, datos congelados, relojes incorrectos,
errores convertidos en vacío o éxito, identificadores tomados de un contexto equivocado y
estados que no representan lo que ocurrió.

### 3. Puntería y conservación

Verificar que cada lectura y escritura llegue al lugar correcto, conserve la pertenencia
del negocio y no pise información ajena. Revisar campo por campo qué se modifica, qué se
conserva, qué se deriva, qué se elimina y qué rastro debe permanecer.

### 4. Consecuencia cruzada

Seguir el efecto dominó: qué cambia en otras pantallas, procesos, saldos, existencias,
historiales, límites, reportes y usuarios. Una regla del negocio debe mantenerse igual en
todos sus caminos, no solo en el botón principal.

### 5. Tiempo y verdad vigente

Preguntar qué pasa si el dato cambia mientras alguien mira, si cambia de sede o cuenta, si
la respuesta llega tarde o si otra persona modifica lo mismo. La pantalla no puede pintar
la respuesta de un contexto anterior como si perteneciera al nuevo.

### 6. Caminos no felices

Definir qué ve la persona y qué queda guardado en cada caso:

- camino normal;
- error de validación o persistencia;
- repetición o doble ejecución;
- interrupción, abandono o cierre;
- información incompleta, eliminada, antigua o inesperada;
- dos personas actuando al mismo tiempo.

### 7. Después de la confirmación

No terminar en “éxito”. Verificar quién necesita saberlo, qué dato debe actualizarse,
qué acción queda disponible, qué responsabilidad nace, qué rastro se consulta mañana y
qué pasa si el usuario vuelve después.

## Modo crítico

Antes de aprobar, asumir que la propuesta puede estar incompleta. Buscar huecos de lógica,
trabajo manual escondido, estados imposibles, decisiones a ciegas, información perdida,
contradicciones y dependencias no declaradas. Si aparece una solución más simple que
protege mejor el resultado, proponerla.

## Evidencia y salida

El resultado debe separar hechos comprobados, riesgos, decisiones y deuda consciente. Para
cualquier hallazgo indicar: dónde ocurre, qué persona lo experimenta, qué daño produce,
cómo se previene y cómo se comprueba. No declarar “todo bien” si falta revisar un camino.

La pregunta de cierre es:

> Si esto funcionara durante un día completo, con cambios, interrupciones y personas
> distintas, ¿seguiría siendo lógico, útil, vigente y fácil de usar?

Si la respuesta no es claramente sí, la auditoría aún no terminó.
