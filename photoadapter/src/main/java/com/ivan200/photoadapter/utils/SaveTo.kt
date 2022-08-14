package com.ivan200.photoadapter.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * @author ivan200
 * @since 14.08.2022
 */
@Parcelize
sealed class SaveTo : Parcelable {
    object OnlyInternal : SaveTo()
    data class Custom(val path: File) : SaveTo() {
        init {
            require(path.exists() && path.canWrite())
        }
    }

    data class ToGalleryWithAlbum(val album: String) : SaveTo() {
        init {
            require(album.isNotBlank())
        }
    }

    object ToGallery : SaveTo()
}
