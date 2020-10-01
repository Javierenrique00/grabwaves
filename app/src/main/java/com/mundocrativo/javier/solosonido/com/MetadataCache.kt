package com.mundocrativo.javier.solosonido.com

import com.mundocrativo.javier.solosonido.base.GeneralCache
import com.mundocrativo.javier.solosonido.model.AudioMetadata
import com.squareup.moshi.Moshi


class MetadataCache(dao: GeneralCache.DaoInterface) : GeneralCache(dao) {
    private val moshi = Moshi.Builder().build()
    private val audioMetadataAdapter = moshi.adapter(AudioMetadata::class.java)

    fun poneMetadata(audioMeta:AudioMetadata){
        pone(audioMeta.mediaId,audioMetadataAdapter.toJson(audioMeta))
    }

    fun traeMetadata(key:String):AudioMetadata?{
        val strData = trae(key)
        if(strData!=null) return audioMetadataAdapter.fromJson(strData)
        return null
    }

}

