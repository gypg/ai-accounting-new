package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.repository.ButlerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
internal class AIAssistantButlerCoordinator(
    private val butlerRepository: ButlerRepository
) {
    fun observeCurrentButler(): Flow<Butler> {
        return butlerRepository.currentButlerId
            .distinctUntilChanged()
            .mapLatest { butlerId ->
                butlerRepository.getButlerByIdSuspend(butlerId)
            }
    }

    suspend fun resolveCurrentButler(cached: Butler?): Butler {
        return cached ?: butlerRepository.getCurrentButler()
    }

    fun switchButler(butlerId: String) {
        butlerRepository.setSelectedButler(butlerId)
    }

    fun getAllButlers(): List<Butler> {
        return butlerRepository.getAllButlers()
    }
}
