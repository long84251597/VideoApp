package com.kai.sniffwebkit.sniff;

import android.os.Build;
import android.util.Log;
import android.view.View;

import com.kai.sniffwebkit.net.HttpReferer;
import com.kai.sniffwebkit.net.Util;
import com.permission.kit.BuildConfig;

import org.jsoup.Connection;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MySniffingFilter {

    public static boolean canLog = true;

    public static void setCanLog(boolean canLog) {
        MySniffingFilter.canLog = canLog;
    }

    private void log(String message){
        if (canLog)
            Log.e("SniffingFilter", message);
    }

    public SniffingVideo onFilter(View webView, String url) {
        return onFilter(webView, url, new HashMap<>(0));
    }

    public SniffingVideo onFilter(View webView, String url, Map<String, String> headers) {
        SniffingVideo video = null;
        Object[] content = null;
        try {
            log("url = " + url);
            if ((url.contains(".m3u8") || url.contains(".mp4")) && url.contains("?url=http") )
                url = url.replaceAll(".*?url=", "");
            if (url.startsWith("https://ycache.parwix.com:4433/c") && url.contains(".m3u8?vkey="))
                video = new SniffingVideo(url, "image/vnd.microsoft.icon", 0, "m3u8");
            else if ((url.contains(".flv")) && !url.contains(".js") && !url.contains("flv.min.js")){
                video = new SniffingVideo(url, "video/x-flv", 0, "flv");
            }
            else if (url.endsWith(".mp4") && Util.getContentLength(url) > 1024 * 1024 *2){
                video = new SniffingVideo(url, "video/mp4", 0, "mp4");
            }
            //vkey请求的mp4和m3u8（防止vkey失效）及常见的index.m3u8后缀文件
            else if ((url.contains("vkey") && (url.contains(".mp4?") )|| (url.contains(".m3u8?") && url.contains("vkey") && !url.contains("https://ycache.parwix.com:4433/")))){
                video = new SniffingVideo(url, url.contains("m3u8")?"video/x-mpegurl":"video/mpeg", 0, url.contains("m3u8")?"m3u8":"mp4");
            }
            else if (url.endsWith("index.m3u8") && !url.contains("?url=")){
                video = new SniffingVideo(url, "video/x-mepgurl", 0, "m3u8");
            }
            //一些服务器请求类型的mp4，因为大体积需要正则匹配
            else if (url.contains("filename") && url.contains(".mp4")){
                String decode = URLDecoder.decode(url, "utf-8");
                Pattern pattern = Pattern.compile("filename.*=.*\\.mp4");
                Matcher matcher = pattern.matcher(decode);
                if (matcher.find())
                    video = new SniffingVideo(url, "video/mp4", 0, "mp4");
            }
            else{
                content = Util.getContent(url, headers);
                String s = content[1].toString().toLowerCase(Locale.ROOT);
                log(url + "|" + s);
                if (s.equals("filtered")){
                    //do nothing
                }else if (s.equals("video/x-flv") || s.equals("video/flv")){
                    video = new SniffingVideo(url, s, 0, "flv");
                }
                //常规mp4
                else if (s.equals("video/mp4")){
                    video = new SniffingVideo(url, s, 0 ,"mp4");
                }
                //avi
                else if (s.equals("video/avi")){
                    video = new SniffingVideo(url, s, 0 ,"avi");
                }
                //未知contentType的二进制流
                else if (s.equals("application/octet-stream")) {
                    String e = getElement(url);
                    //如果后缀为.mp4
                    if (e.equals(".mp4"))
                        video = new SniffingVideo(url, s, 0, "mp4");
                    //如果没有后缀则需要判断大小，一般小文件就忽略即可
                    else if (Util.getContentLength(url) >= 1024 * 1024 *2)
                        video = new SniffingVideo(url, s, 1, "mp4");
                    else if (url.contains(".m3u8"))
                        video = new SniffingVideo(url, s, 0, "m3u8");
                }
                //MPEG格式的媒体文件，包括mpeg-url(HLS流)和mpeg-mp4类
                else if((s.contains("video") || s.contains("mpeg"))){
                    String type = "mp4";
                    if (url.contains("m3u8"))
                        type = "m3u8";
                    else if (s.contains("mpegurl"))
                        type = "m3u8";
                    video = new SniffingVideo(url, s, 0, type);
                }
                //直接将m3u8链接打印出来的html
                else if (s.contains("text/html") && (url.endsWith(".m3u8") || url.contains(".m3u8?")) && !url.contains("?url=")){
                    video = new SniffingVideo(url, s, 0, "m3u8");
                }
                //常用伪装格式，图像格式通常为fuck的image
                else if ((s.contains("image") && s.contains("fuck")) || (s.contains("image") && url.contains(".m3u8"))  || (s.isEmpty() && url.contains(".mp4")) || s.equals("image/fuck/you") || (url.contains("renrenmi") && url.endsWith(".m3u8"))){
                    video = new SniffingVideo(url, s, 0, url.contains("m3u8")?"m3u8":"mp4");
                }


            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (video == null){
            if (content != null && content[0] != null)
                video = SniffingVideo.createNoneVideoSniffVideo(url, (String) content[2], (String) content[1], (Connection.Response) content[0]);
            else
                video = SniffingVideo.createNullSniffVideo();
        }else {

            boolean hasReferer = headers.containsKey("Referer");
            boolean hasOrigin = headers.containsKey("Origin");
            HttpReferer referer = HttpReferer.getInstance(video.getUrl(), video.getUrl());
            Map<String, String> maps = referer.getMap();
            if ((hasReferer || hasOrigin) && maps.size() == 0){
                //去除浏览器才需要的参数
                if (hasReferer && hasOrigin) {
                    maps = new HashMap<>(2);
                    maps.put("Referer", headers.get("Referer"));
                    maps.put("Origin", headers.get("Origin"));
                }else {
                    maps = new HashMap<>(1);
                    if (hasReferer)
                        maps.put("Referer", headers.get("Referer"));
                    else
                        maps.put("Origin", headers.get("Origin"));
                }
                video.addHeaders(maps);
            }else if (maps.size() > 0)
                video.addHeaders(maps);
        }

        return video;
    }

    private String getElement(String url){
        url = url.replaceAll("\\?.*", "");
        int last = url.lastIndexOf("/");
        if (last < url.length()){
            url = url.substring(last);
        }
        if (url.contains(".")){
            return url.substring(url.lastIndexOf("."));
        }
        return "none";
    }
}
