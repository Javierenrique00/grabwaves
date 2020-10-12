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
import com.mundocrativo.javier.solosonido.ui.historia.KIND_URL_PLAYLIST
import com.mundocrativo.javier.solosonido.ui.historia.KIND_URL_VIDEO
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*

class MainViewModel(private val appRepository: AppRepository) : ViewModel() {

    //var enlaceExternal :String? = null
    val musicServiceConnection = appRepository.musicServiceConnection
    val openVideoUrlLiveData = appRepository.openVideoUrlLiveData
    var lastUrlValue = ""
    val openVideoListUrlLiveData = appRepository.openVideoListUrlLiveData
    var lastListOpenUrl : List<VideoObj> = mutableListOf()
    val videoListLiveData : MutableLiveData<List<VideoObj>> by lazy { MutableLiveData<List<VideoObj>>() }
    lateinit var videoLista : MutableList<VideoObj>
    //val videoItemChanged : MutableLiveData<Pair<Int,VideoObj>> by lazy { MutableLiveData<Pair<Int,VideoObj>>() }
    val notifyItemRemoved : MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val listToRemove = mutableListOf<VideoObj>()
    val playVideoListPair = appRepository.playVideoListPair //---para enviar la lista al player
    var loadLinkfromExternalapp = false
    var isServerChecked = false
    //--- observer para lanzar a otro tabfragment
    val pageChangePager : MutableLiveData<Int> by lazy { MutableLiveData<Int>() }


    suspend fun checkForServer(pref: AppPreferences):Boolean = withContext(Dispatchers.IO){
        if(!isServerChecked){
            isServerChecked = appRepository.checkServOk(pref)
        }
        isServerChecked
    }



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

    fun getUrlInfo(position:Int,videoIn:VideoObj,pref: AppPreferences,msgError:String):Pair<Int,VideoObj>{
        //Log.v("msg","trayendo info ${videoIn.url} KindMedia:${videoIn.kindMedia}")
        when(videoIn.kindMedia){
            KIND_URL_VIDEO ->{
                val ruta = Util.transUrlToServInfo(videoIn.url,pref)
                val info = appRepository.getInfoFromUrl(ruta)
                info?.let {
                    return Pair(position,VideoObj(
                        videoIn.id,
                        videoIn.url,
                        it.title,
                        it.channel,
                        it.thumbnailUrl,
                        it.width,
                        it.height,
                        it.duration,
                        videoIn.timestamp,
                        videoIn.kindMedia,
                        0,
                        true,
                        false,
                        false,
                        videoIn.itemPosition,
                        null,
                        false,
                        ""
                    ))
                }
            }
            KIND_URL_PLAYLIST ->{
                val ruta = Util.transUrlToServePlaylist(videoIn.url,pref)
                return Pair(position,appRepository.getPlayListFromUrl(videoIn,ruta,msgError))
            }
        }
        val vidReturnError = VideoObj()
        with(vidReturnError){
            id = videoIn.id
            title = msgError
            url = videoIn.url
            itemPosition = videoIn.itemPosition
            esInfoReady = true
            esUrlReady = true
            timestamp = videoIn.timestamp
            kindMedia = videoIn.kindMedia
        }
        return Pair(position,vidReturnError)
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


    fun launchPlayerMultiple(queueCmd:Int,playVideoList:List<VideoObj>,pref:AppPreferences,context: Context)= viewModelScope.launch(Dispatchers.IO){

        var queueCmdUpdate = queueCmd
        //---crea la lista de videos final agregando los videos de la playlist
        val finalVideoList = mutableListOf<VideoObj>()
        playVideoList.forEach { video ->
            when(Util.checkKindLink(video.url)){
                KIND_URL_VIDEO -> finalVideoList.add(video)
                KIND_URL_PLAYLIST -> finalVideoList.addAll(appRepository.getPlayListFromUrl(Util.transUrlToServePlaylist(video.url,pref)))
            }
        }

        //--- Ahora envia la lista directamente al fragmento player
        //playVideoListPair.postValue(Pair(queueCmd,finalVideoList)) no lo vamos a enviar al player

        //--Ahora envía la lista al exoplayer
        //val audioList = mutableListOf<AudioMetadata>()
        val audioList = ArrayList<AudioMetadata>()
        finalVideoList.forEach {
            var esVacio = true
            if(it.title.isEmpty()){
                //-- si no hay datos los intenta traer del cache
                val metaCache = appRepository.getMetadataCache(it.url)
                //Log.v("msg","No tengo datos, url=${it.url} cache=$metaCache")
                if(metaCache!=null) {
                    audioList.add(metaCache)
                    esVacio = false
                }
            }
            if(esVacio) {
                audioList.add(
                    AudioMetadata(
                        it.url,
                        it.title,
                        it.channel,
                        Util.createUrlConnectionStringPlay(pref.server, it.url, pref.hQ),
                        it.thumbnailUrl,
                        it.duration
                    )
                )
            }
        }
        //---Tenemos el audio list con toda la metadata
        appRepository.putMetadataListCache(audioList)

        withContext(Dispatchers.Main){
            MediaHelper.cmdSendListToPlayer(queueCmd,0,audioList,musicServiceConnection)

            //--- para que cambie al player --> no vamos a abrir el player de manera forzada
            //if(!appRepository.getPlayerIsOpen()) pageChangePager.postValue(2) //--salta al player una vez envía todas las canciones
        }


    }

    fun isVideolistInitialized() = this::videoLista.isInitialized

}