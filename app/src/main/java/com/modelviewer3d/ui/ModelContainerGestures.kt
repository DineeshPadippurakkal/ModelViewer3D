package com.modelviewer3d.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

@Composable
fun ModelContainerGestures(
    enabled: Boolean,
    onDrag: (Offset) -> Unit,
    onResize: (Float) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drag by rememberUpdatedState(onDrag)
    val resize by rememberUpdatedState(onResize)
    val finish by rememberUpdatedState(onFinished)

    // This overlay owns model-area touches, keeping SurfaceView out of container gestures.
    Box(modifier.pointerInput(enabled) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var previousCount = 1
            var accumulatedPan = Offset.Zero
            var accumulatedZoom = 1.0f
            var initialRadius = 0.0f
            var gestureStarted = false
            try {
                do {
                    val event = awaitPointerEvent()
                    val count = event.changes.count { it.pressed }
                    if (enabled) {
                        if (count >= 2) {
                            if (previousCount < 2) {
                                accumulatedPan = Offset.Zero
                                accumulatedZoom = 1.0f
                                initialRadius = 0.0f
                            }
                            val zoom = event.calculateZoom()
                            if (gestureStarted) {
                                resize(zoom)
                            } else {
                                if (initialRadius == 0.0f) {
                                    initialRadius = event.calculateCentroidSize(useCurrent = false)
                                }
                                accumulatedZoom *= zoom
                                val distance = abs(1 - accumulatedZoom) * initialRadius
                                if (distance > viewConfiguration.touchSlop) {
                                    gestureStarted = true
                                    resize(accumulatedZoom)
                                }
                            }
                        } else if (count == 1 && previousCount == 1) {
                            val pan = event.calculatePan()
                            if (gestureStarted) {
                                drag(pan)
                            } else {
                                accumulatedPan += pan
                                if (accumulatedPan.getDistance() > viewConfiguration.touchSlop) {
                                    gestureStarted = true
                                    drag(accumulatedPan)
                                }
                            }
                        }
                    }
                    event.changes.forEach { it.consume() }
                    previousCount = count
                } while (count > 0)
            } finally {
                if (gestureStarted) finish()
            }
        }
    })
}
