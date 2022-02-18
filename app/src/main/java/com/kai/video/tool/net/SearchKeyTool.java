package com.kai.video.tool.net;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

/*
获取热搜关键字和搜索相关关键字
 */
public class SearchKeyTool {
    public static List<String> defaultList = new ArrayList<>();
    public static void init(){
        new Thread(() -> {
            try {
                defaultList.clear();
                Connection.Response response = Jsoup.connect("https://node.video.qq.com/x/api/hot_search/?callback=&channelId=0&otype=json")
                        .method(Connection.Method.GET)
                        .ignoreContentType(true)
                        .execute();
                JSONArray array = JSONObject.parseObject(response.body()).getJSONObject("data")
                        .getJSONObject("mapResult").getJSONObject("0").getJSONArray("listInfo");
                for (Object object:array) {
                    JSONObject obj = (JSONObject) object;
                    defaultList.add(obj.getString("title"));
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();


    }
    /*
    Must use in a child thread
     */
    public static List<String> search(String wd){
        List<String> result = new ArrayList<>();
        try {
            Connection.Response response = Jsoup.connect("https://suggest.video.iqiyi.com/")
                    .data("key", wd)
                    .data("platform", "11")
                    .data("rltnum", "30")
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .execute();
            org.json.JSONObject object = new org.json.JSONObject(response.body());
            org.json.JSONArray array = object.getJSONArray("data");
            for(int i = 0; i < array.length(); i++) {
                result.add(array.getJSONObject(i).getString("name"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;



    }
}
