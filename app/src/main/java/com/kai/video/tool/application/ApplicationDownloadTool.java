package com.kai.video.tool.application;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.listener.DownloadListener;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.kai.video.activity.DownloadActivity;
import com.kai.video.activity.InfoActivity;
import com.kai.video.activity.PlayAcivity;
import com.kai.video.R;
import com.kai.video.bean.item.DeliverVideoTaskItem;

import java.util.HashMap;

public class ApplicationDownloadTool extends DownloadListener {
    @SuppressLint("StaticFieldLeak")
    public static ApplicationDownloadTool tool;
    private Context context;
    private int count = 0;
    private IntentFilter intentFilter;
    private LocalBroadcastManager localBroadcastManager;
    public static int ACTION_UPDATE = 0;
    private HashMap<String, Integer> map = new HashMap<>();
    private int getCount(String url){
        if (map.containsKey(url)){
            return map.get(url);
        }else{
            count++;
            map.put(url, count);
            return count;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDownloadStart(VideoTaskItem item) {
        if (localBroadcastManager != null){
            Intent intent = new Intent("com.kai.video.LOCAL_BROADCAST1");
            intent.putExtra("videoAction", "update");
            intent.putExtra("item", DeliverVideoTaskItem.pack(item));
            localBroadcastManager.sendBroadcast(intent);
        }
        String channelId = createNotificationChannel(NotificationManager.IMPORTANCE_LOW);
        String t = item.getTitle().split("\\|")[0];
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(t.length() > 15?t.substring(0, 15) + "...":t)
                .setContentText("开始下载")
                //.setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, 0, false)
                .setGroup("video")
                .setGroupSummary(true)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int count = getCount(item.getUrl());
        notificationManager.notify(count, notification.build());
        //item.setGroupName(String.valueOf(count));

    }

    @Override
    public void onDownloadProgress(VideoTaskItem item) {
        if (localBroadcastManager != null){
            Intent intent = new Intent("com.kai.video.LOCAL_BROADCAST1");
            intent.putExtra("videoAction", "update");
            intent.putExtra("item", DeliverVideoTaskItem.pack(item));
            localBroadcastManager.sendBroadcast(intent);
        }
        Intent intent = new Intent(context, DownloadActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        @SuppressLint("InlinedApi") String channelId = createNotificationChannel(NotificationManager.IMPORTANCE_LOW);
        String t = item.getTitle().split("\\|")[0];
        assert channelId != null;
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(t.length()>15?t.substring(0, 15) + "...":t)
                .setContentText("正在下载：" + item.getPercentString())
                .setContentIntent(createIntent(intent))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, (int) item.getPercent(), false)
                .setGroup("video")
                .setGroupSummary(true)
                .setAutoCancel(false);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Notification notification = notificationBuilder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(getCount(item.getUrl()), notification);
    }

    @Override
    public void onDownloadSpeed(VideoTaskItem item) {
        Log.e("tag-speed", item.getSpeedString() + ".*");
    }

    @Override
    public void onDownloadPending(VideoTaskItem item) {
        if (localBroadcastManager != null){
            Intent intent = new Intent("com.kai.video.LOCAL_BROADCAST1");
            intent.putExtra("videoAction", "update");
            intent.putExtra("item", DeliverVideoTaskItem.pack(item));
            localBroadcastManager.sendBroadcast(intent);
        }
        super.onDownloadPending(item);
    }

    @Override
    public void onDownloadPause(VideoTaskItem item) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(getCount(item.getUrl()));
    }

    @Override
    public void onDownloadError(VideoTaskItem item) {
        if (localBroadcastManager != null){
            Intent intent = new Intent("com.kai.video.LOCAL_BROADCAST1");
            intent.putExtra("videoAction", "update");
            intent.putExtra("item", DeliverVideoTaskItem.pack(item));
            localBroadcastManager.sendBroadcast(intent);
        }
        Intent intent = new Intent(context, DownloadActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        @SuppressLint("InlinedApi") String channelId = createNotificationChannel(NotificationManager.IMPORTANCE_LOW);
        String t = item.getTitle().split("\\|")[0];
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(t.length() > 15?t.substring(0, 15)+ "...":t)
                .setContentText("下载失败")
                .setContentIntent(createIntent(intent))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setDefaults(Notification.FLAG_ONLY_ALERT_ONCE)
                .setWhen(System.currentTimeMillis())
                .setGroup("video")
                .setGroupSummary(true)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(getCount(item.getUrl()), notification.build());
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private PendingIntent createIntent(Intent intent){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }else
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }



    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDownloadSuccess(VideoTaskItem item) {
        if (localBroadcastManager != null){
            item.setIsCompleted(true);
            Intent intent = new Intent("com.kai.video.LOCAL_BROADCAST1");
            intent.putExtra("videoAction", "success");
            intent.putExtra("item", DeliverVideoTaskItem.pack(item));
            localBroadcastManager.sendBroadcast(intent);
        }
        Intent intent;
        if (item.getGroupName().equals("浏览器|外部下载")){
            intent = new Intent(context, PlayAcivity.class);
            intent.putExtra("url", item.getFilePath());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("extra", new Bundle());
        }else {
            intent = new Intent(context, InfoActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("name", item.getTitle().split("\\|")[0]);
            intent.putExtra("url", item.getTitle().split("\\|")[1]);
            intent.putExtra("direct", true);
        }
        String channelId = createNotificationChannel(NotificationManager.IMPORTANCE_DEFAULT);
        String t = item.getTitle().split("\\|")[0];
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(t.length() > 15? t.substring(0, 15)+ "...":t)
                .setContentText("下载完成")
                .setContentIntent(createIntent(intent))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, (int) item.getPercent(), false)
                .setGroup("video")
                .setGroupSummary(true)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Notification notification = notificationBuilder.build();
        notificationManager.notify(getCount(item.getUrl()), notification);
    }
    
    private ApplicationDownloadTool(){

    }
    public static ApplicationDownloadTool getInstance(){
        if (tool == null)
            tool = new ApplicationDownloadTool();
        return tool;
    }

    private String createNotificationChannel(int level) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("video_download", "视频下载通知", level);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);
            return "video_download";
        } else {
            return null;
        }
    }
    
    public void init(Context context){
        this.context = context.getApplicationContext();
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
        VideoDownloadManager.getInstance().setGlobalDownloadListener(this);
        VideoDownloadManager.getInstance().setIgnoreAllCertErrors(true);
    }

    public void destory(){
        VideoDownloadManager.getInstance().setGlobalDownloadListener(null);
        localBroadcastManager = null;
        tool = null;
    }
    
    
}
