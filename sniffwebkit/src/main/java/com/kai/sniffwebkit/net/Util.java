package com.kai.sniffwebkit.net;

import android.annotation.SuppressLint;
import android.util.Log;

import com.kai.sniffwebkit.sniff.SniffTool;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Util {
    //屏蔽的广告视频
    public static List<String> urls = Arrays.asList("41115.m3u8", "api.jhdyw.vip/1.m3u8", "https://yuncache.52e.cc/m3u8/b079195c40f264fe9f876968c7b3c821.m3u8", "token=" , "https://v10.dious.cc", "index.php", "cdn.jsdelivr.net", "preview.mp4", "web_id=");
    public static List<String> labels = Arrays.asList(".js", ".html", ".png", ".jpg", ".json", "jpe", ".wbmp", ".net", ".fax"
            , ".gif", ".jpeg", ".htm", ".jsp", ".asp", ".ico", ".xml", ".xhtml", ".tif", ".tiff", ".wasm", ".woff", ".svga", ".ts", ".ttf", "f4v", ".dat", "webp", ".gif", ".css");

    private static boolean containsOfficialUrl(String url){
        return url.contains("=https://v.qq.com") || url.contains("=https://v.youku.com") || url.contains("=https://www.bilibili.com") || url.contains("=https://www.mgtv.com/b") || url.contains("=https://www.iqiyi.com/v_");
    }

    public static boolean isFiltered(String url){
        if (url.endsWith("/") || url.endsWith(".php"))
            return true;


        String url1 = url.replaceAll("\\?.*", "");
        for (String label : labels) {
            if (url1.toLowerCase(Locale.ROOT).endsWith(label)){
                return true;
            }
        }
        if (containsOfficialUrl(url))
            return true;
        for (String u: urls){
            if (url.contains(u)) {
                return true;
            }
        }

        return false;
    }
    public static Object[] getContent(String url, Map<String, String> headers) {
        Object[] objects = new Object[3];
        //第一个装byte，第二个装contentType
        if (isFiltered(url)) {
            objects[0] = null;
            objects[1] = "filtered";
            return objects;
        }

        try {
            Connection.Method method = Connection.Method.HEAD;
            if (url.contains(".m3u8"))
                method = Connection.Method.GET;
            Connection.Response response = Jsoup.connect(url)
                    .sslSocketFactory(SSLTool.getSocketFactory())
                    .userAgent(SniffTool.getUserAgent())
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .headers(headers)
                    .method(method)
                    .timeout(5 * 1000)
                    .execute();
            if (method == Connection.Method.GET)
                objects[0] = response;
            if (response.contentType() == null){
                objects[1] = "";
            }else
                objects[1] = Objects.requireNonNull(response.contentType()).split(";")[0];
            if (response.charset() == null)
                objects[2] = "";
            else
                objects[2] = response.charset();


        } catch (Exception e){
            e.printStackTrace();
            Log.e("Content","not available:" + url);
            objects[0] = null;
            if (url.endsWith("m3u8") || url.endsWith("mp4"))
                objects[1] = "video/mpeg-url";
            else
                objects[1] = "filtered";
        }

        if (objects[1] == null) objects[1] = "filtered";
        if (url.replaceAll("\\?.*", "").endsWith(".mp4"))
            objects[1] = "video/mp4";
        return objects;
    }

    public static long getContentLength(String url){
        try {
            Connection.Response response = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .method(Connection.Method.HEAD)
                    .userAgent(SniffTool.getUserAgent())
                    .execute();
            String c = response.header("Content-length");
            if (c != null && !c.isEmpty())
                return Long.parseLong(c);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }






    /**
     * 覆盖java默认的证书验证
     */
    @SuppressLint("CustomX509TrustManager")
    private static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        @SuppressLint("TrustAllX509TrustManager")
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }
    }};

    /**
     * 设置不验证主机
     */
    private static final HostnameVerifier DO_NOT_VERIFY = (hostname, session) -> true;

    /**
     * 信任所有
     * @param connection
     * @return
     */
    private static SSLSocketFactory trustAllHosts(HttpsURLConnection connection) {
        SSLSocketFactory oldFactory = connection.getSSLSocketFactory();
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory newFactory = sc.getSocketFactory();
            connection.setSSLSocketFactory(newFactory);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return oldFactory;
    }


}
