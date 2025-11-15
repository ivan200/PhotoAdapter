package com.ivan200.photoadapter

import android.os.Parcelable
import com.ivan200.photoadapter.base.ScaleDelegate
import kotlinx.parcelize.Parcelize
import java.io.File

//
// Created by Ivan200 on 21.10.2019.
//
@Parcelize
data class PictureInfo(
    val file: File,
    val scale: ScaleDelegate
) : Parcelable

