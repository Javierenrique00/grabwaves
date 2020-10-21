package com.mundocrativo.javier.solosonido.util

import android.util.Log
import com.mundocrativo.javier.solosonido.library.MediaHelper
import com.mundocrativo.javier.solosonido.rep.AppRepository
import com.mundocrativo.javier.solosonido.ui.historia.PRELOAD_ERROR
import com.mundocrativo.javier.solosonido.ui.historia.PRELOAD_INPROCESS
import com.mundocrativo.javier.solosonido.ui.historia.PRELOAD_READY
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PreloadFile(private val pref:AppPreferences,private val fullDurationMs:Int,private val appRepository:AppRepository) {
    var result : Int? = null
    var url = ""

    fun preload(url:String):Int?{
        this.url = url
        result = appRepository.preloadSong(pref,url)
        return result
    }

    suspend fun checkProgress(setProgres:(progres:Int)->Unit) = coroutineScope{
        var salida = false
        var lastMs = 0L
        var acumNoChange = 0
        do{
            delay(500)
            appRepository.getConvertedFiles(pref,Util.md5FileName(pref,url))?.let {conv->
                if(conv.conversion.size>0){
                    val ms = conv.conversion[0].msconverted
                    if(ms==lastMs) acumNoChange
                    lastMs = ms
                    if(acumNoChange>100) salida = true
                    val percent = if(fullDurationMs!=0) 100*ms/(fullDurationMs*1000000) else 50
                    setProgres(percent.toInt())
                }else{
                    salida = true
                }
            }
        }while (!salida)
        setProgres(100)
    }

}