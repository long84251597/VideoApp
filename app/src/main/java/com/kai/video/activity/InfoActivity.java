package com.kai.video.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.kai.sniffwebkit.sniff.SniffTool;
import com.kai.video.R;
import com.kai.video.adapter.GroupAdapter;
import com.kai.video.adapter.SelectionItemAdapter;
import com.kai.video.bean.item.DeliverVideoTaskItem;
import com.kai.video.bean.obj.Api;
import com.kai.video.bean.obj.FloatingSimul;
import com.kai.video.floatUtil.FloatWindow;
import com.kai.video.manager.DeviceManager;
import com.kai.video.manager.MyPlayerManager;
import com.kai.video.manager.PlayerManager;
import com.kai.video.tool.application.SPUtils;
import com.kai.video.tool.log.LogUtil;
import com.kai.video.tool.net.VideoTool;
import com.kai.video.view.dialog.SelectionDialog;
import com.kai.video.view.other.LinearTopSmoothScroller;
import com.kai.video.view.player.TvPlayer;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;


public class InfoActivity extends BaseActivity{
    private LinearLayout playerContainer;
    public SelectionDialog selectionDialog;
    private PlayerManager playerManager;
    private LocalBroadcastManager localBroadcastManager;
    private LocalReceiver localReceiver;
    private IntentFilter intentFilter;
    public OrientationUtils orientationUtils;
    String name = "";
    public TextView header;
    //url是界面启动时传递URL的变量
    public String url = "";
    //用来与服务器通信及获取获取视频信息
    public VideoTool videoTool;
    //默认就是设置为TV
    //本地数据库存储对象
    public TvPlayer player;
    public Button fullButton;
    public Button nextButton;
    public Button changeButton;
    public TextView description;
    public TextView peroid;
    public RecyclerView recyclerView;
    public GridLayoutManager layoutManager;
    public SPUtils spUtils;
    public SelectionItemAdapter adapter;
    public GroupAdapter groupAdapter;
    public LinearLayoutManager groupManager;
    public RecyclerView groupView;
    //直接播放不加载历史
    public boolean direct = false;

    public List<String> apiNames = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        direct = getIntent().getBooleanExtra("direct", false);
        FloatingSimul.setPlayerType(FloatingSimul.TYPE_FORGROUND);
        MyPlayerManager.loadMode(InfoActivity.this);
        Iterator<String> iterable = Api.getApis().keys();
        while (iterable.hasNext()){
            apiNames.add(iterable.next());
        }
        Api.setSize(apiNames.size());
        if (DeviceManager.getDevice() == DeviceManager.DEVICE_TV){
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_info);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            LogUtil.d("TAG", "Running on a TV Device");
        }else if (DeviceManager.getDevice() == DeviceManager.DEVICE_PAD){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                InfoActivity.this.getWindow().setStatusBarColor(InfoActivity.this.getColor(R.color.colorPrimary));
            }
            setContentView(R.layout.activity_info);
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                InfoActivity.this.getWindow().setStatusBarColor(InfoActivity.this.getColor(R.color.colorPrimary));
            }
            setContentView(R.layout.activity_info_none_phone);
        }
        View searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> {
            if (player.isIfCurrentIsFullscreen()) {
                player.getcurrentPlayer().getStartButton().callOnClick();
                return;
            }
            Intent intent;
            if (DeviceManager.isTv()){
                intent = new Intent(InfoActivity.this, SearchTVActivity.class);
            }else {
                intent = new Intent(InfoActivity.this, SearchActivity.class);
            }
            intent.putExtra("wd", "");
            startActivity(intent);
        });
        videoTool = VideoTool.getInstance(this);
        Intent intent = getIntent();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.kai.video.LOCAL_BROADCAST1");
        localReceiver = new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver, intentFilter);
        name = intent.getStringExtra("name");
        url = Objects.requireNonNull(intent.getStringExtra("url"));
        spUtils = SPUtils.get(InfoActivity.this);
        header = findViewById(R.id.title);
        description = findViewById(R.id.description);
        peroid = findViewById(R.id.period);
        nextButton = findViewById(R.id.next);
        fullButton = findViewById(R.id.full);
        fullButton.requestFocus();
        changeButton = findViewById(R.id.change_api);
        spUtils.putValue("api", 0);
        changeButton.setText(apiNames.get(0).replaceAll("\\[.*", ""));
        recyclerView = findViewById(R.id.selection);
        groupView = findViewById(R.id.group_provider);
        adapter = new SelectionItemAdapter(new ArrayList<>());
        adapter.setOnListener(new SelectionItemAdapter.onListener() {
            @Override
            public void onEnsure(int currentPosition) {
                playerManager.fullHandler.postDelayed(() -> {
                    RecyclerView.SmoothScroller smoothScroller = new LinearTopSmoothScroller(InfoActivity.this);
                    smoothScroller.setTargetPosition(currentPosition);
                    layoutManager.startSmoothScroll(smoothScroller);
                }, 0);
            }

            @Override
            public void onClick(int position, int offset) {
                try {
                    updateTime(player.getCurrentPositionWhenPlaying());
                    playerManager.switchVideo(position);
                    fullButton.requestFocus();
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });

        layoutManager = new GridLayoutManager(InfoActivity.this, DeviceManager.isTv()?10:5);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        groupManager = new LinearLayoutManager(this);
        groupManager.setOrientation(RecyclerView.HORIZONTAL);
        groupAdapter = new GroupAdapter();
        groupView.setLayoutManager(groupManager);
        groupAdapter.setOnItemClickListener((index, header, tailer) -> {
            groupAdapter.setCurrent(index);
            layoutManager.scrollToPositionWithOffset(0,0);
            adapter.showList(header, tailer);
        });
        groupView.setAdapter(groupAdapter);
        fullButton.setOnClickListener(v -> {
            if (player.isIfCurrentIsFullscreen()){
                player.getCurrentPlayer().getStartButton().callOnClick();
                return;
            }
            //初始化不打开外部的旋转
            if (!DeviceManager.isTv())
                orientationUtils.resolveByClick();
            //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
            player.startWindowFullscreen(InfoActivity.this, false, false);

        });
        MyPlayerManager.loadScreen(this);
        player = findViewById(R.id.player);
        playerContainer = findViewById(R.id.player_container);
        //playerContainer.addView(player, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        playerManager = PlayerManager.getInstance(this);
        playerManager.initPlayer();
        playerManager.initVideoTool();
        Log.e("fetch", url);
        if (url.startsWith("//"))
            url = "https:" + url;
        videoTool.getInfo(url);
        orientationUtils = new OrientationUtils(InfoActivity.this, player);
        orientationUtils.setOnlyRotateLand(true);
        if (DeviceManager.isTv())
        orientationUtils.setEnable(false);
    }


    public void updateTime(long time){
        new Thread(() -> {
            try {
                videoTool.getHistoryManager().updateTime(videoTool.getInfo().getUrl(), time);
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }


    public String getUrl(){
        try {
            return videoTool.getInfo().getUrl();
        }catch (Exception e){
            return "";
        }
    }


    public static String toUtf8String(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= 255) {
                sb.append(c);
            } else {
                byte[] b;
                try {
                    b = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                } catch (Exception ex) {
                    b = new byte[0];
                }
                for (int value : b) {
                    int k = value;
                    if (k < 0)
                        k += 256;
                    sb.append("%").append(Integer.toHexString(k).toUpperCase());
                }
            }
        }
        return sb.toString();
    }
    public void deleteLog(){
        try {
            videoTool.getHistoryManager().deleteLog(videoTool.getWebsite(), videoTool.getInfo().getName());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void showSelectionDialog() {
        if (adapter.getItemCount() == 0)
            return;
        if (selectionDialog != null)
            selectionDialog.resume(adapter.getCurrent());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        playerManager.cancelFullTimer();
        //监听到按键操作自动取消全屏计时器
        if (player != null && player.isIfCurrentIsFullscreen()){
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
                player.getCurrentPlayer().getStartButton().callOnClick();
            }
            else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
                player.getcurrentPlayer().setSeekAdd(1);
            }
            else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
                player.getcurrentPlayer().setSeekAdd(-1);
            }
            else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE){
                player.hideAllDialog();
                player.getcurrentPlayer().backToNormalWindow();
            }
            else if (keyCode == KeyEvent.KEYCODE_DPAD_UP){
                player.getcurrentPlayer().showDamakuSetting();
            }
            else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
                player.getcurrentPlayer().changeApi(false);
            }
            else if (keyCode == KeyEvent.KEYCODE_MENU){
                player.getcurrentPlayer().showSetting();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU){
            Objects.requireNonNull(player).getcurrentPlayer().showSetting();
            return true;
        }else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY){
            Objects.requireNonNull(player).getCurrentPlayer().getStartButton().callOnClick();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    public void getVideoFromResult(){
        playerManager.getVideoFromResult();
    }

    public boolean isPause() {
        return pause;
    }

    private boolean pause = false;
    @Override
    protected void onPause() {
        pause = true;
        FloatingSimul.setPlayerType(FloatingSimul.TYPE_BACKGROUND_WITH_PAUSED);
        player.getCurrentPlayer().onVideoPause();
        videoTool.pause();
        SniffTool.destoryTool();
        super.onPause();
    }
    @Override
    public void onBackPressed() {
        //释放所有
        if (player.isIfCurrentIsFullscreen()){
            //Toast.makeText(InfoActivity.this, "shsh", Toast.LENGTH_SHORT).show();
            player.getFullWindowPlayer().getBackButton().callOnClick();
            return;
        }
        player.setRelease(true);
        player.setVideoAllCallBack(null);
        GSYVideoManager.releaseAllVideos();
        Objects.requireNonNull(player).destroyWeidgets();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (videoTool != null) {
            videoTool.destory();
            videoTool = null;
        }

        if (FloatWindow.get() != null)
            FloatWindow.destroy();
        player = null;
        localBroadcastManager.unregisterReceiver(localReceiver);
        localBroadcastManager = null;
        localReceiver = null;
        intentFilter = null;
        spUtils = null;
        playerManager.destory();
        playerManager = null;
        if (orientationUtils != null)
            orientationUtils.releaseListener();
        if (selectionDialog != null)
            selectionDialog.dismiss();
        playerContainer.removeAllViews();
        playerContainer = null;
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        pause = false;
        //localBroadcastManager.registerReceiver(localReceiver, intentFilter);
        //检测到小窗播放器有数据，就重载视频到播放处
        if (FloatingSimul.getPlayerType() == FloatingSimul.TYPE_BACKGROUND){
            player.resume(player.getCurrentPositionWhenPlaying());
            FloatingSimul.setPlayerType(FloatingSimul.TYPE_FORGROUND);
        }
        else if (FloatWindow.get() != null){
            player.closeWindowPlayer();
            //player.getcurrentPlayer().resume(player.getCurrentPositionWhenPlaying());
        }if (videoTaskItem != null){
            FloatingSimul.setPlayerType(FloatingSimul.TYPE_FORGROUND);
            Toast.makeText(InfoActivity.this, "播放缓存视频", Toast.LENGTH_SHORT).show();
            player.getcurrentPlayer().resume(player.getCurrentPositionWhenPlaying(), videoTaskItem);
            videoTaskItem = null;
        }else {
            FloatingSimul.setPlayerType(FloatingSimul.TYPE_FORGROUND);
        }
        //SniffTool.resumeTool();
        super.onResume();
    }
    private VideoTaskItem videoTaskItem = null;
    public class LocalReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String videoAction = intent.getStringExtra("videoAction");
                if (videoAction.equals("success")){
                    DeliverVideoTaskItem deliverVideoTaskItem = (DeliverVideoTaskItem) intent.getSerializableExtra("item");
                    VideoTaskItem item = DeliverVideoTaskItem.unpack(Objects.requireNonNull(deliverVideoTaskItem));
                    //如果缓存过程中发现对应的缓存完成
                    if (item.getTitle().equals(videoTool.getInfo().getTitle() + "|" + videoTool.getInfo().getUrl())){
                        player.getcurrentPlayer().setLocalCache(true);
                        if (FloatingSimul.getPlayerType() == FloatingSimul.TYPE_FORGROUND)
                            player.getcurrentPlayer().resume(player.getCurrentPositionWhenPlaying(), item);
                        else
                            videoTaskItem = item;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AudioServiceActivityLeak.preventLeakOf(newBase));
    }
}
