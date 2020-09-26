package com.mundocrativo.javier.solosonido.ui.historia

import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import com.mundocrativo.javier.solosonido.BuildConfig
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.library.MediaHelper
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.ui.config.ConfigActivity
import com.mundocrativo.javier.solosonido.ui.main.*
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime
import kotlinx.android.synthetic.main.historia_fragment.*
import kotlinx.android.synthetic.main.historia_fragment.view.*
import kotlinx.android.synthetic.main.main_fragment.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class HistoriaFragment : Fragment() {

    companion object {
        fun newInstance() =
            HistoriaFragment()
    }

    private val viewModel by sharedViewModel<MainViewModel>()
    private lateinit var pref : AppPreferences
    private lateinit var videoListDataAdapter: VideoListDataAdapter
    private lateinit var videoInfoApi: VideoInfoApi
    private lateinit var imageLoader : ImageLoader


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.historia_fragment, container, false)

        view.selectCb.setOnCheckedChangeListener { compoundButton, b ->
            showButtons()
        }

        view.playIv.setOnClickListener {
            dialogPlayMultiple()
        }

        view.deleteIv.setOnClickListener {
            dialogDeleteMultiple()
        }

        view.pasteBt.setOnClickListener {
            loadPasteText()
            Util.clickAnimator(pasteBt)
        }

        return view
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        pref = AppPreferences(context!!)
        imageLoader = Coil.imageLoader(context!!)

        //--- videoRecyclerView
        setupVideoListRecyclerAdapter()
        viewModel.videoListLiveData.observe(viewLifecycleOwner, Observer {
            videoListDataAdapter.submitList(it)
            videoListDataAdapter.notifyDataSetChanged()
        })

        //---cuando se quita un item de la lista
        viewModel.notifyItemRemoved.observe(viewLifecycleOwner, Observer {
            videoListDataAdapter.notifyItemRemoved(it)
            val delta = viewModel.videoLista.size - it  //--cuando se quita un elemento de la lista hay que notificar también el rango que se mueve hacia arriba
            videoListDataAdapter.notifyItemRangeChanged(it,delta)
        })

        viewModel.openVideoUrlLiveData.observe(viewLifecycleOwner, Observer {
            Log.v("msg","OPEN video ${it.second}")
            if(!((viewModel.lastOpenUrl.first==it.first) and (viewModel.lastOpenUrl.second.contentEquals(it.second)))){
                insertItemAtTopList(it)
                if(it.first!=MediaHelper.QUEUE_NO_PLAY){
                    val urlToPlay = Util.createUrlConnectionStringPlay(pref.server!!,it.second,pref.hQ)
                    viewModel.launchPlayer(it.first,urlToPlay,Util.transUrlToServInfo(it.second,pref),it.second,context!!)
                }
            }
        })

        viewModel.openVideoListUrlLiveData.observe(viewLifecycleOwner, Observer {
            Log.v("msg","Send video List to player")
            if(!detectVideoListEqual(it.second,viewModel.lastListOpenUrl)){
                it.second.forEach { video -> insertItemAtTopList(Pair(it.first,video.url)) }
                viewModel.launchPlayerMultiple(it.first,it.second,pref,context!!)
            }
        })

        //--- debe cargar los videos que están en la base de datos
        viewModel.loadVideosFromDb()
        setupRecyclerFlow()
    }

    override fun onResume() {
        super.onResume()
        //----Inicia el flow
        showButtons()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoInfoApi.acaba()
    }

    //---https://stackoverflow.com/questions/19177231/android-copy-paste-from-clipboard-manager
    fun loadPasteText(){
        var enlaceLetras = ""
        var clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if(!(clipboard.hasPrimaryClip())){

        } else if(!(clipboard.getPrimaryClipDescription()!!.hasMimeType(MIMETYPE_TEXT_PLAIN))){

        } else {
            val item = clipboard.getPrimaryClip()!!.getItemAt(0)
            enlaceLetras = item.text.toString()
        }
        if(!enlaceLetras.isEmpty()) {
            //viewModel.insertNewVideo(enlaceLetras)
            insertItemAtTopList(Pair(MediaHelper.QUEUE_NO_PLAY,enlaceLetras))
        }
    }

    private fun setupRecyclerFlow(){
        //---inicia los eventos del flow del video
        videoInfoApi = VideoInfoApi()
        val flujoVideo = flowFromVideo(
            videoInfoApi
        ).buffer(Channel.UNLIMITED).map { viewModel.getUrlInfo(it,Util.transUrlToServInfo(it.url,pref)) }

        //--- Se quita el GLobalScope
        lifecycleScope.launch(Dispatchers.IO) {
            flujoVideo.collect{
                Log.v("msg","Collecting observer de flujoVide ${it!!.title}")
                if(it!=null) {
                    Log.v("msg","llegó url: ${it.url} titulo:${it.title} duration:${it.duration}")
                    withContext(Dispatchers.Main){ updateItem(it.itemPosition,it) }

                    //---para pedir bajar una imagen
                    val request = ImageRequest.Builder(context!!)
                        .data(it.thumbnailUrl)
                        .target { drawable ->

                            val item = it
                            item.esUrlReady = true
                            item.thumbnailImg = drawable
                            updateItem(it.itemPosition,it)
                        }
                        .build()
                    val disposable = imageLoader.enqueue(request)
                }

            }
        }
    }


    private fun setupVideoListRecyclerAdapter(){


        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        val draw = context!!.getDrawable(R.drawable.vertical_divider)
        itemDecoration.setDrawable(draw!!)
        videoRv.addItemDecoration(itemDecoration)

        itemTouchHelper.attachToRecyclerView(videoRv) //--para el touch swipe
        videoListDataAdapter = VideoListDataAdapter(context!!)
        videoRv.layoutManager = LinearLayoutManager(context)
        videoRv.adapter = videoListDataAdapter
        videoListDataAdapter.event.observe(viewLifecycleOwner, Observer {
            when(it){
                is VideoListEvent.OnItemClick ->{
                    if(selectCb.isChecked){
                        //--- selección multiple
                        val itemSelected = it.item
                        itemSelected.esSelected = !it.item.esSelected
                        //itemChangeApi.genera(Pair(it.position,itemSelected))
                        updateItem(it.position,itemSelected)
                    }
                    else{
                        //--- play directo
                        val itemSelected = it.item
                        itemSelected.esSelected = true
                        //itemChangeApi.genera(Pair(it.position,itemSelected))
                        updateItem(it.position,itemSelected)
                        val urlToPlay = Util.createUrlConnectionStringPlay(pref.server!!,it.item.url,pref.hQ)
                        dialogItemCola(urlToPlay,Util.transUrlToServInfo(it.item.url,pref),it.item.url)//-- es el dialogo para el play a la cola
                        MainScope().launch {
                            delay(100)
                            itemSelected.esSelected = false
                            //itemChangeApi.genera(Pair(it.position,itemSelected))
                            updateItem(it.position,itemSelected)
                        }
                    }
                }
                is VideoListEvent.OnItemGetInfo ->{
                    Log.v("msg","Generando Flow ${it.item.url}")
                    val dataWithPosition = it.item
                    dataWithPosition.itemPosition = it.position
                    videoInfoApi.genera(dataWithPosition)
                }
                is VideoListEvent.OnStartDrag ->{
                    itemTouchHelper.startDrag(it.viewHolder)
                }
                is VideoListEvent.OnSwipeRight ->{
                    Log.v("msg","Recibe evento para borrar por swipe---------==>${it.index}")
                    val urlInfo = Util.transUrlToServInfo(viewModel.videoLista[it.index].url,pref)
                    viewModel.deleteVideoListElement(it.index,urlInfo)
                }
            }
        })
    }

    fun insertItemAtTopList(pair : Pair<Int,String>){
        lifecycleScope.launch {
            val job = lifecycleScope.launch(Dispatchers.IO) {
                val video = VideoObj()
                video.url = pair.second
                video.timestamp = DateTime.now().unixMillisLong
                val id = viewModel.insertNewVideo(video)
                video.id = id
                video.itemPosition = 0
                Log.v("msg","insertando item: $video")
                viewModel.lastOpenUrl = pair
                if(viewModel.isVideolistInitialized()){
                    viewModel.videoLista.add(0, video)
                }else{
                    viewModel.videoLista = mutableListOf()
                    viewModel.videoLista.add(0, video)
                }
            }
            job.join()
            videoListDataAdapter.notifyItemInserted(0)
            val delta = viewModel.videoLista.size -1
            if(viewModel.videoLista.size>1) videoListDataAdapter.notifyItemRangeChanged(1,delta)
        }
    }

    fun showButtons(){
        if(selectCb.isChecked){
            playIv.visibility = View.VISIBLE
            deleteIv.visibility = View.VISIBLE
        }else{
            playIv.visibility = View.GONE
            deleteIv.visibility = View.GONE
            deselectList()
        }
    }

    fun updateItem(index:Int,item:VideoObj){
                Log.v("msg","---Item cambiado pos=${index} videoTitle=${item.title} selected=${item.esSelected}")
                viewModel.videoLista[index].title = item.title
                viewModel.videoLista[index].channel = item.channel
                viewModel.videoLista[index].thumbnailUrl = item.thumbnailUrl
                viewModel.videoLista[index].esInfoReady = item.esInfoReady
                viewModel.videoLista[index].esUrlReady = item.esUrlReady
                viewModel.videoLista[index].thumbnailImg = item.thumbnailImg
                viewModel.videoLista[index].esSelected = item.esSelected
                viewModel.videoLista[index].duration= item.duration
                videoListDataAdapter.notifyItemChanged(index,item)
    }


    fun dialogItemCola(mediaUrl: String,infoUrl:String,originalUrl:String){
        val builder = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.titlequeue))
            .setMessage(getString(R.string.messageQueue))
            .setPositiveButton(getString(R.string.queueAdd)) { p0, p1 -> viewModel.launchPlayer(MediaHelper.QUEUE_ADD,mediaUrl,infoUrl,originalUrl,context!!) }
            .setNeutralButton(getString(R.string.queueNext)) { p0, p1 -> viewModel.launchPlayer(MediaHelper.QUEUE_NEXT,mediaUrl,infoUrl,originalUrl,context!!) }
            .setNegativeButton(getString(R.string.queueNew)) { p0, p1 -> viewModel.launchPlayer(MediaHelper.QUEUE_NEW,mediaUrl,infoUrl,originalUrl,context!!) }

        val dialog =builder.create()
        dialog.show()
    }

    fun dialogDeleteMultiple(){
        val builder = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.DeleteTitle))
            .setMessage(getString(R.string.deleteMessage))
            .setPositiveButton(getString(R.string.Okdelete)) { p0, p1 ->
                viewModel.deleteVideoListSelected(pref)
            }
            .setNegativeButton(getString(R.string.cancelDelete)) { p0, p1 ->  }

        val dialog = builder.create()
        dialog.show()
    }

    fun dialogPlayMultiple(){
        val videoListToPlay = viewModel.videoLista.filter { it.esSelected }
        val builder = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.titlequeue))
            .setMessage(getString(R.string.messageQueue))
            .setPositiveButton(getString(R.string.queueAdd)) { p0, p1 -> viewModel.launchPlayerMultiple(MediaHelper.QUEUE_ADD,videoListToPlay,pref,context!!) }
            .setNegativeButton(getString(R.string.queueNew)) { p0, p1 -> viewModel.launchPlayerMultiple(MediaHelper.QUEUE_NEW,videoListToPlay,pref,context!!) }

        val dialog =builder.create()
        dialog.show()
    }

    fun deselectList(){
        if(viewModel.isVideolistInitialized()){
            viewModel.videoLista.forEachIndexed { index, videoObj ->
                if(videoObj.esSelected){
                    videoObj.esSelected = false
                    videoListDataAdapter.notifyItemChanged(index,videoObj)
                }
            }
        }
    }

    fun detectVideoListEqual(list1:List<VideoObj>,list2:List<VideoObj>):Boolean{
        if(list1.size != list2.size) return false
        list1.forEachIndexed { index, videoObj ->
            if(!videoObj.url.contentEquals(list2[index].url)) return false
        }
        return true
    }

}