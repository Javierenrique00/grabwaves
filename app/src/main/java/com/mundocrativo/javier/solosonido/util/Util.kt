package com.mundocrativo.javier.solosonido.util

import android.util.Base64.DEFAULT
import android.util.Base64
import java.util.*

object Util {

    fun convStringToBase64(texto:String):String{
        val data = texto.toByteArray()
        return Base64.encodeToString(data,DEFAULT)
    }

}