package com.ivan200.photoadapter

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ivan200.photoadapter.base.FragmentChangeState
import com.ivan200.photoadapter.utils.Event
import com.ivan200.photoadapter.utils.parcelableArrayCompat
import com.ivan200.photoadapter.utils.serializableCompat
import java.io.File

//
// Created by Ivan200 on 18.10.2019.
//
class CameraViewModel : ViewModel() {
    private val _pictures = MutableLiveData<MutableList<PictureInfo>>(arrayListOf())
    val pictures: LiveData<MutableList<PictureInfo>> = _pictures

    var imageNumber: Int = 0
        set(value) {
            var newV = value
            if (newV >= _pictures.value!!.size) newV = _pictures.value!!.size - 1
            if (newV < 0) newV = 0
            field = newV
        }

    //вызывается галереей или камерой чтоб поменять текущий фрагмент
    private val _fragmentState = MutableLiveData(defaultFragmentState)
    val fragmentState: LiveData<FragmentChangeState> = _fragmentState
    fun changeState(fragmentState: FragmentChangeState) {
        _fragmentState.value = fragmentState
    }

    //вызывается адаптером галереи, когда картинка в галерее загружена из файла и отрисована на экране,
    //чтоб можно было плавно сменить фрагменты и потом возобновить отрисовку превью
    private val _curPageLoaded = MutableLiveData<PictureInfo>()
    val curPageLoaded: LiveData<PictureInfo> = _curPageLoaded
    fun updateOnCurrentPageLoaded(pictureInfo: PictureInfo) {
        _curPageLoaded.value = pictureInfo
    }

    //Вызывается при нажатии кнопки назад в активити, когда открыта галерея,
    //и при нажатии кнопки 'ещё' в галерее, чтоб автоскролльнуть на первую картинку
    private val _scrollToPage = MutableLiveData<Unit?>()
    val scrollToPage: LiveData<Unit?> = _scrollToPage
    fun needScrollToPage(page: Int) {
        imageNumber = page
        _scrollToPage.value = _scrollToPage.value
    }

    //вызывается адаптером галереи, когда картинка в галерее загружена из файла и отрисована на экране,
    //чтоб можно было сменить фрагменты и потом возобновить отрисовку превью
    private val _success = MutableLiveData<Unit?>()
    val success: LiveData<Unit?> = _success
    fun success() {
        _success.value = _success.value
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

    //При нажатии кнопок громкости
    private val _volumeKeyPressed = MutableLiveData<Unit?>()
    val volumeKeyPressed: LiveData<Unit?> = _volumeKeyPressed
    fun volumeKeyPressed() {
        _volumeKeyPressed.value = _volumeKeyPressed.value
    }

    //При нажатии кнопок громкости
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

    fun onFileSaved(fileToSave: File) {
        _pictures.value!!.add(imageNumber, PictureInfo(fileToSave))
        _pictures.value = _pictures.value   //this need to call liveData update
    }


    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArray(KEY_Pictures, pictures.value?.toTypedArray() ?: emptyArray())
        outState.putSerializable(KEY_FragmentState, this.fragmentState.value ?: defaultFragmentState)
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        _pictures.value = savedInstanceState.parcelableArrayCompat<PictureInfo>(KEY_Pictures).orEmpty().toMutableList()
        _fragmentState.value = savedInstanceState.serializableCompat(KEY_FragmentState) ?: defaultFragmentState
    }

    companion object {
        private val defaultFragmentState = FragmentChangeState.CAMERA
        private const val KEY_Pictures = "KEY_Pictures"
        private const val KEY_FragmentState = "KEY_FragmentState"
    }
}


