package com.mundocrativo.javier.solosonido.util

import android.util.Log
import com.mundocrativo.javier.solosonido.library.MediaHelper
import com.mundocrativo.javier.solosonido.model.ProgressInfo
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

    suspend fun checkProgress(setProgres:(progInfo:ProgressInfo)->Unit) = coroutineScope{
        var salida = false
        do{
            delay(1000)
            appRepository.getConvertedFiles(pref,Util.md5FileName(pref,url))?.let {conv->
                if(conv.conversion.size>0){
                    val ms = conv.conversion[0].msconverted
                    val size = conv.conversion[0].sizeconverted
                    if(size == 0L){
                        val percent = if(fullDurationMs!=0) ms/(fullDurationMs*10000) else 50
                        setProgres(ProgressInfo(percent.toInt(),"  "+percent.toInt()+"%"))
                    }
                    else{
                        val percent = ((size.toInt() / 1048576) % 100 ) //-- avanza cada mega
                        setProgres(ProgressInfo(percent,Util.readableFileSize(size.toInt())))
                    }
                }else{
                    salida = true
                }
            }
        }while (!salida)
        setProgres(ProgressInfo(100,"Finish"))
    }

}