package com.modelviewer3d.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.modelviewer3d.R
import com.modelviewer3d.model.ModelInstance
import com.modelviewer3d.threed.ModelInteraction
import io.github.sceneview.Scene
import io.github.sceneview.environment.Environment
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberScene
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ModelContainer(
    instance: ModelInstance,
    canvasSize: IntSize,
    engine: Engine,
    renderer: Renderer,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    environmentLoader: EnvironmentLoader,
    environment: Environment,
    onInteraction: () -> Unit,
    onLabels: () -> Unit,
    onClose: () -> Unit,
    onBoundsChanged: (Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val canvasWidth = canvasSize.width / density
    val canvasHeight = canvasSize.height / density
    val minWidth = min(160.0f, canvasWidth)
    val minHeight = min(160.0f, canvasHeight)
    var width by remember { mutableFloatStateOf(instance.width.coerceIn(minWidth, canvasWidth)) }
    var height by remember { mutableFloatStateOf(instance.height.coerceIn(minHeight, canvasHeight)) }
    var offsetX by remember {
        mutableFloatStateOf(instance.offsetX.coerceIn(0.0f, canvasWidth - width))
    }
    var offsetY by remember {
        mutableFloatStateOf(instance.offsetY.coerceIn(0.0f, canvasHeight - height))
    }

    LaunchedEffect(canvasSize, density, instance.offsetX, instance.offsetY, instance.width, instance.height) {
        width = instance.width.coerceIn(minWidth, canvasWidth)
        height = instance.height.coerceIn(minHeight, canvasHeight)
        offsetX = instance.offsetX.coerceIn(0.0f, canvasWidth - width)
        offsetY = instance.offsetY.coerceIn(0.0f, canvasHeight - height)
        onBoundsChanged(offsetX, offsetY, width, height)
    }

    val context = LocalContext.current
    val scene = rememberScene(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(0.0f, 0.0f, 2.5f)
    }
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val nodes = remember(modelNode) { listOfNotNull(modelNode) }
    val interaction = remember(modelNode, cameraNode, density) {
        modelNode?.let { ModelInteraction(context, it, cameraNode, density) }
    }
    SideEffect { interaction?.enabled = instance.interactionMode }

    LaunchedEffect(modelLoader, instance.id) {
        try {
            val buffer = withContext(Dispatchers.IO) {
                context.assets.open(instance.model.assetPath).use { input ->
                    ByteBuffer.wrap(input.readBytes())
                }
            }
            // Explicitly return to the engine's thread, including with a test coroutine dispatcher.
            withContext(Dispatchers.Main.immediate) {
                val model = modelLoader.createModel(buffer)
                try {
                    modelNode = ModelNode(
                        modelInstance = model.instance,
                        autoAnimate = false,
                        scaleToUnits = 1.0f,
                        centerOrigin = Position(0.0f, 0.0f, 0.0f)
                    )
                } catch (exception: Exception) {
                    modelLoader.destroyModel(model)
                    throw exception
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e("ModelViewer", "Could not load ${instance.model.assetPath}", exception)
            error = "Could not load ${instance.model.displayName}. Check the file in assets/models."
        } finally {
            isLoading = false
        }
    }

    Surface(
        modifier = modifier
            .offset { IntOffset((offsetX * density).roundToInt(), (offsetY * density).roundToInt()) }
            .size(width.dp, height.dp)
            .testTag("model-${instance.id}"),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onInteraction,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (instance.interactionMode) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).height(48.dp).semantics { selected = instance.interactionMode }
                ) {
                    Icon(painterResource(R.drawable.ic_interaction), contentDescription = "Interaction")
                }
                IconButton(
                    onClick = onLabels,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (instance.labelsVisible) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).height(48.dp).semantics { selected = instance.labelsVisible }
                ) {
                    Icon(painterResource(R.drawable.ic_labels), contentDescription = "Labels")
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Close")
                }
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("gesture-${instance.id}"),
                contentAlignment = Alignment.Center
            ) {
                Scene(
                    modifier = Modifier.fillMaxSize().semantics {
                        if (modelNode != null) {
                            contentDescription = "${instance.model.displayName} 3D model"
                        }
                    },
                    engine = engine,
                    renderer = renderer,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    environmentLoader = environmentLoader,
                    environment = environment,
                    scene = scene,
                    cameraNode = cameraNode,
                    childNodes = nodes,
                    cameraManipulator = null,
                    onGestureListener = null,
                    // Consume native touches before SceneView's picking and default gesture handlers.
                    onViewUpdated = { setOnTouchListener(interaction) }
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopStart),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = instance.model.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(4.dp)
                    )
                }
                if (!instance.interactionMode) {
                    ModelContainerGestures(
                        enabled = true,
                        modifier = Modifier.fillMaxSize(),
                        onDrag = { pan ->
                            offsetX = (offsetX + pan.x / density).coerceIn(0.0f, canvasWidth - width)
                            offsetY = (offsetY + pan.y / density).coerceIn(0.0f, canvasHeight - height)
                        },
                        onResize = { zoom ->
                            if (zoom.isFinite() && zoom > 0.0f) {
                                val minScale = max(minWidth / width, minHeight / height)
                                val maxScale = min(canvasWidth / width, canvasHeight / height)
                                val scale = zoom.coerceIn(minScale, maxScale)
                                val newWidth = (width * scale).coerceIn(minWidth, canvasWidth)
                                val newHeight = (height * scale).coerceIn(minHeight, canvasHeight)
                                offsetX = (offsetX + (width - newWidth) / 2)
                                    .coerceIn(0.0f, canvasWidth - newWidth)
                                offsetY = (offsetY + (height - newHeight) / 2)
                                    .coerceIn(0.0f, canvasHeight - newHeight)
                                width = newWidth
                                height = newHeight
                            }
                        },
                        onFinished = { onBoundsChanged(offsetX, offsetY, width, height) }
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator()
                }
                error?.let { message ->
                    Surface {
                        Text(text = message, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }

    DisposableEffect(modelLoader, scene) {
        onDispose {
            // Remove this asset before releasing it; the shared loader and engine remain alive.
            modelNode?.let { node ->
                scene.removeEntities(node.model.entities)
                node.destroy()
                modelLoader.destroyModel(node.model)
            }
        }
    }
}
