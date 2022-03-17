package com.kai.video.activity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.OrientationEventListener;

import androidx.appcompat.app.AppCompatActivity;

import com.kai.video.R;
import com.kai.video.manager.DeviceManager;
import com.kai.video.manager.MyPlayerManager;
import com.kai.video.view.dialog.CustomDialog;
import com.kai.video.view.player.QuickPlayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PlayAcivity extends AppCompatActivity {
    private QuickPlayer player;
    private MyOrientoinListener myOrientoinListener;
    //强制横屏
    private boolean forceLand = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        MyPlayerManager.loadDefault(this);
        player = findViewById(R.id.player);
        player.setDismissControlTime(4000);
        player.setIsTouchWiget(true);
        player.setRotateViewAuto(true);
        player.setAutoFullWithSize(true);
        player.setLockLand(true);
        player.setShowFullAnimation(false);
        player.setNeedLockFull(true);
        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        Bundle extra = getIntent().getBundleExtra("extra");
        String contentType = getIntent().getStringExtra("contentType");
        Map<String, String> headers = new HashMap<>(extra.size());
        for (String key: extra.keySet()) {
            headers.put(key, extra.getString(key));
        }
        player.setUp(url, headers, contentType, title);
        player.getStartButton().requestFocus();
        player.getBackButton().setOnClickListener(v -> finish());
        myOrientoinListener = new MyOrientoinListener(this);
        myOrientoinListener.enable();
        player.getFullscreenButton().setImageResource(R.drawable.rotate);
        player.getFullscreenButton().setOnClickListener(view -> new CustomDialog.Builder(PlayAcivity.this)
                .setTitle("选择屏幕方向")
                .setList(Arrays.asList("横屏", "竖屏"), null, forceLand?0:1)
                .setOnItemClickListener((item, o, position, dialog) -> {
                    dialog.dismiss();
                    if (position == 0){
                        forceLand = true;
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }else if (position == 1){
                        forceLand = false;
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

                    }
                }).create().show());
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.onVideoPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        player.release();
        myOrientoinListener.disable();
        super.onDestroy();
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
            player.getStartButton().callOnClick();
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
            player.setSeekAdd(1);
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
            player.setSeekAdd(-1);
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_MENU){
            player.showSetting();
            return true;
        }else if (keyCode == KeyEvent.KEYCODE_BACK){
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);


    }

    class MyOrientoinListener extends OrientationEventListener {

        public MyOrientoinListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            int screenOrientation = getResources().getConfiguration().orientation;
            if (((orientation >= 0) && (orientation < 45)) || (orientation > 315)) {//设置竖屏
                if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && orientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                    if (!forceLand)
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                }
            } else if (orientation > 225 && orientation < 315) { //设置横屏
                if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    if (forceLand)
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            } else if (orientation > 45 && orientation < 135) {// 设置反向横屏
                if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    if (forceLand)
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
            } else if (orientation > 135 && orientation < 225) {
                if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                    if (!forceLand)
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                }
            }
        }
    }


}