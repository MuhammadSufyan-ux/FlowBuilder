package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.domain.engine.FlowGeometry
import com.example.domain.model.AppFontFamily
import com.example.domain.model.BorderStyle
import com.example.domain.model.EdgeStyle
import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowGraph
import com.example.domain.model.FlowNode
import com.example.domain.model.GridStyle
import com.example.domain.model.NodeShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object FlowExportManager {

    private const val EXPORT_PADDING = 72f

    suspend fun renderToPngBytes(
        graph: FlowGraph,
        darkBackground: Boolean = false,
        scale: Float = 2f,
    ): ByteArray = withContext(Dispatchers.Default) {
        val bounds = computeContentBounds(graph)
        val widthPx = max(640, ((bounds.width() + EXPORT_PADDING * 2) * scale).toInt()).coerceAtMost(4096)
        val heightPx = max(480, ((bounds.height() + EXPORT_PADDING * 2) * scale).toInt()).coerceAtMost(4096)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val offsetX = -bounds.left + EXPORT_PADDING
        val offsetY = -bounds.top + EXPORT_PADDING
        canvas.save()
        canvas.scale(scale, scale)
        drawGraphOnNativeCanvas(
            canvas = canvas,
            graph = graph,
            viewportWidth = widthPx / scale,
            viewportHeight = heightPx / scale,
            offsetX = offsetX,
            offsetY = offsetY,
            darkBackground = darkBackground,
        )
        canvas.restore()

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        out.toByteArray()
    }

    suspend fun renderToPdfBytes(
        graph: FlowGraph,
        title: String,
        darkBackground: Boolean = false,
    ): ByteArray = withContext(Dispatchers.Default) {
        val bounds = computeContentBounds(graph)
        val pageWidth = max(595, (bounds.width() + EXPORT_PADDING * 2).toInt()).coerceAtMost(2400)
        val pageHeight = max(842, (bounds.height() + EXPORT_PADDING * 2 + 56f).toInt()).coerceAtMost(3200)

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val offsetX = -bounds.left + EXPORT_PADDING
        val offsetY = -bounds.top + EXPORT_PADDING + 36f
        drawGraphOnNativeCanvas(
            canvas = canvas,
            graph = graph,
            viewportWidth = pageWidth.toFloat(),
            viewportHeight = pageHeight.toFloat(),
            offsetX = offsetX,
            offsetY = offsetY,
            darkBackground = darkBackground,
        )

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (darkBackground) Color.parseColor("#E2E8F0") else Color.parseColor("#0F172A")
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(title, 32f, 34f, headerPaint)

        pdf.finishPage(page)
        val out = ByteArrayOutputStream()
        pdf.writeTo(out)
        pdf.close()
        out.toByteArray()
    }

    suspend fun renderToHtmlBytes(
        graph: FlowGraph,
        title: String,
        darkBackground: Boolean = false,
    ): ByteArray = withContext(Dispatchers.Default) {
        val bounds = computeContentBounds(graph)
        val vbWidth = max(640f, bounds.width() + EXPORT_PADDING * 2)
        val vbHeight = max(460f, bounds.height() + EXPORT_PADDING * 2)
        val offsetX = -bounds.left + EXPORT_PADDING
        val offsetY = -bounds.top + EXPORT_PADDING

        val bg = if (darkBackground) "#0C121C" else "#FAFBFD"
        val fg = if (darkBackground) "#F8FAFC" else "#0F172A"
        val cardBg = if (darkBackground) "#151E2C" else "#FFFFFF"
        val borderCol = if (darkBackground) "#283548" else "#E2E8F0"
        val defaultEdgeStroke = if (darkBackground) "#CBD5E1" else "#1E293B"
        val nodeMap = graph.nodes.associateBy { it.id }

        val svgContent = buildString {
            // Edges
            graph.edges.forEach { edge ->
                val from = nodeMap[edge.fromNodeId] ?: return@forEach
                val to = nodeMap[edge.toNodeId] ?: return@forEach
                val geom = FlowGeometry.computeEdgeGeometry(from, to, edge)
                val stroke = if (darkBackground && edge.strokeColorHex.equals("#1E293B", true)) {
                    defaultEdgeStroke
                } else {
                    edge.strokeColorHex
                }
                val dashAttr = if (edge.isDashed) """ stroke-dasharray="9 6"""" else ""
                val pathData = if (edge.style == EdgeStyle.CURVED) {
                    val (c1, c2) = FlowGeometry.computeCubicControls(
                        geom.start, geom.end, geom.startSide, geom.endSide
                    )
                    "M ${f(geom.start.x + offsetX)} ${f(geom.start.y + offsetY)} " +
                        "C ${f(c1.x + offsetX)} ${f(c1.y + offsetY)}, " +
                        "${f(c2.x + offsetX)} ${f(c2.y + offsetY)}, " +
                        "${f(geom.end.x + offsetX)} ${f(geom.end.y + offsetY)}"
                } else {
                    geom.waypoints.mapIndexed { idx, pt ->
                        val cmd = if (idx == 0) "M" else "L"
                        "$cmd ${f(pt.x + offsetX)} ${f(pt.y + offsetY)}"
                    }.joinToString(" ")
                }
                appendLine("""<path d="$pathData" fill="none" stroke="$stroke" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"$dashAttr marker-end="url(#arrow)" />""")

                if (edge.label.isNotBlank()) {
                    val lx = geom.labelPosition.x + offsetX
                    val ly = geom.labelPosition.y + offsetY
                    val pillW = max(44f, edge.label.length * 7.8f + 18f)
                    appendLine("""<rect x="${f(lx - pillW / 2f)}" y="${f(ly - 12f)}" width="${f(pillW)}" height="24" rx="12" fill="$cardBg" stroke="$borderCol" stroke-width="1.2" />""")
                    appendLine("""<text x="${f(lx)}" y="${f(ly + 4f)}" text-anchor="middle" font-size="12" font-weight="600" fill="$fg">${escapeHtml(edge.label)}</text>""")
                }
            }

            // Nodes
            graph.nodes.forEach { node ->
                appendLine(buildSvgNode(node, offsetX, offsetY))
            }
        }

        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${escapeHtml(title)} — FlowBuilder</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      padding: 24px;
      font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: $bg;
      color: $fg;
      display: flex;
      flex-direction: column;
      align-items: center;
      min-height: 100vh;
    }
    .header {
      width: 100%;
      max-width: 1200px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      padding: 14px 20px;
      background: $cardBg;
      border: 1px solid $borderCol;
      border-radius: 16px;
    }
    .brand { display: flex; align-items: center; gap: 10px; font-weight: 700; font-size: 18px; }
    .dot { width: 12px; height: 12px; border-radius: 999px; background: #0D9488; display: inline-block; }
    .meta { font-size: 13px; opacity: 0.7; }
    .canvas-card {
      width: 100%;
      max-width: 1200px;
      background: $cardBg;
      border: 1px solid $borderCol;
      border-radius: 20px;
      padding: 16px;
      overflow: auto;
      box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
    }
    svg { width: 100%; height: auto; display: block; }
  </style>
</head>
<body>
  <header class="header">
    <div class="brand"><span class="dot"></span>${escapeHtml(title)}</div>
    <div class="meta">${graph.nodes.size} shapes • ${graph.edges.size} flows • Exported from FlowBuilder</div>
  </header>
  <main class="canvas-card">
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${f(vbWidth)} ${f(vbHeight)}">
      <defs>
        <marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
          <path d="M 1 1.5 L 8.5 5 L 1 8.5" fill="none" stroke="$defaultEdgeStroke" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
        </marker>
      </defs>
      <rect width="100%" height="100%" fill="$bg" rx="12" />
      $svgContent
    </svg>
  </main>
</body>
</html>
        """.trimIndent()

        html.toByteArray(Charsets.UTF_8)
    }

    private fun buildSvgNode(node: FlowNode, ox: Float, oy: Float): String {
        val x = node.x + ox
        val y = node.y + oy
        val w = node.width
        val h = node.height
        val r = x + w
        val b = y + h
        val cx = x + w / 2f
        val cy = y + h / 2f
        val fill = if (node.shape == NodeShape.TEXT_BOX) "none" else node.fillColorHex
        val stroke = if (node.shape == NodeShape.TEXT_BOX) "none" else node.borderColorHex
        val sw = f(node.borderWidth)
        val dash = when (node.borderStyle) {
            BorderStyle.SOLID -> ""
            BorderStyle.DASHED -> """ stroke-dasharray="10 6""""
            BorderStyle.DOTTED -> """ stroke-dasharray="3 5""""
        }

        val shapeSvg = when (node.shape) {
            NodeShape.TEXT_BOX -> ""
            NodeShape.OVAL ->
                """<rect x="${f(x)}" y="${f(y)}" width="${f(w)}" height="${f(h)}" rx="${f(h / 2f)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            NodeShape.RECTANGLE ->
                """<rect x="${f(x)}" y="${f(y)}" width="${f(w)}" height="${f(h)}" rx="12" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            NodeShape.ALTERNATE_PROCESS ->
                """<rect x="${f(x)}" y="${f(y)}" width="${f(w)}" height="${f(h)}" rx="${f(min(w, h) * 0.34f)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            NodeShape.DIAMOND ->
                """<polygon points="${f(cx)},${f(y)} ${f(r)},${f(cy)} ${f(cx)},${f(b)} ${f(x)},${f(cy)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            NodeShape.PARALLELOGRAM -> {
                val slant = min(22f, w * 0.16f)
                """<polygon points="${f(x + slant)},${f(y)} ${f(r)},${f(y)} ${f(r - slant)},${f(b)} ${f(x)},${f(b)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.SUBROUTINE -> {
                val inset = max(12f, w * 0.1f)
                """<g><rect x="${f(x)}" y="${f(y)}" width="${f(w)}" height="${f(h)}" rx="8" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />""" +
                    """<line x1="${f(x + inset)}" y1="${f(y)}" x2="${f(x + inset)}" y2="${f(b)}" stroke="$stroke" stroke-width="$sw" />""" +
                    """<line x1="${f(r - inset)}" y1="${f(y)}" x2="${f(r - inset)}" y2="${f(b)}" stroke="$stroke" stroke-width="$sw" /></g>"""
            }
            NodeShape.PREPARATION -> {
                val dx = min(24f, w * 0.18f)
                """<polygon points="${f(x + dx)},${f(y)} ${f(r - dx)},${f(y)} ${f(r)},${f(cy)} ${f(r - dx)},${f(b)} ${f(x + dx)},${f(b)} ${f(x)},${f(cy)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.MANUAL_OPERATION -> {
                val dx = min(22f, w * 0.15f)
                """<polygon points="${f(x)},${f(y)} ${f(r)},${f(y)} ${f(r - dx)},${f(b)} ${f(x + dx)},${f(b)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.MANUAL_INPUT -> {
                val drop = min(20f, h * 0.25f)
                """<polygon points="${f(x)},${f(y + drop)} ${f(r)},${f(y)} ${f(r)},${f(b)} ${f(x)},${f(b)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.DELAY -> {
                val rad = h / 2f
                val d = "M ${f(x)} ${f(y)} L ${f(r - rad)} ${f(y)} A ${f(rad)} ${f(rad)} 0 0 1 ${f(r - rad)} ${f(b)} L ${f(x)} ${f(b)} Z"
                """<path d="$d" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.LOOP_LIMIT -> {
                val cut = min(22f, w * 0.16f)
                """<polygon points="${f(x + cut)},${f(y)} ${f(r - cut)},${f(y)} ${f(r)},${f(y + cut)} ${f(r)},${f(b)} ${f(x)},${f(b)} ${f(x)},${f(y + cut)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.DOCUMENT -> {
                val waveH = min(14f, h * 0.18f)
                val d = "M ${f(x)} ${f(y)} L ${f(r)} ${f(y)} L ${f(r)} ${f(b - waveH)} " +
                    "Q ${f(x + w * 0.75f)} ${f(b - waveH * 2.2f)} ${f(cx)} ${f(b - waveH)} " +
                    "Q ${f(x + w * 0.25f)} ${f(b + waveH * 0.5f)} ${f(x)} ${f(b - waveH * 0.4f)} Z"
                """<path d="$d" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.MULTIPLE_DOCUMENTS -> {
                val waveH = min(12f, h * 0.15f)
                val step = 6f
                val frontD = "M ${f(x)} ${f(y + step * 2)} L ${f(r - step * 2)} ${f(y + step * 2)} L ${f(r - step * 2)} ${f(b - waveH)} " +
                    "Q ${f(x + (w - step * 2) * 0.75f)} ${f(b - waveH * 2f)} ${f(x + (w - step * 2) * 0.5f)} ${f(b - waveH)} " +
                    "Q ${f(x + (w - step * 2) * 0.25f)} ${f(b + waveH * 0.4f)} ${f(x)} ${f(b - waveH * 0.4f)} Z"
                """<g><rect x="${f(x + step * 2)}" y="${f(y)}" width="${f(w - step * 2)}" height="${f(h - step * 2 - waveH * 0.4f)}" fill="$fill" stroke="$stroke" stroke-width="$sw" />""" +
                    """<rect x="${f(x + step)}" y="${f(y + step)}" width="${f(w - step * 2)}" height="${f(h - step * 2 - waveH * 0.4f)}" fill="$fill" stroke="$stroke" stroke-width="$sw" />""" +
                    """<path d="$frontD" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash /></g>"""
            }
            NodeShape.DATABASE -> {
                val ry = min(14f, h * 0.16f)
                val body = "M ${f(x)} ${f(y + ry)} L ${f(x)} ${f(b - ry)} A ${f(w / 2f)} ${f(ry)} 0 0 0 ${f(r)} ${f(b - ry)} L ${f(r)} ${f(y + ry)}"
                """<g><path d="$body Z" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />""" +
                    """<ellipse cx="${f(cx)}" cy="${f(y + ry)}" rx="${f(w / 2f)}" ry="${f(ry)}" fill="$fill" stroke="$stroke" stroke-width="$sw" /></g>"""
            }
            NodeShape.STORED_DATA -> {
                val curve = min(22f, w * 0.15f)
                val d = "M ${f(x + curve)} ${f(y)} L ${f(r)} ${f(y)} Q ${f(r - curve)} ${f(cy)} ${f(r)} ${f(b)} L ${f(x + curve)} ${f(b)} Q ${f(x - curve * 0.35f)} ${f(cy)} ${f(x + curve)} ${f(y)} Z"
                """<path d="$d" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.INTERNAL_STORAGE -> {
                val insetX = max(14f, w * 0.14f)
                val insetY = max(14f, h * 0.22f)
                """<g><rect x="${f(x)}" y="${f(y)}" width="${f(w)}" height="${f(h)}" rx="6" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />""" +
                    """<line x1="${f(x + insetX)}" y1="${f(y)}" x2="${f(x + insetX)}" y2="${f(b)}" stroke="$stroke" stroke-width="$sw" />""" +
                    """<line x1="${f(x)}" y1="${f(y + insetY)}" x2="${f(r)}" y2="${f(y + insetY)}" stroke="$stroke" stroke-width="$sw" /></g>"""
            }
            NodeShape.DISPLAY -> {
                val tip = min(24f, w * 0.16f)
                val d = "M ${f(x + tip)} ${f(y)} L ${f(r - tip)} ${f(y)} Q ${f(r)} ${f(cy)} ${f(r - tip)} ${f(b)} L ${f(x + tip)} ${f(b)} L ${f(x)} ${f(cy)} Z"
                """<path d="$d" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.CONNECTOR -> {
                val rad = min(w, h) / 2f
                """<circle cx="${f(cx)}" cy="${f(cy)}" r="${f(rad)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.OFF_PAGE_CONNECTOR -> {
                val shoulderY = y + h * 0.68f
                """<polygon points="${f(x)},${f(y)} ${f(r)},${f(y)} ${f(r)},${f(shoulderY)} ${f(cx)},${f(b)} ${f(x)},${f(shoulderY)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            }
            NodeShape.MERGE ->
                """<polygon points="${f(x)},${f(y)} ${f(r)},${f(y)} ${f(cx)},${f(b)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />"""
            NodeShape.OR_GATE -> {
                val rad = min(w, h) / 2f
                """<g><circle cx="${f(cx)}" cy="${f(cy)}" r="${f(rad)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />""" +
                    """<line x1="${f(cx - rad)}" y1="${f(cy)}" x2="${f(cx + rad)}" y2="${f(cy)}" stroke="$stroke" stroke-width="$sw" />""" +
                    """<line x1="${f(cx)}" y1="${f(cy - rad)}" x2="${f(cx)}" y2="${f(cy + rad)}" stroke="$stroke" stroke-width="$sw" /></g>"""
            }
            NodeShape.SUMMING_JUNCTION -> {
                val rad = min(w, h) / 2f
                val d = rad * 0.707f
                """<g><circle cx="${f(cx)}" cy="${f(cy)}" r="${f(rad)}" fill="$fill" stroke="$stroke" stroke-width="$sw"$dash />""" +
                    """<line x1="${f(cx - d)}" y1="${f(cy - d)}" x2="${f(cx + d)}" y2="${f(cy + d)}" stroke="$stroke" stroke-width="$sw" />""" +
                    """<line x1="${f(cx + d)}" y1="${f(cy - d)}" x2="${f(cx - d)}" y2="${f(cy + d)}" stroke="$stroke" stroke-width="$sw" /></g>"""
            }
            NodeShape.ANNOTATION -> {
                val bracketW = min(18f, w * 0.14f)
                val d = "M ${f(x + bracketW)} ${f(y)} L ${f(x)} ${f(y)} L ${f(x)} ${f(b)} L ${f(x + bracketW)} ${f(b)}"
                """<g><rect x="${f(x)}" y="${f(y)}" width="${f(w)}" height="${f(h)}" fill="$fill" fill-opacity="0.65" />""" +
                    """<path d="$d" fill="none" stroke="$stroke" stroke-width="$sw"$dash /></g>"""
            }
        }

        val fontCss = when (node.fontFamily) {
            AppFontFamily.JETBRAINS_MONO -> "'JetBrains Mono', monospace"
            AppFontFamily.MERRIWEATHER -> "Merriweather, Georgia, serif"
            else -> "'Plus Jakarta Sans', Inter, sans-serif"
        }
        val weight = if (node.isBold) "700" else "400"
        val lines = node.text.split("\n").ifEmpty { listOf("") }
        val textYOffset = if (node.shape == NodeShape.DATABASE) 6f else 0f
        val lineHeight = node.fontSizeSp * 1.25f
        val startY = cy + textYOffset - ((lines.size - 1) * lineHeight / 2f) + node.fontSizeSp * 0.35f

        val textSvg = if (node.text.isNotBlank()) {
            lines.mapIndexed { idx, line ->
                """<text x="${f(cx)}" y="${f(startY + idx * lineHeight)}" text-anchor="middle" font-family="$fontCss" font-size="${f(node.fontSizeSp)}" font-weight="$weight" fill="${node.textColorHex}">${escapeHtml(line)}</text>"""
            }.joinToString("")
        } else {
            ""
        }
        return "$shapeSvg$textSvg"
    }

    private fun f(value: Float): String = String.format(Locale.US, "%.1f", value)

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    suspend fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(bytes)
                    stream.flush()
                } ?: error("Unable to open destination file")
            }
        }

    suspend fun readTextFromUri(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to open selected file")
            }
        }

    suspend fun createShareIntent(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val file = File(exportDir, safeName)
            file.writeBytes(bytes)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, safeName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(sendIntent, "Share Flowchart")
        }
    }

    private data class ContentBounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        fun width(): Float = right - left
        fun height(): Float = bottom - top
    }

    private fun computeContentBounds(graph: FlowGraph): ContentBounds {
        if (graph.nodes.isEmpty()) return ContentBounds(0f, 0f, 600f, 400f)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        graph.nodes.forEach { n ->
            minX = min(minX, n.x)
            minY = min(minY, n.y)
            maxX = max(maxX, n.right)
            maxY = max(maxY, n.bottom)
        }
        return ContentBounds(minX, minY, maxX, maxY)
    }

    private fun drawGraphOnNativeCanvas(
        canvas: Canvas,
        graph: FlowGraph,
        viewportWidth: Float,
        viewportHeight: Float,
        offsetX: Float,
        offsetY: Float,
        darkBackground: Boolean,
    ) {
        val bgColor = if (darkBackground) Color.parseColor("#0C121C") else Color.parseColor("#FAFBFD")
        val gridColor = if (darkBackground) Color.parseColor("#243247") else Color.parseColor("#D5DFEA")
        canvas.drawColor(bgColor)

        if (graph.gridStyle != GridStyle.NONE) {
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gridColor
                strokeWidth = 1.2f
            }
            val spacing = 28f
            var x = spacing
            while (x < viewportWidth) {
                var y = spacing
                while (y < viewportHeight) {
                    when (graph.gridStyle) {
                        GridStyle.PLUS -> {
                            canvas.drawLine(x - 3f, y, x + 3f, y, gridPaint)
                            canvas.drawLine(x, y - 3f, x, y + 3f, gridPaint)
                        }
                        GridStyle.DOTS -> canvas.drawCircle(x, y, 1.5f, gridPaint)
                        GridStyle.LINES -> Unit
                        GridStyle.NONE -> Unit
                    }
                    y += spacing
                }
                x += spacing
            }
            if (graph.gridStyle == GridStyle.LINES) {
                var lx = 0f
                while (lx < viewportWidth) {
                    canvas.drawLine(lx, 0f, lx, viewportHeight, gridPaint)
                    lx += spacing
                }
                var ly = 0f
                while (ly < viewportHeight) {
                    canvas.drawLine(0f, ly, viewportWidth, ly, gridPaint)
                    ly += spacing
                }
            }
        }

        canvas.save()
        canvas.translate(offsetX, offsetY)

        val nodeMap = graph.nodes.associateBy { it.id }
        graph.edges.forEach { edge ->
            val from = nodeMap[edge.fromNodeId] ?: return@forEach
            val to = nodeMap[edge.toNodeId] ?: return@forEach
            drawEdge(canvas, from, to, edge, darkBackground)
        }
        graph.nodes.forEach { node ->
            drawNode(canvas, node)
        }

        canvas.restore()
    }

    private fun drawEdge(
        canvas: Canvas,
        from: FlowNode,
        to: FlowNode,
        edge: FlowEdge,
        darkBackground: Boolean,
    ) {
        val geom = FlowGeometry.computeEdgeGeometry(from, to, edge)
        val strokeColor = parseAndroidColor(
            edge.strokeColorHex,
            if (darkBackground) Color.parseColor("#CBD5E1") else Color.parseColor("#1E293B"),
        )
        val effectiveColor = if (darkBackground && edge.strokeColorHex.equals("#1E293B", true)) {
            Color.parseColor("#CBD5E1")
        } else {
            strokeColor
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = effectiveColor
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            if (edge.isDashed) {
                pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
            }
        }

        val path = Path()
        var lastPrevX = geom.start.x
        var lastPrevY = geom.start.y

        if (edge.style == EdgeStyle.CURVED) {
            val (c1, c2) = FlowGeometry.computeCubicControls(
                geom.start, geom.end, geom.startSide, geom.endSide
            )
            path.moveTo(geom.start.x, geom.start.y)
            path.cubicTo(c1.x, c1.y, c2.x, c2.y, geom.end.x, geom.end.y)
            lastPrevX = c2.x
            lastPrevY = c2.y
        } else {
            val pts = geom.waypoints
            if (pts.size >= 2) {
                path.moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) {
                    path.lineTo(pts[i].x, pts[i].y)
                }
                lastPrevX = pts[pts.size - 2].x
                lastPrevY = pts[pts.size - 2].y
            }
        }
        canvas.drawPath(path, paint)

        val angle = atan2((geom.end.y - lastPrevY).toDouble(), (geom.end.x - lastPrevX).toDouble())
        val arrowLen = 12f
        val arrowAngle = Math.toRadians(28.0)
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = effectiveColor
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
            strokeCap = Paint.Cap.ROUND
        }
        val x1 = geom.end.x - arrowLen * cos(angle - arrowAngle).toFloat()
        val y1 = geom.end.y - arrowLen * sin(angle - arrowAngle).toFloat()
        val x2 = geom.end.x - arrowLen * cos(angle + arrowAngle).toFloat()
        val y2 = geom.end.y - arrowLen * sin(angle + arrowAngle).toFloat()
        canvas.drawLine(geom.end.x, geom.end.y, x1, y1, arrowPaint)
        canvas.drawLine(geom.end.x, geom.end.y, x2, y2, arrowPaint)

        if (edge.label.isNotBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (darkBackground) Color.WHITE else Color.parseColor("#0F172A")
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val textW = textPaint.measureText(edge.label)
            val pillRect = RectF(
                geom.labelPosition.x - textW / 2f - 10f,
                geom.labelPosition.y - 12f,
                geom.labelPosition.x + textW / 2f + 10f,
                geom.labelPosition.y + 12f,
            )
            val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (darkBackground) Color.parseColor("#1E293B") else Color.WHITE
                style = Paint.Style.FILL
            }
            val pillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (darkBackground) Color.parseColor("#334155") else Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }
            canvas.drawRoundRect(pillRect, 12f, 12f, pillBg)
            canvas.drawRoundRect(pillRect, 12f, 12f, pillBorder)
            canvas.drawText(edge.label, geom.labelPosition.x, geom.labelPosition.y + 4.5f, textPaint)
        }
    }

    private fun drawNode(canvas: Canvas, node: FlowNode) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseAndroidColor(node.fillColorHex, Color.WHITE)
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseAndroidColor(node.borderColorHex, Color.parseColor("#161D29"))
            style = Paint.Style.STROKE
            strokeWidth = node.borderWidth
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            when (node.borderStyle) {
                BorderStyle.SOLID -> Unit
                BorderStyle.DASHED -> pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
                BorderStyle.DOTTED -> pathEffect = DashPathEffect(floatArrayOf(4f, 6f), 0f)
            }
        }

        val x = node.x
        val y = node.y
        val w = node.width
        val h = node.height
        val r = node.right
        val b = node.bottom
        val cx = node.centerX
        val cy = node.centerY
        val rect = RectF(x, y, r, b)

        when (node.shape) {
            NodeShape.TEXT_BOX -> Unit
            NodeShape.OVAL -> {
                val radius = h / 2f
                canvas.drawRoundRect(rect, radius, radius, fillPaint)
                canvas.drawRoundRect(rect, radius, radius, strokePaint)
            }
            NodeShape.RECTANGLE -> {
                canvas.drawRoundRect(rect, 12f, 12f, fillPaint)
                canvas.drawRoundRect(rect, 12f, 12f, strokePaint)
            }
            NodeShape.ALTERNATE_PROCESS -> {
                val rad = min(w, h) * 0.34f
                canvas.drawRoundRect(rect, rad, rad, fillPaint)
                canvas.drawRoundRect(rect, rad, rad, strokePaint)
            }
            NodeShape.DIAMOND -> {
                val path = Path().apply {
                    moveTo(cx, y); lineTo(r, cy); lineTo(cx, b); lineTo(x, cy); close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.PARALLELOGRAM -> {
                val slant = min(24f, w * 0.16f)
                val path = Path().apply {
                    moveTo(x + slant, y); lineTo(r, y); lineTo(r - slant, b); lineTo(x, b); close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.SUBROUTINE -> {
                val inset = max(12f, w * 0.10f)
                canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
                canvas.drawRoundRect(rect, 8f, 8f, strokePaint)
                canvas.drawLine(x + inset, y, x + inset, b, strokePaint)
                canvas.drawLine(r - inset, y, r - inset, b, strokePaint)
            }
            NodeShape.PREPARATION -> {
                val dx = min(24f, w * 0.18f)
                val path = Path().apply {
                    moveTo(x + dx, y); lineTo(r - dx, y); lineTo(r, cy)
                    lineTo(r - dx, b); lineTo(x + dx, b); lineTo(x, cy); close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.MANUAL_OPERATION -> {
                val dx = min(22f, w * 0.15f)
                val path = Path().apply {
                    moveTo(x, y); lineTo(r, y); lineTo(r - dx, b); lineTo(x + dx, b); close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.MANUAL_INPUT -> {
                val drop = min(20f, h * 0.25f)
                val path = Path().apply {
                    moveTo(x, y + drop); lineTo(r, y); lineTo(r, b); lineTo(x, b); close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.DELAY -> {
                val rad = h / 2f
                val path = Path().apply {
                    moveTo(x, y)
                    lineTo(r - rad, y)
                    arcTo(RectF(r - rad * 2f, y, r, b), -90f, 180f, false)
                    lineTo(x, b)
                    close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.LOOP_LIMIT -> {
                val cut = min(22f, w * 0.16f)
                val path = Path().apply {
                    moveTo(x + cut, y); lineTo(r - cut, y); lineTo(r, y + cut)
                    lineTo(r, b); lineTo(x, b); lineTo(x, y + cut); close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.DOCUMENT -> {
                val waveH = min(14f, h * 0.18f)
                val path = Path().apply {
                    moveTo(x, y); lineTo(r, y); lineTo(r, b - waveH)
                    quadTo(x + w * 0.75f, b - waveH * 2.2f, cx, b - waveH)
                    quadTo(x + w * 0.25f, b + waveH * 0.5f, x, b - waveH * 0.4f)
                    close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.MULTIPLE_DOCUMENTS -> {
                val step = 6f
                val waveH = min(12f, h * 0.15f)
                val back2 = RectF(x + step * 2, y, r, b - step * 2 - waveH * 0.3f)
                val back1 = RectF(x + step, y + step, r - step, b - step - waveH * 0.3f)
                canvas.drawRect(back2, fillPaint); canvas.drawRect(back2, strokePaint)
                canvas.drawRect(back1, fillPaint); canvas.drawRect(back1, strokePaint)
                val fw = w - step * 2
                val path = Path().apply {
                    moveTo(x, y + step * 2); lineTo(x + fw, y + step * 2); lineTo(x + fw, b - waveH)
                    quadTo(x + fw * 0.75f, b - waveH * 2f, x + fw * 0.5f, b - waveH)
                    quadTo(x + fw * 0.25f, b + waveH * 0.4f, x, b - waveH * 0.4f)
                    close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.DATABASE -> {
                val ry = min(14f, h * 0.16f)
                val bodyPath = Path().apply {
                    moveTo(x, y + ry)
                    lineTo(x, b - ry)
                    arcTo(RectF(x, b - ry * 2f, r, b), 180f, -180f, false)
                    lineTo(r, y + ry)
                    close()
                }
                canvas.drawPath(bodyPath, fillPaint)
                canvas.drawPath(bodyPath, strokePaint)
                val topOval = RectF(x, y, r, y + ry * 2f)
                canvas.drawOval(topOval, fillPaint)
                canvas.drawOval(topOval, strokePaint)
            }
            NodeShape.STORED_DATA -> {
                val curve = min(22f, w * 0.15f)
                val path = Path().apply {
                    moveTo(x + curve, y); lineTo(r, y)
                    quadTo(r - curve, cy, r, b)
                    lineTo(x + curve, b)
                    quadTo(x - curve * 0.35f, cy, x + curve, y)
                    close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.INTERNAL_STORAGE -> {
                val insetX = max(14f, w * 0.14f)
                val insetY = max(14f, h * 0.22f)
                canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
                canvas.drawRoundRect(rect, 6f, 6f, strokePaint)
                canvas.drawLine(x + insetX, y, x + insetX, b, strokePaint)
                canvas.drawLine(x, y + insetY, r, y + insetY, strokePaint)
            }
            NodeShape.DISPLAY -> {
                val tip = min(24f, w * 0.16f)
                val path = Path().apply {
                    moveTo(x + tip, y); lineTo(r - tip, y)
                    quadTo(r, cy, r - tip, b)
                    lineTo(x + tip, b); lineTo(x, cy); close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.CONNECTOR -> {
                val rad = min(w, h) / 2f
                canvas.drawCircle(cx, cy, rad, fillPaint)
                canvas.drawCircle(cx, cy, rad, strokePaint)
            }
            NodeShape.OFF_PAGE_CONNECTOR -> {
                val shoulderY = y + h * 0.68f
                val path = Path().apply {
                    moveTo(x, y); lineTo(r, y); lineTo(r, shoulderY)
                    lineTo(cx, b); lineTo(x, shoulderY); close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.MERGE -> {
                val path = Path().apply {
                    moveTo(x, y); lineTo(r, y); lineTo(cx, b); close()
                }
                canvas.drawPath(path, fillPaint); canvas.drawPath(path, strokePaint)
            }
            NodeShape.OR_GATE -> {
                val rad = min(w, h) / 2f
                canvas.drawCircle(cx, cy, rad, fillPaint)
                canvas.drawCircle(cx, cy, rad, strokePaint)
                canvas.drawLine(cx - rad, cy, cx + rad, cy, strokePaint)
                canvas.drawLine(cx, cy - rad, cx, cy + rad, strokePaint)
            }
            NodeShape.SUMMING_JUNCTION -> {
                val rad = min(w, h) / 2f
                val d = rad * 0.707f
                canvas.drawCircle(cx, cy, rad, fillPaint)
                canvas.drawCircle(cx, cy, rad, strokePaint)
                canvas.drawLine(cx - d, cy - d, cx + d, cy + d, strokePaint)
                canvas.drawLine(cx + d, cy - d, cx - d, cy + d, strokePaint)
            }
            NodeShape.ANNOTATION -> {
                val bracketW = min(18f, w * 0.14f)
                canvas.drawRect(rect, fillPaint)
                val path = Path().apply {
                    moveTo(x + bracketW, y); lineTo(x, y); lineTo(x, b); lineTo(x + bracketW, b)
                }
                canvas.drawPath(path, strokePaint)
            }
        }

        if (node.text.isBlank()) return

        val textPad = when (node.shape) {
            NodeShape.DIAMOND -> (w * 0.24f).toInt()
            NodeShape.PARALLELOGRAM, NodeShape.PREPARATION, NodeShape.DISPLAY -> 24
            NodeShape.CONNECTOR, NodeShape.OR_GATE, NodeShape.SUMMING_JUNCTION -> 8
            NodeShape.SUBROUTINE -> 20
            else -> 14
        }
        val availableWidth = max(24, (w - textPad * 2).toInt())
        val baseTypeface = when (node.fontFamily) {
            AppFontFamily.JETBRAINS_MONO -> Typeface.MONOSPACE
            AppFontFamily.MERRIWEATHER -> Typeface.SERIF
            else -> Typeface.SANS_SERIF
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseAndroidColor(node.textColorHex, Color.parseColor("#0F172A"))
            textSize = node.fontSizeSp
            typeface = Typeface.create(
                baseTypeface,
                if (node.isBold) Typeface.BOLD else Typeface.NORMAL,
            )
        }
        val staticLayout = StaticLayout.Builder.obtain(
            node.text, 0, node.text.length, textPaint, availableWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()

        val yBias = if (node.shape == NodeShape.DATABASE) 8f else 0f
        canvas.save()
        canvas.translate(
            cx - availableWidth / 2f,
            cy + yBias - staticLayout.height / 2f,
        )
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun parseAndroidColor(hex: String, fallback: Int): Int {
        return try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }
    }
}
