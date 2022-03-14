package com.kai.video.bean.item;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.baozi.treerecyclerview.base.ViewHolder;
import com.baozi.treerecyclerview.item.TreeItem;
import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.database.VideoDownloadDatabaseHelper;
import com.jeffmony.downloader.model.Video;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.model.VideoTaskState;
import com.jeffmony.downloader.utils.ContextUtils;
import com.jeffmony.m3u8library.VideoProcessManager;
import com.jeffmony.m3u8library.listener.IVideoTransformListener;
import com.kai.video.R;
import com.kai.video.activity.DownloadActivity;
import com.kai.video.activity.InfoActivity;
import com.kai.video.activity.PlayAcivity;
import com.kai.video.bean.GroupBean;
import com.kai.video.tool.application.ApplicationDownloadTool;
import com.shuyu.gsyvideoplayer.utils.NetworkUtils;

import java.io.File;
import java.util.Objects;

public class VideoItem extends TreeItem<GroupBean.VideoBean> {
    private final static int ACTION_RESUME = 1;
    private final static int ACTION_DELETE = 2;
    private final static int ACTION_PLAY = 3;
    private final static int ACTION_PLAY_WITH_DOWNLOADING = 4;
    private final static int ACTION_PUASE = 5;
    private final static int ACTION_MERGE = 6;


    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder) {
        viewHolder.setText(R.id.speed, "");
        viewHolder.setProgress(R.id.percent, (int) data.getPercent());
        viewHolder.setText(R.id.title, data.getTitle().split("\\|")[0]);
        viewHolder.setText(R.id.source, "来源：" + data.getUrl());

        if (data.isHlsType()){
            viewHolder.setText(R.id.type, "hls");
            viewHolder.setTextColorRes(R.id.type, R.color.color_4k);
        }else {
            viewHolder.setText(R.id.type, "oct");
            viewHolder.setTextColorRes(R.id.type, R.color.color_720);
        }
        switch (data.getState()){
            case 0: viewHolder.setText(R.id.state, "获取信息中");break;
            case 1: viewHolder.setText(R.id.state, "下载准备中");break;
            case -1: viewHolder.setText(R.id.state, "下载排队中");break;
            case 2: viewHolder.setText(R.id.state, "开始下载");break;
            case 3:
                viewHolder.setText(R.id.state, data.getPercentString());
                viewHolder.setText(R.id.speed, data.getSpeedString());
                break;
            case 4:
                viewHolder.setText(R.id.state, "边下边播");
                viewHolder.setText(R.id.speed, data.getSpeedString());
                break;
            case 5:
                viewHolder.setText(R.id.state, "下载完成");
                viewHolder.setText(R.id.speed, data.getFileSize());
                break;
            case 6:viewHolder.setText(R.id.state, "下载错误");break;
            case 7: viewHolder.setText(R.id.state, "下载暂停");break;
            case 8:viewHolder.setText(R.id.state, "空间不足");break;

        }
        int position = viewHolder.getAdapterPosition();
        if (data.getState() == VideoTaskState.SUCCESS){
            viewHolder.setOnClickListener(R.id.list_container, view -> {
                if (data.isHlsType()){
                    new AlertDialog.Builder(view.getContext())
                            .setTitle("选项")
                            .setItems(new String[]{"立即播放", "删除下载", "合并视频"}, (dialog, which) -> {
                                switch (which) {
                                    case 0:
                                        handleAction(view, data, position, ACTION_PLAY);
                                        break;
                                    case 1:
                                        handleAction(view, data, position, ACTION_DELETE);
                                        break;
                                    case 2:
                                        handleAction(view, data, position, ACTION_MERGE);
                                        break;
                                }
                            }).create().show();
                }else
                    new AlertDialog.Builder(view.getContext())
                            .setTitle("选项")
                            .setItems(new String[]{"立即播放", "删除下载"}, (dialog, which) -> {
                                switch (which) {
                                    case 0:
                                        handleAction(view, data, position, ACTION_PLAY);
                                        break;
                                    case 1:
                                        handleAction(view, data, position, ACTION_DELETE);
                                        break;
                                }
                            }).create().show();
            });


        }
        else if (data.getState() == VideoTaskState.PROXYREADY){
            viewHolder.setOnClickListener(R.id.list_container, view -> new AlertDialog.Builder(view.getContext())
                    .setTitle("选项")
                    .setItems(new String[]{"暂停下载", "删除下载", "边下边播"}, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                handleAction(view, data, position, ACTION_PUASE);
                                break;
                            case 1:
                                handleAction(view, data, position, ACTION_DELETE);
                                break;
                            case 2:
                                handleAction(view, data, position, ACTION_PLAY_WITH_DOWNLOADING);
                                break;
                        }
                    }).create().show());


        }else if (data.getState() == VideoTaskState.DOWNLOADING){
            viewHolder.setOnClickListener(R.id.list_container, view -> new AlertDialog.Builder(view.getContext())
                    .setTitle("选项")
                    .setItems(new String[]{"暂停下载", "删除下载"}, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                handleAction(view, data, position, ACTION_PUASE);
                                break;
                            case 1:
                                handleAction(view, data, position, ACTION_DELETE);
                                break;
                        }
                    }).create().show());
        }
        else {
            viewHolder.setOnClickListener(R.id.list_container, view -> new AlertDialog.Builder(view.getContext())
                    .setTitle("选项")
                    .setItems(new String[]{"继续下载", "删除下载"}, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                handleAction(view, data, position, ACTION_RESUME);
                                break;
                            case 1:
                                handleAction(view, data, position, ACTION_DELETE);
                                break;
                        }
                    }).create().show());
        }


    }

    private void deleteVideoTaskItem(GroupBean.VideoBean item){
        try {
            if (item.isHlsType()){
                for(File file: Objects.requireNonNull(Objects.requireNonNull(new File(item.getPath()).getParentFile()).listFiles())){
                    file.delete();
                }
            }
            new File(item.getPath()).delete();
            new File(item.getCoverPath()).delete();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void handleAction(View view, GroupBean.VideoBean item, int position, int action){
        VideoDownloadManager downloadManager = VideoDownloadManager.getInstance();
        Context context = view.getContext();
        if (action == ACTION_RESUME) {
            downloadManager.resumeDownload(item.getUrl());
        } else if (action ==ACTION_DELETE) {
            getItemManager().removeItem(position);
            downloadManager.deleteVideoTask(item.getUrl(), true);
            deleteVideoTaskItem(item);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
            Intent intent = new Intent("com.kai.video.LOCAL_BROADCAST1");
            intent.putExtra("videoAction", "delete");
            intent.putExtra("item", DeliverVideoTaskItem.packBean(item));
            localBroadcastManager.sendBroadcast(intent);
        } else if (action == ACTION_PLAY) {
            if (item.getGroupName().equals("浏览器|外部下载")){
                Intent intent = new Intent(context, PlayAcivity.class);
                intent.putExtra("url", item.getPath());
                intent.putExtra("title", item.getTitle());
                intent.putExtra("extra", new Bundle());
                context.startActivity(intent);
            }else {
                if (NetworkUtils.isAvailable(context)){
                    Intent intent = new Intent(context, InfoActivity.class);
                    intent.putExtra("name", item.getTitle().split("\\|")[0]);
                    intent.putExtra("url", item.getTitle().split("\\|")[1]);
                    intent.putExtra("direct", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);
                }else {
                    Intent intent = new Intent(context, PlayAcivity.class);
                    intent.putExtra("url", item.getPath());
                    intent.putExtra("title", item.getTitle());
                    intent.putExtra("extra", new Bundle());
                    context.startActivity(intent);
                }


            }
        } else if (action == ACTION_PLAY_WITH_DOWNLOADING) {
            downloadManager.pauseDownloadTask(item.getUrl());
            Intent intent = new Intent(context, InfoActivity.class);
            intent.putExtra("name", item.getTitle().split("\\|")[0]);
            intent.putExtra("url", item.getTitle().split("\\|")[1]);
            intent.putExtra("direct", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } else if (action == ACTION_PUASE){
            item.setState(VideoTaskState.PAUSE);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
            Intent intent = new Intent("com.kai.video.LOCAL_BROADCAST1");
            intent.putExtra("videoAction", "update");
            intent.putExtra("item", DeliverVideoTaskItem.packBean(item));
            localBroadcastManager.sendBroadcast(intent);
            downloadManager.pauseDownloadTask(item.getUrl());
        } else if (action == ACTION_MERGE){
            ProgressDialog progressDialog = new ProgressDialog(view.getContext());
            progressDialog.setMessage("正在合并视频，请不要关闭程序");
            //设置弹窗标题
            progressDialog.setTitle("更新中");
            //设置弹窗图标
            progressDialog.setIcon(R.mipmap.ic_launcher_foreground);
            // 能够返回
            progressDialog.setCancelable(false);
            // 点击外部返回
            progressDialog.setCanceledOnTouchOutside(false);
            //设置进度条
            progressDialog.setProgress(0);
            //设置进度条是否明确
            progressDialog.setIndeterminate(true);
            //设置进度条样式
            //ProgressDialog.STYLE_SPINNER 环形精度条
            //ProgressDialog.STYLE_HORIZONTAL 水平样式的进度条
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.show();
            File file = new File(item.getPath());
            String outPath = new File(item.getPath()).getParentFile().getParentFile().getPath()+ "/" + file.getName().replace(".m3u8", "") + ".merge.mp4";
            VideoProcessManager.getInstance().transformM3U8ToMp4(item.getPath(), outPath, new IVideoTransformListener() {
                @Override
                public void onTransformProgress(float progress) {

                    ((Activity)context).runOnUiThread(() -> {
                        if (progressDialog.isIndeterminate())
                            progressDialog.setIndeterminate(false);
                        progressDialog.setProgress((int) progress);
                    });

                }

                @Override
                public void onTransformFailed(Exception e) {
                    ((Activity)context).runOnUiThread(() -> {
                        progressDialog.dismiss();
                        new AlertDialog.Builder(view.getContext())
                                .setTitle("合并失败")
                                .setNegativeButton("返回", (dialogInterface, i) -> dialogInterface.dismiss())
                                .create()
                                .show();
                    });
                }

                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onTransformFinished() {
                    ((Activity)context).runOnUiThread(() -> {
                        progressDialog.dismiss();
                        ((DownloadActivity)context).setMergeDialog(new AlertDialog.Builder(view.getContext())
                                .setTitle("合并成功")
                                .setMessage("请在确认视频可预览后再保存")
                                .setNegativeButton("返回", (dialogInterface, i) -> {
                                    dialogInterface.dismiss();
                                    ((DownloadActivity)context).setMergeDialog(null);
                                })
                                .setCancelable(false)
                                .setNeutralButton("预览", (dialogInterface, i) -> {
                                    Intent intent = new Intent(context, PlayAcivity.class);
                                    intent.putExtra("url", outPath);
                                    intent.putExtra("title", item.getTitle());
                                    intent.putExtra("extra", new Bundle());
                                    ((Activity) context).startActivity(intent);
                                })
                                .setPositiveButton("保存", (dialogInterface, i) -> {
                                    dialogInterface.dismiss();
                                    ((DownloadActivity)context).setMergeDialog(null);
                                    getItemManager().removeItem(position);
                                    downloadManager.deleteVideoTask(item.getUrl(), true);
                                    deleteVideoTaskItem(item);
                                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                                    Intent intent = new Intent("com.kai.video.LOCAL_BROADCAST1");
                                    intent.putExtra("videoAction", "delete");
                                    intent.putExtra("item", DeliverVideoTaskItem.packBean(item));
                                    localBroadcastManager.sendBroadcast(intent);
                                    VideoDownloadDatabaseHelper mVideoDatabaseHelper = new VideoDownloadDatabaseHelper(ContextUtils.getApplicationContext());
                                    VideoTaskItem videoTaskItem = new VideoTaskItem(outPath, item.getCoverPic(), item.getTitle(), item.getGroupName());
                                    videoTaskItem.setDownloadCreateTime(System.currentTimeMillis());
                                    videoTaskItem.setDownloadSize(new File(outPath).length());
                                    videoTaskItem.setMimeType("video/mp4");
                                    videoTaskItem.setLastUpdateTime(System.currentTimeMillis());
                                    videoTaskItem.setVideoType(Video.Type.MP4_TYPE);
                                    videoTaskItem.setSpeed(0);
                                    videoTaskItem.setPercent(100);
                                    videoTaskItem.setIsCompleted(true);
                                    videoTaskItem.setPercent(100);
                                    mVideoDatabaseHelper.markDownloadInfoAddEvent(videoTaskItem);
                                    videoTaskItem.setFileName(new File(outPath).getName());
                                    videoTaskItem.setFilePath(outPath);
                                    videoTaskItem.setTaskState(VideoTaskState.SUCCESS);
                                    mVideoDatabaseHelper.markDownloadProgressInfoUpdateEvent(videoTaskItem);
                                    ApplicationDownloadTool.getInstance().onDownloadSuccess(videoTaskItem);
                                })
                                .create());
                    });
                }
            });
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_item_download;
    }

    @Override
    public int getSpanSize(int maxSpan) {
        return maxSpan;
    }

}
