package com.modelviewer3d.viewmodel

import androidx.lifecycle.ViewModel
import com.modelviewer3d.model.ModelInstance
import com.modelviewer3d.model.ModelItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ModelViewerViewModel : ViewModel() {
    private val _models = MutableStateFlow<List<ModelInstance>>(emptyList())
    val models = _models.asStateFlow()
    private var nextId = 1L

    fun addModel(model: ModelItem) {
        val offset = ((nextId - 1) % 5) * 24.0f
        val instance = ModelInstance(
            id = nextId++,
            model = model,
            offsetX = 12.0f + offset,
            offsetY = 72.0f + offset
        )
        _models.update { it + instance }
    }

    fun updateBounds(id: Long, offsetX: Float, offsetY: Float, width: Float, height: Float) {
        _models.update { models ->
            models.map {
                if (it.id == id) {
                    it.copy(offsetX = offsetX, offsetY = offsetY, width = width, height = height)
                } else it
            }
        }
    }

    fun removeModel(id: Long) {
        _models.update { models -> models.filterNot { it.id == id } }
    }

    fun toggleInteraction(id: Long) {
        _models.update { models ->
            models.map { if (it.id == id) it.copy(interactionMode = !it.interactionMode) else it }
        }
    }

    fun toggleLabels(id: Long) {
        _models.update { models ->
            models.map { if (it.id == id) it.copy(labelsVisible = !it.labelsVisible) else it }
        }
    }
}
