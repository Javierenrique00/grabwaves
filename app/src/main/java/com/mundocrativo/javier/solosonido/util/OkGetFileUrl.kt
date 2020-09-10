package com.mundocrativo.javier.solosonido.util

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.Exception
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

object OkGetFileUrl {

    private fun sslContext(keyManagers: Array<KeyManager>?, trustManagers: Array<TrustManager>): SSLContext {
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagers,
                trustManagers,
                null)
            return sslContext
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException("Couldn't init TLS context", e)
        } catch (e: KeyManagementException) {
            throw IllegalStateException("Couldn't init TLS context", e)
        }

    }


    fun traeWebString(host:String):String?{
        val trustManager = TrustAllX509TrustManager.INSTANCE
        val client = OkHttpClient.Builder()
            .sslSocketFactory(
                sslContext(
                    null,
                    arrayOf<TrustManager>(trustManager)
                ).socketFactory,
                trustManager)
            .hostnameVerifier(TrustAllX509TrustManager.allowAllHostNames())
            .build()

        val request = Request.Builder()
            .url(host)
            .build()

        try{

            client.newCall(request).execute().use {
                    response ->
                if(response.isSuccessful){
                    return  response.body!!.string()
                }
                else{
                    Log.e("msg","HTTP Unexpected code $response")
                }
            }

        }
        catch (e:Exception){
            Log.e("msg","Error in conexion:$e")
        }

        return null
    }

}