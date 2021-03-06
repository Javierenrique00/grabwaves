package com.mundocrativo.javier.solosonido.ui.player

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import coil.Coil
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.library.MediaHelper
import com.mundocrativo.javier.solosonido.model.InfoObj
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.ui.historia.*
import com.mundocrativo.javier.solosonido.ui.main.MainViewModel
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import com.mundocrativo.javier.solosonido.util.toMediaQueueItem
import com.mundocrativo.javier.solosonido.util.trackNumber
import com.soywiz.klock.DateTime
import com.soywiz.klock.ISO8601
import com.soywiz.klock.TimeSpan
import kotlinx.android.synthetic.main.historia_fragment.*
import kotlinx.android.synthetic.main.player_fragment.*
import kotlinx.android.synthetic.main.player_fragment.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.lang.Exception
import java.util.*

class PlayerFragment : Fragment() {

    //private val viewModel by sharedViewModel<PlayerViewModel>()
    private val viewModel : PlayerViewModel by viewModel()

    private lateinit var videoPlayerDataAdapter: VideoPlayerDataAdapter
    private lateinit var videoPairApi : VideoPairApi
    private lateinit var moveApi: MoveApi
    private lateinit var pref : AppPreferences
    private lateinit var imageLoader : ImageLoader
    val recuerdaPair = RecuerdaPair()
    val seekBarControl = SeekBarControl()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.player_fragment,container,false)

        view.pauseBt.setOnClickListener {
            when(viewModel.playBackState.value!!.state){
                PLAYBACK_STATE_PAUSE -> {
                    viewModel.sendCmdPausePlay(PLAYBACK_STATE_PLAY)
                    viewModel.sendPlayDuration()
                }
                PLAYBACK_STATE_PLAY-> viewModel.sendCmdPausePlay(PLAYBACK_STATE_PAUSE)
            }
        }


        view.timeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(!seekBarControl.isRunning){
                    limitTimeTxt.text = Util.shortHour(ISO8601.TIME_LOCAL_COMPLETE.format(TimeSpan(p1*1000.0)))  +" / "+ seekBarControl.maxStr
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                seekBarControl.isRunning = false
                seekBarControl.valid = false
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                //---> tiene que enviar la nueva posicion
                viewModel.playItemOnQueue(viewModel.actualQueueIndex,timeSeekbar.progress*1000L)
            }

        })

        return view
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        pref = AppPreferences(context!!)

        //-- Se suscribe  ala conexion del music service
        viewModel.iniciaMusicService()

        //----Inicia el flow
        setupRecyclerFlow()

        setupVideoPlayerRecyclerAdapter()
        imageLoader = viewModel.imageLoader

        //--- este es la posición relativa a la cola que se trae
        viewModel.playBackState.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val isPlaying = it.state
            val actQueueItem  = it.activeQueueItemId
            //Log.v("msg","queue Playback State Position=${it.position}  estate=$isPlaying activeQueueItem=$actQueueItem")
            showPlayPauseButton()
        })

        //--- es el now playing. no se ve la posición en la cola
        viewModel.musicServiceConnection.nowPlaying.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            seekBarControl.isRunning = false
            val tama= it.description.mediaId!!.length
            //Log.v("msg","xxxx desc:${tama}")
            if(tama>0) {
                val mediaId = it.description.mediaId
                val titulo = it.description.title
                val url = it.description.mediaUri
                val metadataString = it.getString("METADATA_KEY_TITLE")
                Log.v("msg","Now playing in fragment:${mediaId} | titulo=$titulo | url=$url metadataString=$metadataString")
                viewModel.getInfoNowPlaying(Util.transUrlToServInfo(mediaId!!,pref))
                viewModel.sendPlayDuration() //--- lanza el comando de la duracion
                updateSeekBar()  //-- para que haya un ciclo cada segundo para actualizar el seekbar

                checkActualQueueIndexWithPlayer(mediaId)

            }
        })

        viewModel.durationLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            //Log.v("msg","queue Exoplayer--> Position:${it.first} ContentDuration:${it.second} QueueIndex:${it.third}")
            seekBarControl.setProgress(it.first,it.second)
            timeSeekbar.max = (it.second/1000L).toInt()
             if((it.second>0) and (it.second<86400000)){
                 seekBarControl.maxStr = Util.shortHour(ISO8601.TIME_LOCAL_COMPLETE.format(TimeSpan(it.second*1.0)))
                 savePlayingState(viewModel.actualQueueIndex,(it.first/1000L).toInt())
                 //if((viewModel.actualQueueIndex!=it.third) or (viewModel.actualQueueIndex==0)) updateVideoListaEsPlaying(it.third) //----todo vamos a probar si esto sirva para poner el item actual en play
                 if(viewModel.videoLista.size>it.third){
                     if(!viewModel.videoLista[it.third].esPlaying) updateVideoListaEsPlaying(it.third)
                 }else{
                     Log.e("msg","Error in videolista.size ${viewModel.videoLista.size} index to put:${it.third}")
                 }


             }else{
                 seekBarControl.maxStr = "00:00:00"
            }
        })

        //---cuando se quita un item de la lista
        viewModel.notifyItemRemoved.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            videoPlayerDataAdapter.notifyItemRemoved(it)
            val delta = viewModel.videoLista.size - it
            videoPlayerDataAdapter.notifyItemRangeChanged(it,delta)
        })

        //--- Observers del player-----------------
        viewModel.nowPlayingInfo.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            it?.let {
                //thumbnailPlayIv.load(it.thumbnailUrl)
                //Log.v("msg","thumbnailplay ${it.thumbnailUrl}")
                titleTxt.text = it.title
            }
        })

        //--- Esta es la info que carga en el recyclerView
        viewModel.updateVideoListAdapter.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val tama = viewModel.videoLista.size
            if(tama>0){
                it.forEachIndexed { index, videoObj ->
                    if(index<tama){
                        if(!videoObj.url.contentEquals(viewModel.videoLista[index].url)){
                            viewModel.videoLista[index] = videoObj
                            videoPlayerDataAdapter.notifyItemChanged(index,videoObj)
                        }
                    }else{
                        //-- tiene que agregar los que faltan
                        viewModel.videoLista.add(index,videoObj)
                        videoPlayerDataAdapter.notifyItemInserted(index)
                    }
                }
                //---tiene que borrar los items mayores
                for(x in it.size until tama){
                    viewModel.videoLista.removeAt(it.size)
                    videoPlayerDataAdapter.notifyItemRemoved(it.size)
                }
            }else{
                viewModel.videoLista.clear()
                viewModel.videoLista.addAll(it)
                videoPlayerDataAdapter.submitList(viewModel.videoLista)
                videoPlayerDataAdapter.notifyDataSetChanged()
            }

        })

        //---los datos de la conversión del server del server
        viewModel.converted.observe(viewLifecycleOwner, androidx.lifecycle.Observer { converted->
            //--- debe confrontar si hay algún item de la videolista para actualizarla
            //--reviza los convertidos
            viewModel.videoLista.forEachIndexed { index, vid->
                val servState = viewModel.servState(vid.urlMd5)
                if(servState!=vid.servState){
                    vid.servState = servState
                    videoPlayerDataAdapter.notifyItemChanged(index,vid)
                }
            }

        })

        viewModel.isLoading.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            viewModel.loadConvertedFiles(pref) //--- para pedir traer datos convertidos
        })

        viewModel.deleteIndexSong.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            Log.v("msg","Force delete from error index:$it")
            viewModel.deleteQueueItem(it,false)
        })

        viewModel.preloadProgress.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            progressPreload.progress = it.progress
            progressText.text = it.msg
        })

    }

    override fun onResume() {
        super.onResume()
        //--- para actualizar el contenido apenas se carga
        //Log.v("msg","--- Resume view")
        //Log.v("msg","videoListSize ${viewModel.videoLista.size}")
        if(viewModel.videoLista.size==0) viewModel.LoadDefaultPlayListToPlayer(pref) else {
            if(videoPlayerDataAdapter.currentList.size==0) {
                //-- No tengo la lista cargada en el Recyclerview, hay que cargarla
                //Log.v("msg","No tengo lista cargada en el recyclerview, tengo que cargarla")
                videoPlayerDataAdapter.submitList(viewModel.videoLista)
                videoPlayerDataAdapter.notifyDataSetChanged()
            }
        }

        viewModel.sendPlayDuration()

        //--- para tener el listado de los archivos ya bajados
        viewModel.loadConvertedFiles(pref)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.v("msg","----------------------------- Destroy view")
        //------------
        videoPairApi.acaba()
        moveApi.acaba()

        viewModel.appRepository.setPlayerIsOpen(false)
    }

    private fun setupRecyclerFlow(){
        //---inicia los eventos del flow del video
        videoPairApi = VideoPairApi()
        val flujoVideo = flowFromVideoPair(videoPairApi).buffer(Channel.UNLIMITED).map { viewModel.getUrlInfo(it.first,it.second,Util.transUrlToServInfo(it.second.url,pref),pref) }

        //--- Se quita el GLobalScope
        lifecycleScope.launch(Dispatchers.IO) {
            flujoVideo.collect{
                lifecycleScope.launch {
                    if (it != null) {
                        delay(TIME_FOR_PAINT_UPDATE)
                        //Log.v( "msg", "Llegó la info url: ${it.second.url} titulo:${it.second.title} for position:${it.second.itemPosition} urlMd5:${it.second.urlMd5}" )
                        //--para poner el que está sonando directamente en el es playing
                        //if (it.first == viewModel.playBackState.value!!.activeQueueItemId.toInt()) it.second.esPlaying = true todo esto nos e que hace por ahor alo quito para que no interrumpa
                        updateItem(it.first, it.second)

                        //---para pedir bajar una imagen
                        val request = ImageRequest.Builder(context!!)
                            .data(it.second.thumbnailUrl)
                            .target { drawable ->

                                val item = it
                                item.second.esUrlReady = true
                                item.second.thumbnailImg = drawable
                                updateItem(it.first, it.second)
                                //Log.v("msg","Llego thumbnail:${it.thumbnailUrl}")
                            }
                            .build()
                        val disposable = imageLoader.enqueue(request)
                    }
                }

            }
        }



        //---inicia los eventos del flow del move
            moveApi = MoveApi()
            val actPair = flowFromMove(moveApi)

        lifecycleScope.launch {
            actPair.collect {act->
                //Log.v("msg","--->Anterior From:${act.first}  to:From:${act.second}")
                val ant = recuerdaPair.datoAnterior
                if(ant!=null){
                    val tr = uneMoves(ant,act)
                    //Log.v("msg","--->Anterior From:${tr.first}  to:From:${tr.second}")
                    recuerdaPair.storeData(tr)
                }
                else{
                    //Log.v("msg","--->Move NULL")
                    recuerdaPair.storeData(act)
                }
                launch {
                    delay(TIME_FOR_MOVE_ITEMS_UPDATE)
                    if((DateTime.nowUnixLong() - recuerdaPair.timeDataAnterior)>TIME_FOR_MOVE_ITEMS_UPDATE){
                        //---
                        val rec = recuerdaPair.datoAnterior
                        //Log.v("msg","#### Final ####>>Anterior From:${rec?.first}  to:From:${rec?.second}")
                        if(rec!=null) {
                            val elPlaying = viewModel.videoLista[viewModel.actualQueueIndex] //--para saber el item
                            val elItem = viewModel.videoLista.removeAt(rec.first)
                            val elSecond = if(rec.first<rec.second) rec.second -1 else rec.second
                            viewModel.videoLista.add(elSecond,elItem)
                            //--debe actualizar el actualQueueindex
                            val playingIndex = viewModel.videoLista.indexOf(elPlaying)
                            updateVideoListaEsPlaying(playingIndex)
                            videoPlayerDataAdapter.moveItem(rec.first,rec.second)
                            videoPlayerDataAdapter.notifyItemChanged(playingIndex)  //---notifica el movimiento del item del play
                            viewModel.moveQueueItem(rec.first,rec.second)

                            //---para refrescar los datos intermedios
                            val delta =Math.abs(rec.first-rec.second)
                            var inicial = if(rec.first>rec.second) rec.second else rec.first
                            videoPlayerDataAdapter.notifyItemRangeChanged(inicial,delta)

                        }  //--- da la orden de mover los del player
                        recuerdaPair.clearData()
                    }
                }

            }
        }

    }

    fun updateItem(index:Int,video:VideoObj){
        //Log.v("msg","Item cambiado pos=${index} videoTitle=${video.title} selected=${video.esSelected}")
        if(index<viewModel.videoLista.size){
            viewModel.videoLista[index].title = video.title
            viewModel.videoLista[index].channel = video.channel
            viewModel.videoLista[index].thumbnailUrl = video.thumbnailUrl
            viewModel.videoLista[index].esInfoReady = video.esInfoReady
            viewModel.videoLista[index].esUrlReady = video.esUrlReady
            viewModel.videoLista[index].thumbnailImg = video.thumbnailImg
            viewModel.videoLista[index].esSelected = video.esSelected
            viewModel.videoLista[index].esPlaying = video.esPlaying
            viewModel.videoLista[index].duration = video.duration
            viewModel.videoLista[index].servState = video.servState
            viewModel.videoLista[index].urlMd5 = video.urlMd5
            videoPlayerDataAdapter.notifyItemChanged(index,video)
        }
    }


    fun uneMoves(antPair:Pair<Int,Int>?,actPair:Pair<Int,Int>):Pair<Int,Int>{
        if(antPair==null) return actPair
        return if(antPair.second==actPair.first) Pair(antPair.first,actPair.second) else antPair
    }

    private fun setupVideoPlayerRecyclerAdapter(){
        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        val draw = context!!.getDrawable(R.drawable.vertical_divider)
        itemDecoration.setDrawable(draw!!)
        queueRv.addItemDecoration(itemDecoration)

        playerItemTouchHelper.attachToRecyclerView(queueRv) //--para el touch swipe
        videoPlayerDataAdapter = VideoPlayerDataAdapter(context!!)
        queueRv.layoutManager =  LinearLayoutManager(context)
        queueRv.adapter = videoPlayerDataAdapter
        videoPlayerDataAdapter.event.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when(it){
                is VideoPlayerListEvent.OnItemClick ->{
                    //--- Para hacer play directamente sobre este item
                    //Log.v("msg","Hice click en el Player Item")
                    //viewModel.playItemOnQueue(it.position,0L)
                    viewModel.forcePreloadAndPlay(pref,it.position)

                    val itemSelected = it.item
                    itemSelected.esSelected = true
                    updateItem(it.position,itemSelected)
                    lifecycleScope.launch {
                        delay(100)
                        itemSelected.esSelected = false
                        updateItem(it.position,itemSelected)
                    }

                }
                is VideoPlayerListEvent.OnItemGetInfo ->{
                    //Log.v("msg","Asking for Info: ${it.position}")
                    videoPairApi.genera(Pair(it.position,it.item))
                }
                is VideoPlayerListEvent.OnStartDrag ->{
                    playerItemTouchHelper.startDrag(it.viewHolder)
                }
                is VideoPlayerListEvent.OnSwipeRight ->{
                    //---evento para borrar el item de la cola
                    viewModel.deleteQueueItem(it.position,true)
                }
                is VideoPlayerListEvent.OnMoveItem ->{
                    //Log.v("msg","move from:${it.from} to${it.to}")
                    moveApi.genera(Pair(it.from,it.to))
                    //viewModel.moveQueueItem(it.from,it.to)
                }
            }
        })

    }


    fun updateSeekBar(){

        lifecycleScope.launch {
            seekBarControl.programUpdate { cuenta ->
                val prog = seekBarControl.getSegProgress()
                prog?.let {prog ->
                    timeSeekbar.progress = prog
                    limitTimeTxt.text =  Util.shortHour(ISO8601.TIME_LOCAL_COMPLETE.format(TimeSpan(prog*1000.0))) +" / "+ seekBarControl.maxStr
                }
                if ((cuenta % 10) == 0) {
                    viewModel.sendPlayDuration()
                }
//                if ((cuenta % 30) == 0) {
//                    //---para traer  cada 30 segundos la lista
//                    Log.v("msg","Trae la lista-->")
//                    viewModel.loadConvertedFiles(pref)
//                }
            }
        }
    }

    fun savePlayingState(indexSong:Int,progress:Int){
        if(!viewModel.initLoading) {
            //Log.v("msg", "saving state: index:$indexSong Progress=$progress")
            pref.lastSongIndexPlayed = indexSong
            pref.lastTimePlayed = progress
        }
    }

    fun showPlayPauseButton(){
        when(viewModel.playBackState.value!!.state){
            PLAYBACK_STATE_PAUSE -> {
                pauseBt.setImageResource(R.drawable.exo_controls_play)
                seekBarControl.isRunning = false
            }
            PLAYBACK_STATE_PLAY -> {
                pauseBt.setImageResource(R.drawable.exo_controls_pause)
                updateSeekBar()
            }
            else -> pauseBt.setImageResource(R.drawable.exo_controls_play)
        }
    }

    fun updateVideoListaEsPlaying(activeQueueItem:Int){
        Log.v("msg","index setting Videolista is Playing Index =$activeQueueItem")
        if((viewModel.actualQueueIndex<viewModel.videoLista.size) and (viewModel.actualQueueIndex>-1)){
            if(activeQueueItem!=viewModel.actualQueueIndex) queueRv.smoothScrollToPosition(activeQueueItem) //-- es para salte a ver el item actual cada vez que hay cambio de canción
            checkListItemPlayingForClear()
            viewModel.actualQueueIndex = activeQueueItem
            val cambiado = viewModel.videoLista[viewModel.actualQueueIndex]
            cambiado.esPlaying = true
            updateItem(viewModel.actualQueueIndex,cambiado)
        }else{
            Log.e("msg","No puede actualizar actualQueueIndex,  actualQueueIndex(no update)=$activeQueueItem anterior(try actual):${viewModel.actualQueueIndex}  tamaño videoLista=${viewModel.videoLista.size}")
        }
    }

    fun checkListItemPlayingForClear(){
        viewModel.videoLista.forEachIndexed { index, videoObj ->
            if(videoObj.esPlaying){
                videoObj.esPlaying = false
                videoPlayerDataAdapter.notifyItemChanged(index)
            }
        }
    }

    fun checkActualQueueIndexWithPlayer(mediaId:String){
        if((viewModel.actualQueueIndex<viewModel.videoLista.size) and (viewModel.actualQueueIndex>-1)){
            //---vamos a chequear el url del nowplaying es igual al del indice del nowPlaying
            if(mediaId.contentEquals(viewModel.videoLista[viewModel.actualQueueIndex].url)){
                Log.v("msg","Index sincronyzed")
            }
            else{
                Log.e("msg","Index, ActualQueueIndex desynchronized Actualqueueindex:${viewModel.actualQueueIndex} videolista.size=${viewModel.videoLista.size}")
                //--- buscando el indice a las malas, debe seleccionar el mas cercano de toda la lista
                val indexList = mutableListOf<Int>()
                viewModel.videoLista.forEachIndexed { index, videoObj ->
                    if(videoObj.url.contentEquals(mediaId)){
                        indexList.add(index)
                    }
                }
                //--- lista de los indices que tienen ese mismo contenido
                var indexMin = 0
                var dif = Int.MAX_VALUE
                indexList.forEach {
                    val tempDif = Math.abs(it - viewModel.actualQueueIndex)
                    if(tempDif<dif) {
                        dif = tempDif
                        indexMin = it
                    }
                }
                Log.v("msg","Setting index el valor mas cercano es index=$indexMin - setting true")
                viewModel.actualQueueIndex = indexMin
                updateVideoListaEsPlaying(indexMin)
            }
        }else{
            Log.e("msg","index , Outofbounds Actualqueueindex:${viewModel.actualQueueIndex} videolista.size=${viewModel.videoLista.size}")
        }
    }


    class RecuerdaPair(){
        var timeDataAnterior:Long = Long.MAX_VALUE
        var datoAnterior:Pair<Int,Int>? = null
        fun storeData(data:Pair<Int,Int>){
            datoAnterior = data
            timeDataAnterior = DateTime.nowUnixLong()
        }
        fun clearData(){
            datoAnterior = null
            timeDataAnterior = Long.MAX_VALUE
        }
    }

}

const val PLAYBACK_STATE_PLAY = 3
const val PLAYBACK_STATE_PAUSE = 2
const val TIME_FOR_PAINT_UPDATE = 100L  //---ms
const val TIME_FOR_MOVE_ITEMS_UPDATE = 2000L   //---ms

const val SERV_STATE_DOWNLADED = 2
const val SERV_STATE_DOWNLOADING = 1
const val SERV_STATE_IDLE = 0
