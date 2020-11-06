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
    var filename = ""
    var resultFileExists = false

    fun preload(url:String):Int?{
        filename = Util.md5FileName(pref,url)
        this.url = url
        result = appRepository.preloadSong(pref,url)
        return result
    }

    fun loadMp3(url:String):Int{
        filename = Util.md5Mp3Filename(url)
        this.url = url
        return appRepository.askForMp3Conversion(Util.createMp3Link(pref,url))
    }

    suspend fun checkProgress(setProgres:(progInfo:ProgressInfo)->Unit) = coroutineScope{
        var salida = false
        do{
            delay(1000)
            appRepository.getConvertedFiles(pref,filename)?.let {conv->
                //Log.v("msg","checkProgress->$filename")

                if (conv.conversion.size > 0) {
                    val ms = conv.conversion[0].msconverted
                    val size = conv.conversion[0].sizeconverted
                    //Log.v("msg","ms=$ms  size=$size")
                    if (size == 0L) {
                        val percent = if (fullDurationMs != 0) ms / (fullDurationMs * 10000) else 50
                        setProgres(ProgressInfo(percent.toInt(), "  " + percent.toInt() + "%"))
                    } else {
                        val percent = ((size.toInt() / 1048576) % 100) //-- avanza cada mega
                        setProgres(ProgressInfo(percent, Util.readableFileSize(size.toInt())))
                    }
                } else {
                    salida = true
                    resultFileExists = conv.complete.contains(filename)
                }
            }
        }while (!salida)

        if(resultFileExists){
            setProgres(ProgressInfo(100,"Finish"))
        }else{
            setProgres(ProgressInfo(100,"Error"))
        }

    }

}