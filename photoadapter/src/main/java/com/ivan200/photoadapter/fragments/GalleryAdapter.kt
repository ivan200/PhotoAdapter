package com.ivan200.photoadapter.fragments

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView
import com.ivan200.photoadapter.PictureInfo

//
// Created by Ivan200 on 16.10.2019.
//
class GalleryAdapter(var onCurrentPageLoaded: ((PictureInfo)->Unit)? = null) : RecyclerView.Adapter<GalleryAdapter.PagerVH>() {

    inner class PagerVH(itemView: View) : RecyclerView.ViewHolder(itemView)

    var images: List<PictureInfo> = arrayListOf()
    var curPos = 0

    //remove images - with animation, add images - without, to smooth change fragments
    fun update(newListImages: List<PictureInfo>, currentPosition: Int) {
        curPos = currentPosition

        val imageAdded = newListImages.size > images.size
        if (imageAdded) {
            images = ArrayList(newListImages)
            notifyDataSetChanged()
        } else {
            val productDiffResult = DiffUtil.calculateDiff(PictureDiffUtilCallback(images, newListImages))
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

    override fun onBindViewHolder(holder: PagerVH, position: Int){
        Glide.with(holder.itemView)
                .load(images[position].file)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        if(position == curPos) onCurrentPageLoaded?.invoke(images[position])
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        if(position == curPos) onCurrentPageLoaded?.invoke(images[position])
                        return false
                    }
                })
                .into(holder.itemView as PhotoView)
    }

    @Suppress("RedundantOverride")
    override fun onBindViewHolder(holder: PagerVH, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
    }

    inner class PictureDiffUtilCallback(private val oldList: List<PictureInfo>, private val newList: List<PictureInfo>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition].file.absolutePath == newList[newItemPosition].file.absolutePath
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition].file.absolutePath == newList[newItemPosition].file.absolutePath
    }
}

