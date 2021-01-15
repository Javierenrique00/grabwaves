package com.mundocrativo.javier.solosonido.rep

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.mundocrativo.javier.solosonido.com.DirectCache
import com.mundocrativo.javier.solosonido.com.MetadataCache
import com.mundocrativo.javier.solosonido.db.QueueDao
import com.mundocrativo.javier.solosonido.db.QueueFieldDao
import com.mundocrativo.javier.solosonido.db.VideoDao
import com.mundocrativo.javier.solosonido.model.*
import com.mundocrativo.javier.solosonido.service.MusicServiceConnection
import com.mundocrativo.javier.solosonido.ui.historia.*
import com.mundocrativo.javier.solosonido.util.AppPreferences
import com.mundocrativo.javier.solosonido.util.Util
import com.squareup.moshi.Moshi
import java.lang.Exception


class AppRepository(private val videoDao: VideoDao,private val directCache: DirectCache,val musicServiceConnection: MusicServiceConnection,private val queueDao: QueueDao,private val queueFieldDao: QueueFieldDao,private val metadataCache: MetadataCache) {
    val moshi = Moshi.Builder().build()
    val infoAdapter = moshi.adapter(InfoObj::class.java)
    val searchAdapter = moshi.adapter(SearchObj::class.java)
    val playlistAdapter = moshi.adapter(PlaylistObj::class.java)
    val convertedAdapter = moshi.adapter(Converted::class.java)
    val openVideoUrlLiveData : MutableLiveData<Pair<Int,String>> by lazy { MutableLiveData<Pair<Int,String>>() }
    val openVideoListUrlLiveData : MutableLiveData<Pair<Int,List<VideoObj>>> by lazy { MutableLiveData<Pair<Int,List<VideoObj>>>() }
    var defaultPlayListId : Long? = null
    val playVideoListPair : MutableLiveData<Pair<Int,List<VideoObj>>> by lazy { MutableLiveData<Pair<Int,List<VideoObj>>>() } //--- para enviar el listado al player
    private var playerIsOpen = false


    fun checkServOk(pref:AppPreferences):Boolean{
        if(pref.server.isNotEmpty()){
            val resultado = directCache.conexionServer(Util.createCheckLink(pref)) ?: return false
            if(resultado.contentEquals("ok")) return true
            return false
        }
        return false
    }


    fun listVideos():List<VideoObj>{
        return videoDao.traeVideos()
    }

    fun insertVideo(item:VideoObj):Long{
        return videoDao.insert(item)
    }

    fun deleteVideo(key:Long,urlInfo:String){
        videoDao.delete(key)
        directCache.sacaDelCache(urlInfo)
    }

    fun getInfoFromUrl(url:String):InfoObj?{
        var resultado : InfoObj? = null
        //Log.v("msg","traeinfo: $url")
        val strResult = directCache.trae(url)
        //Log.v("msg","before Moshi: $strResult")
        strResult?.let {
            try {
                resultado = infoAdapter.fromJson(it)
                //Log.v("msg","Moshi duration:${resultado!!.duration}")
            }catch (e:Exception){
                Log.e("msg","Error en la conversion Moshi ${e.message}")
            }finally {
                return resultado
            }

        }
        return null
    }

    suspend fun suspendGetInfoFromUrl(url: String):InfoObj?{
        return getInfoFromUrl(url)
    }


    fun getSearchFromUrl(url:String):List<VideoObj>{
        var searchObj :SearchObj? = null
        //Log.v("msg","Search:$url")
        val strResult = directCache.trae(url)
        var resultado = mutableListOf<VideoObj>()
        strResult?.let{
            try {
                searchObj = searchAdapter.fromJson(strResult)
                resultado.addAll(convertSearhToVideoList(searchObj!!))
            }catch (e:Exception){
                Log.e("msg","Error en la conversion Moshi del search ${e.message}")
            }finally {
                return resultado
            }

        }
        return resultado
    }


    private fun convertSearhToVideoList(searchObj: SearchObj):List<VideoObj>{
        val resultList = mutableListOf<VideoObj>()

        searchObj.items.filter { (!it.isLive) and (!it.isUpcoming) }.forEach {
            resultList.add(VideoObj(
                0,
                it.url?:"",
                it.title?:"",
                it.author?.name?:"",
                chooseSmallThumbnail(it.thumbnails),
                0,
                0,
                0,
                0,
                0,
                0,"",0,
                false,
                false,
                false,
                0,
                null,
                false,
                it.duration?:"0"
            ))
        }
        return  resultList
    }

    private fun chooseSmallThumbnail(lista:List<MiniaturasList>):String {
        var minTama = Int.MAX_VALUE
        var selUrl = ""
        lista.forEach {
            val tama = it.width?:100000 * (it.height?:100000)
            if(tama<minTama) {
                selUrl = it.url?:""
                minTama = tama
            }
        }
        return selUrl
    }

    fun getPlayListFromUrl(video:VideoObj,url:String,msgError:String):VideoObj{
        //Log.v("msg","Buscando Playlistinfo:$url")
        val strResult = directCache.trae(url)
        var resultado = VideoObj()
        strResult?.let {
            try{
                val playlist = playlistAdapter.fromJson(strResult)
                playlist?.let { playlist ->
                    if(!playlist.error){
                        //Log.v("msg","Playlist Conversion total_items=${playlist.total_items}")
                        val videoList = convertPlaylistToVideoList(playlist)
                        resultado.url = video.url
                        resultado.title = playlist.title?:""
                        resultado.itemPosition = video.itemPosition
                        resultado.total_items = videoList.size
                        resultado.thumbnailUrl = playlist.items[0].thumbnail?:""
                        resultado.duration = videoList.fold(0){ acc, videoObj -> videoObj.duration + acc }
                        resultado.kindMedia = KIND_URL_PLAYLIST
                        resultado.esInfoReady = true
                    }else{
                        val vidReturnError = VideoObj()
                        with(vidReturnError){
                            id = video.id
                            title = msgError
                            itemPosition = video.itemPosition
                            esInfoReady = true
                            esUrlReady = true
                            timestamp = video.timestamp
                            kindMedia = video.kindMedia
                        }
                        vidReturnError.url = video.url
                        return vidReturnError
                    }
                }
            }catch (e:Exception){
                Log.e("msg","Error en la conversion Moshi del Playlist ${e.message}")
            }finally {
                return resultado
            }
        }
        return resultado
    }

    fun getPlayListFromUrl(url:String):List<VideoObj>{
        val strResult = directCache.trae(url)
        val resultado = mutableListOf<VideoObj>()
        strResult?.let {
            try{
                val playlist = playlistAdapter.fromJson(strResult)
                playlist?.let { playlist ->
                    //Log.v("msg","Playlist Conversion total_items=${playlist.total_items}")
                    resultado.addAll(convertPlaylistToVideoList(playlist))

                }
            }catch (e:Exception){
                Log.e("msg","Error en la conversion Moshi del Playlist ${e.message}")
            }finally {
                return resultado
            }
        }
        return resultado
    }

    private fun convertPlaylistToVideoList(playlist:PlaylistObj):List<VideoObj>{
        val resultList = mutableListOf<VideoObj>()
        playlist.items.forEach {
            resultList.add(
                VideoObj(
                0, it.url?:"",it.title?:"",it.author?:"",it.thumbnail?:"",0,0,it.duration,0, KIND_URL_VIDEO,0,"",0,true,false,false,0,null,false,""))
        }
        return resultList
    }

    fun openVideoUrl(queueCmd:Int,url:String){
        openVideoUrlLiveData.postValue(Pair(queueCmd,url))
    }

    fun openVideoListUrl(queueCmd:Int,list:List<VideoObj>){
        openVideoListUrlLiveData.postValue(Pair(queueCmd,list))
    }

    //--- para el manejo de las listas de reproduccion y la cola ---------------------------------------
    fun updateDefaultQueue(itemList:List<QueueField>){
        //--- chequea que tenga creada la lista DEFAULT
        val defaultIndex = getDefaulQueueIndex()
        itemList.forEach { it.queueId=defaultIndex }
        queueFieldDao.deleteFromQueue(defaultIndex)
        queueFieldDao.insertList(itemList)
        Log.v("msg","Save actualqueuelist size:${itemList.size}")
    }

    private fun getDefaulQueueIndex():Long{
        if(defaultPlayListId==null){
            val defaultQueue = queueDao.searchByName(DEFAULT_QUEUE_NAME)
            defaultPlayListId = if(defaultQueue==null) queueDao.insert(QueueObj(0, DEFAULT_QUEUE_NAME)) else defaultQueue.id
        }
        return defaultPlayListId!!
    }

    fun getDefaultQueue():List<QueueField>{
        return queueFieldDao.traeQueueItems(getDefaulQueueIndex())
    }

    fun getMetadataCache(key:String):AudioMetadata?{
        return metadataCache.traeMetadata(key)
    }

    fun putMetadataListCache(dataList:List<AudioMetadata>){
        dataList.forEach { metadataCache.poneMetadata(it) }
    }

    fun setPlayerIsOpen(isOpen:Boolean){
        playerIsOpen = isOpen
    }

    fun getPlayerIsOpen() = playerIsOpen

    fun getConvertedFiles(pref:AppPreferences,file:String?):Converted?{
        val fileMD5 = Util.createConvertedLink(pref,file)
        directCache.conexionServer(fileMD5)?.let { resultado ->
            return convertedAdapter.fromJson(resultado)
        }
        return null
    }

    fun preloadSong(pref: AppPreferences,url: String):Int{
        Log.v("msg","Preloading:$url")
        directCache.conexionServer(Util.createUrlConnectionStringPlay(pref.server,url,pref.hQ,pref.trans,true))?.let {resultado ->
            return when(resultado){
                "ready" -> PRELOAD_READY
                "inprocess" -> PRELOAD_INPROCESS
                "error" -> PRELOAD_ERROR
                else -> PRELOAD_ERROR
            }
        }
        return PRELOAD_READY
    }

    fun askForMp3Conversion(ruta:String):Int{
        directCache.conexionServer(ruta)?.let { resultado ->
            return when(resultado){
                "ready" -> PRELOAD_READY
                "inprocess" -> PRELOAD_INPROCESS
                "error" -> PRELOAD_ERROR
                else -> PRELOAD_ERROR
            }

        }
        return PRELOAD_ERROR
    }

}

const val DEFAULT_QUEUE_NAME = "default"