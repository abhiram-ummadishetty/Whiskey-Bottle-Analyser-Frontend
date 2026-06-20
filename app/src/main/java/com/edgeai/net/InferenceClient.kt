package com.edgeai.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Detection(
    val trackId: Int,
    val label: String,
    val score: Float,
    // Normalised box in [0,1] relative to the source frame.
    val x: Float, val y: Float, val w: Float, val h: Float,
    val ocrText: String? = null,
    val brand: String? = null,
    val brandScore: Float? = null,
)

data class InferResult(val detections: List<Detection>, val latencyMs: Long)

class InferenceClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun health(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("${baseUrl.trimEnd('/')}/health").get().build()
            http.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    /**
     * POST a single JPEG frame to /infer. The backend already supports a
     * multipart `image` field — see backend/app.py.
     */
    suspend fun infer(baseUrl: String, jpeg: ByteArray, frameW: Int, frameH: Int): InferResult =
        withContext(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                // Backend (FastAPI) expects the file field to be named `frame`.
                .addFormDataPart("frame", "frame.jpg",
                    jpeg.toRequestBody("image/jpeg".toMediaType()))
                .build()
            val req = Request.Builder().url("${baseUrl.trimEnd('/')}/infer").post(body).build()
            http.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Backend ${resp.code}: ${bodyStr.take(200)}")
                val json = JSONObject(bodyStr)
                // Pipeline returns pixel coords plus frame width/height; normalise here.
                val srcW = json.optInt("width", 0).coerceAtLeast(1).toFloat()
                val srcH = json.optInt("height", 0).coerceAtLeast(1).toFloat()
                val arr = json.optJSONArray("detections") ?: JSONArray()
                val list = (0 until arr.length()).map { i ->
                    val d = arr.getJSONObject(i)
                    Detection(
                        trackId = d.optInt("track_id", -1),
                        label = d.optString("label", "object"),
                        score = d.optDouble("confidence", 0.0).toFloat(),
                        x = (d.optDouble("x", 0.0).toFloat() / srcW),
                        y = (d.optDouble("y", 0.0).toFloat() / srcH),
                        w = (d.optDouble("w", 0.0).toFloat() / srcW),
                        h = (d.optDouble("h", 0.0).toFloat() / srcH),
                        ocrText = d.optString("ocr_text", "").takeIf { it.isNotBlank() },
                        brand = d.optString("brand", "").takeIf { it.isNotBlank() },
                        brandScore = d.optDouble("brand_score", Double.NaN)
                            .takeIf { !it.isNaN() }?.toFloat(),
                    )
                }
                InferResult(list, System.currentTimeMillis() - started)
            }
        }
}