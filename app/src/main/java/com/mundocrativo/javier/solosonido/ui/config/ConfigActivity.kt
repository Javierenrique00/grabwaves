package com.mundocrativo.javier.solosonido.ui.config

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mundocrativo.javier.solosonido.R

class ConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.config_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ConfigFragment.newInstance())
                .commitNow()
        }
    }
}