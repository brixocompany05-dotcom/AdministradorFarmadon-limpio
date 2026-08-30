---
name: firebase-android
description: Usa Firebase y Firestore en Farmadon manteniendo aislamiento por farmacia, datos vigentes y operaciones completas. Úsala cuando una pantalla o acción toque Auth, Firestore, Storage, Crashlytics o Remote Config.
---

# FIREBASE, DATOS VIGENTES Y CONTEXTO CORRECTO

## Propósito

Firebase debe representar la realidad del negocio, no una foto vieja ni una operación a
medias. Cada farmacia es un contexto independiente. Cada dato debe leerse y escribirse en
el lugar correcto, con la frescura que exige la decisión y con un ciclo de observación claro.

## Aislamiento esencial

Toda consulta, transacción y escritura operativa conserva explícitamente la pertenencia
correcta de farmacia y, cuando corresponda, sucursal. El contexto proviene de la sesión o
fuente oficial, no de un valor inventado por la pantalla. No agregar permisos, roles o
restricciones nuevas sin aprobación explícita del dueño.

## Contrato de frescura

Antes de elegir listener, recarga o consulta puntual, responder:

1. cuál es la fuente de verdad;
2. qué error produce que envejezca;
3. cuánto tiempo puede tolerarse;
4. cuándo debe actualizarse;
5. cómo se detecta el cambio;
6. quién inicia y quién detiene la observación.

Usar escucha viva cuando el dato cambia mientras la persona decide. Usar recarga cuando
tolera vejez breve. Usar consulta puntual para una decisión específica, pero verificar la
verdad dentro de la operación final cuando exista concurrencia.

Nunca dejar una foto estática indefinidamente. Al cambiar farmacia, sucursal o pantalla,
cancelar lo anterior y evitar que una respuesta tardía del contexto anterior pinte datos
como si fueran actuales.

## Estados honestos

Distinguir entre todavía cargando, vacío real, error real y datos anteriores mientras se
actualizan. No convertir una falla en una lista vacía, cero, texto genérico o éxito falso.
Toda carga termina en resultado, error visible o cancelación clara; todo listener tiene
dueño y se limpia al terminar su ciclo de vida.

## Operaciones completas

Cuando una acción modifica varios datos, leer la verdad necesaria y guardar el conjunto de
forma que quede completo o no quede aplicado. El sistema debe tolerar red lenta y dejar que
Firebase determine cuándo la operación falló definitivamente; no cortar prematuramente por
una suposición.

Después de modificar un dato, revisar qué quedó viejo: detalle, lista, resumen, contador,
badge, límite o pantalla relacionada. Actualizar solo la cadena necesaria, sin recargar
todo ciegamente ni perder la selección o el trabajo de la persona.

## Fechas y listas

`serverTimestamp()` solo puede usarse en campos de primer nivel o submapas directos. En
listas, arreglos o `arrayUnion()` usar `Timestamp.now()` o una fecha UTC válida.

## Revisión antes de cerrar

Comprobar contexto de farmacia/sucursal, fuente de verdad, frescura, respuesta tardía,
concurrencia, guardado completo, estado de error, limpieza de listeners y actualización de
las pantallas dependientes. Si algún punto es incierto, declararlo y no presentarlo como
terminado.
