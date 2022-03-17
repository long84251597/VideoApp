package com.kai.video.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.kai.video.R;
import com.kai.video.manager.ActivityCollector;
import com.kai.video.manager.DeviceManager;
import com.kai.video.tool.application.GC;

import java.util.Arrays;
import java.util.Comparator;

public class BaseActivity extends AppCompatActivity {
    @Override
    public boolean moveTaskToBack(boolean nonRoot) {
        return super.moveTaskToBack(true);
    }
    protected boolean isNightMode(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    protected int getTvLayout() {
        return 0;
    }


    protected int getPadLandLayout() {
        return 0;
    }


    protected int getPadPortLayout() {
        return 0;
    }

    protected int getPhoneLandLayout() {
        return 0;
    }

    protected int getPhonePortLayout() {
        return 0;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                Display.Mode[] modes = getWindowManager().getDefaultDisplay().getSupportedModes();
                Arrays.stream(modes).sorted(new Comparator<Display.Mode>() {
                    @Override
                    public int compare(Display.Mode mode, Display.Mode t1) {
                        return mode.getRefreshRate() > mode.getRefreshRate()?1:-1;
                    }
                });
                Log.e(modes[0].getModeId() + "refresh", modes[0].getRefreshRate() + "");
                Window window = getWindow();
                WindowManager.LayoutParams attribute =  window.getAttributes();
                attribute.preferredDisplayModeId = modes[0].getModeId();
                window.setAttributes(attribute);
            }catch (Exception e){
                e.printStackTrace();
            }

        }

        if (isNightMode(this)){
            setTheme(R.style.NightTheme);
        }else
            setTheme(R.style.DayTheme);

        Intent intent = getIntent();
        if (intent == null){
            super.onCreate(savedInstanceState);
            return;
        }
        if (intent.getFlags() == (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_CLEAR_TOP)){
            ActivityCollector.clearTop(this);
        }else {
            ActivityCollector.addActivity(this);
        }
        super.onCreate(savedInstanceState);
        DeviceManager.getDevice(this, new DeviceManager.OnViewAttachListener() {
            @Override
            public int onInitTV() {
                return getTvLayout();
            }

            @Override
            public int onInitLandPad() {
                return getPadLandLayout();
            }

            @Override
            public int onInitLandPhone() {
                return getPhoneLandLayout();
            }

            @Override
            public int onInitPortPad() {
                return getPadPortLayout();
            }

            @Override
            public int onInitPortPhone() {
                return getPhonePortLayout();
            }
        });

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
        GC.clean();
    }
    protected void setBackgroundColor(int color) {
        Resources res = getResources();
        @SuppressLint("UseCompatLoadingForDrawables") Drawable drawable = res.getDrawable(color);
        this.getWindow().setBackgroundDrawable(drawable);
    }
    @SuppressLint("ResourceAsColor")
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                // 关闭
                setBackgroundColor(R.color.dayBackground);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                // 开启
                Log.e("qian", "false");
                setBackgroundColor(R.color.nightBackground);
                break;
            default:
                break;
        }
    }
}
