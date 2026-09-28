package com.modelviewer3d.threed

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.View
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import kotlin.math.abs
import kotlin.math.hypot

class ModelInteraction(
    context: Context,
    private val modelNode: ModelNode,
    private val cameraNode: CameraNode,
    private val density: Float
) : View.OnTouchListener {
    var enabled = false
        set(value) {
            if (field != value) gestureActive = false
            field = value
        }

    private var gestureActive = false
    private var transformed = false
    private var pitch = modelNode.rotation.x
    private var yaw = modelNode.rotation.y
    private var distance = cameraNode.position.z

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var initialSpan = 0.0f
    private var previousSpan = 0.0f
    private var scaling = false

    private fun zoom(event: MotionEvent) {
        if (event.pointerCount != 2 || event.actionMasked == MotionEvent.ACTION_POINTER_UP) {
            initialSpan = 0.0f
            previousSpan = 0.0f
            scaling = false
            return
        }
        val span = hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0))
        if (span <= 0.0f || !span.isFinite()) return
        if (previousSpan == 0.0f) {
            initialSpan = span
            previousSpan = span
            return
        }
        // The platform scale detector's minimum span is too large for small model viewports.
        if (!scaling && abs(span - initialSpan) > touchSlop) scaling = true
        if (scaling) {
            // Assets are normalized to one unit; bound camera distance to that scale.
            distance = (distance * previousSpan / span).coerceIn(1.5f, 6.0f)
            cameraNode.position = Position(0.0f, 0.0f, distance)
            transformed = true
            previousSpan = span
        }
    }

    private val dragDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent) = true

            override fun onScroll(
                first: MotionEvent?,
                current: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (current.pointerCount == 1) {
                    yaw = (yaw - distanceX / density * 0.5f) % 360.0f
                    pitch = (pitch - distanceY / density * 0.5f).coerceIn(-85.0f, 85.0f)
                    modelNode.rotation = Rotation(pitch, yaw, 0.0f)
                    transformed = true
                }
                return true
            }
        }
    ).apply {
        setIsLongpressEnabled(false)
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            gestureActive = enabled
            transformed = false
            initialSpan = 0.0f
            previousSpan = 0.0f
            scaling = false
        }
        // A mode switch cancels this gesture. Only a fresh down can start another one.
        if (!enabled || !gestureActive) return true

        zoom(event)
        dragDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                if (!transformed) view.performClick()
                gestureActive = false
            }
            MotionEvent.ACTION_CANCEL -> gestureActive = false
        }
        return true
    }
}
