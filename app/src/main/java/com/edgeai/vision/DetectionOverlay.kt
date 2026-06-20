package com.edgeai.vision

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.edgeai.net.Detection

/**
 * Draws normalised detections on top of a video preview. The overlay assumes
 * the preview fills its bounds with `object-contain` semantics, which matches
 * CameraX `PreviewView` set to `FIT_CENTER`.
 */
@Composable
fun DetectionOverlay(detections: List<Detection>, imageAspectRatio: Float? = null) {
    val measurer: TextMeasurer = rememberTextMeasurer()
    Canvas(Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        var offsetX = 0f
        var offsetY = 0f
        var drawWidth = canvasWidth
        var drawHeight = canvasHeight

        if (imageAspectRatio != null && imageAspectRatio > 0) {
            val canvasAspectRatio = canvasWidth / canvasHeight
            if (imageAspectRatio > canvasAspectRatio) {
                // Image is wider than canvas
                drawHeight = canvasWidth / imageAspectRatio
                offsetY = (canvasHeight - drawHeight) / 2
            } else {
                // Image is taller than canvas
                drawWidth = canvasHeight * imageAspectRatio
                offsetX = (canvasWidth - drawWidth) / 2
            }
        }

        detections.forEach { d ->
            val x = offsetX + d.x * drawWidth
            val y = offsetY + d.y * drawHeight
            val w = d.w * drawWidth
            val h = d.h * drawHeight
            val color = colorForTrack(d.trackId)
            drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(w, h),
                style = Stroke(width = 4f))

            val label = buildString {
                if (d.brand != null && d.brand != "null") {
                    append(d.brand)
                } else {
                    append("Unknown Brand")
                }
                append(' '); append((d.score * 100).toInt()); append('%')
            }
            val layout = measurer.measure(label, style = TextStyle(fontSize = 12.sp, color = Color.White))
            drawRect(color = color.copy(alpha = 0.85f),
                topLeft = androidx.compose.ui.geometry.Offset(x, (y - layout.size.height - 6f).coerceAtLeast(0f)),
                size = androidx.compose.ui.geometry.Size(layout.size.width + 12f, layout.size.height + 6f))
            drawText(layout, topLeft = androidx.compose.ui.geometry.Offset(x + 6f,
                (y - layout.size.height - 3f).coerceAtLeast(3f)))
        }
    }
}

private fun colorForTrack(id: Int): Color {
    val palette = listOf(
        Color(0xFF22D3EE), Color(0xFFFB7185), Color(0xFFFACC15),
        Color(0xFF34D399), Color(0xFFA78BFA), Color(0xFFF97316),
    )
    return palette[((id.coerceAtLeast(0)) % palette.size)]
}