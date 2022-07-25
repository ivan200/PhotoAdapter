package com.ivan200.photoadapter.utils

import com.otaliastudios.cameraview.PictureResult
import java.io.File
import java.io.FileOutputStream

//
// Created by Ivan200 on 24.10.2019.
//
class SavePictureResultToFile(
    val file: File
) : (PictureResult) -> File {
    override fun invoke(p1: PictureResult): File {
        saveBytes(p1.data, file)
        return file
    }

    private fun saveBytes(bytes: ByteArray, saveFile: File) {
        FileOutputStream(saveFile).buffered().use {
            it.write(bytes)
        }
    }
}