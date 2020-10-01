package com.mundocrativo.javier.solosonido.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

import org.koin.androidx.viewmodel.ext.android.sharedViewModel

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
        val searchTextFlow = flowFromString(searchTextApi).debounce(1000) //--sample

        lifecycleScope.launch {
            searchTextFlow.collect{ value ->
                //Log.v("msg","ValorString = $value")
                if(value.length>2) viewModel.getSearchData(createUrlConnectionStringSearch(pref.server,value,50))
            }
        }

        setupSearchListRecycler()
        viewModel.videoListLiveData.observe(viewLifecycleOwner, Observer {
            searchListAdapter.submitList(it)
            searchListAdapter.notifyDataSetChanged()
            showBackBtState()
        })

        imageLoader = Coil.imageLoader(context!!)
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
                        dialogItemCola(it.item.url)
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

    fun dialogItemCola(originalUrl:String){
        val builder = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.titlequeue))
            .setMessage(getString(R.string.messageQueue))
            .setPositiveButton(getString(R.string.queueAdd)) { p0, p1 -> viewModel.openVideoUrlLink(MediaHelper.QUEUE_ADD,originalUrl) }
            .setNegativeButton(getString(R.string.queueNew)) { p0, p1 -> viewModel.openVideoUrlLink(MediaHelper.QUEUE_NEW,originalUrl) }
            .setNeutralButton(getString(R.string.queueNext)) { p0, p1 -> viewModel.openVideoUrlLink(MediaHelper.QUEUE_NEXT,originalUrl) }
        val dialog =builder.create()
        dialog.show()
    }

    fun dialogPlaySelectedItems(){
        val builder = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.titlequeue))
            .setMessage(getString(R.string.messageQueue))
            .setPositiveButton(getString(R.string.queueAdd)) { p0, p1 -> viewModel.playSelectedVideo(MediaHelper.QUEUE_ADD) }
            .setNegativeButton(getString(R.string.queueNew)) { p0, p1 -> viewModel.playSelectedVideo(MediaHelper.QUEUE_NEW) }
            .setNeutralButton(getString(R.string.queueNext)) { p0, p1 -> viewModel.playSelectedVideo(MediaHelper.QUEUE_NEXT) }
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

}