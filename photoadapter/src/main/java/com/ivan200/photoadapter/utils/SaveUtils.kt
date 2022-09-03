package com.ivan200.photoadapter.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author ivan200
 * @since 27.07.2022
 */
object SaveUtils {
    private const val JPEG_FILE_PREFIX = "IMG_"
    private const val JPEG_FILE_SUFFIX = ".jpg"
    private const val PENDING = 1
    private const val NOT_PENDING = 0
    private const val COPY_BUFFER_SIZE = 1024

    fun getFileName(): String {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS", Locale.US).format(Date())
        return JPEG_FILE_PREFIX + timeStamp + JPEG_FILE_SUFFIX
    }

    @Throws(IOException::class)
    fun createImageFile(context: Context, saveTo: SaveTo): File {
        val savePhotosDir = getSavePhotosDir(context, saveTo)
        val image = File(savePhotosDir, getFileName())
        image.createNewFile()
        return image
    }

    fun getSavePhotosDir(context: Context, saveTo: SaveTo): File {
        return if (saveTo is SaveTo.Custom) {
            saveTo.path
        } else {
            File(context.filesDir, Environment.DIRECTORY_DCIM).also {
                if (!it.exists()) {
                    it.mkdirs()
                }
            }
        }
    }

    fun moveImagesToGallery(
        context: Context,
        images: List<File>,
        saveTo: SaveTo
    ): List<Uri> = when (saveTo) {
        SaveTo.OnlyInternal -> images.map { Uri.fromFile(it) }
        is SaveTo.Custom -> images.map { Uri.fromFile(it) }
        is SaveTo.ToGalleryWithAlbum -> {
            copyImagesToGalleryWithAlbum(context, images, saveTo)
                ?.also { images.forEach { runCatching { it.delete() } } }
                ?: images.map { Uri.fromFile(it) }
        }
        SaveTo.ToGallery -> {
            copyFilesToMediastoreByUri(context, images, "")
                ?.also { images.forEach { runCatching { it.delete() } } }
                ?: images.map { Uri.fromFile(it) }
        }
    }

    fun copyImagesToGalleryWithAlbum(
        context: Context,
        images: List<File>,
        saveTo: SaveTo.ToGalleryWithAlbum
    ): List<Uri>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val saveDir = getSaveDir(saveTo.album)
            if (saveDir != null) {
                val galleryFiles = runCatching { copyFilesToOtherDir(images, saveDir) }.getOrNull()
                if (galleryFiles != null) {
                    val newUris = updateGallery(context, saveTo.album, galleryFiles)
                    return if (newUris.any { it == null }) {
                        galleryFiles.map { Uri.fromFile(it) }
                    } else {
                        newUris.filterNotNull()
                    }
                }
            }
        }
        return copyFilesToMediastoreByUri(context, images, saveTo.album)
    }

    fun getContentValues(file: File, album: String): ContentValues = ContentValues().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.TITLE, file.name)
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.DATE_TAKEN, file.lastModified())
            put(MediaStore.MediaColumns.DATE_ADDED, file.lastModified())
            put(MediaStore.MediaColumns.DATE_MODIFIED, file.lastModified())
            put(MediaStore.MediaColumns.SIZE, file.length())
            put(MediaStore.MediaColumns.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension))
            if (album.isNotBlank()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    put(MediaStore.MediaColumns.ALBUM, album)
                }
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + album)
            }
        } else {
            put(MediaStore.Images.Media.TITLE, file.name)
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.DESCRIPTION, album)
            put(MediaStore.Images.Media.SIZE, file.length())
            put(MediaStore.Images.Media.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension))
            put(MediaStore.Images.Media.DATE_TAKEN, file.lastModified())
            put(MediaStore.Images.Media.DATE_ADDED, file.lastModified())
            put(MediaStore.Images.Media.DATE_MODIFIED, file.lastModified())
            put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().lowercase().hashCode())
            put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.name.lowercase())
        }
    }

    fun File.orNullIfNotWritable(): File? = try {
        this.mkdirs()
        if (exists() && canWrite()) this else null
    } catch (ex: Exception) {
        null
    }

    fun getSaveDir(album: String): File? {
        var dir: File? = runCatching {
            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            File(dcimDir, album)
        }.getOrNull()?.orNullIfNotWritable()

        if (dir == null) {
            dir = runCatching {
                val storageDir = Environment.getExternalStorageDirectory()
                val dcimDir = File(storageDir, Environment.DIRECTORY_DCIM)
                File(dcimDir, album)
            }.getOrNull()?.orNullIfNotWritable()
        }
        return dir
    }

    fun copyFilesToOtherDir(images: List<File>, albumStorageDir: File): List<File> {
        val newImageFiles = mutableListOf<File>()
        for (image in images) {
            try {
                val newFile = File(albumStorageDir, image.name)
                copyFile(image, newFile)
                newImageFiles.add(newFile)
            } catch (e: Exception) {
                newImageFiles.forEach { it.delete() }
                throw e
            }
        }
        return newImageFiles
    }

    @Throws(IOException::class)
    fun copyFile(sourceFile: File, destFile: File) {
        val source = FileInputStream(sourceFile).channel
        if (!destFile.exists()) {
            destFile.createNewFile()
        }
        val destination = FileOutputStream(destFile).channel
        destination.transferFrom(source, 0, source.size())
        destination.close()
        source.close()
    }

    // Add the image and image album into gallery
    @SuppressLint("InlinedApi")
    private fun updateGallery(context: Context, albumName: String, files: List<File>): List<Uri?> {
        val contentValues = files.map {
            getContentValues(it, albumName).apply {
                put("_data", it.absolutePath)
            }
        }
        val uris = contentValues.map {
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it)
        }

        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(
            context,
            files.map { it.toString() }.toTypedArray(),
            null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
        }
        return uris
    }

    fun copyFilesToMediastoreByUri(context: Context, files: List<File>, album: String): List<Uri>? {
        val newUris = mutableListOf<Uri>()
        var successAll = true
        for (file in files) {
            val contentValues = getContentValues(file, album)
            val uriResult = copyFileToMediastoreByUri(context, contentValues, file)
            if (uriResult.isSuccess) {
                newUris.add(uriResult.getOrThrow())
            } else {
                successAll = false
                break
            }
        }
        if (successAll) {
            return newUris
        } else {
            newUris.filterNotNull().forEach {
                runCatching {
                    context.contentResolver.delete(it, null, null)
                }
            }
        }
        return null
    }

    fun copyFileToMediastoreByUri(context: Context, contentValues: ContentValues, file: File): Result<Uri> = runCatching {
        val volumeUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        if (volumeUri == null) throw IOException("Failed to get external volume URI.")

        val resolver = context.contentResolver
        val values = ContentValues(contentValues)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, PENDING)
        }

        val outputUri = resolver.insert(volumeUri, values)
            ?: throw IOException("Failed to create new MediaStore record.")

        var outputStream: OutputStream? = null
        try {
            outputStream = resolver.openOutputStream(outputUri) ?: throw IOException("Failed to get output stream.")
            Files.copy(file.toPath(), outputStream)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, NOT_PENDING)
                resolver.update(outputUri, contentValues, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(outputUri, null, null)
            throw e
        } finally {
            outputStream?.close()
        }
        return@runCatching outputUri
    }
}
