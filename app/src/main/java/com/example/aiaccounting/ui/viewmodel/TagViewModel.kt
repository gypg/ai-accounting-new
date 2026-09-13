package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Tag
import com.example.aiaccounting.data.local.entity.PresetTags
import com.example.aiaccounting.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagUiState())
    val uiState: StateFlow<TagUiState> = _uiState.asStateFlow()

    val allTags = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.insertTag(tag)
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.updateTag(tag)
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
        }
    }

    fun initPresetTags() {
        viewModelScope.launch {
            val count = tagRepository.getTagCount()
            if (count == 0) {
                tagRepository.insertTags(PresetTags.TAGS)
            }
        }
    }
}

data class TagUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)
