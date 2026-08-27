---
name: firebase-android
description: >-
  Uso de Firebase en Farmadon (Android Kotlin + Jetpack Compose). Consolida: CLI/login/
  config (firebase-basics), Cloud Firestore (nuestra base de datos), Authentication,
  Crashlytics y Remote Config. Regla de seguridad: cero paranoia anti-hacker, solo
  aislamiento por farmacia (R1), y ninguna regla/permiso nuevo sin aprobación explícita
  del dueño. firebase-security-rules-auditor es solo referencia bajo pedido.
---

# FIREBASE-ANDROID (Farmadon = Android Kotlin + Firestore)

> Una sola skill para todo Firebase que usamos en Android. No web, no SQL, no iOS.

---

## 1. BASE OFICIAL

- Proyecto Android nativo Kotlin + Jetpack Compose, desarrollado en **Windows**.
- Compilar SIEMPRE con: `.\gradlew.bat :app:compileDebugKotlin` (ver `gradle-windows-execution`).
- Base de datos: **Cloud Firestore**. Auth: **Firebase Authentication**. Crash: **Crashlytics**.
  Config remota: **Remote Config** (si se usa).

---

## 2. FIRESTORE (datos)

- Modelado: una farmacia = un tenant. TODO documento operativo lleva `farmaciaId` y toda
  consulta/escritura lo verifica (R1, Límite Infranqueable 1).
- Tiempo real: las vistas operativas usan `addSnapshotListener` / `callbackFlow` (R8).
  Prohibidas fotos estáticas `.get()` que congelen la pantalla.
- Fechas: `FieldValue.serverTimestamp()` según **R10 del AGENTS.md** — solo campos
  de primer nivel o submapas directos, nunca dentro de arreglos/listas (ahí va
  `Timestamp.now()`).
- Concurrencia: Firebase decide la conectividad; prohibido asumir "sin conexión" antes de
  tiempo (R11). Operaciones multi-dato se guardan completas o no (R1/R3).

---

## 3. AUTHENTICATION

- Login de personal de la farmacia. El token de auth define a qué farmacia pertenece el
  usuario; esa pertenencia es la fuente de `farmaciaId` para aislar datos (R1).
- Prohibido agregar verificaciones anti-hacker especulativas (R2).

---

## 4. CRASHLYTICS

- Reporta crashes reales. Filtrar el stack por el paquete propio
  (`com.app.administradorfarmadon`); el ruido de sistema/emulador no tumba la app.
- Crash real = `FATAL EXCEPTION: main` + `Caused by` propio.

---

## 5. REMOTE CONFIG (si aplica)

- Flags de configuración. Usar `fetchAndActivate()` y defaults en app. No es la fuente de
  verdad del negocio (esa es Firestore).

---

## 6. REGLA DE SEGURIDAD (decisión del dueño — 2026-08-21)

- **Cero paranoia anti-hacker:** prohibido candados, límites de intentos, verificación de
  identidad "por si hackean". Ese escenario NO es objetivo (R2).
- **Solo lo esencial:** el único requisito es aislamiento por farmacia (R1) en Firestore
  Security Rules — verificar `farmaciaId` contra el token.
- **Ninguna regla/permiso nuevo sin tu aprobación explícita.** La IA propone, tú apruebas.
- `firebase-security-rules-auditor`: referencia técnica **solo bajo pedido explícito**,
  jamás por defecto ni dictando diseño.
