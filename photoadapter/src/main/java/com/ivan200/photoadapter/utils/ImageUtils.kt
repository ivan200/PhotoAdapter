@file:Suppress("DEPRECATION")

package com.ivan200.photoadapter.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import java.io.*
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*

//
// Created by Ivan200 on 21.10.2019.
//

@Suppress("MemberVisibilityCanBePrivate")
object ImageUtils {
    const val PHOTOS = "photos"
    const val THUMBNAILS = "thumbnails"
    const val JPEG_FILE_PREFIX = "IMG_"
    const val JPEG_FILE_SUFFIX = ".jpg"


    fun getFileName(): String {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS", Locale.US).format(Date())
        return JPEG_FILE_PREFIX + timeStamp + JPEG_FILE_SUFFIX
    }

    @Throws(IOException::class)
    fun createImageFile(context: Context, photosDir: File?): File {
        val dirToMake = photosDir ?: getPhotosDir(context)
        if (!dirToMake.exists()) {
            dirToMake.mkdir()
        }
        val image = File(dirToMake, getFileName())
        image.createNewFile()
        return image
    }

    fun getPhotosDir(context: Context, photosDir: File? = null): File {
        val dirToMake = photosDir ?: File(context.filesDir, PHOTOS)
        if (!dirToMake.exists()) {
            dirToMake.mkdirs()
        }
        return dirToMake
    }

    fun getThumbsDir(context: Context, thumbsDir: File? = null): File {
        val dirToMake = thumbsDir ?: File(context.filesDir, THUMBNAILS)
        if (!dirToMake.exists()) {
            dirToMake.mkdirs()
        }
        return dirToMake
    }


    @Throws(IOException::class)
    fun copyImagesToGallery(context: Context, images: Array<File>, ALBUM: String?) {
        if (ALBUM == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            copyImagesToGalleryBelowQ(context, images, ALBUM)
        } else {
            images.forEach { copyImageToGallery(context, it, ALBUM) }
            return
        }
    }

    @Suppress("DEPRECATION")
    fun copyImagesToGalleryBelowQ(context: Context, images: Array<File>, ALBUM: String) {
        //Checks if external storage is available for read and write
        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) return

        val externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val albumStorageDir = if (ALBUM.isEmpty()) externalStoragePublicDirectory else File(externalStoragePublicDirectory, ALBUM)
        albumStorageDir.mkdirs()

        val newGalleryFiles = images.map { image ->
            File(albumStorageDir, image.name).also { newFile ->
                copyFile(image, newFile)
            }
        }.toTypedArray()
        updateGallery(context, ALBUM, newGalleryFiles)
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    fun copyImageToGallery(context: Context, image: File, ALBUM: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, image.name)
            put(MediaStore.MediaColumns.MIME_TYPE, MimeTypeMap.getSingleton().getMimeTypeFromExtension(image.extension))
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + ALBUM)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, contentValues)
            ?: throw IOException("Failed to create new MediaStore record.")

        var outputStream: OutputStream? = null
        try {
            outputStream = resolver.openOutputStream(uri) ?: throw IOException("Failed to get output stream.")
            Files.copy(image.toPath(), outputStream)

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        } finally {
            outputStream?.close()
        }
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

    //Add the image and image album into gallery
    @SuppressLint("InlinedApi")
    private fun updateGallery(context: Context, albumName: String, files: Array<File>) {

        for (file in files) {
            //metadata of new image
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.TITLE, file.name)
                put(MediaStore.Images.Media.DESCRIPTION, albumName)
                put(MediaStore.Images.Media.DATE_TAKEN, file.lastModified())
                put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().toLowerCase(Locale.US).hashCode())
                put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.name.toLowerCase(Locale.US))
                put("_data", file.absolutePath)
            }
            val cr = context.contentResolver
            cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }

        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(
            context, files.map { it.toString() }.toTypedArray(), null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
        }
    }

    @Suppress("unused")
    fun hasFrontCamera(): Boolean {
        val cameraInfo: Camera.CameraInfo = Camera.CameraInfo()
        val numberOfCameras: Int = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return true
            }
        }
        return false
    }

    fun allowCamera2Support(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false

        (activity.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)?.apply {
            try {
                cameraIdList.firstOrNull()?.let {
                    val characteristics = getCameraCharacteristics(it)
                    val support = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    if (support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                        support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3
                    ) {
                        return true
                    }
                }
            } catch (ex: Throwable) {
                return false
            }
        }
        return false
    }

    fun hasDifferentFacingsOldWay(): Boolean {
        var facing: Int? = null
        val cameraInfo: Camera.CameraInfo = Camera.CameraInfo()
        val numberOfCameras: Int = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            Camera.getCameraInfo(i, cameraInfo)
            if (facing == null) {
                facing = cameraInfo.facing
            } else if (facing != cameraInfo.facing) {
                return true
            }
        }
        return false
    }


    fun hasDifferentFacings(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return hasDifferentFacingsOldWay()
        }

        var facing: Int? = null
        (activity.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)
            ?.apply {
                try {
                    cameraIdList.forEach {
                        val characteristics = getCameraCharacteristics(it)
                        if (facing == null) {
                            facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                        } else if (facing != characteristics.get(CameraCharacteristics.LENS_FACING)) {
                            return true
                        }
                    }
                } catch (ex: Throwable) {
                    return hasDifferentFacingsOldWay()
                }
            }
        return false
    }
}