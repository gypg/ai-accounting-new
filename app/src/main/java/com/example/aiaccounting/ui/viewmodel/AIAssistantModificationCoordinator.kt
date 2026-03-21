package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.TransactionModificationHandler

internal data class PendingModificationState(
    val intent: TransactionModificationHandler.ModificationIntent,
    val confirmation: TransactionModificationHandler.ModificationConfirmation
)

internal sealed class ModificationFlowResult {
    data class StartConfirmation(
        val pendingState: PendingModificationState,
        val reply: String
    ) : ModificationFlowResult()

    data class Finish(val reply: String) : ModificationFlowResult()
}

internal class AIAssistantModificationCoordinator(
    private val transactionModificationHandler: TransactionModificationHandler
) {
    suspend fun beginModification(
        message: String,
        butlerId: String
    ): ModificationFlowResult {
        val modificationRequest = transactionModificationHandler.detectModificationIntent(message)
        if (modificationRequest.targetTransaction == null) {
            return ModificationFlowResult.Finish(
                "抱歉，没有找到相关的交易记录。请提供更详细的信息，比如交易金额或时间。"
            )
        }

        val confirmation = transactionModificationHandler.generateModificationConfirmation(modificationRequest)
            ?: return ModificationFlowResult.Finish("抱歉，无法生成修改确认信息。")

        return ModificationFlowResult.StartConfirmation(
            pendingState = PendingModificationState(
                intent = modificationRequest.intent,
                confirmation = confirmation
            ),
            reply = transactionModificationHandler.generatePersonalityConfirmationMessage(butlerId, confirmation)
        )
    }

    suspend fun continueModification(
        message: String,
        butlerId: String,
        pendingState: PendingModificationState
    ): ModificationFlowResult {
        return when {
            transactionModificationHandler.isCancellation(message) -> {
                val reply = when (butlerId) {
                    "xiaocainiang" -> "好的主人～已取消修改。🌸"
                    "taotao" -> "好的～取消啦！✨"
                    "guchen" -> "（翻个身）...不改了？...我继续睡了..."
                    "suqian" -> "（平静地）...已取消。"
                    "yishuihan" -> "（微笑）好的，已为您取消。"
                    else -> "已取消修改。"
                }
                ModificationFlowResult.Finish(reply)
            }

            transactionModificationHandler.isConfirmation(message) -> {
                val result = when (pendingState.intent) {
                    TransactionModificationHandler.ModificationIntent.DELETE_LAST_TRANSACTION,
                    TransactionModificationHandler.ModificationIntent.DELETE_SPECIFIC_TRANSACTION -> {
                        transactionModificationHandler.executeDelete(pendingState.confirmation.transaction)
                    }
                    else -> {
                        transactionModificationHandler.executeModification(pendingState.confirmation)
                    }
                }
                if (result.success) {
                    ModificationFlowResult.Finish(
                        transactionModificationHandler.generatePersonalitySuccessMessage(butlerId, result)
                    )
                } else {
                    ModificationFlowResult.Finish("修改失败：${result.message}")
                }
            }

            else -> ModificationFlowResult.Finish("请回复\"确认\"执行修改，或回复\"取消\"放弃修改。")
        }
    }
}
