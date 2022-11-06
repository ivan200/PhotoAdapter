@file:Suppress("unused")

package com.ivan200.photoadapter.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.Surface
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import com.ivan200.photoadapter.R
import java.io.Serializable
import java.lang.ref.WeakReference
import kotlin.math.ceil

fun <T : View> T.onClick(function: () -> Unit): T {
    setOnClickListener { function() }
    return this
}

fun <T : Activity> T.lockOrientation() {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    if (windowManager != null) {
        val rotation = windowManager.defaultDisplay.rotation
        val orientation = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> when (rotation) {
                Surface.ROTATION_0,
                Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            Configuration.ORIENTATION_PORTRAIT -> when (rotation) {
                Surface.ROTATION_0,
                Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }
            else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        requestedOrientation = orientation
    }
}

@Suppress("DEPRECATION")
fun <T : Activity> T.hideSystemUI() {
    if (SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.apply {
            hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        window.decorView.postDelayed({
            var flag = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            if (SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                flag = flag or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
                flag = flag or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            window.decorView.systemUiVisibility = flag
        }, 500L)
    }

    //WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
}

@Suppress("DEPRECATION", "KotlinConstantConditions")
fun <T : Activity> T.showSystemUI() {
    if (SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(true)
        window.insetsController?.apply {
            show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
    } else {
        var flag = View.SYSTEM_UI_FLAG_VISIBLE
        if (SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            flag = flag or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        window.decorView.systemUiVisibility = flag
    }
}

fun <T : Activity> T.unlockOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}

internal fun View.padBottomViewWithInsets(insets: WindowInsetsCompat) {
    val land = this.resources.getBoolean(R.bool.is_land)
    this.setPadding(
        if (land) 0 else insets.systemWindowInsetLeft,
        if (land) insets.systemWindowInsetTop else 0,
        insets.systemWindowInsetRight,
        insets.systemWindowInsetBottom
    )
}

internal fun View.padTopViewWithInsets(insets: WindowInsetsCompat) {
    this.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, insets.systemWindowInsetTop)
}

internal fun rotateItems(angle: Int, vararg views: View) {
    val rotationDuration = 200L
    views.forEach {
        it.animate().rotation(angle.toFloat()).setDuration(rotationDuration).start()
    }
}

/** Milliseconds used for UI animations */
internal const val ANIMATION_FAST_MILLIS = 50L
internal const val ANIMATION_SLOW_MILLIS = 100L

/**
 * Simulate a button click, including a small delay while it is being pressed to trigger the
 * appropriate animations.
 */
internal fun ImageButton.simulateClick(delay: Long = ANIMATION_SLOW_MILLIS) {
    performClick()
    isPressed = true
    invalidate()
    val ref = WeakReference(this)
    postDelayed({
        ref.get()?.apply {
            isPressed = false
            invalidate()
        }
    }, delay)
}

/**
 * Получение ресурса цвета, привязаного к теме через аттрибуты, например `android.R.attr.textColorPrimary`
 */
@SuppressLint("ResourceAsColor")
@ColorInt
fun Context.getColorResCompat(@AttrRes id: Int) = TypedValue()
    .also { theme.resolveAttribute(id, it, true) }
    .run { if (resourceId != 0) resourceId else data }
    .let { ContextCompat.getColor(this, it) }


@ColorInt
fun Context.getColorCompat(@ColorRes id: Int) = ContextCompat.getColor(this, id)

@JvmOverloads
fun Number.dpToPxInt(context: Context? = null): Int {
    val res = context?.resources ?: android.content.res.Resources.getSystem()
    return ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), res.displayMetrics)).toInt()
}

fun Number.dpToPx(context: Context? = null): Float {
    val res = context?.resources ?: android.content.res.Resources.getSystem()
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), res.displayMetrics)
}


inline fun <reified T : Serializable> Bundle.serializableCompat(key: String): T? = when {
    SDK_INT >= TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

inline fun <reified T : Parcelable> Intent.parcelableCompat(key: String): T? = when {
    SDK_INT >= TIRAMISU -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelableArrayCompat(key: String): Array<out T>? = when {
    SDK_INT >= TIRAMISU -> getParcelableArray(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArray(key)?.filterIsInstance<T>()?.toTypedArray()
}