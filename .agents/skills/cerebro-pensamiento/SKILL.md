---
name: cerebro-pensamiento
description: >-
  Disciplina de pensamiento obligatoria antes y después de codear. Consolida 5 criterios:
  (1) alcance controlado — un criterio del usuario no es tarea nueva; (2) prueba de valor
  real — vale por el trabajo real, no por existir; (3) realidad operativa — simular la
  vida real, no el happy path; (4) pensar como película — el día completo de 8am a cierre;
  (5) pensar en el después — el "Éxito" no es el final. Usar SIEMPRE al razonar, construir
  y cerrar cualquier funcionalidad.
---

# CEREBRO-PENSAMIENTO (Disciplina de razonamiento obligatoria)

> Una sola skill con los 5 filtros de pensamiento. No son tareas separadas: son el modo
> en que se evalúa TODO antes de codear, durante y al cerrar.

---

## 1. ALCANCE CONTROLADO (un criterio no es tarea nueva)

Cuando el usuario, en medio de una tarea, da un principio/crítica/ejemplo/pregunta
conceptual, eso es un **CRITERIO** a aplicar a la tarea actual, NO una tarea nueva.

- Nombrar la tarea actual: "Ahora mismo estamos construyendo: ___".
- Clasificar: (a) cambia la tarea solo si lo pide explícito; si no, es (b) restricción,
  (c) criterio de razonamiento, (d) hueco en lo actual, (e) pregunta, (f) ejemplo.
  Por defecto NO es tarea nueva.
- Fusionar: `tarea actual + criterio → tarea actual revisada`.
- Ideas fuera de alcance: registrar, no ejecutar. Terminar donde se empezó.
- Si dice "eso es un criterio, no una tarea nueva / vuelve al flujo", descartar toda
  propuesta de arquitectura nueva esbozada y retomar la tarea original.

---

## 2. PRUEBA DE VALOR REAL (vale por el trabajo real, no por existir)

Una funcionalidad no es valiosa porque exista. Es valiosa cuando mejora de forma tangible
el trabajo real del negocio.

- Pantalla bonita puede ser inútil; automatización invisible puede ser extremadamente
  valiosa; módulo de 20 funciones puede aportar menos que 1 automatización bien hecha.
- Antes de celebrar: ¿qué trabajo real toca? ¿en qué lo mejora (menos pasos/errores/
  tiempo, cero duplicación)? Si no se responde, está hecho pero no sirve: no se celebra.
- Cierre: nombrar el trabajo real mejorado, no la pieza construida.

---

## 3. RAZONAMIENTO DE REALIDAD OPERATIVA (simular la vida real)

No evaluar el software como software. Evaluarlo como realidad dentro de un negocio:
personas reales, interrupciones, errores, decisiones, consecuencias, dinero, tiempo,
responsabilidades, cambios de estado, situaciones inesperadas.

- Antes de decir "funciona", responder: ¿qué pasa con interrupción (red caída a mitad),
  error humano, concurrencia (dos tocan lo mismo), cambio de estado en vivo, situación
  inesperada (cancelar a medias, reintentar)? Si solo se describe el camino perfecto,
  está rota en la vida aunque compile.
- Persona real, no "el usuario": nombrar quién lo usa y con qué prisa/contexto.

---

## 4. PENSAR COMO UNA PELÍCULA (el día completo, no un diagrama)

Imaginar el día corrido y preguntarse: **¿el sistema sigue diciendo la verdad durante
toda la película?**

- 8:00 abre → 9:15 mercadería → 10:30 vende → 11:00 llega empleado → 12:20 falta stock →
  13:00 proveedor responde → 15:00 se descubre un error → 18:00 se cae internet →
  19:00 vuelve internet → 21:00 cierre.
- Momento crítico: 18:00 cae internet — el sistema NO debe mentir "guardado"; 19:00 vuelve
  y reintenta lo a medias, queda coherente (R3/R9/R11). Cierre honesto: 21:00 refleja TODO
  el día, no esconde huecos.

---

## 5. PENSAR EN EL "DESPUÉS" (el Éxito no es el final)

Nunca terminar el razonamiento en el cartel de "Éxito". "Creado correctamente" no es el fin.

Tras cualquier Éxito, el cierre real es el **después**. Preguntar:
- ¿Quién necesita saberlo? ¿Qué cambia? ¿Qué queda pendiente (consecuencia que el sistema
  debió disparar solo)? ¿Qué acción ahora debería ser posible? ¿Qué información debe
  actualizarse (vista en vivo, R8)? ¿Qué ocurre si el usuario vuelve mañana (dato íntegro)?
- Si no se responde, el flujo NO terminó: solo llegó al cartel de Éxito.

---

---

## 6. LA PRUEBA DE LA VIDA REAL (las 8 preguntas — obligatoria antes de decir "terminado")

> Los candados técnicos no bastan: toda funcionalidad se valida simulando al USUARIO
> PARADO FRENTE AL BOTÓN, con la vida del negocio encima. Son universales — sirven
> para cualquier módulo presente o futuro. Si una falla, la función NO está terminada.

1. **¿Quién NO debería poder hacer esto hoy?** — según el estado VIVO de todo lo
   involucrado (bloqueado, congelado, vencido, agotado, retirado, sin acceso).
   El candado existe ANTES del botón, no después del error.
2. **¿Ya tiene o tuvo algo igual o que choque?** — historial y situaciones en curso
   visibles ANTES de decidir. Decidir a ciegas = bug.
3. **¿Ve el resultado EXACTO antes de confirmar?** — la consecuencia precisa dictada
   por el sistema (fechas, montos, totales). Jamás texto ilustrativo ni aritmética
   hecha a mano por el usuario.
4. **¿Cómo se sale de aquí?** — cancelar visible + gesto atrás cubierto. Cero
   pantallas sin puerta de vuelta.
5. **¿Qué pasa si dos personas lo hacen a la vez / alguien reintenta tras un corte?**
   — carrera serializada y llave anti-duplicado: el segundo intento llega a "ya
   procesado", nunca al daño doble.
6. **¿El después queda registrado y visible?** — quién, cuándo, por qué; rastro
   permanente que el próximo pueda consultar mañana.
7. **¿Los números los dicta el sistema o los inventa el usuario?** — fechas, montos,
   contadores calculados de fuentes verificadas; el usuario ESCOGE opciones, no
   hace aritmética.
8. **Si mañana editan o borran lo que esta acción referencia, ¿cambia lo ya hecho u
   otorgado?** — snapshots congelados al momento del hecho: el pasado es inmune al
   catálogo futuro.

Estas 8 viven aquí porque aplican a CUALQUIER módulo futuro. Las auditorías
(`14-auditoria-verdad`) las usan como gate de salida; los ejemplos locales de cada
dominio las acompañan como evidencia, jamás como límite de alcance.

---

## Cierre de sesión (obligatorio aplicar los 5+1)

La explicación debe incluir: el alcance fijado, el valor real entregado, un escenario de
realidad operativa resuelto, un tramo de la película donde el sistema dijo la verdad, el
recorrido del "después" tras el Éxito — y las 8 preguntas respondidas para cada acción nueva.
