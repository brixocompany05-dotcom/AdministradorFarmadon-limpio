COMUNICACIÓN HUMANA: HALLAZGO + PROPUESTA
Regla obligatoria

Cada vez que se detecte algo (un bug, un riesgo, una decisión técnica, un resultado de revisión), la respuesta se organiza SIEMPRE en dos bloques, en ese orden:

text
🔎 QUÉ ENCONTRÉ
(en lenguaje de negocio, lo que pasa y por qué importa)

✅ QUÉ PROPONGO
(en lenguaje de negocio, qué se haría y qué cambiaría para la persona)

Nada de código, nombres de clases, funciones, variables ni jerga técnica en estos dos bloques. Si hace falta mostrar código, va DESPUÉS, aparte, y solo si se pide o es imprescindible.

Prohibido
Explicar el hallazgo con nombres de archivos, funciones, clases o stacktraces como primera línea.
Usar palabras como "null", "excepción", "caché", "listener", "race condition", "estado" sin traducirlas a lo que la persona vive ("se cae la app", "muestra datos viejos", "dos personas pueden pisarse el cambio al mismo tiempo").
Dejar el hallazgo sin una propuesta concreta al lado. Un hallazgo sin propuesta es una queja, no un reporte útil.
Prometer que algo "ya está resuelto" cuando solo fue intentado o no se verificó.
Cómo redactar "QUÉ ENCONTRÉ"

Responder, en un párrafo corto o 2-3 líneas:

Qué ve o vive la persona (el síntoma real, no la causa técnica).
Qué se esperaba que pasara en cambio.
Qué consecuencia tiene para el negocio (dinero, tiempo, confianza, error visible al cliente).
Cómo redactar "QUÉ PROPONGO"

Responder, en un párrafo corto o lista breve:

Qué se va a cambiar, en términos de comportamiento visible ("cuando pase X, el sistema hará Y").
Cómo se confirma que quedó bien.
Qué pasa si algo falla en el camino (para que no sea sorpresa).
Ejemplo de tono correcto
text
🔎 QUÉ ENCONTRÉ
Cuando dos personas editan el mismo lote casi al mismo tiempo, la segunda
edición borra sin avisar lo que guardó la primera. La persona no se entera
de que perdió su cambio.

✅ QUÉ PROPONGO
Antes de guardar, el sistema va a avisar si alguien más modificó ese lote
mientras tanto, y va a dejar elegir qué versión conservar. Así nadie pierde
un cambio sin darse cuenta.
Boceto de flujo (cuando se explica un cambio o una pantalla nueva)
text
intención de la persona
        ↓
acción visible
        ↓
respuesta honesta del sistema
        ↓
consecuencia y siguiente paso

El boceto muestra experiencia, no nombres de clases, variables o servicios. Si mezcla dos problemas, se divide en dos bocetos.

Regla de claridad final

Una persona no debería tener que memorizar el estado del sistema ni adivinar el siguiente paso. Todo hallazgo va acompañado de su propuesta, y ambos se entienden sin saber programar.