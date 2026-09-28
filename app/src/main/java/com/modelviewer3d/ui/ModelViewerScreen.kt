package com.modelviewer3d.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.modelviewer3d.viewmodel.ModelViewerViewModel
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberRenderer

@Composable
fun ModelViewerScreen(
    modifier: Modifier = Modifier,
    viewModel: ModelViewerViewModel = viewModel()
) {
    val models by viewModel.models.collectAsStateWithLifecycle()
    var pickerVisible by rememberSaveable { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val engine = rememberEngine()
    val renderer = rememberRenderer(engine)
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val environment = rememberEnvironment(environmentLoader)

    Box(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds()
            .onSizeChanged { canvasSize = it }
            .testTag("model-canvas")
    ) {
        if (models.isEmpty()) {
            Text(
                text = "Tap Add Model to get started",
                modifier = Modifier.align(Alignment.Center)
            )
        }
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            models.forEach { instance ->
                key(instance.id) {
                    ModelContainer(
                        instance = instance,
                        canvasSize = canvasSize,
                        engine = engine,
                        renderer = renderer,
                        modelLoader = modelLoader,
                        materialLoader = materialLoader,
                        environmentLoader = environmentLoader,
                        environment = environment,
                        onInteraction = { viewModel.toggleInteraction(instance.id) },
                        onLabels = { viewModel.toggleLabels(instance.id) },
                        onClose = { viewModel.removeModel(instance.id) },
                        onBoundsChanged = { x, y, width, height ->
                            viewModel.updateBounds(instance.id, x, y, width, height)
                        }
                    )
                }
            }
        }
        Button(
            onClick = { pickerVisible = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Text("Add Model")
        }
    }

    if (pickerVisible) {
        ModelPickerDialog(
            onSelect = {
                viewModel.addModel(it)
                pickerVisible = false
            },
            onDismiss = { pickerVisible = false }
        )
    }
}
