package com.mundocrativo.javier.solosonido.ui.main

import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import com.mundocrativo.javier.solosonido.BuildConfig
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.main_fragment.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel by sharedViewModel<MainViewModel>()
    private lateinit var pref : AppPreferences
    private lateinit var videoListDataAdapter: VideoListDataAdapter
    private lateinit var videoInfoApi: VideoInfoApi
    private lateinit var itemChangeApi: ItemChangeApi
    private lateinit var imageLoader : ImageLoader



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.main_fragment, container, false)

        view.checkServerBt.setOnClickListener {
            revizaServer(serverTb.text.toString(),"https://www.youtube.com/watch?v=kA9voL0edJU",false,false)
        }

        view.pasteBt.setOnClickListener {
            loadPasteText()
        }

        view.serverTb.addTextChangedListener(tw)

        view.qualitySw.setOnCheckedChangeListener { compoundButton, b ->
            //Log.v("msg","Cambio el estado del switch a:$b")
            pref.hQ = b
            showCalidadSw(b)
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        pref = AppPreferences(context!!)
        versionTt.text = "V:${BuildConfig.VERSION_NAME}"

        //--- videoRecyclerView
        setupVideoListRecyclerAdapter()
        viewModel.videoListLiveData.observe(viewLifecycleOwner, Observer {
            videoListDataAdapter.submitList(it)
            videoListDataAdapter.notifyDataSetChanged()
        })

        //---cuando se quita un item de la lista
        viewModel.notifyItemRemoved.observe(viewLifecycleOwner, Observer {
            videoListDataAdapter.notifyItemRemoved(it)
        })

        imageLoader = Coil.imageLoader(context!!)

    }

    override fun onResume() {
        super.onResume()
        //-- trae las preferencias hacia el display
        serverTb.setText(pref.server)
        checkFormulary()
        qualitySw.isChecked = pref.hQ

        //----Inicia el flow
        setupRecyclerFlow()


        //--- debe cargar los videos que están en la base de datos
        viewModel.loadVideosFromDb()

        val enlace = viewModel.enlaceExternal
        if(enlace==null){
            Log.v("msg","----> No hay link")
        } else{
            Log.v("msg","----> opening link: $enlace")
            revizaServer(serverTb.text.toString(),enlace,pref.hQ,true)
            viewModel.enlaceExternal = null
        }
    }

    override fun onPause() {
        super.onPause()
        videoInfoApi.acaba()
        itemChangeApi.acaba()
    }


    fun revizaServer(server:String,videoLetras:String,hQ:Boolean,addDb:Boolean) {
        val videoBase64 = Util.convStringToBase64(videoLetras)
        val quality = if(hQ) "hq" else "lq"
        val ruta = server + "/?link="+videoBase64+"&q=$quality"
        Log.v("msg","Contactando streaming:$ruta")
        if(addDb) viewModel.insertNewVideo(videoLetras)
        launchNavigator(ruta)
        //playMedia(ruta)
    }

    fun transUrlToServInfo(url:String):String{
        val videoBase64 = Util.convStringToBase64(url)
        val ruta = serverTb.text.toString() + "/info/?link=" +videoBase64
        return ruta
    }

    fun launchNavigator(ruta:String){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(ruta)
        startActivity(intent)
    }

    fun showCalidadSw(estado:Boolean){
        qualitySw.text = if(estado) getString(R.string.swTextHq) else getString(R.string.swTextLq)
    }

    fun checkFormulary():Boolean{
        val isOk = checkAddress()
        checkServerBt.isEnabled = isOk
        return isOk
    }

    fun checkAddress():Boolean{
        val text = serverTb.text.toString()
        return !text.isEmpty()
    }


    val tw = object : TextWatcher{
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            pref.server = serverTb.text.toString()
            checkFormulary()
            Log.v("msg","textBox=${pref.server}")
        }

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
            enlaceTb.setText(enlaceLetras)
        }
        if(!enlaceLetras.isEmpty()) revizaServer(serverTb.text.toString(),enlaceLetras,pref.hQ,true)
    }

    private fun setupRecyclerFlow(){
        //---inicia los eventos del flow del video
        videoInfoApi = VideoInfoApi()
        val flujoVideo = flowFromVideo(videoInfoApi).buffer(Channel.UNLIMITED).map { viewModel.getUrlInfo(it,transUrlToServInfo(it.url)) }

        GlobalScope.launch(Dispatchers.IO) {
            flujoVideo.collect{
                //--- getFont(it)
                if(it!=null) {
                    Log.v("msg","llegó url: ${it.url} titulo:${it.title}")
                    itemChangeApi.genera(Pair(it.itemPosition,it))

                    //---para pedir bajar una imagen
                    val request = ImageRequest.Builder(context!!)
                        .data(it.thumbnailUrl)
                        .target { drawable ->

                            val item = it
                            item.esUrlReady = true
                            item.thumbnailImg = drawable
                            itemChangeApi.genera(Pair(it.itemPosition,item))
                            Log.v("msg","Llego thumbnail:${it.thumbnailUrl}")
                        }
                        .build()
                    val disposable = imageLoader.enqueue(request)
                }

            }
        }

        //---inicia los eventos del flow de cambio de item
        itemChangeApi = ItemChangeApi()
        val flujoItem = flowFromItem(itemChangeApi).buffer(Channel.UNLIMITED)

        GlobalScope.launch(Dispatchers.Main) {
            flujoItem.collect{

                Log.v("msg","---Item cambiado pos=${it.first} videoTitle=${it.second.title}")
                viewModel.videoLista[it.first].title = it.second.title
                viewModel.videoLista[it.first].channel = it.second.channel
                viewModel.videoLista[it.first].thumbnailUrl = it.second.thumbnailUrl
                viewModel.videoLista[it.first].esInfoReady = it.second.esInfoReady
                viewModel.videoLista[it.first].esUrlReady = it.second.esUrlReady
                viewModel.videoLista[it.first].thumbnailImg = it.second.thumbnailImg

                videoListDataAdapter.notifyItemChanged(it.first,it.second)

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
                    val itemSelected = it.item
                    itemSelected.esSelected = true
                    itemChangeApi.genera(Pair(it.position,itemSelected))
                    revizaServer(serverTb.text.toString(),it.item.url,pref.hQ,false)
                }
                is VideoListEvent.OnItemGetInfo ->{
                    val dataWithPosition = it.item
                    dataWithPosition.itemPosition = it.position
                    videoInfoApi.genera(dataWithPosition)
                }
                is VideoListEvent.OnStartDrag ->{
                    itemTouchHelper.startDrag(it.viewHolder)
                }
                is VideoListEvent.OnSwipeRight ->{
                    Log.v("msg","Recibe evento para borrar por swipe---------==>${it.id}")
                    val urlInfo = transUrlToServInfo(it.url)
                    viewModel.deleteVideoListElement(it.id,urlInfo)
                }
            }
        })
    }



    fun playMedia(ruta:String){
        val mediaPlayer: MediaPlayer? = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(ruta)
            prepare() // might take long! (for buffering, etc)
            start()
        }
    }


}