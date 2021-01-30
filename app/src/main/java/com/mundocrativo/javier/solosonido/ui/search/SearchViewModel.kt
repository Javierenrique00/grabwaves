package com.mundocrativo.javier.solosonido.ui.search

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mundocrativo.javier.solosonido.library.MediaHelper
import com.mundocrativo.javier.solosonido.model.SearchItem
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.rep.AppRepository
import com.squareup.moshi.internal.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class SearchViewModel(val appRepository: AppRepository) : ViewModel(){

    val imageLoader = appRepository.getAppCoilImageLoader()
    val videoListLiveData : MutableLiveData<List<VideoObj>> by lazy { MutableLiveData<List<VideoObj>>() }
    lateinit var videoLista : MutableList<VideoObj>
    val recVideoList = mutableListOf<List<VideoObj>>()
    val showToastMessage : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val sugerenciasList : MutableLiveData<List<SearchItem>> by lazy { MutableLiveData<List<SearchItem>>() }


    suspend fun getSearchData(searchURL:String):List<VideoObj> = coroutineScope {
        Log.v("msg","Search URL: $searchURL")
        val listaEsp = async<List<VideoObj>> {
            appRepository.getSearchFromUrl(searchURL)
        }
        //Log.v("msg","Resultados.size=${data.size}")
        listaEsp.await()
    }


    fun openVideoItem(queueCmd:Int,item:VideoObj){
        //appRepository.openVideoUrl(queueCmd, url)
        appRepository.openVideoListUrl(queueCmd, mutableListOf(item))
    }

    fun playSelectedVideo(queueCmd: Int){
        appRepository.openVideoListUrl(queueCmd, videoLista.filter { it.esSelected })
    }


    fun getRelatedVideos(url:String)=viewModelScope.launch(Dispatchers.IO){
        //--- debe traer el info del video
        val info = appRepository.getInfoFromUrl(url)
        info?.let { info ->
            val relatedVideoList = mutableListOf<VideoObj>()
            info.related.forEach {
                val isInfoReady = it.title.isNotEmpty() and it.author.isNotEmpty() and it.iUrl.isNotEmpty()
                //Log.v("msg","Es info ready =$isInfoReady  relatedIndex=${relatedVideoList.size}")
                relatedVideoList.add(
                    VideoObj(
                    0,
                    com.mundocrativo.javier.solosonido.util.Util.createUrlFromVideoId(it.id),
                        it.title,
                        it.author,
                        it.iUrl,
                        0,
                        0,
                        it.duration,
                        0,
                        0,
                        0,
                        "",0,
                        isInfoReady,
                        false,
                        false,
                        0,
                        null,
                        false,
                        "")) }

            //--- tengo una lista de related videos
            recVideoList.add(videoLista)
            videoLista = relatedVideoList
            videoListLiveData.postValue(videoLista)
        }
    }

    fun isVideolistInitialized() = this::videoLista.isInitialized

    fun insertSearchItem(inputValue:String) = viewModelScope.launch(Dispatchers.IO) {
        if(inputValue.length>1){
            Log.v("msg","--> Inserting to DB $inputValue")
            appRepository.insertSearchItemDB(inputValue)
        }
    }

    fun getAllSearchItemsDB() = viewModelScope.launch(Dispatchers.IO) {
        appRepository.purgarSearchDB()
        sugerenciasList.postValue(appRepository.loadSearchCompleteListDB())
    }

}