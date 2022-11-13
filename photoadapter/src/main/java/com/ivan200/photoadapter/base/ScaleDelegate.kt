package com.ivan200.photoadapter.base

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ivan200.photoadapter.R

/**
 * @author ivan200
 * @since 22.02.2022
 */
enum class ScaleDelegate(
    @DrawableRes
    val iconRes: Int,
    @StringRes
    val descriptionRes: Int
) {
    FIT(
        R.drawable.ic_photo_fit,
        R.string.photo_scale_type_fill
    ),
    FILL(
        R.drawable.ic_photo_fill,
        R.string.photo_scale_type_fit
    );
}