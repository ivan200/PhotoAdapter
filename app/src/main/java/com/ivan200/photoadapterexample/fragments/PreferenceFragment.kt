package com.ivan200.photoadapterexample.fragments

import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.ivan200.photoadapterexample.R


//
// Created by Ivan200 on 15.11.2019.
//
class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val space = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80f, resources.displayMetrics).toInt()
        listView.addItemDecoration(SpacesItemDecoration(space))
    }

    private class SpacesItemDecoration(private val space: Int) : ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.bottom = if (parent.getChildAdapterPosition(view) == state.itemCount - 1) space else 0
        }
    }
}