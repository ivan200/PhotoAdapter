package com.ivan200.photoadapter.utils

import com.otaliastudios.cameraview.PictureResult
import java.io.File
import java.io.FileOutputStream

//
// Created by Ivan200 on 24.10.2019.
//
class SavePictureResultToFile(
    val file: File,
    val fixJpegBytes: Boolean = false
) : (PictureResult) -> File {
    override fun invoke(p1: PictureResult): File {
        if(fixJpegBytes) {
            saveBytesFixJpeg(p1.data, file)
        } else{
            saveBytes(p1.data, file)
        }
        return file
    }

    private fun saveBytes(bytes: ByteArray, saveFile: File){
        FileOutputStream(saveFile).buffered().use {
            it.write(bytes)
        }
    }

    // Some servers can't handle jpeg if it has the wrong first or last 2 bytes. (ffd8 and ffd9)
    // So we have to check and edit it manually
    private fun saveBytesFixJpeg(bytes: ByteArray, saveFile: File){
        FileOutputStream(saveFile).buffered().use {
            val properStart = bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte()
            val properEnd = bytes[bytes.size - 2] == 0xff.toByte() && bytes[bytes.size - 1] == 0xd9.toByte()

            if (!properStart) {
                it.write(0xff.toByte().toInt())
                it.write(0xd8.toByte().toInt())
            }
            it.write(bytes)
            if (!properEnd) {
                it.write(0xff.toByte().toInt())
                it.write(0xd9.toByte().toInt())
            }
        }
    }
}