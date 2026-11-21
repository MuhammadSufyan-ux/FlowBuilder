package com.example.domain.model

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class ShapeCategory(val title: String) {
    CORE("Core Shapes"),
    PROCESS_CONTROL("Process & Control"),
    DATA_STORAGE("Data, Document & Storage"),
    CONNECTORS("Connectors & Routing"),
}

enum class AppFontFamily(val label: String) {
    PLUS_JAKARTA_SANS("Jakarta Sans"),
    INTER("Inter"),
    OUTFIT("Outfit"),
    SPACE_GROTESK("Space Grotesk"),
    MERRIWEATHER("Merriweather"),
    JETBRAINS_MONO("JetBrains Mono"),
}

enum class NodeShape(
    val displayName: String,
    val category: ShapeCategory,
    val defaultWidth: Float,
    val defaultHeight: Float,
    val defaultFillHex: String,
    val defaultText: String,
) {
    // --- Core Shapes ---
    OVAL(
        displayName = "Terminator",
        category = ShapeCategory.CORE,
        defaultWidth = 148f,
        defaultHeight = 62f,
        defaultFillHex = "#BEF6AC",
        defaultText = "Start",
    ),
    RECTANGLE(
        displayName = "Process",
        category = ShapeCategory.CORE,
        defaultWidth = 176f,
        defaultHeight = 74f,
        defaultFillHex = "#F1F0D4",
        defaultText = "Process",
    ),
    DIAMOND(
        displayName = "Decision",
        category = ShapeCategory.CORE,
        defaultWidth = 164f,
        defaultHeight = 110f,
        defaultFillHex = "#A3E9CC",
        defaultText = "Condition?",
    ),
    PARALLELOGRAM(
        displayName = "Data I/O",
        category = ShapeCategory.CORE,
        defaultWidth = 180f,
        defaultHeight = 72f,
        defaultFillHex = "#D4EFF0",
        defaultText = "Input / Output",
    ),
    TEXT_BOX(
        displayName = "Text Box",
        category = ShapeCategory.CORE,
        defaultWidth = 170f,
        defaultHeight = 52f,
        defaultFillHex = "#00FFFFFF",
        defaultText = "Add note…",
    ),

    // --- Process & Control Shapes ---
    SUBROUTINE(
        displayName = "Predefined Process",
        category = ShapeCategory.PROCESS_CONTROL,
        defaultWidth = 184f,
        defaultHeight = 74f,
        defaultFillHex = "#E5D9FF",
        defaultText = "Subroutine",
    ),
    PREPARATION(
        displayName = "Preparation",
        category = ShapeCategory.PROCESS_CONTROL,
        defaultWidth = 176f,
        defaultHeight = 74f,
        defaultFillHex = "#FFE4BA",
        defaultText = "Initialize",
    ),
    MANUAL_OPERATION(
        displayName = "Manual Operation",
        category = ShapeCategory.PROCESS_CONTROL,
        defaultWidth = 178f,
        defaultHeight = 74f,
        defaultFillHex = "#F1F0D4",
        defaultText = "Manual Step",
    ),
    MANUAL_INPUT(
        displayName = "Manual Input",
        category = ShapeCategory.PROCESS_CONTROL,
        defaultWidth = 178f,
        defaultHeight = 74f,
        defaultFillHex = "#D4EFF0",
        defaultText = "User Input",
    ),
    DELAY(
        displayName = "Delay",
        category = ShapeCategory.PROCESS_CONTROL,
        defaultWidth = 164f,
        defaultHeight = 72f,
        defaultFillHex = "#FFE4BA",
        defaultText = "Wait",
    ),
    LOOP_LIMIT(
        displayName = "Loop Limit",
        category = ShapeCategory.PROCESS_CONTROL,
        defaultWidth = 176f,
        defaultHeight = 72f,
        defaultFillHex = "#A3E9CC",
        defaultText = "Loop Start",
    ),
    ALTERNATE_PROCESS(
        displayName = "Alternate Process",
        category = ShapeCategory.PROCESS_CONTROL,
        defaultWidth = 176f,
        defaultHeight = 74f,
        defaultFillHex = "#D4EFF0",
        defaultText = "Alt Process",
    ),

    // --- Data, Document & Storage Shapes ---
    DOCUMENT(
        displayName = "Document",
        category = ShapeCategory.DATA_STORAGE,
        defaultWidth = 172f,
        defaultHeight = 82f,
        defaultFillHex = "#FFFFFF",
        defaultText = "Document",
    ),
    MULTIPLE_DOCUMENTS(
        displayName = "Multiple Docs",
        category = ShapeCategory.DATA_STORAGE,
        defaultWidth = 178f,
        defaultHeight = 88f,
        defaultFillHex = "#FFFFFF",
        defaultText = "Documents",
    ),
    DATABASE(
        displayName = "Database",
        category = ShapeCategory.DATA_STORAGE,
        defaultWidth = 148f,
        defaultHeight = 96f,
        defaultFillHex = "#D4EFF0",
        defaultText = "Database",
    ),
    STORED_DATA(
        displayName = "Stored Data",
        category = ShapeCategory.DATA_STORAGE,
        defaultWidth = 172f,
        defaultHeight = 74f,
        defaultFillHex = "#E5D9FF",
        defaultText = "Stored Data",
    ),
    INTERNAL_STORAGE(
        displayName = "Internal Storage",
        category = ShapeCategory.DATA_STORAGE,
        defaultWidth = 166f,
        defaultHeight = 80f,
        defaultFillHex = "#F1F0D4",
        defaultText = "Memory",
    ),
    DISPLAY(
        displayName = "Display",
        category = ShapeCategory.DATA_STORAGE,
        defaultWidth = 176f,
        defaultHeight = 74f,
        defaultFillHex = "#D4EFF0",
        defaultText = "Display",
    ),

    // --- Connectors & Routing Shapes ---
    CONNECTOR(
        displayName = "On-Page Connector",
        category = ShapeCategory.CONNECTORS,
        defaultWidth = 64f,
        defaultHeight = 64f,
        defaultFillHex = "#E5D9FF",
        defaultText = "A",
    ),
    OFF_PAGE_CONNECTOR(
        displayName = "Off-Page Connector",
        category = ShapeCategory.CONNECTORS,
        defaultWidth = 86f,
        defaultHeight = 82f,
        defaultFillHex = "#FFE4BA",
        defaultText = "P2",
    ),
    MERGE(
        displayName = "Merge",
        category = ShapeCategory.CONNECTORS,
        defaultWidth = 96f,
        defaultHeight = 82f,
        defaultFillHex = "#A3E9CC",
        defaultText = "Merge",
    ),
    OR_GATE(
        displayName = "Or",
        category = ShapeCategory.CONNECTORS,
        defaultWidth = 64f,
        defaultHeight = 64f,
        defaultFillHex = "#BEF6AC",
        defaultText = "",
    ),
    SUMMING_JUNCTION(
        displayName = "Summing Junction",
        category = ShapeCategory.CONNECTORS,
        defaultWidth = 64f,
        defaultHeight = 64f,
        defaultFillHex = "#FA93B2",
        defaultText = "",
    ),
    ANNOTATION(
        displayName = "Annotation",
        category = ShapeCategory.CONNECTORS,
        defaultWidth = 174f,
        defaultHeight = 66f,
        defaultFillHex = "#F8FAFC",
        defaultText = "Comment",
    ),
}

enum class BorderStyle(val label: String) {
    SOLID("Solid"),
    DASHED("Dashed"),
    DOTTED("Dotted"),
}

enum class EdgeStyle(val label: String) {
    ORTHOGONAL("Orthogonal"),
    CURVED("Curved"),
    STRAIGHT("Straight"),
}

enum class AnchorSide {
    AUTO,
    TOP,
    RIGHT,
    BOTTOM,
    LEFT,
}

enum class GridStyle(val label: String) {
    PLUS("Plus Grid"),
    DOTS("Dot Grid"),
    LINES("Line Grid"),
    NONE("Clean"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class LayoutDirection(val label: String, val subtitle: String) {
    TOP_DOWN("Top to Bottom", "Classic vertical algorithm flow"),
    LEFT_RIGHT("Left to Right", "Horizontal process pipeline"),
    COMPACT_TREE("Compact Tree", "Tight spacing for large diagrams"),
}

enum class EditorTool(val label: String) {
    SELECT("Select"),
    PAN("Pan"),
    CONNECT("Connect"),
    MULTI_SELECT("Multi"),
}

enum class ValidationSeverity {
    ERROR,
    WARNING,
    INFO,
}

@JsonClass(generateAdapter = true)
data class FlowNode(
    val id: String = UUID.randomUUID().toString(),
    val shape: NodeShape = NodeShape.RECTANGLE,
    val text: String = shape.defaultText,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = shape.defaultWidth,
    val height: Float = shape.defaultHeight,
    val fillColorHex: String = shape.defaultFillHex,
    val borderColorHex: String = "#161D29",
    val textColorHex: String = "#0F172A",
    val borderWidth: Float = 2.4f,
    val borderStyle: BorderStyle = BorderStyle.SOLID,
    val fontSizeSp: Float = 14f,
    val isBold: Boolean = true,
    val fontFamily: AppFontFamily = AppFontFamily.PLUS_JAKARTA_SANS,
) {
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
    val right: Float get() = x + width
    val bottom: Float get() = y + height
}

@JsonClass(generateAdapter = true)
data class FlowEdge(
    val id: String = UUID.randomUUID().toString(),
    val fromNodeId: String,
    val toNodeId: String,
    val label: String = "",
    val fromSide: AnchorSide = AnchorSide.AUTO,
    val toSide: AnchorSide = AnchorSide.AUTO,
    val style: EdgeStyle = EdgeStyle.ORTHOGONAL,
    val strokeColorHex: String = "#1E293B",
    val isDashed: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class FlowGraph(
    val nodes: List<FlowNode> = emptyList(),
    val edges: List<FlowEdge> = emptyList(),
    val gridStyle: GridStyle = GridStyle.PLUS,
    val snapToGrid: Boolean = true,
)

data class FlowProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: String = "General",
    val graph: FlowGraph = FlowGraph(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
)

data class FlowTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val badge: String,
    val graph: FlowGraph,
)

data class ValidationIssue(
    val id: String,
    val severity: ValidationSeverity,
    val title: String,
    val description: String,
    val nodeId: String? = null,
    val edgeId: String? = null,
)

data class UserProfileData(
    val userId: String = "",
    val displayName: String = "FlowBuilder User",
    val subtitle: String = "Flowchart Creator",
    val email: String = "",
    val photoUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class FlowProjectExportPayload(
    val schemaVersion: Int = 2,
    val app: String = "FlowBuilder",
    val title: String,
    val description: String = "",
    val category: String = "General",
    val exportedAt: Long = System.currentTimeMillis(),
    val graph: FlowGraph,
)
