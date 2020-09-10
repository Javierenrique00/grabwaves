package com.mundocrativo.javier.solosonido.ui.main

import android.util.Log
import com.mundocrativo.javier.solosonido.model.VideoObj
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.Exception

class VideoInfoApi {

    interface Callback{
        fun onNextValue(value: VideoObj)
        fun onCompleted()
    }

    private var callBack : Callback? = null

    fun register(callbackt:Callback){
        this.callBack = callbackt
    }

    fun unregister(){
        callBack = null
    }

    fun genera(entrada: VideoObj){
        callBack!!.onNextValue(entrada)
    }

    fun acaba(){
        callBack!!.onCompleted()
    }
}

fun flowFromVideo(api:VideoInfoApi): Flow<VideoObj> = callbackFlow {
    val callback = object : VideoInfoApi.Callback {
        override fun onNextValue(value: VideoObj) {
            try {
                sendBlocking(value)
            } catch (e:Exception){
                Log.v("msg","--Error in flow: $e")
            }
        }

        override fun onCompleted() {
            channel.close()
        }
    }

    api.register(callback)
    awaitClose{
        api.unregister()
    }

}