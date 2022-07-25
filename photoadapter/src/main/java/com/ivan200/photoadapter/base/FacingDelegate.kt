package com.ivan200.photoadapter.base

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ivan200.photoadapter.R

/**
 * @author ivan200
 * @since 22.02.2022
 */
enum class FacingDelegate(
    @DrawableRes
    val iconRes: Int,
    @StringRes
    val descriptionRes: Int
) {
    BACK(
        R.drawable.ic_photo_camera_rear,
        R.string.photo_camera_rear_description
    ),
    FRONT(
        R.drawable.ic_photo_camera_front,
        R.string.photo_camera_front_description
    ),
    EXTERNAL(
        R.drawable.ic_photo_camera_external,
        R.string.photo_camera_rear_description
    );
}