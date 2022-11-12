package com.ivan200.photoadapter.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ivan200.photoadapter.R
import com.ivan200.photoadapter.base.SimpleCameraInfo
import com.ivan200.photoadapter.utils.CameraSelector
import com.ivan200.photoadapter.utils.SimpleDiffUtilCallback
import com.ivan200.photoadapter.utils.rotateItems

/**
 * @author ivan200
 * @since 11.09.2022
 */
class CamerasAdapter(private val onCameraSelected: (SimpleCameraInfo) -> Unit) : RecyclerView.Adapter<CamerasAdapter.CamerasViewHolder>() {

    data class SelectableCamera(
        val cameraInfo: SimpleCameraInfo,
        val isSelected: Boolean,
        val rotateAngle: Int
    )

    var cameras: List<SelectableCamera> = listOf()

    fun update(allCameras: List<SimpleCameraInfo>, current: SimpleCameraInfo, rotateAngle: Int) {
        val newList = allCameras.map {
            SelectableCamera(it, it == current, rotateAngle)
        }

        val diffCallback = SimpleDiffUtilCallback(oldList = cameras, newList = newList) { it.cameraInfo.cameraId }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        cameras = newList
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CamerasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.select_camera_cell, parent, false)
        return CamerasViewHolder(view, onCameraSelected)
    }

    override fun getItemCount(): Int {
        return cameras.size
    }

    override fun onBindViewHolder(holder: CamerasViewHolder, position: Int) {
        holder.bind(cameras[position])
    }

    class CamerasViewHolder(val view: View, private val onCameraSelected: (SimpleCameraInfo) -> Unit) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private lateinit var currentCamera: SelectableCamera
        private val itemSelector = view.findViewById<CameraSelector>(R.id.item_selector)
        private val textChecked = view.findViewById<TextView>(R.id.item_text_checked)
        private val textUnchecked = view.findViewById<TextView>(R.id.item_text_unchecked)

        init {
            view.setOnClickListener(this)
        }

        fun bind(camera: SelectableCamera) {
            itemSelector.setChecked(camera.isSelected, ::currentCamera.isInitialized)
            currentCamera = camera

            textChecked.isInvisible = camera.isSelected
            textUnchecked.isVisible = camera.isSelected

            textChecked.text = camera.cameraInfo.name
            textUnchecked.text = camera.cameraInfo.nameSelected

            textChecked.rotation = camera.rotateAngle.toFloat()
            textUnchecked.rotation = camera.rotateAngle.toFloat()
        }

        fun rotateItem(rotateAngle: Int) {
            rotateItems(rotateAngle, textChecked, textUnchecked)
        }

        override fun onClick(v: View?) {
            onCameraSelected.invoke(currentCamera.cameraInfo)
        }
    }
}
