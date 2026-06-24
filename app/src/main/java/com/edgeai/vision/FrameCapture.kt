package com.edgeai.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Convert a CameraX ImageProxy (YUV_420_888) to a JPEG byte array. Rotates
 * to the device orientation reported by ImageInfo so the backend sees an
 * upright frame.
 */
fun ImageProxy.toJpeg(quality: Int = 70, maxDim: Int = 720): ByteArray {
    val nv21 = yuv420ToNv21(this)
    val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val baos = ByteArrayOutputStream()
    yuv.compressToJpeg(Rect(0, 0, width, height), quality, baos)
    val raw = baos.toByteArray()

    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    var bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size, opts) ?: return raw
    val rotation = imageInfo.rotationDegrees
    if (rotation != 0 || maxOf(bmp.width, bmp.height) > maxDim) {
        val matrix = Matrix()
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        val scale = (maxDim.toFloat() / maxOf(bmp.width, bmp.height)).coerceAtMost(1f)
        if (scale < 1f) matrix.postScale(scale, scale)
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    }
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return out.toByteArray()
}

/**
 * Converts raw NV21 bytes to a JPEG byte array.
 */
fun nv21ToJpeg(nv21: ByteArray, width: Int, height: Int, quality: Int = 70): ByteArray {
    val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val baos = ByteArrayOutputStream()
    yuv.compressToJpeg(Rect(0, 0, width, height), quality, baos)
    return baos.toByteArray()
}

private fun yuv420ToNv21(image: ImageProxy): ByteArray {
    val y = image.planes[0].buffer
    val u = image.planes[1].buffer
    val v = image.planes[2].buffer
    val ySize = y.remaining(); val uSize = u.remaining(); val vSize = v.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    y.get(nv21, 0, ySize)
    v.get(nv21, ySize, vSize)
    u.get(nv21, ySize + vSize, uSize)
    return nv21
}