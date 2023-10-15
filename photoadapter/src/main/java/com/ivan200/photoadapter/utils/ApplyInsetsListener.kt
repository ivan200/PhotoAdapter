package com.ivan200.photoadapter.utils

import androidx.core.view.WindowInsetsCompat

//
// Created by Ivan200 on 16.10.2019.
//

fun interface ApplyInsetsListener {
    fun onApplyInsets(insets: WindowInsetsCompat)
}