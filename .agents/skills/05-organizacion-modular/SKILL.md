---
name: organizacion-modular
description: Estándar universal de orden y separación de responsabilidades en módulos (UI, Lógica, Datos).
---

# CEREBRO 05: ORGANIZACIÓN MODULAR Y SEPARACIÓN DE RESPONSABILIDADES

> Un módulo mantenible es aquel donde cualquier persona o IA sabe exactamente dónde buscar,
> dónde modificar y cómo agregar una función sin miedo a romper nada.

---

## 1. El Estándar de Excelencia: 3 Capas (Cuando se justifica)

Esta estructura es el destino final de cualquier módulo que, según la **Skill 02**, necesite organización por su complejidad. Si el módulo es simple, se mantiene lo más directo posible. Si requiere separación, se organiza así:

```
modulo/
 ├── ui/
 │    ├── ModuloScreen.kt                # Pantalla principal (Layout general)
 │    └── componentes/                   # Widgets visuales reutilizables
 │         ├── ModuloItemCard.kt
 │         └── ModuloFormularioPanel.kt
 │
 ├── logica/
 │    ├── ModuloViewModel.kt             # Orquestación de eventos y reglas
 │    └── ModuloUiState.kt               # Estado inmutable de la pantalla
 │
 └── datos/
      ├── ModuloModelo.kt                # Entidades y data classes
      └── ModuloRepository.kt            # Consultas Firestore y transacciones
```

---

## 2. Responsabilidad de Cada Capa

### A. Capa de UI (`ui/`):
- **Qué hace:** Dibuja la pantalla, captura eventos del usuario (toques, texto escrito, scrolls) y aplica animaciones.
- **Regla estricta:** Cero lógica de negocio, cero cálculos complejos y **prohibido interactuar con Firebase/Firestore**.
- **Diseño:** Consume siempre los tokens del sistema (`FDColors` / `FDType`, ver `FarmadonDesignSystem`) para garantizar soporte instantáneo de Tema Claro y Tema Oscuro.

### B. Capa de Lógica (`logica/`):
- **Qué hace:** El `ViewModel` recibe los eventos de la UI, valida las entradas del usuario, aplica reglas de negocio (ej. validación de límites de plan) y actualiza el `UiState`.
- **Regla estricta:** No debe contener referencias a clases de UI (como `Context`, `Modifier`, `Color`, etc.). Expone un `StateFlow<UiState>` limpio.

### C. Capa de Datos (`datos/`):
- **Qué hace:** Define la estructura de datos (`data class`) y el `Repository` que gestiona las lecturas en tiempo real (`addSnapshotListener`), escrituras (`set`, `update`) y transacciones atómicas (`runTransaction`).
- **Regla estricta:** Usa `FieldValue.serverTimestamp()` solo en campos de primer nivel
  o submapas directos — **prohibido dentro de arreglos/listas** (`Timestamp.now()` ahí,
  ver R10 del AGENTS.md) — para fechas de creación y actualización.

---

## 3. Clases Pequeñas y Legibles

- **Una sola responsabilidad por clase:** Si un formulario tiene selector de mapa, lista de elementos y cabecera, cada parte vive en su propio archivo en `ui/componentes/`.
- **Prohibido clases monstruo:** No crear archivos de más de 300-400 líneas cuando pueden descomponerse en componentes simples y testeables.
- **Sin código huérfano ni parches:** Si una clase ya no se utiliza o fue reemplazada, se elimina por completo.

---

## 4. Un dato compartido, un solo traductor

Todo repo que toque inventario/{id} usa ProductoParser; todo código de barras usa CodigoBarraHelper; todo lote usa resolverLote(). Si agregas un campo nuevo, lo agregas 1 vez en el helper.
