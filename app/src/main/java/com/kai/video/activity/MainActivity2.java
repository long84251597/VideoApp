package com.kai.video.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kai.sniffwebkit.sniff.MySniffingFilter;
import com.kai.sniffwebkit.sniff.SniffTool;
import com.kai.sniffwebkit.sniff.SniffingVideo;
import com.kai.video.R;
import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;

import java.util.HashMap;
import java.util.Map;

public class MainActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Map<String ,Object> map = new HashMap<>();
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
        QbSdk.initTbsSettings(map);
        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {

            @Override
            public void onViewInitFinished(boolean b) {
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                if (!b)
                    Toast.makeText(getApplicationContext(), "X5内核初始化失败，使用系统web内核", Toast.LENGTH_SHORT).show();
                SniffTool.getInstance(MainActivity2.this)
                        //.bindLoadingView(loadingView)
                        .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1")
                        .setCallback(new SniffTool.Callback() {
                            @Override
                            public void onSuccess(SniffingVideo video) {
                                Toast.makeText(MainActivity2.this, video.getUrl(), Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onFailed(int errorCode) {
                                Toast.makeText(MainActivity2.this, "error:" + errorCode , Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onProgress(int progress) {

                            }

                        })
                        .setSniffTimeout(40 * 1000)
                        .setJsTimeout(30 * 1000)
                        .setFilter(new MySniffingFilter())
                        .target("https://video.lanyingtv.com/?url=https://v.qq.com/x/cover/mzc0020036ro0ux/e0040wour6t.html")
                        .start();
            }

            @Override
            public void onCoreInitFinished() {
                // TODO Auto-generated method stub
            }
        };
        QbSdk.initX5Environment(getApplicationContext(), cb);

        //loadingView.show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}