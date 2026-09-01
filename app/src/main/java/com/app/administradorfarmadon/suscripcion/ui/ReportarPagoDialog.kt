package com.app.administradorfarmadon.suscripcion.ui
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.ui.AplicarBloqueoTecladoVentana
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.configuracion.plan.datos.BrixoCanalesPagoInfo
import com.app.administradorfarmadon.configuracion.plan.datos.MetodoPagoBrixoItem
import com.app.administradorfarmadon.configuracion.plan.datos.PlanFacturacionInfo
import com.app.administradorfarmadon.configuracion.plan.datos.PlanFacturacionPaths
import com.app.administradorfarmadon.configuracion.plan.datos.PlanFacturacionRepository
import com.app.administradorfarmadon.suscripcion.ComprobanteCompartidoData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun ReportarPagoDialog(
    dataCompartida: ComprobanteCompartidoData?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember { PlanFacturacionRepository() }

    val clienteId = SessionManager.clienteIdGarantizado

    // Cuentas e Información Real desde Firestore (brixo_configuracion/empresa)
    val canalesPago by repo.observarCanalesPagoBrixo().collectAsState(initial = BrixoCanalesPagoInfo())
    // Precio y Vigencia Real de la Suscripción de la Farmacia
    val planInfo by repo.observarPlanInfo(clienteId).collectAsState(initial = PlanFacturacionInfo())
    // íšltima solicitud de pago enviada (para detectar observaciones de BRIXO en tiempo real)
    val ultimaSolicitud by repo.observarUltimaSolicitudPago(clienteId).collectAsState(initial = null)

    val estaObservada = ultimaSolicitud != null && (
        ultimaSolicitud?.estado.equals("OBSERVADO", ignoreCase = true) ||
        ultimaSolicitud?.estado.equals("OBSERVADA", ignoreCase = true) ||
        ultimaSolicitud?.estado.equals("RECHAZADO", ignoreCase = true) ||
        ultimaSolicitud?.estado.equals("RECHAZADA", ignoreCase = true)
    )

    var imagenUri by remember { mutableStateOf<Uri?>(dataCompartida?.imageUri) }
    var bancoSeleccionado by remember { mutableStateOf("") }
    var montoTexto by remember { mutableStateOf("") }
    var numeroOperacion by remember { mutableStateOf("") }
    var notaAclaratoria by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var exitoEnvio by remember { mutableStateOf(false) }

    // Pre-llenar datos si la solicitud fue observada por BRIXO (Cero pegamento humano)
    LaunchedEffect(ultimaSolicitud) {
        val sol = ultimaSolicitud
        if (sol != null && (
            sol.estado.equals("OBSERVADO", ignoreCase = true) ||
            sol.estado.equals("OBSERVADA", ignoreCase = true) ||
            sol.estado.equals("RECHAZADO", ignoreCase = true) ||
            sol.estado.equals("RECHAZADA", ignoreCase = true)
        )) {
            if (montoTexto.isBlank() && sol.montoReportado > 0.0) {
                montoTexto = "%.2f".format(sol.montoReportado)
            }
            if (numeroOperacion.isBlank() && sol.numeroOperacion.isNotBlank()) {
                numeroOperacion = sol.numeroOperacion
            }
            if (bancoSeleccionado.isBlank() && sol.banco.isNotBlank()) {
                bancoSeleccionado = sol.banco
            }
            if (notaAclaratoria.isBlank() && sol.notaAclaratoriaCliente.isNotBlank()) {
                notaAclaratoria = sol.notaAclaratoriaCliente
            }
        }
    }

    // Pre-llenar monto real del contrato de la farmacia cuando cargue Firestore
    LaunchedEffect(planInfo.precioMensual, planInfo.precioProximaRenovacion) {
        if (montoTexto.isBlank()) {
            val precio = planInfo.precioProximaRenovacion ?: planInfo.precioMensual
            if (precio > 0.0) {
                montoTexto = "%.2f".format(precio)
            }
        }
    }

    // Auto-seleccionar primer banco real publicado por BRIXO
    LaunchedEffect(canalesPago.metodosActivos) {
        if (bancoSeleccionado.isBlank() && canalesPago.metodosActivos.isNotEmpty()) {
            bancoSeleccionado = canalesPago.metodosActivos.first().bancoNombre
        }
    }

    // Auto-extraer número de operación si vino en el texto compartido del banco
    LaunchedEffect(dataCompartida?.textContent) {
        val texto = dataCompartida?.textContent ?: ""
        if (texto.isNotBlank()) {
            val matchOp = Regex("""(?:op|operaci[oó]n|nro|c[oó]digo)[\s:]*([0-9A-Za-z]+)""", RegexOption.IGNORE_CASE).find(texto)
            if (matchOp != null) {
                numeroOperacion = matchOp.groupValues[1]
            }
            canalesPago.metodosActivos.forEach { metodo ->
                if (texto.contains(metodo.bancoNombre, ignoreCase = true)) {
                    bancoSeleccionado = metodo.bancoNombre
                }
            }
        }
    }

    val launcherImagen = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) imagenUri = uri
    }


    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Regla "Bloquear Teclado": este diálogo tampoco abre el teclado si está activada.
        AplicarBloqueoTecladoVentana()
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.92f)
                .padding(16.dp)
        ) {
            if (exitoEnvio) {
                // Estado de Éxito
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FDColors.Success,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "¡Constancia de Pago Enviada a BRIXO!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = FDColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "El equipo de finanzas de BRIXO validará tu comprobante en breve. En cuanto se confirme, tu suscripción se renovará automáticamente sin interrupción.",
                        fontSize = 13.sp,
                        color = FDColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text("ENTENDIDO", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Cabecera
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = if (estaObservada) Icons.Default.WarningAmber else Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (estaObservada) Color(0xFFF59E0B) else Color(0xFF3B82F6),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (estaObservada) "Subsanar Comprobante Observado por BRIXO" else "Reportar Pago de Mensualidad a BRIXO",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = if (estaObservada) "Revisa la observación del agente y reenvía tu constancia corregida" else if (planInfo.planNombre.isNotBlank()) "Plan actual: ${planInfo.planNombre.uppercase()}" else "Envío oficial de constancia de servicio",
                                    fontSize = 11.5.sp,
                                    color = if (estaObservada) Color(0xFFF59E0B) else Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, enabled = !isLoading) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFF94A3B8))
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2A2D3A), thickness = 0.5.dp)

                    if (estaObservada && !ultimaSolicitud?.mensajeBrixo.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "OBSERVACIÓN DEL AGENTE DE BRIXO:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFF59E0B)
                                    )
                                    Text(
                                        text = ultimaSolicitud?.mensajeBrixo ?: "",
                                        fontSize = 12.5.sp,
                                        color = Color.White,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    if (!errorMensaje.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMensaje!!,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Contenido en 2 Columnas
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // COLUMNA IZQUIERDA: Comprobante Visual (Weight 1f)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "COMPROBANTE DE PAGO (CAPTURA O FOTO)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F1117))
                                    .border(BorderStroke(1.dp, Color(0xFF2A2D3A)), RoundedCornerShape(12.dp))
                                    .clickable { launcherImagen.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (imagenUri != null) {
                                    AsyncImage(
                                        model = imagenUri,
                                        contentDescription = "Voucher de pago",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
                                        Text("Toca para adjuntar voucher", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }

                        // COLUMNA DERECHA: Cuentas Reales de BRIXO y Formulario (Weight 1.3f)
                        Column(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Selector dinámico de Bancos reales configurados por BRIXO (Cero bancos inventados)
                            val bancosDisponibles = canalesPago.metodosActivos.map { it.bancoNombre }.filter { it.isNotBlank() }.distinct()
                            Text(text = "CANAL DE PAGO UTILIZADO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            if (bancosDisponibles.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    bancosDisponibles.forEach { banco ->
                                        val isSelected = bancoSeleccionado.equals(banco, ignoreCase = true)
                                        Surface(
                                            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E212B),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFF2A2D3A)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clickable { bancoSeleccionado = banco }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = banco,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = bancoSeleccionado,
                                    onValueChange = { bancoSeleccionado = it },
                                    label = { Text("Banco o medio de pago") },
                                    placeholder = { Text("Ej: Transferencia Bancaria / Depósito") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF3B82F6),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF3B82F6),
                                        unfocusedLabelColor = Color(0xFF94A3B8)
                                    )
                                )
                            }

                            // Monto y Número de Operación
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = montoTexto,
                                    onValueChange = { montoTexto = it },
                                    label = { Text("Monto pagado (S/)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF3B82F6),
                                        unfocusedBorderColor = Color(0xFF2A2D3A)
                                    )
                                )

                                OutlinedTextField(
                                    value = numeroOperacion,
                                    onValueChange = { numeroOperacion = it },
                                    label = { Text("Nº de Operación") },
                                    placeholder = { Text("Código de voucher") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = if (estaObservada) Color(0xFFF59E0B) else Color(0xFF3B82F6),
                                        unfocusedBorderColor = Color(0xFF2A2D3A)
                                    )
                                )
                            }

                            // Campo de Aclaración / Descargo para BRIXO (Cuestionable / Toma y Dame)
                            OutlinedTextField(
                                value = notaAclaratoria,
                                onValueChange = { notaAclaratoria = it },
                                label = { Text(if (estaObservada) "Aclaración o descargo para BRIXO *" else "Nota o aclaración para BRIXO (Opcional)") },
                                placeholder = { Text(if (estaObservada) "Explica la corrección (ej: se adjunta nuevo voucher nítido del BCP)..." else "Detalles adicionales sobre tu pago...") },
                                singleLine = false,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = if (estaObservada) Color(0xFFF59E0B) else Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color(0xFF2A2D3A)
                                )
                            )

                            // Cuentas Bancarias Reales Publicadas por BRIXO
                            Text(
                                text = "CUENTAS BANCARIAS OFICIALES DE BRIXO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )

                            if (canalesPago.metodosActivos.isEmpty()) {
                                Surface(
                                    color = Color(0xFF0F1117),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF2A2D3A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (canalesPago.razonSocial.isNotBlank()) "BRIXO: ${canalesPago.razonSocial}" else "BRIXO SOLUTIONS",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (canalesPago.ruc.isNotBlank()) {
                                            Text(text = "RUC: ${canalesPago.ruc}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                        }
                                        if (canalesPago.whatsappCobranzas.isNotBlank()) {
                                            Text(text = "WhatsApp Cobranzas: ${canalesPago.whatsappCobranzas}", fontSize = 11.sp, color = Color(0xFF3B82F6))
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    canalesPago.metodosActivos.forEach { metodo ->
                                        Surface(
                                            color = Color(0xFF0F1117),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF2A2D3A)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                                                        Text(
                                                            text = "${metodo.bancoNombre} (${if (metodo.moneda == "PEN") "Soles" else "USD"})",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = " ·  ${metodo.tipoCuenta}",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                    Text(
                                                        text = "Cta: ${metodo.numeroCuenta}",
                                                        fontSize = 11.5.sp,
                                                        color = Color(0xFFCBD5E1),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    if (metodo.numeroCci.isNotBlank()) {
                                                        Text(
                                                            text = "CCI: ${metodo.numeroCci}",
                                                            fontSize = 10.5.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                        val limpio = metodo.numeroCuenta.replace(" ", "").replace("-", "")
                                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Cuenta ${metodo.bancoNombre}", limpio))
                                                        Toast.makeText(context, " Cuenta ${metodo.bancoNombre} copiada", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Botones Inferiores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isLoading) {
                            Text("CANCELAR", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                val monto = montoTexto.toDoubleOrNull() ?: 0.0
                                if (monto <= 0.0) {
                                    errorMensaje = "Ingresa un monto válido."
                                    return@Button
                                }
                                if (numeroOperacion.isBlank() && imagenUri == null && (ultimaSolicitud?.comprobanteUrl.isNullOrBlank())) {
                                    errorMensaje = "Ingresa el Nº de operación o adjunta la captura del comprobante."
                                    return@Button
                                }
                                // UNA CONSTANCIA EN CURSO POR VECES: si ya existe una
                                // PENDIENTE o SUBSANADA propia, no se crea otra —” evita
                                // apilar recibos que luego nadie puede aprobar sin
                                // duplicar ciclos. (OBSERVADO sí habilita reenvío: es la
                                // subsanación que BRIXO pidió.)
                                val estadoEnCurso = ultimaSolicitud?.estado?.uppercase() ?: ""
                                if (estadoEnCurso == "PENDIENTE") {
                                    errorMensaje = "Ya tienes una constancia en revisión por BRIXO. Espera su validación antes de enviar otra."
                                    return@Button
                                }
                                if (estadoEnCurso == "SUBSANADA") {
                                    errorMensaje = "Tu constancia corregida está en revisión. Espera la respuesta antes de enviar otra."
                                    return@Button
                                }

                                isLoading = true
                                errorMensaje = null

                                coroutineScope.launch {
                                    try {
                                        val authUser = FirebaseAuth.getInstance().currentUser
                                        val email = authUser?.email ?: ""
                                        val db = FarmadonFirestore.db


                                        // Subir imagen a Firebase Storage si es un URI local
                                        var urlDescarga = ultimaSolicitud?.comprobanteUrl ?: ""
                                        if (imagenUri != null) {
                                            val uriStr = imagenUri.toString()
                                            if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                                                urlDescarga = uriStr
                                            } else {
                                                val fileId = java.util.UUID.randomUUID().toString()
                                                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                                                    .child("comprobantes_pago")
                                                    .child(clienteId)
                                                    .child("${fileId}_${numeroOperacion.ifBlank { "voucher" }}.jpg")
                                                storageRef.putFile(imagenUri!!).await()
                                                urlDescarga = storageRef.downloadUrl.await().toString()

                                            }
                                        }

                                        if (estaObservada && !ultimaSolicitud?.id.isNullOrBlank()) {
                                            // Limpiar foto rechazada anterior de Storage si se cargó una nueva (Cero basura residual)
                                            val fotoAnteriorUrl = ultimaSolicitud?.comprobanteUrl
                                            if (!fotoAnteriorUrl.isNullOrBlank() && fotoAnteriorUrl != urlDescarga && fotoAnteriorUrl.contains("firebasestorage.googleapis.com")) {
                                                try {
                                                    val oldStorageRef = com.google.firebase.storage.FirebaseStorage.getInstance().getReferenceFromUrl(fotoAnteriorUrl)
                                                    oldStorageRef.delete()
                                                } catch (_: Exception) {}
                                            }

                                            // Subsanación atómica del comprobante observado con candado de carrera (P3)
                                            val solRef = db.collection("compartido").document("ecosistema").collection("solicitudes_pago").document(ultimaSolicitud!!.id)
                                            db.runTransaction { tx ->
                                                val snap = tx.get(solRef)
                                                if (!snap.exists()) throw Exception("La solicitud de pago ya no existe.")
                                                val estadoActual = snap.getString("estado") ?: ""
                                                if (estadoActual.equals("APROBADO", ignoreCase = true) || estadoActual.equals("APROBADA", ignoreCase = true)) {
                                                    throw Exception("Esta solicitud ya fue aprobada por BRIXO.")
                                                }
                                                val updates = mutableMapOf<String, Any>(
                                                    "montoReportado" to monto,
                                                    "banco" to bancoSeleccionado.ifBlank { "Bancario" },
                                                    "numeroOperacion" to numeroOperacion.trim(),
                                                    "comprobanteUrl" to urlDescarga,
                                                    "notaAclaratoria" to notaAclaratoria.trim(),
                                                    "notaAclaratoriaCliente" to notaAclaratoria.trim(),
                                                    "estado" to "SUBSANADA",
                                                    "subsanadaAt" to FieldValue.serverTimestamp(),
                                                    "updatedAt" to FieldValue.serverTimestamp(),
                                                    "lineaTiempo" to FieldValue.arrayUnion(mapOf(
                                                        "tipo" to "SUBSANACION_CLIENTE",
                                                        "agente" to email,
                                                        "ts" to com.google.firebase.Timestamp.now(),
                                                        "detalle" to notaAclaratoria.trim().ifBlank { "El cliente envió el comprobante subsanado." }
                                                    ))
                                                )
                                                tx.update(solRef, updates)
                                            }.await()
                                        } else {
                                            // P2: Resolver nombre comercial auténtico de la farmacia (no de la sucursal)
                                            val farmaciaSnap = try { PlanFacturacionPaths.farmacia(db, clienteId).get().await() } catch (_: Exception) { null }
                                            val nombreFarmaciaReal = farmaciaSnap?.getString("nombreFarmacia")
                                                ?: farmaciaSnap?.getString("nombreComercial")
                                                ?: farmaciaSnap?.getString("nombre")
                                                ?: farmaciaSnap?.getString("razonSocial")
                                                ?: (SessionManager.sucursalNombre.ifBlank { "Farmacia" })

                                            // Nuevo reporte de pago
                                            // REGLA DE LA VENTANA DE PAGO (espejo del candado
                                            // de BRIXO): solo se reporta con el contrato POR
                                            // VENCER (5 días) o VENCIDO. FAIL-CLOSED: si la
                                            // vigencia no se puede VERIFICAR contra el servidor,
                                            // el reporte NO sale —” jamás adivinar la ventana.
                                            val subSnapVentana = try {
                                                PlanFacturacionPaths.farmacia(db, clienteId)
                                                    .collection("suscripciones")
                                                    .orderBy("fechaCreacion", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                                    .limit(1)
                                                    .get(com.google.firebase.firestore.Source.SERVER)
                                                    .await()
                                                    .documents.firstOrNull()
                                            } catch (_: Exception) { null }
                                            if (subSnapVentana == null) {
                                                throw Exception(
                                                    "VENTANA_NO_VERIFICABLE: No pudimos verificar la vigencia de tu contrato (conexión). " +
                                                        "El reporte quedará en espera hasta que tengas señal estable."
                                                )
                                            }
                                            // Fecha tolerante: acepta Timestamp nativo o texto ISO legado
                                            val finAny: Any? = subSnapVentana.get("fechaFin")
                                            val finMsVentana: Long = when (finAny) {
                                                is com.google.firebase.Timestamp -> finAny.toDate().time
                                                is java.util.Date -> finAny.time
                                                else -> {
                                                    val txt = finAny?.toString() ?: ""
                                                    val formatos = listOf(
                                                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                                        "yyyy-MM-dd'T'HH:mm:ss",
                                                        "yyyy-MM-dd HH:mm:ss",
                                                        "yyyy-MM-dd"
                                                    )
                                                    var r: Long = 0L
                                                    for (f in formatos) {
                                                        try {
                                                            val d = SimpleDateFormat(f, Locale.US).apply {
                                                                timeZone = TimeZone.getTimeZone("UTC")
                                                            }.parse(txt)
                                                            if (d != null) { r = d.time; break }
                                                        } catch (_: Exception) {}
                                                    }
                                                    r
                                                }
                                            }
                                            if (finMsVentana <= 0L) {
                                                throw Exception(
                                                    "VENTANA_NO_VERIFICABLE: No pudimos leer la vigencia de tu contrato. " +
                                                        "Contacta a soporte para revisarla antes de reportar pagos."
                                                )
                                            }
                                            // HORA SERVIDOR (sin cuello): el reloj
                                            // local engañado ya no decide la ventana
                                            // —” mismo reloj oficial que usará BRIXO.
                                            val hoyMsVentana = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
                                            val diasRestantesVentana = (finMsVentana - hoyMsVentana) / 86_400_000L
                                            if (diasRestantesVentana > 5) {
                                                val fechaHumanaVentana = try {
                                                    SimpleDateFormat("d 'de' MMMM", Locale.forLanguageTag("es")).format(java.util.Date(finMsVentana))
                                                } catch (_: Exception) { finAny?.toString()?.take(10) ?: "" }
                                                throw Exception(
                                                    "VENTANA_DE_PAGO_CERRADA: Tu ciclo está vigente hasta $fechaHumanaVentana. " +
                                                        "El reporte de voucher abre los últimos 5 días o al vencer."
                                                )
                                            }

                                            val solRef = db.collection("compartido").document("ecosistema").collection("solicitudes_pago").document()
                                            val data = mapOf(
                                                "clienteId" to clienteId,
                                                "nombreFarmacia" to nombreFarmaciaReal,
                                                "banco" to bancoSeleccionado.ifBlank { "Bancario" },
                                                "montoReportado" to monto,
                                                "numeroOperacion" to numeroOperacion.trim(),
                                                "comprobanteUrl" to urlDescarga,
                                                "notaAclaratoria" to notaAclaratoria.trim(),
                                                "reportadoPorEmail" to email,
                                                "reportadoPorNombre" to (SessionManager.nombreUsuario.ifBlank { "Administrador" }),
                                                "estado" to "PENDIENTE",
                                                "createdAt" to FieldValue.serverTimestamp(),
                                                "updatedAt" to FieldValue.serverTimestamp(),
                                                "lineaTiempo" to listOf(mapOf(
                                                    "tipo" to "PAGO_REPORTADO_CLIENTE",
                                                    "agente" to email,
                                                    "ts" to com.google.firebase.Timestamp.now(),
                                                    "detalle" to "Comprobante reportado inicialmente por el cliente."
                                                ))
                                            )
                                            solRef.set(data).await()
                                        }


                                        isLoading = false
                                        exitoEnvio = true
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMensaje = "Error al enviar comprobante: ${e.localizedMessage}"
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = if (estaObservada) Color(0xFFF59E0B) else Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (estaObservada) "REENVIAR SUBSANACIÓN A BRIXO" else "ENVIAR CONSTANCIA A BRIXO",
                                fontWeight = FontWeight.Black,
                                color = if (estaObservada) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

        }
    }
}

