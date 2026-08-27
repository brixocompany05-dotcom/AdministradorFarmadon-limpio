package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.base_datos.MonedaHelper
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario

// ── COMPONENTES REUTILIZABLES ENTERPRISE ──

@Composable
internal fun EnterpriseStatusPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
internal fun FichaFieldEnterprise(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = FDColors.Background.copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, FDColors.Border)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = FDType.Label.copy(fontSize = 9.5.sp, color = FDColors.TextTertiary))
            Text(text = value, style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary))
        }
    }
}

@Composable
internal fun EnterpriseEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String?,
    onAction: () -> Unit
) {
    Surface(
        color = FDColors.SurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, FDColors.Border),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = FDColors.TextTertiary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary))
            Spacer(Modifier.height(4.dp))
            Text(description, style = FDType.BodySmall.copy(color = FDColors.TextSecondary), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (buttonText != null) {
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText)
                ) {
                    Text(buttonText, style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
