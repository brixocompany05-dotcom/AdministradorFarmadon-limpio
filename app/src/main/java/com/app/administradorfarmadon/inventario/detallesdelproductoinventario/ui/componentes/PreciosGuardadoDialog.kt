package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.R
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDLottieFeedback

@Composable
internal fun PreciosGuardadoDialog(
    estadoGuardado: EstadoGuardadoPrecios,
    mensajeError: String,
    productoNombre: String,
    presentacionesCount: Int,
    onDismiss: () -> Unit,
    onReintentar: () -> Unit
) {
    if (estadoGuardado != EstadoGuardadoPrecios.INACTIVO) {
        Dialog(
            onDismissRequest = {
                if (estadoGuardado == EstadoGuardadoPrecios.ERROR) {
                    onDismiss()
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = estadoGuardado == EstadoGuardadoPrecios.ERROR,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = FDColors.SurfaceElevated,
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier.width(360.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (estadoGuardado) {
                        EstadoGuardadoPrecios.GUARDANDO -> {
                            FDLottieFeedback(
                                resId = R.raw.anim_guardando,
                                isLoop = true,
                                size = 100.dp
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "SUBIENDO Y GUARDANDO...",
                                    style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Sincronizando ${presentacionesCount} presentaciones en la nube.",
                                    style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary, textAlign = TextAlign.Center)
                                )
                            }
                        }

                        EstadoGuardadoPrecios.EXITO -> {
                            FDLottieFeedback(
                                resId = R.raw.anim_exito,
                                isLoop = false,
                                size = 100.dp
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "¡PRECIOS Y PRESENTACIONES GUARDADOS!",
                                    style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.Success),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "La política comercial de \"$productoNombre\" está actualizada.",
                                    style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary, textAlign = TextAlign.Center)
                                )
                            }
                        }

                        EstadoGuardadoPrecios.ERROR -> {
                            FDLottieFeedback(
                                resId = R.raw.anim_error,
                                isLoop = false,
                                size = 100.dp
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "NO SE PUDO GUARDAR",
                                    style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.Error),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = mensajeError.ifBlank { "Ocurrió un error inesperado al sincronizar con el servidor." },
                                    style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary, textAlign = TextAlign.Center)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onDismiss() },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cerrar", style = FDType.Label.copy(fontSize = 12.sp))
                                }
                                Button(
                                    onClick = { onReintentar() },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText)
                                ) {
                                    Text("Reintentar", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        EstadoGuardadoPrecios.INACTIVO -> {}
                    }
                }
            }
        }
    }
}
