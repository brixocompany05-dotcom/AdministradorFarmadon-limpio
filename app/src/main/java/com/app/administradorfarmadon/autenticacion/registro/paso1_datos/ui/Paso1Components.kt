package com.app.administradorfarmadon.autenticacion.registro.paso1_datos.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun RegistroSelectorField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    error: String? = null,
    isWarning: Boolean = false,
    warningMessage: String? = null,
    s: MedidaAdaptativa,
    placeholder: String? = "Toca para seleccionar...",
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.LocationOn
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(s.gapTiny)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = TokensFarmadon.tipografia.leyenda.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold),
                color = if (enabled) TokensFarmadon.colores.textoSecundario else TokensFarmadon.colores.textoTerciario
            )
            if (!enabled && isWarning.not()) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    tint = TokensFarmadon.colores.textoTerciario,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        
        Surface(
            onClick = if (enabled) onClick else { {} },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = s.inputMinH)
                .then(if (enabled) Modifier.bounceClick() else Modifier),
            color = if (enabled) TokensFarmadon.colores.inputFondo else TokensFarmadon.colores.superficieDefecto.copy(alpha = 0.4f),
            shape = TokensFarmadon.formas.mediana,
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                when {
                    error != null -> TokensFarmadon.colores.estadoPeligro
                    isWarning -> TokensFarmadon.colores.estadoAlerta
                    !enabled -> TokensFarmadon.colores.bordeSutil.copy(alpha = 0.3f)
                    else -> TokensFarmadon.colores.inputBorde.copy(alpha = 0.5f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled && value.isNotBlank()) TokensFarmadon.colores.textoPrincipal else TokensFarmadon.colores.textoTerciario,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = value.ifBlank { placeholder ?: "" },
                    style = TokensFarmadon.tipografia.cuerpo,
                    color = if (enabled && value.isNotBlank()) TokensFarmadon.colores.textoPrincipal else TokensFarmadon.colores.textoTerciario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (enabled) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TokensFarmadon.colores.textoTerciario,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        if (error != null) {
            Text(text = error, color = TokensFarmadon.colores.estadoPeligro, style = TokensFarmadon.tipografia.cuerpoPequeno)
        } else if (warningMessage != null) {
            Text(text = warningMessage, color = TokensFarmadon.colores.estadoAlerta, style = TokensFarmadon.tipografia.cuerpoPequeno)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegistroTextField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onFocus: (() -> Unit)? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    showPasswordToggle: Boolean = false,
    focusRequester: FocusRequester? = null,
    onImeAction: (() -> Unit)? = null,
    s: MedidaAdaptativa,
    placeholder: String? = null,
    // Texto fijo NO editable al inicio del campo (ej.: prefijo telefónico "+51").
    prefixText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    isWarning: Boolean = false,
    warningMessage: String? = null,
    isProgrammaticScroll: MutableState<Boolean>? = null
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }

    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            snapshotFlow { ime.getBottom(density) }
                .distinctUntilChanged()
                .collectLatest { bottomInset ->
                    if (bottomInset > 0) {
                        isProgrammaticScroll?.value = true
                        val offset = with(density) { 90.dp.toPx() }
                        bringIntoViewRequester.bringIntoView(
                            Rect(0f, 0f, fieldSize.width.toFloat(), fieldSize.height.toFloat() + offset)
                        )
                        isProgrammaticScroll?.value = false
                    }
                }
        }
    }

    var passwordVisible by remember { mutableStateOf(false) }
    val isPassword = keyboardType == KeyboardType.Password
    
    val actualVisualTransformation = if (isPassword && !passwordVisible) {
        PasswordVisualTransformation()
    } else {
        visualTransformation
    }

    val finalTrailingIcon = if (isPassword && showPasswordToggle) {
        {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                    tint = TokensFarmadon.colores.textoTerciario
                )
            }
        }
    } else {
        trailingIcon
    }

    Column(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { fieldSize = it.size },
        verticalArrangement = Arrangement.spacedBy(s.gapTiny)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = TokensFarmadon.tipografia.leyenda.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold),
                color = if (readOnly && !isWarning) TokensFarmadon.colores.textoTerciario else TokensFarmadon.colores.textoSecundario
            )
            if (readOnly && !isWarning) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    tint = TokensFarmadon.colores.textoTerciario,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = s.inputMinH)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        onFocus?.invoke()
                    }
                },
            textStyle = TokensFarmadon.tipografia.cuerpo.copy(
                color = if (readOnly && !isWarning) TokensFarmadon.colores.textoTerciario else TokensFarmadon.colores.textoPrincipal
            ),
            placeholder = placeholder?.let { { Text(it, color = TokensFarmadon.colores.textoTerciario, style = TokensFarmadon.tipografia.cuerpoPequeno) } },
            leadingIcon = prefixText?.let { prefijo -> { Text(prefijo, color = TokensFarmadon.colores.textoSecundario, style = TokensFarmadon.tipografia.cuerpoPequeno) } },
            isError = error != null,
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Text else keyboardType,
                capitalization = if (isPassword) KeyboardCapitalization.None else if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Words else KeyboardCapitalization.None,
                imeAction = imeAction,
                autoCorrect = false
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            visualTransformation = actualVisualTransformation,
            trailingIcon = finalTrailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TokensFarmadon.colores.textoPrincipal,
                unfocusedTextColor = if (readOnly && !isWarning) TokensFarmadon.colores.textoTerciario else TokensFarmadon.colores.textoPrincipal,
                disabledTextColor = TokensFarmadon.colores.textoTerciario,
                focusedContainerColor = TokensFarmadon.colores.inputFondoFoco,
                unfocusedContainerColor = if (readOnly && !isWarning) TokensFarmadon.colores.inputFondo.copy(alpha = 0.4f) else TokensFarmadon.colores.inputFondo,
                disabledContainerColor = TokensFarmadon.colores.inputFondo.copy(alpha = 0.4f),
                focusedBorderColor = if (isWarning && error == null) TokensFarmadon.colores.estadoAlerta else TokensFarmadon.colores.bordeEnfoque,
                unfocusedBorderColor = if (isWarning && error == null) TokensFarmadon.colores.estadoAlerta else if (readOnly) TokensFarmadon.colores.inputBorde.copy(alpha = 0.3f) else TokensFarmadon.colores.inputBorde.copy(alpha = 0.7f),
                disabledBorderColor = if (isWarning && error == null) TokensFarmadon.colores.estadoAlerta else TokensFarmadon.colores.inputBorde.copy(alpha = 0.3f),
                focusedPlaceholderColor = TokensFarmadon.colores.inputPlaceholder,
                unfocusedPlaceholderColor = TokensFarmadon.colores.inputPlaceholder
            ),
            shape = TokensFarmadon.formas.mediana
        )
        if (error != null) {
            Text(text = error, color = TokensFarmadon.colores.estadoPeligro, style = TokensFarmadon.tipografia.cuerpoPequeno)
        } else if (warningMessage != null) {
            Text(text = warningMessage, color = TokensFarmadon.colores.estadoAlerta, style = TokensFarmadon.tipografia.cuerpoPequeno)
        }
    }
}

@Composable
fun ChecklistContrasena(pass: String, s: MedidaAdaptativa) {
    val rules = listOf(
        "Mínimo 8 caracteres" to (pass.length >= 8),
        "Al menos 1 mayúscula" to pass.any { it.isUpperCase() },
        "Al menos 1 número" to pass.any { it.isDigit() }
    )

    Column(
        modifier = Modifier.padding(start = s.gapSmall, top = s.gapTiny),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        rules.forEach { (label, met) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
            ) {
                Icon(
                    imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (met) TokensFarmadon.colores.estadoExito else TokensFarmadon.colores.textoTerciario.copy(alpha = 0.4f)
                )
                Text(
                    text = label,
                    style = TokensFarmadon.tipografia.leyenda,
                    color = if (met) TokensFarmadon.colores.estadoExito else TokensFarmadon.colores.textoTerciario
                )
            }
        }
    }
}

@Composable
fun MiniMapaConfirmacion(
    lat: Double,
    lng: Double,
    direccion: String,
    onLocationManual: (Double, Double) -> Unit
) {
    var isUserInteracting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var isLocating by remember { mutableStateOf(false) }

    fun irAUbicacionActual() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            // Se asume que el launcher está definido más abajo o lo definimos aquí arriba
            return
        }

        isLocating = true
        scope.launch {
            try {
                val location = locationClient.lastLocation.await()
                if (location != null) {
                    onLocationManual(location.latitude, location.longitude)
                } else {
                    android.widget.Toast.makeText(context, "No se pudo detectar el GPS. Actívalo e intenta de nuevo.", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.w("FARMADON_UI", "Error GPS: ${e.message}")
            } finally {
                isLocating = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            irAUbicacionActual()
        }
    }

    // Re-definimos la función para que use el launcher que ahora sí conoce
    val irAUbicacionActualWithPermission = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            irAUbicacionActual()
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = expandVertically(animationSpec = tween(220)) + fadeIn(),
        exit = shrinkVertically(animationSpec = tween(220)) + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = TokensFarmadon.colores.superficieElevada,
            shape = TokensFarmadon.formas.grande,
            border = androidx.compose.foundation.BorderStroke(1.dp, TokensFarmadon.colores.bordeSutil)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(TokensFarmadon.formas.grande)
                ) {
                    AndroidView(
                        factory = { context ->
                            Configuration.getInstance().userAgentValue = context.packageName
                            MapView(context).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(17.5)
                                
                                setOnTouchListener { v, event ->
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            isUserInteracting = true
                                            v.parent.requestDisallowInterceptTouchEvent(true)
                                        }
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            isUserInteracting = false
                                            v.parent.requestDisallowInterceptTouchEvent(false)
                                            val center = this.mapCenter
                                            onLocationManual(center.latitude, center.longitude)
                                            v.performClick()
                                        }
                                    }
                                    false
                                }

                                addMapListener(object : MapListener {
                                    override fun onScroll(event: ScrollEvent?): Boolean = true
                                    override fun onZoom(event: ZoomEvent?): Boolean {
                                        if (isUserInteracting) {
                                            val center = mapCenter
                                            onLocationManual(center.latitude, center.longitude)
                                        }
                                        return true
                                    }
                                })
                            }
                        },
                        update = { view ->
                            if (!isUserInteracting) {
                                val currentCenter = view.mapCenter
                                val targetPoint = OsmGeoPoint(lat, lng)
                                val latDiff = Math.abs(currentCenter.latitude - lat)
                                val lngDiff = Math.abs(currentCenter.longitude - lng)
                                if (latDiff > 0.00001 || lngDiff > 0.00001) {
                                    view.controller.animateTo(targetPoint)
                                }
                            }
                            view.onResume()
                        },
                        onRelease = { view -> view.onPause() },
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp, 4.dp)
                                    .background(TokensFarmadon.colores.textoPrincipal.copy(alpha = 0.3f), CircleShape)
                            )
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Punto central",
                                tint = TokensFarmadon.colores.estadoExito,
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer {
                                        val scale = if (isUserInteracting) 1.2f else 1.0f
                                        scaleX = scale
                                        scaleY = scale
                                        translationY = if (isUserInteracting) -10f else 0f
                                    }
                            )
                        }
                    }

                    // Botón de mi ubicación actual flotante
                    SmallFloatingActionButton(
                        onClick = { irAUbicacionActualWithPermission() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        containerColor = TokensFarmadon.colores.superficieElevada,
                        contentColor = TokensFarmadon.colores.textoPrincipal,
                        shape = CircleShape
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TokensFarmadon.colores.textoPrincipal)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                Column(modifier = Modifier.padding(12.dp)) {
                    Surface(
                        color = TokensFarmadon.colores.estadoExito.copy(alpha = 0.1f),
                        shape = TokensFarmadon.formas.completa
                    ) {
                        Text(
                            "UBICACIÓN CONFIRMADA",
                            style = TokensFarmadon.tipografia.leyenda.copy(fontWeight = FontWeight.Bold),
                            color = TokensFarmadon.colores.estadoExito,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        direccion,
                        style = TokensFarmadon.tipografia.cuerpoPequeno,
                        color = TokensFarmadon.colores.textoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

class PhoneNumberVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val digits = text.text
        val sb = StringBuilder()
        val startIdx = if (digits.startsWith("+51")) {
            sb.append("+51 ")
            3
        } else if (digits.startsWith("+")) {
            sb.append("+")
            1
        } else 0

        var digitCount = 0
        for (i in startIdx until digits.length) {
            if (digitCount > 0 && digitCount % 3 == 0) sb.append(" ")
            sb.append(digits[i])
            digitCount++
        }

        val out = sb.toString()
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var transformedOffset = offset
                if (digits.startsWith("+51") && offset > 3) transformedOffset += 1
                val digitsAfterPrefix = if (offset > startIdx) offset - startIdx else 0
                if (digitsAfterPrefix > 0) transformedOffset += (digitsAfterPrefix - 1) / 3
                return transformedOffset.coerceAtMost(out.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                var originalOffset = offset
                if (out.startsWith("+51 ")) {
                    if (offset > 4) originalOffset -= 1
                    else if (offset > 3) return 3
                }
                var spacesCount = 0
                for (i in 0 until offset.coerceAtMost(out.length)) {
                    if (out[i] == ' ') spacesCount++
                }
                originalOffset -= spacesCount
                return originalOffset.coerceAtMost(digits.length)
            }
        }
        return TransformedText(androidx.compose.ui.text.AnnotatedString(out), offsetMapping)
    }
}
