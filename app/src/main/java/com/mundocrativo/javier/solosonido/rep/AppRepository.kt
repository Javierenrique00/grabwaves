package com.mundocrativo.javier.solosonido.rep

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.mundocrativo.javier.solosonido.com.DirectCache
import com.mundocrativo.javier.solosonido.db.VideoDao
import com.mundocrativo.javier.solosonido.model.InfoObj
import com.mundocrativo.javier.solosonido.model.Related
import com.mundocrativo.javier.solosonido.model.SearchObj
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.util.OkGetFileUrl
import com.mundocrativo.javier.solosonido.util.Util
import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import java.lang.Exception


class AppRepository(private val videoDao: VideoDao,private val directCache: DirectCache) {
    val moshi = Moshi.Builder().build()
    val infoAdapter = moshi.adapter(InfoObj::class.java)
    val searchAdapter = moshi.adapter(SearchObj::class.java)
    val openVideoUrlLiveData : MutableLiveData<String> by lazy { MutableLiveData<String>() }



    fun listVideos():List<VideoObj>{
        return videoDao.traeVideos()
    }

    fun insertVideo(item:VideoObj){
        videoDao.insert(item)
    }

    fun deleteVideo(key:Long,urlInfo:String){
        videoDao.delete(key)
        directCache.sacaDelCache(urlInfo)
    }

    fun getInfoFromUrl(url:String):InfoObj?{
        var resultado : InfoObj? = null

        val strResult = directCache.trae(url)

        strResult?.let {
            try {
                resultado = infoAdapter.fromJson(it)
            }catch (e:Exception){
                Log.e("msg","Error en la conversion Moshi ${e.message}")
            }finally {
                return resultado
            }

        }
        return null
    }

    fun getSearchFromUrl(url:String):List<VideoObj>{
        var searchObj :SearchObj? = null
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
                null
            ))
        }
        return  resultList
    }

    fun openVideoUrl(url:String){
        openVideoUrlLiveData.postValue(url)
    }

}