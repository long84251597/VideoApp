package com.kai.video.tool.net;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.IPTool;
import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.database.VideoDownloadDatabaseHelper;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.model.VideoTaskState;
import com.jeffmony.downloader.utils.ContextUtils;
import com.kai.sniffwebkit.LoadingView;
import com.kai.sniffwebkit.sniff.MySniffingFilter;
import com.kai.sniffwebkit.sniff.SniffTool;
import com.kai.sniffwebkit.sniff.SniffingVideo;
import com.kai.video.R;
import com.kai.video.activity.InfoActivity;
import com.kai.video.bean.obj.Api;
import com.kai.video.bean.obj.History;
import com.kai.video.bean.obj.Info;
import com.kai.video.manager.DeviceManager;
import com.kai.video.tool.application.GC;
import com.kai.video.tool.application.SPUtils;
import com.kai.video.fragment.PlaceholderFragment;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VideoTool {
    private final Context context;
    private OnGetInfo onGetInfo;
    private OnGetVideo onGetVideo;
    private OnGetHistory onGetHistory;
    private Info info;
    private String website = "";
    //注意只能有唯一的信息获取线程和视频获取线程
    private VideoTask videoTask;
    private String tname = "";
    private long currentTime = 0;
    private InfoTask infoTask;
    private History history;
    private HistoryTask historyTask = null;
    private final MySniffingFilter mFilter = new MySniffingFilter();
    private Iterator<PreSniffingVideo> videoIterator;
    public static VideoTool getInstance(Activity activity){
        return new VideoTool(activity);
    }
    public History getHistoryManager(){
        return history;
    }
    private VideoTool(Context context){
        this.context = context;
    }


    public String getWebsite() {
        return website;
    }

    public void setOnGetInfo(OnGetInfo onGetInfo){
        this.onGetInfo = onGetInfo;
    }

    public void setOnGetHistory(OnGetHistory onGetHistory) {
        this.onGetHistory = onGetHistory;
    }

    public void setOnGetVideo(OnGetVideo onGetVideo){
        this.onGetVideo = onGetVideo;
    }


    public Info getInfo() {
        return info;
    }

    @SuppressLint("StaticFieldLeak")
    private class HistoryTask extends AsyncTask{
        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                final JSONObject result = history.getCurrent(info.getCurrent() + "", info.getUrl());
                if (result.getBoolean("success"))
                    return result;
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (o == null)
                onGetHistory.onGetFail();
            else
                onGetHistory.onGetSuccess((JSONObject)o);
            super.onPostExecute(o);
        }
    }
    public void getHistory(Context context){
        try {
            if (history == null)
                history = new History(context, info.getName(), info.getVideoType(), info.getType());
            if (historyTask != null && !historyTask.isCancelled()) {
                historyTask.cancel(true);
                historyTask = null;
            }
            historyTask = new HistoryTask();
            historyTask.execute();
        }catch (Exception e){
            e.printStackTrace();
            onGetHistory.onGetFail();
        }
    }
    public void getInfo(String url){
        if (infoTask != null) {
            infoTask.cancel(true);
            infoTask = null;

        }
        Log.e("获取", url);
        infoTask = new InfoTask(url, onGetInfo);
        infoTask.execute();
    }

    public void getVideo(String api, int type){
        if (loadingView != null) {
            loadingView.show();
            loadingView.setProgress(0, "获取解析地址");
        }

        if (videoTask != null) {
            videoTask.cancel(true);
            videoTask = null;

        }
        videoTask = new VideoTask(info, api, type);
        videoTask.execute();
    }


    public void reTry(){
        if (videoIterator != null && videoIterator.hasNext())
            sniff();
        else {
            onGetVideo.onGetFail();
        }
    }

    public void pause(){
        if (videoTask != null) {
            videoTask.cancel(true);
            videoTask = null;
        }
    }
    public void deleteCache(String filePath){
        VideoDownloadDatabaseHelper mVideoDatabaseHelper = new VideoDownloadDatabaseHelper(ContextUtils.getApplicationContext());
        List<VideoTaskItem> taskItems = mVideoDatabaseHelper.getDownloadInfos();
        for (VideoTaskItem taskItem : taskItems) {
            if (taskItem.getFilePath().equals(filePath)){
                VideoDownloadManager.getInstance().deleteVideoTask(taskItem, true);
                break;
            }
        }
    }

    public void deleteVideo(String title, String groupName){
        VideoDownloadDatabaseHelper mVideoDatabaseHelper = new VideoDownloadDatabaseHelper(ContextUtils.getApplicationContext());
        List<VideoTaskItem> taskItems = mVideoDatabaseHelper.getDownloadInfos();
        for (VideoTaskItem taskItem : taskItems) {
            if (taskItem.getTitle().equals(title) && taskItem.getGroupName().equals(groupName)){
                VideoDownloadManager.getInstance().deleteVideoTask(taskItem, true);
                break;
            }
        }
    }

    public boolean isDownloading(String title, String groupName, String url){
        VideoDownloadDatabaseHelper mVideoDatabaseHelper = new VideoDownloadDatabaseHelper(ContextUtils.getApplicationContext());
        List<VideoTaskItem> taskItems = mVideoDatabaseHelper.getDownloadInfos();
        for (VideoTaskItem taskItem : taskItems) {
            if (taskItem.getTitle().equals(title) && taskItem.getGroupName().equals(groupName) && taskItem.getUrl().equals(url) ){
                return true;
            }
        }
        return false;
    }




    public void destory(){
        //在活动结束时调用，避免内存泄漏
        //消除所有消息反馈及内存占用
        long id = 0;
        if (historyTask!=null)
            historyTask.cancel(true);
        if(infoTask != null)
            infoTask.cancel(true);
        if (videoTask != null) {
            videoTask.cancel(true);
        }
        historyTask = null;
        videoTask = null;
        infoTask = null;
        //将SniffTool销毁
        SniffTool.destoryTool();
    }

    private void sniff(JSONArray videos){
        if (videos == null){
            onGetVideo.onGetFail();
            loadingView.finish("嗅探失败");
            return;
        }

        List<PreSniffingVideo> preSniffingVideos = videos.toJavaList(PreSniffingVideo.class);
        if (preSniffingVideos.size() > 5)
            preSniffingVideos = preSniffingVideos.subList(0, 3);
        Log.e("sniffTool", "待嗅探数量：" + preSniffingVideos.size());
        videoIterator = preSniffingVideos.iterator();
        sniff();
    }
    private LoadingView loadingView;

    public void rebindLoadingTool(LoadingView loadingView){
        this.loadingView = loadingView;
        SniffTool.getInstance((Activity) context)
                .bindLoadingView(loadingView);
    }

    public void setLoadingView(LoadingView loadingView) {
        this.loadingView = loadingView;
    }

    private void sniff(){
        //没有后续解析，则直接返回解析失败
        if (videoIterator == null)
            return;
        if (!videoIterator.hasNext()) {
            GC.clean();
            Log.i("tag", "snifferror");
            onGetVideo.onGetFail();
            loadingView.finish("嗅探失败");
            return;
        }
        PreSniffingVideo video = videoIterator.next();
        if (!video.isSniff()){
            GC.clean();
            SniffingVideo sniffingVideo = new SniffingVideo(video.getUrl(), "");
            if (video.url.startsWith( IPTool.getLocal() + "/video"))
                sniffingVideo = new SniffingVideo(video.getUrl(), "video/ijk-concat", -1, "concat");
            else if (video.url.contains("bilivideo")) {
                sniffingVideo = new SniffingVideo(video.url, "video/x-flv", -1, "flv");
                sniffingVideo.addHeaders(HttpReferer.getInstance(info.getUrl(), video.url).getMap());
            }
            onGetVideo.onGetSuccess(sniffingVideo, currentTime, tname, false);
            loadingView.finish("获取成功");
            return;
        }
        String web = video.getUrl();
        SniffTool.getInstance((Activity) context)

                .userAgent(DeviceManager.getUserAgent())
                .bindLoadingView(loadingView)
                .setCallback(new SniffTool.Callback() {
                    @Override
                    public void onProgress(int progress) {
                        //loadingView.setSubTitle("线路:" + (SPUtils.get(context).getValue("api", 0) + 1) + "/" + Api.getSize());
                    }

                    @Override
                    public void onSuccess(SniffingVideo video) {
                        Map<String, String> headers = HttpReferer.getInstance(info.getUrl(), video.getUrl()).getMap();
                        if (!headers.isEmpty())
                            video.addHeaders(headers);
                        onGetVideo.onGetSuccess(video, currentTime, tname, false);
                        loadingView.finish("嗅探成功");
                    }

                    @Override
                    public void onFailed(int errorCode) {
                        Log.e("SniffTool", "next url");

                        sniff();
                    }

                })
                .setFilter(mFilter)
                .target(web)
                .setSniffTimeout(40 * 1000)
                .setJsTimeout(30 * 1000)
                .start();

    }



    @SuppressLint("StaticFieldLeak")
    public class VideoTask extends AsyncTask{
        private Iterator<PreSniffingVideo> iterator;
        private Info info;
        private String api;
        private String url = "";
        private Map<String, String> cookies;
        private final MySniffingFilter mfilter = new MySniffingFilter();
        private int remainTime = 1;
        public VideoTask(Info info, String api, int type){
            super();
            iterator = new Iterator<PreSniffingVideo>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public PreSniffingVideo next() {
                    return null;
                }
            };
            this.info = info;
            this.api = api;
            if (api.equals("0") && !info.getVideoType_EN().equals("bilibili")){
                if (type > 1)
                    return;
            }if (info.getVideoType_EN().equals("bilibili") && info.getType() == 1 && api.equals("0")){
                if (context != null)
                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "如果当前片段为试看，请切换线路", Toast.LENGTH_SHORT).show();
                    }
                });

            }

            cookies = new ArrayMap<>();
            try {
                Log.e("tag", JSONObject.toJSONString(info));
                cookies.put("web-url", URLEncoder.encode(info.getUrl().replace("?theme=movie", "")));
                cookies.put("web-current", URLEncoder.encode(info.getCurrent() + ""));
                cookies.put("web-current-text", URLEncoder.encode(info.getCurrentText()));
                cookies.put("web-name", URLEncoder.encode(info.getName()));
                cookies.put("web-zy", URLEncoder.encode(info.isZongyi() + ""));
                cookies.put("web-pname", URLEncoder.encode(info.getPname()));
                cookies.put("web-season", URLDecoder.decode(info.getSeason()));
                cookies.put("web-series", URLDecoder.decode(info.getSeries()));
                cookies.put("web-mobile", "12345678901");
                cookies.put("web-tv", "true");
                url = getInfo().getUrl();
                if (info.getType() == Info.TYPE_MOVIE)
                    tname = info.getName();
                else
                    tname = info.getTitle();
            }catch (Exception e){
                e.printStackTrace();
                cancel(true);
            }

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingView.setSubTitle("线路：" + (SPUtils.get(context).getValue("api", 0) + 1) + "/" + Api.getSize());
            if (isCancelled()) {
                onGetVideo.onGetFail();
                Log.i("tag", "s_cancel");
            }
            else
                onGetVideo.onGetStart();

        }




        private Bundle checkLocalCache(){
            Bundle bundle = new Bundle();
            bundle.putBoolean("success", false);
            VideoDownloadDatabaseHelper mVideoDatabaseHelper = new VideoDownloadDatabaseHelper(ContextUtils.getApplicationContext());
            List<VideoTaskItem> taskItems = mVideoDatabaseHelper.getDownloadInfos();
            for (VideoTaskItem taskItem : taskItems) {
                String title = taskItem.getTitle();
                int taskState = taskItem.getTaskState();
                int videoType = taskItem.getVideoType();
                if (title.equals(tname + "|" + url) &&
                        (taskState == 5 || taskState == 4)) {
                    Log.i("local", taskItem.getFilePath());
                    bundle.putBoolean("success", true);
                    bundle.putString("url", taskItem.getFilePath());
                    bundle.putLong("time", currentTime);
                    bundle.putString("tname", tname);
                    bundle.putBoolean("localCache", true);
                    break;
                }
            }


            return bundle;
        }
        @Override
        protected Object doInBackground(Object[] objects) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("success", false);
            try {
                if (history != null)
                    currentTime = history.getTime(info.getUrl());
                bundle = checkLocalCache();
                if (bundle.getBoolean("success"))
                    return bundle;
                while (!isCancelled() && remainTime > 0){
                    bundle = getVideo();
                    if (bundle.getBoolean("success")){
                        return bundle;
                    }
                    if (remainTime == 1){
                        return bundle;
                    }
                    remainTime--;
                }

            }catch (Exception e){
                e.printStackTrace();
            }
            return bundle;

        }



        @Override
        protected void onCancelled() {
            super.onCancelled();
            info = null;
            api = null;
            if (cookies!=null)
                cookies.clear();
            cookies = null;
        }






        private Bundle getVideo(){
            Bundle bundle = new Bundle();
            try {
                Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/videogoar")
                        .ignoreContentType(true)
                        .data("url", info.getUrl().replace("?theme=movie", ""))
                        .data("api", api)
                        .cookies(cookies)
                        .method(Connection.Method.GET)
                        .timeout(1000 * 20)
                        .execute();
                String body = response.body();
                Log.d("TAG", body);
                JSONObject object = JSONObject.parseObject(body);
                JSONArray videos = object.getJSONArray("videos");
                website = object.containsKey("website")?object.getString("website"):"";
                runUI(new Runnable() {
                    @Override
                    public void run() {
                        sniff(videos);
                    }
                });


            }catch (Exception e){
                e.printStackTrace();
                bundle.putBoolean("success", false);
            }
            return bundle;

        }
        @Override
        protected void onPostExecute(Object o) {

            Bundle bundle = (Bundle)o;
            if (bundle.containsKey("success")){
                if (bundle.getBoolean("success")) {
                    if (bundle.getString("url").startsWith(IPTool.getLocal() + "analysis?url=")) {
                        onGetVideo.onGetSuccess(new SniffingVideo(bundle.getString("url"), "video/ijk-concat", 0, "concat"), currentTime, tname, bundle.getBoolean("localCache"));
                        return;
                    }
                    else if (bundle.getString("url").startsWith("/")){
                        loadingView.finish("视频已缓存");
                        onGetVideo.onGetSuccess(new SniffingVideo(bundle.getString("url"), "video/cache", 0, bundle.getString("url").endsWith(".m3u8")?"m3u8":"mp4"), currentTime, tname, true);
                        return;
                    }
                    SniffingVideo sniffingVideo = mfilter.onFilter(null, InfoActivity.toUtf8String(bundle.getString("url", "")), HttpReferer.getInstance(info.getUrl(), bundle.getString("url")).getMap());
                    if (sniffingVideo == null){

                    }else {
                        sniffingVideo.addHeaders(HttpReferer.getInstance(getInfo().getUrl(), sniffingVideo.getUrl()).getMap());
                        onGetVideo.onGetSuccess(sniffingVideo, currentTime, tname, bundle.getBoolean("localCache"));
                    }
                }else {
                    loadingView.finish("获取地址失败");
                    onGetVideo.onGetFail();
                    Log.i("tag", "s_fail");
                }
                cancel(true);
            }else if (bundle.containsKey("sniff")){
                //do nothing but wait for sniff operation ending
            }
            super.onPostExecute(o);
        }

    }

    @SuppressLint("StaticFieldLeak")
    public class InfoTask extends AsyncTask{
        private final OnGetInfo onGetInfo;
        private final String url;
        public InfoTask(String url, OnGetInfo onGetInfo){
            super();
            this.url = url;
            this.onGetInfo = onGetInfo;

        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onGetInfo.onGetStart();
        }
        private String transToTv(String url){
            Connection.Response response = null;
            try {
                response = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36")
                        .method(Connection.Method.GET)
                        .execute();
                return  response.url().toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return url;

        }
        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                Info info = getInfo();
                if (info == null) {
                } else if (info.getType() !=1 && info.getSelections().size() == 0) {
                } else {
                    VideoTool.this.info = info;
                    return info;
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }
        //递归获取视频信息
        private Info getInfo(){
            try {
                Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/VideoServlet?url=" + url)
                        .ignoreContentType(true)
                        //.data("mobile", SPUtils.get(context).getValue("username", ""))
                        .timeout(60*1000)
                        .execute();
                 return JSONObject.parseObject(response.body(), Info.class);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }

        }
        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if (o == null){
                onGetInfo.onGetFail();
            }else {
                onGetInfo.onGetSuccess((Info) o);
            }
            cancel(true);
        }
    }

    private void runUI(Runnable runnable){
        if (context == null){
            return;
        }
        ((Activity)context).runOnUiThread(runnable);
    }

    public interface OnGetInfo{
        void onGetStart();
        void onGetFail();
        void onGetSuccess(Info result);
    }
    public interface OnGetVideo{
        void onGetStart();
        void onGetFail();
        void onGetSuccess(SniffingVideo video, long Time, String tname, boolean localcache);

    }
    public interface OnGetHistory{
        void onGetSuccess(JSONObject history);
        void onGetFail();
    }
    public interface OnGetDanmu{
        void onGetSuccess(JSONArray danmuList);
        void onGetFail();
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public static class SectionsPagerAdapter extends FragmentPagerAdapter {

        @StringRes
        private static final int[] TAB_TITLES = new int[]{R.string.tab_text_1, R.string.tab_text_2, R.string.tab_text_3, R.string.tab_text_4};
        private PlaceholderFragment currentFragment;
        private final Context mContext;

        public SectionsPagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }
        public void destroy(){
            currentFragment = null;
        }
        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            currentFragment = (PlaceholderFragment) object;
            Bundle bundle = new Bundle();
            bundle.putInt("index", position);
            currentFragment.setArguments(bundle);
            super.setPrimaryItem(container, position, object);
        }

        public PlaceholderFragment getCurrentFragment() {
            return currentFragment;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            PlaceholderFragment fragment =  new PlaceholderFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("index", position);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return mContext.getResources().getString(TAB_TITLES[position]);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return TAB_TITLES.length;
        }
    }
    private static class PreSniffingVideo{
        private String url = "";
        private boolean sniff = false;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isSniff() {
            return sniff;
        }

        public void setSniff(boolean sniff) {
            this.sniff = sniff;
        }
    }
}
