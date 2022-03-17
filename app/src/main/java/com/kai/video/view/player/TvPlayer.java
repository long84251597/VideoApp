package com.kai.video.view.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.BounceInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.IPTool;
import com.danikula.videocache.ProxyCacheUtils;
import com.google.android.material.snackbar.Snackbar;
import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.model.Video;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.kai.sniffwebkit.LoadingView;
import com.kai.sniffwebkit.sniff.SniffingVideo;
import com.kai.video.R;
import com.kai.video.activity.DownloadActivity;
import com.kai.video.activity.InfoActivity;
import com.kai.video.activity.SniffActivity;
import com.kai.video.adapter.DanamakuAdapter;
import com.kai.video.bean.danmu.DanmuFile;
import com.kai.video.bean.obj.FloatingSimul;
import com.kai.video.bean.obj.Quality;
import com.kai.video.floatUtil.FloatWindow;
import com.kai.video.floatUtil.MoveType;
import com.kai.video.floatUtil.Screen;
import com.kai.video.manager.DeviceManager;
import com.kai.video.manager.MyPlayerManager;
import com.kai.video.manager.ProxyCacheManager;
import com.kai.video.service.TransService;
import com.kai.video.tool.application.GC;
import com.kai.video.tool.application.SPUtils;
import com.kai.video.tool.danmu.MyDanmakuParser;
import com.kai.video.tool.file.FileUtils;
import com.kai.video.tool.log.LogUtil;
import com.kai.video.view.battery.BatteryView;
import com.kai.video.view.battery.OnBatteryPowerListener;
import com.kai.video.view.dialog.CustomDialog;
import com.kai.video.view.other.CustomTimeView;
import com.shuyu.gsyvideoplayer.listener.VideoAllCallBack;
import com.shuyu.gsyvideoplayer.utils.CommonUtil;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.utils.NetworkUtils;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;

public class TvPlayer extends NormalGSYVideoPlayer implements PopupMenu.OnMenuItemClickListener {
    private static final String[] settingArray = new String[]{"线路设置", "内核设置", "下载设置", "倍速设置", "画面设置", "弹幕信息", "视频信息", "视频报错", "小窗模式","催更视频", "全网搜索此影片"};
    private CustomDialog settingDialog;
    private CustomDialog changeDialog;
    private CustomDialog danmuDialog;
    private CustomDialog mediaDialog;
    //浮窗的宽度
    private static final float FLOAT_WIDTH = DeviceManager.isTv()?0.4f:0.8f;
    //浮窗的高度
    private static final float FLOAT_HEIGHT =  FLOAT_WIDTH/16.0f*9.0f;

    //浮窗的横坐标
    private static final float FLOAT_X = DeviceManager.isTv()?0.9f:0.8f;
    //浮窗的纵坐标
    private static final float FLOAT_Y = DeviceManager.isTv()?1.0f:0.6f;
    //自滚动字体的宽度
    private static final int ems_normal = DeviceManager.isTv()?11:7;
    private static final int ems_full = DeviceManager.isTv()?35:30;
    //弹幕滚动速度
    private static final float mDanmaKuSpeed = 1.2f;
    private TextView qualityView;
    private OnBatteryPowerListener onBatteryPowerListener;
    private FloatingVideo floatingVideo;
    private String quality = "";
    private boolean localCache;
    private String url;
    private String title;
    private boolean cacheWithPlay;
    private RelativeLayout mDamakuBar;
    private RelativeLayout surface;
    private ImageView setting;
    boolean mDanmaKuShow = true;
    private CustomTimeView clockView;
    private BatteryView batteryView;
    private BaseDanmakuParser mParser;//解析器对象
    private IDanmakuView mDanmakuView;//弹幕view
    private DanmakuContext mDanmakuContext;
    private File danmuFile;
    private boolean release;
    private long mDanmakuStartSeekPosition = -1;
    private boolean prepared = false;
    private TextView contentType;
    private TextView address;
    private LoadingView loadingView;

    private SniffingVideo sniffingVideo = new SniffingVideo("", "");//存储视频嗅探本体

    public LoadingView getLoadingView() {
        return loadingView;
    }

    public void setSniffingVideo(SniffingVideo sniffingVideo, long seekTime, String tname) {
        this.sniffingVideo = sniffingVideo;
        this.danmuFile = null;
        this.mParser = null;
        String url = sniffingVideo.getUrl();
        boolean cache = true;
        if (url.contains("subaibai")) {
            url = IPTool.getLocal() + "/m3u8?url=" + URLEncoder.encode(url);
            cache = false;
        }else if (sniffingVideo.getType().equals("m3u8") || (sniffingVideo.getType().equals("vkey") && sniffingVideo.getUrl().contains(".m3u8")))
            cache = false;
        else if (ProxyCacheUtils.isConcatList(url)){
            cache = url.contains("v.qq.com");
        }



        //默认不使用边下边看
        setSeekOnStart(seekTime);
        setUp(url, cache,null, sniffingVideo.getHeaders(), tname);
        startPlayLogic();
    }



    @Override
    public GSYBaseVideoPlayer getCurrentPlayer() {
        if (getFullWindowPlayer() != null) {
            return getFullWindowPlayer();
        }
        if (getSmallWindowPlayer() != null) {
            return getSmallWindowPlayer();
        }
        return this;
    }

    public void setLocalCache(boolean localCache) {
        this.localCache = localCache;
    }

    public boolean isRelease() {
        return release;
    }

    private Runnable bufferRunnable = () -> {

    };
    @Override
    public void release() {
        mProgressBar.setProgress(0);
        danmakuOnPause();
        super.release();
        danmuFile = null;
        getSurface().removeCallbacks(bufferRunnable);
        setRelease(true);
    }
    @Override
    public void setVideoAllCallBack(VideoAllCallBack mVideoAllCallBack) {
        super.setVideoAllCallBack(mVideoAllCallBack);
    }

    public void setRelease(boolean release) {
        if (release)
        try {
            if (getDanmakuView() != null && getDanmakuView().isPrepared())
                ((TvPlayer)getCurrentPlayer()).getDanmakuView().clearDanmakusOnScreen();
        }catch (Exception e){
            e.printStackTrace();
        }


        this.release = release;
    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_tv_player;
    }

    public TvPlayer(Context context) {
        super(context);
    }

    public TvPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }



    boolean buffering = false;
    @Override
    protected void updateStartImage() {
        super.updateStartImage();
        if (getCurrentState() == CURRENT_STATE_PLAYING && buffering) {
            danmakuOnResume();
            buffering = false;

        }
    }

    @Override
    protected void setViewShowState(View view, int visibility) {
        super.setViewShowState(view, visibility);
        if (view.getId() == R.id.start && isIfCurrentIsFullscreen()){
            int v = visibility==VISIBLE?GONE:VISIBLE;
            if (DeviceManager.isTv()) {
                tvAlert.setVisibility(visibility);
            }
            else
                batteryView.setVisibility(v);
            clockView.setVisibility(v);
        }
    }

    @Override
    protected void hideAllWidget() {
        super.hideAllWidget();
    }


    @Override
    protected void changeUiToPreparingShow() {
        super.changeUiToPreparingShow();
        buffering = true;
    }
    @Override
    protected void changeUiToPlayingBufferingShow() {
        //处于加载时停止弹幕的滚动
        super.changeUiToPlayingBufferingShow();
        danmakuOnPause();
        LogUtil.d("tag", "buffering");
        buffering = true;
        danmakuOnPause();
    }

    @Override
    protected void changeUiToError() {
        //播放失败时停止弹幕的滚动
        super.changeUiToError();
        danmakuOnPause();
    }


    @Override
    public void seekTo(long position) {
        super.seekTo(position);
    }

    public float dp2px(float dp){
        final float scale = getResources().getDisplayMetrics().density;
        return (dp / scale + 0.5f);
    }
    private void initDanmaku() {
        LogUtil.d("tag", "初始化弹幕上下文");
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
        DanamakuAdapter danamakuAdapter = new DanamakuAdapter(mDanmakuView);
        mDanmakuContext = DanmakuContext.create();
        mDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3)
                .setDuplicateMergingEnabled(DeviceManager.isTv())
                .setScrollSpeedFactor(DeviceManager.isTv()?1.65f:1.2f)
                .setScaleTextSize(dp2px(3))
                .setCacheStuffer(new SpannedCacheStuffer(), danamakuAdapter) // 图文混排使用SpannedCacheStuffer
                .preventOverlapping(overlappingEnablePair);
        if (!isIfCurrentIsFullscreen()){
            loadDanmuSetting(4);
        }else
            loadDanmuSetting(spUtils.getValue("danmu", 4));
        if (mDanmakuView != null) {
            if (danmuFile != null) {
                mParser = getParser();
            }

            mDanmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
                @Override
                public void updateTimer(DanmakuTimer timer) {

                }

                @Override
                public void drawingFinished() {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {

                }

                @Override
                public void prepared() {
                    if (mDanmakuView != null && mDanmakuContext !=null) {
                        resolveDanmakuShow();
                        //Toast.makeText(getActivityContext(), "弹幕加载完毕" , Toast.LENGTH_SHORT).show();
                        if (getCurrentState() == CURRENT_STATE_PAUSE || buffering) {
                            setDanmakuStartSeekPosition(getCurrentPositionWhenPlaying());
                            resolveDanmakuSeek(TvPlayer.this, getDanmakuStartSeekPosition());
                            danmakuOnPause();
                            setDanmakuStartSeekPosition(-1);
                        }
                        else {
                            setDanmakuStartSeekPosition(getCurrentPositionWhenPlaying());
                            resolveDanmakuSeek(TvPlayer.this, getDanmakuStartSeekPosition());
                            setDanmakuStartSeekPosition(-1);
                        }
                    }
                }
            });
            mDanmakuView.enableDanmakuDrawingCache(!DeviceManager.isTv());
        }


    }



    @Override
    public void onError(int what, int extra) {
        super.onError(what, extra);

    }

    @Override
    public void onSurfaceUpdated(Surface surface) {
        super.onSurfaceUpdated(surface);
    }

    public InfoActivity getActivity(){
        return (InfoActivity) mContext;
    }



    @Override
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
        GSYBaseVideoPlayer gsyBaseVideoPlayer = super.startWindowFullscreen(context, actionBar, statusBar);
        if (gsyBaseVideoPlayer != null) {
            //如果是TV设备先死按键说明
            TvPlayer gsyVideoPlayer = (TvPlayer) gsyBaseVideoPlayer;
            ((TvPlayer) gsyBaseVideoPlayer).initSputils(getActivity());
            gsyVideoPlayer.setQuality(quality);
            gsyVideoPlayer.prepared = true;
            gsyVideoPlayer.setViewShowState(((TvPlayer) gsyBaseVideoPlayer).batteryView, GONE);
            gsyVideoPlayer.batteryView.setLifecycleOwner(getActivity());
            gsyVideoPlayer.batteryView.setChargingSpeed(3);
            gsyVideoPlayer.onBatteryPowerListener = power -> batteryView.setPower(power);
            gsyVideoPlayer.danmuFile = danmuFile;
            gsyVideoPlayer.mTitleTextView.setMaxEms(ems_full);
            gsyVideoPlayer.address.setText(sniffingVideo.getUrl().replaceAll("\\?.*", ""));
            gsyVideoPlayer.contentType.setText(sniffingVideo.getContentType());
            gsyVideoPlayer.contentType.setVisibility(VISIBLE);
            gsyVideoPlayer.address.setVisibility(VISIBLE);
            gsyVideoPlayer.loadDanmuSetting(SPUtils.get(context).getValue("danmu", 4));
            if (DeviceManager.isTv()){
                gsyVideoPlayer.tvAlert.setVisibility(VISIBLE);
            }else {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                ((TvPlayer)gsyBaseVideoPlayer).initDanmuSetting(context);
                gsyBaseVideoPlayer.setRotateViewAuto(true);
                gsyBaseVideoPlayer.setOnlyRotateLand(true);
                gsyVideoPlayer.setting.setVisibility(VISIBLE);

            }
            gsyVideoPlayer.setDanmakuStartSeekPosition(getCurrentPositionWhenPlaying());
            gsyVideoPlayer.setDanmaKuShow(getDanmaKuShow());
            onPrepareDanmaku(gsyVideoPlayer);

        }
        return gsyBaseVideoPlayer;

    }

    public void stopClock(){
        if (clockView != null)
            clockView.stop();
    }



    @Override
    protected void resolveNormalVideoShow(View oldF, ViewGroup vp, GSYVideoPlayer gsyVideoPlayer) {
        //退出全屏播放时更新进度
        getActivity().updateTime(getCurrentPositionWhenPlaying());
        if (gsyVideoPlayer != null) {
            TvPlayer gsyDanmaVideoPlayer = (TvPlayer) gsyVideoPlayer;
            gsyDanmaVideoPlayer.tvAlert.setVisibility(INVISIBLE);
            gsyDanmaVideoPlayer.batteryView.setVisibility(GONE);
            gsyDanmaVideoPlayer.batteryView.removeOnBatteryPowerListener();
            gsyDanmaVideoPlayer.clockView.setVisibility(INVISIBLE);
            gsyDanmaVideoPlayer.setDanmaKuShow(gsyDanmaVideoPlayer.getDanmaKuShow());
            if (gsyDanmaVideoPlayer.getDanmakuView() != null && gsyDanmaVideoPlayer.getDanmakuView().isPrepared()) {
                gsyDanmaVideoPlayer.getDanmakuView().stop();
                gsyDanmaVideoPlayer.getDanmakuView().clearDanmakusOnScreen();
                gsyDanmaVideoPlayer.getDanmakuView().hideAndPauseDrawTask();

            }
            if (gsyDanmaVideoPlayer.loadingView.getVisibility() == VISIBLE){
                gsyDanmaVideoPlayer.loadingView.hide();
                loadingView.show();
                getActivity().videoTool.rebindLoadingTool(loadingView);
            }
            gsyDanmaVideoPlayer.destroyWeidgets();
            release = gsyDanmaVideoPlayer.release;
            tvAlert.setVisibility(INVISIBLE);
        }

        batteryView.setVisibility(GONE);
        clockView.setVisibility(INVISIBLE);
        super.resolveNormalVideoShow(oldF, vp, gsyVideoPlayer);
        GC.clean();
    }


    public boolean isPrepared() {
        return prepared;
    }

    @Override
    protected void cloneParams(GSYBaseVideoPlayer from, GSYBaseVideoPlayer to) {
        Log.d("tag",  " 传递数据");
        try {
            TvPlayer to_tv = (TvPlayer) to;
            TvPlayer from_tv = (TvPlayer) from;
            to_tv.title = from_tv.title;
            to_tv.url = from_tv.url;
            to_tv.cacheWithPlay = from_tv.cacheWithPlay;
            to_tv.quality = from_tv.quality;
            to_tv.localCache = from_tv.localCache;
            to_tv.release = from_tv.release;
            to_tv.sniffingVideo = from_tv.sniffingVideo;
            to_tv.danmuFile = from_tv.danmuFile;
            to_tv.prepared = prepared;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            super.cloneParams(from, to);
        }


    }
    public void loadDanmuSetting(int i){
        float percent = 1f;
        int maxTop = DeviceManager.isTv()?5:5;
        int maxRL = DeviceManager.isTv()?20:15;
        switch (i){
            case 0:percent = 1.0f;maxTop = 5;break;
            case 1:percent = 0.5f;maxTop = 4;break;
            case 2:percent = 0.25f;maxTop = 3;break;
            case 3:percent = 0.15f;maxTop= 2;break;
            case 4:percent = 0f;maxTop = 0;break;
        }
        if (i == 4 && mDanmaKuShow){
            mDanmaKuShow = false;
            resolveDanmakuShow();
        }else if (i != 4){
            mDanmaKuShow = true;
            if (danmuFile == null)
                loadDanmu();
            resolveDanmakuShow();
        }
        maxRL = (int) (percent * maxRL);
        HashMap<Integer, Integer> maxLinesPair = new HashMap<>();
        maxLinesPair.put(BaseDanmaku.TYPE_FIX_TOP, maxTop);
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, maxRL); // 滚动弹幕最大显示5行
        mDanmakuContext.setMaximumLines(maxLinesPair);
    }

    public void showDanmuState(){
        List<String> items = new ArrayList<>();
        items.add("弹幕缓存状态：" + (danmuFile == null ? "未缓存" : "已缓存"));
        items.add("弹幕缓存大小：" + (danmuFile == null ? "0KB" : FileUtils.readableFileSize(danmuFile.length())));
        items.add("弹幕总条数：" + (mParser == null? "0" : mParser.getDanmakus().size() + ""));
        new CustomDialog.Builder(mContext, getTheme())
                .setTitle("弹幕信息")
                .setList(items, null, -1)
                .setOnCancelListner(dialog -> settingDialog.show())
                .setOnItemClickListener((item, o, position, dialog) -> dialog.dismiss())
                .create()
                .show();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

    }

    public void showVideoState(){
        Quality quality = new Quality(getCurrentVideoWidth());
        quality.get();
        String[] its = MyPlayerManager.getInfo(mContext);
        List<String> messages = new ArrayList<>();
        messages.add("视频分辨率：" + getCurrentVideoWidth() + " x " + getCurrentVideoHeight());
        messages.add("视频清晰度：" + quality.get());
        messages.add("视频来源站：" + getActivity().getUrl());
        messages.add("视频源地址：" + mUrl);
        messages.add("视频请求头：" + JSONObject.toJSONString(mMapHeadData));
        messages.add("视频总时长：" + translateLong((long)getDuration()));
        messages.add("视频资源站：" + getActivity().apiNames.get(spUtils.getValue("api", 0)).replaceAll("\\s.*", ""));
        messages.add("视频解码器：" + its[1]);
        messages.add("播放器内核：" + its[0]);
        messages.add("视频内容类型：" + sniffingVideo.getContentType());
        messages.add("视频文件类型：" + sniffingVideo.getType());



        new CustomDialog.Builder(mContext, getTheme())
                .setTitle("视频信息")
                .setList(messages, null, -1)
                .setOnCancelListner(dialog -> settingDialog.show())
                .setOnItemClickListener((item, o, position, dialog) -> {
                    Toast.makeText(mContext, item, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                })
                .create()
                .show();
    }
    public void showErrorAlert(){
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle("视频报错")
                .setMessage("如果视频内容与描述不符或者无法播放，请点击立即反馈，管理系统将后续修正视频。")
                .setNegativeButton("暂时忽略", (dialog1, which) -> dialog1.cancel())
                .setPositiveButton("立即反馈", (dialog12, which) -> {
                    getActivity().deleteLog();
                    Snackbar.make(getSurface(), "反馈已经提交，请暂时切换其他线路，视频后续会修复", Snackbar.LENGTH_LONG).setAction("Action", null).setBackgroundTint(getResources().getColor(R.color.colorPrimary)).show();
                }).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
    }
    public void changeApi(boolean showSetting){
        if (changeDialog != null && changeDialog.isShowing())
            return;
        if (changeDialog == null){
            changeDialog = new CustomDialog.Builder(mContext, getTheme())
                    .setTitle("线路设置")
                    .setMessage("请选择画质合适的线路观看")
                    .setList(getActivity().apiNames, null, spUtils.getValue("api", 0))
                    .setOnCancelListner(dialog -> {
                        if (showSetting)
                            showSetting();
                    })
                    .setOnItemClickListener((item, o, i, dialog) -> {
                        spUtils.putValue("api", i);
                        getActivity().updateTime(getCurrentPositionWhenPlaying());
                        getActivity().fullButton.requestFocus();
                        getActivity().changeButton.setText(getActivity().apiNames.get(i).replaceAll("\\s.*", ""));
                        getActivity().getVideoFromResult();
                        dialog.dismiss();
                    })
                    .create();
            changeDialog.show();
        }else {
            changeDialog.setCurrent(spUtils.getValue("api", 0));
            changeDialog.resume();
        }

    }
    private ViewGroup getViewGroup() {
        return (CommonUtil.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
    }


    @Override
    public GSYVideoPlayer getSmallWindowPlayer() {
        return floatingVideo;
    }

    public void closeWindowPlayer(){
        mCurrentState = getGSYVideoManager().getLastState();
        cloneParams(getSmallWindowPlayer(), this);
        floatingVideo = null;
        createNetWorkState();
        final ViewGroup vp = getViewGroup();

        final View oldF = vp.findViewById(getSmallId());
        if (oldF != null && oldF.getParent() != null) {
            ViewGroup viewGroup = (ViewGroup) oldF.getParent();
            vp.removeView(viewGroup);
        }
        if (FloatWindow.get() != null)
            FloatWindow.destroy();
        getGSYVideoManager().setListener(this);
        getGSYVideoManager().setLastListener(null);
        setStateAndUi(mCurrentState);
        if (mVideoAllCallBack != null) {
            Debuger.printfError("onQuitSmallWindow");
            mVideoAllCallBack.onQuitSmallWidget(mOriginUrl, mTitle, this);
        }
        mSaveChangeViewTIme = System.currentTimeMillis();
        startProgressTimer();
        if (mCurrentState != CURRENT_STATE_PAUSE)
            resume(getCurrentPositionWhenPlaying());
    }

    private void createWindowPlayer(TvPlayer player){
        if (player != null && !player.mHadPlay)
            return;
        assert player != null;
        player.getSurface().postDelayed(() -> {
            FloatingSimul.setPlayerType(FloatingSimul.TYPE_BACKGROUND);
            player.cancelProgressTimer();
            FloatPlayerView floatPlayerView = new FloatPlayerView(player.mContext.getApplicationContext());
            floatPlayerView.getVideoPlayer().setId(getSmallId());
            player.cloneParams(player, floatPlayerView.getVideoPlayer());
            floatPlayerView.getVideoPlayer().setVideoAllCallBack(player.mVideoAllCallBack);
            floatPlayerView.getVideoPlayer().setIfCurrentIsFullscreen(false);
            floatPlayerView.getVideoPlayer().addTextureView();
            floatPlayerView.getVideoPlayer().startProgressTimer();
            player.getGSYVideoManager().setLastListener(player);
            getGSYVideoManager().setListener(floatPlayerView.getVideoPlayer());
            checkoutState();
            player.floatingVideo = floatPlayerView.getVideoPlayer();
            player.mVideoAllCallBack.onEnterSmallWidget(url, title, floatPlayerView.getVideoPlayer());
            FloatWindow
                    .with(player.mContext.getApplicationContext())
                    .setView(floatPlayerView)
                    .setWidth(Screen.width, FLOAT_WIDTH)
                    .setHeight(Screen.width, FLOAT_HEIGHT)
                    .setX(Screen.width, FLOAT_X)
                    .setY(Screen.height, FLOAT_Y)
                    .setMoveType(MoveType.slide)
                    .setFilter(false)
                    .setOnDestroyListener(time -> {

                    })
                    .setMoveStyle(500, new BounceInterpolator())
                    .build();
            FloatWindow.get().show();
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            player.mContext.startActivity(intent);

        }, 1000);

    }

    @Override
    public void startAfterPrepared() {
        super.startAfterPrepared();
        prepared = true;
        address.setText(sniffingVideo.getUrl().replaceAll("\\?.*", ""));
        contentType.setText(sniffingVideo.getContentType());
    }

    private void startWindowPlayer(){
        if (FloatWindow.get() != null) {
            return;
        }
        getStartButton().callOnClick();
        createWindowPlayer(getActivity().player);
        if (isIfCurrentIsFullscreen())
            getFullscreenButton().callOnClick();



    }
    private int getTheme(){
        if (isIfCurrentIsFullscreen())
            return R.style.BannerDialog;
        else 
            return DeviceManager.getDialogTheme();
    }


    public void showSetting(){
        if (settingDialog == null)
            settingDialog = new CustomDialog.Builder(mContext, getTheme())
                    .setMessage("播放设置页")
                    .setTitle("设置")
                    .setList(Arrays.asList(TvPlayer.settingArray), null, -1)
                    .setOnCancelListner(dialog -> {})
                    .setOnItemClickListener((item, o, which, dialog) -> {
                        if (which == 0){
                            changeApi(true);
                        }
                        else if (which == 1){
                            showVideoSetting();
                        }
                        else if (which == 2){
                            showDownloadSetting(true);
                        }
                        else if (which == 3){
                            showSpeedSetting();
                        }
                        else if (which == 4){
                            showScreenSetting();
                        }
                        else if (which == 5){
                            showDanmuState();
                        }
                        else if (which == 6){
                            showVideoState();
                        }
                        else if (which == 7){
                            showErrorAlert();
                        }
                        else if (which == 8){
                            startWindowPlayer();
                        }
                        else if (which == 9){
                            new Thread(() -> {
                                try {
                                    Jsoup.connect(IPTool.getLocal() + "/VideoServlet")
                                            .data("name", getActivity().videoTool.getInfo().getName())
                                            .data("videoType", getActivity().videoTool.getInfo().getVideoType())
                                            .data("type", getActivity().videoTool.getInfo().getType() +"")
                                            .method(Connection.Method.POST)
                                            .execute();
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getActivity(), "请重新进入视频，我们已经标记了更新", Toast.LENGTH_LONG).show();
                                        getActivity().finish();
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                        else if (which == 10){
                            Intent intent= new Intent(mContext, SniffActivity.class);
                            intent.putExtra("wd", getActivity().videoTool.getInfo().getName());
                            mContext.startActivity(intent);
                        }
                        dialog.hide();

                    })
                    .create();
        settingDialog.show();
    }



    private static final String[] cacheArray = new String[]{"一键下载", "下载管理", "一键分享"};
    private void showDownloadSetting(boolean showSetting){
        List<String> cacheList = new ArrayList<>(Arrays.asList(cacheArray));
        if (localCache)
            cacheList.set(0, "删除下载");
        StringBuilder builder = new StringBuilder();
        builder.append("将视频永久下载到本地保存\n" + "当前状态：" );
        if (localCache){
            builder.append("已下载（").append(FileUtils.readableFileSize(new File(mUrl).length())).append("）");
        }else if (ProxyCacheManager.instance().isCached(mUrl, mContext)){
            builder.append("已缓存（临时缓存，可极速转存至本地）");
        }else {
            builder.append("未下载");
            if (ProxyCacheUtils.isConcatList(mUrl))
                builder.append(" (当前视频仅支持临时缓存，无法直接下载）");
            else if (isCached(mUrl)){
                builder.append(" (当前视频支持临时缓存：可直接下载，也可在缓存进度满后极速转存至本地）");
            }else
                builder.append("（当前视频仅支持直接下载）");
        }

        new CustomDialog.Builder(mContext, getTheme())
                .setTitle("下载设置")
                .setMessage(builder.toString())
                .setList(cacheList, null, -1)
                .setOnCancelListner(dialog -> {
                    if (showSetting)
                        showSetting();
                })
                .setOnItemClickListener((item, o, which, dialog) -> {
                    dialog.dismiss();
                    String downloadTitle = mTitle + "|" + getActivity().getUrl();
                    String downloadGroup = getActivity().videoTool.getInfo().getOutput() + "|" + getActivity().videoTool.getInfo().getVideoType();
                    if (which == 0){
                        if (localCache){
                            release();
                            Toast.makeText(mContext, "删除成功", Toast.LENGTH_SHORT).show();
                            getActivity().videoTool.deleteCache(mUrl);
                            getActivity().getVideoFromResult();
                        }else if (ProxyCacheManager.instance().isCached(mUrl, mContext)){
                            getActivity().videoTool.deleteVideo(downloadTitle, downloadGroup);
                            Toast.makeText(mContext, "视频已经缓存，正在后台转移", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(mContext, TransService.class);
                            intent.putExtra("title", downloadTitle);
                            intent.putExtra("url", ProxyCacheManager.instance().getFilePath(mUrl, mContext));
                            intent.putExtra("groupName", downloadGroup);
                            intent.putExtra("cover", getActivity().videoTool.getInfo().getCoverPic());
                            mContext.startService(intent);


                        }else if (!ProxyCacheUtils.isConcatList(mUrl)){

                            if (!getActivity().videoTool.isDownloading(downloadTitle, downloadGroup, mUrl)){
                                Toast.makeText(mContext, "视频开始下载", Toast.LENGTH_SHORT).show();
                                getActivity().videoTool.deleteVideo(downloadTitle, downloadGroup);
                                VideoTaskItem item1 = new VideoTaskItem(mUrl, getActivity().videoTool.getInfo().getCoverPic(), downloadTitle, getActivity().videoTool.getInfo().getOutput() + "|" + getActivity().videoTool.getInfo().getVideoType());
                                if (mUrl.contains("analysis?url=")){
                                    Map<String, String> map = new HashMap<>(1);
                                    map.put("Referer", "download");
                                    setMapHeadData(map);
                                    item1.setVideoType(Video.Type.HLS_TYPE);
                                }
                                VideoDownloadManager.getInstance().startDownload(item1, mMapHeadData);
                            }else {
                                Toast.makeText(mContext, "该视频已在下载队列中", Toast.LENGTH_SHORT).show();
                            }


                        }else {
                            Toast.makeText(mContext, "该视频不支持下载", Toast.LENGTH_SHORT).show();
                        }



                    }
                    else if (which == 1){
                        Intent intent = new Intent(mContext, DownloadActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        mContext.startActivity(intent);
                    }else if (which == 2){
                        shareFile(mUrl);
                    }
                })
                .create().show();
    }


    private static final String[] screenArray = new String[]{"默认", "16:9", "4:3", "全屏裁剪", "全屏拉伸"};
    public void showScreenSetting(){
        int current = spUtils.getValue("screen", 0);
        if (current == GSYVideoType.SCREEN_MATCH_FULL)
            current = 3;

        new CustomDialog.Builder(mContext, getTheme())
                .setTitle("画面设置")
                .setMessage("设置播放器的画面比例")
                .setList(Arrays.asList(TvPlayer.screenArray), null, current)
                .setOnCancelListner(dialog -> {
                    if (isIfCurrentIsFullscreen())
                        settingDialog.show();
                })
                .setOnItemClickListener((item, o, which, dialog) -> {
                    if (which == 0){
                        MyPlayerManager.changeScreen(mContext, GSYVideoType.SCREEN_TYPE_DEFAULT);
                    }else if (which == 1){
                        MyPlayerManager.changeScreen(mContext, GSYVideoType.SCREEN_TYPE_16_9);
                    }else if (which == 2){
                        MyPlayerManager.changeScreen(mContext, GSYVideoType.SCREEN_TYPE_4_3);
                    }else if (which == 3){
                        MyPlayerManager.changeScreen(mContext, GSYVideoType.SCREEN_MATCH_FULL);
                    }else if (which == 4){
                        MyPlayerManager.changeScreen(mContext, GSYVideoType.SCREEN_TYPE_FULL);
                    }
                    resume(getCurrentPositionWhenPlaying());
                    if (!DeviceManager.isTv() && !isIfCurrentIsFullscreen())
                        dialog.dismiss();
                })
                .create()
                .show();

    }
    private static final String[] danmuArray = new String[]{"全屏","半屏","顶部","条状","屏蔽"};
    public void showDamakuSetting(){
        if (danmuDialog == null) {
            danmuDialog = new CustomDialog.Builder(mContext, getTheme())
                    .setTitle("弹幕设置")
                    .setMessage("设置弹幕覆盖的范围")
                    .setList(Arrays.asList(TvPlayer.danmuArray), null, SPUtils.get(mContext).getValue("danmu", 4))
                    .setOnCancelListner(dialog -> {})
                    .setOnItemClickListener((item, o, position, dialog) -> {
                        spUtils.putValue("danmu", position);
                        loadDanmuSetting(position);
                        dialog.hide();
                    })
                    .create();
            danmuDialog.show();
        }
        else{
            danmuDialog.resume();
        }


    }
    private static final String[] speedArray = new String[]{"x 1.0", "x 1.25", "x 1.5", "x 1.75", "x 2.0"};
    public void showSpeedSetting(){
        float currentSpeed =  getSpeed();
        int current = 0;
        if (currentSpeed == 1.25)
            current = 1;
        else if (currentSpeed == 1.5)
            current = 2;
        else if (currentSpeed == 1.75)
            current = 3;
        else if (currentSpeed == 2.0)
            current = 4;
        new CustomDialog.Builder(mContext, getTheme())
                .setTitle("倍速设置")
                .setMessage("设置播放器倍速")
                .setList(Arrays.asList(TvPlayer.speedArray), null, current)
                .setOnCancelListner(dialog -> {
                    if (isIfCurrentIsFullscreen())
                        settingDialog.show();
                })
                .setOnItemClickListener((item, o, position, dialog) -> {
                    double s = 1 + position * 0.25;


                    setSpeed((float) s);
                    if (mDanmakuContext!=null)
                    mDanmakuContext.setScrollSpeedFactor((float)( mDanmaKuSpeed/ s));
                    if (!DeviceManager.isTv() && !isIfCurrentIsFullscreen())
                        dialog.dismiss();
                })
                .create()
                .show();
    }
    public static String[] mediasArray = new String[]{"软解", "硬解"};
    public void showVideoSetting(){
        if (mediaDialog != null && mediaDialog.isShowing())
            return;
        if (mediaDialog == null){
            AtomicBoolean hide = new AtomicBoolean(false);
            mediaDialog = new CustomDialog.Builder(mContext, getTheme())
                    .setTitle("内核设置")
                    .setMessage("选择内核和解码器\n硬解：更省电\n软解：更稳定")
                    .setList(Arrays.asList(TvPlayer.mediasArray), null, MyPlayerManager.getCurrentKernel(mContext))
                    .setOnCancelListner(dialog -> {
                        if (!hide.get())
                            showSetting();
                    })
                    .setOnItemClickListener((item, o, i, dialog) -> {
                        onVideoPause();
                        MyPlayerManager.changeMode(mContext, i);
                        spUtils.putValue("media", i);
                        resume(getCurrentPositionWhenPlaying());
                        dialog.dismiss();
                        hide.set(true);
                    })
                    .create();
            mediaDialog.show();
        }
        else {
            mediaDialog.resume();
        }
    }

    public RelativeLayout getSurface() {
        return surface;
    }
    private EditText editText;
    public void initDanmuSetting(final Context context){
        mDamakuBar.setVisibility(VISIBLE);
        ImageButton danmuSetting = findViewById(R.id.setting1);
        danmuSetting.setOnClickListener(v -> showDamakuSetting());
        editText = findViewById(R.id.danmu_editer);


        editText.setOnFocusChangeListener((v, hasFocus) -> {

            if (hasFocus){
                getStartButton().callOnClick();
                cancelDismissControlViewTimer();
            }
        });
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_DOWN == event.getAction())){
                if (!editText.getText().toString().isEmpty()) {
                    addDanmaku(editText.getText().toString());
                    editText.setText("");
                    InputMethodManager manager = (InputMethodManager) context.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (manager != null)
                        manager.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    getStartButton().callOnClick();
                    startDismissControlViewTimer();
                    return true;
                }
            }
            return false;
        });
        Button send = findViewById(R.id.send);
        send.setOnClickListener(v -> {
            if (editText.getText().toString().isEmpty())
                return;
            addDanmaku(editText.getText().toString());
            editText.setText("");
            InputMethodManager manager = (InputMethodManager) context.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null)
                manager.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            getStartButton().callOnClick();
            startDismissControlViewTimer();
        });
    }
    private void setDialogProgress(int progress){
        if (mDialogSeekTime != null)
            mDialogSeekTime.setText(translateLong((long)getDuration() * progress / 100));
        if (mDialogProgressBar != null)
            mDialogProgressBar.setProgress(progress);

    }

    public OnBatteryPowerListener getOnBatteryPowerListener() {
        return onBatteryPowerListener;
    }

    public void setOnBatteryPowerListener(OnBatteryPowerListener onBatteryPowerListener) {
        this.onBatteryPowerListener = onBatteryPowerListener;
    }

    @SuppressLint("StaticFieldLeak")
    public  class SeekTask extends AsyncTask{
        private boolean started = false;
        private int progress = 0;
        private int remainTime = 2;
        private final Handler handler = new Handler();
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            started = true;
            showProgressDialog((float) 1, translateLong(getGSYVideoManager().getCurrentPosition()), getGSYVideoManager().getBufferedPercentage(), translateLong((long)getDuration()), 100);
            progress = (int) ((float)getCurrentPositionWhenPlaying()/(float) getDuration()*100);
            LogUtil.d("tag", "ss" + progress);
            mDialogProgressBar.setProgress(progress);
        }

        public boolean isStarted() {
            return started;
        }

        public void activateDelay() {
            this.remainTime = 1;
        }
        public void add(int a){
            if (a > 0)
                add();
            else
                sub();
        }
        public void add(){
            progress++;
            if (progress > 100)
                progress = 100;
            activateDelay();
            handler.post(() -> setDialogProgress(progress));
        }
        public void sub(){
            progress--;
            if (progress < 0)
                progress = 0;
            activateDelay();
            handler.post(() -> setDialogProgress(progress));
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Log.d("tag", remainTime + "s");
            while (remainTime >= 0){
                try {
                    Thread.sleep(500);
                    remainTime--;
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            dismissProgressDialog();
            if (progress == 0)
                seekTo(1000);
            else if (progress == 100)
                seekTo(getDuration() - 1000);
            else
                seekTo((long) getDuration() * progress /100);
            cancel(true);
        }
    }
    private SeekTask seekTask;
    public void setSeekAdd(int add){
        if (seekTask.isCancelled()){
            seekTask = new SeekTask();
            seekTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else if (!seekTask.isStarted()){
            seekTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        seekTask.add(add);
    }



    public static String translateLong(Long time) {
        try{
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            format.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
            String dateTime = format.format(time);
            Date date = format.parse(dateTime);

            Calendar calendar = Calendar.getInstance();
            assert date != null;
            calendar.setTime(date);

            Calendar orgin = Calendar.getInstance();
            orgin.setTimeInMillis(0);

            int hour = calendar.get(Calendar.HOUR_OF_DAY) - orgin.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE) - orgin.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND) - orgin.get(Calendar.SECOND);

            StringBuilder builder = new StringBuilder();
            if(hour!=0){
                if (hour < 10)
                    builder.append(0 + "");
                builder.append(hour).append(":");
            }
            if(minute!=0){
                if (minute < 10)
                    builder.append(0 + "");
                builder.append(minute).append(":");
            }else {
                builder.append("00:");
            }
            if(second!=0){
                if (second < 10)
                    builder.append(0 + "");
                builder.append(second);
            }else {
                builder.append("00");
            }

            return builder.toString();
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }


    private boolean isCached(String url){
        boolean cached = ProxyCacheManager.instance().isCached(url, mContext);
        if (cached)
            onBufferingUpdate(100);
        return cached || (cacheWithPlay && NetworkUtils.isWifiConnected(mContext));
    }

    @Override
    public boolean setUp(String url, boolean cacheWithPlay, String title) {
        return super.setUp(url, isCached(url), title);
    }

    public void resume(long currentTime, VideoTaskItem item){
        release();
        setUp(item.getFilePath(), cacheWithPlay, mTitle);
        setSeekOnStart(currentTime);
        startPlayLogic();
    }


    public void resume(long currentTime){
        release();
        setUp(mUrl, cacheWithPlay, mTitle);
        setSeekOnStart(currentTime);
        startPlayLogic();
    }
    public void backToNormalWindow(){
        if (mCurrentState != CURRENT_STATE_PAUSE && DeviceManager.isTv())
            getStartButton().callOnClick();
        getSurface().postDelayed(() -> getFullscreenButton().callOnClick(), DeviceManager.isTv()?500:0);
    }

    public void setQuality(String text) {
        int color = R.color.color_low;
        switch (text){
            case "4K 蓝光HDR":color = R.color.color_4k;break;
            case "1080P 蓝光":color = R.color.color_1080;break;
            case "720P 超清":color = R.color.color_720;break;
            default:break;
        }
        this.quality = text;
        qualityView.setText(quality);
        if (!quality.isEmpty()){
            qualityView.setVisibility(VISIBLE);
        }else
            qualityView.setVisibility(INVISIBLE);
        qualityView.getBackground().setColorFilter(mContext.getResources().getColor(color), PorterDuff.Mode.SRC);


    }

    @Override
    public void onAutoCompletion() {
        getActivity().updateTime(0);
        super.onAutoCompletion();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.download:showDownloadSetting(false);break;
            case R.id.change_api:changeApi(false);break;
            case R.id.share:shareFile(mUrl);break;


        }
        return false;
    }

    private void shareFile(String filePath) {
        if (filePath == null)
            return;
        Intent share_intent = new Intent();
        share_intent.setAction(Intent.ACTION_SEND);//设置分享行为
        if (filePath.startsWith("http")){
            if (sniffingVideo.getType().equals("flv")){
                    Toast.makeText(mContext, "FLV视频无法直接分享，请下载后分享文件", Toast.LENGTH_SHORT).show();
                    return;
                }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "来自影视凯TV分享 <<" + mTitle + ">>" +
                    getActivity().videoTool.getInfo().getVideoType() + "：" + getActivity().getUrl() + "\n" +
                    "播放链接：" + mUrl);
            share_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            share_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(Intent.createChooser(intent, mTitle));
        }else {
            File file = new File(filePath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".fileProvider", file);
                share_intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            } else {
                share_intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            }

            share_intent.setType("video/mp4");//设置分享内容的类型
            share_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            share_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(Intent.createChooser(share_intent, mTitle + "——视频文件分享"));
        }
    }



    private SPUtils spUtils = null;
    private RelativeLayout tvAlert;
    public void initSputils(Activity activity){
        spUtils = SPUtils.get(activity);
    }
    @Override
    protected void init(final Context context) {
        super.init(context);
        if (DeviceManager.isTv())
            mFullscreenButton.setVisibility(GONE);
        mTitleTextView.setMaxEms(ems_normal);
        initSputils(getActivity());
        seekTask = new SeekTask();
        View pip;
        View more;
        View next;
        View selection;
        clockView = findViewById(R.id.clock);
        batteryView = findViewById(R.id.batteryView);
        loadingView = findViewById(R.id.loadingView);
        tvAlert = findViewById(R.id.tv_alert);
        qualityView = findViewById(R.id.quality);
        qualityView.setText(quality);
        contentType = findViewById(R.id.contentType);
        address = findViewById(R.id.address);
        pip = findViewById(R.id.pip);
        more = findViewById(R.id.more);
        more.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(mContext, v);//第二个参数是绑定的那个view
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.more, popup.getMenu());
            popup.setOnMenuItemClickListener(this);
            popup.show();
        });
        selection = findViewById(R.id.selectionButton);
        selection.setOnClickListener(v -> getActivity().showSelectionDialog());
        pip.setOnClickListener(v -> startWindowPlayer());
        next = findViewById(R.id.next);
        next.setOnClickListener(v -> {
            if (getActivity().adapter.getCurrent() < getActivity().adapter.getAllCount() - 1)
                onAutoCompletion();
            else
                Toast.makeText(mContext, "没有下一集了", Toast.LENGTH_SHORT).show();
        });
        mDanmakuView = findViewById(R.id.danmaku_view);
        mDamakuBar = findViewById(R.id.danmu_bar);
        surface = findViewById(R.id.surface_container);
        setting = findViewById(R.id.setting);
        if (DeviceManager.isTv())
            setting.setVisibility(INVISIBLE);
        setting.setOnClickListener(v -> showSetting());
        initDanmaku();

    }

    public IDanmakuView getDanmakuView() {
        return mDanmakuView;
    }

    public DanmakuContext getDanmakuContext() {
        return mDanmakuContext;
    }
    public MyDanmakuParser createParser(File file) {
        if (file == null || !file.exists()) {
            LogUtil.i("tag", "弹幕信息缺失，生成空白弹幕控件");
            return new MyDanmakuParser() {

                @Override
                protected Danmakus parse() {
                    return new Danmakus();
                }
            };
        }
        ILoader loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_ACFUN);
        try {
            InputStream stream = new FileInputStream(file);
            loader.load(stream);
        } catch (IllegalDataException | IOException e) {
            e.printStackTrace();
        }
        MyDanmakuParser parser = new MyDanmakuParser();
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
        LogUtil.i("tag", "弹幕流加载完毕");
        return parser;

    }

    public void destroyWeidgets(){
        stopClock();
        if (mDanmakuView != null) {
            releaseDanmaku(this);
            mDanmakuView = null;
        }
        if (seekTask != null) {
            seekTask.cancel(true);
            seekTask = null;
        }
        if (changeDialog != null) {
            changeDialog.dismiss();
            changeDialog = null;
        }
        if (danmuDialog != null) {
            danmuDialog.dismiss();
            danmuDialog = null;
        }
        if (settingDialog != null) {
            settingDialog.dismiss();
            settingDialog = null;
        }
        mDanmakuContext = null;
        if (editText != null)
            editText.setOnFocusChangeListener(null);
    }
    /**
     释放弹幕控件
     */
    private void releaseDanmaku(TvPlayer danmakuVideoPlayer) {
        if (danmakuVideoPlayer != null && danmakuVideoPlayer.getDanmakuView() != null && danmakuVideoPlayer.getDanmakuView().isShown()) {
            Debuger.printfError("release Danmaku!");
            danmakuVideoPlayer.getDanmakuView().release();
        }
    }

    /**
     模拟添加弹幕数据
     */
    private void addDanmaku(String text) {
        BaseDanmaku danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null || mDanmakuView == null) {
            return;
        }
        danmaku.text = text;
        danmaku.padding = 5;
        danmaku.priority = 8;  // 可能会被各种过滤器过滤并隐藏显示，所以提高等级
        danmaku.isLive = true;
        danmaku.setTime(mDanmakuView.getCurrentTime() + 500);
        danmaku.textSize = 14f;
        danmaku.textColor = Color.RED;
        //danmaku.textShadowColor = Color.GRAY;
        danmaku.borderColor = Color.WHITE;
        mDanmakuView.addDanmaku(danmaku);

    }

    private void resolveDanmakuShow() {
        post(() -> {
            if (getDanmakuView() == null){
                return;
            }
            if (mDanmaKuShow) {
                if (!getDanmakuView().isShown())
                    getDanmakuView().show();
                //mToogleDanmaku.setText("弹幕关");
            } else {
                if (getDanmakuView().isShown()) {
                    getDanmakuView().hide();
                }
                //mToogleDanmaku.setText("弹幕开");
            }
        });
    }
    public TvPlayer getcurrentPlayer(){
        try {
            return (TvPlayer) getCurrentPlayer();
        }catch (Exception e){
            return getActivity().player;
        }

    }

    public void loadDanmu() {
        try {
            this.mParser = null;
            this.danmuFile = null;
            if (spUtils.getValue("danmu", 4) == 4)
                return;
            DanmuFile danmuFile = new DanmuFile(getActivity().getUrl());
            if (danmuFile.existsCache()) {
                getcurrentPlayer().danmuFile = danmuFile.getCacheFile();
                getcurrentPlayer().setDanmakuStartSeekPosition(getCurrentPositionWhenPlaying());
                onPrepareDanmaku(getcurrentPlayer());
                Toast.makeText(mContext, "从缓存中读取弹幕", Toast.LENGTH_SHORT).show();
            }else {
                danmuFile.cache(new DanmuFile.OnCacheListner() {
                    @Override
                    public void onCached(File file) {
                        getcurrentPlayer().danmuFile = file;
                        getSurface().post(() -> {
                            getcurrentPlayer().setDanmakuStartSeekPosition(getCurrentPositionWhenPlaying());
                            onPrepareDanmaku(getcurrentPlayer());
                            Toast.makeText(mContext, "弹幕加载成功", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onCacheFailed() {
                        getSurface().post(() -> Toast.makeText(mContext, "弹幕加载失败", Toast.LENGTH_SHORT).show());
                    }
                });
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }


    @Override
    public boolean setUp(String url, boolean cacheWithPlay,File file,  Map<String, String> headers, String title) {
        prepared = false;
        this.url = url;
        this.title = title;
        this.cacheWithPlay = cacheWithPlay;
        if (isRelease())
            return false;
        setRelease(false);
        return super.setUp(url, isCached(url), null, headers, title);
    }


    @Override
    public void onPrepared() {
        prepared = true;
        loadDanmu();
        long time = mSeekOnStart;
        super.onPrepared();
    }

    @Override
    public void onVideoPause() {
        super.onVideoPause();
        if (DeviceManager.isTv() && isIfCurrentIsFullscreen())
            tvAlert.setVisibility(VISIBLE);
        //ProxyCacheManager.instance().pause(mUrl);
        danmakuOnPause();
        try {
            getActivity().updateTime(getCurrentPositionWhenPlaying());
        }catch (Exception ignored){

        }

    }

    @Override
    public void onVideoResume() {
        super.onVideoResume();
        danmakuOnResume();

    }


    @Override
    public void onBufferingUpdate(int percent) {
        super.onBufferingUpdate(percent);
    }

    @Override
    protected void clickStartIcon() {
        super.clickStartIcon();
        if (mCurrentState == CURRENT_STATE_PLAYING) {
            onVideoResume();
        } else if (mCurrentState == CURRENT_STATE_PAUSE) {
            danmakuOnPause();
        }
    }

    @Override
    public void onCompletion() {
        releaseDanmaku(this);
    }




    @Override
    public void onSeekComplete() {
        super.onSeekComplete();
        //如果已经初始化过的，直接seek到对于位置
        int progress = mBottomProgressBar.getProgress();
        long currentTime = ((long) progress * getDuration() / 100);
        if (currentTime > 0)
            getActivity().updateTime(currentTime);
        syncDanmu(progress);
    }
    protected void danmakuOnPause() {
        try {
            getActivity().updateTime(getCurrentPositionWhenPlaying());
            if (mDanmakuView != null && mDanmakuView.isPrepared()) {
                mDanmakuView.pause();
            }
        }catch (Exception ignored){

        }

    }

    protected void danmakuOnResume() {
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }

    public void hideAllDialog(){
        if (mediaDialog != null)
            mediaDialog.hide();
        if (danmuDialog != null)
            danmuDialog.hide();
        if (settingDialog != null)
            settingDialog.hide();
        if (changeDialog != null)
            changeDialog.hide();
    }
    /**
     开始播放弹幕
     */
    private void onPrepareDanmaku(TvPlayer gsyVideoPlayer) {
        if (gsyVideoPlayer.getDanmakuView() != null && !gsyVideoPlayer.getDanmakuView().isPrepared() && gsyVideoPlayer.getParser() != null) {
            gsyVideoPlayer.getDanmakuView().prepare(gsyVideoPlayer.getParser(),
                    gsyVideoPlayer.getDanmakuContext());
        }
    }

    /**
     弹幕偏移
     */
    private void resolveDanmakuSeek(TvPlayer gsyVideoPlayer, long time) {
        if (mHadPlay && gsyVideoPlayer.getDanmakuView() != null && gsyVideoPlayer.getDanmakuView().isPrepared()) {
            gsyVideoPlayer.getDanmakuView().seekTo(time);
        }
    }


    public BaseDanmakuParser getParser() {
        if (mParser == null) {
            if (danmuFile != null) {
                mParser = createParser(danmuFile);
            }
        }
        return mParser;
    }


    public long getDanmakuStartSeekPosition() {
        return mDanmakuStartSeekPosition;
    }

    public void setDanmakuStartSeekPosition(long danmakuStartSeekPosition) {
        this.mDanmakuStartSeekPosition = danmakuStartSeekPosition;
    }

    public void setDanmaKuShow(boolean danmaKuShow) {
        mDanmaKuShow = danmaKuShow;
    }

    public boolean getDanmaKuShow() {
        return mDanmaKuShow;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);
        if (!isIfCurrentIsFullscreen()){
            //在非全屏状态时，每百分之一都要同步一下
            if (!DeviceManager.isTv())
                getActivity().updateTime((long) ((long) getDuration() * progress / 100.0));
        }else {
            //如果是用户操作，检查一下弹幕有没有同步上视频
            if (fromUser) {
                syncDanmu(progress);
            }
            //全屏状态不需要定时同步，退出全屏时同步一次即可
            else if (progress % 2 == 0){
                //每百分之二纠正一下弹幕进度
                syncDanmu(progress);
            }
        }
    }
    private void syncDanmu(int progress){
        int duration = getDuration();
        new Thread(() -> {

            if (duration == 0 || mDanmakuView == null || (mDanmakuView != null && !mDanmakuView.isPrepared())) {
                setDanmakuStartSeekPosition((long) ((long) progress * duration / 100.0));
                return;
            }
            int danmuProgress = (int)Math.round((double)getDanmakuView().getCurrentTime()/duration *100);
            if (progress != danmuProgress) {
                bufferRunnable = () -> {
                    long currentTime = (long) ((long) progress * duration / 100.0);
                    if (mHadPlay && getDanmakuView() != null && getDanmakuView().isPrepared()) {
                        resolveDanmakuSeek(TvPlayer.this, currentTime);
                    } else if (mHadPlay && getDanmakuView() != null && !getDanmakuView().isPrepared()) {
                        //如果没有初始化过的，记录位置等待
                        setDanmakuStartSeekPosition(currentTime);
                    }
                };
                getSurface().post(bufferRunnable);

            }
        }).start();
    }
}