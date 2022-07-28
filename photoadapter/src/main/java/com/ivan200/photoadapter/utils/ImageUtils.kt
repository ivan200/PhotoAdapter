@file:Suppress("DEPRECATION")

package com.ivan200.photoadapter.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
import android.hardware.Camera
import android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
import android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.Display
import android.view.WindowManager
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.core.content.ContextCompat
import com.ivan200.photoadapter.base.FacingDelegate
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

    fun allowCamera2Support(activity: Context): Boolean {
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

    fun getFacingsOldWay(): Set<FacingDelegate> {
        val facings = mutableSetOf<FacingDelegate>()
        val cameraInfo = Camera.CameraInfo()
        val numberOfCameras: Int = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            Camera.getCameraInfo(i, cameraInfo)
            if(cameraInfo.facing == CAMERA_FACING_BACK) facings.add(FacingDelegate.BACK)
            if(cameraInfo.facing == CAMERA_FACING_FRONT) facings.add(FacingDelegate.FRONT)
        }
        return facings
    }


    fun getFacings(context: Context): Set<FacingDelegate> {
        if (!isCameraAvailable(context)) {
            return emptySet()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return getFacingsOldWay()
        }
        val facings = mutableSetOf<FacingDelegate>()
        (context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)?.apply {
            try {
                cameraIdList.forEach {
                    val characteristics = getCameraCharacteristics(it)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    when (facing) {
                        CameraCharacteristics.LENS_FACING_BACK -> facings.add(FacingDelegate.BACK)
                        CameraCharacteristics.LENS_FACING_FRONT -> facings.add(FacingDelegate.FRONT)
                        else -> Unit //Ontario is not support external cameras
                    }
                }
            } catch (ex: Throwable) {
                return getFacingsOldWay()
            }
        }
        return facings
    }

    fun isCameraAvailable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                || context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
                || context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL)
    }

    fun Number.dpToPx(context: Context? = null): Float {
        val res = context?.resources ?: android.content.res.Resources.getSystem()
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), res.displayMetrics)
    }

    fun aspectRatio(size: PointF?): Int {
        if (size == null || size.x <= 0f || size.y <= 0f) {
            return DEFAULT_ASPECT_RATIO
        }
        val previewRatio = max(size.x, size.y).toDouble() / min(size.x, size.y)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private const val RATIO_4_3_VALUE: Double = 4.0 / 3.0
    private const val RATIO_16_9_VALUE: Double = 16.0 / 9.0
    private const val DEFAULT_ASPECT_RATIO = AspectRatio.RATIO_4_3

    fun targetSize(cameraRatio: Int, targetSideSize: Int): Size = when (cameraRatio) {
        AspectRatio.RATIO_16_9 -> Size((targetSideSize / RATIO_16_9_VALUE).toInt(), targetSideSize)
        else -> Size((targetSideSize / RATIO_4_3_VALUE).toInt(), targetSideSize)
    }

    fun Size.scaleDown(maxSide: Int): Size {
        if(width == 0 || height == 0) {
            return Size(0,0)
        }
        if (width <= maxSide && height <= maxSide || maxSide < 0) {
            return this
        }
        val ratio = width.toFloat() / height.toFloat()
        return when {
            ratio >= 1 -> Size(maxSide, (maxSide / ratio).toInt())
            else -> Size((maxSide * ratio).toInt(), maxSide)
        }
    }

    @Suppress("DEPRECATION")
    val Context.displayCompat: Display?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else null
            ?: ContextCompat.getSystemService(this, WindowManager::class.java)?.defaultDisplay
}