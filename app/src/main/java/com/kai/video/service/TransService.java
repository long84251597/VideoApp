package com.kai.video.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.jeffmony.downloader.database.VideoDownloadDatabaseHelper;
import com.jeffmony.downloader.model.Video;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.model.VideoTaskState;
import com.jeffmony.downloader.utils.ContextUtils;
import com.kai.video.tool.application.ApplicationDownloadTool;
import com.just.x5.util.FilePath;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class TransService extends IntentService {


    public TransService() {
        super("TransService");
    }



    private void copyFileUsingFileChannels(File source, File dest, VideoTaskItem item) throws IOException {
       try {
           FileInputStream fis=new FileInputStream(source);//要复制文件的路径
           FileOutputStream fos=new FileOutputStream(dest);//要把文件复制到哪里的路径
           BufferedInputStream bufis=new BufferedInputStream(fis);
           BufferedOutputStream bufos=new BufferedOutputStream(fos);
           byte[] by=new byte[1024*1024*150];//byte[]数组的大小，根据复制文件的大小可以调整，1G一下可以5M。1G以上150M，自己多试试
           int len;
           boolean flag=true;
           long f=System.nanoTime();
           double begin=bufis.available();
           while(flag)
           {
               len=bufis.read(by);
               if(len==-1)
               {
                   flag=false;
                   continue;
               }
               bufos.write(by,0,len);
               bufos.flush();
               item.setPercent((float)(1-bufis.available()/begin)*100);
               item.setTaskState(VideoTaskState.DOWNLOADING);
               item.setIsCompleted(false);
               ApplicationDownloadTool.getInstance().onDownloadProgress(item);
           }
           bufos.close();
           bufis.close();
           //long e=System.nanoTime();
           //System.out.println("\n用时"+(e-f)/1000000000+"秒");//显示总用时
       }catch (Exception e){
           e.printStackTrace();
       }
    }

    private void createVideoItem(String path, String title, String groupName){
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                try {
                    File source = new File(path);
                    if (!source.exists())
                        return;
                    File dest = new File(FilePath.getFilePath(getApplicationContext(), "video").getAbsolutePath() + "/" + source.getName());
                    if (!dest.exists())
                        dest.createNewFile();
                    VideoDownloadDatabaseHelper mVideoDatabaseHelper = new VideoDownloadDatabaseHelper(ContextUtils.getApplicationContext());
                    VideoTaskItem item = new VideoTaskItem(dest.getAbsolutePath(), "",  title, groupName);
                    item.setDownloadCreateTime(System.currentTimeMillis());
                    item.setDownloadSize(source.length());
                    item.setMimeType("video/mp4");
                    item.setLastUpdateTime(System.currentTimeMillis());
                    item.setVideoType(Video.Type.MP4_TYPE);
                    item.setSpeed(0);
                    item.setPercent(0);
                    //copy file
                    copyFileUsingFileChannels(source, dest, item);
                    item.setIsCompleted(true);
                    item.setPercent(100);
                    mVideoDatabaseHelper.markDownloadInfoAddEvent(item);
                    item.setFileName(dest.getName());
                    item.setFilePath(dest.getAbsolutePath());
                    item.setTaskState(VideoTaskState.SUCCESS);
                    mVideoDatabaseHelper.markDownloadProgressInfoUpdateEvent(item);
                    ApplicationDownloadTool.getInstance().onDownloadSuccess(item);
                    source.delete();
                }catch (Exception ignored){
                    ignored.printStackTrace();
                }

            }
        }).start();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String originPath = intent.getStringExtra("url");
            String title = intent.getStringExtra("title");
            String groupName = intent.getStringExtra("groupName");
            if (originPath == null || title == null)
                return;
            createVideoItem(originPath, title, groupName);

        }
    }



}