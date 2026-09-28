package com.modelviewer3d

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.modelviewer3d.viewmodel.ModelViewerViewModel
import io.github.sceneview.SceneView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import java.io.File

class ModelViewerScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun pickerLoadsFiveModelsAndCloseKeepsOtherInstances() {
        val names = listOf("Bulb", "Lungs", "Microscope", "Fiagena", "Solar System")
        names.forEach { name ->
            addModel(name)
            waitForModel(name)
        }
        names.forEach { name ->
            compose.onAllNodesWithContentDescription("$name 3D model").assertCountEquals(1)
        }

        val first = hasAnyAncestor(hasTestTag("model-1"))
        val second = hasAnyAncestor(hasTestTag("model-2"))
        compose.onNode(first and hasContentDescription("Interaction"))
            .performTouchInput { click(Offset(center.x, 4f)) }.assertIsSelected()
        compose.onNode(first and hasContentDescription("Labels"))
            .performTouchInput { click(Offset(center.x, 4f)) }.assertIsSelected()
        compose.onNode(second and hasContentDescription("Interaction")).assertIsNotSelected()
        compose.onNode(second and hasContentDescription("Labels")).assertIsNotSelected()

        compose.activityRule.scenario.recreate()
        names.forEach(::waitForModel)
        compose.onNode(first and hasContentDescription("Interaction")).assertIsSelected()
        compose.onNode(first and hasContentDescription("Labels")).assertIsSelected()

        compose.onNode(first and hasContentDescription("Close"))
            .performTouchInput { click(Offset(center.x, 4f)) }
        compose.onNodeWithTag("model-1").assertDoesNotExist()
        names.drop(1).forEach { name ->
            compose.onAllNodesWithContentDescription("$name 3D model").assertCountEquals(1)
        }
        addModel("Bulb")
        waitForModel("Bulb")
        addModel("Bulb")
        waitForModel("Bulb", count = 2)
        compose.onNode(hasAnyAncestor(hasTestTag("model-7")) and hasContentDescription("Close")).performClick()
        compose.onAllNodesWithContentDescription("Bulb 3D model").assertCountEquals(1)
        compose.onNodeWithTag("model-6").assertIsDisplayed()
    }

    @Test
    fun eachAssetLoadsIndividually() {
        listOf("Bulb", "Fiagena", "Lungs", "Microscope", "Solar System").forEach { name ->
            addModel(name)
            waitForModel(name)
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithText("Tap Add Model to get started").assertIsDisplayed()
        }
    }

    @Test
    fun dragAndPinchKeepIndependentBoundsAndTheSameRenderingObjects() {
        listOf("Bulb", "Microscope", "Lungs").forEach {
            addModel(it)
            waitForModel(it)
        }
        val views = compose.runOnIdle { sceneViews(compose.activity.window.decorView) }
        val nodes = compose.runOnIdle { views.map { it.childNodes.single() } }
        val canvas = compose.onNodeWithTag("model-canvas").fetchSemanticsNode().boundsInRoot
        val bulbTarget = canvas.topLeft + Offset(4f, 4f)
        dragTo(1, bulbTarget)
        val microscope = bounds(2)
        val microscopeTarget = canvas.bottomRight - Offset(microscope.width + 4, microscope.height + 4)
        dragTo(2, microscopeTarget)
        val lungs = bounds(3)
        val lungsTarget = canvas.center - Offset(lungs.width / 2, lungs.height / 2)
        dragTo(3, lungsTarget)
        assertPosition(bulbTarget, bounds(1).topLeft)
        assertPosition(microscopeTarget, bounds(2).topLeft)
        assertPosition(lungsTarget, bounds(3).topLeft)

        val before = (1L..3L).map(::bounds)
        pinch(1, 1.35f)
        assertTrue(bounds(1).width > before[0].width + 20)
        assertEquals(before[1], bounds(2))
        assertEquals(before[2], bounds(3))
        val largerBulb = bounds(1)
        pinch(2, 0.6f)
        assertTrue(bounds(2).width < before[1].width - 20)
        assertEquals(largerBulb, bounds(1))
        assertEquals(before[2], bounds(3))

        val control = hasAnyAncestor(hasTestTag("model-3")) and hasContentDescription("Labels")
        compose.onNode(control).performTouchInput {
            down(center)
            moveBy(Offset(60f, 90f))
            up()
        }
        assertEquals(before[2], bounds(3))
        compose.runOnIdle {
            val current = sceneViews(compose.activity.window.decorView)
            views.forEachIndexed { index, view ->
                assertTrue(current.any { it === view })
                assertSame(nodes[index], view.childNodes.single())
            }
        }
        val placed = (1L..3L).map { bounds(it).translate(-canvas.topLeft) }
        InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let { directory ->
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            try {
                File(directory, "free-canvas.png").outputStream().use {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally {
                screenshot.recycle()
            }
        }
        compose.activityRule.scenario.recreate()
        listOf("Bulb", "Microscope", "Lungs").forEach(::waitForModel)
        val recreatedCanvas = compose.onNodeWithTag("model-canvas").fetchSemanticsNode().boundsInRoot
        (1L..3L).forEach {
            assertEquals(placed[(it - 1).toInt()], bounds(it).translate(-recreatedCanvas.topLeft))
        }
    }

    @Test
    fun dragCommitsOnlyOnReleaseAndStaysWithinCanvas() {
        addModel("Bulb")
        waitForModel("Bulb")
        val viewModel = compose.runOnIdle {
            ViewModelProvider(compose.activity)[ModelViewerViewModel::class.java]
        }
        val initialState = viewModel.models.value
        val canvas = compose.onNodeWithTag("model-canvas")
        val canvasBounds = canvas.fetchSemanticsNode().boundsInRoot
        val gesture = compose.onNodeWithTag("gesture-1").fetchSemanticsNode().boundsInRoot
        val start = gesture.center - canvasBounds.topLeft
        canvas.performTouchInput {
            down(start)
            moveTo(start + Offset(50f, 70f))
        }
        compose.runOnIdle { assertSame(initialState, viewModel.models.value) }
        assertTrue(bounds(1).top > initialState[0].offsetY * compose.activity.resources.displayMetrics.density)
        canvas.performTouchInput { up() }
        compose.runOnIdle { assertTrue(initialState != viewModel.models.value) }
        dragTo(1, canvasBounds.topLeft - Offset(1000f, 1000f))
        assertPosition(canvasBounds.topLeft, bounds(1).topLeft)
        pinch(1, 5f)
        assertTrue(bounds(1).width <= canvasBounds.width + 1)
        assertTrue(bounds(1).height <= canvasBounds.height + 1)
        pinch(1, 0.05f)
        val minSize = 160f * compose.activity.resources.displayMetrics.density
        assertTrue(bounds(1).width >= minSize - 1)
        assertTrue(bounds(1).height >= minSize - 1)
    }

    @Test
    fun interactionModeRotatesAndZoomsWithoutChangingContainerBounds() {
        addModel("Bulb")
        waitForModel("Bulb")
        val viewModel = compose.runOnIdle {
            ViewModelProvider(compose.activity)[ModelViewerViewModel::class.java]
        }
        val bulbView = compose.runOnIdle { sceneViews(compose.activity.window.decorView).single() }
        val bulbNode = compose.runOnIdle { bulbView.childNodes.single() }
        val originalRotation = compose.runOnIdle { bulbNode.rotation }
        val originalDistance = compose.runOnIdle { bulbView.cameraNode.position.z }

        val initialBounds = bounds(1)
        dragTo(1, initialBounds.topLeft + Offset(40f, 60f))
        assertTrue(bounds(1).topLeft != initialBounds.topLeft)
        val movedBounds = bounds(1)
        pinch(1, 1.5f)
        assertTrue(bounds(1).width > movedBounds.width)
        compose.runOnIdle {
            assertEquals(originalRotation, bulbNode.rotation)
            assertEquals(originalDistance, bulbView.cameraNode.position.z)
        }

        toggleInteraction(1)
        val fixedBounds = bounds(1)
        val stateBeforeInteraction = viewModel.models.value
        dragTo(1, fixedBounds.topLeft + Offset(90f, 55f))
        assertEquals(fixedBounds, bounds(1))
        val rotated = compose.runOnIdle { bulbNode.rotation }
        assertTrue(originalRotation != rotated)
        pinch(1, 1.5f)
        assertEquals(fixedBounds, bounds(1))
        val zoomedDistance = compose.runOnIdle { bulbView.cameraNode.position.z }
        assertTrue(zoomedDistance < originalDistance)
        compose.runOnIdle {
            assertEquals(rotated, bulbNode.rotation)
            assertSame(stateBeforeInteraction, viewModel.models.value)
            assertSame(bulbNode, bulbView.childNodes.single())
        }
        pinch(1, 0.5f)
        compose.runOnIdle { assertTrue(bulbView.cameraNode.position.z > zoomedDistance) }
        assertEquals(fixedBounds, bounds(1))
        val keptDistance = compose.runOnIdle { bulbView.cameraNode.position.z }

        toggleInteraction(1)
        val canvas = compose.onNodeWithTag("model-canvas").fetchSemanticsNode().boundsInRoot
        dragTo(1, canvas.topLeft + Offset(4f, 4f))
        assertPosition(canvas.topLeft + Offset(4f, 4f), bounds(1).topLeft)
        pinch(1, 0.6f)
        assertTrue(bounds(1).width < fixedBounds.width)
        compose.runOnIdle {
            assertEquals(rotated, bulbNode.rotation)
            assertEquals(keptDistance, bulbView.cameraNode.position.z)
        }

        dragTo(1, canvas.topLeft + Offset(4f, 4f))
        addModel("Lungs")
        waitForModel("Lungs")
        val lungsView = compose.runOnIdle {
            sceneViews(compose.activity.window.decorView).single { it !== bulbView }
        }
        val lungsNode = compose.runOnIdle { lungsView.childNodes.single() }
        val lungsRotation = compose.runOnIdle { lungsNode.rotation }
        val lungsBounds = bounds(2)
        toggleInteraction(1)
        val bulbBounds = bounds(1)
        dragTo(1, bulbBounds.topLeft + Offset(15f, 100f))
        assertEquals(bulbBounds, bounds(1))
        assertEquals(lungsBounds, bounds(2))
        val bulbRotation = compose.runOnIdle { bulbNode.rotation }
        assertTrue(bulbRotation != rotated)
        dragTo(2, lungsBounds.topLeft + Offset(20f, 200f))
        assertTrue(bounds(2).topLeft != lungsBounds.topLeft)
        assertEquals(bulbBounds, bounds(1))
        compose.runOnIdle {
            assertEquals(lungsRotation, lungsNode.rotation)
            assertEquals(bulbRotation, bulbNode.rotation)
            assertSame(bulbNode, bulbView.childNodes.single())
            assertSame(lungsNode, lungsView.childNodes.single())
            assertTrue(sceneViews(compose.activity.window.decorView).any { it === bulbView })
        }
    }

    @Test
    fun compactControlsFitAndZoomRemainsBounded() {
        addModel("Bulb")
        waitForModel("Bulb")
        val view = compose.runOnIdle { sceneViews(compose.activity.window.decorView).single() }
        pinch(1, 0.05f)
        val container = bounds(1)
        val minTouchSize = 48f * compose.activity.resources.displayMetrics.density
        listOf("Interaction", "Labels", "Close").forEach { name ->
            val button = compose.onNode(
                hasAnyAncestor(hasTestTag("model-1")) and hasContentDescription(name)
            ).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(button.width >= minTouchSize - 1)
            assertTrue(button.height >= minTouchSize - 1)
            assertTrue(button.left >= container.left && button.right <= container.right)
        }
        toggleInteraction(1)
        repeat(3) { pinch(1, 4f) }
        compose.runOnIdle { assertEquals(1.5f, view.cameraNode.position.z, 0.001f) }
        repeat(3) { pinch(1, 0.1f) }
        compose.runOnIdle { assertEquals(6.0f, view.cameraNode.position.z, 0.001f) }
        assertEquals(container, bounds(1))
    }

    private fun toggleInteraction(id: Long) {
        compose.onNode(hasAnyAncestor(hasTestTag("model-$id")) and hasContentDescription("Interaction"))
            .performTouchInput { click(Offset(center.x, 4f)) }
    }

    private fun bounds(id: Long): Rect =
        compose.onNodeWithTag("model-$id").fetchSemanticsNode().boundsInRoot

    private fun assertPosition(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 2f)
        assertEquals(expected.y, actual.y, 2f)
    }

    private fun dragTo(id: Long, target: Offset) {
        val canvas = compose.onNodeWithTag("model-canvas")
        val canvasBounds = canvas.fetchSemanticsNode().boundsInRoot
        val modelBounds = bounds(id)
        val gesture = compose.onNodeWithTag("gesture-$id").fetchSemanticsNode().boundsInRoot
        val start = Offset(gesture.left + 8, gesture.center.y) - canvasBounds.topLeft
        val delta = target - modelBounds.topLeft
        canvas.performTouchInput {
            down(start)
            for (step in 1..12) moveTo(start + delta * (step / 12f), delayMillis = 16)
            up()
        }
        compose.waitForIdle()
    }

    private fun pinch(id: Long, scale: Float) {
        val canvas = compose.onNodeWithTag("model-canvas")
        val canvasBounds = canvas.fetchSemanticsNode().boundsInRoot
        val gesture = compose.onNodeWithTag("gesture-$id").fetchSemanticsNode().boundsInRoot
        val center = gesture.center - canvasBounds.topLeft
        val radius = minOf(gesture.width, gesture.height) * 0.15f
        canvas.performTouchInput {
            down(0, center - Offset(radius, 0f))
            down(1, center + Offset(radius, 0f))
            for (step in 1..12) {
                val distance = radius * (1 + (scale - 1) * step / 12f)
                updatePointerTo(0, center - Offset(distance, 0f))
                updatePointerTo(1, center + Offset(distance, 0f))
                move(delayMillis = 16)
            }
            up(0)
            up(1)
        }
        compose.waitForIdle()
    }

    private fun sceneViews(view: View): List<SceneView> = when (view) {
        is SceneView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { sceneViews(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun addModel(name: String) {
        compose.onNodeWithText("Add Model").performClick()
        compose.onNode(hasText(name) and hasClickAction()).performClick()
    }

    private fun waitForModel(name: String, count: Int = 1) {
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithContentDescription("$name 3D model")
                .fetchSemanticsNodes().size == count
        }
    }
}
