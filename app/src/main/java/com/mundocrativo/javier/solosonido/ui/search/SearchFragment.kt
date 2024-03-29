package com.mundocrativo.javier.solosonido.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.library.MediaHelper
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.ui.historia.ItemChangeApi
import com.mundocrativo.javier.solosonido.ui.historia.VideoInfoApi
import com.mundocrativo.javier.solosonido.ui.historia.flowFromItem
import com.mundocrativo.javier.solosonido.ui.historia.flowFromVideo
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import com.mundocrativo.javier.solosonido.util.Util.createUrlConnectionStringSearch

import kotlinx.android.synthetic.main.search_fragment.*
import kotlinx.android.synthetic.main.search_fragment.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.IOException
import java.net.SocketTimeoutException

class SearchFragment : Fragment() {

    companion object {
        fun newInstance() = SearchFragment()
    }

    private val viewModel by sharedViewModel<SearchViewModel>()
    private lateinit var searchTextApi: SearchTextApi
    private lateinit var pref : AppPreferences
    private lateinit var searchListAdapter: VideoSearchDataAdapter
    private lateinit var thumbnailApi : VideoInfoApi
    private lateinit var itemChangeApi: ItemChangeApi
    private lateinit var imageLoader : ImageLoader
    private lateinit var searchStringAdapter : ArrayAdapter<String>
    private lateinit var autoCompleteTB : AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.search_fragment, container, false)

        view.searchTb.addTextChangedListener(tw)
        view.cancelBt.setOnClickListener {
            searchTb.setText("")
        }
        autoCompleteTB = view.searchTb

        view.backBt.setOnClickListener {
            val lastIndex = viewModel.recVideoList.size -1
            //Log.v("msg","Back button last backIndex=$lastIndex")
            if(lastIndex>=0){
                viewModel.videoLista = viewModel.recVideoList.removeAt(lastIndex).toMutableList()
                viewModel.videoLista.forEach { it.esSelected=false }
                viewModel.videoListLiveData.postValue(viewModel.videoLista)

                //----
                Util.clickAnimator(backBt)
            }
            showBackBtState()
        }

        view.multipleCb.setOnCheckedChangeListener { compoundButton, b ->
            showSelectButtons()
        }

        view.playSearchBt.setOnClickListener {
            dialogPlaySelectedItems()
            Util.clickAnimator(playSearchBt)
        }

        return view
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        pref = AppPreferences(context!!)

        searchTextApi = SearchTextApi()
        val searchTextFlow = flowFromString(searchTextApi)
            .map { it.toLowerCase()}
            .buffer(Channel.UNLIMITED)
            .debounce(500)
            .map {
                viewModel.insertSearchItem(it)
                insertSearchStringAutoTb(it)
                it
        }.retry(3){ cause ->
            if (cause is IOException) {
                viewModel.showToastMessage.postValue(getString(R.string.msgRetryIOException))
                Log.e("msg","Exepcion - DELAY/1000")
                delay(1000)
                return@retry true
            } else {
                return@retry false
            }
        }.map { value ->
            viewModel.getSearchData(createUrlConnectionStringSearch(pref.server,value,50)) }


        lifecycleScope.launch(Dispatchers.IO) {
            searchTextFlow.collect{ lista ->
                Log.v("msg","ListaSize = ${lista.size}")
                if(lista.size>0){
                    viewModel.videoLista = lista.toMutableList()
                    viewModel.videoListLiveData.postValue(lista)
                }
            }
        }

        setupSearchListRecycler()
        viewModel.videoListLiveData.observe(viewLifecycleOwner, Observer {
            searchListAdapter.submitList(it)
            searchListAdapter.notifyDataSetChanged()
            showBackBtState()
        })

        imageLoader = viewModel.imageLoader

        viewModel.showToastMessage.observe(viewLifecycleOwner, Observer {
            sendToast(it)
        })

        viewModel.sugerenciasList.observe(viewLifecycleOwner, Observer { sugerenciasList ->
            Log.v("msg","---Cargando la lista de sugerencias size=${sugerenciasList.size}")

            val listOption = mutableListOf<String>()
            sugerenciasList.forEach { listOption.add(it.busqueda) }
            searchStringAdapter = ArrayAdapter(context!!,R.layout.support_simple_spinner_dropdown_item,listOption)
            autoCompleteTB.setAdapter(searchStringAdapter)
        })
        viewModel.getAllSearchItemsDB()

    }

    val tw = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            searchTextApi.genera(searchTb.text.toString())
            showSearchButton()
        }

    }

    override fun onResume() {
        super.onResume()
        showBackBtState()
        showSelectButtons()
        showSearchButton()
    }

    private fun setupThumbnailFlow(){
        thumbnailApi = VideoInfoApi()
        val thumbnailFlow = flowFromVideo(thumbnailApi).buffer(Channel.UNLIMITED)

        lifecycleScope.launch(Dispatchers.IO) {
            thumbnailFlow.collect{ video ->
                //--pedimos el thumbnail
                val request = ImageRequest.Builder(context!!)
                    .data(video.thumbnailUrl)
                    .target { drawable ->

                        val item = video
                        item.esUrlReady = true
                        item.thumbnailImg = drawable
                        itemChangeApi.genera(Pair(video.itemPosition,item))
                        //Log.v("msg","Llego thumbnail:${video.thumbnailUrl}")
                    }
                    .build()
                val disposable = imageLoader.enqueue(request)

            }
        }

    }

    private fun setupItemChangeFlow(){
        itemChangeApi = ItemChangeApi()
        val itemFlujo = flowFromItem(itemChangeApi).buffer(Channel.UNLIMITED)

        lifecycleScope.launch(Dispatchers.Main) {
            itemFlujo.collect { item ->
                with(viewModel.videoLista[item.first]){
                    esUrlReady = item.second.esUrlReady
                    thumbnailImg = item.second.thumbnailImg
                }
                searchListAdapter.notifyItemChanged(item.first,item.second)

            }
        }

    }



    private fun setupSearchListRecycler(){

        //---- para el setup del flow de los thumbnail
        setupThumbnailFlow()
        setupItemChangeFlow()

        //---Agrega el item decoration
        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        val draw = context!!.getDrawable(R.drawable.vertical_divider)
        itemDecoration.setDrawable(draw!!)
        searchRv.addItemDecoration(itemDecoration)

        searchListAdapter = VideoSearchDataAdapter(context!!)
        searchRv.layoutManager = LinearLayoutManager(context)
        searchRv.adapter = searchListAdapter
        searchListAdapter.event.observe(viewLifecycleOwner, Observer {
            when(it){
                is SearchListEvent.OnItemClick ->{
                    if(multipleCb.isChecked){
                        it.item.esSelected = !it.item.esSelected
                        searchListAdapter.notifyItemChanged(it.position)
                    }
                    else{
                        dialogItemCola(it.item)
                        val selecItem = it.item
                        selecItem.esSelected = true
                        itemChangeApi.genera(Pair(it.position,selecItem))
                        MainScope().launch {
                            delay(100)
                            selecItem.esSelected = false
                            itemChangeApi.genera(Pair(it.position,selecItem))
                        }
                    }
                }
                is SearchListEvent.OnItemGetThumbnail ->{
                    val item = it.item
                    item.itemPosition = it.position
                    thumbnailApi.genera(it.item)
                }
                is SearchListEvent.OnItemLongClick ->{
                    searchTb.setText("")
                    it.item.esSelected = true
                    itemChangeApi.genera(Pair(it.position,it.item))
                    viewModel.getRelatedVideos(Util.transUrlToServInfo(it.item.url,pref))
                }
            }
        })


    }

    fun sendToast(mensaje:String){
        val toast = Toast.makeText(context,mensaje, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER,0,0)
        toast.show()
    }

    fun dialogItemCola(item:VideoObj){
        val builder = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.titlequeue))
            .setMessage(getString(R.string.messageQueue))
            .setPositiveButton(getString(R.string.queueAdd)) { p0, p1 -> viewModel.openVideoItem(MediaHelper.QUEUE_ADD,item) }
            .setNegativeButton(getString(R.string.queueNew)) { p0, p1 -> viewModel.openVideoItem(MediaHelper.QUEUE_NEW,item) }
            .setNeutralButton(getString(R.string.playNow)) { p0, p1 -> viewModel.openVideoItem(MediaHelper.QUEUE_NEXT,item) }
        val dialog =builder.create()
        dialog.show()
    }


    fun dialogPlaySelectedItems(){
        val builder = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.titlequeue))
            .setMessage(getString(R.string.messageQueue))
            .setPositiveButton(getString(R.string.queueAdd)) { p0, p1 -> viewModel.playSelectedVideo(MediaHelper.QUEUE_ADD) }
            .setNegativeButton(getString(R.string.queueNew)) { p0, p1 -> viewModel.playSelectedVideo(MediaHelper.QUEUE_NEW) }
            .setNeutralButton(getString(R.string.playNow)) { p0, p1 -> viewModel.playSelectedVideo(MediaHelper.QUEUE_NEXT) }
        val dialog =builder.create()
        dialog.show()
    }

    fun showSearchButton(){
        if(searchTb.text.toString().isEmpty()) {
            searchImg.visibility = View.VISIBLE
        }
        else{
            searchImg.visibility = View.GONE
        }
    }


    fun showBackBtState(){
        if(viewModel.recVideoList.size==0){
            backBt.setImageResource(R.drawable.ic_baseline_arrow_back_grey)
            viewModel.recVideoList.clear()
        }else{
            backBt.setImageResource(R.drawable.ic_baseline_arrow_back_24)
        }
    }

    fun showSelectButtons(){
        if(multipleCb.isChecked){
            playSearchBt.visibility = View.VISIBLE
        }else{
            playSearchBt.visibility = View.GONE
            if(viewModel.isVideolistInitialized()){
                viewModel.videoLista.forEachIndexed { index, videoObj ->
                    if(videoObj.esSelected){
                        videoObj.esSelected = false
                        searchListAdapter.notifyItemChanged(index,videoObj)
                    }
                }
            }
        }
    }


    fun insertSearchStringAutoTb(lineStr:String){
        lifecycleScope.launch(Dispatchers.Main) {
            if(lineStr.length>1){
                val pos = searchStringAdapter.getPosition(lineStr)
                if(pos<0) searchStringAdapter.add(lineStr)
            }
        }
    }


}