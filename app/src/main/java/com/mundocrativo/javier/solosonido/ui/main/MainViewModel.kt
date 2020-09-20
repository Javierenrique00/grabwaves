package com.mundocrativo.javier.solosonido.ui.main

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.load
import com.google.android.gms.cast.framework.CastContext
import com.mundocrativo.javier.solosonido.library.MediaHelper
import com.mundocrativo.javier.solosonido.model.AudioMetadata
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.rep.AppRepository
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(private val appRepository: AppRepository) : ViewModel() {

    //var enlaceExternal :String? = null
    val musicServiceConnection = appRepository.musicServiceConnection
    val openVideoUrlLiveData = appRepository.openVideoUrlLiveData
    val videoListLiveData : MutableLiveData<List<VideoObj>> by lazy { MutableLiveData<List<VideoObj>>() }
    lateinit var videoLista : MutableList<VideoObj>
    //val videoItemChanged : MutableLiveData<Pair<Int,VideoObj>> by lazy { MutableLiveData<Pair<Int,VideoObj>>() }
    val notifyItemRemoved : MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    fun insertNewVideo(url:String) =  viewModelScope.launch(Dispatchers.IO){
        //Log.v("msg","Inserting video to the database")
        val video = VideoObj()
        video.url = url
        video.timestamp = DateTime.now().unixMillisLong
        appRepository.insertVideo(video)
        loadVideosFromDb()
    }

    fun loadVideosFromDb() = viewModelScope.launch(Dispatchers.IO){
        videoLista = appRepository.listVideos().toMutableList()
        videoLista.forEach {
            it.esSelected = false
            it.esUrlReady = false
            it.esInfoReady = false
            it.itemPosition = 0
            it.thumbnailImg = null
        }
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
                false,
                videoIn.itemPosition,
                null,
                false,
                ""
            )
        }
        return null
    }

    fun deleteVideoListElement(id:Long,urlInfo:String)=viewModelScope.launch(Dispatchers.IO){
        appRepository.deleteVideo(id,urlInfo)
        val index = videoLista.indexOfFirst { it.id==id }
        if(index>=0){
            //Log.v("msg","---Removiendo de la lista index=$index and id=$id del cache:$urlInfo")
            videoLista.removeAt(index)
            notifyItemRemoved.postValue(index)
        }
    }

//    fun openVideoUrlLink(url:String){
//        appRepository.openVideoUrl(url)
//    }

    //--- Commando que adiciona directamente a la cola
    fun launchPlayer(queueCmd:Int,mediaUrl: String,infoUrl:String,originalUrl:String,context: Context) = viewModelScope.launch(Dispatchers.IO){
        //--- tiene que traer la metadata
        val info = appRepository.suspendGetInfoFromUrl(infoUrl)
        info?.let {
            val audioMetadata = AudioMetadata(
                originalUrl,
                it.title,
                it.channel,
                mediaUrl,
                it.thumbnailUrl,
                Util.getBitmap(it.thumbnailUrl,context)
            )
            launch(Dispatchers.Main) {
                MediaHelper.cmdSendSongWithMetadataToPlayer(queueCmd,audioMetadata, musicServiceConnection)
            }
        }

    }




}