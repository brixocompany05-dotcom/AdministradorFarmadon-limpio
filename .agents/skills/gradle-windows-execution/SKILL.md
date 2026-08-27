---
name: gradle-windows-execution
description: Protocolo de alta eficiencia para compilar y diagnosticar errores de compilación Kotlin en Windows sin bloqueos de consola ni sobrecarga de Gradle.
---

# Skill: Fast & Non-Blocking Gradle Diagnostics (Windows/PowerShell)

> **REGLA OBLIGATORIA EN ESTE REPO (Farmadon y BrixoPanel).**
> Se aplica SIEMPRE que se ejecute Gradle (`gradlew.bat`). Nunca colgar la consola.

## Contexto y Problemática

1. **Bloqueo de consola (Hang):** Canalizar (`|`) la salida en vivo de `gradlew.bat` hacia
   `Select-String` retiene los descriptores STDOUT del Gradle Daemon, dejando la terminal
   colgada indefinidamente tras un error.

2. **Sobrecarga de trabajo:** Ejecutar tareas globales o sin restricciones fuerza
   re-evaluaciones de tareas secundarias (ksp, dataBinding, linter, dexing) que consumen
   memoria y tiempo innecesario cuando solo se busca validar sintaxis Kotlin.

**Prohibido** escribir en la terminal:

```powershell
# ❌ PROHIBIDO — puede bloquear el demonio y colgar la sesión
.\gradlew.bat :app:compileDebugKotlin 2>&1 | Select-String -Pattern "e:"
.\gradlew.bat :app:compileDebugKotlin | Select-Object -First 20
.\gradlew.bat build          # ❌ sobrecarga: ejecuta todo el pipeline innecesario
```

## Directivas de Ejecución

### 0. Reglas del Entorno (fusionadas desde entorno-compilacion-windows)

La compilación oficial SIEMPRE se ejecuta desde **Windows**:

```
.\gradlew.bat :app:compileDebugKotlin
```

**NO utilizar:** WSL · `/mnt/c` · `/tmp/jdk21` · `/tmp/android-sdk-linux` · Linux SDK ·
shells Linux · contenedores · **subagentes para compilar** · entornos temporales ·
instalaciones temporales de Java o Android SDK.

* El proyecto ya tiene SDK y JDK configurados en Windows. **NO crear ni descargar otro entorno.**
* No ejecutar `clean`, `assemble`, `build` completo ni suites de pruebas completas salvo necesidad real.
* NO descargar herramientas/SDKs/JDKs/Gradle/plugins/dependencias nuevas salvo que el código las exija;
  antes de agregar una dependencia comprobar si existe una equivalente o si se resuelve con las actuales.
* NO ejecutar `--offline` si falta una dependencia requerida. NO cambiar configuración del SDK para ocultar un error.
* Si falla la compilación: leer el error real → identificar archivo/línea → corregir solo el problema → recompilar con este protocolo. **NO TOCAR EL ENTORNO** para disfrazar errores de código.
* Si falta una herramienta necesaria: DETENERSE y pedir autorización, jamás instalarla automáticamente.

### 1. Comando Rápido — La Forma Más Fácil y Rápida

Protocolo recomendado para casi todos los casos diarios:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain --offline
```

* `compileDebugKotlin` — solo sintaxis Kotlin, nada más (nada de ksp/dexing/lint).
* `--console=plain` — **elimina el progress bar animado** que "cuelga" la consola en
  PowerShell; salida limpia, inmediata, no bloquea el demonio.
* `--offline` — usa **solo el cache local**, cancelando recompilaciones lentas por
  re-descarga de dependencias o re-evaluación de tareas secundarias.
* Resultado visual directo: `BUILD SUCCESSFUL` o `e: ...` (errores de Kotlin).

Salida directa en consola, **sin `build.log`**, sin `Select-String`, sin `$LASTEXITCODE`
(confundido por warnings del JVM daemon). Veredicto: lee el texto impreso.

Ejemplo de salida limpia:
```
> Task :app:compileDebugKotlin UP-TO-DATE
BUILD SUCCESSFUL in 1s
```

Veredicto rápido: si ves `BUILD SUCCESSFUL` → OK. Si ves `e:` → hay errores; corrígelos y repite.

### 2. Comando Alternativo — Con `build.log` (diagnóstico profundo)

Solo cuando necesites filtrar patrones específicos o builds muy verbosos (`--info`):

```powershell
.\gradlew.bat :app:compileDebugKotlin --build-cache --parallel --quiet > build.log 2>&1; Select-String -Path build.log -Pattern "e:" | Select-Object -First 20
```

* `:app:compileDebugKotlin` — solo sintaxis Kotlin, nada más.
* `--build-cache` — reusa salidas previas entre builds.
* `--parallel` — tareas internas concurrentes donde es seguro.
* `--quiet` — menos ruido; solo errores y warnings relevantes.
* Salida a `build.log` **antes** de filtrar — jamás streamear al demonio.

### 3. Verificación de Éxito/Fallo (modo rápido)

Con `--console=plain`, el BUILD message aparece directamente. Busca `BUILD SUCCESSFUL` o
`BUILD FAILED` en la consola. Para builds previos (`UP-TO-DATE`) no se vuelve a compilar.

### 4. Verificación de Éxito/Fallo (modo build.log)

El resultado autorizado es el TEXTO del log, nunca asumir por memoria:

```powershell
Get-Content build.log | Select-String -Pattern "BUILD SUCCESSFUL|BUILD FAILED"
```

### 5. Lectura de Errores de Compilación (líneas Kotlin `e:`)

```powershell
Select-String -Path build.log -Pattern "^e:" | Select-Object -First 20
```

### 6. Alcance

Aplica a TODAS las tareas Gradle (compile, test, lint, assemble, bundle). Para diagnóstico
de sintaxis usar siempre la variante del punto 1 — no `build` completo.

### Limpieza Obligatoria

El log temporal debe eliminarse al terminar para no ensuciar el workspace:

```powershell
Remove-Item build.log -ErrorAction SilentlyContinue
```

### Condiciones de Contorno

* **Modo rápido (comando #1, `--console=plain --offline`):** el veredicto es el
  texto impreso directamente: `BUILD SUCCESSFUL` o `e: ...`. No se usa `$LASTEXITCODE`
  ni `build.log`; los warnings del JVM daemon aparecen en la consola pero no invalidan
  el build (ignorar `WARNING: ... System::load`).
* **Modo build.log (comando #2, `--quiet`):** Gradle suprime `BUILD SUCCESSFUL`.
  Veredicto: si `Select-String -Pattern "^e:"` sobre `build.log` NO devuelve líneas de
  error, la compilación es válida; para veredicto formal, repetir sin `--quiet`.
  No usar `$LASTEXITCODE` como única fuente de verdad en este modo: ciertos warnings
  del daemon JVM pueden acompañar builds válidos; el texto impreso (`BUILD SUCCESSFUL`
  / `BUILD FAILED`) es el criterio final, o la ausencia de errores `^e:` bajo `--quiet`.
* Anclar el patrón de error con `^e:` cuando sea posible — el patrón suelto `"e:"`
  puede producir falsos positivos con warnings del daemon JVM (ej. rutas con
  `...native-platform...jar`).
* Si el log contiene errores, leer las líneas `e:` ANTES de volver a compilar:
  iterar corrección → recompilar con este mismo protocolo.
* En diagnósticos largos (`--info` o `--debug`), redirigir siempre a archivo y filtrar
  después; jamás streamear en vivo hacia la consola.

## Cómo se invoca

Esta skill se aplica automáticamente ante cualquier petición de compilación/verificación
Gradle en los repos Farmadon (`AdministradorFarmadon-limpio-main`) y BrixoPanel
(`BrixoPanelAdministrativo`). No requiere invocación explícita; su incumplimiento es una
violación de reglas de trabajo al mismo nivel que R1–R13 de AGENTS.md.
