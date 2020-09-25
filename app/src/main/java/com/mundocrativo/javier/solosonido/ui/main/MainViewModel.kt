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
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*

class MainViewModel(private val appRepository: AppRepository) : ViewModel() {

    //var enlaceExternal :String? = null
    val musicServiceConnection = appRepository.musicServiceConnection
    val openVideoUrlLiveData = appRepository.openVideoUrlLiveData
    var lastOpenUrl : Pair<Int,String> = Pair(0,"")
    val videoListLiveData : MutableLiveData<List<VideoObj>> by lazy { MutableLiveData<List<VideoObj>>() }
    lateinit var videoLista : MutableList<VideoObj>
    //val videoItemChanged : MutableLiveData<Pair<Int,VideoObj>> by lazy { MutableLiveData<Pair<Int,VideoObj>>() }
    val notifyItemRemoved : MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val listToRemove = mutableListOf<VideoObj>()

    suspend fun insertNewVideo(videoObj:VideoObj):Long = coroutineScope {
        appRepository.insertVideo(videoObj)
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
        Log.v("msg","trayendo info ${videoIn.url}")
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

    fun deleteVideoListElement(index:Int,urlInfo:String)=viewModelScope.launch(Dispatchers.IO){
        appRepository.deleteVideo(videoLista[index].id,urlInfo)
        videoLista.removeAt(index)
        notifyItemRemoved.postValue(index)
    }

    fun deleteVideoListSelected(pref: AppPreferences) = viewModelScope.launch(Dispatchers.IO) {
        listToRemove.clear()
        videoLista.forEach {
            if(it.esSelected){
                appRepository.deleteVideo(it.id,Util.transUrlToServInfo(it.url,pref))
                listToRemove.add(it)
            }
        }
        listToRemove.forEach {itemDelete ->
            val index= videoLista.indexOfFirst { it.id==itemDelete.id }
            if(index>=0){
                videoLista.removeAt(index)
                notifyItemRemoved.postValue(index)
                delay(300)
            }
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
                Util.getBitmap(it.thumbnailUrl,context))
            launch(Dispatchers.Main) {
                MediaHelper.cmdSendSongWithMetadataToPlayer(queueCmd,audioMetadata, musicServiceConnection)
            }
        }

    }

    fun launchPlayerMultiple(queueCmd:Int,pref:AppPreferences,context: Context)= viewModelScope.launch(Dispatchers.IO){
        val seleccionados = videoLista.filter { it.esSelected }
        var queueCmdUpdate = queueCmd
        seleccionados.forEach { video->
            val info = appRepository.getInfoFromUrl(Util.transUrlToServInfo(video.url,pref))
            info?.let { info ->
                val audioMetadata = AudioMetadata(
                    video.url,
                    video.title,
                    video.channel,
                    Util.createUrlConnectionStringPlay(pref.server!!,video.url,pref.hQ),
                    video.thumbnailUrl,
                    Util.getBitmap(video.thumbnailUrl,context))
                withContext(Dispatchers.Main){
                    MediaHelper.cmdSendSongWithMetadataToPlayer(queueCmdUpdate,audioMetadata,musicServiceConnection)
                    queueCmdUpdate = MediaHelper.QUEUE_ADD
                }
            }
        }
    }



    fun isVideolistInitialized() = this::videoLista.isInitialized



}