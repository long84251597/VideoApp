package com.kai.video.floatUtil;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;

/**
 * Created by yhao on 17-12-1.
 * 用于控制悬浮窗显示周期
 * 使用了三种方法针对返回桌面时隐藏悬浮按钮
 * 1.startCount计数，针对back到桌面可以及时隐藏
 * 2.监听home键，从而及时隐藏
 * 3.resumeCount计时，针对一些只执行onPause不执行onStop的奇葩情况
 */

class FloatLifecycle extends BroadcastReceiver implements Application.ActivityLifecycleCallbacks {

    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    private static final long delay = 300;
    private final Handler mHandler;
    private final Class[] activities;
    private final boolean showFlag;
    private int startCount;
    private int resumeCount;
    private boolean appBackground;
    private final LifecycleListener mLifecycleListener;


    FloatLifecycle(Context applicationContext, boolean showFlag, Class[] activities, LifecycleListener lifecycleListener) {
        this.showFlag = showFlag;
        this.activities = activities;
        mLifecycleListener = lifecycleListener;
        mHandler = new Handler();
        ((Application) applicationContext).registerActivityLifecycleCallbacks(this);
        applicationContext.registerReceiver(this, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }


    private boolean needShow(Activity activity) {
        if (activities == null) {
            return true;
        }
        for (Class a : activities) {
            if (a.isInstance(activity)) {
                return showFlag;
            }
        }
        return !showFlag;
    }


    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        resumeCount++;
        if (needShow(activity)) {
            mLifecycleListener.onShow();
        } else {
            //mLifecycleListener.onHide();
        }
        if (appBackground) {
            appBackground = false;
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        resumeCount--;
        mHandler.postDelayed(() -> {
            if (resumeCount == 0) {
                appBackground = true;
                //mLifecycleListener.onPostHide();
            }
        }, delay);

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        startCount++;
    }


    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        startCount--;
        if (startCount == 0) {
            //mLifecycleListener.onHide();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
            String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
            if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                //mLifecycleListener.onHide();
            }
        }
    }


    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {

    }


    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }


}
