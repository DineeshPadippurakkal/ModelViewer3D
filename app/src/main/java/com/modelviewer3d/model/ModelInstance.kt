package com.modelviewer3d.model

data class ModelInstance(
    val id: Long,
    val model: ModelItem,
    // Container bounds are in dp; rendering transforms stay in the UI.
    val offsetX: Float,
    val offsetY: Float,
    val width: Float = 210.0f,
    val height: Float = 210.0f,
    val interactionMode: Boolean = false,
    val labelsVisible: Boolean = false
)
