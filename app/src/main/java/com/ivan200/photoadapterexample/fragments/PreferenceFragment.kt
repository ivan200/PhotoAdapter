package com.ivan200.photoadapterexample.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.ivan200.photoadapterexample.R

//
// Created by Ivan200 on 15.11.2019.
//
class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }
}