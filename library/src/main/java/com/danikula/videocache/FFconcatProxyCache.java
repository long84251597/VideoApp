package com.danikula.videocache;


import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.file.FileCache;
import com.danikula.videocache.file.LogContent;
import com.kingsoft.media.httpcache.KSYProxyService;
import com.kingsoft.media.httpcache.OnCacheStatusListener;
import com.kingsoft.media.httpcache.stats.OnLogEventListener;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FFconcatProxyCache extends HttpProxyCache implements OnCacheStatusListener, OnLogEventListener{
    private final Context context;
    private static int CONCAT_CACHE_STEP = 1;
    //为大分片准备，最多支持20分片并行下载，可根据分片总数量决定
    private static final int MAX_FILES = 6;
    private static final int MAX_CACHE_SIZE = 1024 * 1024 * 1024;
    private float duration = 0;
    private final File ffCacheDir;
    private final List<String> mUrls = new ArrayList<>();
    private KSYProxyService proxy;
    private int mCurCachePos = 0;   // 代理端正在缓存的
    private int mCurRequestPos = 0; // 客户端正在请求的索引
    private float segment_duration = 0;
    private final Map<String, String> downloadingTask = new HashMap<>();
    
    private void onProgressUpdate(int index, int singlePercent){
        float current = (float) (index + singlePercent / 100.0) * segment_duration;
        logE("更新次序 ：" + index + "，百分比" + (current / duration) * 100 + "%");
        if (index > mCurCachePos || mUrls.size() == 1)
            listener.onCacheAvailable(cache.file, source.getUrl(), (int) (current / duration * 100) );
    }

    private void scheduleCache() {
        //废弃之前的任务
        //dumpOldTask();
        int start = mCurRequestPos + 1;
        if (start >= mUrls.size())
            return;
        logE("开始缓存队列");
        int end = start + CONCAT_CACHE_STEP;
        if (end >= mUrls.size()) {
            end = mUrls.size();
        }

        for(int pos = start; pos < end; pos++) {
            String url = mUrls.get(pos);
            if (!isCached(url)) {
                proxy.startPreDownload(url);
                break;
            }else {
                onProgressUpdate(pos, 100);
            }
        }

    }

    //暂停当前分片前的缓存任务
    private void dumpOldTask(){
        for(String key: downloadingTask.keySet()){
            int index = mUrls.indexOf(downloadingTask.get(key));
            if (index < mCurRequestPos){
                logE("丢弃任务" + index);
                proxy.stopPreDownload(downloadingTask.get(key));
                downloadingTask.remove(key);
            }
        }
    }
    private boolean isIdDownloading(String downloadId){
        return downloadingTask.containsKey(downloadId);
    }

    private boolean isUrlDownloading(String url){
        return downloadingTask.containsKey(url);
    }


    @Override
    public void onLogEvent(String log) {
        try {
            LogContent content = JSONObject.parseObject(log, LogContent.class);
            logE(log);
            String type = content.getBody_type();

            switch (type){
                case "probe":
                    logE("次序：" + mUrls.indexOf(downloadingTask.get(content.getCacheId())));
                    int index = mUrls.indexOf(downloadingTask.get(content.getCacheId()));
                    if (index > 0) {
                        mCurRequestPos = index;
                        scheduleCache();
                    }
                    break;
                case "stopCache":
                    if (mUrls.get(mCurCachePos).equals(content.getUrl()))
                        scheduleCache();
                    break;
                case "startCache":
                    downloadingTask.put(content.getCacheId(), content.getUrl());
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void OnCacheStatus(String url, long sourceLength, int percentsAvailable) {

        if (percentsAvailable == 100) {
            this.listener.onM3U8ItemDecrypt(new M3U8ProxyCache.CacheItem(url, new M3U8ProxyCache.DecryptInfo("xxxx", 0)));
        }
        onProgressUpdate(mUrls.indexOf(url), percentsAvailable);
    }

    public FFconcatProxyCache(Context context, HttpUrlSource source, FileCache cache) {
        super(source, cache);
        this.context = context;
        Log.e("cache TAG", cache.getFile().getAbsolutePath());
        ffCacheDir = new File(cache.file.getAbsolutePath() + ".cache/");

        initProxy();
    }

    private void initProxy(){
        if (!ffCacheDir.exists())
            ffCacheDir.mkdirs();
        else
            for(File file:ffCacheDir.listFiles())
                file.delete();
        proxy = new KSYProxyService(context);
        proxy.setMaxFilesCount(MAX_FILES);
        proxy.setMaxCacheSize(MAX_CACHE_SIZE);
        proxy.setCacheRoot(ffCacheDir);
        proxy.startServer();
    }

    private boolean isCached(String url){
        return proxy.isCached(url);
    }


    private void logE(String message){
        //Log.e("ffconcat", message);
    }



    @Override
    protected void onCachePercentsAvailableChanged(int percents) {

    }

    @Override
    protected boolean isUseCache(GetRequest request) throws ProxyCacheException {
        String uri = ProxyCacheUtils.decode(request.uri);
        if (ProxyCacheUtils.isConcatList(uri)){
            return cache.file.exists();
        }
        return super.isUseCache(request);
    }


    @Override
    public void processRequest(GetRequest request, Socket socket) throws IOException, ProxyCacheException {
        String uri = ProxyCacheUtils.decode(request.uri);
        //处理清单文件

        if (ProxyCacheUtils.isConcatList(uri)){
            // 清单文件的请求，让父类处理
            // 等待清单文件预处理（去除其中绝对路径）完成
            logE("处理清单" + uri);
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());
            out.write(newResponseHeaders(request).getBytes(StandardCharsets.UTF_8));
            hookTransformList(out);
        }else {
            super.processRequest(request, socket);
        }
    }


    protected void hookTransformListWithCache(OutputStream out) {
        BufferedReader buffreader = null;
        try {
            buffreader = new BufferedReader(new FileReader(cache.file));
            mUrls.clear();
            String br;
            logE("开始获取清单");
            while (( br = buffreader.readLine()) != null) {
                //直接替换链接
                if (br.startsWith("file")){
                    br = br.replace("file ", "");
                    mUrls.add(br);
                    proxy.registerCacheStatusListener(this, br);
                    br = "file " + proxy.getProxyUrl(br);
                }
                if (br.startsWith("duration")){
                    duration += Float.parseFloat(br.replace("duration ", ""));
                }
                Log.e("br", br);
                out.write((br + "\n").getBytes(StandardCharsets.UTF_8));
            }
            out.flush();
            buffreader.close();
            if (mUrls.size() > 0)
                segment_duration = duration / mUrls.size();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                buffreader.close();
            } catch (Exception e) {
                Log.e("sth. went wrong", "source failed to close");
            }
        }


    }

    @Override
    public void registerCacheListener(CacheListener cacheListener) {
        super.registerCacheListener(cacheListener);
        proxy.registerLogEventListener(this);
    }
    

    @Override
    public void shutdown() {
        proxy.shutDownServer();
        proxy.unregisterLogEventListener(this);
        try {
            for(File file: ffCacheDir.listFiles())
                file.delete();
            cache.getFile().delete();
        }catch (Exception e){
            e.printStackTrace();
        }
        super.shutdown();
    }



    //拦截清单文件并替换为自己的
    private void hookTransformList(OutputStream out) {
        //FileOutputStream out_cache = null;
        try {

            mUrls.clear();
            //打开source
            source.open(0);
            //out_cache = new FileOutputStream(cache.file);
            InputStreamReader inputreader = source.getReader();
            BufferedReader buffreader = new BufferedReader(inputreader);
            String br;
            logE("开始获取清单");
            while (( br = buffreader.readLine()) != null) {
                //直接替换链接
                //out_cache.write((br + "\n").getBytes(StandardCharsets.UTF_8));
                if (br.startsWith("file")){
                    br = br.replace("file ", "");
                    mUrls.add(br);
                    proxy.registerCacheStatusListener(this, br);
                    br = "file " + proxy.getProxyUrl(br);
                }
                if (br.startsWith("duration")){
                    duration += Float.parseFloat(br.replace("duration ", ""));
                }
                Log.e("br", br);
                out.write((br + "\n").getBytes(StandardCharsets.UTF_8));
            }
            out.flush();
            //out_cache.flush();
            buffreader.close();
            if (mUrls.size() > 0)
                segment_duration = duration / mUrls.size();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            source.closeReader();
            try {
                source.close();

            } catch (Exception e) {
                Log.e("sth. went wrong", "source failed to close");
            }
        }
        if (mUrls.size() < 3)
            CONCAT_CACHE_STEP = 1;
        else if (mUrls.size() < 10)
            CONCAT_CACHE_STEP = 3;
        else if (mUrls.size() < 20)
            CONCAT_CACHE_STEP = 5;
        else
            CONCAT_CACHE_STEP = mUrls.size() * 20 /100;
    }


}
