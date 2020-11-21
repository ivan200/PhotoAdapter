package com.ivan200.photoadapter.utils

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.fragment.app.FragmentActivity

//
// Created by Ivan200 on 08.11.2019.
//
interface PhotoChecker : Parcelable {
    fun checkPhoto(activity: FragmentActivity, photo: Bitmap): Boolean
}