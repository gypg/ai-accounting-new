package com.example.aiaccounting.ui.viewmodel

internal data class AIAssistantEntityReference(
    val id: Long?,
    val name: String,
    val rawIdText: String = "",
    val kind: String
)

internal data class AIAssistantActionEnvelope(
    val actions: List<AIAssistantTypedAction>,
    val reply: String = ""
)

internal sealed class AIAssistantTypedAction {
    data class AddTransaction(
        val amount: Double,
        val transactionTypeRaw: String,
        val categoryRef: AIAssistantEntityReference,
        val accountRef: AIAssistantEntityReference,
        val transferAccountRef: AIAssistantEntityReference? = null,
        val note: String,
        val dateTimestamp: Long
    ) : AIAssistantTypedAction()

    data class CreateAccount(
        val name: String,
        val accountTypeRaw: String,
        val balance: Double
    ) : AIAssistantTypedAction()

    data class CreateCategory(
        val name: String,
        val categoryTypeRaw: String,
        val parentId: Long?
    ) : AIAssistantTypedAction()

    data class Query(
        val target: String
    ) : AIAssistantTypedAction()

    data class Unknown(
        val rawAction: String
    ) : AIAssistantTypedAction()
}
