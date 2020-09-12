package com.mundocrativo.javier.solosonido.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.mundocrativo.javier.solosonido.R
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModel<MainViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow()
        }
        Log.v("msg","From onCreate")
        getDataFromIntent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.v("msg","From new Intent")
        getDataFromIntent()
    }

    fun getDataFromIntent(){
        //--- trae el dato del enlace
        val extras = intent.extras
        val enlace = extras?.getString(Intent.EXTRA_TEXT)
        enlace?.let {
            Log.v("msg","Enlace = $it")
            viewModel.enlaceExternal = it
        }
    }


}