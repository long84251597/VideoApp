package com.kai.video.view.player;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.kai.video.view.player.FloatingVideo;

/**
 * 适配了悬浮窗的view
 * Created by guoshuyu on 2017/12/25.
 */

public class FloatPlayerView extends FrameLayout {

    FloatingVideo videoPlayer;


    public FloatPlayerView(Context context) {
        super(context);
        init();
    }

    public FloatingVideo getVideoPlayer() {
        return videoPlayer;
    }

    public FloatPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        videoPlayer = new FloatingVideo(getContext());
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        addView(videoPlayer, layoutParams);
        new Handler().postDelayed(() -> videoPlayer.getStartButton().callOnClick(),1000);
        //增加封面

        //是否可以滑动调整
        videoPlayer.setIsTouchWiget(false);

    }


    public void onPause() {
        videoPlayer.getCurrentPlayer().onVideoPause();
    }

    public void onResume() {
        videoPlayer.getCurrentPlayer().onVideoResume();
    }

}
