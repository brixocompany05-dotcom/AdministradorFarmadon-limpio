package com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.ui

import android.app.Activity
import android.content.ContextWrapper
import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.datos.TecladoPrefs

private const val TAG = "BloqueoTeclado"

/**
 * Regla "Bloquear Teclado" aplicada por ventana.
 *
 * Se coloca en la raíz de la ventana principal y en la raíz de cada diálogo con campos
 * de texto. Cuando la preferencia está activa se aplican capas a NIVEL DE VISTA DEL
 * SISTEMA (no dependen del estado de Compose) para que ningún input abra el teclado
 * del dispositivo, sin quitar el foco (la pistola física sigue escribiendo en el campo):
 *  1. La ventana deja de ser objetivo del teclado (FLAG_ALT_FOCUSABLE_IM).
 *  2. El modo de ventana pide "teclado siempre oculto" (SOFT_INPUT_STATE_ALWAYS_HIDDEN).
 *  3. Vigía de layout de la vista raíz: cada vez que la pantalla se reorganiza (lo que
 *     ocurre cuando el teclado intenta aparecer), se ordena ocultarlo al instante por
 *     las dos vías del sistema (método de entrada y control de insets).
 *  4. Vigía de foco: cuando un campo gana el foco, se oculta en el mismo fotograma.
 *  5. Vigía de foco de ventana: al volver a la app con el switch activo, se oculta.
 * La regla es por ventana para que ningún diálogo (que crea su propia ventana) se la salte.
 */
@Composable
fun AplicarBloqueoTecladoVentana() {
    val bloquear by TecladoPrefs.bloquearTecladoFlow.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current

    SideEffect {
        val ventana = view.obtenerVentana() ?: (context as? Activity)?.window
        if (ventana == null) {
            android.util.Log.w(TAG, "No se encontró la ventana (bloquear=$bloquear)")
            return@SideEffect
        }
        if (bloquear) {
            ventana.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            ventana.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
            android.util.Log.d(
                TAG,
                "Bloqueo aplicado a ventana: flag=${(ventana.attributes.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM) != 0}"
            )
            ocultarTeclado(ventana, view, context)
        } else {
            ventana.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            ventana.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            android.util.Log.d(TAG, "Bloqueo retirado de ventana")
        }
    }

    // Vigías a nivel de vista del sistema: funcionan sin depender de si Compose
    // se entera del teclado. Si el sistema lo muestra, se oculta de inmediato.
    DisposableEffect(bloquear, view) {
        val ventana = view.obtenerVentana() ?: (context as? Activity)?.window
        val decor = ventana?.decorView
        if (decor == null) {
            return@DisposableEffect onDispose {}
        }

        val observer = decor.viewTreeObserver

        // El teclado, al aparecer, cambia los insets y la vista se reorganiza:
        // aquí se cierra en el mismo ciclo, antes de que dibuje.
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (bloquear) ocultarTeclado(ventana, view, context)
        }

        // Un campo ganó el foco: se cancela la apertura en el mismo fotograma.
        val focusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, _ ->
            if (bloquear) {
                view.post { ocultarTeclado(ventana, view, context) }
            }
        }

        // La ventana recupera el foco con el switch activo: nada de teclado heredado.
        val windowFocusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus && bloquear) ocultarTeclado(ventana, view, context)
        }

        observer.addOnGlobalLayoutListener(layoutListener)
        observer.addOnGlobalFocusChangeListener(focusListener)
        observer.addOnWindowFocusChangeListener(windowFocusListener)

        onDispose {
            observer.removeOnGlobalLayoutListener(layoutListener)
            observer.removeOnGlobalFocusChangeListener(focusListener)
            observer.removeOnWindowFocusChangeListener(windowFocusListener)
        }
    }
}

private fun ocultarTeclado(ventana: Window, view: View, context: Context) {
    ventana.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(view.windowToken, 0)
    WindowCompat.getInsetsController(ventana, view).hide(WindowInsetsCompat.Type.ime())
}

private tailrec fun View.obtenerVentana(): Window? {
    if (this is DialogWindowProvider) return window
    val padre = parent
    if (padre is View) return padre.obtenerVentana()

    // Fallback robusto: el contexto puede estar envuelto (ContextThemeWrapper) y
    // no ser la Activity directamente; se desenvuelve hasta encontrarla.
    (context as? Activity)?.window?.let { return it }
    var ctx: Context? = context
    while (ctx is ContextWrapper) {
        ctx = ctx.baseContext
        if (ctx is Activity) return ctx.window
    }
    return null
}
