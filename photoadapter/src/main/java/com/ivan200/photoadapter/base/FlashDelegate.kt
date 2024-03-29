package com.ivan200.photoadapter.base

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ivan200.photoadapter.R

/**
 * @author ivan200
 * @since 22.02.2022
 */
sealed class FlashDelegate {
    object NoFlash : FlashDelegate()

    sealed class HasFlash : FlashDelegate() {
        abstract val iconRes: Int
            @DrawableRes get

        abstract val descriptionRes: Int
            @StringRes get

        abstract val orderValue: Int

        data object Off : HasFlash() {
            override val iconRes: Int = R.drawable.ic_photo_flash_off
            override val descriptionRes: Int = R.string.photo_torch_off_description
            override val orderValue: Int = 0
        }

        data object On : HasFlash() {
            override val iconRes: Int = R.drawable.ic_photo_flash_on
            override val descriptionRes: Int = R.string.photo_torch_on_description
            override val orderValue: Int = 1
        }

        data object Auto : HasFlash() {
            override val iconRes: Int = R.drawable.ic_photo_flash_auto
            override val descriptionRes: Int = R.string.photo_torch_auto_description
            override val orderValue: Int = 2
        }

        data object Torch : HasFlash() {
            override val iconRes: Int = R.drawable.ic_photo_flash_torch
            override val descriptionRes: Int = R.string.photo_torch_torch_description
            override val orderValue: Int = 3
        }

        data class OffOnMainCamera(val cameraId: String) : HasFlash() {
            override val iconRes: Int = R.drawable.ic_photo_flash_off
            override val descriptionRes: Int = R.string.photo_torch_off_description
            override val orderValue: Int = 4
        }

        data class TorchOnMainCamera(val cameraId: String) : HasFlash() {
            override val iconRes: Int = R.drawable.ic_photo_flash_torch
            override val descriptionRes: Int = R.string.photo_torch_torch_description
            override val orderValue: Int = 4
        }
    }
}
