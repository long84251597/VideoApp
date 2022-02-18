package com.kai.video.tool.net;

import android.app.Activity;
import android.util.Log;

import com.danikula.videocache.IPTool;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class LoginTool {

    public static void login(Activity activity, String username, String password, OnLogin onLogin){
        new Thread(() -> {
            try {
                Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/login")
                        .data("action", "login")
                        .data("mobile", username)
                        .data("password", password)
                        .data("tv", "true")
                        .method(Connection.Method.GET)
                        .ignoreContentType(true)
                        .execute();
                Log.e("result", response.body() + "s");
                if (response.body().contains("right"))
                    activity.runOnUiThread(onLogin::success);

                else
                    activity.runOnUiThread(onLogin::fail);

            }catch (Exception e){
                e.printStackTrace();
                activity.runOnUiThread(onLogin::fail);
            }

        }).start();
    }

    public interface OnLogin{
        void success();
        void fail();
    }
}
