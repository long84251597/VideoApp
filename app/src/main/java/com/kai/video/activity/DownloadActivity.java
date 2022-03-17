package com.kai.video.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baozi.treerecyclerview.adpater.TreeRecyclerType;
import com.baozi.treerecyclerview.factory.ItemHelperFactory;
import com.baozi.treerecyclerview.item.TreeItem;
import com.baozi.treerecyclerview.item.TreeItemGroup;
import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.listener.IDownloadInfosCallback;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.model.VideoTaskState;
import com.kai.video.R;
import com.kai.video.adapter.MyTreeRecyclerAdapter;
import com.kai.video.bean.GroupBean;
import com.kai.video.bean.item.DeliverVideoTaskItem;
import com.kai.video.manager.DeviceManager;
import com.kai.video.manager.MyOrientoinListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class DownloadActivity extends BaseActivity{
    private IDownloadInfosCallback callback;
    private LocalBroadcastManager localBroadcastManager;
    private LocalReceiver receiver;
    private ProgressBar progressBar;
    private VideoDownloadManager downloadManager;
    private final MyTreeRecyclerAdapter adapter = new MyTreeRecyclerAdapter(TreeRecyclerType.SHOW_EXPAND);
    private List<GroupBean> groupItems = new ArrayList<>();
    private final Map<String, GroupBean> map = new HashMap<>();
    private Timer refreshTimer = null;
    private TimerTask refreshTask = null;
    private AlertDialog mergeDialog = null;

    public void setMergeDialog(AlertDialog mergeDialog) {
        if (mergeDialog != null)
            mergeDialog.show();
        this.mergeDialog = mergeDialog;
    }

    private void startTimer(){
        refreshTimer = new Timer();
        refreshTask = new TimerTask() {
            @Override
            public void run() {
                refresh();
            }
        };
        refreshTimer.schedule(refreshTask, 100, 1200);
    }

    private void stopTimer(){
        if (refreshTimer != null)
            refreshTimer.cancel();
        if (refreshTask != null)
            refreshTask.cancel();
        refreshTimer = null;
        refreshTask = null;
    }

    @Override
    protected int getTvLayout() {
        return R.layout.activity_download;
    }

    @Override
    protected int getPadLandLayout() {
        return R.layout.activity_download;
    }

    @Override
    protected int getPadPortLayout() {
        return R.layout.activity_download;
    }

    @Override
    protected int getPhonePortLayout() {
        return R.layout.activity_download;
    }

    @Override
    protected int getPhoneLandLayout() {
        return R.layout.activity_download;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        receiver = new LocalReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.kai.video.LOCAL_BROADCAST1");
        localBroadcastManager.registerReceiver(receiver, intentFilter);
        progressBar = findViewById(R.id.progress);
        RecyclerView recyclerView = findViewById(R.id.video_list);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 6));
        recyclerView.setItemAnimator(null);
        adapter.setHasStableIds(true);
        adapter.setOnItemClickListener((viewHolder, position) -> {

        });
        recyclerView.setAdapter(adapter);
        downloadManager = VideoDownloadManager.getInstance();
        callback = items -> runOnUiThread(() -> {
            try {
                for (VideoTaskItem item: items) {
                    if (map.containsKey(item.getGroupName()))
                        Objects.requireNonNull(map.get(item.getGroupName())).add(item);
                    else{
                        map.put(item.getGroupName(), new GroupBean(item));
                    }
                }
                groupItems = new ArrayList<>(map.values());
                refresh();
                startTimer();


            }catch (Exception e){
                e.printStackTrace();
            }

        });
        downloadManager.fetchDownloadItems(callback);
        //downloadManager.fetchDownloadItems();
    }



    private void refresh() {
        runOnUiThread(() -> {
            //创建item
            //新的
            progressBar.setVisibility(View.INVISIBLE);
            List<TreeItem> items = ItemHelperFactory.createItems(groupItems);
            //添加到adapter
            for (int i = 0; i < items.size(); i++) {
                TreeItemGroup treeItem = (TreeItemGroup) items.get(i);
                treeItem.setCanExpand(false);
                treeItem.setExpand(true);
            }
            adapter.getItemManager().replaceAllItem(items);
        });

    }
    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();

    }

    @Override
    protected void onResume() {
        super.onResume();
        startTimer();
        if (mergeDialog != null)
            mergeDialog.show();
    }

    @Override
    public void onDestroy() {
        downloadManager.removeDownloadInfosCallback(callback);
        localBroadcastManager.unregisterReceiver(receiver);
        super.onDestroy();
    }
    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String videoAction = intent.getStringExtra("videoAction");
            if (videoAction.equals("expand")){
                new Thread(() -> {
                    String groupName = intent.getStringExtra("groupName");
                    boolean expand = intent.getBooleanExtra("expand", false);
                    for(int i = 0; i < groupItems.size(); i++){
                        GroupBean bean = groupItems.get(i);
                        if (bean.getGroupName().equals(groupName)) {
                            bean.setExpand(expand);
                            break;
                        }
                    }
                    runOnUiThread(DownloadActivity.this::refresh);
                }).start();

                return;
            }
            DeliverVideoTaskItem deliverVideoTaskItem = (DeliverVideoTaskItem) intent.getSerializableExtra("item");
            assert deliverVideoTaskItem != null;
            VideoTaskItem item = DeliverVideoTaskItem.unpack(deliverVideoTaskItem);
            boolean find = false;
            for(int position = 0; position < groupItems.size(); position++){
                GroupBean groupBean = groupItems.get(position);
                if (groupBean.getGroupName().equals(item.getGroupName())){
                    groupBean.setAlive(false);
                    if (item.getTaskState() == VideoTaskState.DOWNLOADING)
                        groupBean.setAlive(true);
                    find = true;
                    if (videoAction.equals("update") || videoAction.equals("success")){
                        if (item.getTaskState() == VideoTaskState.DOWNLOADING)
                            groupBean.setAlive(true);
                        else if (item.getTaskState() == VideoTaskState.PAUSE || item.getTaskState() == VideoTaskState.SUCCESS)
                            groupBean.setAlive(false);
                        if (!groupBean.replace(item))
                            groupBean.add(item);
                    }else if (videoAction.equals("delete")){
                        groupBean.delete(item);
                        if (groupBean.getVideoBeans().size() == 0){
                            File picFile = new File(groupBean.getPoster());
                            if (picFile.exists()){
                                picFile.delete();
                            }
                            groupItems.remove(position);
                        }
                    }
                    break;
                }
            }
            if (!find){
                groupItems.add(new GroupBean(item));
            }
        }
    }





}
