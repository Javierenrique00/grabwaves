package com.mundocrativo.javier.solosonido.base

import com.mundocrativo.javier.solosonido.model.CacheMetaStr
import com.mundocrativo.javier.solosonido.model.CacheStr
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime

open class GeneralCache(private val daoInterface: DaoInterface) {
    private val dataCache = mutableMapOf<Long,String>()

    interface DaoInterface{
        fun putData(cache: CacheMetaStr)
        fun getData(key:Long):CacheMetaStr?
    }

    //--- traer dato del cache
    fun trae(key:String):String?{
        val hash = Util.genHashFromString(key)
        if(dataCache.containsKey(hash)){
            //--- está en el cache de la RAM
            return  dataCache[hash]
        }else{
            //--- no está en el cache de RAM - va a buscarlo en el ROOM
            val cacheItem = daoInterface.getData(hash)
            if(cacheItem!=null) return cacheItem.cacheData
            return null
        }
    }

    //--- pone los datos en el cache
    fun pone(key:String,data:String){
        val hash = Util.genHashFromString(key)
        dataCache[hash] = data
        val cacheStr = CacheMetaStr(
            hash,
            data,
            DateTime.nowUnixLong()
        )
        daoInterface.putData(cacheStr)
    }


}