package com.mundocrativo.javier.solosonido.ui.player

import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime
import kotlinx.android.synthetic.main.player_fragment.*
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SeekBarControl {
    private var baseTime = DateTime(0)
    private var baseMs = 0L
    private var limite = 0L
    var maxStr = ""
    var isRunning = false

    fun setProgress(actual:Long,max:Long){
        baseTime = DateTime.now()
        baseMs = actual
        limite = max/1000
    }

    fun getSegProgress():Int{
        val ahora = DateTime.now()
        var avanceSeg = (ahora - baseTime).seconds.toLong() + (baseMs/1000L)
        if(avanceSeg>limite) avanceSeg = limite
        return avanceSeg.toInt()
    }

    suspend fun programUpdate(ejecuta:(cuenta:Int)->Unit):Boolean{
        if(!isRunning){
            delay(1100)
            isRunning = true
            var cont = 0
            while (isRunning){
                ejecuta(cont)
                cont++
                delay(1000)
            }
        }
        return true
    }


}