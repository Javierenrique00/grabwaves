package com.mundocrativo.javier.solosonido.ui.player

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
import com.mundocrativo.javier.solosonido.model.AudioMetadata
import com.mundocrativo.javier.solosonido.model.InfoObj
import com.mundocrativo.javier.solosonido.model.ListaAudioMetadata
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.rep.AppRepository
import com.mundocrativo.javier.solosonido.service.EMPTY_PLAYBACK_STATE
import com.mundocrativo.javier.solosonido.service.NOTHING_PLAYING
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        MediaHelper.cmdSendDeleteQueueIndex(index,musicServiceConnection)
        videoLista.removeAt(index)
        notifyItemRemoved.postValue(index)
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


    companion object{
        const val MUSIC_ROOT = "MUSICROOT"
    }

}