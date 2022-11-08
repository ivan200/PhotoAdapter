package com.ivan200.photoadapter.camerax.touch

import android.graphics.PointF
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.ivan200.photoadapter.camerax.FocusView
import java.lang.ref.WeakReference

/**
 * @author ivan200
 * @since 24.02.2022
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class TouchHandler(
    private val camera: Camera?,
    private val preview: PreviewView,
    private val focusView: FocusView
) : View.OnTouchListener, TapFinder.OnTapListener, ScrollFinder.OnScrollListener, ScaleFinder.OnScaleListener {

    val tapFinder = TapFinder(preview.context, this)
    val scaleFinder = ScaleFinder(preview.context, this)
    val scrollFinder = ScrollFinder(preview.context, this)

    var startScrollIndex = 0

    /**
     * Zoom camera on scale
     */
    override fun onScale(scaleFactor: Float) {
        camera?.apply {
            val zoom = cameraInfo.zoomState.value?.zoomRatio ?: 0f
            val scale = zoom * scaleFactor
            runCatching {
                cameraControl.setZoomRatio(scale)
            }
        }
    }

    /**
     * Focus and meter camera on single tap
     */
    override fun onTap(point: PointF) {
        camera?.apply {
            val focusPoint = preview.meteringPointFactory.createPoint(point.x, point.y)
            val action = FocusMeteringAction.Builder(focusPoint).build()
            if(!camera.cameraInfo.isFocusMeteringSupported(action)) return

            val future = cameraControl.startFocusAndMetering(action)
            focusView.anim(point.x, point.y)
            val weakRef = WeakReference(focusView)
            future.addListener(
                { weakRef.get()?.hide() },
                ContextCompat.getMainExecutor(focusView.context)
            )
        }
    }

    /**
     * Remember exposureCompensationIndex on start scroll vertically
     */
    override fun onStartScroll(horisontal: Boolean) {
        if (horisontal || camera?.cameraInfo?.exposureState?.isExposureCompensationSupported != true) return

        startScrollIndex = camera.cameraInfo.exposureState.exposureCompensationIndex
    }

    /**
     * Change exposureCompensationIndex on scroll vertically
     */
    override fun onScroll(horisontal: Boolean, distance: Float) {
        if (horisontal || camera?.cameraInfo?.exposureState?.isExposureCompensationSupported != true) return

        var factor = distance / preview.height
        val range = camera.cameraInfo.exposureState.exposureCompensationRange
        val minValue = range.lower.toFloat()
        val maxValue = range.upper.toFloat()
        factor *= maxValue - minValue
        factor *= 2f // Add some sensitivity.
        var value = startScrollIndex + factor
        if (value > maxValue) value = maxValue
        if (value < minValue) value = minValue
        runCatching {
            camera?.cameraControl?.setExposureCompensationIndex(value.toInt())
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        v.performClick()

        return when {
            tapFinder.handleTouchEvent(event) -> true
            scaleFinder.handleTouchEvent(event) -> true
            scrollFinder.handleTouchEvent(event) -> true
            else -> true
        }
    }
}
