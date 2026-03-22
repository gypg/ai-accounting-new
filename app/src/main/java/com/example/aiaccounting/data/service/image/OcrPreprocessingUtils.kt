package com.example.aiaccounting.data.service.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import java.io.ByteArrayOutputStream

internal object OcrPreprocessingUtils {
    private const val MAX_EDGE = 1600
    private const val JPEG_QUALITY = 92
    private const val CONTRAST_SCALE = 1.15f

    fun preprocessImage(context: Context, uri: Uri): ByteArray? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val original = BitmapFactory.decodeStream(inputStream) ?: return null
                val normalized = scaleDownIfNeeded(original)
                val enhanced = enhanceContrastAndGrayscale(normalized)
                bitmapToJpegBytes(enhanced)
            }
        }.getOrNull()
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDimension = maxOf(bitmap.width, bitmap.height)
        if (maxDimension <= MAX_EDGE) return bitmap

        val ratio = MAX_EDGE.toFloat() / maxDimension.toFloat()
        val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun enhanceContrastAndGrayscale(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = ColorMatrix(
            floatArrayOf(
                CONTRAST_SCALE, 0f, 0f, 0f, 0f,
                0f, CONTRAST_SCALE, 0f, 0f, 0f,
                0f, 0f, CONTRAST_SCALE, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun bitmapToJpegBytes(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        return outputStream.toByteArray()
    }
}
