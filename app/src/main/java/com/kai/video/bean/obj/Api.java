package com.kai.video.bean.obj;

import com.danikula.videocache.IPTool;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class Api {
    static JSONObject apis = null;
    static int size = 0;
    public static void loadApis(){
        new Thread(() -> {
            try {
                Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/json")
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .method(Connection.Method.GET)
                        .execute();
                apis = new JSONObject(response.body());
            }catch (Exception e){
                e.printStackTrace();
                apis = null;
            }


        }).start();

    }

    public static int getSize() {
        return size;
    }

    public static void setSize(int size) {
        Api.size = size;
    }

    public static JSONObject getApis(){
        if (apis != null)
            return apis;
        List<FutureTask<Integer>> futureTasks = new ArrayList<>();
        futureTasks.add(new FutureTask<>(() -> {
            try {
                Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/json")
                        .method(Connection.Method.GET)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .execute();
                apis = new JSONObject(response.body());
            } catch (Exception e) {
                e.printStackTrace();
                apis = null;
            }


        }, 1));
        for (Future<Integer> future : futureTasks){
            try {
                future.get();
            }catch (Exception ignored){

            }
        }
        if (apis != null)
            return apis;
        else
            return new JSONObject();
    }
}
