package com.ivan200.photoadapterexample.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.permission.PermissionsDelegate
import com.ivan200.photoadapter.utils.ImageUtils
import com.ivan200.photoadapterexample.Prefs
import com.ivan200.photoadapterexample.R

class MainFragment : Fragment(R.layout.fragment_main) {
    private val navigateGallery = Navigation.createNavigateOnClickListener(R.id.action_mainFragment_to_galleryFragment)

    private val mActivity get() = activity as AppCompatActivity

    private val fabPhoto get() = requireView().findViewById<FloatingActionButton>(R.id.fabPhoto)
    private val fabGallery get() = requireView().findViewById<FloatingActionButton>(R.id.fabGallery)

    private var cameraBuilder = CameraBuilder()
    private var permissionsDelegate: PermissionsDelegate? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        mActivity.title = getString(R.string.app_name)
        fabGallery.setOnClickListener(navigateGallery::onClick)

        permissionsDelegate = PermissionsDelegate(
            requireActivity(),
            this,
            savedInstanceState,
            this::takePicture,
            this::onPermissionsRejected
        )
        fabPhoto.setOnClickListener {
            updateCameraBuilder()
            permissionsDelegate?.initWithBuilder(cameraBuilder)
            permissionsDelegate?.requestPermissions()
        }
    }

    private fun updateCameraBuilder() {
        val prefs = Prefs(requireContext())
        cameraBuilder = CameraBuilder()
            .withCameraFacingBack(prefs.facingBack)
            .withChangeCameraAllowed(prefs.changeCameraAllowed)
            .withAllowMultipleImages(prefs.allowMultipleImages)
            .withLockRotate(prefs.lockRotate)
            .withSavePhotoToGallery(if (prefs.saveToGallery) prefs.galleryName else null)
            .withPreviewImage(prefs.previewImage)
            .withFullScreenMode(prefs.fullScreenMode)
            .withFitMode(prefs.fitMode)
            .withThumbnails(prefs.hasThumbnails)
            .withMaxImageSize(prefs.maxImageSize)
            .withUseSnapshot(prefs.useSnapshot)
            .withDialogTheme(R.style.AppThemeDialog)
    }

    private fun takePicture() {
        cameraBuilder.start(this)
    }

    private fun onPermissionsRejected() {
        Toast.makeText(requireContext(), getString(R.string.toast_permission_rejected), Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        permissionsDelegate?.saveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsDelegate?.onRequestPermissionsResult(requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionsDelegate?.onActivityResult(requestCode)
        cameraBuilder.onActivityResult(requestCode, resultCode, data, this::onImagesTaken)
    }

    private fun onImagesTaken(images: List<String>) {
        Prefs(requireContext()).images = Prefs(requireContext()).images.apply { addAll(images) }
        navigateGallery.onClick(fabGallery)
    }

    override fun onDestroy() {
        super.onDestroy()

        Prefs(requireContext()).images = mutableSetOf()
        ImageUtils.getPhotosDir(requireActivity()).listFiles()?.forEach { it.delete() }
        ImageUtils.getThumbsDir(requireActivity()).listFiles()?.forEach { it.delete() }
    }
}
