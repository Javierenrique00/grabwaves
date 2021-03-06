package com.mundocrativo.javier.solosonido.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class TrustAllX509TrustManager implements X509TrustManager {
    public static final X509TrustManager INSTANCE = new TrustAllX509TrustManager();

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException { }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException { }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
    

    public static HostnameVerifier allowAllHostNames() {
        // Create all-trusting host name verifier
        HostnameVerifier allowAllHostNamesConstante = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                HostnameVerifier hv =
                        HttpsURLConnection.getDefaultHostnameVerifier();
                return hv.verify(hostname, session);
            }
        };
        return allowAllHostNamesConstante;
    }
}
