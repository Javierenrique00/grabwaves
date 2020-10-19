package com.mundocrativo.javier.solosonido.com

import android.util.Log
import com.mundocrativo.javier.solosonido.model.CacheStr
import com.mundocrativo.javier.solosonido.util.OkGetFileUrl
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime

class DirectCache(val cacheStrDao: CacheStrDao) {

    private val cacheInfoUrl = mutableMapOf<Long,String>()

    fun trae(url:String):String?{
        var hashUrl = Util.genHashFromString(url)
        if(cacheInfoUrl.containsKey(hashUrl)){
            //--- Está en el cache de la RAM
            return cacheInfoUrl[hashUrl]!!
        }else{
            //--- No está en el cache de la RAM - Debe busacarlo en ROOM
            val dato =cacheStrDao.traeCache(hashUrl)
            if(dato!=null){
                //-- esta en ROOM, hay que cargarlo a ram y retornar el valor
                cacheInfoUrl.put(hashUrl,dato.cacheData!!)
                return dato.cacheData!!
            }else{
                //-- no está en room, hay que traerlo de la nube
                val resultado = OkGetFileUrl.traeWebString(url)
                if(resultado!=null){
                    cacheInfoUrl.put(hashUrl,resultado)
                    cacheStrDao.insert(CacheStr(
                        hashUrl,
                        resultado,
                        DateTime.nowUnixLong()
                    ))
                    return resultado
                }
                else{
                    return null
                }
            }

        }
    }

    fun sacaDelCache(urlInfo:String){
        var hashUrl = Util.genHashFromString(urlInfo)
        cacheStrDao.deleteCacheId(hashUrl)
    }

    fun conexionServer(url:String):String?{
        return OkGetFileUrl.traeWebString(url)
    }

}