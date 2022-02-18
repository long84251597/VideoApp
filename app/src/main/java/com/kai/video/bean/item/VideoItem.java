package com.kai.video.bean.item;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.baozi.treerecyclerview.base.ViewHolder;
import com.baozi.treerecyclerview.item.TreeItem;
import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.model.VideoTaskState;
import com.kai.video.R;
import com.kai.video.activity.InfoActivity;
import com.kai.video.activity.PlayAcivity;
import com.kai.video.bean.GroupBean;
import com.shuyu.gsyvideoplayer.utils.NetworkUtils;

public class VideoItem extends TreeItem<GroupBean.VideoBean> {
    private static int ACTION_RESUME = 1;
    private static int ACTION_DELETE = 2;
    private static int ACTION_PLAY = 3;
    private static int ACTION_PLAY_WITH_DOWNLOADING = 4;
    private static int ACTION_PUASE = 5;


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
            viewHolder.setOnClickListener(R.id.list_container, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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
                }
            });


        }
        else if (data.getState() == VideoTaskState.PROXYREADY){
            viewHolder.setOnClickListener(R.id.list_container, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(view.getContext())
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
                            }).create().show();
                }
            });


        }else if (data.getState() == VideoTaskState.DOWNLOADING){
            viewHolder.setOnClickListener(R.id.list_container, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(view.getContext())
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
                            }).create().show();
                }
            });
        }
        else {
            viewHolder.setOnClickListener(R.id.list_container, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(view.getContext())
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
                            }).create().show();
                }
            });
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
        } else  if (action == ACTION_PUASE){
            item.setState(VideoTaskState.PAUSE);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
            Intent intent = new Intent("com.kai.video.LOCAL_BROADCAST1");
            intent.putExtra("videoAction", "update");
            intent.putExtra("item", DeliverVideoTaskItem.packBean(item));
            localBroadcastManager.sendBroadcast(intent);
            downloadManager.pauseDownloadTask(item.getUrl());
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
