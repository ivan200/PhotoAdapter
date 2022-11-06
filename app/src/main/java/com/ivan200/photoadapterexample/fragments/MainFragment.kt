package com.ivan200.photoadapterexample.fragments

import android.content.Intent
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

class MainFragment : Fragment(R.layout.fragment_main) {
    private val navigateGallery = Navigation.createNavigateOnClickListener(R.id.action_mainFragment_to_galleryFragment)

    private val mActivity get() = activity as AppCompatActivity

    private val fabPhoto get() = requireView().findViewById<FloatingActionButton>(R.id.fabPhoto)
    private val fabGallery get() = requireView().findViewById<FloatingActionButton>(R.id.fabGallery)

    private var cameraBuilder = CameraBuilder()
    private lateinit var permissionsDelegate: PermissionsDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateCameraBuilder()
        permissionsDelegate = PermissionsDelegate(
            requireActivity(),
            savedInstanceState,
            cameraBuilder.dialogTheme,
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
        cameraBuilder = CameraBuilder()
            .setCameraFacingBack(prefs.facingBack)
            .setChangeCameraAllowed(prefs.changeCameraAllowed)
            .setAllowMultipleImages(prefs.allowMultipleImages)
            .setLockRotate(prefs.lockRotate)
            .setSaveTo(if (prefs.saveToGallery) SaveTo.ToGalleryWithAlbum(prefs.galleryName) else SaveTo.OnlyInternal)
            .setPreviewImage(prefs.previewImage)
            .setFullScreenMode(prefs.fullScreenMode)
            .setFlipFrontResult(prefs.flipFrontal)
            .setFitMode(prefs.fitMode)
//            .setMaxImageSize(prefs.maxImageSize)
            .setUseSnapshot(prefs.useSnapshot)
            .setDialogTheme(R.style.AppThemeDialog)
            .setCameraImplSelector(if (prefs.forceCamera1) CameraImplSelector.AlwaysCamera1 else CameraImplSelector.Camera2FromApi21)
    }

    private fun onPermissionResult(resultType: ResultType) = when (resultType) {
        is ResultType.Allow -> cameraBuilder.start(this)
        is ResultType.Denied ->
            Toast.makeText(requireContext(), getString(R.string.toast_permission_rejected), Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        permissionsDelegate.saveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        cameraBuilder.onActivityResult(requestCode, resultCode, data, this::onImagesTaken)
    }

    private fun onImagesTaken(images: List<Uri>) {
        Prefs(requireContext()).images = Prefs(requireContext()).images.apply { addAll(images.map { it.toString() }) }
        navigateGallery.onClick(fabGallery)
    }

    override fun onDestroy() {
        super.onDestroy()

        Prefs(requireContext()).images = mutableSetOf()
        SaveUtils.getSavePhotosDir(requireContext(), cameraBuilder.saveTo).listFiles()?.forEach { it.delete() }
    }
}
