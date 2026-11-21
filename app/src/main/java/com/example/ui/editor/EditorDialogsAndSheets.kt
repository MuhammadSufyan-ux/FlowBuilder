package com.example.ui.editor

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AiConfigurationState
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AppFontFamily
import com.example.domain.model.EdgeStyle
import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowNode
import com.example.domain.model.LayoutDirection
import com.example.domain.model.ValidationIssue
import com.example.domain.model.ValidationSeverity
import com.example.ui.theme.FlowTeal600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFlowStudioColors
import com.example.ui.theme.NodeSwatches
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.toComposeFontFamily
import kotlin.math.roundToInt

@Composable
fun EditNodeTextDialog(
    node: FlowNode,
    onDismiss: () -> Unit,
    onConfirm: (
        text: String,
        fontSizeSp: Float,
        isBold: Boolean,
        textColorHex: String,
        fontFamily: AppFontFamily,
    ) -> Unit,
) {
    var text by remember(node.id) { mutableStateOf(node.text) }
    var fontSize by remember(node.id) { mutableFloatStateOf(node.fontSizeSp) }
    var isBold by remember(node.id) { mutableStateOf(node.isBold) }
    var textColorHex by remember(node.id) { mutableStateOf(node.textColorHex) }
    var fontFamily by remember(node.id) { mutableStateOf(node.fontFamily) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${node.shape.displayName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text content") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_node_text_input"),
                )

                // Font size - / + stepper (NO slider!) + Bold switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Font Size", style = MaterialTheme.typography.labelLarge)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { fontSize = (fontSize - 2f).coerceAtLeast(10f) }
                                    .testTag("dialog_font_size_minus"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease font size", modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                text = "${fontSize.roundToInt()}sp",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(46.dp),
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { fontSize = (fontSize + 2f).coerceAtMost(32f) }
                                    .testTag("dialog_font_size_plus"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase font size", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bold", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = isBold, onCheckedChange = { isBold = it })
                    }
                }

                Text("Font Family", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppFontFamily.entries.forEach { family ->
                        FilterChip(
                            selected = fontFamily == family,
                            onClick = { fontFamily = family },
                            label = {
                                Text(
                                    text = family.label,
                                    fontFamily = family.toComposeFontFamily(),
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }

                Text("Text Color", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NodeSwatches.TextColors.forEach { hex ->
                        val selected = textColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color(0x440F172A),
                                    shape = CircleShape,
                                )
                                .clickable { textColorHex = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text, fontSize, isBold, textColorHex, fontFamily) },
                modifier = Modifier.testTag("save_node_text_button"),
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun EditEdgeDialog(
    edge: FlowEdge,
    onDismiss: () -> Unit,
    onConfirm: (label: String, style: EdgeStyle, strokeColorHex: String, isDashed: Boolean) -> Unit,
) {
    var label by remember(edge.id) { mutableStateOf(edge.label) }
    var style by remember(edge.id) { mutableStateOf(edge.style) }
    var strokeHex by remember(edge.id) { mutableStateOf(edge.strokeColorHex) }
    var isDashed by remember(edge.id) { mutableStateOf(edge.isDashed) }

    val quickLabels = listOf("Yes", "No", "True", "False", "Retry", "Loop", "Next", "")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Arrow Connection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    label = { Text("Arrow label (optional)") },
                    placeholder = { Text("e.g., Yes, No, Valid") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_edge_label_input"),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    quickLabels.forEach { preset ->
                        FilterChip(
                            selected = label == preset,
                            onClick = { label = preset },
                            label = { Text(preset.ifEmpty { "None" }) },
                        )
                    }
                }

                Text("Routing Style", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EdgeStyle.entries.forEach { option ->
                        FilterChip(
                            selected = style == option,
                            onClick = { style = option },
                            label = { Text(option.label, maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Dashed Line", style = MaterialTheme.typography.labelLarge)
                    Switch(checked = isDashed, onCheckedChange = { isDashed = it })
                }

                Text("Arrow Color", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NodeSwatches.BorderColors.take(6).forEach { hex ->
                        val selected = strokeHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color(0x440F172A),
                                    shape = CircleShape,
                                )
                                .clickable { strokeHex = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label.trim(), style, strokeHex, isDashed) },
                modifier = Modifier.testTag("save_edge_button"),
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoLayoutBottomSheet(
    onDismiss: () -> Unit,
    onSelectLayout: (LayoutDirection) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Smart Auto-Arrange",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Automatically organize connected nodes and branching paths without overlapping.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LayoutDirection.entries.forEach { direction ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectLayout(direction) }
                        .testTag("layout_option_${direction.name.lowercase()}"),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.AccountTree,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(direction.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                direction.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartValidationBottomSheet(
    issues: List<ValidationIssue>,
    showHighlights: Boolean,
    onToggleHighlights: () -> Unit,
    onFocusNode: (String) -> Unit,
    onExplainErrorsWithAi: () -> Unit,
    onDismiss: () -> Unit,
) {
    val studioColors = LocalFlowStudioColors.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Smart Diagram Validator",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (issues.isEmpty()) "All structural checks passed" else "${issues.size} item(s) detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Badges", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(6.dp))
                    Switch(checked = showHighlights, onCheckedChange = { onToggleHighlights() })
                }
            }

            if (issues.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = studioColors.successColor.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = studioColors.successColor,
                            modifier = Modifier.size(30.dp),
                        )
                        Column {
                            Text("Flowchart looks structurally sound!", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Start and End nodes are valid, decisions have branches, and no shapes are disconnected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                ) {
                    items(issues, key = { it.id }) { issue ->
                        val (icon, accent) = when (issue.severity) {
                            ValidationSeverity.ERROR -> Icons.Filled.Error to studioColors.errorColor
                            ValidationSeverity.WARNING -> Icons.Filled.Warning to studioColors.warningColor
                            ValidationSeverity.INFO -> Icons.Filled.Info to MaterialTheme.colorScheme.primary
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = accent.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = issue.nodeId != null) {
                                    issue.nodeId?.let(onFocusNode)
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(issue.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        issue.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (issue.nodeId != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Tap to locate on canvas",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onExplainErrorsWithAi,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("validation_explain_with_ai_button"),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (issues.isEmpty()) "Review Workflow with AI" else "Explain & Fix Errors with AI",
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

/**
 * Unified AI Workflow Assistant Bottom Sheet inside the Flowchart Editor:
 * - Suggest workflow improvements
 * - Explain workflow errors
 * - Understand nodes & connections
 * - Generate & append new workflow steps onto the canvas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWorkflowAssistantBottomSheet(
    aiConfig: AiConfigurationState,
    focusedNodeText: String?,
    issueCount: Int,
    isBusy: Boolean,
    outputTitle: String,
    outputText: String,
    errorMessage: String?,
    onSuggestImprovements: () -> Unit,
    onExplainErrors: () -> Unit,
    onExplainNodesAndConnections: () -> Unit,
    onGenerateSteps: (String) -> Unit,
    onCopied: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var stepsPrompt by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = { if (!isBusy) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = FlowTeal600,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "AI Workflow Assistant",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = when {
                                aiConfig.status == AiConnectionStatus.CONNECTED && aiConfig.provider != null ->
                                    "Connected • ${aiConfig.provider.displayName}"
                                aiConfig.hasApiKey ->
                                    "AI Key • ${aiConfig.status.label}"
                                else ->
                                    "FlowBuilder Built-in AI"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = FlowTeal600,
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    enabled = !isBusy,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close AI Assistant")
                }
            }

            // Quick AI workflow analysis actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiActionCard(
                    icon = Icons.Filled.Lightbulb,
                    title = "Suggest\nImprovements",
                    enabled = !isBusy,
                    onClick = onSuggestImprovements,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_ai_suggest_improvements"),
                )
                AiActionCard(
                    icon = Icons.Filled.Warning,
                    title = if (issueCount > 0) "Explain\n$issueCount Error(s)" else "Explain\nErrors",
                    enabled = !isBusy,
                    onClick = onExplainErrors,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_ai_explain_errors"),
                )
                AiActionCard(
                    icon = Icons.Filled.HelpOutline,
                    title = if (focusedNodeText != null) "Explain\nSelected" else "Understand\nFlow",
                    enabled = !isBusy,
                    onClick = onExplainNodesAndConnections,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_ai_understand_nodes"),
                )
            }

            // Generate & append workflow steps onto canvas
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Generate Workflow Steps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = stepsPrompt,
                        onValueChange = { stepsPrompt = it },
                        enabled = !isBusy,
                        minLines = 2,
                        maxLines = 3,
                        placeholder = { Text("e.g., Add OTP verification and retry limit branch") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_ai_steps_prompt"),
                    )
                    Button(
                        onClick = {
                            onGenerateSteps(stepsPrompt)
                            stepsPrompt = ""
                        },
                        enabled = !isBusy && stepsPrompt.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_ai_generate_steps_button"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Generate & Add Steps to Canvas", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (isBusy) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.4.dp,
                            color = FlowTeal600,
                        )
                        Text(
                            text = "Analyzing flowchart with AI…",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (outputText.isNotBlank() && !isBusy) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = outputTitle.ifBlank { "AI Insights" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FlowTeal600,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(outputText))
                                    onCopied()
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Copy AI response",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Text(
                            text = outputText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun AiActionCard(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (enabled) 0.48f else 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FlowTeal600,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportAndShareBottomSheet(
    projectTitle: String,
    defaultDarkExport: Boolean,
    onDismiss: () -> Unit,
    onSavePng: (Boolean) -> Unit,
    onSharePng: (Boolean) -> Unit,
    onSavePdf: (Boolean) -> Unit,
    onSharePdf: (Boolean) -> Unit,
    onSaveHtml: (Boolean) -> Unit,
    onShareHtml: (Boolean) -> Unit,
    onSaveJson: () -> Unit,
    onShareJson: () -> Unit,
) {
    var darkCanvasExport by remember { mutableStateOf(defaultDarkExport) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Export & Share",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Save \"$projectTitle\" to your device or share directly in high quality.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Dark Canvas Theme",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Switch(checked = darkCanvasExport, onCheckedChange = { darkCanvasExport = it })
                }
            }

            ExportOptionRow(
                icon = Icons.Filled.Image,
                title = "PNG Image (2x HD)",
                subtitle = "Crisp transparent-free diagram image",
                onSave = { onSavePng(darkCanvasExport) },
                onShare = { onSharePng(darkCanvasExport) },
                saveTag = "export_save_png",
                shareTag = "export_share_png",
            )

            ExportOptionRow(
                icon = Icons.Filled.PictureAsPdf,
                title = "PDF Document",
                subtitle = "Print-ready document with header",
                onSave = { onSavePdf(darkCanvasExport) },
                onShare = { onSharePdf(darkCanvasExport) },
                saveTag = "export_save_pdf",
                shareTag = "export_share_pdf",
            )

            ExportOptionRow(
                icon = Icons.Filled.Language,
                title = "HTML Web Page (.html)",
                subtitle = "Standalone interactive SVG webpage",
                onSave = { onSaveHtml(darkCanvasExport) },
                onShare = { onShareHtml(darkCanvasExport) },
                saveTag = "export_save_html",
                shareTag = "export_share_html",
            )

            ExportOptionRow(
                icon = Icons.Filled.DataObject,
                title = "FlowBuilder JSON",
                subtitle = "Portable project backup for re-import",
                onSave = onSaveJson,
                onShare = onShareJson,
                saveTag = "export_save_json",
                shareTag = "export_share_json",
            )

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ExportOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onSave: () -> Unit,
    onShare: () -> Unit,
    saveTag: String,
    shareTag: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(saveTag),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Save",
                        maxLines = 1,
                        softWrap = false,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(shareTag),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        maxLines = 1,
                        softWrap = false,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * Live Diagram Code Inspector supporting Pseudocode, Flow DSL, and JSON!
 */
@Composable
fun DiagramCodeInspectorPanel(
    pseudocode: String,
    mermaidCode: String,
    jsonCode: String,
    onClose: () -> Unit,
    onCopied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val studioColors = LocalFlowStudioColors.current
    val clipboard = LocalClipboardManager.current
    // 0 = Pseudocode, 1 = Flow DSL, 2 = JSON
    var selectedTab by remember { mutableIntStateOf(0) }

    val activeText = when (selectedTab) {
        0 -> pseudocode
        1 -> mermaidCode
        else -> jsonCode
    }
    val headerTitle = when (selectedTab) {
        0 -> "Pseudocode"
        1 -> "Flow DSL"
        else -> "Project JSON"
    }
    val headerIcon = when (selectedTab) {
        0 -> Icons.Filled.Terminal
        1 -> Icons.Filled.Code
        else -> Icons.Filled.Description
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = studioColors.codeBackground,
        border = BorderStroke(1.dp, studioColors.floatingPillBorder),
        modifier = modifier.shadow(10.dp, RoundedCornerShape(20.dp)),
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(studioColors.codeHeader)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        headerIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(activeText))
                            onCopied()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(17.dp))
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close code panel", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Pseudocode", maxLines = 1, softWrap = false, fontSize = 12.sp) },
                    modifier = Modifier.testTag("code_tab_pseudocode"),
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Flow DSL", maxLines = 1, softWrap = false, fontSize = 12.sp) },
                    modifier = Modifier.testTag("code_tab_dsl"),
                )
                FilterChip(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text("JSON", maxLines = 1, softWrap = false, fontSize = 12.sp) },
                    modifier = Modifier.testTag("code_tab_json"),
                )
            }

            HorizontalDivider(color = studioColors.floatingPillBorder)

            val lines = activeText.lines()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                lines.forEachIndexed { idx, line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "${idx + 1}".padStart(2, ' '),
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                        Text(
                            text = colorizeCodeLine(line, selectedTab, studioColors.isDark),
                            fontFamily = JetBrainsMono,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun colorizeCodeLine(line: String, mode: Int, isDark: Boolean): AnnotatedString {
    val keywordColor = if (isDark) Color(0xFF34D399) else Color(0xFF0D9488)
    val arrowColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val stringColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
    val styleColor = if (isDark) Color(0xFFF472B6) else Color(0xFFBE185D)
    val commentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val defaultColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)

    return buildAnnotatedString {
        val trimmed = line.trimStart()
        when (mode) {
            0 -> {
                // Pseudocode highlighting
                when {
                    trimmed.startsWith("//") ->
                        withStyle(SpanStyle(color = commentColor)) { append(line) }
                    trimmed.startsWith("ALGORITHM") || trimmed.startsWith("BEGIN") || trimmed.startsWith("END") ->
                        withStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) { append(line) }
                    trimmed.startsWith("IF ") || trimmed.startsWith("ELSE") || trimmed.startsWith("ENDIF") ||
                        trimmed.startsWith("LOOP ") || trimmed.startsWith("ENDLOOP") || trimmed.startsWith("GOTO ") ->
                        withStyle(SpanStyle(color = arrowColor, fontWeight = FontWeight.SemiBold)) { append(line) }
                    trimmed.startsWith("CALL ") || trimmed.startsWith("READ ") || trimmed.startsWith("WRITE ") ||
                        trimmed.startsWith("DISPLAY ") || trimmed.startsWith("WAIT ") || trimmed.startsWith("INIT ") ->
                        withStyle(SpanStyle(color = styleColor, fontWeight = FontWeight.SemiBold)) { append(line) }
                    else ->
                        withStyle(SpanStyle(color = defaultColor)) { append(line) }
                }
            }
            1 -> {
                // Flow DSL highlighting
                when {
                    trimmed.startsWith("flowchart") ->
                        withStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) { append(line) }
                    trimmed.startsWith("style ") ->
                        withStyle(SpanStyle(color = styleColor)) { append(line) }
                    line.contains("-->") || line.contains("-.->") -> {
                        val sep = if (line.contains("-.->")) "-.->" else "-->"
                        val parts = line.split(sep, limit = 2)
                        withStyle(SpanStyle(color = stringColor)) { append(parts[0]) }
                        withStyle(SpanStyle(color = arrowColor, fontWeight = FontWeight.Bold)) { append(sep) }
                        if (parts.size > 1) {
                            withStyle(SpanStyle(color = stringColor)) { append(parts[1]) }
                        }
                    }
                    else -> withStyle(SpanStyle(color = defaultColor)) { append(line) }
                }
            }
            else -> {
                // JSON highlighting
                if (line.contains(":")) {
                    val idx = line.indexOf(":")
                    withStyle(SpanStyle(color = keywordColor)) { append(line.substring(0, idx)) }
                    withStyle(SpanStyle(color = defaultColor)) { append(":") }
                    withStyle(SpanStyle(color = stringColor)) { append(line.substring(idx + 1)) }
                } else {
                    withStyle(SpanStyle(color = defaultColor)) { append(line) }
                }
            }
        }
    }
}
