package com.kai.sniffwebkit.net;

import android.annotation.SuppressLint;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class SSLTool {
	static SSLContext ctx;
	static {
		try {
			 ctx= SSLContext.getInstance("TLSv1.2");
			
			ctx.init(null, new X509TrustManager[] {
					new X509TrustManager() {
						
						@Override
						public X509Certificate[] getAcceptedIssuers() {
							// TODO Auto-generated method stub
							return new X509Certificate[0];
						}
						
						@SuppressLint("TrustAllX509TrustManager")
						@Override
						public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
							// TODO Auto-generated method stub
							
						}
						
						@SuppressLint("TrustAllX509TrustManager")
						@Override
						public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
							// TODO Auto-generated method stub
							
						}
					}
			}, new SecureRandom());
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	public static SSLSocketFactory getSocketFactory() {
		return ctx.getSocketFactory();
	}
}
