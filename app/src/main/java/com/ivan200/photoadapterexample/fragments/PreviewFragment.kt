package com.ivan200.photoadapterexample.fragments

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.ivan200.photoadapterexample.Prefs
import com.ivan200.photoadapterexample.R
import java.io.File

class PreviewFragment : Fragment(R.layout.fragment_preview) {
    private val pagerImages get() = requireView().findViewById<ViewPager2>(R.id.pager_preview_images)
    private lateinit var pagerAdapter: PreviewAdapter
    private val mActivity get() = activity as AppCompatActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        mActivity.title = getString(R.string.preview)

        pagerAdapter = PreviewAdapter(Prefs(requireContext()).sortedImages)
        pagerImages.adapter = pagerAdapter
        pagerImages.setCurrentItem(Prefs(requireContext()).imagePreviewNumber, false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            mActivity.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class PreviewAdapter(private val images: List<String>) :
        RecyclerView.Adapter<PreviewAdapter.PreviewViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
            return PreviewViewHolder(
                PhotoView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    minimumScale = 1F
                    mediumScale = 2F
                    maximumScale = 4F
                })
        }

        override fun getItemCount(): Int = images.size

        override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
            Glide.with(holder.itemView.context)
                .load(Uri.parse(images[position]))
                .into((holder.itemView as ImageView))
        }

        class PreviewViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}