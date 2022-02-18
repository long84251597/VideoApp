package com.danikula.videocache;

import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

public class IPTool {
    private static String ip = "http://124.223.67.180/video";
    public static String getLocal(){
        return ip;
    }
    public static void load(Runner runner){
        //从github上检索最新服务器名称
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        Connection.Response response = Jsoup.connect("http://124.223.67.180/video/live.json")
                                .ignoreContentType(true).execute();
                        if (JSONObject.parseObject(response.body()).getBoolean("live")) {
                            runner.onSuccess();
                            return;
                        }
                    }catch (Exception e){

                    }

                    Connection.Response response = Jsoup.connect("https://kaihuang666.github.io/")
                            .ignoreContentType(true).execute();
                    ip = JSONObject.parseObject(response.body()).getString("ip");
                    Log.e("current server ip", ip);
                    runner.onSuccess();
                } catch (Exception e) {
                    e.printStackTrace();
                    runner.onSuccess();
                }
            }
        }).start();
    }
    public interface Runner{
        void onSuccess();
    }

}
