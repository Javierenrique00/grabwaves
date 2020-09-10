package com.mundocrativo.javier.solosonido.rep

import android.util.Log
import com.mundocrativo.javier.solosonido.com.DirectCache
import com.mundocrativo.javier.solosonido.db.VideoDao
import com.mundocrativo.javier.solosonido.model.InfoObj
import com.mundocrativo.javier.solosonido.model.Related
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.util.OkGetFileUrl
import com.mundocrativo.javier.solosonido.util.Util
import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import java.lang.Exception


class AppRepository(private val videoDao: VideoDao,private val directCache: DirectCache) {
    val moshi = Moshi.Builder().build()
    val infoAdapter = moshi.adapter(InfoObj::class.java)



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


}