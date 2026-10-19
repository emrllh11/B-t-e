package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import com.example.data.model.TransactionCategory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ReceiptScanner"

data class ScannedReceiptData(
    val title: String,
    val amount: Double,
    val category: TransactionCategory,
    val note: String,
    val dateString: String
)

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Processing(val progressText: String) : ScanUiState
    data class Success(val data: ScannedReceiptData, val bitmap: Bitmap?) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ReceiptScanner {

    private val receiptSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Market, mağaza veya işletme adı (Örn: Migros, A101, Petrol Ofisi, Eczane)"),
            "amount" to Schema.double(description = "Fişteki toplam tutar"),
            "category" to Schema.enumeration(
                values = listOf(
                    "MARKET", "BILLS", "HOUSING", "TRANSPORT",
                    "EATING_OUT", "ENTERTAINMENT", "HEALTH", "CHILDREN",
                    "CLOTHING", "OTHER"
                ),
                description = "En uygun harcama kategorisi"
            ),
            "note" to Schema.string(description = "Fişteki öne çıkan ürünler veya kısa harcama özeti"),
            "dateString" to Schema.string(description = "Fiş tarihi")
        ),
        optionalProperties = listOf("note", "dateString")
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = receiptSchema
            }
        )
    }

    suspend fun scanReceiptBitmap(bitmap: Bitmap): ScannedReceiptData = withContext(Dispatchers.IO) {
        try {
            val promptText = "Sen Türk market, mağaza ve restoran fişlerini inceleyen uzman bir yapay zekasın. " +
                    "Bu fiş görselini dikkatle analiz et. İşletme adını (title), fişteki toplam ödenecek tutarı (amount), " +
                    "en uygun kategoriyi (category), ürünlerin özetini (note) ve tarihi (dateString) JSON olarak çıkar."

            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(bitmap)
                        text(promptText)
                    }
                )
            }

            val jsonString = response.text ?: throw IllegalStateException("Model boş yanıt döndürdü.")
            Log.d(TAG, "Receipt scan response: $jsonString")

            val json = JSONObject(jsonString)
            val title = json.optString("title", "Market Fişi")
            val amount = json.optDouble("amount", 0.0)
            val categoryStr = json.optString("category", "MARKET")
            val note = json.optString("note", "")
            val dateStr = json.optString("dateString", "")

            val category = try {
                TransactionCategory.valueOf(categoryStr)
            } catch (_: Exception) {
                TransactionCategory.MARKET
            }

            ScannedReceiptData(
                title = title.ifBlank { "Fiş Harcaması" },
                amount = if (amount > 0) amount else 0.0,
                category = category,
                note = note,
                dateString = dateStr
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Receipt scan timed out", e)
            throw Exception("Fiş tarama işlemi zaman aşımına uğradı. Lütfen tekrar deneyin.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Receipt scan failed", e)
            throw Exception(e.localizedMessage ?: "Fiş taranırken bir hata oluştu.")
        }
    }

    /**
     * Generates a realistic Turkish receipt bitmap on the fly so users on the emulator
     * can test receipt scanning immediately without needing an external image upload.
     */
    fun createSampleReceiptBitmap(type: String): Bitmap {
        val width = 450
        val height = 650
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Receipt paper background (slightly warm off-white)
        canvas.drawColor(Color.rgb(250, 250, 245))

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 30)
            textSize = 20f
            typeface = Typeface.MONOSPACE
        }

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 10, 10)
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 180, 180)
            strokeWidth = 2f
        }

        var y = 50f
        val centerX = width / 2f

        when (type) {
            "MİGROS" -> {
                canvas.drawText("MİGROS TİCARET A.Ş.", centerX, y, headerPaint)
                y += 35f
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("KADIKÖY ŞUBESİ", centerX, y, textPaint)
                y += 28f
                canvas.drawText("TARİH: 19.10.2026  SAAT: 16:42", centerX, y, textPaint)
                y += 25f
                canvas.drawLine(30f, y, width - 30f, y, linePaint)
                y += 35f

                textPaint.textAlign = Paint.Align.LEFT
                val rightPaint = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }

                canvas.drawText("1 TAM YAĞLI SÜT 1L", 40f, y, textPaint)
                canvas.drawText("39.50", width - 40f, y, rightPaint)
                y += 35f

                canvas.drawText("1 BEYAZ PEYNİR 500G", 40f, y, textPaint)
                canvas.drawText("165.00", width - 40f, y, rightPaint)
                y += 35f

                canvas.drawText("1 SIZMA ZEYTİNYAĞI", 40f, y, textPaint)
                canvas.drawText("340.00", width - 40f, y, rightPaint)
                y += 35f

                canvas.drawText("1 SARI MERCİMEK 1KG", 40f, y, textPaint)
                canvas.drawText("58.50", width - 40f, y, rightPaint)
                y += 35f

                canvas.drawText("1 DOMATES SALÇASI", 40f, y, textPaint)
                canvas.drawText("42.00", width - 40f, y, rightPaint)
                y += 40f

                canvas.drawLine(30f, y, width - 30f, y, linePaint)
                y += 45f

                val totalPaint = Paint(headerPaint).apply { textAlign = Paint.Align.LEFT; textSize = 28f }
                val totalValPaint = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 28f }
                canvas.drawText("TOPLAM TUTAR:", 40f, y, totalPaint)
                canvas.drawText("645.00 ₺", width - 40f, y, totalValPaint)
                y += 45f

                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("KREDİ KARTI İLE ÖDENDİ", centerX, y, textPaint)
                y += 30f
                canvas.drawText("KDV DAHİLDİR - TEŞEKKÜRLER", centerX, y, textPaint)
            }
            "PETROL" -> {
                canvas.drawText("PETROL OFİSİ A.Ş.", centerX, y, headerPaint)
                y += 35f
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("BOĞAZİÇİ İSTASYONU", centerX, y, textPaint)
                y += 28f
                canvas.drawText("TARİH: 19.10.2026  SAAT: 08:15", centerX, y, textPaint)
                y += 25f
                canvas.drawLine(30f, y, width - 30f, y, linePaint)
                y += 45f

                textPaint.textAlign = Paint.Align.LEFT
                val rightPaint = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }

                canvas.drawText("V/MAX KURŞUNSUZ 95", 40f, y, textPaint)
                canvas.drawText("1,850.00", width - 40f, y, rightPaint)
                y += 35f

                canvas.drawText("LİTRE: 41.20 LT", 40f, y, textPaint)
                canvas.drawText("FİYAT: 44.90", width - 40f, y, rightPaint)
                y += 45f

                canvas.drawLine(30f, y, width - 30f, y, linePaint)
                y += 50f

                val totalPaint = Paint(headerPaint).apply { textAlign = Paint.Align.LEFT; textSize = 28f }
                val totalValPaint = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 28f }
                canvas.drawText("TOPLAM:", 40f, y, totalPaint)
                canvas.drawText("1,850.00 ₺", width - 40f, y, totalValPaint)
                y += 50f

                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("PLAKA: 34 SGL 55", centerX, y, textPaint)
                y += 30f
                canvas.drawText("TEMASSIZ KREDİ KARTI", centerX, y, textPaint)
            }
            else -> {
                canvas.drawText("ŞİFA ECZANESİ", centerX, y, headerPaint)
                y += 35f
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("TARİH: 19.10.2026", centerX, y, textPaint)
                y += 25f
                canvas.drawLine(30f, y, width - 30f, y, linePaint)
                y += 45f

                textPaint.textAlign = Paint.Align.LEFT
                val rightPaint = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }

                canvas.drawText("MULTİVİTAMİN KOMPLEKS", 40f, y, textPaint)
                canvas.drawText("420.00", width - 40f, y, rightPaint)
                y += 35f

                canvas.drawText("AĞRI KESİCİ TABLET", 40f, y, textPaint)
                canvas.drawText("95.00", width - 40f, y, rightPaint)
                y += 45f

                canvas.drawLine(30f, y, width - 30f, y, linePaint)
                y += 50f

                val totalPaint = Paint(headerPaint).apply { textAlign = Paint.Align.LEFT; textSize = 28f }
                val totalValPaint = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 28f }
                canvas.drawText("ÖDENECEK:", 40f, y, totalPaint)
                canvas.drawText("515.00 ₺", width - 40f, y, totalValPaint)
                y += 50f

                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("SAĞLIKLI GÜNLER DİLERİZ", centerX, y, textPaint)
            }
        }

        return bitmap
    }
}
