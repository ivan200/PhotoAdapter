package com.ivan200.photoadapter.fragments

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.core.graphics.alpha
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.ivan200.photoadapter.CameraActivity
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.CameraViewModel
import com.ivan200.photoadapter.R
import com.ivan200.photoadapter.base.FragmentChangeState
import com.ivan200.photoadapter.utils.ApplyInsetsListener
import com.ivan200.photoadapter.utils.getColorCompat
import com.ivan200.photoadapter.utils.onClick
import com.ivan200.photoadapter.utils.padBottomViewWithInsets
import com.ivan200.photoadapter.utils.padTopViewWithInsets
import com.ivan200.photoadapter.utils.rotateItems
import com.ivan200.photoadapter.utils.simulateClick
import me.relex.circleindicator.CircleIndicator3

//
// Created by Ivan200 on 16.10.2019.
//
@Suppress("unused")
class GalleryFragment : Fragment(R.layout.photo_fragment_gallery), ApplyInsetsListener {

    private val cameraViewModel: CameraViewModel by lazy {
        ViewModelProvider(activity as CameraActivity)[CameraViewModel::class.java]
    }
    private lateinit var cameraBuilder: CameraBuilder

    private val pagerImages get() = requireView().findViewById<ViewPager2>(R.id.pager_images)
    private val statusView get() = requireView().findViewById<View>(R.id.statusView)
    private val btnAccept get() = requireView().findViewById<ImageButton>(R.id.btn_accept)
    private val btnDelete get() = requireView().findViewById<ImageButton>(R.id.btn_deletePicture)
    private val btnMore get() = requireView().findViewById<ImageButton>(R.id.btn_more)
    private val btnBack get() = requireView().findViewById<ImageButton>(R.id.btn_back)
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
            // Очищаем значение обозначающее что вью галереи должно быть над панелью кнопок
            (pagerImages.layoutParams as? RelativeLayout.LayoutParams)?.apply {
                arrayOf(RelativeLayout.BELOW, RelativeLayout.ABOVE, RelativeLayout.LEFT_OF, RelativeLayout.RIGHT_OF).forEach {
                    addRule(it, 0)
                }
            }
            val alpha = view.context.getColorCompat(R.color.circle_icon_fullscreen_transparency).alpha
            btnMore.background.alpha = alpha
            btnDelete.background.alpha = alpha
        }
        indicator.setViewPager(pagerImages)
        pagerAdapter.registerAdapterDataObserver(indicator.adapterDataObserver)

        pagerAdapter.update(cameraViewModel.pictures.value!!, cameraViewModel.imageNumber)
        pagerImages.setCurrentItem(cameraViewModel.imageNumber, false)

        cameraViewModel.pictures.observe(requireActivity()) {
            if (it.isEmpty()) {
                cameraViewModel.changeState(FragmentChangeState.CAMERA)
            }
            indicator.isVisible = it.size > 1
            pagerAdapter.update(it, cameraViewModel.imageNumber)
        }

        btnDelete.onClick {
            showDialogDeletePage(cameraViewModel::deleteCurrentPage)
        }

        cameraViewModel.scrollToFirstPage.observe(requireActivity()) {
            it?.let {
                pagerImages.setCurrentItem(cameraViewModel.imageNumber, false)
            }
        }

        btnMore.isVisible = cameraBuilder.allowMultipleImages
        btnMore.onClick(this::onMorePhotosPressed)

        btnAccept.onClick(cameraViewModel::success)

        cameraViewModel.backPressed.observe(requireActivity()) {
            it.get()?.apply { onMorePhotosPressed() }
        }

        cameraViewModel.acceptSimulate.observe(requireActivity()) {
            it.get()?.let {
                btnAccept.simulateClick()
            }
        }

        cameraViewModel.rotate.observe(requireActivity()) {
            rotateItems(it, btnAccept, btnDelete, btnMore, btnBack)
        }

        insets?.let {
            setWindowInsets(it)
        }

        if (cameraBuilder.showBackButton) {
            btnBack.isVisible = true
            btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        } else {
            btnBack.isVisible = false
        }
    }

    private fun onMorePhotosPressed() {
        if (cameraBuilder.allowMultipleImages) {
            cameraViewModel.needScrollToFirstPage()
            cameraViewModel.changeState(FragmentChangeState.CAMERA)
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

    private fun setWindowInsets(insets: WindowInsetsCompat) {
        actionLayout.padBottomViewWithInsets(insets)
        if (!cameraBuilder.fullScreenMode) {
            statusView.padTopViewWithInsets(insets)
        }
    }

    private fun showDialogDeletePage(onOk: () -> Unit) {
        val dialog = AlertDialog.Builder(activity, cameraBuilder.dialogTheme)
            .setTitle(R.string.title_confirm)
            .setMessage(R.string.delete_dialog)
            .setPositiveButton(R.string.button_yes) { dialog, _ ->
                onOk.invoke()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
        dialog.show()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
}
