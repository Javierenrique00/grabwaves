package com.mundocrativo.javier.solosonido.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.session.MediaSession
import android.util.Log
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.mundocrativo.javier.solosonido.model.AudioMetadata
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.service.MusicServiceConnection
import com.mundocrativo.javier.solosonido.util.artist
import com.mundocrativo.javier.solosonido.util.mediaUri

object MediaHelper {

    //---- tiene una lista de todos los items del player
    val videosMetadata = mutableListOf<MediaBrowserCompat.MediaItem>()

    private var commandMediaItem : MediaBrowserCompat.MediaItem? = null
    private var commandMetadataItem : MediaMetadataCompat? = null
    val durationLiveData : MutableLiveData<Triple<Long,Long,Int>> by lazy {  MutableLiveData<Triple<Long,Long,Int>>() }


    //---https://developer.android.com/reference/kotlin/android/support/v4/media/MediaMetadataCompat.Builder
    private fun addMediaItemAndMetadata(mediaId:String,mediaUrl:String,title:String,artist:String,imgUrl:String,imgBitmap:Bitmap?){
        val desc = MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setMediaUri(Uri.parse(mediaUrl))
            .setTitle(title)
            .setIconUri(Uri.parse(imgUrl))
            .setDescription(artist)
            .build()
        commandMediaItem = MediaBrowserCompat.MediaItem(desc,MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)

        commandMetadataItem = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,mediaId)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,mediaUrl)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,artist)
//            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,imgBitmap)
//            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART,imgBitmap)
            .build()
    }

    //---
    private fun convFromAudiometadataToMediaItem(audio:AudioMetadata):MediaBrowserCompat.MediaItem? {
        val bundle = Bundle()
        bundle.putInt("DURATION",audio.duration)
        val desc = MediaDescriptionCompat.Builder()
            .setMediaId(audio.mediaId)
            .setMediaUri(Uri.parse(audio.url))
            .setTitle(audio.title)
            .setSubtitle(audio.artist)
            .setIconUri(Uri.parse(audio.thumbnailUrl))
            .setExtras(bundle)
            .build()

        return MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }


//----- Esta es la conversion de MediaItem a VideoObj,
    fun convMediaItemToVideoObj(mediaItem:MediaBrowserCompat.MediaItem):VideoObj{
        val bundle = mediaItem.description.extras
        val duration = bundle!!.getInt("DURATION",0)
        return VideoObj(
            0,
            mediaItem.description.mediaId?:"",
            mediaItem.description.title.toString(),
            mediaItem.description.subtitle.toString(),
            mediaItem.description.iconUri.toString(),
            0,
            0,
            duration,
            0,
            0,
            0,
            "",0,
            mediaItem.description.mediaId?:"",
            false,
            false,
            false,
            0,
            null,
            false,
            ""
        )
    }
//--- esta es la conversionde mediaItem del android al exoplayer
    fun convMediaItemToExoplayer(mediaItem: MediaBrowserCompat.MediaItem):MediaItem{

    val exoItem = MediaItem.Builder()
        .setUri(mediaItem.description.mediaUri)
        .setMediaId(mediaItem.mediaId)
        .setTag(mediaItem)  //--- la metadata se pone en el tag
        .build()

    return exoItem
    }


//----- comando para agregar la canción a la cola (Este es elcomando ADDQUEUE)

    fun cmdSendSongWithMetadataToPlayer(queueCmd:Int,metadata:AudioMetadata,msc:MusicServiceConnection){
        val bundle = Bundle()
        bundle.putParcelable(CMD_SEND_SONG_METADATA_PARAM,metadata)
        bundle.putInt(CMD_SEND_SONG_QUEUECMD_PARAM,queueCmd)
        msc.sendCommand(CMD_SEND_SONG_METADATA,bundle)
    }

    fun cmdRecSongWithMetadataToPlayer(bundle: Bundle):Int{
        val audioMetadata = bundle.getParcelable<AudioMetadata>(CMD_SEND_SONG_METADATA_PARAM)
        val queueCmd = bundle.getInt(CMD_SEND_SONG_QUEUECMD_PARAM)
        audioMetadata?.let {
            addMediaItemAndMetadata(it.mediaId,it.url,it.title,it.artist,it.thumbnailUrl,null)
        }
        return queueCmd
    }

//----- comando para enviar una lista completa
    fun cmdSendListToPlayer(queueCmd: Int,index:Int,metadataList:ArrayList<AudioMetadata>,msc:MusicServiceConnection){
        val bundle = Bundle()
        bundle.putInt(CMD_SEND_LIST_PARAM_COMMAND,queueCmd)
        bundle.putInt(CMD_SEND_LIST_PARAM_INDEX,index)
        bundle.putParcelableArrayList (CMD_SEND_LIST_PARAM_DATA,metadataList)
        msc.sendCommand(CMD_SEND_LIST,bundle)
    }

    fun cmdRecListToPlayer(bundle: Bundle,exoPlayer: ExoPlayer){
        val queueCmd = bundle.getInt(CMD_SEND_LIST_PARAM_COMMAND)
        val index = bundle.getInt(CMD_SEND_LIST_PARAM_INDEX)
        val metadataList = bundle.getParcelableArrayList<AudioMetadata>(CMD_SEND_LIST_PARAM_DATA)

        val exoItems = mutableListOf<MediaItem>()
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        metadataList!!.forEach {
            val mediaItem = convFromAudiometadataToMediaItem(it)
            val exoItem =    convMediaItemToExoplayer( mediaItem!! )
            mediaItems.add(mediaItem)
            exoItems.add(exoItem)
        }

        var changeCmd = queueCmd
        if(exoPlayer.mediaItemCount==0) changeCmd = QUEUE_NEW
        when(changeCmd){
            QUEUE_NEW ->{
                videosMetadata.clear()
                videosMetadata.addAll(mediaItems)

                exoPlayer.playWhenReady = true
                exoPlayer.stop(true)
                exoPlayer.addMediaItems(exoItems)
                exoPlayer.prepare()
                exoPlayer.seekTo(0,0)
            }
            QUEUE_ADD ->{
                videosMetadata.addAll(mediaItems)
                exoPlayer.addMediaItems(exoItems)
            }
            QUEUE_NEXT ->{
                val nextIndex = exoPlayer.currentWindowIndex + 1
                videosMetadata.addAll(nextIndex,mediaItems)
                //Log.v("msg","nextindex:$nextIndex")
                exoPlayer.addMediaItems(nextIndex,exoItems)
            }
        }
    }


//----- comando para borrar un Item de la cola en un indice determinado

    fun cmdSendDeleteQueueIndex(indice:Int,msc:MusicServiceConnection){
        val bundle = Bundle()
        bundle.putInt(CMD_SEND_DELETE_QUEUE_ITEM_PARAM,indice)
        msc.sendCommand(CMD_SEND_DELETE_QUEUE_ITEM,bundle)
    }

    fun cmdRecDeleteQueueIndex(bundle: Bundle):Int{
        return bundle.getInt(CMD_SEND_DELETE_QUEUE_ITEM_PARAM)
    }

//--- comando para mover un item de la cola desde un origen a un destino

    fun cmdSendMoveQueueItem(from:Int,to:Int,msc:MusicServiceConnection){
        val bundle = Bundle()
        bundle.putInt(CMD_SEND_MOVE_QUEUE_PARAM_FROM,from)
        bundle.putInt(CMD_SEND_MOVE_QUEUE_PARAM_TO,to)
        msc.sendCommand(CMD_SEND_MOVE_QUEUE,bundle)
    }

    fun cmdRecMoveQueueItem(bundle: Bundle):Pair<Int,Int>{
        val from = bundle.getInt(CMD_SEND_MOVE_QUEUE_PARAM_FROM)
        val to = bundle.getInt(CMD_SEND_MOVE_QUEUE_PARAM_TO)
        return Pair(from,to)
    }

//--- comando playItem en cola en ms

    fun cmdSendPlayAt(index:Int,ms:Long,msc:MusicServiceConnection){
        val bundle = Bundle()
        bundle.putInt(CMD_PLAY_PARAM_INDEX,index)
        bundle.putLong(CMD_PLAY_PARAM_MS,ms)
        msc.sendCommand(CMD_PLAY_AT,bundle)
    }

    fun cmdRecPlayAt(bundle: Bundle):Pair<Int,Long>{
        val index = bundle.getInt(CMD_PLAY_PARAM_INDEX)
        val ms = bundle.getLong(CMD_PLAY_PARAM_MS)
        return Pair(index,ms)
    }

//--- Comando para leer la duración de la cancion

    fun cmdSendPlayDuration(msc:MusicServiceConnection){
        val bundle = Bundle()
        msc.sendCommand(CMD_SEND_PLAY_DURATION,bundle)
    }

    fun cmdRecPlayDuration(exoPlayer: ExoPlayer){
            val position = exoPlayer.contentPosition
            val contentDuration = exoPlayer.contentDuration
            val index = exoPlayer.currentWindowIndex
        durationLiveData.postValue(Triple(position,contentDuration,index))
    }

//--- Comando para poner pausa / play

    fun cmdPausaPlay(pausaPlay:Int,msc:MusicServiceConnection){
        val bundle = Bundle()
        bundle.putInt(CMD_PAUSEPLAY_PARAM,pausaPlay)
        msc.sendCommand(CMD_PAUSEPLAY,bundle)
    }

    fun cmdRecPausaPlay(bundle: Bundle):Int{
        return bundle.getInt(CMD_PAUSEPLAY_PARAM)
    }


//----------------------------------------


    fun clearMediaItemMetadata(){
        commandMediaItem = null
        commandMetadataItem = null
    }

    fun getCurrentMediaId():String?{
        val devValue = commandMediaItem
        devValue?.let {
            return devValue.mediaId
        }
        return null
    }

    fun getCurrentMediaItem():MediaBrowserCompat.MediaItem?{
        return commandMediaItem
    }

    fun getCurrentMediaMetadata():MediaMetadataCompat?{
        return commandMetadataItem
    }

    //--- del comando enviar una song con la metadata
    const val CMD_SEND_SONG_METADATA = "cmd_send_song_metadata"
    const val CMD_SEND_SONG_METADATA_PARAM = "param_metadata"
    const val CMD_SEND_SONG_QUEUECMD_PARAM = "param_queuecmd"
    const val QUEUE_ADD = 0
    const val QUEUE_NEXT = 1
    const val QUEUE_NEW = 2
    const val QUEUE_NO_PLAY = 3
    const val QUEUE_NEW_NOSAVE = 4

    //--- commando para enviar toda una lista
    const val CMD_SEND_LIST = "cmd_send_list"
    const val CMD_SEND_LIST_PARAM_INDEX = "cmd_send_list_param_index"
    const val CMD_SEND_LIST_PARAM_COMMAND = "cmd_send_list_param_command"
    const val CMD_SEND_LIST_PARAM_DATA = "cmd_send_list_param_data"


    //-- comando para borrar de la cola un item
    const val CMD_SEND_DELETE_QUEUE_ITEM = "cmd_delete_queue"
    const val CMD_SEND_DELETE_QUEUE_ITEM_PARAM = "param_delete_index"

    //--- comando para mover un item de la cola desde un origen a un destino
    const val CMD_SEND_MOVE_QUEUE = "cmd_move_queue"
    const val CMD_SEND_MOVE_QUEUE_PARAM_FROM = "cmd_move_param_from"
    const val CMD_SEND_MOVE_QUEUE_PARAM_TO = "cmd_move_param_to"

    //--- comado para hacer play de un item en la cola
    const val CMD_PLAY_AT = "cmd_play_at"
    const val CMD_PLAY_PARAM_INDEX = "cmd_play_param_index"
    const val CMD_PLAY_PARAM_MS = "cmd_play_param_ms"

    //--- comando para leer el avance de la cancion
    const val CMD_SEND_PLAY_DURATION = "cmd_play_duration"

    //--- comando para poner pause/play
    const val CMD_PAUSEPLAY = "cmd_play_pause"
    const val CMD_PAUSEPLAY_PARAM = "cmd_play_pause_PARAM" //-0 pausa 1-play

}