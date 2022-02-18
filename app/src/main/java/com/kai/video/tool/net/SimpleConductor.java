package com.kai.video.tool.net;

import android.util.Log;

import com.danikula.videocache.IPTool;
import com.kai.video.bean.obj.Commend;
import com.kai.video.manager.DeviceManager;
import com.kai.video.bean.item.NaviItem;
import com.kai.video.tool.log.LogUtil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;


public class SimpleConductor {
    private String action = "tv";
    private final List<NaviItem> list = new ArrayList<>();
    private final List<FutureTask<Integer>> tagTasks = new ArrayList<>();
    private final List<String> types = new ArrayList<>();

    public List<String> getTypes() {
        return types;
    }

    public SimpleConductor(String action){
        this.action = action;
        switch (action) {
            case "tv":
                types.add("tencent");
                types.add("iqiyi");
                types.add("mgtv");
                types.add("bilibili");
                break;
            case "film":
                types.add("mgtv");
                types.add("bilibili");
                types.add("tencent");
                types.add("iqiyi");

                break;
            case "cartoon":
                types.add("tencent");
                types.add("bilibili");
                types.add("bilibili1");
                types.add("iqiyi");


                break;
            case "zy":
                types.add("tencent");
                types.add("mgtv");
                types.add("bilibili");
                types.add("iqiyi");

                break;
        }
    }
    public SimpleConductor(){

    }
    public void search(final String key,final OnSearchListener onSearchListener){
         new Thread(() -> {
             try {
                 Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/SearchServlet")
                         .data("wd", key)
                         .method(Connection.Method.GET)
                         .timeout(10 * 1000)
                         .execute();
                 onSearchListener.onSearch(new JSONObject(response.body()).getJSONArray("data"));
             }catch (Exception e){
                 e.printStackTrace();
             }


         }).start();
    }

    private void run(final String action, final String type){

        FutureTask<Integer> task = new FutureTask<>(() -> {
            try {

                Connection.Response response = Jsoup.connect(IPTool.getLocal() +  "/HotList")
                        .data("action", type)
                        .data("type", action)
                        .method(Connection.Method.GET)
                        .execute();
                List<NaviItem> items = com.alibaba.fastjson.JSONObject.parseObject(response.body()).getJSONArray("data").toJavaList(NaviItem.class);
                if (items.size() == 0) {
                    Log.i("tag", type);
                    return;
                }
                List<NaviItem> additions = new ArrayList<>();
                Commend commend = new Commend(type, action);
                additions.add(commend.getDocument());
                additions.addAll(items);
                int ems = DeviceManager.isTv() ? 7 : 4;
                int rem = ems - items.size() % ems;
                for (int i = 0; i < rem; i++) {
                    additions.add(null);
                }
                list.addAll(additions);
                if (action.equals("japanese"))
                    types.add("bilibili1");
                else
                    types.add(type);
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("tag", e.toString());
            }
        }, 1);
        tagTasks.add(task);
        new Thread(task).start();
    }

    public void get(final OnGetListener onGetListener){
        list.clear();
        tagTasks.clear();
        LogUtil.d("TAG", "从服务器获取视频汇总");
        for(String type: types){
            if (action.equals("cartoon") && type.equals("bilibili")){
                run("chinese", "bilibili");
                continue;
            }
            if (action.equals("cartoon") && type.equals("bilibili1")){
                run("japanese", "bilibili");
                continue;
            }
            run(action, type);
        }
        types.clear();
        for (FutureTask<Integer> task:tagTasks) {
            try {
                task.get();
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        onGetListener.onFinish(list);


    }
    public interface OnGetListener{
        void onFinish(List<NaviItem> list);
    }
    public interface OnSearchListener{
        void onSearch(JSONArray list);
    }
}
