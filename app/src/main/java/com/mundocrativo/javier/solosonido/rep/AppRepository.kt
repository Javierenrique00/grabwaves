package com.mundocrativo.javier.solosonido.rep

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.mundocrativo.javier.solosonido.com.DirectCache
import com.mundocrativo.javier.solosonido.db.QueueDao
import com.mundocrativo.javier.solosonido.db.QueueFieldDao
import com.mundocrativo.javier.solosonido.db.VideoDao
import com.mundocrativo.javier.solosonido.model.*
import com.mundocrativo.javier.solosonido.service.MusicServiceConnection
import com.squareup.moshi.Moshi
import java.lang.Exception


class AppRepository(private val videoDao: VideoDao,private val directCache: DirectCache,val musicServiceConnection: MusicServiceConnection,private val queueDao: QueueDao,private val queueFieldDao: QueueFieldDao) {
    val moshi = Moshi.Builder().build()
    val infoAdapter = moshi.adapter(InfoObj::class.java)
    val searchAdapter = moshi.adapter(SearchObj::class.java)
    val openVideoUrlLiveData : MutableLiveData<Pair<Int,String>> by lazy { MutableLiveData<Pair<Int,String>>() }
    var defaultPlayListId : Long? = null



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
        Log.v("msg","traeinfo: $url")
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

        searchObj.items.forEach {
            resultList.add(VideoObj(
                0,
                it.link?:"",
                it.title?:"",
                it.author?.name?:"",
                it.thumbnail,
                0,
                0,
                0,
                0,
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

    fun openVideoUrl(queueCmd:Int,url:String){
        openVideoUrlLiveData.postValue(Pair(queueCmd,url))
    }

    //--- para el manejo de las listas de reproduccion y la cola ---------------------------------------
    fun updateDefaultQueue(itemList:List<QueueField>){
        //--- chequea que tenga creada la lista DEFAULT
        val defaultIndex = getDefaulQueueIndex()
        itemList.forEach { it.queueId=defaultIndex }
        queueFieldDao.deleteFromQueue(defaultIndex)
        queueFieldDao.insertList(itemList)
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
}

const val DEFAULT_QUEUE_NAME = "default"