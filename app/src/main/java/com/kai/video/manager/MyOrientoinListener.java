package com.kai.video.manager;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.OrientationEventListener;

public class MyOrientoinListener extends OrientationEventListener {
    private final Activity activity;
    private boolean forceLand = false;
    public static MyOrientoinListener getInstance(Activity activity){
        return new  MyOrientoinListener(activity);
    }
    private MyOrientoinListener(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    public void setForceLand(boolean forceLand) {
        this.forceLand = forceLand;
    }

    public void enterFullScreen(){
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public void quitFullScreen(){
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }


    @Override
    public void onOrientationChanged(int orientation) {
        int screenOrientation = activity.getResources().getConfiguration().orientation;
        if (((orientation >= 0) && (orientation < 45)) || (orientation > 315)) {//设置竖屏
            if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && orientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                if (!forceLand)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        } else if (orientation > 225 && orientation < 315) { //设置横屏
            if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                if (forceLand)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } else if (orientation > 45 && orientation < 135) {// 设置反向横屏
            if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                if (forceLand)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
        } else if (orientation > 135 && orientation < 225) {
            if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                if (!forceLand)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }
    }
}

