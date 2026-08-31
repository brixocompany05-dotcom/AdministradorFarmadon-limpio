ARQUITECTURA SIMPLE, SEPARADA Y CAMBIABLE (Kotlin + Compose)
Prohibido
Un Composable que decide lógica de negocio, llama a Firebase/Repository directo, o hace cálculos que no son de UI.
Un ViewModel que conoce Firestore, rutas, o detalles de red directamente (eso vive en datos/).
Una clase/archivo que mezcla más de una responsabilidad porque "ya estaba ahí".
Duplicar un dato: totales, listas filtradas, contadores o copias que se pueden derivar de la fuente única.
Un if/when gigante o anidado en vez de estados/sellados claros.
Una función que hace de todo (fetch + valida + transforma + pinta).
Agregar una capa, interfaz o abstracción "por si acaso" sin un problema real hoy.
Un archivo que crece y crece porque es más fácil pegarle algo encima que separarlo.
Capas (guía, no excusa para fragmentar de más)
ui/ (Composables): solo pinta estado y captura eventos del usuario. Cero lógica de negocio, cero Firebase.
logica/ (ViewModel / UseCase): mantiene el estado de pantalla, valida, coordina el flujo. No conoce detalles de persistencia.
datos/ (Repository / DataSource): conoce Firestore, rutas y qué debe guardarse completo.

Cada clase necesita una razón evidente para existir. Si no puedes explicar en una frase por qué existe una clase nueva, no la crees.

Una sola verdad

Cada dato tiene un único dueño. Todo lo demás se deriva ahí mismo (con derivedStateOf, mapeo en el ViewModel, etc.), nunca se guarda por separado. El Composable recibe un UiState y lo representa — no mantiene su propia copia de los datos de negocio.

Cuándo SÍ separar

Solo cuando exista una frontera real:

Responsabilidades mezcladas de verdad (no solo "se ve largo").
Lógica que hoy es difícil de entender o testear.
Reutilización real (ya se usa en 2+ lugares, no "podría usarse").
Una regla compleja que merece su propio lugar.

Si la solución directa es clara y corta, esa es la solución. Nada de arquitectura para un futuro imaginado, otra plataforma o "por si escala".

Señales de que hay que rehacer YA
Un archivo Kotlin de cientos de líneas mezclando UI + lógica + datos.
Muchos Boolean flags o estados gemelos representando lo mismo.
Un cambio pequeño que obliga a tocar 5 archivos sin relación clara.
Parches "temporales" que ya llevan semanas.
Un ViewModel con más de una responsabilidad de pantalla.

Ante cualquiera de estas: parar y corregir la estructura, no apilar otra capa encima.

Checklist antes de dar por cerrado un módulo
 ¿El Composable solo pinta y emite eventos, sin lógica de negocio?
 ¿El ViewModel/UseCase no toca Firestore/red directamente?
 ¿Cada dato tiene un único dueño, sin copias derivadas guardadas aparte?
 ¿Cada clase/archivo tiene una sola responsabilidad clara?
 ¿Alguien puede decir en una frase dónde vive la verdad y quién la puede modificar?
 ¿Qué pieza cambia si cambia la regla de negocio? (debe ser una, no cinco)

Si no puede responderse con claridad, simplificar antes de seguir.