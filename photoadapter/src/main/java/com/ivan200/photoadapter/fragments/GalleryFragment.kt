package com.ivan200.photoadapter.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.ivan200.photoadapter.CameraActivity
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.CameraViewModel
import com.ivan200.photoadapter.R
import com.ivan200.photoadapter.utils.*
import me.relex.circleindicator.CircleIndicator3

//
// Created by Ivan200 on 16.10.2019.
//
@Suppress("unused")
class GalleryFragment : Fragment(R.layout.photo_fragment_gallery), ApplyInsetsListener {

    private val cameraViewModel: CameraViewModel by lazy {
        ViewModelProvider(activity as CameraActivity).get(CameraViewModel::class.java)
    }
    private lateinit var cameraBuilder: CameraBuilder

    private val pagerImages get() = requireView().findViewById<ViewPager2>(R.id.pager_images)
    private val statusView get() = requireView().findViewById<View>(R.id.statusView)
    private val btnAccept get() = requireView().findViewById<ImageButton>(R.id.btn_accept)
    private val btnDelete get() = requireView().findViewById<ImageButton>(R.id.btn_deletePicture)
    private val btnMore get() = requireView().findViewById<ImageButton>(R.id.btn_more)
    private val indicator get() = requireView().findViewById<CircleIndicator3>(R.id.indicator)
    private val actionLayout get() = requireView().findViewById<RelativeLayout>(R.id.action_layout_gallery)

    private lateinit var pagerAdapter: GalleryAdapter

    private var insets: WindowInsetsCompat? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraBuilder = (activity as? CameraActivity)?.cameraBuilder ?: CameraBuilder()
        pagerAdapter = GalleryAdapter(cameraViewModel::updateOnCurrentPageLoaded)
        pagerImages.adapter = pagerAdapter
        pagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                cameraViewModel.imageNumber = position
            }
        })

        if (cameraBuilder.fullScreenMode) {
            //Очищаем значение обозначающее что вью галереи должно быть над панелью кнопок
            (pagerImages.layoutParams as? RelativeLayout.LayoutParams)?.apply {
                arrayOf(RelativeLayout.BELOW, RelativeLayout.ABOVE, RelativeLayout.LEFT_OF, RelativeLayout.RIGHT_OF).forEach {
                    addRule(it, 0)
                }
            }
        }
        indicator.setViewPager(pagerImages)
        pagerAdapter.registerAdapterDataObserver(indicator.adapterDataObserver)

        cameraViewModel.pictures.observe(requireActivity()) {
            if (it.size == 0) {
                cameraViewModel.changeFragment(true)
            }
            indicator.showIf { it.size > 1 }
            pagerAdapter.update(it, cameraViewModel.imageNumber)
        }

        btnDelete.onClick {
            showDialogDeletePage(cameraViewModel::deleteCurrentPage)
        }

        cameraViewModel.scrollToPage.observe(requireActivity()) {
            pagerImages.setCurrentItem(cameraViewModel.imageNumber, false)
        }

        btnMore.showIf(cameraBuilder::allowMultipleImages)
            .onClick(this::onMorePhotosPressed)

        btnAccept.onClick(cameraViewModel::success)

        cameraViewModel.backPressed.observe(requireActivity()) {
            onMorePhotosPressed()
        }

        cameraViewModel.rotate.observe(requireActivity()) {
            rotateItems(it, btnAccept, btnDelete, btnMore)
        }

        insets?.let {
            setWindowInsets(it)
        }
    }

    fun onMorePhotosPressed() {
        if (cameraBuilder.allowMultipleImages) {
            cameraViewModel.needScrollToPage(0)
            cameraViewModel.changeFragment(true)
        } else {
            showDialogDeletePage {
                cameraViewModel.deleteCurrentPage()
            }
        }
    }

    override fun onApplyInsets(insets: WindowInsetsCompat) {
        insets.let {
            this.insets = it
            if (view != null) {
                setWindowInsets(it)
            }
        }
    }

    fun setWindowInsets(insets: WindowInsetsCompat) {
        actionLayout.padBottomViewWithInsets(insets)
        if (!cameraBuilder.fullScreenMode) {
            statusView.padTopViewWithInsets(insets)
        }
    }

    fun showDialogDeletePage(onOk: () -> Unit) {
        AlertDialog.Builder(activity, cameraBuilder.dialogTheme)
            .setTitle(R.string.title_confirm)
            .setMessage(R.string.delete_dialog)
            .setPositiveButton(R.string.button_yes) { dialog, id ->
                onOk.invoke()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, id -> dialog.dismiss() }
            .create()
            .show()
    }

}

