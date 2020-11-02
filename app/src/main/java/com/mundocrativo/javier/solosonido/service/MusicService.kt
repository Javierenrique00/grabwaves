/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mundocrativo.javier.solosonido.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.media.MediaBrowserServiceCompat
import androidx.media.MediaBrowserServiceCompat.BrowserRoot.EXTRA_RECENT
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.library.MediaHelper
import com.mundocrativo.javier.solosonido.model.Converted
import com.mundocrativo.javier.solosonido.model.ListaAudioMetadata
import com.mundocrativo.javier.solosonido.ui.player.PLAYBACK_STATE_PAUSE
import com.mundocrativo.javier.solosonido.ui.player.PLAYBACK_STATE_PLAY
import com.mundocrativo.javier.solosonido.util.toMediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * This class is the entry point for browsing and playback commands from the APP's UI
 * and other apps that wish to play music via UAMP (for example, Android Auto or
 * the Google Assistant).
 *
 * Browsing begins with the method [MusicService.onGetRoot], and continues in
 * the callback [MusicService.onLoadChildren].ac
 *
 * For more information on implementing a MediaBrowserService,
 * visit [https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice.html](https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice.html).
 *
 * This class also handles playback for Cast sessions.
 * When a Cast session is active, playback commands are passed to a
 * [CastPlayer](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/ext/cast/CastPlayer.html),
 * otherwise they are passed to an ExoPlayer for local playback.
 */
open class MusicService : MediaBrowserServiceCompat() {

    private lateinit var notificationManager: UampNotificationManager
    // The current player will either be an ExoPlayer (for local playback) or a CastPlayer (for
    // remote playback through a Cast device).
    private lateinit var currentPlayer: Player

    private val serviceJob = SupervisorJob()

    protected lateinit var mediaSession: MediaSessionCompat
    protected lateinit var mediaSessionConnector: MediaSessionConnector

    private val uAmpAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private var isForegroundService = false
    private val playerListener = PlayerEventListener()

    private val dataSourceFactory: DefaultDataSourceFactory by lazy {
        DefaultDataSourceFactory(
            /* context= */ this,
            Util.getUserAgent(/* context= */ this, UAMP_USER_AGENT), /* listener= */
            null
        )
    }

//--- para tratar de buscar sobre los mp3s
    val extractorFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(false) //---todo se puso en false, pero no se ve ningun efecto notorio


    /**
     * Configure ExoPlayer to handle audio focus for us.
     * See [Player.AudioComponent.setAudioAttributes] for details.
     */
    private val exoPlayer: ExoPlayer by lazy {
        SimpleExoPlayer.Builder(this).setMediaSourceFactory(DefaultMediaSourceFactory(this,extractorFactory)).build().apply {
            setAudioAttributes(uAmpAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
            setWakeMode(C.WAKE_MODE_NETWORK)
        }
    }


    @ExperimentalCoroutinesApi
    override fun onCreate() {
        super.onCreate()

        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, 0)
            }

        // Create a new MediaSession.
        mediaSession = MediaSessionCompat(this, "MusicService")
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }

        sessionToken = mediaSession.sessionToken

        notificationManager = UampNotificationManager(this, mediaSession.sessionToken, PlayerNotificationListener())

        // ExoPlayer will manage the MediaSession for us.
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(UampPlaybackPreparer())
        mediaSessionConnector.setQueueNavigator(UampQueueNavigator(mediaSession))
        currentPlayer = exoPlayer
        mediaSessionConnector.setPlayer(currentPlayer)
        notificationManager.showNotificationForPlayer(currentPlayer)

    }

    /**
     * This is the code that causes UAMP to stop playing when swiping the activity away from
     * recents. The choice to do this is app specific. Some apps stop playback, while others allow
     * playback to continue and allow users to stop it with the notification.
     */
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)

        /**
         * By stopping playback, the player will transition to [Player.STATE_IDLE] triggering
         * [Player.EventListener.onPlayerStateChanged] to be called. This will cause the
         * notification to be hidden and trigger
         * [PlayerNotificationManager.NotificationListener.onNotificationCancelled] to be called.
         * The service will then remove itself as a foreground service, and will call
         * [stopSelf].
         */
        currentPlayer.stop(/* reset= */true)
    }

    override fun onDestroy() {
        mediaSession.run {
            isActive = false
            release()
        }

        // Cancel coroutines when the service is going away.
        serviceJob.cancel()

        // Free ExoPlayer resources.
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    /**
     * Returns the "root" media ID that the client should request to get the list of
     * [MediaItem]s to browse/play.
     */

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): MediaBrowserServiceCompat.BrowserRoot? {

        /*
         * By default, all known clients are permitted to search, but only tell unknown callers
         * about search if permitted by the [BrowseTree].
         */
        val isKnownCaller = true
        val rootExtras = Bundle().apply {
            putBoolean(
                MEDIA_SEARCH_SUPPORTED,
                isKnownCaller
            )
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        }
        return BrowserRoot(MUSIC_ROOT, rootExtras)
    }




    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.v("msg","--->ParentId=$parentId")

        result.sendResult(MediaHelper.videosMetadata)
    }


    private inner class UampQueueNavigator(
        mediaSession: MediaSessionCompat
    ) : TimelineQueueNavigator(mediaSession) {

        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            //Log.v("msg","Buscando media description WindowIndex =$windowIndex")
            val mediadesc = exoPlayer.getMediaItemAt(windowIndex).playbackProperties!!.tag as MediaBrowserCompat.MediaItem
            return mediadesc.description
        }

    }


    private inner class UampPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {

        /**
         * UAMP supports preparing (and playing) from search, as well as media ID, so those
         * capabilities are declared here.
         *
         * TODO: Add support for ACTION_PREPARE and ACTION_PLAY, which mean "prepare/play something".
         */
        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

        override fun onPrepare(playWhenReady: Boolean) {
            //Log.v("msg","--onPrepare inicial cargando media")
            val mediaId = MediaHelper.getCurrentMediaId()
            if(mediaId!=null) onPrepareFromMediaId(mediaId,playWhenReady,null)
        }

        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            //Log.v("msg","--preparando el medio to play mediaId=$mediaId  %%%%%%")
            val mediaItem =  MediaHelper.getCurrentMediaItem()
            mediaItem?.let {
                if(it.mediaId!!.contentEquals(mediaId)){
                    currentPlayer.playWhenReady = playWhenReady
                    currentPlayer.stop(/* reset= */ true)
                    val addMediaItem = MediaHelper.convMediaItemToExoplayer(it)
                    exoPlayer.addMediaItem(addMediaItem)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(0, 0) //--Play
                    MediaHelper.clearMediaItemMetadata()
                }
            }
        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {

        }

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

//        override fun onCommand(
//            player: Player,
//            controlDispatcher: ControlDispatcher,
//            command: String,
//            extras: Bundle?,
//            cb: ResultReceiver?
//        ) = false

        override fun onCommand(
            player: Player,
            controlDispatcher: ControlDispatcher,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ): Boolean {
            //Log.v("msg","LLegó un comando  (Comando) $command")
            when(command){
                MediaHelper.CMD_SEND_SONG_METADATA ->{
                    val queueCmd = MediaHelper.cmdRecSongWithMetadataToPlayer(extras!!)
                    if((exoPlayer.mediaItemCount==0) or (queueCmd==MediaHelper.QUEUE_NEW)){
                        onPrepare(true)
                    }else{
                        //-- agrega un Item
                        val exoItem = MediaHelper.convMediaItemToExoplayer(MediaHelper.getCurrentMediaItem()!!)
                        when(queueCmd){
                            MediaHelper.QUEUE_ADD -> exoPlayer.addMediaItem(exoItem)
                            MediaHelper.QUEUE_NEXT ->{
                                val nextIndex = exoPlayer.nextWindowIndex
                                if(nextIndex<0) exoPlayer.addMediaItem(exoItem) else exoPlayer.addMediaItem(exoPlayer.nextWindowIndex,exoItem)
                            }
                        }

                    }
                }
                MediaHelper.CMD_SEND_LIST ->{
                    MediaHelper.cmdRecListToPlayer(extras!!,exoPlayer)
                    notifyChildrenChanged(MUSIC_ROOT)
                }
                MediaHelper.CMD_SEND_DELETE_QUEUE_ITEM ->{
                    val index = MediaHelper.cmdRecDeleteQueueIndex(extras!!)
                    MediaHelper.videosMetadata.removeAt(index)
                    notifyChildrenChanged(MUSIC_ROOT)
                    exoPlayer.removeMediaItem(index)
                }
                MediaHelper.CMD_SEND_MOVE_QUEUE ->{
                    val pair = MediaHelper.cmdRecMoveQueueItem(extras!!)
                    //Log.v("msg","Mover: from${pair.first} to:${pair.second}")
                    val temp = MediaHelper.videosMetadata.removeAt(pair.first)
                    MediaHelper.videosMetadata.add(pair.second,temp)
                    notifyChildrenChanged(MUSIC_ROOT)
                    exoPlayer.moveMediaItem(pair.first,pair.second)
                }
                MediaHelper.CMD_PLAY_AT ->{
                    val pair = MediaHelper.cmdRecPlayAt(extras!!)
                    exoPlayer.seekTo(pair.first,pair.second)
                }
                MediaHelper.CMD_SEND_PLAY_DURATION ->{
                    MediaHelper.cmdRecPlayDuration(exoPlayer)
                }
                MediaHelper.CMD_PAUSEPLAY ->{
                    val pausaPlay = MediaHelper.cmdRecPausaPlay(extras!!)
                    if(pausaPlay == PLAYBACK_STATE_PLAY) exoPlayer.play()
                    if(pausaPlay == PLAYBACK_STATE_PAUSE) exoPlayer.pause()
                }

            }

            return false
        }
    }


    /**
     * Listen for notification events.
     */
    private inner class PlayerNotificationListener :
        PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            //Log.v("msg","Notification event llegó for Foreground service-----$notificationId  $notification")

            if (ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(
                    applicationContext,
                    Intent(applicationContext, this@MusicService.javaClass)
                )
                //Log.v("msg","Activating foreground service")
                startForeground(notificationId, notification)
                isForegroundService = true
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            //Log.v("msg","Stop foreground service")
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }


    /**
     * Listen for events from ExoPlayer.
     */
    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            //Log.v("msg","Player cambiando de estado $playbackState")
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    notificationManager.showNotificationForPlayer(currentPlayer)
                    if (playbackState == Player.STATE_READY) {

                        //Log.v("msg","Estado Ready/buffering")
                        // When playing/paused save the current media item in persistent
                        // storage so that playback can be resumed between device reboots.
                        // Search for "media resumption" for more information.

                        if (!playWhenReady) {
                            // If playback is paused we remove the foreground state which allows the
                            // notification to be dismissed. An alternative would be to provide a
                            // "close" button in the notification which stops playback and clears
                            // the notification.
                            stopForeground(false)
                        }
                    }
                }
                else -> {
                    //Log.v("msg","Estado Otro=$playbackState   ended=${Player.STATE_ENDED}  IDLE=${Player.STATE_IDLE}")
                    notificationManager.hideNotification()
                }
            }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            super.onIsLoadingChanged(isLoading)
            val bundle = Bundle()
            bundle.putBoolean(PLAYER_EVENT_ISLOADING_PARAM,isLoading)
            mediaSession.sendSessionEvent(PLAYER_EVENT_ISLOADING_CMD,bundle)

        }

        override fun onPlayerError(error: ExoPlaybackException) {
            Log.e("msg","Player error------------")
            var message = R.string.generic_error;
            when (error.type) {
                // If the data from MediaSource object could not be loaded the Exoplayer raises
                // a type_source error.
                // An error message is printed to UI via Toast message to inform the user.
                ExoPlaybackException.TYPE_SOURCE -> {
                    message = R.string.error_media_not_found;
                    Log.e(TAG, "TYPE_SOURCE: " + error.sourceException.message)
                    val itemIndex = exoPlayer.currentWindowIndex

                    MediaHelper.videosMetadata.removeAt(itemIndex)
                    //notifyChildrenChanged(MUSIC_ROOT)

                    //--- genera un evento para eliminar el item del player
                    val bundle = Bundle()
                    bundle.putInt(PLAYER_EVENT_DELETE_SONG_PARAM,itemIndex)
                    mediaSession.sendSessionEvent(PLAYER_EVENT_DELETE_SONG_CMD,bundle)

                    exoPlayer.removeMediaItem(itemIndex)
                    if(itemIndex!=0){
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }
                // If the error occurs in a render component, Exoplayer raises a type_remote error.
                ExoPlaybackException.TYPE_RENDERER -> {
                    Log.e(TAG, "TYPE_RENDERER: " + error.rendererException.message)
                }
                // If occurs an unexpected RuntimeException Exoplayer raises a type_unexpected error.
                ExoPlaybackException.TYPE_UNEXPECTED -> {
                    Log.e(TAG, "TYPE_UNEXPECTED: " + error.unexpectedException.message)
                }
                // Occurs when there is a OutOfMemory error.
                ExoPlaybackException.TYPE_OUT_OF_MEMORY -> {
                    Log.e(TAG, "TYPE_OUT_OF_MEMORY: " + error.outOfMemoryError.message)
                }
                // If the error occurs in a remote component, Exoplayer raises a type_remote error.
                ExoPlaybackException.TYPE_REMOTE -> {
                    Log.e(TAG, "TYPE_REMOTE: " + error.message)
                }
            }
            Log.e("msg","mensaje de error haciendo play =$message")

            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_LONG
            ).show()
        }


    }

    companion object{
        const val TAG = "msg"
        const val MUSIC_ROOT = "MUSICROOT"
        const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

        private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val CONTENT_STYLE_LIST = 1
        private const val CONTENT_STYLE_GRID = 2

        private const val UAMP_USER_AGENT = "uamp.next"

    }

}

const val PLAYER_EVENT_ISLOADING_CMD = "isloadingevent"
const val PLAYER_EVENT_ISLOADING_PARAM = "isloadingparam"

const val PLAYER_EVENT_DELETE_SONG_CMD = "deletesongevent"
const val PLAYER_EVENT_DELETE_SONG_PARAM = "deletesongparam"
