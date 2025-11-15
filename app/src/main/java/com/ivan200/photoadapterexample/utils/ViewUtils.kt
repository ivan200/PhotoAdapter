package com.ivan200.photoadapterexample.utils

import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.ivan200.photoadapter.R

/**
 * @author ivan200
 * @since 15.11.2025
 */
object ViewUtils {

    fun Fragment.updateInsets(insets: WindowInsetsCompat) {
        val insetsBars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
        val insetsCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val unionInsets = Insets.max(insetsBars, insetsCutout)
        upd(R.id.inset_left) { width = unionInsets.left }
        upd(R.id.inset_right) { width = unionInsets.right }
        upd(R.id.inset_top) { height = unionInsets.top }
        upd(R.id.inset_bottom) { height = unionInsets.bottom }
    }

    fun Fragment.upd(@IdRes viewId: Int, block: ViewGroup.LayoutParams.() -> Unit) {
        requireView().findViewById<FrameLayout>(viewId)?.updateLayoutParams<MarginLayoutParams>(block = block)
    }
}