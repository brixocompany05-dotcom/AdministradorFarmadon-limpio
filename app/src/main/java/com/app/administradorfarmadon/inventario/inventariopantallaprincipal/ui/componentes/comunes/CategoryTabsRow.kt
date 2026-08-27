package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors

@Composable
fun CategoryTabsRow(
    categories: List<String>,
    selectedCategories: Set<String>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    var showMoreMenu by remember { mutableStateOf(false) }

    val allCategories = remember(categories) {
        listOf("Todos") + categories.filter { it.isNotBlank() && it != "Todos" }
    }

    val maxVisible = 6
    val mainCategories = remember(allCategories) {
        allCategories.take(maxVisible)
    }
    val hiddenCategories = remember(allCategories) {
        allCategories.drop(maxVisible)
    }

    val isAnyHiddenSelected = remember(selectedCategories, hiddenCategories) {
        selectedCategories.any { it in hiddenCategories }
    }

    val bgFadeColor = FDColors.Background

    Box(modifier = modifier) {
        LazyRow(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    val fadeWidth = 24.dp.toPx()
                    
                    if (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(bgFadeColor, Color.Transparent),
                                startX = 0f,
                                endX = fadeWidth
                            ),
                            size = size.copy(width = fadeWidth)
                        )
                    }

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, bgFadeColor),
                            startX = size.width - fadeWidth,
                            endX = size.width
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width - fadeWidth, 0f),
                        size = size.copy(width = fadeWidth)
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(mainCategories) { category ->
                CategoryTabItem(
                    label = category,
                    isSelected = if (category == "Todos") selectedCategories.isEmpty() else selectedCategories.contains(category),
                    onClick = { onCategoryClick(category) }
                )
            }

            if (hiddenCategories.isNotEmpty()) {
                item {
                    Box {
                        CategoryTabItem(
                            label = "MÁS",
                            isSelected = isAnyHiddenSelected,
                            hasDropdown = true,
                            onClick = { showMoreMenu = true }
                        )

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier
                                .background(FDColors.SurfaceElevated)
                                .border(0.5.dp, FDColors.Border, RoundedCornerShape(8.dp))
                        ) {
                            hiddenCategories.forEach { category ->
                                val isSelected = selectedCategories.contains(category)
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            category.uppercase(), 
                                            color = if (isSelected) FDColors.TextPrimary else FDColors.TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            letterSpacing = 0.5.sp
                                        ) 
                                    },
                                    onClick = { 
                                        onCategoryClick(category)
                                        showMoreMenu = false 
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, null, tint = FDColors.TextPrimary, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabItem(
    label: String,
    isSelected: Boolean,
    hasDropdown: Boolean = false,
    onClick: () -> Unit
) {
    val selectedBg = FDColors.Primary
    val unselectedBg = FDColors.SurfaceElevated
    val selectedText = if (FDColors.isDark) Color.Black else Color.White
    val unselectedText = FDColors.TextSecondary

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) selectedBg else unselectedBg,
        label = "bgColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) selectedText else unselectedText,
        label = "textColor"
    )

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(bgColor)
            .border(0.5.dp, if (isSelected) Color.Transparent else FDColors.Border, RoundedCornerShape(7.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label.uppercase(),
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            if (hasDropdown) {
                Icon(
                    Icons.Default.KeyboardArrowDown, 
                    null, 
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
