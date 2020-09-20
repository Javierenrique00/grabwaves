package com.mundocrativo.javier.solosonido.ui.search

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.rep.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel(val appRepository: AppRepository) : ViewModel(){

    val videoListLiveData : MutableLiveData<List<VideoObj>> by lazy { MutableLiveData<List<VideoObj>>() }
    lateinit var videoLista : MutableList<VideoObj>


    fun getSearchData(searchURL:String)=viewModelScope.launch(Dispatchers.IO){
        Log.v("msg","Search URL: $searchURL")
        val data = appRepository.getSearchFromUrl(searchURL)
        Log.v("msg","Resultados.size=${data.size}")
        videoLista = data.toMutableList()
        videoListLiveData.postValue(videoLista)
    }

    fun openVideoUrlLink(queueCmd:Int,url:String){
        appRepository.openVideoUrl(queueCmd, url)
    }

}