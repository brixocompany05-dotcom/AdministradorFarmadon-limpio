Firebase: cero datos viejos, cero mezcla entre contextos
Prohibido
Mostrar un dato de otra farmacia, sucursal o usuario, aunque sea por un instante.
Pintar la respuesta de un listener/consulta que ya no corresponde al contexto actual (usuario cambió de farmacia/sucursal/pantalla).
Mostrar una hora, fecha o timestamp que no venga de una fuente real (serverTimestamp(), Timestamp.now() o UTC válido). Nunca inventar, hardcodear o "aproximar" una fecha/hora.
Disfrazar un error como lista vacía, cero, texto genérico o "éxito".
Guardar una operación multi-dato a medias (mitad aplicada, mitad no).
Dejar un listener vivo sin dueño que lo cierre.
Recargar todo el estado cuando bastaba actualizar una parte (y de paso se pierde la selección o el trabajo del usuario).
Contexto = ley

El contexto de farmacia/sucursal sale SIEMPRE de la sesión o fuente oficial, nunca de la pantalla. Toda query, transacción o escritura lo lleva explícito. Al cambiar de farmacia/sucursal/pantalla: cancelar todo listener/consulta anterior antes de pintar algo nuevo. Ninguna respuesta tardía de un contexto viejo debe llegar a pintarse.

Elegir la fuente de datos

Antes de escribir código, define en una línea:

Listener → el dato puede cambiar mientras la persona decide.
Recarga → una desactualización breve no importa.
Consulta puntual → decisión única y acotada; si hay concurrencia, se reverifica la verdad dentro de la operación final.

Nunca dejar una foto estática indefinidamente.

Estados obligatorios

Todo dato en pantalla pasa por uno de estos, sin mezclarlos: cargando / vacío real / error real / datos previos refrescándose. Toda carga termina en éxito, error visible o cancelación — nunca en silencio.

Operaciones atómicas

Si una acción toca varios datos: todo o nada. Después de guardar, actualizar solo lo que quedó viejo (detalle, lista, resumen, contador, badge) — no recargar todo a lo bruto.

Fechas

serverTimestamp() solo en campos de primer nivel o submapas directos. En arreglos/listas/arrayUnion() usar Timestamp.now() o UTC válido. Ninguna fecha/hora falsa o de relleno, nunca.

Checklist antes de cerrar
 ¿El contexto de farmacia/sucursal es el correcto en cada operación?
 ¿Se canceló todo listener/consulta del contexto anterior?
 ¿Ninguna respuesta tardía puede pintarse fuera de contexto?
 ¿La operación es atómica (todo o nada)?
 ¿Los estados de error/vacío/carga son honestos?
 ¿Todo listener tiene dueño y se limpia?
 ¿Las fechas/horas vienen de fuente real, ninguna inventada?
 ¿Se actualizó solo lo que quedó desactualizado, sin recargar todo?

Si algo queda incierto, se declara — nunca se presenta como resuelto.