package com.kai.video.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.IPTool;
import com.kai.video.R;
import com.kai.video.adapter.HistoryItemAdapter;
import com.kai.video.manager.DeviceManager;
import com.kai.video.tool.application.SPUtils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends BaseActivity {
    ProgressBar progressBar;
    RecyclerView recyclerView;
    HistoryItemAdapter adapter;
    private List<HistoryItem> items = new ArrayList<>();
    private int col = 2;
    @Override
    protected int getTvLayout() {
        return R.layout.activity_history;
    }

    @Override
    protected int getPadLandLayout() {
        return R.layout.activity_history;
    }

    @Override
    protected int getPadPortLayout() {
        return R.layout.activity_history;
    }

    @Override
    protected int getPhonePortLayout() {
        return R.layout.activity_history;
    }

    @Override
    protected int getPhoneLandLayout() {
        return R.layout.activity_history;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressBar = findViewById(R.id.progress);
        recyclerView = findViewById(R.id.video_list);
        adapter = new HistoryItemAdapter(items);
        recyclerView.setLayoutManager(new GridLayoutManager(this,col));
        recyclerView.setAdapter(adapter);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/HistoryServlet")
                            .data("action", "get")
                            .data("username", SPUtils.get(HistoryActivity.this).getValue("username", ""))
                            .ignoreContentType(true)
                            .execute();
                    JSONObject object = JSONObject.parseObject(response.body());
                    Log.e("a", object.toJSONString());
                    if (object.getBooleanValue("success")){
                        items = object.getJSONArray("history").toJavaList(HistoryItem.class);
                        adapter.setItems(items);
                    }
                    runOnUiThread(new Runnable() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static class HistoryItem{
        private String name;
        private String videoType;
        private String coverPic;
        private String time;
        private String videoTitle;
        private String url;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setCoverPic(String coverPic) {
            this.coverPic = coverPic;
        }

        public String getCoverPic() {
            return coverPic;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getTime() {
            return time;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setVideoTitle(String videoTitle) {
            this.videoTitle = videoTitle;
        }

        public String getVideoTitle() {
            return videoTitle;
        }

        public void setVideoType(String videoType) {
            this.videoType = videoType;
        }

        public String getVideoType() {
            return videoType;
        }
    }
}