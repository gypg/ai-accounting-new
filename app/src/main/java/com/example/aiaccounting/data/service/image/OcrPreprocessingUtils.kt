package com.example.aiaccounting.data.service.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlin.math.max
import kotlin.math.min

internal object OcrPreprocessingUtils {
    private const val MAX_EDGE = 2200
    private const val JPEG_QUALITY = 92
    private const val BASE_CONTRAST_SCALE = 1.15f
    private const val DETAIL_CONTRAST_SCALE = 1.3f
    private const val DETAIL_BRIGHTNESS_SHIFT = 10f
    private const val DOCUMENT_THRESHOLD = 170

    fun preprocessImage(context: Context, uri: Uri): ByteArray? {
        return preprocessVariants(context, uri).firstOrNull()?.bitmap?.let(::bitmapToJpegBytes)
    }

    fun preprocessVariants(context: Context, uri: Uri, screenshotLikely: Boolean = false): List<OcrPreprocessedVariant> {
        return try {
            val variants = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val original = BitmapFactory.decodeStream(inputStream) ?: return@use emptyList<OcrPreprocessedVariant>()
                val normalized = scaleDownIfNeeded(original)
                val baseMetrics = calculateImageQualityMetrics(normalized)
                buildList {
                    add(
                        OcrPreprocessedVariant(
                            profile = OcrPreprocessingProfile.BASE,
                            bitmap = enhanceContrastAndGrayscale(normalized, BASE_CONTRAST_SCALE),
                            imageQualityMetrics = baseMetrics
                        )
                    )
                    if (screenshotLikely || shouldAddScreenshotProfile(baseMetrics)) {
                        add(
                            OcrPreprocessedVariant(
                                profile = OcrPreprocessingProfile.SCREENSHOT,
                                bitmap = enhanceScreenshotText(normalized),
                                imageQualityMetrics = baseMetrics
                            )
                        )
                    }
                    add(
                        OcrPreprocessedVariant(
                            profile = OcrPreprocessingProfile.DETAIL,
                            bitmap = enhanceContrastAndBrightness(normalized, DETAIL_CONTRAST_SCALE, DETAIL_BRIGHTNESS_SHIFT),
                            imageQualityMetrics = baseMetrics
                        )
                    )
                    if (!screenshotLikely && shouldAddDocumentProfile(baseMetrics)) {
                        add(
                            OcrPreprocessedVariant(
                                profile = OcrPreprocessingProfile.DOCUMENT,
                                bitmap = toThresholdDocument(normalized),
                                imageQualityMetrics = baseMetrics
                            )
                        )
                    }
                }
            }
            variants ?: emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDimension = max(bitmap.width, bitmap.height)
        if (maxDimension <= MAX_EDGE) return bitmap

        val ratio = MAX_EDGE.toFloat() / maxDimension.toFloat()
        val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun enhanceContrastAndGrayscale(bitmap: Bitmap, contrastScale: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val grayscaleMatrix = ColorMatrix().apply { setSaturation(0f) }
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrastScale, 0f, 0f, 0f, 0f,
                0f, contrastScale, 0f, 0f, 0f,
                0f, 0f, contrastScale, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        grayscaleMatrix.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(grayscaleMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun enhanceContrastAndBrightness(bitmap: Bitmap, contrastScale: Float, brightnessShift: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = ColorMatrix(
            floatArrayOf(
                contrastScale, 0f, 0f, 0f, brightnessShift,
                0f, contrastScale, 0f, 0f, brightnessShift,
                0f, 0f, contrastScale, 0f, brightnessShift,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun toThresholdDocument(bitmap: Bitmap): Bitmap {
        val grayscale = enhanceContrastAndGrayscale(bitmap, DETAIL_CONTRAST_SCALE)
        val output = Bitmap.createBitmap(grayscale.width, grayscale.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until grayscale.width) {
            for (y in 0 until grayscale.height) {
                val pixel = grayscale.getPixel(x, y)
                val luminance = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                val thresholded = if (luminance >= DOCUMENT_THRESHOLD) 255 else 0
                output.setPixel(x, y, Color.argb(255, thresholded, thresholded, thresholded))
            }
        }
        return output
    }

    private fun shouldAddDocumentProfile(metrics: ImageQualityMetrics): Boolean {
        return metrics.contrastRange < 80 || metrics.nearWhiteRatio >= 55 || metrics.averageBrightness >= 150
    }

    private fun shouldAddScreenshotProfile(metrics: ImageQualityMetrics): Boolean {
        return metrics.width >= 900 && metrics.height >= 1200 ||
            metrics.height >= 900 && metrics.nearWhiteRatio >= 30 && metrics.contrastRange >= 40
    }

    private fun enhanceScreenshotText(bitmap: Bitmap): Bitmap {
        return enhanceContrastAndBrightness(
            enhanceContrastAndGrayscale(bitmap, 1.35f),
            1.1f,
            6f
        )
    }

    private fun calculateImageQualityMetrics(bitmap: Bitmap): ImageQualityMetrics {
        val stepX = max(1, bitmap.width / 64)
        val stepY = max(1, bitmap.height / 64)
        var pixelCount = 0
        var totalBrightness = 0
        var minBrightness = 255
        var maxBrightness = 0
        var nearWhiteCount = 0
        var nearBlackCount = 0

        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                totalBrightness += brightness
                minBrightness = min(minBrightness, brightness)
                maxBrightness = max(maxBrightness, brightness)
                if (brightness >= 235) nearWhiteCount += 1
                if (brightness <= 20) nearBlackCount += 1
                pixelCount += 1
            }
        }

        if (pixelCount == 0) {
            return ImageQualityMetrics(width = bitmap.width, height = bitmap.height)
        }

        return ImageQualityMetrics(
            averageBrightness = totalBrightness / pixelCount,
            contrastRange = maxBrightness - minBrightness,
            nearWhiteRatio = nearWhiteCount * 100 / pixelCount,
            nearBlackRatio = nearBlackCount * 100 / pixelCount,
            width = bitmap.width,
            height = bitmap.height
        )
    }

    private fun bitmapToJpegBytes(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        return outputStream.toByteArray()
    }
}
