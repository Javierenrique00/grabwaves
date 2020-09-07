package com.mundocrativo.javier.solosonido.util

import android.content.Context
import hu.autsoft.krate.SimpleKrate
import hu.autsoft.krate.booleanPref
import hu.autsoft.krate.stringPref

class AppPreferences(context: Context) : SimpleKrate(context) {

    var server : String? by stringPref("serverStr")
    var hQ : Boolean by booleanPref("hightQ",false)

}