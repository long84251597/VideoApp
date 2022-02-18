package com.kai.video.manager;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson.JSONObject;
import com.kai.sniffwebkit.sniff.SniffingVideo;
import com.kai.video.activity.InfoActivity;
import com.kai.video.R;
import com.kai.video.activity.SniffActivity;
import com.kai.video.adapter.SelectionItemAdapter;
import com.kai.video.bean.obj.Api;
import com.kai.video.bean.obj.Info;
import com.kai.video.bean.obj.Quality;
import com.kai.video.bean.obj.Selection;
import com.kai.video.bean.item.VideoSaver;
import com.kai.video.view.other.LinearTopSmoothScroller;
import com.kai.video.tool.log.LogUtil;
import com.kai.video.tool.net.VideoTool;
import com.kai.video.view.dialog.SelectionDialog;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView;

import org.litepal.LitePal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
分离主线程以外的视频播放器操作
 */
public class PlayerManager {
    private InfoActivity activity;
    /*
    自动全屏操作
     */

    public Handler fullHandler = new Handler();
    private final Runnable fullRunnable = new Runnable() {
        @Override
        public void run() {
            if (!activity.player.isIfCurrentIsFullscreen() && DeviceManager.isTv() & !activity.player.isRelease()) {
                activity.player.getFullscreenButton().callOnClick();
            }

        }
    };

    public void destory(){
        fullHandler.removeCallbacksAndMessages(null);
        otherHandler.removeCallbacksAndMessages(null);
        fullHandler = null;
        otherHandler = null;
        activity = null;

    }
    public void cancelFullTimer(){
        fullHandler.removeCallbacks(fullRunnable);
    }
    /*
    自动更换线路操作
     */
    private final static int MESSAGE_CHANGE_API = 0;
    private final static int MESSAGE_RETRY = 1;

    @SuppressLint("HandlerLeak")
    private final Handler timeoutHandler = new Handler(){};
    private final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            boolean prepared = false;
            if (activity.player.isIfCurrentIsFullscreen()){
                prepared = activity.player.getcurrentPlayer().isPrepared();
            }else {
                prepared = activity.player.isPrepared();
            }
            if (prepared)
                changeOtherApi();
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler otherHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            activity.player.getCurrentPlayer().release();
            activity.player.setRelease(true);
            fullHandler.removeCallbacks(fullRunnable);
            otherHandler.removeCallbacksAndMessages(null);

            if (msg.what == MESSAGE_CHANGE_API){
                changeOtherApi();
            }else if (msg.what == MESSAGE_RETRY){
                activity.videoTool.reTry();
            }
        }
    };

    private PlayerManager(InfoActivity activity){
        this.activity = activity;

    }
    public static PlayerManager getInstance(InfoActivity activity){
        return new PlayerManager(activity);
    }

    public void initPlayer(){
        //初始化播放器设置
        activity.player.setDismissControlTime(4000);
        activity.player.initSputils(activity);
        activity.player.setIsTouchWiget(true);
        //关闭自动旋转
        activity.player.setRotateViewAuto(true);
        activity.player.setAutoFullWithSize(true);
        activity.player.setLockLand(false);
        activity.player.setShowFullAnimation(false);
        activity.player.setNeedLockFull(true);
        activity.player.getFullscreenButton().setOnClickListener(v -> {
            //直接横屏
            //初始化不打开外部的旋转
            if (!DeviceManager.isTv())
                activity.orientationUtils.resolveByClick();
            //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
            activity.player.startWindowFullscreen(activity, true, true);
        });
        activity.player.setVideoAllCallBack(new GSYSampleCallBack() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                List<VideoSaver> savers = LitePal.where("url = ?", activity.getUrl()).order("api").find(VideoSaver.class);
                VideoSaver saver = null;
                if (savers.size() > 0)
                    saver = savers.get(0);
                if (saver == null)
                    saver = new VideoSaver();
                saver.setUrl(activity.getUrl());
                saver.setApi(activity.spUtils.getValue("api", 0));
                saver.save();
                fullHandler.postDelayed(fullRunnable, 1000);
                LogUtil.d("tag", activity.player.getCurrentPlayer().getCurrentVideoHeight() + "x" + activity.player.getCurrentPlayer().getCurrentVideoWidth());
                Quality quality = new Quality(activity.player.getCurrentPlayer().getCurrentVideoWidth());
                activity.player.getcurrentPlayer().setQuality(quality.get());
                if (quality.getLevel() < 5){
                    AlertDialog dialog = new AlertDialog.Builder(activity)
                            .setTitle("视频清晰度低")
                            .setMessage("当前视频清晰度低，可能会影响观看效果。是否需要切换下一个接口？")
                            .setPositiveButton("确定", (dialog12, which) -> otherHandler.sendEmptyMessage(MESSAGE_RETRY))
                            .setNegativeButton("取消", (dialog1, which) -> {

                            })
                            .create();
                    dialog.show();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.getResources().getColor(R.color.colorPrimary));
                    dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(activity.getResources().getColor(R.color.colorPrimary));
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();

                }
            }

            @Override
            public void onPlayError(String url, Object... objects) {
                super.onPlayError(url, objects);
                //继续嗅探列表中的其他视频，如果列表中没有了就切换下一个接口
                otherHandler.sendEmptyMessage(MESSAGE_RETRY);
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
                if (activity.adapter.getCurrent() < activity.adapter.getAllCount() - 1) {
                    activity.player.setRelease(true);
                    activity.player.release();
                    activity.player.getcurrentPlayer().release();
                    activity.player.getcurrentPlayer().setRelease(true);
                    switchNext();
                }
                else
                    Toast.makeText(activity, "已经是最后一集了", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onClickStartError(String url, Object... objects) {
                super.onClickStartError(url, objects);

            }

            @Override
            public void onEnterFullscreen(String url, Object... objects) {

                activity.fullButton.clearFocus();
                full(true);
                if (DeviceManager.isTv() && activity.player.getCurrentState() == GSYVideoView.CURRENT_STATE_PAUSE)
                    activity.player.getcurrentPlayer().getStartButton().callOnClick();
                super.onEnterFullscreen(url, objects);

            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                super.onQuitFullscreen(url, objects);
                activity.fullButton.requestFocus();
                full(false);
                activity.layoutManager.scrollToPositionWithOffset(activity.adapter.getCurrent(), 0);
                if (DeviceManager.isTv())
                    activity.player.onVideoPause();
                activity.orientationUtils.setEnable(false);
            }

            @Override
            public void onEnterSmallWidget(String url, Object... objects) {
                super.onEnterSmallWidget(url, objects);
            }

            @Override
            public void onQuitSmallWidget(String url, Object... objects) {
                super.onQuitSmallWidget(url, objects);
            }
        });
    }
    private void full(boolean enable) {
        if (DeviceManager.isTv()) {
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            return;
        }
        if (enable) {
            WindowManager.LayoutParams lp =  activity.getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            activity.getWindow().setAttributes(lp);
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            WindowManager.LayoutParams attr = activity.getWindow().getAttributes();
            attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getWindow().setAttributes(attr);
            //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }
    private boolean first = false;
    private void changeOtherApi(){
        new Thread(() -> {
            int new_api = activity.spUtils.getValue("api", 0);
            if (!first) {
                new_api = 1;
                first = true;
            }else
                new_api++;
            //已经超出界限
            Log.i("tag", new_api+ "");
            if (new_api >= activity.apiNames.size()) {
                new_api = 0;
                final String apiName = activity.apiNames.get(new_api).replaceAll("\\[.*", "");
                fullHandler.post(() -> {
                    activity.changeButton.setText(apiName);
                    Toast.makeText(activity, "请尝试使用全网搜索观看", Toast.LENGTH_SHORT).show();
                    Intent intent= new Intent(activity, SniffActivity.class);
                    intent.putExtra("wd", activity.videoTool.getInfo().getName());
                    activity.startActivity(intent);
                });
                new_api = 0;
                activity.spUtils.putValue("api", new_api );
                return;
            }
            //回到起始点结束解析
            activity.spUtils.putValue("api", new_api);
            final String apiName = activity.apiNames.get(new_api).replaceAll("\\[.*", "");
            fullHandler.post(() -> {
                activity.changeButton.setText(apiName);
                getVideoFromResult();
            });


        }).start();




    }
    public void switchVideo(int i){
        try {
            Selection selection = activity.videoTool.getInfo().getSelections().get(i);
            activity.videoTool.getInfo().setUrl(selection.getUrl());
            activity.videoTool.getInfo().setTitle(selection.getVideoTitle());
            String c = selection.getTitle();
            activity.videoTool.getInfo().setCurrent(c);
            activity.videoTool.getInfo().setCurrentText(c);
            //如果集数中含有非数字
            if (activity.videoTool.getInfo().isZongyi()){
                activity.videoTool.getInfo().setCurrent((i+1) + "");
                activity.videoTool.getInfo().setPname(c);
            }else if (activity.videoTool.getInfo().getType() == Info.TYPE_MOVIE){
                activity.videoTool.getInfo().setCurrent("-1");
            }else
            try {
                Integer.parseInt(c);
            }catch (Exception e){
                e.printStackTrace();
                activity.videoTool.getInfo().setCurrent("-2");
                activity.videoTool.getInfo().setCurrentText(c.replaceAll("\\s", ""));
            }
            getVideoFromResult();
        }catch (Exception e){
            e.printStackTrace();
        }


    }
    public void switchVideo(String newUrl){
        new Thread(() -> {
            try {
                List<Selection> array1 = activity.adapter.getArrayAll();
                for(int i = 0; i < array1.size(); i++){
                    Selection object = array1.get(i);
                    if (object.getUrl().equals(newUrl)){
                        final int index = i;
                        activity.videoTool.getInfo().setTitle(object.getVideoTitle());
                        activity.videoTool.getInfo().setUrl(object.getUrl());
                        String c = object.getTitle();
                        activity.videoTool.getInfo().setCurrent(c);
                        activity.videoTool.getInfo().setCurrentText(c);
                        //如果集数中含有非数字
                        if (activity.videoTool.getInfo().isZongyi()){
                            activity.videoTool.getInfo().setCurrent((i+1) + "");
                            activity.videoTool.getInfo().setCurrent(object.getTitle());
                            activity.videoTool.getInfo().setPname(c);
                        }else if (activity.videoTool.getInfo().getType() == Info.TYPE_MOVIE){
                            activity.videoTool.getInfo().setCurrent("-1");
                        }else
                        try {
                            Integer.parseInt(c);

                        }catch (Exception e){
                            e.printStackTrace();
                            activity.videoTool.getInfo().setCurrent("-2");
                            activity.videoTool.getInfo().setCurrentText(c.replaceAll("\\s", ""));
                        }

                        fullHandler.postDelayed(() -> activity.groupAdapter.changeGroup(index, (group, header, tailer) -> {
                            activity.groupAdapter.setCurrent(group);
                            if (group > 0) {
                                activity.adapter.showList(header, tailer);
                                activity.groupManager.scrollToPositionWithOffset(group, 0);
                            }
                            activity.adapter.changeAbsolutely(index);
                            LinearTopSmoothScroller smoothScroller = new LinearTopSmoothScroller(activity);
                            smoothScroller.setTargetPosition(index - header);
                            activity.layoutManager.startSmoothScroll(smoothScroller);
                            getVideoFromResult();
                        }), 500);
                        break;

                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();

    }
    public void out(String url){
        //支持腾讯视频和B站的正版打开
        if (url.startsWith("https://v.qq.com")){
            Button out = activity.findViewById(R.id.out);

            Pattern pattern = Pattern.compile("v.qq.com/x/cover/(.*)/");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()){
                out.setOnClickListener(v -> {
                    Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse("tenvideo2://?action=1&stay_flag=0&cover_id=" + matcher.group(1)));
                    activity.startActivity(it);
                });

            }
        }

    }
    public void switchNext(){
        new Thread(() -> {
            try {
                SelectionItemAdapter adapter = activity.adapter;
                int offset = adapter.getOffset();
                boolean changePage = offset == adapter.getItemCount()-1;

                int c = adapter.getCurrent();
                if (c == adapter.getAllCount()-1){
                    activity.recyclerView.post(() -> Toast.makeText(activity, "已经是最后一集了", Toast.LENGTH_SHORT).show());
                    return;
                }


                int i = c;
                Selection obj;
                do {
                    obj = adapter.getArrayAll().get(++i);
                }
                while (obj.getType() == 1);
                final int index = i;
                final Selection jsonObject = obj;
                if (changePage){
                    activity.recyclerView.post(() -> activity.groupAdapter.changeGroup(index, (group, header, tailer) -> {
                        activity.groupAdapter.setCurrent(group);
                        adapter.showList(header, tailer);
                        adapter.changeAbsolutely(index);
                        switchVideo(index);
                        Toast.makeText(activity, "播放下一集", Toast.LENGTH_SHORT).show();
                        activity.layoutManager.scrollToPositionWithOffset(0, 0);
                    }));
                }else
                fullHandler.post(() -> {
                    adapter.changeAbsolutely(index);
                    switchVideo(index);
                    Toast.makeText(activity, "播放下一集", Toast.LENGTH_SHORT).show();
                });
                activity.videoTool.getHistoryManager().updateTime(activity.videoTool.getInfo().getUrl(), 0);


            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();


    }
    private void loadApiWithUrl(String url){
        List<VideoSaver> savers = LitePal.where("url = ?", url).order("api").find(VideoSaver.class);
        VideoSaver saver;
        if (savers.size() > 0) {
            saver = savers.get(0);
            activity.changeButton.setText(activity.apiNames.get(saver.getApi()).replaceAll("\\[.*", ""));
            activity.spUtils.putValue("api", saver.getApi());
        }
    }
    public void initVideoTool(){

        activity.nextButton.setOnClickListener(v -> {
            otherHandler.removeCallbacksAndMessages(null);

            if (activity.player.isIfCurrentIsFullscreen()){
                activity.player.getCurrentPlayer().getStartButton().callOnClick();
                return;
            }
            switchNext();

        });
        activity.changeButton.setOnClickListener(v -> {
            otherHandler.removeCallbacksAndMessages(null);
            if (activity.player.isIfCurrentIsFullscreen()){
                activity.player.getCurrentPlayer().getStartButton().callOnClick();
                return;
            }
            activity.player.changeApi(false);
        });
        activity.videoTool.setOnGetHistory(new VideoTool.OnGetHistory() {
            @Override
            public void onGetSuccess(JSONObject history) {
                try {
                    String url;
                    if (activity.direct){
                        url = activity.getIntent().getStringExtra("url");
                        activity.direct = false;
                    }else {
                        url = history.getString("url");
                        String c =history.getString("current");
                        if (c.equals("第-1集"))
                            return;
                        else if (c.equals("第-2集")){
                            return;
                        }

                    }
                    loadApiWithUrl(url);
                    switchVideo(url);

                }catch (Exception e){
                    e.printStackTrace();
                }

            }

            @Override
            public void onGetFail() {
                try {
                    LogUtil.d("tag", "history none");

                    List<Selection> array = activity.videoTool.getInfo().getSelections();
                    if (activity.direct){
                        String url = activity.getIntent().getStringExtra("url");
                        loadApiWithUrl(url);
                        switchVideo(url);
                    }
                    else if (array.size()>0) {
                        loadApiWithUrl(array.get(0).getUrl());
                        switchVideo(0);
                        fullHandler.postDelayed(() -> {
                            activity.layoutManager.scrollToPosition(0);
                            activity.adapter.change(0);
                        }, 1000);


                    }
                    else {
                        loadApiWithUrl(activity.getUrl());
                        getVideoFromResult();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        activity.videoTool.setOnGetInfo(new VideoTool.OnGetInfo() {
            @Override
            public void onGetStart() {
                activity.player.getLoadingView().show();
                activity.player.getLoadingView().setProgress(0, "信息获取中");
                fullHandler.removeCallbacks(fullRunnable);
                activity.fullButton.setClickable(false);
                new Thread(() -> {
                    try {
                        activity.videoTool.getHistoryManager().updateCurrent(activity.videoTool.getInfo().getCurrent(), activity.videoTool.getInfo().getUrl());
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }).start();
            }

            @Override
            public void onGetFail() {
                activity.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                activity.recyclerView.setAdapter(new SelectionItemAdapter(new ArrayList<>()));
                activity.videoTool.getHistory(activity);
                Toast.makeText(activity, "非常抱歉，我们暂未收录当前视频", Toast.LENGTH_SHORT).show();
                fullHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        activity.finish();
                    }
                }, 500);
            }

            @Override
            public void onGetSuccess(Info info) {
                activity.player.getLoadingView().finish("资源获取成功");
                try {
                    activity.header.setText(info.getOutput());
                    activity.description.setText(info.getDescription());
                    activity.peroid.setText(info.getPeriod());
                    if (info.isZongyi()){
                        activity.layoutManager = new GridLayoutManager(activity, DeviceManager.isTv()? 7:4);
                        activity.recyclerView.setLayoutManager(activity.layoutManager);
                        activity.adapter.setArray(info.getSelections(), null);
                        activity.groupView.setVisibility(View.GONE);
                    }else {
                        activity.groupAdapter.initItems(info.getSelections());
                        activity.adapter.setArray(info.getSelections(), activity.groupAdapter.getFirstItem());
                        if (activity.groupAdapter.getItemCount() > 1)
                            activity.groupView.setVisibility(View.VISIBLE);
                    }

                    activity.selectionDialog = new SelectionDialog(activity, info.getSelections());
                    activity.selectionDialog.setOnItemClickListener((item, position, dialog) -> {
                        dialog.hide();
                        fullHandler.postDelayed(() -> activity.groupAdapter.changeGroup(position, (group, header, tailer) -> {
                            activity.groupAdapter.setCurrent(group);
                            if (group > 0) {
                                activity.adapter.showList(header, tailer);
                                activity.groupManager.scrollToPositionWithOffset(group, 0);
                            }
                            activity.adapter.changeAbsolutely(position);
                            LinearTopSmoothScroller smoothScroller = new LinearTopSmoothScroller(activity);
                            smoothScroller.setTargetPosition(position - header);
                            activity.layoutManager.startSmoothScroll(smoothScroller);
                            switchVideo(position);
                        }), 500);
                    });
                    activity.videoTool.getHistory(activity);
                    //设置好
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
        activity.videoTool.setOnGetVideo(new VideoTool.OnGetVideo() {
            @Override
            public void onGetStart() {
                fullHandler.removeCallbacks(fullRunnable);
                activity.player.setQuality("");
                activity.player.getcurrentPlayer().setQuality("");
                activity.player.setLocalCache(false);
                activity.player.getcurrentPlayer().setLocalCache(false);
                activity.fullButton.setClickable(false);
                activity.player.getGSYVideoManager().pause();
                activity.player.getcurrentPlayer().release();
                activity.player.release();
                activity.player.setRelease(true);
                new Thread(() -> {
                    try {
                        activity.videoTool.getHistoryManager().updateCurrent(activity.videoTool.getInfo().getCurrent(), activity.videoTool.getInfo().getUrl());
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }).start();
            }

            @Override
            public void onGetFail() {
                activity.videoTool.getHistoryManager().deleteLog(activity.videoTool.getWebsite(), activity.videoTool.getInfo().getName());
                if (activity == null)
                    return;
                if (!activity.player.isRelease())
                    return;
                Log.i("tag", "fail");
                otherHandler.sendEmptyMessage(MESSAGE_CHANGE_API);
            }

            @Override
            public void onGetSuccess(SniffingVideo video, long currentTime, String tname, boolean localCache) {

                if (activity == null)
                    return;
                if (!activity.player.isRelease())
                    return;
                activity.player.getcurrentPlayer().setRelease(false);
                activity.player.setRelease(false);
                if (localCache)
                    Toast.makeText(activity, "视频已缓存", Toast.LENGTH_SHORT).show();
                activity.fullButton.setClickable(true);
                activity.player.setLocalCache(localCache);
                activity.player.setStartAfterPrepared(!activity.isPause());
                activity.player.getcurrentPlayer().setSniffingVideo(video, currentTime, tname);
            }
        });
        activity.url = activity.url.replaceAll("\\?.*", "");

    }

    public void getVideoFromResult(){
        try {
            final String url = activity.videoTool.getInfo().getUrl();
            String api = Api.getApis().getString(activity.apiNames.get(activity.spUtils.getValue("api", 0)));
            activity.videoTool.setLoadingView(activity.player.getcurrentPlayer().getLoadingView());
            activity.videoTool.getVideo(api, activity.adapter.getType());
        }catch (Exception e){
            e.printStackTrace();
        }

    }





}
