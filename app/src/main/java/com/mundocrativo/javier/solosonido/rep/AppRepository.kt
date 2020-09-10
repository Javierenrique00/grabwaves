package com.mundocrativo.javier.solosonido.rep

import android.util.Log
import com.mundocrativo.javier.solosonido.db.VideoDao
import com.mundocrativo.javier.solosonido.model.InfoObj
import com.mundocrativo.javier.solosonido.model.Related
import com.mundocrativo.javier.solosonido.model.VideoObj
import com.mundocrativo.javier.solosonido.util.OkGetFileUrl
import com.mundocrativo.javier.solosonido.util.Util
import com.squareup.moshi.Moshi


class AppRepository(val videoDao: VideoDao) {
    val moshi = Moshi.Builder().build()
    val infoAdapter = moshi.adapter(InfoObj::class.java)
    //val relatedAdapter = moshi.adapter(Related::class.java)
    private val cacheInfoUrl = mutableMapOf<Long,String>()


    fun listVideos():List<VideoObj>{
        return videoDao.traeVideos()
    }

    fun insertVideo(item:VideoObj){
        videoDao.insert(item)
    }

    fun getInfoFromUrl(url:String):InfoObj?{
        var hashUrl = Util.genHashFromString(url)
        var datos : String? = null
        Log.v("msg","Buscando INFO url=$url")
        if(!cacheInfoUrl.containsKey(hashUrl)) {
            datos = OkGetFileUrl.traeWebString(url)
            if(datos!=null) cacheInfoUrl[hashUrl] = datos
        }else{
            datos = cacheInfoUrl[hashUrl]
            Log.v("msg","Datos en el cache ${datos!!.length}")
        }
        datos?.let {

            return infoAdapter.fromJson(it)
        }
        return null
    }


}