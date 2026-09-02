package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.view.MotionEvent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.logica.UbicacionHelper
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale

@Composable
fun UbicacionPasoMapaUberStyle(
    direccionActual: String,
    latitudActual: Double?,
    longitudActual: Double?,
    onUbicacionSeleccionada: (direccion: String, lat: Double, lng: Double) -> Unit,
    s: MedidaAdaptativa,
    modifier: Modifier = Modifier,
    esModoEdicionEstatico: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colores = TokensFarmadon.colores

    // Detectar si hay GPS/Ubicación actual en el dispositivo
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
    val tienePermisoUbicacion = remember {
        locationManager != null &&
            (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val ubicacionGPSInicial: Location? = remember {
        if (tienePermisoUbicacion && locationManager != null) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } else null
    }

    // Coordenadas iniciales: Si el formulario ya tenía coords, usa esas; de lo contrario usa la ubicación GPS física real.
    val latInicial = latitudActual ?: ubicacionGPSInicial?.latitude ?: -12.0464
    val lngInicial = longitudActual ?: ubicacionGPSInicial?.longitude ?: -77.0428

    var isArrastrandoMapa by remember { mutableStateOf(false) }
    var geocodingCargando by remember { mutableStateOf(false) }

    var latitudEstado by remember { mutableDoubleStateOf(latInicial) }
    var longitudEstado by remember { mutableDoubleStateOf(lngInicial) }
    var direccionEstado by remember { mutableStateOf(direccionActual) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    val pinElevationY by animateDpAsState(
        targetValue = if (isArrastrandoMapa) (-28).dp else (-20).dp,
        animationSpec = tween(durationMillis = 120),
        label = "pinBounce"
    )

    // Inicialización al abrir la pantalla: Obtener la dirección exacta del punto inicial
    LaunchedEffect(Unit) {
        if (direccionActual.isBlank() || latitudActual == null) {
            geocodingCargando = true
            UbicacionHelper.obtenerDireccion(context, latitudEstado, longitudEstado) { dirResolved ->
                geocodingCargando = false
                val dirFinal = dirResolved.ifBlank {
                    "Ubicación fijada (${String.format(Locale.US, "%.4f", latitudEstado)}, ${String.format(Locale.US, "%.4f", longitudEstado)})"
                }
                direccionEstado = dirFinal
                onUbicacionSeleccionada(dirFinal, latitudEstado, longitudEstado)
            }
        }
    }

    val alturaMapa = if (esModoEdicionEstatico) 200.dp else 420.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(s.xs)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(alturaMapa)
                .clip(RoundedCornerShape(s.radiusInput)),
            color = if (colores.esTemaClaro) colores.fondoBase else colores.textoPrincipal.copy(alpha = 0.03f),
            border = BorderStroke(s.borderWidth * 0.8f, colores.cardBorde)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. EL MAPA (AndroidView 100% fluido)
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            setBuiltInZoomControls(false)
                            isClickable = true
                            isFocusable = true
                            isVerticalMapRepetitionEnabled = false
                            isHorizontalMapRepetitionEnabled = false
                            controller.setZoom(17.2)

                            // En modo edición estático, el mapa es 100% fijo e inamovible en la dirección de la sede
                            if (esModoEdicionEstatico) {
                                setOnTouchListener { view, _ ->
                                    view.parent?.requestDisallowInterceptTouchEvent(false)
                                    false
                                }
                            }

                            // Soporte nativo para Zoom In / Zoom Out con la perilla/rueda del mouse (Scroll Wheel)
                            setOnGenericMotionListener { _, event ->
                                if (event.action == MotionEvent.ACTION_SCROLL) {
                                    val vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                                    if (vscroll > 0f) {
                                        controller.zoomIn()
                                        true
                                    } else if (vscroll < 0f) {
                                        controller.zoomOut()
                                        true
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            }

                            val initialPoint = GeoPoint(latitudEstado, longitudEstado)
                            controller.setCenter(initialPoint)

                            addMapListener(object : MapListener {
                                override fun onScroll(event: ScrollEvent?): Boolean {
                                    if (esModoEdicionEstatico) {
                                        // En modo edición estático, el pin permanece 100% fijo en las coordenadas guardadas de la sede
                                        return true
                                    }
                                    isArrastrandoMapa = true
                                    debounceJob?.cancel()
                                    debounceJob = scope.launch {
                                        delay(400) // Debounce fluido
                                        isArrastrandoMapa = false
                                        val center = mapCenter
                                        if (center != null) {
                                            val newLat = center.latitude
                                            val newLng = center.longitude
                                            latitudEstado = newLat
                                            longitudEstado = newLng
                                            geocodingCargando = true
                                            UbicacionHelper.obtenerDireccion(ctx, newLat, newLng) { dirResolved ->
                                                geocodingCargando = false
                                                val dirFinal = dirResolved.ifBlank {
                                                    "Ubicación fijada (${String.format(Locale.US, "%.4f", newLat)}, ${String.format(Locale.US, "%.4f", newLng)})"
                                                }
                                                direccionEstado = dirFinal
                                                onUbicacionSeleccionada(dirFinal, newLat, newLng)
                                            }
                                        }
                                    }
                                    return true
                                }

                                override fun onZoom(event: ZoomEvent?): Boolean {
                                    // Zoom In / Zoom Out preserva las coordenadas del pin sin re-consultar la dirección
                                    return true
                                }
                            })

                            mapViewRef = this
                        }
                    },
                    update = { _ ->
                        // Cero animateTo circular: El mapa responde 100% al gesto libre del usuario
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. PIN ENTERPRISE FIJO AL CENTRO (Alto contraste visual sobre el mapa)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 4.dp)
                        .size(
                            width = if (isArrastrandoMapa) 22.dp else 16.dp,
                            height = if (isArrastrandoMapa) 8.dp else 6.dp
                        )
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = if (isArrastrandoMapa) 0.25f else 0.55f))
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = pinElevationY)
                ) {
                    Surface(
                        color = Color(0xFF0F172A), // Slate 900 de alto contraste
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 12.dp,
                        border = BorderStroke(2.5.dp, Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colores.estadoExito)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "UBICACIÓN SEDE",
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.6.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 10.dp, height = 7.dp)
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(Color(0xFF0F172A))
                    )
                }

                // 3. CONTROLES DE NAVEGACIÓN MAPA (Zoom +/- y GPS)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(s.sm),
                    verticalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Surface(
                        onClick = { mapViewRef?.controller?.zoomIn() },
                        color = colores.cardBase.copy(alpha = 0.95f),
                        shape = CircleShape,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, "Zoom in", tint = colores.textoPrincipal, modifier = Modifier.size(20.dp))
                        }
                    }
                    Surface(
                        onClick = { mapViewRef?.controller?.zoomOut() },
                        color = colores.cardBase.copy(alpha = 0.95f),
                        shape = CircleShape,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, "Zoom out", tint = colores.textoPrincipal, modifier = Modifier.size(20.dp))
                        }
                    }
                    Surface(
                        onClick = {
                            val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                            val tienePermiso = locManager != null &&
                                (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                    context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)

                            val ubicacionReal: Location? = if (tienePermiso && locManager != null) {
                                locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                    ?: locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                    ?: locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                            } else null

                            if (ubicacionReal != null) {
                                val geoReal = GeoPoint(ubicacionReal.latitude, ubicacionReal.longitude)
                                mapViewRef?.controller?.animateTo(geoReal)
                                latitudEstado = geoReal.latitude
                                longitudEstado = geoReal.longitude
                                geocodingCargando = true
                                UbicacionHelper.obtenerDireccion(context, geoReal.latitude, geoReal.longitude) { dirResolved ->
                                    geocodingCargando = false
                                    val dirFinal = dirResolved.ifBlank {
                                        "Ubicación fijada (${String.format(Locale.US, "%.4f", geoReal.latitude)}, ${String.format(Locale.US, "%.4f", geoReal.longitude)})"
                                    }
                                    direccionEstado = dirFinal
                                    onUbicacionSeleccionada(dirFinal, geoReal.latitude, geoReal.longitude)
                                }
                            } else {
                                direccionEstado = if (!tienePermiso)
                                    "Permiso de ubicación no concedido — mueve el mapa manualmente"
                                else
                                    "GPS no disponible en este momento — mueve el mapa manualmente"
                            }
                        },
                        color = colores.cardBase.copy(alpha = 0.95f),
                        shape = CircleShape,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MyLocation, "Centrar en mi ubicación", tint = colores.botonPrimarioFondo, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // 4. TARJETA FLOTANTE INFERIOR CON DIRECCIÓN RESOLVIDA (Solo visible en modo selección/creación)
                if (!esModoEdicionEstatico) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(s.sm),
                        color = colores.cardBase.copy(alpha = 0.97f),
                        shape = RoundedCornerShape(s.radiusInput),
                        shadowElevation = 6.dp,
                        border = BorderStroke(s.borderWidth * 0.8f, colores.cardBorde.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(s.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            Surface(
                                color = colores.botonPrimarioFondo.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(s.radiusChip),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (geocodingCargando) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = colores.botonPrimarioFondo,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            null,
                                            tint = colores.botonPrimarioFondo,
                                            modifier = Modifier.size(s.iconSmall)
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isArrastrandoMapa) "BUSCANDO UBICACIÓN EN EL MAPA…" else "DIRECCIÓN EN EL PIN CENTRAL",
                                        style = TokensFarmadon.tipografia.etiqueta.copy(
                                            fontSize = s.textLabel.value.sp * 0.8f,
                                            letterSpacing = 0.6.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isArrastrandoMapa) colores.botonPrimarioFondo else colores.textoTerciario
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.4f", latitudEstado)}, ${String.format(Locale.US, "%.4f", longitudEstado)}",
                                        style = TokensFarmadon.tipografia.etiqueta.copy(
                                            fontSize = s.textLabel.value.sp * 0.75f,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = colores.textoTerciario
                                    )
                                }
                                Text(
                                    text = when {
                                        geocodingCargando -> "Obteniendo dirección del punto…"
                                        direccionEstado.isNotBlank() -> direccionEstado
                                        else -> "Arrastra el mapa para situar la sede en la dirección exacta"
                                    },
                                    style = TokensFarmadon.tipografia.cuerpo.copy(
                                        fontSize = s.textBody.value.sp * 0.92f,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colores.textoPrincipal,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
