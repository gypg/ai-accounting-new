package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.exporter.LogCsvExporter
import com.example.aiaccounting.data.local.entity.AppLogEntry
import com.example.aiaccounting.data.repository.AppLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogBrowserViewModel @Inject constructor(
    private val appLogRepository: AppLogRepository,
    private val logCsvExporter: LogCsvExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogBrowserUiState())
    val uiState: StateFlow<LogBrowserUiState> = _uiState.asStateFlow()

    private val logs = appLogRepository.getRecentLogs(1000)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredLogs = combine(logs, _uiState) { items, state ->
        items.filter { entry -> matchesLog(entry, state) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun setSourceFilter(source: String) {
        _uiState.value = _uiState.value.copy(sourceFilter = source)
    }

    fun setActionFilter(action: String) {
        _uiState.value = _uiState.value.copy(actionFilter = action)
    }

    fun setStatusFilter(status: String) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearLogs() {
        viewModelScope.launch {
            try {
                appLogRepository.clearLogs()
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun exportFilteredLogs(onComplete: (File?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExporting = true, error = null)
                val result = logCsvExporter.exportLogs(filteredLogs.value)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportFile = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
                onComplete(result.getOrNull())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isExporting = false, error = e.message)
                onComplete(null)
            }
        }
    }

    fun buildClipboardText(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return filteredLogs.value.joinToString("\n\n") { entry ->
            buildString {
                append("[")
                append(entry.level)
                append("] ")
                append(formatter.format(Date(entry.timestamp)))
                append("\n")
                append(entry.message)
                append("\n")
                append(entry.source)
                append(" / ")
                append(entry.category)
                entry.traceId?.takeIf { it.isNotBlank() }?.let {
                    append(" / traceId=")
                    append(it)
                }
                entry.entityType?.takeIf { it.isNotBlank() }?.let {
                    append(" / entityType=")
                    append(it)
                }
                entry.entityId?.takeIf { it.isNotBlank() }?.let {
                    append(" / entityId=")
                    append(it)
                }
                entry.details?.takeIf { it.isNotBlank() }?.let {
                    append("\n")
                    append(it)
                }
            }
        }
    }

    private fun matchesLog(entry: AppLogEntry, state: LogBrowserUiState): Boolean {
        val query = state.query.trim()
        val queryMatches = if (query.isBlank()) {
            true
        } else {
            listOf(
                entry.level,
                entry.source,
                entry.category,
                entry.message,
                entry.details.orEmpty(),
                entry.traceId.orEmpty(),
                entry.entityType.orEmpty(),
                entry.entityId.orEmpty()
            ).any { it.contains(query, ignoreCase = true) }
        }

        val statusMatches = when (state.statusFilter) {
            "success" -> entry.level != "ERROR" && entry.level != "CRITICAL"
            "error" -> entry.level == "ERROR" || entry.level == "CRITICAL"
            else -> true
        }

        return queryMatches && statusMatches
    }
}

data class LogBrowserUiState(
    val query: String = "",
    val sourceFilter: String = "all",
    val actionFilter: String = "all",
    val statusFilter: String = "all",
    val isExporting: Boolean = false,
    val lastExportFile: File? = null,
    val error: String? = null
)
