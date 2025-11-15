package com.ivan200.photoadapter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ivan200.photoadapter.base.FragmentChangeState
import com.ivan200.photoadapter.base.ScaleDelegate
import com.ivan200.photoadapter.utils.Event
import com.ivan200.photoadapter.utils.SavedStateUtils
import com.ivan200.photoadapter.utils.SavedStateUtils.SavedLiveData
import java.io.File

//
// Created by Ivan200 on 18.10.2019.
//
class CameraViewModel(handle: SavedStateHandle) : ViewModel() {

    private val _pictures by SavedLiveData<MutableList<PictureInfo>>(handle, arrayListOf())
    val pictures: LiveData<MutableList<PictureInfo>> = _pictures

    var _imageNumber by SavedStateUtils.SavedValue<Int>(handle, 0)
    var imageNumber: Int
        get() = _imageNumber
        set(value) {
            var newV = value
            if (newV >= _pictures.value!!.size) newV = _pictures.value!!.size - 1
            if (newV < 0) newV = 0
            _imageNumber = newV
        }

    //вызывается галереей или камерой чтоб поменять текущий фрагмент
    private val _fragmentState by SavedLiveData(handle, defaultFragmentState)
    val fragmentState: LiveData<FragmentChangeState> = _fragmentState
    fun changeState(fragmentState: FragmentChangeState) {
        _fragmentState.value = fragmentState
    }

    //вызывается адаптером галереи, когда картинка в галерее загружена из файла и отрисована на экране,
    //чтоб можно было плавно сменить фрагменты и потом возобновить отрисовку превью
    private val _curPageLoaded by SavedLiveData<PictureInfo>(handle)
    val curPageLoaded: LiveData<PictureInfo> = _curPageLoaded
    fun updateOnCurrentPageLoaded(pictureInfo: PictureInfo) {
        _curPageLoaded.value = pictureInfo
    }

    //Вызывается при нажатии кнопки назад в активити, когда открыта галерея,
    //и при нажатии кнопки 'ещё' в галерее, чтоб автоскролльнуть на первую картинку
    private val _scrollToFirstPage = MutableLiveData<Event<Boolean>>()
    val scrollToFirstPage: LiveData<Event<Boolean>> = _scrollToFirstPage
    fun needScrollToFirstPage() {
        imageNumber = 0
        _scrollToFirstPage.postValue(Event(true))
    }

    //вызывается адаптером галереи, когда картинка в галерее загружена из файла и отрисована на экране,
    //чтоб можно было сменить фрагменты и потом возобновить отрисовку превью
    private val _success = MutableLiveData<Unit?>()
    val success: LiveData<Unit?> = _success
    fun success() {
        _success.value = null
    }

    //вызывается в активити при нажатии кнопки назад,
    //чтобы если открыт фрагмент галереи показать возможный диалог удаления фотки
    private val _backPressed = MutableLiveData<Event<Boolean>>()
    val backPressed: LiveData<Event<Boolean>> = _backPressed
    fun backPressed() {
        _backPressed.value = Event(true)
    }

    //при повороте
    private val _rotate = MutableLiveData(0)
    val rotate: LiveData<Int> = _rotate
    fun rotate(angle: Int) {
        if (_rotate.value != angle) {
            _rotate.value = angle
        }
    }

    //симуляция нажатия кнопки фотографирования
    private val _captureSimulate = MutableLiveData<Event<Boolean>>()
    val captureSimulate: LiveData<Event<Boolean>> = _captureSimulate

    //симуляция нажатия кнопки "принять" в галерее
    private val _acceptSimulate = MutableLiveData<Event<Boolean>>()
    val acceptSimulate: LiveData<Event<Boolean>> = _acceptSimulate

    //При нажатии кнопок громкости
    fun volumeKeyPressed() {
        when (fragmentState.value!!) {
            FragmentChangeState.CAMERA -> _captureSimulate.postValue(Event(true))
            FragmentChangeState.GALLERY -> _acceptSimulate.postValue(Event(true))
            FragmentChangeState.WAITING_FOR_IMAGE -> Unit
        }
    }

    //При изменении разрешений на камеру, нужно перезапустить камеру
    private val _restartCamera = MutableLiveData<Event<Boolean>>()
    val restartCamera: LiveData<Event<Boolean>> = _restartCamera
    fun restartCamera() {
        _restartCamera.value = Event(true)
    }

    fun deleteCurrentPage() {
        val deletedPicture = _pictures.value!!.getOrNull(imageNumber) ?: return

        _pictures.value!!.removeAt(imageNumber)
        imageNumber = imageNumber //reset current image number
        _pictures.value = _pictures.value

        deletePage(deletedPicture)
    }

    fun deleteAllFiles() {
        imageNumber = 0
        _pictures.value!!.forEach { deletePage(it) }
    }

    fun deletePage(page: PictureInfo) {
        page.apply {
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun onFileSaved(fileToSave: File, currentScale: ScaleDelegate) {
        _pictures.value!!.add(imageNumber, PictureInfo(fileToSave, currentScale))
        _pictures.value = _pictures.value   //this need to call liveData update
    }

    companion object {
        private val defaultFragmentState = FragmentChangeState.CAMERA
        private const val KEY_Pictures = "KEY_Pictures"
        private const val KEY_FragmentState = "KEY_FragmentState"
    }
}
