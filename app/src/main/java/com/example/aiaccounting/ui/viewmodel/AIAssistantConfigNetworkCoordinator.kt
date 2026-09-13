package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class AIAssistantConfigNetworkCoordinator(
    private val aiConfigRepository: AIConfigRepository,
    private val networkUtils: NetworkUtils
) {
    fun observeConfig(): Flow<Pair<AIConfig, Boolean>> {
        return combine(
            aiConfigRepository.getAIConfig(),
            aiConfigRepository.getUseBuiltin()
        ) { config, useBuiltin ->
            config to useBuiltin
        }
    }

    suspend fun isNetworkAvailable(): Boolean {
        return networkUtils.isNetworkAvailable()
    }
}
