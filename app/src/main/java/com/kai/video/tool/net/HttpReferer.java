package com.kai.video.tool.net;

import java.util.HashMap;
import java.util.Map;

public class HttpReferer {
    private final String url;
    private final String video;
    private HttpReferer(String url, String video){
        this.url = url;
        this.video = video;
    }
    public static HttpReferer getInstance(String url, String video){
        return new HttpReferer(url, video);
    }

    public Map<String, String > getMap(){
        if (video.contains("bilivideo") || video.contains("upgcxcode")){
            Map<String, String> map = new HashMap<>(5);
            map.put("User-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_16_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36");
            map.put("Authority", "upos-sz-mirrorkodoo1.bilivideo.com");
            map.put("Origin", "https://okjx.superchen.top:3389");
            map.put("Accept-language", "zh-CN,zh;q=0.9");
            map.put("Referer", "https://okjx.superchen.top:3389/?url=" + url);
            return map;
        }else if (video.contains("zy.acampt.com") || video.contains("qie2.suipq.com")){
            Map<String, String> map = new HashMap<>(2);
            map.put("Origin", "https://jhpc.manduhu.com");
            map.put("Referer", "https://jhpc.manduhu.com/jianghu.php?url=" + url);
            return map;
        }else if (video.contains("wy.bigmao.top") && video.endsWith("mp4")){
            Map<String, String> map = new HashMap<>(2);
            map.put("Rederer", "https://www.nfmovies.com/js/player/m3u8.html?" + System.currentTimeMillis());
            map.put("Range", "bytes=0-1");
            return map;
        }
        return new HashMap<>(0);
    }
}
