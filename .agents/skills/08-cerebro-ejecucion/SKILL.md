---
name: cerebro-ejecucion
description: ¿Está terminado y verificado? Cierre limpio, evidencia real, cero alucinaciones, cero entregas a medias.
---

# CEREBRO 08: EJECUCIÓN (TERMINAR BIEN Y DEMOSTRAR)

> Un cambio no está terminado hasta que compila, corre y el usuario no ve ningún error.
> "Debería funcionar" no existe como evidencia.

---

## 1. Inspección ANTES de actuar (Cero Alucinaciones)

- **Antes de diagnosticar:** Leer el archivo real. Nunca asumir qué hay en él.
- **Antes de cambiar:** Verificar que la línea a cambiar existe exactamente como se cree.
- **Antes de declarar que algo "está bien":** Correr la compilación y leer el resultado.
- Si no se inspeccionó el código real → no hay diagnóstico válido → no se habla.

---

## 2. Verificación Obligatoria después de cada cambio

### A. Compilación siempre
Después de cualquier modificación de código en BrixoPanel o Farmadon:
```
.\gradlew.bat :app:compileDebugKotlin
```
- `BUILD SUCCESSFUL` → el cambio es válido.
- `BUILD FAILED` → se lee el error, se corrige, se vuelve a compilar. Sin excepción.

### B. Buscar otros afectados
Si se cambia un constructor, una firma de función o un nombre de clase, buscar
con `grep_search` o `Get-ChildItem | Select-String` qué otros archivos lo usan.
Nunca asumir que solo hay un llamador.

### C. No declarar éxito parcial como total
- Si compiló pero no se probó en el dispositivo, decirlo explícitamente.
- Si se corrigió el crash reportado pero quedan otros sin verificar, decirlo.
- Prohibido decir "listo" cuando hay partes sin verificar.

---

## 3. Cierre en 3 líneas (sin jerga)

1. **Qué cambió:** En palabras de farmacia, no de código. Con boceto ASCII si hay pantalla.
2. **Por qué es el mínimo limpio:** Sin parches, sin complejidad inventada.
3. **Evidencia:** Log real de compilación o test. Nunca "debería funcionar".

---

## 4. Score de sesión honesto

Cerrar siempre con:
```
Sesión X/10 — [qué se logró]. Lección: [qué aprendimos para no repetir].
```
- Si algo quedó sin verificar → se descuenta puntaje y se informa.
- Si hubo un error previo (como declarar que algo estaba bien y no lo estaba) → se descuenta y se reconoce.
- El score no se maquilla. Es la herramienta de confianza del socio.

---

## 5. Checklist de Cierre antes de reportar al usuario

- [ ] ¿Se compiló y fue `BUILD SUCCESSFUL`?
- [ ] ¿Se buscaron otros archivos que usen lo que se cambió?
- [ ] ¿El mensaje de cierre está en lenguaje de farmacia, sin jerga?
- [ ] ¿Se reportó qué quedó sin probar (si algo quedó)?
- [ ] ¿Se incluyó el score honesto?
