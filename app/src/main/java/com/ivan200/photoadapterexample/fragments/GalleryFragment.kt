package com.ivan200.photoadapterexample.fragments

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_UNDEFINED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ivan200.photoadapterexample.Prefs
import com.ivan200.photoadapterexample.R
import com.ivan200.photoadapterexample.utils.SquareImageView
import com.ivan200.photoadapterexample.utils.ViewUtils.updateInsets

//
// Created by Ivan200 on 14.11.2019.
//
class GalleryFragment : Fragment(R.layout.fragment_gallery), MenuProvider {
    private val recyclerView get() = requireView().findViewById<RecyclerView>(R.id.gallery_recycle)
    private val mActivity get() = activity as AppCompatActivity

    var orientation = ORIENTATION_UNDEFINED

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.setSupportActionBar(requireView().findViewById(R.id.action_bar))
        mActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mActivity.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        mActivity.title = getString(R.string.gallery)

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = GalleryAdapter(Prefs(requireContext()).sortedImages)

        orientation = resources.configuration.orientation
        setRecycler()

        ViewCompat.setOnApplyWindowInsetsListener(requireView()) { _, insets ->
            updateInsets(insets)
            return@setOnApplyWindowInsetsListener insets
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (orientation != newConfig.orientation) {
            orientation = newConfig.orientation
            setRecycler()
        }
    }

    fun setRecycler() {
        val span = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3
        recyclerView.layoutManager = GridLayoutManager(mActivity, span)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            mActivity.onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    class GalleryAdapter(private val images: List<String>) :
        RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

        private val navigatePreview = Navigation.createNavigateOnClickListener(R.id.action_galleryFragment_to_previewFragment)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
            return GalleryViewHolder(
                SquareImageView(parent.context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    val padd = context.resources.getDimensionPixelOffset(R.dimen.padding_gallery)
                    setPadding(padd, padd, padd, padd)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        cropToPadding = true
                    }
                }
            )
        }

        override fun getItemCount(): Int {
            return images.size
        }

        override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
            Glide.with(holder.itemView.context)
                .load(Uri.parse(images[position]))
                .into((holder.itemView as ImageView))

            holder.itemView.setOnClickListener {
                Prefs(it.context).imagePreviewNumber = position
                navigatePreview.onClick(it)
            }
        }

        class GalleryViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
