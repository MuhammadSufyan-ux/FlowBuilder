package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.domain.model.NodeShape
import kotlin.math.max
import kotlin.math.min

/**
 * Shared Compose DrawScope vector renderer for all 24 standard flowchart shapes.
 * Used identically by FlowCanvasView, FlowPreviewCanvas, and ShapeGlyphIcon.
 */
fun DrawScope.drawFlowNodeShape(
    shape: NodeShape,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    fillColor: Color,
    borderColor: Color,
    stroke: Stroke,
    isIconPreview: Boolean = false,
) {
    val r = x + w
    val b = y + h
    val cx = x + w / 2f
    val cy = y + h / 2f

    when (shape) {
        NodeShape.TEXT_BOX -> {
            if (isIconPreview) {
                // Draw crisp typographic "T" glyph
                val topY = y + h * 0.22f
                val botY = b - h * 0.16f
                val leftX = x + w * 0.18f
                val rightX = r - w * 0.18f
                val sw = stroke.width * 1.15f
                drawLine(borderColor, Offset(leftX, topY), Offset(rightX, topY), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(borderColor, Offset(cx, topY), Offset(cx, botY), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(borderColor, Offset(cx - w * 0.14f, botY), Offset(cx + w * 0.14f, botY), strokeWidth = sw * 0.85f, cap = StrokeCap.Round)
            } else {
                // On canvas, Text Box has optional fill (transparent by default) and subtle dashed frame only if fill is visible
                if (fillColor.alpha > 0.01f) {
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(x, y),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                }
            }
        }

        NodeShape.OVAL -> {
            val cr = CornerRadius(h / 2f, h / 2f)
            drawRoundRect(fillColor, Offset(x, y), Size(w, h), cr)
            drawRoundRect(borderColor, Offset(x, y), Size(w, h), cr, style = stroke)
        }

        NodeShape.RECTANGLE -> {
            val cr = CornerRadius(min(12f, h * 0.22f), min(12f, h * 0.22f))
            drawRoundRect(fillColor, Offset(x, y), Size(w, h), cr)
            drawRoundRect(borderColor, Offset(x, y), Size(w, h), cr, style = stroke)
        }

        NodeShape.ALTERNATE_PROCESS -> {
            val rad = min(w, h) * 0.34f
            val cr = CornerRadius(rad, rad)
            drawRoundRect(fillColor, Offset(x, y), Size(w, h), cr)
            drawRoundRect(borderColor, Offset(x, y), Size(w, h), cr, style = stroke)
        }

        NodeShape.DIAMOND -> {
            val p = Path().apply {
                moveTo(cx, y)
                lineTo(r, cy)
                lineTo(cx, b)
                lineTo(x, cy)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.PARALLELOGRAM -> {
            val slant = min(22f, w * 0.18f)
            val p = Path().apply {
                moveTo(x + slant, y)
                lineTo(r, y)
                lineTo(r - slant, b)
                lineTo(x, b)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.SUBROUTINE -> {
            val cr = CornerRadius(min(8f, h * 0.14f), min(8f, h * 0.14f))
            val inset = max(w * 0.13f, if (isIconPreview) 3.5f else 12f)
            drawRoundRect(fillColor, Offset(x, y), Size(w, h), cr)
            drawRoundRect(borderColor, Offset(x, y), Size(w, h), cr, style = stroke)
            drawLine(borderColor, Offset(x + inset, y), Offset(x + inset, b), strokeWidth = stroke.width)
            drawLine(borderColor, Offset(r - inset, y), Offset(r - inset, b), strokeWidth = stroke.width)
        }

        NodeShape.PREPARATION -> {
            val dx = min(24f, w * 0.20f)
            val p = Path().apply {
                moveTo(x + dx, y)
                lineTo(r - dx, y)
                lineTo(r, cy)
                lineTo(r - dx, b)
                lineTo(x + dx, b)
                lineTo(x, cy)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.MANUAL_OPERATION -> {
            val dx = min(22f, w * 0.16f)
            val p = Path().apply {
                moveTo(x, y)
                lineTo(r, y)
                lineTo(r - dx, b)
                lineTo(x + dx, b)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.MANUAL_INPUT -> {
            val drop = min(20f, h * 0.26f)
            val p = Path().apply {
                moveTo(x, y + drop)
                lineTo(r, y)
                lineTo(r, b)
                lineTo(x, b)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.DELAY -> {
            val rad = h / 2f
            val p = Path().apply {
                moveTo(x, y)
                lineTo(r - rad, y)
                arcTo(
                    rect = Rect(r - rad * 2f, y, r, b),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false,
                )
                lineTo(x, b)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.LOOP_LIMIT -> {
            val cut = min(22f, min(w, h) * 0.28f)
            val p = Path().apply {
                moveTo(x + cut, y)
                lineTo(r - cut, y)
                lineTo(r, y + cut)
                lineTo(r, b)
                lineTo(x, b)
                lineTo(x, y + cut)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.DOCUMENT -> {
            val waveH = min(14f, h * 0.20f)
            val p = Path().apply {
                moveTo(x, y)
                lineTo(r, y)
                lineTo(r, b - waveH)
                quadraticTo(x + w * 0.75f, b - waveH * 2.2f, cx, b - waveH)
                quadraticTo(x + w * 0.25f, b + waveH * 0.5f, x, b - waveH * 0.4f)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.MULTIPLE_DOCUMENTS -> {
            val step = if (isIconPreview) min(w, h) * 0.11f else 6f
            val waveH = min(12f, h * 0.16f)
            val fw = w - step * 2f
            val fh = h - step * 2f

            // Back two pages
            drawRect(fillColor, Offset(x + step * 2f, y), Size(fw, fh - waveH * 0.3f))
            drawRect(borderColor, Offset(x + step * 2f, y), Size(fw, fh - waveH * 0.3f), style = stroke)
            drawRect(fillColor, Offset(x + step, y + step), Size(fw, fh - waveH * 0.3f))
            drawRect(borderColor, Offset(x + step, y + step), Size(fw, fh - waveH * 0.3f), style = stroke)

            // Front wavy page
            val p = Path().apply {
                moveTo(x, y + step * 2f)
                lineTo(x + fw, y + step * 2f)
                lineTo(x + fw, b - waveH)
                quadraticTo(x + fw * 0.75f, b - waveH * 2f, x + fw * 0.5f, b - waveH)
                quadraticTo(x + fw * 0.25f, b + waveH * 0.4f, x, b - waveH * 0.4f)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.DATABASE -> {
            val ry = min(14f, h * 0.19f)
            val body = Path().apply {
                moveTo(x, y + ry)
                lineTo(x, b - ry)
                arcTo(
                    rect = Rect(x, b - ry * 2f, r, b),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false,
                )
                lineTo(r, y + ry)
                close()
            }
            drawPath(body, fillColor)
            drawPath(body, borderColor, style = stroke)
            drawOval(fillColor, Offset(x, y), Size(w, ry * 2f))
            drawOval(borderColor, Offset(x, y), Size(w, ry * 2f), style = stroke)
        }

        NodeShape.STORED_DATA -> {
            val curve = min(22f, w * 0.18f)
            val p = Path().apply {
                moveTo(x + curve, y)
                lineTo(r, y)
                quadraticTo(r - curve, cy, r, b)
                lineTo(x + curve, b)
                quadraticTo(x - curve * 0.35f, cy, x + curve, y)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.INTERNAL_STORAGE -> {
            val cr = CornerRadius(min(6f, h * 0.12f), min(6f, h * 0.12f))
            val insetX = max(w * 0.18f, if (isIconPreview) 4f else 14f)
            val insetY = max(h * 0.24f, if (isIconPreview) 4f else 14f)
            drawRoundRect(fillColor, Offset(x, y), Size(w, h), cr)
            drawRoundRect(borderColor, Offset(x, y), Size(w, h), cr, style = stroke)
            drawLine(borderColor, Offset(x + insetX, y), Offset(x + insetX, b), strokeWidth = stroke.width)
            drawLine(borderColor, Offset(x, y + insetY), Offset(r, y + insetY), strokeWidth = stroke.width)
        }

        NodeShape.DISPLAY -> {
            val tip = min(24f, w * 0.18f)
            val p = Path().apply {
                moveTo(x + tip, y)
                lineTo(r - tip, y)
                quadraticTo(r, cy, r - tip, b)
                lineTo(x + tip, b)
                lineTo(x, cy)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.CONNECTOR -> {
            val rad = min(w, h) / 2f
            drawCircle(fillColor, rad, Offset(cx, cy))
            drawCircle(borderColor, rad, Offset(cx, cy), style = stroke)
        }

        NodeShape.OFF_PAGE_CONNECTOR -> {
            val shoulderY = y + h * 0.66f
            val p = Path().apply {
                moveTo(x, y)
                lineTo(r, y)
                lineTo(r, shoulderY)
                lineTo(cx, b)
                lineTo(x, shoulderY)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.MERGE -> {
            val p = Path().apply {
                moveTo(x, y)
                lineTo(r, y)
                lineTo(cx, b)
                close()
            }
            drawPath(p, fillColor)
            drawPath(p, borderColor, style = stroke)
        }

        NodeShape.OR_GATE -> {
            val rad = min(w, h) / 2f
            drawCircle(fillColor, rad, Offset(cx, cy))
            drawCircle(borderColor, rad, Offset(cx, cy), style = stroke)
            drawLine(borderColor, Offset(cx - rad, cy), Offset(cx + rad, cy), strokeWidth = stroke.width)
            drawLine(borderColor, Offset(cx, cy - rad), Offset(cx, cy + rad), strokeWidth = stroke.width)
        }

        NodeShape.SUMMING_JUNCTION -> {
            val rad = min(w, h) / 2f
            val d = rad * 0.707f
            drawCircle(fillColor, rad, Offset(cx, cy))
            drawCircle(borderColor, rad, Offset(cx, cy), style = stroke)
            drawLine(borderColor, Offset(cx - d, cy - d), Offset(cx + d, cy + d), strokeWidth = stroke.width)
            drawLine(borderColor, Offset(cx + d, cy - d), Offset(cx - d, cy + d), strokeWidth = stroke.width)
        }

        NodeShape.ANNOTATION -> {
            val bracketW = min(18f, w * 0.24f)
            if (fillColor.alpha > 0.01f) {
                drawRect(fillColor.copy(alpha = 0.75f), Offset(x, y), Size(w, h))
            }
            val p = Path().apply {
                moveTo(x + bracketW, y)
                lineTo(x, y)
                lineTo(x, b)
                lineTo(x + bracketW, b)
            }
            drawPath(p, borderColor, style = stroke)
            if (isIconPreview) {
                // Little dashed hint lines inside preview
                val dashed = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                drawLine(
                    color = borderColor.copy(alpha = 0.55f),
                    start = Offset(x + bracketW * 0.7f, cy - h * 0.14f),
                    end = Offset(r - w * 0.12f, cy - h * 0.14f),
                    strokeWidth = stroke.width * 0.8f,
                    pathEffect = dashed,
                )
                drawLine(
                    color = borderColor.copy(alpha = 0.55f),
                    start = Offset(x + bracketW * 0.7f, cy + h * 0.14f),
                    end = Offset(r - w * 0.24f, cy + h * 0.14f),
                    strokeWidth = stroke.width * 0.8f,
                    pathEffect = dashed,
                )
            }
        }
    }
}
