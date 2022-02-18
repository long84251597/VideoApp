package com.kai.video.bean.danmu;

import android.content.Context;
import android.util.Log;

import com.danikula.videocache.IPTool;
import com.just.x5.util.FilePath;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 用来缓存弹幕文件，总共可缓存10个
 */
public class DanmuFile{
    //默认的缓存目录
    private static final int BYTE = 12;
    private static String CACHE_PATH = "";
    private static  File cacheDir = null;
    public static void init(Context context){
        cacheDir = FilePath.getCachePath(context, "danmu");
        CACHE_PATH = cacheDir.getAbsolutePath();
        if (!cacheDir.exists()){
            cacheDir.mkdirs();
        }
    }
    //官方链接
    private final String url;

    public static DanmuFile getInstance(String url){
        return new DanmuFile(url);
    }

    /**
     * 官方参数
     * @param url
     */
    public DanmuFile(String url){
        this.url = url;
    }
    public static String Md5(String str) {
        if (str != null && !str.equals("")) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
                byte[] md5Byte = md5.digest(str.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : md5Byte) {
                    sb.append(HEX[(b & 0xff) / 16]);
                    sb.append(HEX[(b & 0xff) % 16]);
                }
                str = sb.toString();
            } catch (Exception ignored) {
            }
        }
        return str;
    }
    private String getCachePath(){
        return CACHE_PATH + "/" + Md5(url) + ".cache";
    }
    public boolean existsCache(){
        File cache = new File(getCachePath());
        return cache.exists() && System.currentTimeMillis() - cache.lastModified() > 5 * 24 * 3600 * 1000;
    }

    public File getCacheFile() throws IOException {
        if (existsCache())
            return createCacheFile();
        else
            return null;
    }
    public interface OnCacheListner{
        void onCached(File file);
        void onCacheFailed();
    }
    public void clearOldCaches(){
        if (cacheDir == null || !cacheDir.exists())
            return;
        File[] files = cacheDir.listFiles();
        assert files != null;
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                long diff = f1.lastModified() - f2.lastModified();
                if (diff > 0)
                    return 1;
                else if (diff == 0)
                    return 0;
                else
                    return -1;
            }

            public boolean equals(Object obj) {
                return true;
            }

        });
        int length = files.length < 5?0: files.length - 5;
        for(int i = 0; i < length; i++){
            files[i].delete();
        }
    }
    public void cache(OnCacheListner onCacheListner) throws IOException {
        new Thread(() -> {

            OutputStream outputStream = null;
            try {
                clearOldCaches();
                Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/danmu")
                        .data("url", url.replaceAll("\\?.*", ""))
                        .method(Connection.Method.GET)
                        .timeout(60 * 1000)
                        .ignoreContentType(true)
                        .execute();
                File file = createCacheFile();
                outputStream = new FileOutputStream(file);
                outputStream.write(response.body().getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                onCacheListner.onCached(file);
            }catch (Exception e){
                e.printStackTrace();
                onCacheListner.onCacheFailed();
            }finally {
                if (outputStream != null){
                    try {
                        outputStream.close();
                    }catch (Exception ignored){

                    }
                }
            }

        }).start();
    }

    private File createCacheFile() throws IOException {
        File file = new File(getCachePath());
        if (!file.exists())
            file.createNewFile();
        return file;
    }


}
