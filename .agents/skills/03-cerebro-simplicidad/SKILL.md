---
name: cerebro-simplicidad
description: ¿Lo entiende y usa el personal de la farmacia sin manual? Fácil, directo, sin fricción.
---

# CEREBRO 03: SIMPLICIDAD (QUE NO SEA MÁS TRABAJO QUE PAPEL)

> Lo simple es más difícil de romper, más fácil de mantener y más fácil de entender.
> Elegir lo complejo cuando existe lo simple es una mala decisión de diseño.

---

## 1. El filtro de las 2 Opciones (OBLIGATORIO antes de cualquier cambio)

Antes de modificar código, evaluar:
- **Opción A (La más simple):** La menor cantidad de cambios limpios que resuelve el problema hoy.
- **Opción B (La alternativa compleja descartada):** Lo que evitamos explícitamente y por qué.

Si no se puede articular Opción A vs Opción B → no se empieza a escribir código.

### Ejemplos reales de este proyecto:
| Problema | Opción Compleja (DESCARTADA) | Opción Simple (CORRECTA) |
|---|---|---|
| ViewModel no instancia | Crear Factory personalizada | Mover repo al `init` del ViewModel |
| Crash en Context | Pasar Activity por parámetro | Usar `viewModel()` estándar de Compose |
| Lectura lenta de Firestore (pantalla NO operativa, ej: reporte) | Cache manual + sync custom | `.get()` estándar (usa cache automático). En vistas operativas NUNCA `.get()`: es `addSnapshotListener` (R8) |
| Estado de login al salir | Limpiar en DisposableEffect anidado | `limpiarEstado()` en `SessionState.LoggedOut` |

---

## 2. La regla del usuario

Si el personal de la farmacia lo hacía en papel, el software debe costar igual o menos esfuerzo.
La pregunta siempre es: ¿dónde está el botón y qué pasa después de tocarlo?

---

## 3. Menos es más

- Una pantalla hace una cosa clara.
- Si un cambio agrega pasos o complejidad técnica, es un problema, no una mejora.
- Prohibido agregar capas "por si acaso" o abstracciones especulativas.
- Si hay 3 líneas de código que hacen lo mismo que 30, usar las 3.

---

## 4. Estados honestos (sin fingir)

El software nunca engaña ni oculta fallas. Los estados posibles son:
- `Cargando` → spinner visible, botón deshabilitado.
- `Listo` → datos reales en pantalla, botón habilitado.
- `Error` → mensaje en español plano + siguiente paso claro para el usuario.

Si algo no se guardó completo, se dice y se permite reintentar.
Prohibido mostrar "Guardado ✓" sin haber verificado el `.await()` de Firestore.
