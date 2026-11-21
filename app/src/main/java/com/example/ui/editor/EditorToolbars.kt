package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BorderStyle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppFontFamily
import com.example.domain.model.BorderStyle
import com.example.domain.model.EditorTool
import com.example.domain.model.NodeShape
import com.example.domain.model.ShapeCategory
import com.example.ui.components.ShapeGlyphIcon
import com.example.ui.theme.LocalFlowStudioColors
import com.example.ui.theme.NodeSwatches
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.toComposeFontFamily
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.SaveStatus
import kotlin.math.roundToInt

@Composable
fun EditorTopBar(
    title: String,
    saveStatus: SaveStatus,
    autoSaveEnabled: Boolean,
    issueCount: Int,
    hasErrors: Boolean,
    showCodePanel: Boolean,
    onBack: () -> Unit,
    onRenameClick: () -> Unit,
    onManualSave: () -> Unit,
    onToggleCodePanel: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenValidation: () -> Unit,
    onOpenAutoLayout: () -> Unit,
    onOpenExport: () -> Unit,
) {
    val studioColors = LocalFlowStudioColors.current

    Surface(
        color = studioColors.floatingPillSurface,
        border = BorderStroke(1.dp, studioColors.floatingPillBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("editor_back_button"),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home")
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onRenameClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    val dotColor = when (saveStatus) {
                        SaveStatus.SAVED -> studioColors.successColor
                        SaveStatus.SAVING -> studioColors.warningColor
                        SaveStatus.UNSAVED -> studioColors.errorColor
                    }
                    val statusText = when (saveStatus) {
                        SaveStatus.SAVED -> if (autoSaveEnabled) "Synced realtime" else "Saved"
                        SaveStatus.SAVING -> "Syncing…"
                        SaveStatus.UNSAVED -> "Unsaved changes"
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(dotColor, CircleShape)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (!autoSaveEnabled && saveStatus == SaveStatus.UNSAVED) {
                IconButton(
                    onClick = onManualSave,
                    modifier = Modifier.testTag("editor_manual_save_button"),
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = "Save flowchart now",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            IconButton(
                onClick = onOpenAiAssistant,
                modifier = Modifier.testTag("editor_ai_assistant_button"),
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = "AI Workflow Assistant",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            IconButton(
                onClick = onToggleCodePanel,
                modifier = Modifier.testTag("editor_toggle_code_button"),
            ) {
                Icon(
                    Icons.Filled.Code,
                    contentDescription = "Pseudocode, DSL & JSON",
                    tint = if (showCodePanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    hasErrors -> studioColors.errorColor.copy(alpha = 0.14f)
                    issueCount > 0 -> studioColors.warningColor.copy(alpha = 0.16f)
                    else -> studioColors.successColor.copy(alpha = 0.14f)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenValidation)
                    .testTag("editor_validate_button"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (issueCount == 0) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = "Validate flowchart",
                        modifier = Modifier.size(16.dp),
                        tint = when {
                            hasErrors -> studioColors.errorColor
                            issueCount > 0 -> studioColors.warningColor
                            else -> studioColors.successColor
                        },
                    )
                    if (issueCount > 0) {
                        Text(
                            text = issueCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasErrors) studioColors.errorColor else studioColors.warningColor,
                        )
                    }
                }
            }

            IconButton(
                onClick = onOpenAutoLayout,
                modifier = Modifier.testTag("editor_auto_layout_button"),
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = "Auto-arrange flowchart")
            }

            IconButton(
                onClick = onOpenExport,
                modifier = Modifier.testTag("editor_export_button"),
            ) {
                Icon(
                    Icons.Filled.IosShare,
                    contentDescription = "Export and share",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun FloatingModePillToolbar(
    activeTool: EditorTool,
    showShapePalette: Boolean,
    onSelectTool: (EditorTool) -> Unit,
    onAddTextBox: () -> Unit,
    onToggleShapePalette: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val studioColors = LocalFlowStudioColors.current

    Surface(
        color = studioColors.floatingPillSurface,
        border = BorderStroke(1.dp, studioColors.floatingPillBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.shadow(5.dp, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PillToolButton(
                icon = Icons.Filled.NearMe,
                label = "Select",
                selected = activeTool == EditorTool.SELECT && !showShapePalette,
                onClick = { onSelectTool(EditorTool.SELECT) },
                testTag = "tool_select",
            )
            PillToolButton(
                icon = Icons.Filled.PanTool,
                label = "Pan",
                selected = activeTool == EditorTool.PAN,
                onClick = { onSelectTool(EditorTool.PAN) },
                testTag = "tool_pan",
            )
            PillToolButton(
                icon = Icons.Filled.Timeline,
                label = "Connect",
                selected = activeTool == EditorTool.CONNECT,
                onClick = { onSelectTool(EditorTool.CONNECT) },
                testTag = "tool_connect",
            )
            PillToolButton(
                icon = Icons.Filled.SelectAll,
                label = "Multi",
                selected = activeTool == EditorTool.MULTI_SELECT,
                onClick = { onSelectTool(EditorTool.MULTI_SELECT) },
                testTag = "tool_multi",
            )
            PillToolButton(
                icon = Icons.Filled.Title,
                label = "Text",
                selected = false,
                onClick = onAddTextBox,
                testTag = "tool_add_text_box",
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(studioColors.floatingPillBorder)
            )

            PillToolButton(
                icon = Icons.Filled.AddBox,
                label = "Shapes",
                selected = showShapePalette,
                accentFill = true,
                onClick = onToggleShapePalette,
                testTag = "tool_shapes_palette",
            )
        }
    }
}

@Composable
private fun PillToolButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accentFill: Boolean = false,
    onClick: () -> Unit,
    testTag: String,
) {
    val bg = when {
        selected && accentFill -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val fg = when {
        selected && accentFill -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(17.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            softWrap = false,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/**
 * Scrollable bottom sheet displaying ALL 24+ flowchart shapes grouped by category.
 * Shows ONLY the shape icon and shape name under each shape (clean, minimal, uncluttered).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapesPickerBottomSheet(
    onSelectShape: (NodeShape) -> Unit,
    onSelectFlowlineTool: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val studioColors = LocalFlowStudioColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 540.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Flowchart Shapes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "${NodeShape.entries.size + 1} symbols",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = studioColors.floatingPillBorder)

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ShapeCategory.entries.forEach { category ->
                    item(key = category.name) {
                        val shapesInCategory = NodeShape.entries.filter { it.category == category }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )

                            val rows = if (category == ShapeCategory.CORE) {
                                // Include Flowline (Arrow) quick tile alongside Core Shapes
                                val itemsWithArrow: List<NodeShape?> = shapesInCategory + null
                                itemsWithArrow.chunked(3)
                            } else {
                                shapesInCategory.map { it as NodeShape? }.chunked(3)
                            }

                            rows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    rowItems.forEach { shapeOrNull ->
                                        if (shapeOrNull == null) {
                                            ShapePickerGridTile(
                                                name = "Flowline",
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("palette_shape_flowline"),
                                                onClick = {
                                                    onSelectFlowlineTool()
                                                    onDismiss()
                                                },
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                                                    contentDescription = "Flowline",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(32.dp),
                                                )
                                            }
                                        } else {
                                            ShapePickerGridTile(
                                                name = shapeOrNull.displayName,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("palette_shape_${shapeOrNull.name.lowercase()}"),
                                                onClick = {
                                                    onSelectShape(shapeOrNull)
                                                    onDismiss()
                                                },
                                            ) {
                                                ShapeGlyphIcon(
                                                    shape = shapeOrNull,
                                                    fillColor = if (shapeOrNull == NodeShape.TEXT_BOX) {
                                                        Color.Transparent
                                                    } else {
                                                        parseHexColor(shapeOrNull.defaultFillHex)
                                                    },
                                                    strokeColor = if (studioColors.isDark && shapeOrNull == NodeShape.TEXT_BOX) {
                                                        Color(0xFFE2E8F0)
                                                    } else {
                                                        Color(0xFF161D29)
                                                    },
                                                    size = 36.dp,
                                                )
                                            }
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ShapePickerGridTile(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            iconContent()
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
            )
        }
    }
}

/**
 * Contextual floating action strip with horizontally scrollable 24-shape switcher,
 * font size −/+ stepper, font family picker, and color swatches.
 */
@Composable
fun SelectionContextToolbar(
    state: EditorUiState,
    onEditText: () -> Unit,
    onToggleStylePopover: () -> Unit,
    onConnectFromNode: () -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onUpdateFillColor: (String) -> Unit,
    onUpdateBorderColor: (String) -> Unit,
    onUpdateBorderStyle: (BorderStyle) -> Unit,
    onChangeShape: (NodeShape) -> Unit,
    onChangeFontFamily: (AppFontFamily) -> Unit,
    onAdjustFontSize: (Float) -> Unit,
    onEditEdge: () -> Unit,
    onCycleEdgeStyle: () -> Unit,
    onToggleEdgeDashed: () -> Unit,
    onReverseEdge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val studioColors = LocalFlowStudioColors.current
    val hasNodes = state.selectedNodeIds.isNotEmpty()
    val hasEdge = state.selectedEdge != null

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(
            visible = hasNodes && state.showStylePopover,
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut() + scaleOut(targetScale = 0.95f),
        ) {
            val primary = state.primarySelectedNode
            Surface(
                color = studioColors.floatingPillSurface,
                border = BorderStroke(1.dp, studioColors.floatingPillBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(324.dp)
                    .heightIn(max = 390.dp)
                    .shadow(10.dp, RoundedCornerShape(20.dp)),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Change Shape", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        NodeShape.entries.forEach { shape ->
                            val active = primary?.shape == shape
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                    .border(
                                        width = if (active) 1.5.dp else 0.dp,
                                        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable { onChangeShape(shape) }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .testTag("change_shape_${shape.name.lowercase()}"),
                            ) {
                                ShapeGlyphIcon(
                                    shape = shape,
                                    fillColor = if (shape == NodeShape.TEXT_BOX) {
                                        Color.Transparent
                                    } else {
                                        parseHexColor(primary?.fillColorHex ?: shape.defaultFillHex)
                                    },
                                    strokeColor = Color(0xFF161D29),
                                    size = 26.dp,
                                )
                                Text(
                                    text = shape.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = studioColors.floatingPillBorder)

                    // Typography quick controls: Font Size -/+ stepper + Font Family chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Font Size", style = MaterialTheme.typography.labelLarge)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onAdjustFontSize(-2f) }
                                    .testTag("style_font_size_minus"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease font size", modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                text = "${(primary?.fontSizeSp ?: 14f).roundToInt()}sp",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(42.dp),
                                textAlign = TextAlign.Center,
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onAdjustFontSize(2f) }
                                    .testTag("style_font_size_plus"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase font size", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AppFontFamily.entries.forEach { font ->
                            val selected = primary?.fontFamily == font
                            FilterChip(
                                selected = selected,
                                onClick = { onChangeFontFamily(font) },
                                label = {
                                    Text(
                                        text = font.label,
                                        fontFamily = font.toComposeFontFamily(),
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }

                    HorizontalDivider(color = studioColors.floatingPillBorder)

                    Text("Fill Color", style = MaterialTheme.typography.labelLarge)
                    SwatchRow(
                        colors = NodeSwatches.PastelColors,
                        selectedHex = primary?.fillColorHex,
                        onSelect = onUpdateFillColor,
                    )
                    SwatchRow(
                        colors = NodeSwatches.VividColors,
                        selectedHex = primary?.fillColorHex,
                        onSelect = onUpdateFillColor,
                    )

                    HorizontalDivider(color = studioColors.floatingPillBorder)

                    Text("Border Color & Style", style = MaterialTheme.typography.labelLarge)
                    SwatchRow(
                        colors = NodeSwatches.BorderColors,
                        selectedHex = primary?.borderColorHex,
                        onSelect = onUpdateBorderColor,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BorderStyle.entries.forEach { style ->
                            val active = primary?.borderStyle == style
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onUpdateBorderStyle(style) },
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 7.dp),
                                ) {
                                    Text(
                                        text = style.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compact Floating Action Pill (Node Selection)
        AnimatedVisibility(visible = hasNodes) {
            Surface(
                color = studioColors.floatingPillSurface,
                border = BorderStroke(1.dp, studioColors.floatingPillBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(6.dp, RoundedCornerShape(16.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.selectedNodeIds.size > 1) {
                        Text(
                            text = "${state.selectedNodeIds.size} selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    if (state.selectedNodeIds.size == 1) {
                        IconButton(
                            onClick = onEditText,
                            modifier = Modifier.testTag("action_edit_node_text"),
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit text")
                        }
                    }
                    IconButton(
                        onClick = onToggleStylePopover,
                        modifier = Modifier.testTag("action_node_style"),
                    ) {
                        Icon(
                            Icons.Filled.Palette,
                            contentDescription = "Colors, font and shape",
                            tint = if (state.showStylePopover) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (state.selectedNodeIds.size == 1) {
                        IconButton(
                            onClick = onConnectFromNode,
                            modifier = Modifier.testTag("action_connect_node"),
                        ) {
                            Icon(Icons.Filled.AccountTree, contentDescription = "Connect arrow")
                        }
                    }
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.testTag("action_copy_node"),
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.testTag("action_duplicate_node"),
                    ) {
                        Icon(Icons.Filled.FileCopy, contentDescription = "Duplicate")
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("action_delete_selected"),
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // Compact Floating Action Pill (Edge Selection)
        AnimatedVisibility(visible = hasEdge) {
            val edge = state.selectedEdge
            Surface(
                color = studioColors.floatingPillSurface,
                border = BorderStroke(1.dp, studioColors.floatingPillBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(6.dp, RoundedCornerShape(16.dp)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onEditEdge,
                        modifier = Modifier.testTag("action_edit_edge"),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit arrow label")
                    }
                    IconButton(onClick = onCycleEdgeStyle) {
                        Icon(Icons.Filled.Category, contentDescription = "Switch arrow curve style")
                    }
                    IconButton(onClick = onToggleEdgeDashed) {
                        Icon(
                            Icons.Filled.BorderStyle,
                            contentDescription = "Toggle dashed arrow",
                            tint = if (edge?.isDashed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onReverseEdge) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Reverse arrow direction")
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("action_delete_edge"),
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete arrow",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwatchRow(
    colors: List<String>,
    selectedHex: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        colors.forEach { hex ->
            val isSelected = selectedHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(hex))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x440F172A),
                        shape = CircleShape,
                    )
                    .clickable { onSelect(hex) },
            )
        }
    }
}

@Composable
fun BottomZoomAndHistoryBar(
    zoom: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    hasClipboard: Boolean,
    snapToGrid: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPaste: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onFitToScreen: () -> Unit,
    onToggleSnap: () -> Unit,
    onCycleGridStyle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val studioColors = LocalFlowStudioColors.current

    Surface(
        color = studioColors.floatingPillSurface,
        border = BorderStroke(1.dp, studioColors.floatingPillBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.shadow(5.dp, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.testTag("editor_undo_button"),
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.testTag("editor_redo_button"),
            ) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
            }
            if (hasClipboard) {
                IconButton(
                    onClick = onPaste,
                    modifier = Modifier.testTag("editor_paste_button"),
                ) {
                    Icon(
                        Icons.Filled.ContentPaste,
                        contentDescription = "Paste shapes",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(studioColors.floatingPillBorder)
            )

            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.testTag("editor_zoom_out"),
            ) {
                Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out")
            }
            Text(
                text = "${(zoom * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.testTag("editor_zoom_in"),
            ) {
                Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in")
            }
            IconButton(
                onClick = onFitToScreen,
                modifier = Modifier.testTag("editor_fit_screen"),
            ) {
                Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Fit diagram to screen")
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(studioColors.floatingPillBorder)
            )

            IconButton(
                onClick = onToggleSnap,
                modifier = Modifier.testTag("editor_toggle_snap"),
            ) {
                Icon(
                    Icons.Filled.GridOn,
                    contentDescription = "Toggle snap to grid",
                    tint = if (snapToGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onCycleGridStyle,
                modifier = Modifier.testTag("editor_cycle_grid"),
            ) {
                Icon(Icons.Filled.Grid4x4, contentDescription = "Switch grid pattern")
            }
        }
    }
}
