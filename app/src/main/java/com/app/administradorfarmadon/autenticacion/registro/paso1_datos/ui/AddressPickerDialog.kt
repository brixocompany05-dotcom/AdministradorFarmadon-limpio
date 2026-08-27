package com.app.administradorfarmadon.autenticacion.registro.paso1_datos.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressPickerDialog(
    onAddressSelected: (String, Double?, Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Address>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geocoder = remember { Geocoder(context, Locale("es", "PE")) }
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()

    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var onPermissionGrantedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGrantedAction?.invoke()
        }
    }

    fun obtenerUbicacionActual() {
        if (isLocating) return
        
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            onPermissionGrantedAction = { obtenerUbicacionActual() }
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        isLocating = true
        scope.launch {
            try {
                val location = locationClient.lastLocation.await()
                if (location != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                    if (addresses.isNotEmpty()) {
                                        val addr = addresses[0]
                                        scope.launch {
                                            onAddressSelected(addr.getAddressLine(0), addr.latitude, addr.longitude)
                                        }
                                    }
                                    isLocating = false
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val addr = addresses[0]
                                    onAddressSelected(addr.getAddressLine(0), addr.latitude, addr.longitude)
                                }
                                isLocating = false
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("FARMADON_UI", "Geocoding falló: ${e.message}")
                            isLocating = false
                        }
                    }
                } else {
                    isLocating = false
                    android.widget.Toast.makeText(context, "No se pudo obtener la ubicación actual. Verifica tu GPS.", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.w("FARMADON_UI", "Error ubicación: ${e.message}")
                isLocating = false
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(50)
        focusRequester.requestFocus()
    }

    LaunchedEffect(query) {
        if (query.length > 3) {
            delay(600)
            isSearching = true
            
            withContext(Dispatchers.IO) {
                try {
                    val lowerLeftLat = -18.35
                    val lowerLeftLon = -81.33
                    val upperRightLat = -0.03
                    val upperRightLon = -68.65

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocationName(
                            query,
                            15,
                            lowerLeftLat, 
                            lowerLeftLon, 
                            upperRightLat, 
                            upperRightLon
                        ) { addresses ->
                            results = addresses
                            isSearching = false
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(
                            query, 
                            15, 
                            lowerLeftLat, 
                            lowerLeftLon, 
                            upperRightLat, 
                            upperRightLon
                        )
                        results = addresses ?: emptyList()
                        isSearching = false
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FARMADON_UI", "Búsqueda dirección falló: ${e.message}", e)
                    isSearching = false
                }
            }
        } else {
            results = emptyList()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth(0.85f)
                .padding(vertical = 24.dp)
                .imePadding(),
            color = colores.cardBase,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colores.cardBorde),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Buscar dirección",
                        style = TokensFarmadon.tipografia.titulo3.copy(fontSize = 17.sp),
                        color = colores.textoPrincipal
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = colores.textoTerciario, modifier = Modifier.size(20.dp))
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = s.inputMinH)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            "Escribe calle, avenida o referencia",
                            color = colores.textoTerciario,
                            style = TokensFarmadon.tipografia.cuerpoPequeno
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = colores.textoSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colores.textoPrincipal,
                        unfocusedTextColor = colores.textoPrincipal,
                        focusedBorderColor = colores.bordeEnfoque,
                        unfocusedBorderColor = colores.bordeDefecto,
                        focusedContainerColor = if (colores.esTemaClaro) colores.fondoBase else colores.cardBase,
                        unfocusedContainerColor = if (colores.esTemaClaro) colores.fondoBase else colores.cardBase
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    textStyle = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 14.5.sp)
                )

                if (isSearching || isLocating) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colores.textoPrincipal)
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isLocating) { obtenerUbicacionActual() },
                            color = colores.cardElevada.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colores.bordeSutil)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(colores.textoPrincipal.copy(alpha = 0.08f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLocating) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colores.textoPrincipal)
                                    } else {
                                        Icon(
                                            Icons.Default.MyLocation,
                                            null,
                                            tint = colores.textoPrincipal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Usar mi ubicación actual",
                                        style = TokensFarmadon.tipografia.cuerpo.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                        color = colores.textoPrincipal
                                    )
                                    Text(
                                        text = "Detectar posición GPS automática",
                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 11.5.sp),
                                        color = colores.textoSecundario
                                    )
                                }
                            }
                        }
                    }

                    items(results) { address ->
                        val queryLower = query.lowercase()
                        val addressLineLower = (address.getAddressLine(0) ?: "").lowercase()
                        val isMzLt = queryLower.contains("mz") || queryLower.contains("lt") || addressLineLower.contains("mz") || addressLineLower.contains("lt")
                        val esValida = address.thoroughfare != null || isMzLt

                        val mainAddress = if (address.thoroughfare != null) {
                            if (address.featureName != null && address.featureName != address.thoroughfare) {
                                "${address.featureName}, ${address.thoroughfare}"
                            } else {
                                address.getAddressLine(0).split(",")[0]
                            }
                        } else {
                            address.featureName ?: address.getAddressLine(0).split(",")[0]
                        }
                        
                        val subAddress = listOfNotNull(
                            address.subLocality,
                            address.locality,
                            address.subAdminArea
                        ).distinct().joinToString(", ")

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onAddressSelected(
                                        address.getAddressLine(0),
                                        address.latitude,
                                        address.longitude
                                    ) 
                                },
                            color = colores.cardElevada,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colores.cardBorde)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (colores.esTemaClaro) colores.fondoBase else colores.textoPrincipal.copy(alpha = 0.06f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (esValida) Icons.Default.LocationOn else Icons.Default.Search,
                                        null,
                                        tint = colores.textoPrincipal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mainAddress,
                                        style = TokensFarmadon.tipografia.cuerpo.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                        color = colores.textoPrincipal
                                    )
                                    if (subAddress.isNotBlank()) {
                                        Text(
                                            text = subAddress,
                                            style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 11.5.sp),
                                            color = colores.textoSecundario
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (results.isEmpty() && !isSearching && query.length > 3) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "No encontramos esa dirección exacta en el mapa",
                            style = TokensFarmadon.tipografia.cuerpoPequeno,
                            color = colores.textoTerciario,
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddressSelected(query.trim(), null, null) },
                            color = colores.cardElevada,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colores.cardBorde)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(colores.textoPrincipal.copy(alpha = 0.08f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = colores.textoPrincipal, modifier = Modifier.size(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Usar \"${query.trim().take(36)}\" tal cual",
                                        style = TokensFarmadon.tipografia.cuerpo.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                        color = colores.textoPrincipal
                                    )
                                    Text(
                                        text = "Se guardará tu texto y podrás ajustar el pin en el mapa",
                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 11.5.sp),
                                        color = colores.textoSecundario
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
