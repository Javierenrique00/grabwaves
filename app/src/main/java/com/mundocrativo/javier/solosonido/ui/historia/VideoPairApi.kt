package com.mundocrativo.javier.solosonido.ui.historia

import android.util.Log
import com.mundocrativo.javier.solosonido.model.VideoObj
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.Exception

class VideoPairApi {

    interface Callback{
        fun onNextValue(value: Pair<Int, VideoObj>)
        fun onCompleted()
    }

    private var callBack : Callback? = null

    fun register(callbackt: Callback){
        this.callBack = callbackt
    }

    fun unregister(){
        callBack = null
    }

    fun genera(entrada: Pair<Int,VideoObj>){
        callBack?.onNextValue(entrada)
    }

    fun acaba(){
        callBack?.onCompleted()
        unregister()
    }
}

fun flowFromVideoPair(api: VideoPairApi): Flow<Pair<Int,VideoObj>> = callbackFlow {
    val callback = object :
        VideoPairApi.Callback {

        override fun onNextValue(value: Pair<Int, VideoObj>) {
            try {
                sendBlocking(value)
            } catch (e:Exception){
                Log.e("msg","--Error in flow: $e")
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