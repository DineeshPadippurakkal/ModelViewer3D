package com.modelviewer3d.ui

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.modelviewer3d.ui.theme.ViewerCyan
import com.modelviewer3d.ui.theme.ViewerCard
import com.modelviewer3d.ui.theme.ViewerControl
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
        shape = RoundedCornerShape(9.dp),
        color = ViewerCard,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box {
            Column {
                Column(
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(32.dp, 4.dp).background(Color(0xFF3B494C), RoundedCornerShape(4.dp)))
                    Row(Modifier.padding(top = 6.dp, start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(instance.model.displayName, fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        if (width >= 260f) {
                            Surface(shape = RoundedCornerShape(20.dp),
                                color = if (instance.interactionMode) Color(0xFF173C40) else ViewerControl) {
                                Text(if (instance.interactionMode) "ORBIT MODE" else "MOVE MODE",
                                    color = if (instance.interactionMode) ViewerCyan else Color(0xFFBAC9CC),
                                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp, letterSpacing = 0.6.sp,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
                            }
                        }
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
                    if (instance.labelsVisible && modelNode != null) {
                        ModelLabel(instance.model.displayName, Modifier.align(Alignment.TopStart)
                            .padding(16.dp))
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
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (width >= 260f) 8.dp else 0.dp)) {
                    ModelControl(
                        label = if (instance.interactionMode) "Orbit" else "Move",
                        description = "Interaction", icon = R.drawable.ic_interaction,
                        active = instance.interactionMode, showText = width >= 260f,
                        onClick = onInteraction, modifier = Modifier.weight(1f))
                    ModelControl(label = "Labels", description = "Labels", icon = R.drawable.ic_labels,
                        active = instance.labelsVisible, showText = width >= 260f,
                        onClick = onLabels, modifier = Modifier.weight(1f))
                    if (width >= 260f) Spacer(Modifier.weight(0.7f))
                    IconButton(onClick = onClose,
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFF401719), contentColor = Color(0xFFFFB4AB))) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Close",
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val inset = 9.dp.toPx()
                val length = 9.dp.toPx()
                val bottom = size.height - 56.dp.toPx()
                for (x in listOf(inset, size.width - inset)) {
                    val directionX = if (x == inset) 1 else -1
                    for (y in listOf(inset, bottom)) {
                        val directionY = if (y == inset) 1 else -1
                        drawLine(ViewerCyan.copy(alpha = 0.75f), Offset(x, y),
                            Offset(x + length * directionX, y), 2.dp.toPx())
                        drawLine(ViewerCyan.copy(alpha = 0.75f), Offset(x, y),
                            Offset(x, y + length * directionY), 2.dp.toPx())
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

@Composable
private fun ModelControl(
    label: String, description: String, icon: Int, active: Boolean,
    showText: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick,
        modifier = modifier.height(48.dp).semantics {
            contentDescription = description
            selected = active
        },
        shape = RoundedCornerShape(13.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (active) ViewerCyan else ViewerControl,
            contentColor = if (active) Color(0xFF00363D) else ViewerCyan)) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        if (showText) {
            Spacer(Modifier.width(6.dp))
            Text(label, fontFamily = FontFamily.Serif, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun ModelLabel(name: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Surface(shape = RoundedCornerShape(20.dp), color = ViewerControl,
            border = BorderStroke(1.dp, ViewerCyan.copy(alpha = 0.6f))) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).background(ViewerCyan, RoundedCornerShape(6.dp)))
                Text(name, color = Color(0xFFC3F5FF), fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }
        Canvas(Modifier.size(90.dp, 54.dp)) {
            val start = Offset(16.dp.toPx(), 0f)
            val elbow = Offset(start.x, 18.dp.toPx())
            val end = Offset(80.dp.toPx(), 46.dp.toPx())
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))
            drawLine(ViewerCyan, start, elbow, 1.5.dp.toPx(), pathEffect = dash)
            drawLine(ViewerCyan, elbow, end, 1.5.dp.toPx(), pathEffect = dash)
            drawCircle(ViewerCyan, 3.dp.toPx(), end)
            drawCircle(ViewerCyan.copy(alpha = 0.6f), 6.dp.toPx(), end, style = Stroke(1.dp.toPx()))
        }
    }
}
