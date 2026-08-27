---
name: responsabilidad-arquitectura
description: Código fácil de leer, encontrar y modificar. Responsabilidades claras, arquitectura mínima y cero abstracción especulativa.
---

# SKILL 02: RESPONSABILIDAD Y ARQUITECTURA RADICALMENTE SIMPLE

> **Responsabilidad clara ≠ Arquitectura fragmentada.**
>
> El objetivo es mantener el código ordenado y comprensible, no multiplicar archivos.

---

## 1. REGLA DE SIMPLICIDAD ARQUITECTÓNICA

**La arquitectura mínima que mantiene responsabilidades claras es la arquitectura correcta.**

Antes de crear una:

* clase
* capa
* interfaz
* mapper
* use case
* manager
* helper
* abstracción

debes poder responder:

> **¿Qué problema concreto de la implementación actual resuelve esto?**

Si no existe un problema real, **no crearlo**.

Si el problema se resuelve limpiamente con una estructura directa, se mantiene así.

Una separación adicional (hacia el estándar de la **Skill 05: Organización Modular**) solo está justificada cuando:

* una responsabilidad ya está claramente mezclada;
* una parte de la lógica se volvió difícil de entender;
* existe una regla de negocio suficientemente compleja;
* existe reutilización real;
* existe acoplamiento problemático;
* la separación reduce la carga mental;
* la separación facilita una necesidad real de pruebas o mantenimiento.

---

## 2. NO ANTICIPAR ARQUITECTURA

**No construir hoy lo que quizá necesitemos mañana.**

Prohibido crear estructuras únicamente por:

* "escalabilidad futura";
* "por si usamos otra base de datos";
* "por si agregamos otra plataforma";
* "por si mañana tenemos múltiples implementaciones";
* "por si necesitamos más abstracción".

Resolver la complejidad cuando exista.

Preparar el código para cambiar significa principalmente:

* responsabilidades claras;
* nombres correctos;
* dependencias simples;
* lógica comprensible;
* bajo acoplamiento innecesario.

**Preparar para el cambio ≠ construir infraestructura para cambios hipotéticos.**

---

## 3. RESPONSABILIDAD IDENTIFICABLE

Antes de agregar lógica preguntar:

> **¿Quién es responsable de esto?**

### UI

Responsable de:

* mostrar información;
* recibir interacción;
* representar estados;
* controlar comportamiento visual.

No debe convertirse en el lugar donde viven reglas de negocio complejas.

### Lógica / ViewModel

Responsable principalmente de:

* mantener el estado de la pantalla;
* coordinar acciones;
* orquestar el flujo;
* transformar acciones del usuario en operaciones del sistema.

No debe convertirse automáticamente en el dueño de toda la lógica existente.

### Datos / Repository

Responsable de:

* acceder a datos;
* guardar datos;
* actualizar datos;
* eliminar datos;
* ocultar los detalles de persistencia.

No debe encargarse de UI, navegación o presentación.

### Otras clases

Solo introducirlas cuando exista una responsabilidad que realmente necesite vivir separada.

Ejemplos:

* Validator → cuando una validación tenga suficiente complejidad para justificarlo.
* Mapper → cuando una transformación sea suficientemente relevante o repetida.
* UseCase → cuando un proceso de negocio tenga complejidad o coordinación propia.
* Servicio → cuando exista una responsabilidad externa o especializada que realmente lo requiera.

**No crear estas piezas automáticamente.**

---

## 4. UNA RESPONSABILIDAD NO SIGNIFICA UNA SOLA FUNCIÓN

Una clase puede contener varias funciones relacionadas si todas pertenecen al mismo propósito.

Por ejemplo:

`ProductoRepository`

puede contener:

* `obtenerProductos()`
* `obtenerProducto()`
* `guardarProducto()`
* `actualizarProducto()`
* `eliminarProducto()`

Todas pertenecen a:

> **persistencia y acceso de productos**

No es necesario dividirlas artificialmente.

La responsabilidad se evalúa por **propósito**, no por cantidad de funciones.

---

## 5. NO SEPARAR ARTIFICIALMENTE

No crear una nueva clase simplemente porque una parte del código podría técnicamente vivir en otro archivo.

Evitar:

`ProductoReader`

`ProductoWriter`

`ProductoUpdater`

`ProductoDeleter`

si todas forman naturalmente parte de `ProductoRepository`.

La pregunta no es:

> "¿Puedo separar esto?"

La pregunta es:

> **"¿Separarlo hace que el sistema sea más fácil de entender y mantener?"**

Si la respuesta es no, mantenerlo unido.

---

## 6. NAMING POR INTENCIÓN

Los nombres deben explicar **qué representa algo**, no cómo está implementado.

### Bien

`validarStockSuficiente()`

`obtenerVentasDelDia()`

`calcularTotalVenta()`

`guardarProducto()`

### Mal

`processData()`

`handleInput()`

`executeRequest()`

`doStuff()`

`manager()`

Un buen nombre debe permitir entender el propósito sin leer primero toda la implementación.

---

## 7. EVITAR FUNCIONES DIOS

No dividir una función solamente porque es larga.

Dividirla cuando:

* mezcla responsabilidades diferentes;
* contiene una operación con intención independiente;
* una parte puede entenderse claramente por separado;
* la lectura se vuelve difícil;
* existe una frontera lógica real.

Una función de 40 líneas con una responsabilidad clara puede ser mejor que diez funciones artificiales de cuatro líneas.

---

## 8. EVITAR CLASES DIOS

Una clase se convierte en problemática cuando acumula responsabilidades que no pertenecen al mismo propósito.

Ejemplo problemático:

`ProductoViewModel`

que termina manejando:

* estado;
* Firebase;
* imágenes;
* validaciones;
* navegación;
* normalización;
* búsquedas;
* reglas de negocio;
* persistencia;
* mensajes de UI.

No solucionar esto creando automáticamente diez clases.

Primero identificar **qué responsabilidades realmente están mezcladas**.

Después separar únicamente las que necesiten separación.

---

## 9. NO PARCHEAR ARQUITECTURA

Si una solución empieza a necesitar:

* múltiples flags;
* `if` anidados;
* estados temporales;
* condiciones especiales;
* excepciones para casos concretos;
* código duplicado;
* bridges;
* workarounds;

detenerse antes de agregar otro parche.

Preguntar:

> **¿La estructura actual está haciendo que esta funcionalidad sea innecesariamente difícil?**

Si la respuesta es sí:

**cuestionar la estructura y reorganizarla.**

No continuar acumulando complejidad encima de una estructura que ya está fallando.

---

## 10. CAMBIOS LOCALES

Un cambio pequeño debería afectar una cantidad razonable de código.

Si modificar una regla sencilla obliga a tocar:

`UI → ViewModel → Repository → Mapper → Helper → Manager → Service`

detenerse y revisar.

La arquitectura probablemente tiene demasiado acoplamiento o demasiadas capas.

La solución no es necesariamente agregar otra abstracción.

Puede ser **eliminar las existentes**.

---

## 11. UBICACIÓN OBVIA

El código debe ser fácil de encontrar.

Si alguien pregunta:

> "¿Dónde se guarda este producto?"

debe existir un lugar evidente.

Si pregunta:

> "¿Dónde se valida este dato?"

debe existir un lugar evidente.

Si pregunta:

> "¿Dónde se calcula este total?"

debe existir un lugar evidente.

La arquitectura debe reducir la búsqueda mental.

---

## 12. ABSTRACCIÓN CON PROPÓSITO

Toda abstracción debe justificar su existencia.

Antes de crearla responder:

1. ¿Qué problema actual resuelve?
2. ¿Qué responsabilidad está aislando?
3. ¿Hace el código más fácil de entender?
4. ¿Reduce acoplamiento real?
5. ¿Reduce complejidad real?

Si la respuesta es simplemente:

> "porque es más escalable"

o:

> "porque Clean Architecture lo recomienda"

**no crearla.**

---

## 13. REGLA DE MANTENIBILIDAD

El código debe permitir que una persona pueda:

* encontrar una responsabilidad;
* entenderla;
* modificarla;
* probarla;
* saber qué podría verse afectado.

Sin tener que recorrer innecesariamente todo el proyecto.

---

## 14. PRINCIPIO FINAL

No buscar:

**la arquitectura más sofisticada.**

Buscar:

**la arquitectura más simple que mantenga las responsabilidades claras.**

No buscar:

**menos archivos a cualquier precio.**

Buscar:

**menos complejidad mental.**

No buscar:

**preparar todo para el futuro.**

Buscar:

**hacer que el código actual sea fácil de entender y cambiar.**

### Regla definitiva

> **Si puedes resolverlo directamente sin crear una nueva abstracción, hazlo directamente.**
>
> **Si hacerlo directamente empieza a mezclar responsabilidades o aumentar claramente la complejidad, separa.**
>
> **Si separar crea más complejidad de la que elimina, no separes.**

**Clean Code es una herramienta para mantener el orden. No una excusa para construir una arquitectura innecesariamente compleja.**

---

## 15. FUENTE ÚNICA DE VERDAD EN DATOS COMPARTIDOS

Si un dato se lee en más de una pantalla, su lectura vive en un solo sitio. Para inventario: ProductoParser, CodigoBarraHelper y FechaVencimientoHelper en compartido/logica son los 3 maestros. Prohibido copiar getString("codigoBarras") o llaveLote() en otro repo. Un grep con 2 hits es deuda.

---

## 16. MANTENIBILIDAD RADICAL — DELTA (decisión del dueño — 2026-08-25)

> Lo anterior ya cubre: justificación de cada pieza, no anticipar, naming por intención,
> clases dios vs fragmentación artificial, funciones que se dejan describir.
> Esta sección agrega SOLO las reglas que faltaban:

### A. Prohibido el cajón de sastre
Prohibido crear/crecer archivos `Utils.kt`, `Common.kt`, `Misc.kt`, `Extensions.kt` genéricos,
y carpetas-cajón (`util/`) donde caen piezas sin relación. Si una función tiene dueño de
responsabilidad, vive junto a él. Un helper puede llamarse Helper SOLO si su nombre lleva dominio:
`CodigoBarraHelper` ✓ · `ProductUtils` ✗.

### B. Duplicación semántica = duplicación de conocimiento
Dos códigos distintos pueden decir la misma regla de negocio. Antes de crear lógica nueva:
*"¿esto ya existe, aunque se vea diferente?"* Si dos sitios codifican la misma regla, una es la
fuente y el otro la consume. El grep de sospecha es obligatorio antes de escribir.

### C. Rutas Firestore: centralizadas + visibles + legibles
La ruta canónica vive en su objeto `*Paths.kt`. Prohibido escribir
`db.collection("...")` fuera de un Paths salvo infraestructura propia del archivo y comentada.
El tenant debe ser EVIDENTE en la firma: `(db, clienteId, sucursalId)` — jamás escondido en una
cadena difícil de rastrear. Tampoco cinco builders para llegar a "productos".

### D. Estados sin gemelos (ni gemelos con tilde)
Un concepto = un campo. Prohibido coexistencias tipo `isLoading`+`cargando`,
`productsList`+`listaAcumulada`, `ultimoDoc`+`ultimoDocumento`,
`isLoadingMore`+`isNextPageLoading`, `BusquedaVacia`+`BusquedaVacía`.
Los alias "por compatibilidad" son deuda nombrada: se eliminan migrando lectores, no se heredan.

### E. Lectura lineal cuando sea razonable
El flujo principal se lee arriba→abajo: validar → ejecutar → actualizar → responder.
Si entender una operación sencilla exige saltar entre 5+ archivos sin necesidad real,
hay sobrearquitectura: se elimina capas, no se agregan.
