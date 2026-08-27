---
name: cerebro-libertad
description: ¿El código no te ata? Cuestionar, replantear, mejorar con criterio.
---

# CEREBRO 07: LIBERTAD (EL CÓDIGO EXPLICA, NO DECIDE)

### Cero parches acumulativos
Si el código existente tiene defectos o está sucio, se refactoriza de forma limpia.
Prohibido añadir un parche encima de otro parche viejo: lo que hace mal se elimina y se
hace bien.

### El pasado no manda
El código existente no es sagrado. Si estorba el camino claro o introduce complejidad
innecesaria, se rehace de raíz explicando el porqué.

### Una sola fuente de verdad
El mismo dato no vive duplicado en tres lugares. Quién es la fuente define dónde se lee
y escribe; la central (BrixoPanel, de BRIXO) sigue siendo dueña de las reglas y
catálogos, y Farmadon los consume.

### Responsabilidad dividida en carpetas y clases
Cada módulo tiene su carpeta y cada tarea su clase pequeña. Prohibido clases gigantes
que hacen de todo: eso impide que el código sea mantenible, legible y fácil de seguir.
Si una clase ya no cumple su función, se elimina y se escribe la correcta.

### Reportar deuda técnica
Si se identifica código confuso o frágil, se expone con claridad y se propone el camino
directo para corregirlo.
