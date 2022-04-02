package com.mundocrativo.javier.solosonido.ui.historia

import android.Manifest
import android.app.DownloadManager
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_MUSIC
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
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
import com.mundocrativo.javier.solosonido.model.QueueObj
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.ui.config.ConfigActivity
import com.mundocrativo.javier.solosonido.ui.main.*
import com.mundocrativo.javier.solosonido.ui.selection.*
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime
import kotlinx.android.synthetic.main.historia_fragment.*
import kotlinx.android.synthetic.main.historia_fragment.view.*
import kotlinx.android.synthetic.main.main_fragment.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.File
import java.io.IOException

class HistoriaFragment : Fragment() {

    companion object {
        fun newInstance() =
            HistoriaFragment()
    }

    private val viewModel by sharedViewModel<MainViewModel>()
    private lateinit var pref : AppPreferences
    private lateinit var videoListDataAdapter: VideoListDataAdapter
    private lateinit var videoPairApi : VideoPairApi
    private lateinit var imageLoader : ImageLoader
    private val downloadMannager by lazy { context!!.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    private var pendingDownloads = 0


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.historia_fragment, container, false)

        view.selectCb.setOnCheckedChangeListener { compoundButton, b ->
            showButtons()
        }

        view.playIv.setOnClickListener {
            //dialogPlayMultiple(viewModel.videoLista.filter { it.esSelected })
            createSelectionDialog(viewModel.videoLista.filter { it.esSelected })
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
        imageLoader = viewModel.imageLoader

        //--- videoRecyclerView
        setupVideoListRecyclerAdapter()
        viewModel.videoListLiveData.observe(viewLifecycleOwner, Observer {
            lifecycleScope.launch {
                val isServerWorking = viewModel.checkForServer(pref)
                if(isServerWorking){
                    videoListDataAdapter.submitList(it)
                    videoListDataAdapter.notifyDataSetChanged()
                }else{
                    sendToast(getString(R.string.msgConfigServer))
                }

            }
        })

        //---cuando se quita un item de la lista
        viewModel.notifyItemRemoved.observe(viewLifecycleOwner, Observer {
            videoListDataAdapter.notifyItemRemoved(it)
            val delta = viewModel.videoLista.size - it  //--cuando se quita un elemento de la lista hay que notificar también el rango que se mueve hacia arriba
            videoListDataAdapter.notifyItemRangeChanged(it,delta)
        })

        viewModel.openVideoUrlLiveData.observe(viewLifecycleOwner, Observer {
            if(!viewModel.lastUrlValue.contentEquals(it.second)){
                viewModel.lastUrlValue = it.second
                Log.v("msg","OPEN video ${it.second}")
                if(viewModel.loadLinkfromExternalapp){
                    viewModel.loadLinkfromExternalapp = false
                    insertItemAtTopList(it)
                    if(it.first!=MediaHelper.QUEUE_NO_PLAY){
                        val videoToPlay = VideoObj()
                        videoToPlay.url = it.second
                        val list = mutableListOf(videoToPlay)
                        Log.v("msg","Trying to play url: size =${list.size} url=${list[0].url}")
                        viewModel.launchPlayerMultiple(it.first, list,pref,getString(R.string.msgPlaying))
                    }else{
                        //Log.v("msg","Only smooth Scroll")
                        videoRv.smoothScrollToPosition(0)
                    }
                }
            }else{
                Log.v("msg","Last URL already opened ${it.second} not adding to the list")
            }
        })

        viewModel.openVideoListUrlLiveData.observe(viewLifecycleOwner, Observer {
            //Log.v("msg","Send video List to player")
            if(!detectVideoListEqual(it.second,viewModel.lastListOpenUrl)){
                var queueCmd = it.first
                if(queueCmd==MediaHelper.QUEUE_NEXT) {
                    viewModel.launchPlayerDirect(MediaHelper.QUEUE_NEW,it.second,pref,getString(R.string.msgPlaying))
                }else{
                    if(it.first!=MediaHelper.QUEUE_NEW_NOSAVE) it.second.forEach { video -> insertItemAtTopList(Pair(it.first,video.url)) } else queueCmd = MediaHelper.QUEUE_NEW
                    viewModel.launchPlayerMultiple(queueCmd,it.second,pref,getString(R.string.msgPlaying))
                }

            }
        })

        viewModel.showToastMessage.observe(viewLifecycleOwner, Observer {
            sendToast(it)
        })

        viewModel.preloadProgress.observe(viewLifecycleOwner, Observer {
            preloadProgressBar.progress = it.progress
            avanceTT.text = it.msg
        })

        viewModel.download.observe(viewLifecycleOwner, Observer {
            createDownloadMannagerRequest(it)
        })

        //--- debe cargar los videos que están en la base de datos
        viewModel.loadVideosFromDb()
        setupRecyclerFlow()
    }

    override fun onResume() {
        super.onResume()

        checkRightsForSDAccess()

        //--- para checkear de nuevo los servidores
        if(!viewModel.isServerChecked){
            viewModel.loadVideosFromDb()
        }

        showButtons()
        showDescargas()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoPairApi.acaba()
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
            viewModel.showToastMessage.postValue(enlaceLetras)
            insertItemAtTopList(Pair(MediaHelper.QUEUE_NO_PLAY,enlaceLetras))
            videoRv.smoothScrollToPosition(0)
        }
    }

    private fun setupRecyclerFlow(){
        //---inicia los eventos del flow del video
        videoPairApi = VideoPairApi()
        val flujoVideo = flowFromVideoPair(videoPairApi).buffer(Channel.UNLIMITED).map { viewModel.getUrlInfo(it.first,it.second,pref,getString(R.string.msgPlayListError)) }.retry(3){ cause ->
            if (cause is IOException) {
                viewModel.showToastMessage.postValue(getString(R.string.msgRetryIOException))
                Log.e("msg","Exepcion - DELAY/2000")
                delay(2000)
                return@retry true
            } else {
                return@retry false
            }
        }

        //--- Se quita el GLobalScope
        lifecycleScope.launch(Dispatchers.IO) {
            flujoVideo.collect{
                //Log.v("msg","llegó url: ${it.second.url} titulo:${it.second.title} duration:${it.second.duration} position:${it.second.itemPosition}")
                withContext(Dispatchers.Main){ updateItem(it.first,it.second) }

                //---para pedir bajar una imagen
                val request = ImageRequest.Builder(context!!)
                    .data(it.second.thumbnailUrl)
                    .target { drawable ->

                         val item = it.second
                        item.esUrlReady = true
                        item.thumbnailImg = drawable
                        updateItem(it.first,item)
                    }
                    .build()
                val disposable = imageLoader.enqueue(request)

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
                    //Log.v("msg","Click en position:${it.position} kind:${it.item.kindMedia}  title:${it.item.title} url:${it.item.url} esInfoReady:${it.item.esInfoReady} esUrlReady:${it.item.esUrlReady} ${it.item.thumbnailUrl}")
                    if(selectCb.isChecked){
                        //--- selección multiple
                        val itemSelected = it.item
                        itemSelected.esSelected = !it.item.esSelected
                        updateItem(it.position,itemSelected)
                    }
                    else{
                        //--- play directo
                        val itemSelected = it.item
                        itemSelected.esSelected = true
                        updateItem(it.position,itemSelected)
                        //dialogPlayMultiple(mutableListOf(itemSelected))
                        createSelectionDialog(mutableListOf(itemSelected))
                        MainScope().launch {
                            delay(100)
                            itemSelected.esSelected = false
                            //itemChangeApi.genera(Pair(it.position,itemSelected))
                            updateItem(it.position,itemSelected)
                        }
                    }
                }
                is VideoListEvent.OnItemGetInfo ->{
                    //Log.v("msg","Generando Flow ${it.item.url}")
                    videoPairApi.genera(Pair(it.position,it.item))
                }
                is VideoListEvent.OnStartDrag ->{
                    itemTouchHelper.startDrag(it.viewHolder)
                }
                is VideoListEvent.OnSwipeRight ->{
                    //Log.v("msg","Recibe evento para borrar por swipe---------==>${it.index}")
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

                when(Util.checkKindLink(pair.second)){
                    KIND_URL_PLAYLIST ->{
                        //Log.v("msg","Kind Playlist")
                        video.kindMedia = KIND_URL_PLAYLIST
                    }
                    KIND_URL_VIDEO ->{
                        //Log.v("msg","Kind Video")
                        video.kindMedia = KIND_URL_VIDEO
                    }
                    else ->{
                        //Log.v("msg","Kind Undefined trying video")
                        video.kindMedia = KIND_URL_VIDEO
                    }
                }
                val id = viewModel.insertNewVideo(video)
                video.id = id
                Log.v("msg","insertando item: $video")
                if(viewModel.isVideolistInitialized()){
                    viewModel.videoLista.add(0, video)
                }else{
                    viewModel.videoLista = mutableListOf(video)
                    viewModel.videoLista.add( video)
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
                //Log.v("msg","---Item cambiado pos=${index} videoTitle=${item.title} selected=${item.esSelected}")
                viewModel.videoLista[index].title = item.title
                viewModel.videoLista[index].channel = item.channel
                viewModel.videoLista[index].thumbnailUrl = item.thumbnailUrl
                viewModel.videoLista[index].esInfoReady = item.esInfoReady
                viewModel.videoLista[index].esUrlReady = item.esUrlReady
                viewModel.videoLista[index].thumbnailImg = item.thumbnailImg
                viewModel.videoLista[index].esSelected = item.esSelected
                viewModel.videoLista[index].duration= item.duration
                viewModel.videoLista[index].total_items = item.total_items
                viewModel.videoLista[index].kindMedia = item.kindMedia
                viewModel.videoLista[index].extraUrlVideo = item.extraUrlVideo

                videoListDataAdapter.notifyItemChanged(index,viewModel.videoLista[index])
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

//    fun dialogPlayMultiple(videoListToPlay:List<VideoObj>){
//        //val videoListToPlay = viewModel.videoLista.filter { it.esSelected }
//        val msg = getString(R.string.msgPlaying)
//        val builder = AlertDialog.Builder(context!!)
//            .setTitle(getString(R.string.titlequeue))
//            .setMessage(getString(R.string.messageQueue))
//            .setPositiveButton(getString(R.string.queueAdd)) { p0, p1 -> viewModel.launchPlayerMultiple(MediaHelper.QUEUE_ADD,videoListToPlay,pref,msg) }
//            .setNegativeButton(getString(R.string.queueNew)) { p0, p1 -> viewModel.launchPlayerMultiple(MediaHelper.QUEUE_NEW,videoListToPlay,pref,msg) }
//            .setNeutralButton(getString(R.string.queueNext)) { p0, p1 -> viewModel.launchPlayerMultiple(MediaHelper.QUEUE_NEXT,videoListToPlay,pref,msg) }
//
//        val dialog =builder.create()
//        dialog.show()
//    }

    fun createSelectionDialog(videoListToPlay:List<VideoObj>){

        val msg = getString(R.string.msgPlaying)
        val ft = childFragmentManager.beginTransaction()
        val prev = childFragmentManager.findFragmentByTag("selection")
        if(prev!=null) ft.remove(prev)
        val dialogFragment = SelectionDialogFragment(){
            when(it){
                SEL_END -> viewModel.launchPlayerMultiple(MediaHelper.QUEUE_ADD,videoListToPlay,pref,msg)
                SEL_NEW -> viewModel.launchPlayerMultiple(MediaHelper.QUEUE_NEW,videoListToPlay,pref,msg)
                SEL_NEXT -> viewModel.launchPlayerMultiple(MediaHelper.QUEUE_NEXT,videoListToPlay,pref,msg)
                SEL_PLAYNOW -> viewModel.launchPlayerDirect(MediaHelper.QUEUE_NEW,videoListToPlay,pref,msg)
                SEL_DOWNLOAD -> viewModel.convToMp3(videoListToPlay,pref,getString(R.string.msgNotImplemented))
            }

        }.apply { show(ft,"selection") }

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

    fun sendToast(mensaje:String){
        val toast = Toast.makeText(context,mensaje,Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER,0,0)
        toast.show()
    }

    fun createDownloadMannagerRequest(song:VideoObj){
        val url = song.url
        Log.v("msg","Download url=$")
        val urlPath = Util.createMp3DownloadLink(pref,url,song.extraUrlVideo)
        Log.v("msg","path:->$urlPath")

        //val filePath = "file://" + getMusicPath() + File.separator + Util.md5Mp3Filename(url)
        val filePath = "file://" + getMusicPath() + File.separator + song.title.slice(0 until if(song.title.length>30) 30 else song.title.length) +".mp3"

        Log.v("msg","FilePath:->$filePath")

        val downLoadReq = DownloadManager.Request(Uri.parse(urlPath))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.parse(filePath))
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val downId = downloadMannager.enqueue(downLoadReq)
        pendingDownloads++
        val checkDownloads = lifecycleScope.launch {

            while(!isDownloadFinish(downId)){
                delay(1000)
                showDescargas()
            }
            pendingDownloads--
            showDescargas()
        }

    }

    fun getMusicPath():String{
//        return if(Build.VERSION.SDK_INT >28){
//            Log.v("msg","api 29>=")
//            MediaStore.Audio.Media.getContentUri()
//        }else{
//            Log.v("msg","api 28<")
//            Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC).path
//        }
        return Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC).path
    }


    fun isDownloadFinish(id:Long):Boolean{
        val cursor = downloadMannager.query(DownloadManager.Query().setFilterById(id))
        if(cursor.moveToFirst()){
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            when(status){
                DownloadManager.STATUS_FAILED ->{
                    val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                    Log.v("msg","Download failed - reason:$reason")
                }
                DownloadManager.STATUS_PAUSED ->{
                    //Log.v("msg","Download paused")
                }
                DownloadManager.STATUS_PENDING ->{
                    //Log.v("msg","Download pending")
                }
                DownloadManager.STATUS_RUNNING ->{
                    //Log.v("msg","Download running")
                }
                DownloadManager.STATUS_SUCCESSFUL ->{
                    //Log.v("msg","Download complete")
                }
            }
            return ((status==DownloadManager.STATUS_SUCCESSFUL) or (status==DownloadManager.STATUS_FAILED))
        }
        return true
    }


    fun checkRightsForSDAccess(){
        Log.v("Security:", "Chequeando permisos")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context!!.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.v("MSG:", "**********No tiene permiso")
                ActivityCompat.requestPermissions(
                    activity!!,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    0
                )
            } else {
                Log.v("Security:", "Si tiene permisos")
            }
        }
    }

    fun showDescargas(){
        descargasTt.text = pendingDownloads.toString()+" "
    }

}

const val KIND_URL_VIDEO = 1
const val KIND_URL_PLAYLIST = 2
const val KIND_URL_UNDEFINED = 0

const val PRELOAD_READY = 1
const val PRELOAD_INPROCESS = 2
const val PRELOAD_ERROR = 0
