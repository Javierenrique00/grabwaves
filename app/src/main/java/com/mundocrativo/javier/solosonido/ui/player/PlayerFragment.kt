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
import java.lang.Exception
import java.util.*

class PlayerFragment : Fragment() {

    private val viewModel by sharedViewModel<PlayerViewModel>()

    private lateinit var videoPlayerDataAdapter: VideoPlayerDataAdapter
    private lateinit var videoInfoApi: VideoInfoApi
    private lateinit var itemChangeApi: ItemChangeApi
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
                    //limitTimeTxt.text = Util.calcDeltaTiempo(0, p1.toLong()) + "/" + seekBarControl.maxStr
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
                //updateSeekBar()  //-- para que haya un ciclo cada segundo para actualizar el seekbar
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
        imageLoader = Coil.imageLoader(context!!)

        viewModel.playBackState.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val isPlaying = it.state
            val actQueueItem  = it.activeQueueItemId
            Log.v("msg","queue Playback State Position=${it.position}  estate=$isPlaying activeQueueItem=$actQueueItem")
            showPlayPauseButton()
            updateVideoListaEsPlaying(actQueueItem.toInt())
        })

        viewModel.musicServiceConnection.nowPlaying.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            seekBarControl.isRunning = false
            val tama= it.description.mediaId!!.length
            //Log.v("msg","xxxx desc:${tama}")
            if(tama>0) {
                val mediaId = it.description.mediaId
                val titulo = it.description.title
                val metadataString = it.getString("METADATA_KEY_TITLE")
                Log.v("msg","queue Now playing in fragment:${mediaId} | titulo=$titulo  metadataString=$metadataString trackNumber=${it.trackNumber}") //--tracknumber no sirve para la queueIndex
                viewModel.getInfoNowPlaying(Util.transUrlToServInfo(mediaId!!,pref))
                viewModel.sendPlayDuration() //--- lanza el comando de la duracion
                updateSeekBar()  //-- para que haya un ciclo cada segundo para actualizar el seekbar
            }
        })

        viewModel.queueLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer { queueList ->
            //--- hay que hacer la conversion a VideoObj

            val tempList = mutableListOf<VideoObj>()
            queueList.forEach {queueItem ->
                val item = MediaHelper.convMediaItemToVideoObj(queueItem,context!!)
                tempList.add(item)
            }
            val iguales = checkTwoVideoListEqual(tempList,viewModel.videoLista)
            if(!iguales){
                viewModel.videoLista.clear()
                queueList.forEach {queueItem ->
                    viewModel.videoLista.add(MediaHelper.convMediaItemToVideoObj(queueItem,context!!))
                    //Log.v("msg","Cargando Conversion MediaId=->${queueItem.description.mediaId} ")
                    videoPlayerDataAdapter.submitList(viewModel.videoLista)
                    videoPlayerDataAdapter.notifyDataSetChanged()
                }
            }else{
                //---debe chequear que tenga cargada la lista en el adapter
                val size = videoPlayerDataAdapter.currentList.size
                //Log.v("msg","Chequeando si tiene cargado el adapter cargado size=$size")
                if(size==0){
                    videoPlayerDataAdapter.submitList(viewModel.videoLista)
                    videoPlayerDataAdapter.notifyDataSetChanged()
                }
            }
            viewModel.updateCurrentPlayList()

            Log.v("msg","Cargando la cola del player para mostrar Fragmento ${queueList.size}  iguales=$iguales")
        })

//-- se quitó porque no se usa
//        viewModel.nowPlaying.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
//            //Log.v("msg","Now Playing de prueba ${it.description.title}")
//        })

        viewModel.durationLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            Log.v("msg","queue Exoplayer--> Position:${it.first} ContentDuration:${it.second} QueueIndex:${it.third}")
            seekBarControl.setProgress(it.first,it.second)
            timeSeekbar.max = (it.second/1000L).toInt()
             if((it.second>0) and (it.second<86400000)){
                 seekBarControl.maxStr = Util.shortHour(ISO8601.TIME_LOCAL_COMPLETE.format(TimeSpan(it.second*1.0)))
                 savePlayingState(viewModel.actualQueueIndex,(it.first/1000L).toInt())
            }else{
                 seekBarControl.maxStr = "00:00:00"
            }
        })

        //---cuando se quita un item de la lista
        viewModel.notifyItemRemoved.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            videoPlayerDataAdapter.notifyItemRemoved(it)
        })

        //--- Observers del player-----------------
        viewModel.nowPlayingInfo.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            it?.let {
                //thumbnailPlayIv.load(it.thumbnailUrl)
                //Log.v("msg","thumbnailplay ${it.thumbnailUrl}")
                titleTxt.text = it.title
            }
        })


    }

    override fun onResume() {
        super.onResume()
        //--- para actualizar el contenido apenas se carga
        Log.v("msg","--- Resume view")
        Log.v("msg","videoListSize ${viewModel.videoLista.size}")
        if(viewModel.videoLista.size==0) viewModel.LoadDefaultPlayListToPlayer(context!!,pref)

        viewModel.sendPlayDuration()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        //Log.v("msg","---- Destroy view")
        //------------
        videoInfoApi.acaba()
        itemChangeApi.acaba()
        moveApi.acaba()
    }

    private fun setupRecyclerFlow(){
        //---inicia los eventos del flow del video
        videoInfoApi = VideoInfoApi()
        val flujoVideo = flowFromVideo(
            videoInfoApi
        ).buffer(Channel.UNLIMITED).map { viewModel.getUrlInfo(it,Util.transUrlToServInfo(it.url,pref)) }

        //--- Se quita el GLobalScope
        lifecycleScope.launch {
            flujoVideo.collect{
                lifecycleScope.launch {
                    if (it != null) {
                        delay(TIME_FOR_PAINT_UPDATE)
                        //Log.v( "msg", "Llegó la info url: ${it.url} titulo:${it.title} for position:${it.itemPosition}" )
                        //--para poner el que está sonando directamente en el es playing
                        if (it.itemPosition == viewModel.playBackState.value!!.activeQueueItemId.toInt()) it.esPlaying =
                            true
                        //itemChangeApi.genera(Pair(it.itemPosition,it))
                        updateItem(it.itemPosition, it)

                        //---para pedir bajar una imagen
                        val request = ImageRequest.Builder(context!!)
                            .data(it.thumbnailUrl)
                            .target { drawable ->

                                val item = it
                                item.esUrlReady = true
                                item.thumbnailImg = drawable
                                //itemChangeApi.genera(Pair(it.itemPosition,item))
                                updateItem(it.itemPosition, it)
                                //Log.v("msg","Llego thumbnail:${it.thumbnailUrl}")
                            }
                            .build()
                        val disposable = imageLoader.enqueue(request)
                    }
                }

            }
        }

        //---inicia los eventos del flow de cambio de item
        itemChangeApi = ItemChangeApi()
        val flujoItem = flowFromItem(itemChangeApi).buffer(Channel.UNLIMITED)

        //--- Se quita el GLobalScope
        lifecycleScope.launch(Dispatchers.Main) {
            flujoItem.collect{
                updateItem(it.first,it.second)
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
                    viewModel.playItemOnQueue(it.position,0L)
                    val itemSelected = it.item
                    itemSelected.esSelected = true
                    itemChangeApi.genera(Pair(it.position,itemSelected))
                    lifecycleScope.launch {
                        delay(100)
                        itemSelected.esSelected = false
                        itemChangeApi.genera(Pair(it.position,itemSelected))
                    }

                }
                is VideoPlayerListEvent.OnItemGetInfo ->{
                    //Log.v("msg","Asking for Info: ${it.position}")
                    val dataWithPosition = it.item
                    dataWithPosition.itemPosition = it.position
                    videoInfoApi.genera(dataWithPosition)
                }
                is VideoPlayerListEvent.OnStartDrag ->{
                    playerItemTouchHelper.startDrag(it.viewHolder)
                }
                is VideoPlayerListEvent.OnSwipeRight ->{
                    //---evento para borrar el item de la cola
                    viewModel.deleteQueueItem(it.position)
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
            }
        }
    }

    fun savePlayingState(indexSong:Int,progress:Int){
        if(!viewModel.initLoading) {
            Log.v("msg", "saving state: index:$indexSong Progress=$progress")
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
        if((viewModel.actualQueueIndex<viewModel.videoLista.size) and (viewModel.actualQueueIndex>-1)){
            //Log.v("msg","Updating esPlaying actualQueueIndex=$activeQueueItem anterior:${viewModel.actualQueueIndex}")
            val actual = viewModel.videoLista[viewModel.actualQueueIndex]
            actual.esPlaying = false
            itemChangeApi.genera(Pair(viewModel.actualQueueIndex,actual)) //--> quita el anterior
            viewModel.actualQueueIndex = activeQueueItem
            val cambiado = viewModel.videoLista[viewModel.actualQueueIndex]
            cambiado.esPlaying = true
            itemChangeApi.genera(Pair(viewModel.actualQueueIndex,cambiado))
        }else{
            Log.e("msg","No puede actualizar actualQueueIndex,  actualQueueIndex(no update)=$activeQueueItem anterior(try actual):${viewModel.actualQueueIndex}  tamaño videoLista=${viewModel.videoLista.size}")
        }
    }


    fun checkTwoVideoListEqual(list1:List<VideoObj>,list2:List<VideoObj>):Boolean{
        if(list1.size!=list2.size) return false
        list1.forEachIndexed { index, videoObj ->
            if(!videoObj.url.contentEquals(list2[index].url)) return false
        }
        return true
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