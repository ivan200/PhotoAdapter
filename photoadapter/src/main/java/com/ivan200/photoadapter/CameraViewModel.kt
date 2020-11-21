package com.ivan200.photoadapter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
            if(newV >=_pictures.value!!.size) newV = _pictures.value!!.size - 1
            if(newV < 0) newV = 0
            field = newV
        }

    //вызывается галереей или камерой чтоб поменять текущий фрагмент
    private val _showCamera = MutableLiveData(true)
    val showCamera: LiveData<Boolean> = _showCamera
    fun changeFragment(showCamera: Boolean) {
        _showCamera.value = showCamera
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
    private val _backPressed = MutableLiveData<Unit?>()
    val backPressed: LiveData<Unit?> = _backPressed
    fun backPressed() {
        _backPressed.value = _backPressed.value
    }

    //при повороте
    private val _rotate = MutableLiveData(0)
    val rotate: LiveData<Int> = _rotate
    fun rotate(angle: Int) {
        if(_rotate.value != angle) {
            _rotate.value = angle
        }
    }

    //При нажатии кнопок громкости
    private val _volumeKeyPressed = MutableLiveData<Unit?>()
    val volumeKeyPressed: LiveData<Unit?> = _volumeKeyPressed
    fun volumeKeyPressed() {
        _volumeKeyPressed.value = _volumeKeyPressed.value
    }

    fun deleteCurrentPage(){
        val deletedPicture = _pictures.value!!.getOrNull(imageNumber) ?: return

        _pictures.value!!.removeAt(imageNumber)
        imageNumber = imageNumber //reset current image number
        _pictures.value = _pictures.value

        deletePage(deletedPicture)
    }

    fun deleteAllFiles(){
        imageNumber = 0
        _pictures.value!!.forEach { deletePage(it) }
    }

    fun deletePage(page: PictureInfo){
        page.apply {
            if(file.exists()){
                file.delete()
            }
            if(thumbFile?.exists() == true){
                thumbFile.delete()
            }
        }
    }

    fun onFileSaved(fileToSave: File, thumbToSave: File?){
        _pictures.value!!.add(imageNumber, PictureInfo(fileToSave, thumbToSave))
        _pictures.value = _pictures.value   //this need to call liveData update
    }

}