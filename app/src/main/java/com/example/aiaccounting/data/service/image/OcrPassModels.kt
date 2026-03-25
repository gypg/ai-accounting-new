package com.example.aiaccounting.data.service.image

import android.graphics.Bitmap
import com.example.aiaccounting.data.service.ImageProcessingService

enum class OcrPreprocessingProfile {
    BASE,
    DETAIL,
    DOCUMENT
}

data class ImageQualityMetrics(
    val averageBrightness: Int = 0,
    val contrastRange: Int = 0,
    val nearWhiteRatio: Int = 0,
    val nearBlackRatio: Int = 0,
    val width: Int = 0,
    val height: Int = 0
)

data class OcrPreprocessedVariant(
    val profile: OcrPreprocessingProfile,
    val bitmap: Bitmap,
    val imageQualityMetrics: ImageQualityMetrics
)

enum class OcrAgreementLevel {
    NONE,
    WEAK,
    STRONG
}

data class OcrPassAnalysis(
    val profile: OcrPreprocessingProfile,
    val rawText: String,
    val keyLines: List<String>,
    val receiptSignals: ImageProcessingService.ReceiptSignals,
    val qualityScore: Int,
    val confidence: ImageProcessingService.OcrConfidence,
    val imageQualityMetrics: ImageQualityMetrics
)

data class OcrSelectionResult(
    val best: OcrPassAnalysis,
    val agreementLevel: OcrAgreementLevel
)
