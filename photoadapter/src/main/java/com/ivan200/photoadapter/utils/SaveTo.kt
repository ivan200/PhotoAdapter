package com.ivan200.photoadapter.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * Where will the images be saved
 *
 * @author ivan200
 * @since 14.08.2022
 */
@Parcelize
sealed class SaveTo : Parcelable {
    /**
     * Only save into internal storage
     * context.filesDir/DCIM/
     */
    data object OnlyInternal : SaveTo()

    /**
     * Only save into custom directory with specified [path]
     */
    data class Custom(val path: File) : SaveTo() {
        init {
            require(path.exists() && path.canWrite())
        }
    }

    /**
     * Save to internal storage, then copy to gallery via MediaStore api
     * if success, then delete files from internal storage and return gallery uris
     * if not, internal files uri will be returned
     */
    data object ToGallery : SaveTo()

    /**
     * Save to internal storage, then copy to gallery with [album] name
     * Beginning android Q we save file to gallery via MediaStore api with RELATIVE_PATH = DCIM/AlbumName
     *
     * prior to android Q there was no RELATIVE_PATH column in MediaStore
     * so we need to manually copy files into `getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)/AlbumName/`
     * folder, and if success, request gallery to find this pictures
     * if this folder is not accessible, then we try to save file to gallery via MediaStore api without AlbumName
     *
     * and if mediastore save will fail, so internal files uri will be returned
     */
    data class ToGalleryWithAlbum(val album: String) : SaveTo() {
        init {
            require(album.isNotBlank())
        }
    }
}
