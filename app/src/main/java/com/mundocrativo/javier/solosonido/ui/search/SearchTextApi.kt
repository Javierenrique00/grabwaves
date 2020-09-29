package com.mundocrativo.javier.solosonido.ui.search

import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.Exception

class SearchTextApi {

    interface Callback{
        fun onNextValue(value: String)
        fun onCompleted()
    }

    private var callBack : Callback? = null

    fun register(callbackt: Callback){
        this.callBack = callbackt
    }

    fun unregister(){
        callBack = null
    }

    fun genera(entrada: String){
        callBack?.onNextValue(entrada)
    }

    fun acaba(){
        callBack?.onCompleted()
        unregister()
    }
}

fun flowFromString(api: SearchTextApi): Flow<String> = callbackFlow {
    val callback = object :
        SearchTextApi.Callback {
        override fun onNextValue(value: String) {
            try {
                sendBlocking(value)
            } catch (e: Exception){
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