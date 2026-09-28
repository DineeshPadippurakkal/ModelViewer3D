package com.modelviewer3d.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

    val density = LocalDensity.current.density
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0C0E13))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("3D Model Viewer", fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, fontSize = 22.sp,
                modifier = Modifier.weight(1f), maxLines = 1)
            Button(onClick = { pickerVisible = true }, shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(44.dp)) {
                Text("+", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text("Add Model", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clipToBounds()
                .onSizeChanged { canvasSize = it }
                .testTag("model-canvas")
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 20.dp.toPx()
                var x = step / 2
                while (x < size.width) {
                    var y = step / 2
                    while (y < size.height) {
                        drawCircle(Color(0xFF293032), 0.8.dp.toPx(), Offset(x, y))
                        y += step
                    }
                    x += step
                }
            }
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
        }
    }

    if (pickerVisible) {
        ModelPickerDialog(
            onSelect = {
                viewModel.addModel(it)
                val added = viewModel.models.value.last()
                val cardWidth = (canvasSize.width / density - 32f).coerceAtLeast(160f)
                val cardHeight = minOf(cardWidth, (canvasSize.height / density - 40f) / 2f)
                    .coerceAtLeast(160f)
                val slot = models.size % 2
                viewModel.updateBounds(added.id, 16f, 8f + slot * (cardHeight + 16f),
                    cardWidth, cardHeight)
                pickerVisible = false
            },
            onDismiss = { pickerVisible = false }
        )
    }
}
