package com.ivan200.photoadapter.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

//
// Created by Ivan200 on 23.10.2019.
//
class SaveThumbnailToFile(
        private val thumbnailFile: File?,
        private val maxThumbSize: Int
) : (File?) -> File? {

    override fun invoke(origFile: File?): File? {
        if(origFile == null || thumbnailFile == null) return null

        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inJustDecodeBounds = true // obtain the size of the image, without loading it in memory
        BitmapFactory.decodeFile(origFile.absolutePath, bitmapOptions)

        // find the best scaling factor for the desired dimensions
        val widthScale = bitmapOptions.outWidth.toFloat() / maxThumbSize
        val heightScale = bitmapOptions.outHeight.toFloat() / maxThumbSize
        val scale = min(widthScale, heightScale)

        var sampleSize = 1
        while (sampleSize < scale) {
            sampleSize *= 2
        }
        bitmapOptions.inSampleSize = sampleSize // this value must be a power of 2,
        // this is why you can not have an image scaled as you would like
        bitmapOptions.inJustDecodeBounds = false // now we want to load the image

        // Let's load just the part of the image necessary for creating the thumbnail, not the whole image
        val thumbnail = BitmapFactory.decodeFile(origFile.absolutePath, bitmapOptions)

        // Save the thumbnail
        val fos = FileOutputStream(thumbnailFile)
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        fos.flush()
        fos.close()

        // Use the thumbail on an ImageView or recycle it!
        thumbnail.recycle()

        return null
    }
}




