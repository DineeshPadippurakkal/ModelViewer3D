package com.modelviewer3d

import com.modelviewer3d.model.ModelItem
import com.modelviewer3d.viewmodel.ModelViewerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelViewerViewModelTest {
    @Test
    fun duplicateModelsHaveIndependentStateAndUniqueIds() {
        val viewModel = ModelViewerViewModel()
        repeat(5) { viewModel.addModel(ModelItem.Bulb) }
        val initial = viewModel.models.value
        assertEquals(5, initial.map { it.id }.distinct().size)
        assertEquals(5, initial.map { it.offsetX to it.offsetY }.distinct().size)
        assertTrue(initial.all { !it.interactionMode && !it.labelsVisible })

        val id = initial.first().id
        viewModel.toggleInteraction(id)
        viewModel.toggleLabels(id)
        assertTrue(viewModel.models.value.first().interactionMode)
        assertTrue(viewModel.models.value.first().labelsVisible)
        assertTrue(viewModel.models.value.drop(1).all { !it.interactionMode && !it.labelsVisible })

        viewModel.toggleInteraction(id)
        viewModel.toggleLabels(id)
        assertFalse(viewModel.models.value.first().interactionMode)
        assertFalse(viewModel.models.value.first().labelsVisible)
    }

    @Test
    fun closeRemovesOnlyItsInstanceWithoutMovingOtherModels() {
        val viewModel = ModelViewerViewModel()
        ModelItem.entries.forEach(viewModel::addModel)
        val removed = viewModel.models.value[1]
        val remaining = viewModel.models.value.filterNot { it.id == removed.id }
        viewModel.removeModel(removed.id)
        assertEquals(remaining, viewModel.models.value)

        viewModel.addModel(ModelItem.Lungs)
        val added = viewModel.models.value.last()
        assertTrue(added.id > remaining.maxOf { it.id })
        assertEquals(remaining, viewModel.models.value.dropLast(1))
        assertEquals(5, viewModel.models.value.size)
    }

    @Test
    fun committingBoundsChangesOnlyTheSelectedInstance() {
        val viewModel = ModelViewerViewModel()
        viewModel.addModel(ModelItem.Bulb)
        viewModel.addModel(ModelItem.Microscope)
        val first = viewModel.models.value.first()
        val second = viewModel.models.value.last()
        viewModel.updateBounds(first.id, 5.0f, 30.0f, 250.0f, 250.0f)
        assertEquals(first.copy(offsetX = 5.0f, offsetY = 30.0f, width = 250.0f, height = 250.0f),
            viewModel.models.value.first())
        assertEquals(second, viewModel.models.value.last())
    }
}
