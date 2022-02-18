package com.kai.video.view.player;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.kai.video.activity.InfoActivity;
import com.kai.video.R;
import com.kai.video.bean.obj.FloatingSimul;
import com.kai.video.floatUtil.FloatWindow;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.utils.NetworkUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.util.Timer;

/**
 * 多窗体下的悬浮窗页面支持Video
 * Created by shuyu on 2017/12/25.
 */

public class FloatingVideo extends StandardGSYVideoPlayer {
    private ImageButton seekForth;
    private ImageButton seekBack;
    private ImageButton mFullscreen;
    protected Timer mDismissControlViewTimer;

    /**
     * 1.5.0开始加入，如果需要不同布局区分功能，需要重载
     */
    public FloatingVideo(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    public FloatingVideo(Context context) {
        super(context);
    }

    @Override
    protected void hideAllWidget() {
        super.hideAllWidget();
        mSmallClose.setVisibility(INVISIBLE);
        mFullscreen.setVisibility(INVISIBLE);
        seekBack.setVisibility(INVISIBLE);
        seekForth.setVisibility(INVISIBLE);
    }

    public FloatingVideo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void init(Context context) {
        if (getActivityContext() != null) {
            this.mContext = getActivityContext();
        } else {
            this.mContext = context;
        }
        setDismissControlTime(2500);
        initInflate(mContext);
        mSmallClose = findViewById(R.id.small_close);
        mSmallClose.setVisibility(INVISIBLE);
        mFullscreen = findViewById(R.id.full_screen);
        seekBack = findViewById(R.id.seek_back);
        seekBack.setVisibility(INVISIBLE);
        seekForth = findViewById(R.id.seek_forth);
        seekForth.setVisibility(INVISIBLE);
        seekBack.setOnClickListener(v -> seekTo(getCurrentPositionWhenPlaying() - 10*1000));
        seekForth.setOnClickListener(v -> seekTo(getCurrentPositionWhenPlaying() + 10*1000));
        mFullscreen.setVisibility(INVISIBLE);
        mSmallClose.setOnClickListener(v -> {
            FloatingSimul.setPlayerType(FloatingSimul.TYPE_BACKGROUND);
            getGSYVideoManager().pause();
            cloneParams(FloatingVideo.this, (TvPlayer)getGSYVideoManager().lastListener());
            FloatWindow.destroy();
        });

        mFullscreen.setOnClickListener(v -> {
            FloatingSimul.setPlayerType(FloatingSimul.TYPE_FORGROUND);
            exitPlayer(context);
        });
        mTextureViewContainer = findViewById(R.id.surface_container);
        mTextureViewContainer.setOnTouchListener((v, event) -> {
            mStartButton.setVisibility(VISIBLE);
            mFullscreen.setVisibility(VISIBLE);
            mSmallClose.setVisibility(VISIBLE);
            seekBack.setVisibility(VISIBLE);
            seekForth.setVisibility(VISIBLE);
            //startDismissControlViewTimer();
            return false;
        });
        mStartButton = findViewById(R.id.start);
        if (isInEditMode())
            return;
        mScreenWidth = getActivityContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getActivityContext().getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) getActivityContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mStartButton = findViewById(com.shuyu.gsyvideoplayer.R.id.start);
        mStartButton.setOnClickListener(v -> clickStartIcon());
    }
    public void exitPlayer(Context context){
        Intent i = new Intent(context, InfoActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_floating_video;
    }

    @Override
    public void release() {
        super.release();
    }

    @Override
    protected void startPrepare() {
        if (getGSYVideoManager().listener() != null) {
            getGSYVideoManager().listener().onCompletion();
        }
        getGSYVideoManager().setListener(this);
        getGSYVideoManager().setPlayTag(mPlayTag);
        getGSYVideoManager().setPlayPosition(mPlayPosition);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        //((Activity) getActivityContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mBackUpPlayingBufferState = -1;
        getGSYVideoManager().prepare(mUrl, mMapHeadData, mLooping, mSpeed, mCache, mCachePath, null);
        setStateAndUi(CURRENT_STATE_PREPAREING);
    }

    @Override
    public void onAutoCompletion() {
        setStateAndUi(CURRENT_STATE_AUTO_COMPLETE);

        mSaveChangeViewTIme = 0;

        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        if (!mIfCurrentIsFullscreen)
            getGSYVideoManager().setLastListener(null);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        //((Activity) getActivityContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        releaseNetWorkState();

        if (mVideoAllCallBack != null && isCurrentMediaListener()) {
            Debuger.printfLog("onAutoComplete");
            mVideoAllCallBack.onAutoComplete(mOriginUrl, mTitle, this);
        }
        //exitPlayer(mContext);
    }

    @Override
    public void onCompletion() {
        //make me normal first
        setStateAndUi(CURRENT_STATE_NORMAL);

        mSaveChangeViewTIme = 0;

        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        if (!mIfCurrentIsFullscreen) {
            getGSYVideoManager().setListener(null);
            getGSYVideoManager().setLastListener(null);
        }
        getGSYVideoManager().setCurrentVideoHeight(0);
        getGSYVideoManager().setCurrentVideoWidth(0);

        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        //((Activity) getActivityContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        releaseNetWorkState();

    }

    @Override
    protected void addTextureView() {
        super.addTextureView();
    }

    @Override
    protected void cancelProgressTimer() {
        super.cancelProgressTimer();
    }

    @Override
    protected void startProgressTimer() {
        super.startProgressTimer();
    }

    @Override
    protected Context getActivityContext() {
        return getContext();
    }


    @Override
    protected boolean isShowNetConfirm() {
        return false;
    }

    @Override
    protected void showWifiDialog() {
        if (!NetworkUtils.isAvailable(mContext)) {
            //Toast.makeText(mContext, getResources().getString(R.string.no_net), Toast.LENGTH_LONG).show();
            startPlayLogic();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivityContext());
        builder.setMessage(getResources().getString(com.shuyu.gsyvideoplayer.R.string.tips_not_wifi));
        builder.setPositiveButton(getResources().getString(com.shuyu.gsyvideoplayer.R.string.tips_not_wifi_confirm), (dialog, which) -> {
            dialog.dismiss();
            startPlayLogic();
        });
        builder.setNegativeButton(getResources().getString(com.shuyu.gsyvideoplayer.R.string.tips_not_wifi_cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog =  builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        alertDialog.show();
    }
}
