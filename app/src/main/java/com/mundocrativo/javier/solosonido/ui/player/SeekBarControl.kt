package com.mundocrativo.javier.solosonido.ui.player

import android.content.Context
import android.os.Looper.loop
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.mundocrativo.javier.solosonido.util.Util
import com.soywiz.klock.DateTime
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import kotlinx.android.synthetic.main.player_fragment.*
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SeekBarControl {
    private var baseTime : DateTime
    private var baseMs = 0L
    private var limite = 0L
    var maxStr = ""
    var isRunning = false
    var valid = false
    var lastCycle : DateTime

    init {
        baseTime = DateTime.now()
        lastCycle = DateTime.now()
    }

    fun setProgress(actual:Long,max:Long){
        baseTime = DateTime.now()
        baseMs = actual
        limite = max/1000
        valid = true
    }

    fun getSegProgress():Int?{
        if(!valid) return null
        val ahora = DateTime.now()
        var avanceSeg = (ahora - baseTime).seconds.toLong() + (baseMs/1000L)
        if(avanceSeg>limite) avanceSeg = limite
        return avanceSeg.toInt()
    }

    suspend fun programUpdate(ejecuta:(cuenta:Int)->Unit):Boolean{
        if(!isRunning){
            isRunning = true
            var cont = 0
            while (isRunning){
                delay(1000)
                val ahora = DateTime.now()
                val delta = (ahora-lastCycle)
                if(delta>=1.seconds){
                    //Log.v("msg","Delta=$delta")
                    ejecuta(cont)
                    cont++
                    lastCycle = ahora
                }
            }
        }
        return true
    }


}