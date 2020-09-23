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



    fun iniciaMusicService(){
        musicServiceConnection.subscribe(MUSIC_ROOT,subscriptionCallback)
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

    fun getInfoNowPlaying(ruta:String)=viewModelScope.launch(Dispatchers.IO) {
        //Log.v("msg","Buscando info en ruta:$ruta")
        nowPlayingInfo.postValue(appRepository.getInfoFromUrl(ruta))
    }


    fun playItemOnQueue(index:Int,positionMs:Long){
        MediaHelper.cmdSendPlayAt(index,positionMs,musicServiceConnection)
    }


    fun deleteQueueItem(index:Int){
        if(actualQueueIndex>=index) {
            actualQueueIndex -=1
            if(actualQueueIndex<0) actualQueueIndex = 0
        }
        
        videoLista.removeAt(index)
        notifyItemRemoved.postValue(index)
        MediaHelper.cmdSendDeleteQueueIndex(index,musicServiceConnection)
    }

    fun moveQueueItem(from:Int,to:Int){
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

    fun LoadDefaultPlayListToPlayer(context: Context,pref: AppPreferences) = viewModelScope.launch(Dispatchers.IO){
        initLoading = true
        val defaultQueue = appRepository.getDefaultQueue()
        defaultQueue.forEach {
            Log.v("msg","${it.itemId} orden=${it.order}")
                val job = launchPlayer(
                    MediaHelper.QUEUE_ADD,
                    Util.createUrlConnectionStringPlay(pref.server ?: "", it.itemId, pref.hQ),
                    Util.transUrlToServInfo(it.itemId, pref),
                    it.itemId,
                    context)
            job.join()
        }
        withContext(Dispatchers.Main){
            delay(1000)
            playItemOnQueue(pref.lastSongIndexPlayed,pref.lastTimePlayed*1000L)
        }
        initLoading = false

    }



    //--- Commando que adiciona directamente a la cola
    fun launchPlayer(queueCmd:Int,mediaUrl: String,infoUrl:String,originalUrl:String,context: Context)= viewModelScope.launch(Dispatchers.IO){
        //--- tiene que traer la metadata
        val info = appRepository.getInfoFromUrl(infoUrl)
        info?.let {
            val audioMetadata = AudioMetadata(
                originalUrl,
                it.title,
                it.channel,
                mediaUrl,
                it.thumbnailUrl,
                null
            )
            withContext(Dispatchers.Main) {
                MediaHelper.cmdSendSongWithMetadataToPlayer(queueCmd,audioMetadata, musicServiceConnection)
            }
        }
    }


    companion object{
        const val MUSIC_ROOT = "MUSICROOT"
    }

}