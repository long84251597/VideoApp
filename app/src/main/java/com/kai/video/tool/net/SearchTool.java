package com.kai.video.tool.net;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.IPTool;
import com.kai.video.activity.PlayAcivity;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchTool {
    public void setWd(String wd) {
        if (wd == null)
            wd = "";
        this.wd = wd;
    }
    private boolean special = false;
    private int offset = 1;
    private String wd;
    private String type = "";
    private String api = "1";
    private String summary = "";
    private List<SearchItem> searchItems = new ArrayList<>();
    private OnFetchListner onFetchListner;
    private boolean useOffset = false;

    public void setUseOffset(boolean useOffset) {
        this.useOffset = useOffset;
    }

    public void setOnFetchListner(OnFetchListner onFetchListner) {
        this.onFetchListner = onFetchListner;
    }

    public void setApi(String api) {
        if (api == null)
            api = "1";
        this.api = api;
    }

    public void setType(String type) {
        if (type == null)
            type = "";
        this.type = type;
    }

    public String getApi() {
        return api;
    }

    public String getType() {
        return type;
    }

    public static SearchTool getInstance(Context context, String name){
        return new SearchTool(context, name);
    }
    private SearchTool(Context context, String name){
        this.wd = name;
    }

    public String getWd() {
        return wd;
    }

    public String getSummary() {
        return summary;
    }

    public void more(String offset){
        useOffset = true;
        this.offset = Integer.parseInt(offset);
        more();
    }
    public void more(){
        new Thread(() -> {
            try {

                Connection.Response response = Jsoup.connect(IPTool.getLocal() +  "/quicksearch")
                        .data("wd", wd)
                        .data("action", special?"fetch":"search")
                        .data("count", String.valueOf(offset))
                        .data("type", type)
                        .data("api", api)
                        .method(Connection.Method.GET)
                        .execute();
                JSONObject object = JSONObject.parseObject(response.body());
                searchItems = object.getJSONArray("data").toJavaList(SearchItem.class);
                handler.post(() -> {
                    summary = "共搜索到" + searchItems.size() + "部影片";
                    Log.i("tag", "item.size = " + searchItems.size());
                    onFetchListner.onFetched(-1, searchItems, object.getBoolean("more"));
                    //不使用页数就自动加一
                    if (!useOffset)
                        offset++;
                });

            }catch (Exception e){
                e.printStackTrace();
                handler.post(() -> onFetchListner.onFetchFail());

            }

        }).start();
    }

    public void setSpecial(boolean special) {
        this.special = special;
    }

    public boolean isSpecial() {
        return special;
    }
    public void fetch(){
        offset = 1;
        new Thread(() -> {
            try {

                Connection.Response response = Jsoup.connect(IPTool.getLocal() +  "/quicksearch")
                        .data("wd", wd)
                        .data("action", special?"fetch":"search")
                        .data("count", String.valueOf(offset))
                        .data("type", type)
                        .data("api", api)
                        .method(Connection.Method.GET)
                        .execute();
                JSONObject object = JSONObject.parseObject(response.body());
                searchItems = object.getJSONArray("data").toJavaList(SearchItem.class);
                handler.post(() -> {
                    try {
                        summary = "共搜索到" + searchItems.size() + "部影片";
                        Log.i("tag", "item.size = " + searchItems.size());
                        onFetchListner.onFetched(object.getIntValue("pageCount"), searchItems, object.getBoolean("more"));
                        if (!useOffset)
                            offset++;
                    }catch (Exception e){
                        onFetchListner.onFetchFail();
                    }

                });

            }catch (Exception e){
                e.printStackTrace();
                handler.post(() -> onFetchListner.onFetchFail());

            }

        }).start();
    }
    private final Handler handler = new Handler();

    public interface OnFetchListner{
        void onFetched(int page, List<SearchItem> items, boolean more);
        void onFetchFail();
    }
    public interface OnConnectListner{
        void onConnected(String href, String title);
        void onConnected(List<String> names, List<String> hrefs);
        void onDisConnected();
    }
    public SearchJtem createJtem(){
        return new SearchJtem();
    }

    public static class PlayItem{
        private String url;
        private String type;

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
    public static class SearchItem{
        private String poster = "";
        private String score = "";
        private String name = "";
        private String year = "";
        private String id = "";
        private String type = "";
        private boolean special = false;

        public void setSpecial(boolean special) {
            this.special = special;
        }

        public boolean isSpecial() {
            return special;
        }

        private List<PlayItem> playList = new ArrayList<>();
        public SearchItem(){
        }

        public void setPlayList(List<PlayItem> playList) {
            this.playList = playList;
        }

        public List<PlayItem> getPlayList() {
            return playList;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public String getYear() {
            return year;
        }


        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setPoster(String poster) {
            this.poster = poster;
        }

        public String getPoster() {
            return poster;
        }

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void location(OnConnectListner onConnectListner){
            if (isSpecial()){
                onConnectListner.onConnected(id, name);
                return;
            }
            List<String> names = new ArrayList<>();
            List<String> urls = new ArrayList<>();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/quicksearch")
                                .data("action", "info")
                                .data("id", id)
                                .execute();
                        setPlayList(JSONArray.parseArray(response.body()).toJavaList(PlayItem.class));
                        for (PlayItem item : getPlayList()) {
                            names.add(item.getType());
                            urls.add(item.getUrl());
                        }
                        if (names.size() > 0)
                            onConnectListner.onConnected(names, urls);
                        else
                            onConnectListner.onDisConnected();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }

    }
    public static class SearchJtem{
        private final Map<String, String> startMap = new HashMap<>();
        private String img = "";
        private String year = "";
        private String href = "";
        private String title = "";
        private String rate = "";
        private Handler handler;
        public SearchJtem(){
            startMap.put("腾讯", "");
            startMap.put("爱奇艺", "");
            startMap.put("芒果TV", "");
            startMap.put("优酷", "");
            startMap.put("哔哩哔哩", "");
        }

        public void setYear(String year) {
            this.year = year;
        }

        public String getYear() {
            return year;
        }

        public void setImg(String img) {
            this.img = img;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setRate(String rate) {
            this.rate = rate;
        }

        public String getTitle() {
            return title;
        }

        public String getHref() {
            return href;
        }

        public String getImg() {
            return img;
        }

        public String getRate() {
            return rate;
        }
        private String getHref(String doc) {
            Pattern pattern = Pattern.compile("https?://www.douban.com/link2/\\?url=(.*)\",");
            Matcher matcher = pattern.matcher(doc);
            if (matcher.find()) {
                return URLDecoder.decode(matcher.group(1)).replace("http:", "https:");
            }
            return null;
        }
        public void location(OnConnectListner onConnectListner){
            handler = new Handler();
            new Thread(() -> {
                try {
                    Document document = Jsoup.connect(href)
                            .get();
                    Element element = JsoupHelper.search(document, "id:videoApp/action:next");
                    Pattern pattern = Pattern.compile("window.__INITIAL_STATE__=(\\{.*\\});");
                    Matcher matcher = pattern.matcher(element.outerHtml());
                    List<String> sites = new ArrayList<>();
                    List<String> names = new ArrayList<>();
                    if (matcher.find()) {
                        JSONObject object = JSONObject.parseObject(matcher.group(1));
                        JSONArray array = object.getJSONObject("detail").getJSONObject("itemData").getJSONArray("playInfo");

                        for(int i = 0; i < array.size(); i++) {
                            JSONObject obj =  array.getJSONObject(i);
                            String name = obj.getString("siteName");
                            String ourl =obj.getString("ourl");
                            String site = obj.getString("site");
                            if (!startMap.containsKey(name)){
                                continue;
                            }
                            if (ourl.startsWith("/vc/np")) {
                                document = Jsoup.connect("https://v.sogou.com" + ourl).get();
                                pattern = Pattern.compile("window.open\\(\\'(.*)\\',");
                                matcher = pattern.matcher(document.getElementsByTag("script").first().outerHtml());
                                if (matcher.find()) {
                                    sites.add(matcher.group(1));
                                    names.add(name + "  [推荐]");
                                }
                            }else {
                                sites.add(ourl);
                                names.add(name + "[推荐]");
                            }
                        }
                    }
                    if (names.size() > 0) {
                        handler.post(() -> onConnectListner.onConnected(names, sites));
                    }else {
                        handler.post(onConnectListner::onDisConnected);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    handler.post(onConnectListner::onDisConnected);
                }
            }).start();
        }

    }
}
