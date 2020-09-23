package com.mundocrativo.javier.solosonido.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64.DEFAULT
import android.util.Base64
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import java.util.*

object Util {

    fun convStringToBase64(texto:String):String{
        val data = texto.toByteArray()
        //return Base64.encodeToString(data,DEFAULT)
        val base64Valor = Base64.encodeToString(data,DEFAULT)
        val remplazo1= base64Valor.replace("/","-")
        val remplazo2= remplazo1.replace("+","_")
        return remplazo2
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

    fun genHashFromString(inStr:String):Long{
        if(inStr.contentEquals("")) return -1L
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        var result = 0L
        var potencia = 1L
        inStr.forEach {
            var indice = charPool.indexOf(it).toLong()
            if(indice<0L) indice = it.toLong()
            result+=indice*potencia
            potencia = potencia*charPool.size
            if(potencia>(Long.MAX_VALUE/potencia)) potencia = 1
        }
        return  result
    }

    fun createUrlConnectionStringPlay(server:String,videoLetras:String,hQ:Boolean):String {
        val videoBase64 = Util.convStringToBase64(videoLetras)
        val quality = if(hQ) "hq" else "lq"
        val ruta = server + "/?link="+videoBase64+"&q=$quality"
        Log.v("msg","Contactando streaming:$ruta")
        return ruta
    }

    fun createUrlConnectionStringSearch(server:String,searchLetras:String,limit:Int):String {
        val videoBase64 = convStringToBase64(searchLetras)
        val ruta = server + "/search/?question="+videoBase64+"&limit=$limit"
        Log.v("msg","Buscando:$ruta")
        return ruta
    }

    fun transUrlToServInfo(url:String,pref: AppPreferences):String{
        val videoBase64 = convStringToBase64(url)
        val ruta = pref.server + "/info/?link=" +videoBase64
        return ruta
    }

    suspend fun getBitmap(url:String,context:Context):Bitmap?{
        val imageLoader = Coil.imageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()
        val bitmap = imageLoader.execute(request).drawable?.toBitmap(160,90) //--160 x 90
        return bitmap
    }

    fun shortHour(hora:String):String{
        var salida = hora
        var salida2 = ""
        var salida3 = ""
        var salida4 = ""
        var salida5 = ""
        if(hora.startsWith("0")) salida=hora.trimStart('0') else return hora
        if(salida.startsWith(":")) salida2=salida.trimStart(':') else return salida
        if(salida2.startsWith("0")) salida3=salida2.trimStart('0') else return salida2
        if(salida3.startsWith(":")) salida4=salida3.trimStart(':') else return salida3
        if(salida4.startsWith("0")) salida5=salida4.trimStart('0') else return salida4
        return salida5
    }


}