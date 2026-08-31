package com.app.administradorfarmadon.compras.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonSecundario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDCampoTexto
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDDialogoContenedor
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor
import java.util.Locale

@Composable
fun DialogoCrearProveedor(
    proveedorEditando: Proveedor? = null,
    guardando: Boolean = false,
    onGuardar: (
        nombre: String,
        idFiscal: String,
        contacto: String,
        telefono: String,
        email: String,
        direccion: String,
        montoMinimo: Double
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val colores = TokensFarmadon.colores

    var nombre by remember { mutableStateOf(proveedorEditando?.nombre ?: "") }
    var idFiscal by remember { mutableStateOf(proveedorEditando?.idFiscal ?: "") }
    var telefono by remember { mutableStateOf(proveedorEditando?.telefono ?: "") }
    var email by remember { mutableStateOf(proveedorEditando?.email ?: "") }
    var direccion by remember { mutableStateOf(proveedorEditando?.direccion ?: "") }
    var montoMinimoTexto by remember {
        mutableStateOf(
            if (proveedorEditando != null && proveedorEditando.montoMinimoPedido > 0)
                String.format(Locale.US, "%.2f", proveedorEditando.montoMinimoPedido)
            else ""
        )
    }

    var errorMontoMinimo by remember { mutableStateOf<String?>(null) }

    var menuDominioExpandido by remember { mutableStateOf(false) }
    val dominiosComunes = listOf("@gmail.com", "@outlook.com", "@hotmail.com", "@yahoo.com", "@empresa.com")
    val esEdicion = proveedorEditando != null

    val formularioValido = nombre.trim().isNotBlank() && idFiscal.trim().isNotBlank()

    FDDialogoContenedor(
        titulo = if (esEdicion) "EDITAR PROVEEDOR" else "REGISTRAR NUEVO PROVEEDOR",
        subtitulo = "Los datos se sincronizan con compras, reposición y recepción de facturas",
        iconoCabecera = Icons.Outlined.Business,
        onDismiss = onDismiss,
        bloqueado = guardando
    ) {
        // ── FORMULARIO EN CUADRÍCULA 50% / 50% ESPACIOSA ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // FILA 1 (50% / 50%): Razón Social + RUC / Documento Fiscal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FDCampoTexto(
                    valor = nombre,
                    onValorCambio = { nombre = it },
                    etiqueta = "RAZÓN SOCIAL O DROGUERÍA",
                    placeholder = "",
                    iconoInicio = Icons.Default.Business,
                    esObligatorio = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    habilitado = !guardando,
                    modifier = Modifier.weight(1f)
                )

                FDCampoTexto(
                    valor = idFiscal,
                    onValorCambio = { idFiscal = it },
                    etiqueta = "RUC / DOCUMENTO FISCAL",
                    placeholder = "",
                    iconoInicio = Icons.Default.Badge,
                    esObligatorio = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    habilitado = !guardando,
                    modifier = Modifier.weight(1f)
                )
            }

            // FILA 2 (50% / 50%): Teléfono/WhatsApp + Correo con Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FDCampoTexto(
                    valor = telefono,
                    onValorCambio = { telefono = it },
                    etiqueta = "TELÉFONO / WHATSAPP (OPCIONAL)",
                    placeholder = "",
                    iconoInicio = Icons.Default.Phone,
                    iconoInicioColor = if (telefono.isNotBlank()) Color(0xFF25D366) else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    habilitado = !guardando,
                    modifier = Modifier.weight(1f)
                )

                // Correo con Selector Desplegable integrado
                Box(modifier = Modifier.weight(1f)) {
                    FDCampoTexto(
                        valor = email,
                        onValorCambio = { email = it },
                        etiqueta = "CORREO ELECTRÓNICO (OPCIONAL)",
                        placeholder = "",
                        iconoInicio = Icons.Outlined.AlternateEmail,
                        iconoFin = {
                            IconButton(onClick = { menuDominioExpandido = true }) {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Elegir dominio",
                                    tint = colores.botonPrimarioFondo,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        habilitado = !guardando,
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = menuDominioExpandido,
                        onDismissRequest = { menuDominioExpandido = false },
                        modifier = Modifier.background(colores.cardElevada)
                    ) {
                        dominiosComunes.forEach { dominio ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = dominio,
                                        style = TokensFarmadon.tipografia.cuerpo.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = colores.textoPrincipal
                                    )
                                },
                                onClick = {
                                    val usuarioParte = if (email.contains("@")) {
                                        email.substringBefore("@")
                                    } else {
                                        email.trim()
                                    }
                                    email = if (usuarioParte.isNotBlank()) "$usuarioParte$dominio" else dominio
                                    menuDominioExpandido = false
                                }
                            )
                        }
                    }
                }
            }

            // FILA 3 (50% / 50%): Dirección Fiscal + Pedido Mínimo con Guía
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FDCampoTexto(
                    valor = direccion,
                    onValorCambio = { direccion = it },
                    etiqueta = "DIRECCIÓN FISCAL / ALMACÉN (OPCIONAL)",
                    placeholder = "Ciudad, avenida o sede de despacho",
                    iconoInicio = Icons.Default.LocationOn,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    habilitado = !guardando,
                    modifier = Modifier.weight(1f)
                )

                FDCampoTexto(
                    valor = montoMinimoTexto,
                    onValorCambio = { nuevo ->
                        // Teclado decimal peruano: la coma es separador válido; letras no.
                        montoMinimoTexto = nuevo.filter { it.isDigit() || it == '.' || it == ',' }
                        errorMontoMinimo = null
                    },
                    etiqueta = "PEDIDO MÍNIMO DE DESPACHO ($) (OPCIONAL)",
                    placeholder = "0.00",
                    iconoInicio = Icons.Default.ShoppingBag,
                    textoError = errorMontoMinimo,
                    textoAyuda = if (errorMontoMinimo == null) "ℹ️ Monto mínimo exigido por la droguería para despachar. Si los pedidos alcanzan este valor, el sistema confirmará que ya puedes pedir." else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    habilitado = !guardando,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = colores.divisor.copy(alpha = 0.5f))
        Spacer(Modifier.height(20.dp))

        // ── BOTONES DE ACCIÓN SIMÉTRICOS (50% / 50%) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FDBotonSecundario(
                texto = "CANCELAR",
                onClick = onDismiss,
                habilitado = !guardando,
                modifier = Modifier.weight(1f)
            )

            FDBotonPrimario(
                texto = if (esEdicion) "ACTUALIZAR PROVEEDOR" else "GUARDAR PROVEEDOR",
                icono = Icons.Default.Check,
                onClick = {
                    // La coma decimal peruana se acepta y se normaliza. Un texto no
                    // numérico JAMÁS se convierte en 0 a escondidas: se bloquea aquí.
                    val textoMonto = montoMinimoTexto.trim()
                    val montoDouble = if (textoMonto.isBlank()) 0.0
                        else textoMonto.replace(',', '.').toDoubleOrNull() ?: -1.0
                    if (montoDouble < 0.0) {
                        errorMontoMinimo = "Escribe un monto válido (ej. 0.00 o 50.00). No se guardó nada."
                    } else {
                        errorMontoMinimo = null
                        onGuardar(
                            nombre.trim(),
                            idFiscal.trim(),
                            "",
                            telefono.trim(),
                            email.trim(),
                            direccion.trim(),
                            montoDouble
                        )
                    }
                },
                habilitado = !guardando && formularioValido,
                cargando = guardando,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
