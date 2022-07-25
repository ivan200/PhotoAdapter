package com.ivan200.photoadapter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

//
// Created by Ivan200 on 21.10.2019.
//
@Parcelize
data class PictureInfo(val file: File, val thumbFile: File?) : Parcelable

