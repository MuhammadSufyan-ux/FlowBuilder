package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.NodeShape
import com.example.ui.theme.FlowEmerald500
import com.example.ui.theme.FlowTeal600

@Composable
fun FlowBuilderLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawRoundRect(
            color = Color(0xFF064E3B),
            topLeft = Offset.Zero,
            size = this.size,
            cornerRadius = CornerRadius(w * 0.28f, w * 0.28f),
        )

        val lineStroke = Stroke(width = w * 0.065f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(
            color = Color.White,
            start = Offset(w * 0.5f, h * 0.30f),
            end = Offset(w * 0.5f, h * 0.43f),
            strokeWidth = w * 0.065f,
            cap = StrokeCap.Round,
        )
        val branchPath = Path().apply {
            moveTo(w * 0.28f, h * 0.68f)
            lineTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.72f, h * 0.52f)
            lineTo(w * 0.72f, h * 0.68f)
        }
        drawPath(branchPath, color = Color.White, style = lineStroke)

        drawRoundRect(
            color = Color(0xFF86EFAC),
            topLeft = Offset(w * 0.30f, h * 0.14f),
            size = Size(w * 0.40f, h * 0.16f),
            cornerRadius = CornerRadius(h * 0.08f, h * 0.08f),
        )

        val diamond = Path().apply {
            moveTo(w * 0.50f, h * 0.39f)
            lineTo(w * 0.64f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.65f)
            lineTo(w * 0.36f, h * 0.52f)
            close()
        }
        drawPath(diamond, color = Color(0xFF2DD4BF))

        drawRoundRect(
            color = Color(0xFFFCD34D),
            topLeft = Offset(w * 0.16f, h * 0.68f),
            size = Size(w * 0.24f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
        )

        drawRoundRect(
            color = Color(0xFFFB7185),
            topLeft = Offset(w * 0.60f, h * 0.68f),
            size = Size(w * 0.24f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
        )
    }
}

/**
 * Stylized geometric "F" ribbon mark matching FlowBuilder brand identity in Splash, Auth, and Drawer.
 */
@Composable
fun FlowBuilderBrandMarkF(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    tintLight: Boolean = false,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val mainBrush = if (tintLight) {
            Brush.linearGradient(
                colors = listOf(Color.White, Color(0xFFCCFBF1)),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(FlowEmerald500, FlowTeal600, Color(0xFF065F46)),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            )
        }

        // Main sweeping stem + top bar of stylized F
        val stemPath = Path().apply {
            moveTo(w * 0.20f, h * 0.92f)
            lineTo(w * 0.20f, h * 0.32f)
            quadraticTo(w * 0.20f, h * 0.08f, w * 0.44f, h * 0.08f)
            lineTo(w * 0.86f, h * 0.08f)
            quadraticTo(w * 0.84f, h * 0.30f, w * 0.62f, h * 0.30f)
            lineTo(w * 0.44f, h * 0.30f)
            quadraticTo(w * 0.41f, h * 0.30f, w * 0.41f, h * 0.34f)
            lineTo(w * 0.41f, h * 0.72f)
            quadraticTo(w * 0.41f, h * 0.90f, w * 0.20f, h * 0.92f)
            close()
        }
        drawPath(stemPath, brush = mainBrush)

        // Lower leaf/wing bar of F
        val midPath = Path().apply {
            moveTo(w * 0.46f, h * 0.62f)
            quadraticTo(w * 0.46f, h * 0.42f, w * 0.65f, h * 0.42f)
            lineTo(w * 0.85f, h * 0.42f)
            quadraticTo(w * 0.82f, h * 0.63f, w * 0.62f, h * 0.63f)
            lineTo(w * 0.46f, h * 0.62f)
            close()
        }
        drawPath(midPath, brush = mainBrush)
    }
}

@Composable
fun ShapeGlyphIcon(
    shape: NodeShape,
    fillColor: Color,
    strokeColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val pad = 3.dp.toPx()
        val fullW = this.size.width - pad * 2
        val fullH = this.size.height - pad * 2
        val stroke = Stroke(width = 1.7.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)

        val isTallOrSquare = shape in setOf(
            NodeShape.DIAMOND,
            NodeShape.DATABASE,
            NodeShape.CONNECTOR,
            NodeShape.OFF_PAGE_CONNECTOR,
            NodeShape.MERGE,
            NodeShape.OR_GATE,
            NodeShape.SUMMING_JUNCTION,
            NodeShape.DOCUMENT,
            NodeShape.MULTIPLE_DOCUMENTS,
            NodeShape.TEXT_BOX,
        )
        val drawH = if (isTallOrSquare) fullH * 0.90f else fullH * 0.70f
        val drawW = if (shape in setOf(NodeShape.CONNECTOR, NodeShape.OR_GATE, NodeShape.SUMMING_JUNCTION)) {
            drawH
        } else {
            fullW
        }
        val left = (this.size.width - drawW) / 2f
        val top = (this.size.height - drawH) / 2f

        drawFlowNodeShape(
            shape = shape,
            x = left,
            y = top,
            w = drawW,
            h = drawH,
            fillColor = fillColor,
            borderColor = strokeColor,
            stroke = stroke,
            isIconPreview = true,
        )
    }
}
