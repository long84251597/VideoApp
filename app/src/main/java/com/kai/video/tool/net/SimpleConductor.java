package com.kai.video.tool.net;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.IPTool;
import com.kai.video.bean.obj.Commend;
import com.kai.video.manager.DeviceManager;
import com.kai.video.bean.item.NaviItem;
import com.kai.video.tool.log.LogUtil;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;


public class SimpleConductor {
    private String action;
    private final List<NaviItem> list = new ArrayList<>();
    private final List<FutureTask<Integer>> tagTasks = new ArrayList<>();
    private final List<String> types = new ArrayList<>();

    public List<String> getTypes() {
        return types;
    }

    public SimpleConductor(String action){
        this.action = action;
    }
    public void search(final String key,final OnSearchListener onSearchListener){
         new Thread(() -> {
             try {
                 Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/SearchServlet")
                         .data("wd", key)
                         .method(Connection.Method.GET)
                         .timeout(10 * 1000)
                         .execute();
                 onSearchListener.onSearch(JSONObject.parseObject(response.body()).getJSONArray("data"));
             }catch (Exception e){
                 e.printStackTrace();
             }


         }).start();
    }

    private void run(final String action){

        FutureTask<Integer> task = new FutureTask<>(() -> {
            try {

                Connection.Response response = Jsoup.connect(IPTool.getLocal() +  "/HotList")
                        .data("action", action)
                        .method(Connection.Method.GET)
                        .execute();
                JSONObject result = JSONObject.parseObject(response.body());
                List<NaviItem> items = result.getJSONArray("data").toJavaList(NaviItem.class);
                if (items.size() == 0) {
                    return;
                }
                List<NaviItem> additions = new ArrayList<>();
                additions.addAll(items);
                int ems = DeviceManager.isTv() ? 7 : 4;
                int rem = ems - items.size() % ems;
                for (int i = 0; i < rem; i++) {
                    additions.add(null);
                }
                for(Object o : result.getJSONArray("types")){
                    types.add((String) o);
                }
                list.addAll(additions);

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
        types.clear();
        run(action);
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
