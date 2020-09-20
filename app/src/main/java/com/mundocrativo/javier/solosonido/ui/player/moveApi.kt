package com.mundocrativo.javier.solosonido.ui.player

import android.util.Log
import com.mundocrativo.javier.solosonido.model.VideoObj
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.Exception

class MoveApi {
    interface Callback{
        fun onNextValue(value: Pair<Int,Int>)
        fun onCompleted()
        fun onApiError(causa:Throwable)
    }

    private var callBack : Callback? = null

    fun register(callbackt: Callback){
        this.callBack = callbackt
    }

    fun unregister(){
        callBack = null
    }

    fun genera(entrada: Pair<Int,Int>){
        callBack?.onNextValue(entrada)
    }

    fun acaba(){
        callBack?.onCompleted()
        unregister()
    }

}

fun flowFromMove(api: MoveApi): Flow<Pair<Int,Int>> = callbackFlow {
    val callback = object :
        MoveApi.Callback {
        override fun onNextValue(value: Pair<Int,Int>) {
            try {
                sendBlocking(value)
            } catch (e: Exception){
                //Log.v("msg","--Error in flow: $e")
            }
        }

        override fun onApiError(causa: Throwable) {

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