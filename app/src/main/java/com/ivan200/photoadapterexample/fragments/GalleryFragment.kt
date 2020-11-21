package com.ivan200.photoadapterexample.fragments

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivan200.photoadapter.utils.ImageUtils
import com.ivan200.photoadapterexample.Prefs
import com.ivan200.photoadapterexample.R
import com.ivan200.photoadapterexample.utils.SquareImageView
import java.io.File


//
// Created by Ivan200 on 14.11.2019.
//
class GalleryFragment : Fragment(R.layout.fragment_gallery) {
    private val recyclerView get() = requireView().findViewById<RecyclerView>(R.id.gallery_recycle)
    private val mActivity get() = activity as AppCompatActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        mActivity.title = "Gallery"

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = GalleryAdapter(Prefs(requireContext()).sortedImages)

        val span = if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3
        recyclerView.layoutManager = GridLayoutManager(mActivity, span)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            mActivity.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
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
                })
        }

        override fun getItemCount(): Int {
            return images.size
        }

        override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
            val imageFile = File(images[position])
            val thumbFile = File(ImageUtils.getThumbsDir(holder.itemView.context), imageFile.name)
            val loadFile = if(thumbFile.exists())  thumbFile else imageFile
            (holder.itemView as ImageView).setImageURI(Uri.fromFile(loadFile))
            holder.itemView.setOnClickListener {
                Prefs(it.context).imagePreviewNumber = position
                navigatePreview.onClick(it)
            }
        }

        class GalleryViewHolder (view: View) : RecyclerView.ViewHolder(view)
    }
}