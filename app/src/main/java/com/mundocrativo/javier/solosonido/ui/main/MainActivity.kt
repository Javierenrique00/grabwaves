package com.mundocrativo.javier.solosonido.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.library.MediaHelper
import com.mundocrativo.javier.solosonido.ui.historia.HistoriaFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        Log.v("msg","From onCreate----------------------------------------")
        getDataFromIntent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.v("msg","From new Intent---------------------------------")
        getDataFromIntent()
    }

    fun getDataFromIntent(paramIntent:Intent?=null){
        //--- trae el dato del enlace
        val extras = intent.extras
        var enlace = extras?.getString(Intent.EXTRA_TEXT)
        if(paramIntent!=null) enlace = paramIntent.extras!!.getString(Intent.EXTRA_TEXT)
        enlace?.let {
            //Log.v("msg","Enlace = $it")
            lifecycleScope.launch {
                delay(2000)
                viewModel.loadLinkfromExternalapp = true
                viewModel.openVideoUrlLiveData.postValue(Pair(MediaHelper.QUEUE_NO_PLAY,it))
            }

        }
    }

    override fun onStop() {
        super.onStop()
        Log.v("msg","Stop Activity---------------------------------")
    }


}