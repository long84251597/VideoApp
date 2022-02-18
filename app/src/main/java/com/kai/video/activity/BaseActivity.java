package com.kai.video.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.kai.video.R;
import com.kai.video.manager.ActivityCollector;
import com.kai.video.tool.application.GC;

public class BaseActivity extends AppCompatActivity {
    @Override
    public boolean moveTaskToBack(boolean nonRoot) {
        return super.moveTaskToBack(true);
    }
    public static boolean isNightMode(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
        GC.clean();
    }
    private void setBackgroundColor(int color) {
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
