package com.kai.video.manager;

import com.kai.sniffwebkit.sniff.SniffingVideo;

import java.util.Map;

public class UserAgentManager {
    public static void init(SniffingVideo video){
        Map<String, String> headers = video.getHeaders();
        //if (headers.containsKey("User-Agent"))

    }
}
