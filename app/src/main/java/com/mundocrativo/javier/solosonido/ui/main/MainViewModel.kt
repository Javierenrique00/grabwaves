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
import com.mundocrativo.javier.solosonido.library.MediaHelper.QUEUE_NEW
import com.mundocrativo.javier.solosonido.model.AudioMetadata
import com.mundocrativo.javier.solosonido.model.ProgressInfo
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.rep.AppRepository
import com.mundocrativo.javier.solosonido.ui.historia.*
import com.mundocrativo.javier.solosonido.util.*
import com.soywiz.klock.DateTime
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import java.lang.Exception

class MainViewModel(private val appRepository: AppRepository) : ViewModel() {

    val imageLoader = appRepository.getAppCoilImageLoader()
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
    val showToastMessage : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val preloadProgress : MutableLiveData<ProgressInfo> by lazy { MutableLiveData<ProgressInfo>() }
    val download : MutableLiveData<VideoObj> by lazy { MutableLiveData<VideoObj>() }
    private lateinit var jobPreload : Job

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
        Log.v("msg","trayendo info ${videoIn.url} KindMedia:${videoIn.kindMedia}")
        when(videoIn.kindMedia){
            KIND_URL_VIDEO ->{
                val ruta = Util.transUrlToServInfo(videoIn.url,pref)
                Log.v("msg","Get Info:$ruta")
                val info = appRepository.getInfoFromUrl(ruta)
                Log.v("msg","Info-----------$info")
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
                        0,"",0,
                        it.urlVideo,
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
                Log.v("msg","get Playlist:$ruta")
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


    fun launchPlayerMultiple(queueCmd:Int,playVideoList:List<VideoObj>,pref:AppPreferences,msgPreload:String){
        if(this::jobPreload.isInitialized) jobPreload.cancel()

        jobPreload = viewModelScope.launch(Dispatchers.IO){
            var queueCmdUpdate = queueCmd
            //---crea la lista de videos final agregando los videos de la playlist
            val finalVideoList = mutableListOf<VideoObj>()
            playVideoList.forEach { video ->
                when(Util.checkKindLink(video.url)){
                    KIND_URL_VIDEO -> finalVideoList.add(video)
                    KIND_URL_PLAYLIST -> finalVideoList.addAll(appRepository.getPlayListFromUrl(Util.transUrlToServePlaylist(video.url,pref)))
                }
            }

            //--Ahora envía la lista al exoplayer
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
                            Util.createUrlConnectionStringPlay(pref.server, it.url, pref.hQ,pref.trans,false,it.extraUrlVideo),
                            it.thumbnailUrl,
                            it.duration,
                            it.extraUrlVideo
                        )
                    )
                }
            }

            //--- para hacer el preload
            if((audioList.size>0) and (queueCmd==QUEUE_NEW)){
                var result :Int? = null
                val preloadFile = PreloadFile(pref,audioList[0].duration,appRepository)
                launch {
                    result = preloadFile.preload(audioList[0].mediaId,audioList[0].extraUrlVideo)
                }

                preloadFile.checkProgress {
                    preloadProgress.postValue(it)
                }

                if(result == PRELOAD_READY){
                    showToastMessage.postValue(msgPreload)
                    //---Tenemos el audio list con toda la metadata
                    appRepository.putMetadataListCache(audioList)

                    withContext(Dispatchers.Main){
                        MediaHelper.cmdSendListToPlayer(queueCmd,0,audioList,musicServiceConnection)

                        //--- para que cambie al player --> no vamos a abrir el player de manera forzada
                        //if(!appRepository.getPlayerIsOpen()) pageChangePager.postValue(2) //--salta al player una vez envía todas las canciones
                    }
                }
                else{
                    showToastMessage.postValue("Error")
                }

            }else{
                //---Tenemos el audio list con toda la metadata
                appRepository.putMetadataListCache(audioList)

                withContext(Dispatchers.Main){
                    MediaHelper.cmdSendListToPlayer(queueCmd,0,audioList,musicServiceConnection)

                }
            }

        }
    }

    fun launchPlayerDirect(queueCmd:Int,playVideoList:List<VideoObj>,pref:AppPreferences,msgPreload:String)=viewModelScope.launch(Dispatchers.IO){

        //---crea la lista de videos final agregando los videos de la playlist
        val finalVideoList = mutableListOf<VideoObj>()
        playVideoList.forEach { video ->
            when(Util.checkKindLink(video.url)){
                KIND_URL_VIDEO -> finalVideoList.add(video)
                KIND_URL_PLAYLIST -> finalVideoList.addAll(appRepository.getPlayListFromUrl(Util.transUrlToServePlaylist(video.url,pref)))
            }
        }


        //--Ahora la lista para el exoplayer
        val audioList = ArrayList<AudioMetadata>()
        finalVideoList.forEach {
                val streamUrl = getOnlySoundUrl(pref,it.url)
                if(streamUrl!=null){
                    audioList.add(
                        AudioMetadata(
                            it.url,
                            it.title,
                            it.channel,
                            streamUrl,
                            it.thumbnailUrl,
                            it.duration,
                            it.extraUrlVideo
                        )
                    )
                }
        }

        //Envía la lista al Exoplayer
        showToastMessage.postValue(msgPreload)
        //---Tenemos el audio list con toda la metadata
        withContext(Dispatchers.Main){
            if(audioList.size>0) MediaHelper.cmdSendListToPlayer(queueCmd,0,audioList,musicServiceConnection)
        }
    }

    fun getOnlySoundUrl(pref: AppPreferences,videoUrl:String):String?{
        var salida : String? = null
        val result = OkGetFileUrl.traeWebString(Util.getYtbUrl(pref,videoUrl))
        result?.let { result ->
            if(result.length>20){
                val decodedUrl = Base58.decode(result).toString(Charsets.UTF_8)
                Log.v("msg","Decoded URL=${decodedUrl}")
                salida = decodedUrl
            }
        }
        return salida
    }

    fun convToMp3(playVideoList:List<VideoObj>,pref:AppPreferences,msgNotImplemented:String)= viewModelScope.launch(Dispatchers.IO){

        //---crea la lista de videos final agregando los videos de la playlist
        val finalVideoList = mutableListOf<VideoObj>()
        playVideoList.forEach { video ->
            when(Util.checkKindLink(video.url)){
                KIND_URL_VIDEO -> finalVideoList.add(video)
                KIND_URL_PLAYLIST -> finalVideoList.addAll(appRepository.getPlayListFromUrl(Util.transUrlToServePlaylist(video.url,pref)))
            }
        }

        //---Esto es para verificar que solo se convierten videos de youtube, no de otras plataformas todo se puede quitar cuando se implemente en el server
        var hasOther = false
        finalVideoList.forEach {
            if(!Util.isYoutubeUrl(it.url)) hasOther = true
        }
        if(!hasOther){

            finalVideoList.forEach {
                val mp3Conversion = PreloadFile(pref,it.duration,appRepository)
                launch {
                    mp3Conversion.loadMp3(it.url,it.extraUrlVideo)
                }

                mp3Conversion.checkProgress {
                    preloadProgress.postValue(it)
                }

                if(mp3Conversion.resultFileExists) download.postValue(it) //--- para hacer la solicitud de descarga
            }

        }else{
            showToastMessage.postValue(msgNotImplemented)
        }



    }


    fun isVideolistInitialized() = this::videoLista.isInitialized

}