package com.ivan200.photoadapterexample.fragments

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.permission.PermissionsDelegate
import com.ivan200.photoadapter.permission.ResultType
import com.ivan200.photoadapter.utils.CameraImplSelector
import com.ivan200.photoadapter.utils.SaveTo
import com.ivan200.photoadapter.utils.SaveUtils
import com.ivan200.photoadapterexample.Prefs
import com.ivan200.photoadapterexample.R

class MainFragment : Fragment(R.layout.fragment_main), CameraBuilder.ImagesTakenCallback {
    private val navigateGallery = Navigation.createNavigateOnClickListener(R.id.action_mainFragment_to_galleryFragment)

    private val mActivity get() = activity as AppCompatActivity

    private val fabPhoto get() = requireView().findViewById<FloatingActionButton>(R.id.fabPhoto)
    private val fabGallery get() = requireView().findViewById<FloatingActionButton>(R.id.fabGallery)

    private val cameraBuilder = CameraBuilder()
    private val takePicturesLauncher = cameraBuilder.registerForResult(this, this)

    private lateinit var permissionsDelegate: PermissionsDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateCameraBuilder()
        permissionsDelegate = PermissionsDelegate(
            requireActivity(),
            savedInstanceState,
            R.style.AppThemeDialog,
            this::onPermissionResult
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        mActivity.title = getString(R.string.app_name)
        fabGallery.setOnClickListener(navigateGallery::onClick)

        fabPhoto.setOnClickListener {
            updateCameraBuilder()
            permissionsDelegate.initWithBuilder(cameraBuilder)
            permissionsDelegate.queryPermissionsOnStart()
        }
    }

    private fun updateCameraBuilder() {
        val prefs = Prefs(requireContext())
        cameraBuilder.apply {
            facingBack = prefs.facingBack
            allowChangeCamera = prefs.allowChangeCamera
            allowMultipleImages = prefs.allowMultipleImages
            lockRotate = prefs.lockRotate
            saveTo = if (prefs.saveToGallery) SaveTo.ToGalleryWithAlbum(prefs.galleryName) else SaveTo.OnlyInternal
            allowPreviewResult = prefs.allowPreviewResult
            fullScreenMode = prefs.fullScreenMode
            flipFrontResult = prefs.flipFrontal
            blurOnSwitch = prefs.blurOnSwitch
            fillPreview = prefs.fillPreview
            allowToggleFit = prefs.allowToggleFit
            showBackButton = prefs.showBackButton
            useSnapshot = prefs.useSnapshot
            dialogTheme = R.style.AppThemeDialog
            cameraImplSelector = if (prefs.forceCamera1) CameraImplSelector.AlwaysCamera1 else CameraImplSelector.Camera2FromApi21
        }
    }

    private fun onPermissionResult(resultType: ResultType) = when (resultType) {
        is ResultType.Allow -> takePicturesLauncher.launch(cameraBuilder.getTakePictureIntent(requireContext()))
        is ResultType.Denied -> Toast.makeText(requireContext(), getString(R.string.toast_permission_rejected), Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        permissionsDelegate.saveInstanceState(outState)
    }

    override fun onImagesTaken(images: List<Uri>) {
        Prefs(requireContext()).images = Prefs(requireContext()).images.apply { addAll(images.map { it.toString() }) }
        navigateGallery.onClick(fabGallery)
    }

    override fun onDestroy() {
        super.onDestroy()

        Prefs(requireContext()).images = mutableSetOf()
        SaveUtils.getSavePhotosDir(requireContext(), cameraBuilder.saveTo).listFiles()?.forEach { it.delete() }
    }
}
