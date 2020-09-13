package com.mundocrativo.javier.solosonido.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.rep.AppRepository
import com.soywiz.klock.DateTime
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(private val appRepository: AppRepository) : ViewModel() {

    //var enlaceExternal :String? = null
    val openVideoUrlLiveData = appRepository.openVideoUrlLiveData
    val videoListLiveData : MutableLiveData<List<VideoObj>> by lazy { MutableLiveData<List<VideoObj>>() }
    lateinit var videoLista : MutableList<VideoObj>
    //val videoItemChanged : MutableLiveData<Pair<Int,VideoObj>> by lazy { MutableLiveData<Pair<Int,VideoObj>>() }
    val notifyItemRemoved : MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    fun insertNewVideo(url:String) =  viewModelScope.launch(Dispatchers.IO){
        Log.v("msg","Inserting video to the database")
        val video = VideoObj()
        video.url = url
        video.timestamp = DateTime.now().unixMillisLong
        appRepository.insertVideo(video)
    }

    fun loadVideosFromDb() = viewModelScope.launch(Dispatchers.IO){
        videoLista = appRepository.listVideos().toMutableList()
        videoListLiveData.postValue(videoLista)
    }

    fun getUrlInfo(videoIn:VideoObj,ruta:String):VideoObj?{
        val info = appRepository.getInfoFromUrl(ruta)
        info?.let {
            return VideoObj(
                videoIn.id,
                videoIn.url,
                it.title,
                it.channel,
                it.thumbnailUrl,
                it.width,
                it.height,
                it.duration,
                videoIn.timestamp,
                true,
                false,
                true,
                videoIn.itemPosition,
                null
            )
        }
        return null
    }

    fun deleteVideoListElement(id:Long,urlInfo:String)=viewModelScope.launch(Dispatchers.IO){
        appRepository.deleteVideo(id,urlInfo)
        val index = videoLista.indexOfFirst { it.id==id }
        if(index>=0){
            Log.v("msg","---Removiendo de la lista index=$index and id=$id del cache:$urlInfo")
            videoLista.removeAt(index)
            notifyItemRemoved.postValue(index)
        }
    }

    fun openVideoUrlLink(url:String){
        appRepository.openVideoUrl(url)
    }

}