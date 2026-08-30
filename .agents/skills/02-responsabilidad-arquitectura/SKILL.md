---
name: responsabilidad-arquitectura
description: Mantén el software simple, legible y fácil de cambiar. Úsala cuando una solución pueda crear capas, clases, estados o duplicación innecesaria.
---

# ARQUITECTURA SIMPLE Y CAMBIABLE

## Propósito

La arquitectura debe ayudar a resolver el problema actual, no construir un sistema
hipotético para problemas que todavía no existen. La mejor solución es la más pequeña
que mantiene responsabilidades claras y permite cambiar la decisión mañana.

## Antes de separar algo

Preguntar qué problema concreto de la implementación actual resuelve una nueva clase,
capa, interfaz, helper o abstracción. Solo separar cuando exista una frontera real:

- responsabilidades mezcladas;
- lógica difícil de entender o verificar;
- reutilización real;
- dependencia que vuelve peligroso un cambio;
- una regla compleja que necesita un lugar propio.

Si una solución directa es clara, usarla. No crear infraestructura por escalabilidad,
por otra base de datos, por otra plataforma ni por un futuro imaginado.

## Responsabilidades visibles

- `ui/`: muestra el estado y captura decisiones. No conoce Firebase ni reglas de negocio.
- `logica/`: mantiene el estado de la pantalla, valida y coordina el flujo.
- `datos/`: conoce la persistencia, las rutas y las operaciones que deben guardarse completas.

Esta separación es una guía, no una excusa para fragmentar un flujo simple en muchas
piezas. Cada clase debe tener una razón evidente para existir y un nombre que explique
su intención.

## Una sola verdad

Cada dato tiene un dueño. El resto se deriva de él. No guardar por separado totales,
listas filtradas, contadores o copias de objetos que pueden calcularse desde la fuente.
La pantalla recibe un estado y lo representa; no mantiene una segunda versión de los
datos del negocio.

Las rutas de persistencia deben estar centralizadas y cada lectura o escritura debe
conservar el contexto correcto del negocio. Una acción importante debe poder seguirse
de principio a fin sin saltar entre capas artificiales.

## Señales para rehacer

Si aparecen muchos flags, estados gemelos, `if` anidados, funciones que hacen de todo,
parches de compatibilidad o una modificación que obliga a tocar demasiados lugares,
detenerse y corregir la estructura. No agregar otra capa encima del problema.

## Criterio de cierre

La arquitectura está bien cuando otra persona puede responder rápidamente:

1. dónde vive la verdad;
2. quién puede modificarla;
3. qué ocurre después de modificarla;
4. qué pieza debe cambiar si cambia la regla.

Si no puede responderse, simplificar antes de continuar.
