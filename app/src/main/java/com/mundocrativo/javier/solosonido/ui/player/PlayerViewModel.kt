package com.mundocrativo.javier.solosonido.ui.player

import android.content.Context
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mundocrativo.javier.solosonido.library.MediaHelper
import com.mundocrativo.javier.solosonido.model.*
import com.mundocrativo.javier.solosonido.rep.AppRepository
import com.mundocrativo.javier.solosonido.service.EMPTY_PLAYBACK_STATE
import com.mundocrativo.javier.solosonido.service.NOTHING_PLAYING
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import kotlinx.coroutines.*
import org.koin.ext.scope

class PlayerViewModel(val appRepository: AppRepository) : ViewModel(){

    val musicServiceConnection = appRepository.musicServiceConnection
    val queueLiveData = musicServiceConnection.queueLiveData
    val nowPlaying = appRepository.musicServiceConnection.nowPlaying
    val playBackState = musicServiceConnection.playbackState
    val durationLiveData = MediaHelper.durationLiveData
    val videoLista = mutableListOf<VideoObj>()
    val notifyItemRemoved : MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val nowPlayingInfo : MutableLiveData<InfoObj> by lazy { MutableLiveData<InfoObj>() }
    var actualQueueIndex = 0 //-- el indice de la canción que se está tocando
    var initLoading = false
    val playVideoListPair = appRepository.playVideoListPair



    fun iniciaMusicService(){
        musicServiceConnection.subscribe(MUSIC_ROOT,subscriptionCallback)
    }


    override fun onCleared() {
        super.onCleared()
        Log.e("msg","Cerrando el PlayerViewmodel, liberando musicserverconnection")
        musicServiceConnection.unsubscribe(MUSIC_ROOT,subscriptionCallback)
    }

    val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback(){

        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            //Log.v("msg","On loadChildren parentId:$parentId children:${children.size}")
            children.forEach{
                //Log.v("msg","$it.description.mediaId")
            }
            //Log.v("msg","---------------------")
        }

    }


    fun getUrlInfo(position:Int,videoIn:VideoObj,ruta:String):Pair<Int,VideoObj>{
        val metaData = appRepository.getMetadataCache(videoIn.url) //--es posible encontrar en el cache esta metadata pero sin bitmap
        metaData?.let {
            return Pair(position,VideoObj(
                videoIn.id,
                videoIn.url,
                it.title,
                it.artist,
                it.thumbnailUrl,
                0,0,it.duration,videoIn.timestamp,9,9,true,false,false,0,null,false,"ºº"))
        }
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
                0,
                0,
                true,
                false,
                false,
                0,
                null,
                false,
                ""
            ))
        }
        return Pair(position,VideoObj(videoIn.id,videoIn.url,"---","---","",0,0,0,0,videoIn.kindMedia,0,true,true,false,0,null,false,""))
    }

    fun getInfoNowPlaying(ruta:String)=viewModelScope.launch(Dispatchers.IO) {
        //Log.v("msg","Buscando info en ruta:$ruta")
        nowPlayingInfo.postValue(appRepository.getInfoFromUrl(ruta))
    }


    fun playItemOnQueue(index:Int,positionMs:Long){
        MediaHelper.cmdSendPlayAt(index,positionMs,musicServiceConnection)
    }


    fun deleteQueueItem(index:Int,toPlayer:Boolean){
        if(actualQueueIndex>=index) {
            actualQueueIndex -=1
            if(actualQueueIndex<0) actualQueueIndex = 0
        }
        
        videoLista.removeAt(index)
        notifyItemRemoved.postValue(index)
        updateCurrentPlayList()
        if(toPlayer) MediaHelper.cmdSendDeleteQueueIndex(index,musicServiceConnection)
    }

    fun moveQueueItem(from:Int,to:Int){
        updateCurrentPlayList()
        MediaHelper.cmdSendMoveQueueItem(from,to,musicServiceConnection)
    }

    fun sendPlayDuration(){
        MediaHelper.cmdSendPlayDuration(musicServiceConnection)
    }

    fun sendCmdPausePlay(pausaPlay:Int){
        MediaHelper.cmdPausaPlay(pausaPlay,musicServiceConnection)
    }

    fun updateCurrentPlayList() = viewModelScope.launch(Dispatchers.IO){
        //---tiene que usar la videoLista para guardarla
        appRepository.updateDefaultQueue(convertVideoListToQueueFieldList(videoLista))
    }

    fun convertVideoListToQueueFieldList(inList:List<VideoObj>):List<QueueField>{
        val salida = mutableListOf<QueueField>()
        var cont = 0
        inList.forEach { salida.add(QueueField(0,0,it.url,cont++,0)) }
        return salida
    }


    fun LoadDefaultPlayListToPlayer(pref:AppPreferences)= viewModelScope.launch(Dispatchers.IO){
        initLoading = true
        val defaultQueue = appRepository.getDefaultQueue()
        val newList = mutableListOf<VideoObj>()
        defaultQueue.forEach {
            val video = VideoObj()
            video.url = it.itemId
            newList.add(video)
        }
        appRepository.openVideoListUrlLiveData.postValue(Pair(MediaHelper.QUEUE_NEW_NOSAVE,newList))
        withContext(Dispatchers.Main){
            delay(2000)
            //Log.v("msg","Traying to play item in queue:${pref.lastSongIndexPlayed}  time:${pref.lastTimePlayed}")
            playItemOnQueue(pref.lastSongIndexPlayed,pref.lastTimePlayed*1000L)
            initLoading = false
        }
    }

    companion object{
        const val MUSIC_ROOT = "MUSICROOT"
    }

}