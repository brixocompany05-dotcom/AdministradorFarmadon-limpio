package com.app.administradorfarmadon.configuracion.plan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.BoxWithConstraints
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.plan.logica.PlanFacturacionViewModel
import com.app.administradorfarmadon.configuracion.plan.ui.componentes.BrixoCanalesPagoCard
import com.app.administradorfarmadon.configuracion.plan.ui.componentes.CatalogoPlanesModal
import com.app.administradorfarmadon.configuracion.plan.ui.componentes.ConstanciaPagoDialog
import com.app.administradorfarmadon.configuracion.plan.ui.componentes.HistorialFacturacionCard
import com.app.administradorfarmadon.configuracion.plan.ui.componentes.PlanCapacidadesCard
import com.app.administradorfarmadon.configuracion.plan.ui.componentes.PlanCronogramaCard
import com.app.administradorfarmadon.configuracion.plan.ui.componentes.PlanResumenHeader
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun PlanFacturacionScreen(
    onVolver: () -> Unit,
    viewModel: PlanFacturacionViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()
    val scrollState = rememberScrollState()

    val nombreFarmacia = SessionManager.nombreUsuario.ifBlank { "Mi Farmacia" }
    val rucFarmacia = SessionManager.dni.ifBlank { "" }

    // Modal de Catálogo de Planes y Solicitud de Upgrade
    if (uiState.mostrarModalPlanes) {
        CatalogoPlanesModal(
            planes = uiState.catalogoPlanes,
            planActualNombre = uiState.planInfo.planNombre,
            whatsappCobranzas = uiState.canalesPago.whatsappCobranzas,
            nombreFarmacia = nombreFarmacia,
            rucFarmacia = rucFarmacia,
            catalogoError = uiState.catalogoError,
            onDismiss = { viewModel.cerrarModalPlanes() }
        )
    }

    // Modal de Constancia Digital Oficial BRIXO
    uiState.pagoSeleccionado?.let { pago ->
        ConstanciaPagoDialog(
            pago = pago,
            nombreFarmacia = nombreFarmacia,
            rucFarmacia = rucFarmacia,
            emisorRazonSocial = uiState.canalesPago.razonSocial,
            emisorRuc = uiState.canalesPago.ruc,
            onDismiss = { viewModel.seleccionarPagoParaDetalle(null) }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colores.fondoBase)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
    ) {
        val padH = s.padScreenH
        val padV = s.padScreenV
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = padH, vertical = padV)
        ) {
        if (uiState.cargando) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colores.botonPrimarioFondo, modifier = Modifier.size(s.iconLarge))
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(s.padCardLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(s.sm)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = colores.estadoPeligro,
                        modifier = Modifier.size(s.iconLarge * 1.25f)
                    )
                    Text(
                        text = "No pudimos cargar tu plan y facturación",
                        style = TokensFarmadon.tipografia.titulo2.copy(fontSize = s.textSubtitle.value.sp),
                        color = colores.textoPrincipal,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = uiState.error ?: "Ocurrió un problema al consultar tus datos. Intenta de nuevo.",
                        style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                        color = colores.textoSecundario,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.iniciarEscuchaTiempoReal() },
                        shape = RoundedCornerShape(s.radiusChip),
                        colors = ButtonDefaults.buttonColors(containerColor = colores.botonPrimarioFondo)
                    ) {
                        Text(
                            text = "REINTENTAR",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, color = colores.botonPrimarioTexto, fontSize = s.textLabel.value.sp)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(s.gapLarge)
            ) {
                // Header con botón Volver
                PlanResumenHeader(
                    onVolver = onVolver,
                    planNombre = uiState.planInfo.planNombre
                )

                // Layout de 2 Columnas Simétricas (50% / 50% exacto)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
                ) {
                    // COLUMNA IZQUIERDA (50%): Cronograma con Timeline + Capacidades
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                    ) {
                        PlanCronogramaCard(
                            planInfo = uiState.planInfo,
                            onExplorarPlanes = { viewModel.abrirModalPlanes() }
                        )
                        PlanCapacidadesCard(planInfo = uiState.planInfo)
                    }

                    Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                    ) {
                    BrixoCanalesPagoCard(
                        canalesPago = uiState.canalesPago,
                        nombreFarmacia = nombreFarmacia,
                        rucFarmacia = rucFarmacia,
                        error = uiState.canalesError
                    )
                    HistorialFacturacionCard(
                        planInfo = uiState.planInfo,
                        historial = uiState.historialPagos,
                        onVerConstancia = { pago -> viewModel.seleccionarPagoParaDetalle(pago) }
                    )
                    }
                }
            }
        }
        }
    }
}
