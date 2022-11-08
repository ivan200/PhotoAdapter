package com.ivan200.photoadapter.fragments

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.ivan200.photoadapter.PictureInfo
import com.ivan200.photoadapter.fragments.GalleryAdapter.PagerVH
import com.ivan200.photoadapter.utils.SimpleDiffUtilCallback
import com.ivan200.photoadapter.utils.SimpleRequestListener

//
// Created by Ivan200 on 16.10.2019.
//
class GalleryAdapter(var onCurrentPageLoaded: ((PictureInfo) -> Unit)? = null) : RecyclerView.Adapter<PagerVH>() {

    inner class PagerVH(itemView: View) : RecyclerView.ViewHolder(itemView)

    var images: List<PictureInfo> = arrayListOf()
    var curPos = 0

    //remove images - with animation, add images - without, to smooth change fragments
    @SuppressLint("NotifyDataSetChanged")
    fun update(newListImages: List<PictureInfo>, currentPosition: Int) {
        curPos = currentPosition

        val imageAdded = newListImages.size > images.size
        if (imageAdded) {
            images = ArrayList(newListImages)
            notifyDataSetChanged()
        } else {
            val diffCalback = SimpleDiffUtilCallback(images, newListImages) { it.file.absolutePath }
            val productDiffResult = DiffUtil.calculateDiff(diffCalback)
            images = ArrayList(newListImages)
            productDiffResult.dispatchUpdatesTo(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerVH =
        PagerVH(PhotoView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            minimumScale = 1F
            mediumScale = 2F
            maximumScale = 4F
        })

    override fun getItemCount(): Int = images.count()

    override fun onBindViewHolder(holder: PagerVH, position: Int) {
        Glide.with(holder.itemView)
            .load(images[position].file)
            .listener(SimpleRequestListener {
                if (position == curPos) onCurrentPageLoaded?.invoke(images[position])
            })
            .into(holder.itemView as PhotoView)
    }

    @Suppress("RedundantOverride")
    override fun onBindViewHolder(holder: PagerVH, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
    }
}
