package com.kai.video.tool.net;

import android.app.Activity;

import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.IPTool;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class LoginTool {

    public static void login(Activity activity, String username, String password, OnLogin onLogin){
        new Thread(() -> {
            try {
                Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/login")
                        .data("action", "getvip")
                        .data("mobile", username)
                        .data("password", password)
                        .method(Connection.Method.GET)
                        .ignoreContentType(true)
                        .execute();
                JSONObject resultObj = JSONObject.parseObject(response.body());
                if (resultObj.getBooleanValue("success")) {
                    activity.runOnUiThread(() -> onLogin.success(resultObj.getBooleanValue("vip")));
                }
                else
                    activity.runOnUiThread(onLogin::fail);

            }catch (Exception e){
                e.printStackTrace();
                activity.runOnUiThread(onLogin::fail);
            }

        }).start();
    }

    public interface OnLogin{
        void success(boolean vip);
        void fail();
    }
}
