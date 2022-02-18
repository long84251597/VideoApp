package com.kai.video.bean.obj;

import android.content.Context;

import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.IPTool;
import com.kai.video.tool.application.SPUtils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class History {
    private String username = "";
    private final String videoType;
    private final String name;
    private final int type;
    public History(Context context, String name, String videoType, int type){
        this.name = name;
        this.videoType = videoType;
        this.type = type;
        username = SPUtils.get(context).getValue("username", "");
    }
    public void updateCurrent(String current, String url){
        if (username.isEmpty())
            return;
        try {
            Jsoup.connect(IPTool.getLocal() + "/history")
                    .data("action", "updateCurrent")
                    .data("name", name)
                    .data("username", username)
                    .data("videoType", videoType)
                    .data("current", current)
                    .data("url", url)
                    .data("type", String.valueOf(type))
                    .method(Connection.Method.POST)
                    .ignoreContentType(true)
                    .execute();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public JSONObject getCurrent(String current, String url){
        if (username.isEmpty())
            return new JSONObject();
        try {
            Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/history")
                    .data("action", "current")
                    .data("name", name)
                    .data("username", username)
                    .data("videoType", videoType)
                    .data("current", current)
                    .data("type", String.valueOf(type))
                    .data("url", url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .execute();
            return JSONObject.parseObject(response.body());
        }catch (Exception e){
            e.printStackTrace();
            return new JSONObject();
        }
    }
    public long getTime(String url){
        if (username.isEmpty())
            return 0;
        try {
            Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/history")
                    .data("action", "time")
                    .data("username", username)
                    .data("url", url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .execute();
            JSONObject object = JSONObject.parseObject(response.body());
            if (object.getBoolean("success")){
                return object.getLong("time");
            }else {
                updateTime(url, 0);
            }

        }catch (Exception e){
            e.printStackTrace();

        }
        return 0;
    }
    public void deleteLog(String website, String name){
        new Thread(() -> {
            try {
                Jsoup.connect(IPTool.getLocal() + "/history")
                        .data("action", "deleteLog")
                        .data("website", website)
                        .data("username", username)
                        .data("name", name)
                        .method(Connection.Method.POST)
                        .ignoreContentType(true)
                        .execute();

            }catch (Exception e){
                e.printStackTrace();

            }
        }).start();
    }
    private long lastTime = 0;
    public void updateTime(String url, long time){
        if (username.isEmpty() || time <= 0 || lastTime == time)
            return;
        lastTime = time;
        try {
            Jsoup.connect(IPTool.getLocal() + "/history")
                    .data("action", "updateTime")
                    .data("username", username)
                    .data("url", url)
                    .data("time", String.valueOf(time))
                    .method(Connection.Method.POST)
                    .ignoreContentType(true)
                    .execute();


        }catch (Exception e){
            e.printStackTrace();

        }
    }
    public static JSONObject getAll(Context context){
        try {
            Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/history")
                    .data("action", "get")
                    .data("username", SPUtils.get(context).getValue("username", ""))
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .execute();
            return JSONObject.parseObject(response.body());
        }catch (Exception e){
            e.printStackTrace();
            return new JSONObject();
        }
    }

}
