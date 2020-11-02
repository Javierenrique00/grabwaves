package com.mundocrativo.javier.solosonido.util

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64.DEFAULT
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import com.mundocrativo.javier.solosonido.ui.historia.KIND_URL_PLAYLIST
import com.mundocrativo.javier.solosonido.ui.historia.KIND_URL_UNDEFINED
import com.mundocrativo.javier.solosonido.ui.historia.KIND_URL_VIDEO
import java.text.DecimalFormat
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

    fun createUrlConnectionStringPlay(server:String,videoLetras:String,hQ:Boolean,trans:Boolean,preload:Boolean):String {
        val videoBase64 = Util.convStringToBase64(videoLetras)
        val quality = if(hQ) "hq" else "lq"
        val tranStr = if(trans) "true" else "false"
        val preStr = if(preload) "true" else "false"
        val ruta = server + "/?link="+videoBase64+"&q=$quality"+"&tran=$tranStr"+"&pre=$preStr"
        //Log.v("msg","Contactando streaming:$ruta")
        return ruta
    }

    fun createUrlConnectionStringSearch(server:String,searchLetras:String,limit:Int):String {
        val videoBase64 = convStringToBase64(searchLetras)
        val ruta = server + "/search/?question="+videoBase64+"&limit=$limit"
        //Log.v("msg","Buscando:$ruta")
        return ruta
    }

    fun transUrlToServInfo(url:String,pref: AppPreferences):String{
        val videoBase64 = convStringToBase64(url)
        val ruta = pref.server + "/info/?link=" +videoBase64
        return ruta
    }

    fun transUrlToServePlaylist(url:String,pref: AppPreferences):String{
        val videoBase64 = convStringToBase64(url)
        val ruta = pref.server + "/pl/?link=" +videoBase64
        return ruta
    }

    fun createCheckLink(pref: AppPreferences):String{
        return pref.server + "/check"
    }

    fun createConvertedLink(pref:AppPreferences,file:String?):String{
        val hasFile = if(file!=null) "?file=$file" else ""
        return pref.server + "/converted" + hasFile
    }

    fun createUrlFromVideoId(videoId:String):String{
        return "https://www.youtube.com/watch?v=$videoId"
    }


    fun clickAnimator(view: View){
        val animator =ValueAnimator.ofFloat(1f,1.8f,1f).apply {
            duration = 500
            addUpdateListener {
                val scala = it.getAnimatedValue() as Float
                view.scaleX = scala
                view.scaleY = scala
            }
            start()
        }

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

    fun checkKindLink(url:String):Int{
        if(url.contains("/playlist")) return KIND_URL_PLAYLIST
        return KIND_URL_VIDEO  //-- por ahora no asignamos KIND_URL_UNDEFINED
    }

    fun hexMd5Checksum(name:String):String{
        val digest = java.security.MessageDigest.getInstance("MD5")
        digest.update(name.toByteArray())
        val hexArray =  digest.digest()
        return hexArray.fold("", { acc, byte -> acc + (byte.toUByte()).toString(16).padStart(2,'0') })
    }

    fun md5FileName(pref: AppPreferences,name: String):String{
        val quality = if(pref.hQ) "hq" else "lq"
        val transcode = if(pref.trans) "t" else "f"
        return hexMd5Checksum(name)+"$quality$transcode.opus"
    }


    fun readableFileSize(size2:Int):String {
        val size = size2.toLong()
        if (size <= 0) return "0"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

}