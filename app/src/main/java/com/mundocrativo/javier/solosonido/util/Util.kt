package com.mundocrativo.javier.solosonido.util

import android.util.Base64.DEFAULT
import android.util.Base64
import java.util.*

object Util {

    fun convStringToBase64(texto:String):String{
        val data = texto.toByteArray()
        return Base64.encodeToString(data,DEFAULT)
    }

    fun calcDeltaTiempo(time1:Long,time2:Long):String{
        var tiempo = time2 - time1
        var sec = tiempo
        var min = tiempo/(60)
        var hora = tiempo/(60*60)
        var dia = tiempo/(60*60*24)
        var veces = 0


        var resta = 0L
        var salida = ""
        if(dia==1L) {
            salida = "1 day,"
            resta = 24L
            veces++
        }
        if(dia>1L) {
            salida = dia.toString()+" days,"
            resta = 24L * dia
            veces++
        }
        hora = hora - resta
        resta = resta * 60 //--minutos
        if(hora==1L){
            salida+=" 1 hour,"
            resta += 60
            veces++
            if(veces>=2) return salida
        }
        if(hora>1L){
            salida+=" "+hora.toString()+" hours,"
            resta += 60 * hora
            veces++
            if(veces>=2) return salida
        }
        min = min - resta
        resta = resta * 60 //--segundos
        if(min==1L){
            salida+=" 1 min"
            resta += 60
            veces++
            if(veces>=2) return salida
        }
        if(min>1L){
            salida+=" "+min.toString()+" mins,"
            resta += 60 * min
            veces++
            if(veces>=2) return salida
        }
        sec = sec - resta
        if(sec==1L) salida += " 1 sec"
        if(sec>1L) salida += " "+sec.toString()+" secs"
        if(salida.contentEquals("")) salida = "now"
        return salida
    }

}