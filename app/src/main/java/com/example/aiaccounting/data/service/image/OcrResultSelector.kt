package com.example.aiaccounting.data.service.image

internal object OcrResultSelector {
    fun selectBestCandidate(candidates: List<OcrPassAnalysis>): OcrSelectionResult {
        require(candidates.isNotEmpty()) { "candidates must not be empty" }

        val best = candidates.maxWithOrNull(
            compareBy<OcrPassAnalysis> { it.qualityScore }
                .thenBy { signalStrength(it.receiptSignals) }
                .thenBy { it.keyLines.size }
                .thenBy { it.rawText.length }
        ) ?: candidates.first()

        return OcrSelectionResult(
            best = best,
            agreementLevel = calculateAgreementLevel(candidates)
        )
    }

    private fun signalStrength(signals: com.example.aiaccounting.data.service.ImageProcessingService.ReceiptSignals): Int {
        return signals.amounts.size * 3 + signals.paymentMethods.size * 2 + signals.merchants.size * 2 + signals.dates.size
    }

    private fun calculateAgreementLevel(candidates: List<OcrPassAnalysis>): OcrAgreementLevel {
        if (candidates.size < 2) return OcrAgreementLevel.NONE

        val amountAgreement = overlappingDistinctCount(candidates.map { it.receiptSignals.amounts })
        val paymentAgreement = overlappingDistinctCount(candidates.map { it.receiptSignals.paymentMethods })
        val merchantAgreement = overlappingDistinctCount(candidates.map { it.receiptSignals.merchants })
        val dateAgreement = overlappingDistinctCount(candidates.map { it.receiptSignals.dates })
        val agreementScore = amountAgreement * 3 + paymentAgreement * 2 + merchantAgreement * 2 + dateAgreement

        return when {
            agreementScore >= 4 -> OcrAgreementLevel.STRONG
            agreementScore >= 2 -> OcrAgreementLevel.WEAK
            else -> OcrAgreementLevel.NONE
        }
    }

    private fun overlappingDistinctCount(values: List<List<String>>): Int {
        val normalizedSets = values
            .map { list -> list.map { it.trim() }.filter { it.isNotBlank() }.toSet() }
            .filter { it.isNotEmpty() }
        if (normalizedSets.size < 2) return 0

        return normalizedSets
            .reduce { acc, set -> acc.intersect(set) }
            .size
    }
}
