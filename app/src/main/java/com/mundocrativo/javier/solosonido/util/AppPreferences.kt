package com.mundocrativo.javier.solosonido.util

import android.content.Context
import hu.autsoft.krate.*

class AppPreferences(context: Context) : SimpleKrate(context) {

    var server : String by stringPref("serverStr","")
    var hQ : Boolean by booleanPref("hightQ",false)
    var lastSongIndexPlayed :Int by intPref("lastIndexSong",0)  //--la canción por default que se tocó
    var lastTimePlayed : Int by intPref("lasttimeplayed",0) //--en segundos

}