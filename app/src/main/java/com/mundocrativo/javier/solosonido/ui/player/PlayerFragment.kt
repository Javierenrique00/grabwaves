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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
                    limitTimeTxt.text = ISO8601.TIME_LOCAL_COMPLETE.format(TimeSpan(p1*1000.0))  +" / "+ seekBarControl.maxStr
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                seekBarControl.isRunning = false
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                //---> tiene que enviar la nueva posicion
                viewModel.playItemOnQueue(viewModel.actualQueueIndex,timeSeekbar.progress*1000L)
                updateSeekBar()  //-- para que haya un ciclo cada segundo para actualizar el seekbar
            }

        })

        return view
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        pref = AppPreferences(context!!)

        //-- Se suscribe  ala conexion del music service
        viewModel.iniciaMusicService()
        viewModel.playBackState.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val isPlaying = it.state
            //Log.v("msg","Playback State Position=${it.position}  estate=$isPlaying")
            showPlayPauseButton()

        })

        viewModel.musicServiceConnection.nowPlaying.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            seekBarControl.isRunning = false
            val tama= it.description.mediaId!!.length
            //Log.v("msg","xxxx desc:${tama}")
            if(tama>0) {
                val mediaId = it.description.mediaId
                val titulo = it.description.title
                val metadataString = it.getString("METADATA_KEY_TITLE")
                //Log.v("msg","Now playing in fragment:${mediaId} | titulo=$titulo  metadataString=$metadataString trackNumber=${it.trackNumber}")
                viewModel.getInfoNowPlaying(transUrlToServInfo(mediaId!!))
                viewModel.sendPlayDuration() //--- lanza el comando de la duracion
                updateSeekBar()  //-- para que haya un ciclo cada segundo para actualizar el seekbar
            }
        })

        viewModel.queueLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer { queueList ->
            //Log.v("msg","Cargando la cola del player para mostrar Fragmento ${queueList.size}")
            //--- hay que hacer la conversion a VideoObj
            viewModel.videoLista.clear()
            queueList.forEach {queueItem ->
                viewModel.videoLista.add(MediaHelper.convMediaItemToVideoObj(queueItem,context!!))
                //Log.v("msg","Conversion MediaId=->${queueItem.description.mediaId} selected=")
            }
            videoPlayerDataAdapter.submitList(viewModel.videoLista)
            videoPlayerDataAdapter.notifyDataSetChanged()
        })

        viewModel.nowPlaying.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            //Log.v("msg","Now Playing de prueba ${it.description.title}")
        })

        viewModel.durationLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            //Log.v("msg","Exoplayer--> Position:${it.first} ContentDuration:${it.second} QueueIndex:${it.third}")
            seekBarControl.setProgress(it.first,it.second)
            timeSeekbar.max = (it.second/1000L).toInt()
             if((it.second>0) and (it.second<86400000)){
                 seekBarControl.maxStr = ISO8601.TIME_LOCAL_COMPLETE.format(TimeSpan(it.second*1.0))
                try {
                    //-- probablemente esta el cambio del track entonces hay que desactivar mostrar el track antiguo y mostrar elnuevo en la cola
                    val actual = viewModel.videoLista[viewModel.actualQueueIndex]
                    actual.esPlaying = false
                    itemChangeApi.genera(Pair(viewModel.actualQueueIndex,actual)) //--> quita el anterior
                    viewModel.actualQueueIndex = it.third
                    val cambiado = viewModel.videoLista[viewModel.actualQueueIndex]
                    cambiado.esPlaying = true
                    itemChangeApi.genera(Pair(viewModel.actualQueueIndex,cambiado)) //--> pone el nuevo
                }catch (e:Exception){
                    Log.e("msg","--Error actualizando actual song${e.message}")
                }

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


        imageLoader = Coil.imageLoader(context!!)

        //----Inicia el flow
        setupRecyclerFlow()

        setupVideoPlayerRecyclerAdapter()
    }

    override fun onResume() {
        super.onResume()
        //--- para actualizar el contenido apenas se carga
        videoPlayerDataAdapter.notifyDataSetChanged()

        viewModel.sendPlayDuration()


    }

    override fun onDestroyView() {
        super.onDestroyView()

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
        ).buffer(Channel.UNLIMITED).map { viewModel.getUrlInfo(it,transUrlToServInfo(it.url)) }

        //--- Se quita el GLobalScope
        lifecycleScope.launch(Dispatchers.IO) {
            flujoVideo.collect{
                if(it!=null) {
                    //Log.v("msg","Llegó url: ${it.url} titulo:${it.title} selected:${it.esSelected}")
                    itemChangeApi.genera(Pair(it.itemPosition,it))

                    //---para pedir bajar una imagen
                    val request = ImageRequest.Builder(context!!)
                        .data(it.thumbnailUrl)
                        .target { drawable ->

                            val item = it
                            item.esUrlReady = true
                            item.thumbnailImg = drawable
                            itemChangeApi.genera(Pair(it.itemPosition,item))
                            //Log.v("msg","Llego thumbnail:${it.thumbnailUrl}")
                        }
                        .build()
                    val disposable = imageLoader.enqueue(request)
                }

            }
        }

        //---inicia los eventos del flow de cambio de item
        itemChangeApi = ItemChangeApi()
        val flujoItem = flowFromItem(itemChangeApi).buffer(Channel.UNLIMITED)

        //--- Se quita el GLobalScope
        lifecycleScope.launch(Dispatchers.Main) {
            flujoItem.collect{

                //Log.v("msg","Item cambiado pos=${it.first} videoTitle=${it.second.title} selected=${it.second.esSelected}")
                viewModel.videoLista[it.first].title = it.second.title
                viewModel.videoLista[it.first].channel = it.second.channel
                viewModel.videoLista[it.first].thumbnailUrl = it.second.thumbnailUrl
                viewModel.videoLista[it.first].esInfoReady = it.second.esInfoReady
                viewModel.videoLista[it.first].esUrlReady = it.second.esUrlReady
                viewModel.videoLista[it.first].thumbnailImg = it.second.thumbnailImg
                viewModel.videoLista[it.first].esSelected = it.second.esSelected
                viewModel.videoLista[it.first].esPlaying = it.second.esPlaying
                viewModel.videoLista[it.first].duration = it.second.duration
                videoPlayerDataAdapter.notifyItemChanged(it.first,it.second)

            }
        }

        //---inicia los eventos del flow del move
            moveApi = MoveApi()
            val actPair = flowFromMove(moveApi)


        lifecycleScope.launch(Dispatchers.Main) {
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
                    delay(1000)
                    if((DateTime.nowUnixLong() - recuerdaPair.timeDataAnterior)>1000){
                        //---
                        val rec = recuerdaPair.datoAnterior
                        //Log.v("msg","#### Final ####>>Anterior From:${rec?.first}  to:From:${rec?.second}")
                        if(rec!=null) viewModel.moveQueueItem(rec.first,rec.second)  //--- da la orden de mover los del player
                        recuerdaPair.clearData()
                    }
                }


            }
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
                    MainScope().launch {
                        delay(100)
                        itemSelected.esSelected = false
                        itemChangeApi.genera(Pair(it.position,itemSelected))
                    }

                }
                is VideoPlayerListEvent.OnItemGetInfo ->{
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


    fun transUrlToServInfo(url:String):String{
        val videoBase64 = Util.convStringToBase64(url)
        val ruta = pref.server + "/info/?link=" +videoBase64
        return ruta
    }

    fun updateSeekBar(){
        lifecycleScope.launch {
            seekBarControl.programUpdate { cuenta ->
                if(viewModel.playBackState.value!!.state== PLAYBACK_STATE_PLAY){
                    val prog = seekBarControl.getSegProgress()
                    timeSeekbar.progress = prog
                    limitTimeTxt.text =  ISO8601.TIME_LOCAL_COMPLETE.format(TimeSpan(prog*1000.0)) +" / "+ seekBarControl.maxStr
                    if ((cuenta % 10) == 0) {
                        viewModel.sendPlayDuration()
                    }
                }
            }
        }
    }

    fun showPlayPauseButton(){
        when(viewModel.playBackState.value!!.state){
            PLAYBACK_STATE_PAUSE -> pauseBt.setImageResource(R.drawable.exo_controls_play)
            PLAYBACK_STATE_PLAY -> pauseBt.setImageResource(R.drawable.exo_controls_pause)
            else -> pauseBt.setImageResource(R.drawable.exo_controls_play)
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