package com.ivan200.photoadapter.base

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ivan200.photoadapter.R

/**
 * @author ivan200
 * @since 22.02.2022
 */
sealed class TorchDelegate() {
    interface TorchRes {
        val iconRes: Int
            @DrawableRes get

        val descriptionRes: Int
            @StringRes get
    }

    object NoTorch : TorchDelegate()

    object On : TorchDelegate(), TorchRes {
        override val iconRes: Int get() = R.drawable.ic_photo_flash_on
        override val descriptionRes: Int get() = R.string.photo_torch_on_description
    }

    object Off: TorchDelegate(), TorchRes {
        override val iconRes: Int get() = R.drawable.ic_photo_flash_off
        override val descriptionRes: Int get() = R.string.photo_torch_off_description
    }
    object Auto: TorchDelegate(), TorchRes {
        override val iconRes: Int get() = R.drawable.ic_photo_flash_auto
        override val descriptionRes: Int get() = R.string.photo_torch_auto_description
    }
    object Torch: TorchDelegate(), TorchRes {
        override val iconRes: Int get() = R.drawable.ic_photo_flash_torch
        override val descriptionRes: Int get() = R.string.photo_torch_torch_description
    }
}