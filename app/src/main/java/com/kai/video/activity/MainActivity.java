package com.kai.video.activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.IPTool;
import com.google.android.material.snackbar.Snackbar;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.kai.video.BuildConfig;
import com.kai.video.bean.obj.Info;
import com.kai.video.manager.MyPlayerManager;
import com.kai.video.R;
import com.kai.video.bean.danmu.DanmuFile;
import com.kai.video.manager.DeviceManager;
import com.kai.video.bean.obj.History;
import com.kai.video.tool.net.LoginTool;
import com.kai.video.manager.ActivityCollector;
import com.kai.video.tool.file.FileUtils;
import com.kai.video.tool.log.LogUtil;
import com.kai.video.tool.application.SPUtils;
import com.kai.video.tool.net.VideoTool;
import com.kai.video.view.other.CustomTimeView;
import com.kai.video.fragment.PlaceholderFragment;
import com.kai.video.view.dialog.CustomDialog;
import com.kai.video.view.other.TabViews;
import com.kai.video.view.other.TvTabLayout;
import com.king.app.dialog.AppDialogConfig;
import com.king.app.updater.AppUpdater;
import com.king.app.updater.callback.AppUpdateCallback;
import com.tencent.smtt.sdk.QbSdk;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends BaseActivity implements View.OnFocusChangeListener{
    private int side = 0;
    private TabViews tabViews;
    ViewPager viewPager;
    private LocalReceiver localReceiver;
    private IntentFilter intentFilter;
    private LocalBroadcastManager localBroadcastManager;
    private VideoTool.SectionsPagerAdapter sectionsPagerAdapter;
    @Override
    protected void onDestroy() {
        localBroadcastManager.unregisterReceiver(localReceiver);
        localBroadcastManager = null;
        if (loginDialog != null) {
            loginDialog.dismiss();
            loginDialog = null;
        }
        tabViews = null;
        localReceiver = null;
        intentFilter = null;
        sectionsPagerAdapter.destroy();
        sectionsPagerAdapter = null;
        if (DeviceManager.isTv())
            ((CustomTimeView)(findViewById(R.id.clock))).stop();
        super.onDestroy();
    }

    private void checkUpdate(){
        new Thread(() -> {
            try {
                Connection.Response response = Jsoup.connect(IPTool.getLocal() + "/version")
                        .data("versionCode", String.valueOf(BuildConfig.VERSION_CODE))
                        .data("versionName", BuildConfig.VERSION_NAME)
                        .data("action", "check")
                        .data("tv", DeviceManager.isTv() + "")
                        .method(Connection.Method.GET)
                        .execute();
                JSONObject object = JSONObject.parseObject(response.body());
                Log.e("ob", object.toJSONString());
                boolean upToDate = object.getBoolean("upToDate");
                //发现更新
                if (!upToDate){
                    Bundle data = new Bundle();
                    data.putInt("versionCode", object.getIntValue("versionCode"));
                    data.putString("versionName", object.getString("versionName"));
                    data.putString("versionLog", object.getString("versionLog"));
                    data.putString("downloadUrl", object.getString("downloadUrl"));
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                            .setTitle("检测到版本更新：" + data.getString("versionName"))
                            .setMessage(data.getString("versionLog"));
                    builder.setCancelable(true);
                    builder.setPositiveButton("升级", (dialog, which) -> {
                        AppUpdater appUpdater = new AppUpdater.Builder()
                                .setUrl(data.getString("downloadUrl"))
                                .setInstallApk(true)
                                .setDeleteCancelFile(true)
                                .setShowNotification(true)
                                .setChannelId("com.kai.update")
                                .setChannelName("影视凯更新")
                                .setVersionCode(data.getInt("versionCode"))
                                .build(MainActivity.this);

                        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setMessage("正在下载安装程序，你可以将程序放置后台，但请不要关闭后台！");
                        //设置弹窗标题
                        progressDialog.setTitle("更新中");
                        //设置弹窗图标
                        progressDialog.setIcon(R.mipmap.ic_launcher_foreground);
                        // 能够返回
                        progressDialog.setCancelable(false);
                        // 点击外部返回
                        progressDialog.setCanceledOnTouchOutside(false);
                        //设置进度条
                        progressDialog.setProgress(0);
                        //设置进度条是否明确
                        progressDialog.setIndeterminate(false);
                        //设置进度条样式
                        //ProgressDialog.STYLE_SPINNER 环形精度条
                        //ProgressDialog.STYLE_HORIZONTAL 水平样式的进度条
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialog.show();
                        appUpdater.setUpdateCallback(new AppUpdateCallback() {
                            @Override
                            public void onProgress(long progress, long total, boolean isChange) {
                                int p = (int) ((progress*100)/total);
                                progressDialog.setProgress(p);

                            }

                            @Override
                            public void onFinish(File file) {
                                progressDialog.dismiss();
                            }
                        });
                        appUpdater.start();
                    });
                    //设置反面按钮
                    builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

                         //创建AlertDialog对象
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
                        }
                    });

                    /*
                    config.setTitle("检测到版本更新：" + data.getString("versionName"))
                            .setConfirm("升级") //旧版本使用setOk
                            .setContent(data.getString("versionLog"))
                            .setOnClickConfirm(v -> {
                                AppDialog.INSTANCE.dismissDialog();
                                AppUpdater appUpdater = new AppUpdater.Builder()
                                        .setUrl(data.getString("downloadUrl"))
                                        .setInstallApk(true)
                                        .setDeleteCancelFile(true)
                                        .setShowNotification(true)
                                        .setChannelId("com.kai.update")
                                        .setChannelName("影视凯更新")
                                        .setVersionCode(data.getInt("versionCode"))
                                        .build(MainActivity.this);
                                appUpdater.start();
                            });
                    runOnUiThread(() -> AppDialog.INSTANCE.showDialog(MainActivity.this, config));
                     */
                    AppDialogConfig config = new AppDialogConfig(MainActivity.this);
                    //旧版本使用setOnClickOk


                    //handler.sendMessage(message);
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }).start();

    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!isTaskRoot())
        {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent
                    .ACTION_MAIN))
            {
                finish();
                return;
            }
        }
        super.onCreate(savedInstanceState);
        //startActivity(new Intent(this, AvActivity.class));
        if (DeviceManager.getDevice() == DeviceManager.DEVICE_TV){
            requestWindowFeature(Window.FEATURE_NO_TITLE);

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_main);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            LogUtil.d("TAG", "Running on a TV Device");
        }else if (DeviceManager.getDevice() == DeviceManager.DEVICE_PAD){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                MainActivity.this.getWindow().setStatusBarColor(MainActivity.this.getColor(R.color.colorPrimary));
            }
            setContentView(R.layout.activity_main);

        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                MainActivity.this.getWindow().setStatusBarColor(MainActivity.this.getColor(R.color.colorPrimary));
            }
            setContentView(R.layout.activity_main_none_phone);
        }

        MyPlayerManager.setKernelDefault(DeviceManager.isTv());
        String[] permissions = new String[]{
                Permission.SYSTEM_ALERT_WINDOW,
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.NOTIFICATION_SERVICE,
                Permission.READ_EXTERNAL_STORAGE
        };
        if (DeviceManager.isTv())
            permissions = new String[]{
                    Permission.WRITE_EXTERNAL_STORAGE
            };
        boolean hasPermission = XXPermissions.isGranted(this, permissions);
        if (!hasPermission) {
            final String[] ps = permissions;
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("权限请求")
                    .setMessage("为了APP的更好体验，请授予必要权限")
                    .setNegativeButton("以后再说", (dialog12, which) -> dialog12.cancel())
                    .setPositiveButton("立即授予", (dialog1, which) -> {
                        XXPermissions.setScopedStorage(true);
                        XXPermissions.with(MainActivity.this)
                                .permission(ps)
                                .request(new OnPermissionCallback() {

                                    @Override
                                    public void onGranted(List<String> permissions1, boolean all) {
                                        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {

                                            @Override
                                            public void onViewInitFinished(boolean b) {
                                                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                                                if (!b)
                                                    Toast.makeText(getApplicationContext(), "使用webview内核", Toast.LENGTH_SHORT).show();
                                                else
                                                    Toast.makeText(getApplicationContext(), "使用x5内核", Toast.LENGTH_SHORT).show();
                                            }

                                            @Override
                                            public void onCoreInitFinished() {
                                                // TODO Auto-generated method stub
                                            }
                                        };
                                        QbSdk.initX5Environment(getApplicationContext(), cb);
                                    }

                                    @Override
                                    public void onDenied(List<String> permissions1, boolean never) {
                                        if (never) {
                                            //toast("被永久拒绝授权，请手动授予录音和日历权限");
                                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                            XXPermissions.startPermissionActivity(MainActivity.this, permissions1);
                                        }  //toast("获取录音和日历权限失败");

                                    }
                                });
                    }).create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
        }else {
            QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {

                @Override
                public void onViewInitFinished(boolean b) {
                    //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                    if (!b)
                        Toast.makeText(getApplicationContext(), "使用webview内核", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getApplicationContext(), "使用x5内核", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onCoreInitFinished() {
                    // TODO Auto-generated method stub
                }
            };
            QbSdk.initX5Environment(getApplicationContext(), cb);
        }
        ProgressDialog dialog = ProgressDialog.show(this, "正在连接服务器", "请等待");
        //setContentView(R.layout.activity_main);
        IPTool.load(new IPTool.Runner() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        checkUpdate();
                        initViews();
                        intentFilter = new IntentFilter();
                        intentFilter.addAction("com.android.application.LOCAL_BROADCAST");
                        localReceiver = new LocalReceiver();
                        localBroadcastManager = LocalBroadcastManager.getInstance(MainActivity.this);
                        localBroadcastManager.registerReceiver(localReceiver, intentFilter);
                        if (!DeviceManager.isTv())
                            findViewById(R.id.hot).setOnClickListener(v -> {
                                Intent intent = new Intent(MainActivity.this, TypeActivity.class);
                                startActivity(intent);
                            });
                    }
                });
            }
        });




    }
    private void initViews() {
        View searchView = findViewById(R.id.search_button);
        searchView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            intent.putExtra("wd", "");
            startActivity(intent);
        });
        sectionsPagerAdapter = new VideoTool.SectionsPagerAdapter(this, getSupportFragmentManager());
        viewPager = findViewById(R.id.view_pager);
        viewPager.setOnFocusChangeListener((v, hasFocus) -> Toast.makeText(MainActivity.this, hasFocus + "", Toast.LENGTH_SHORT).show());
        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(sectionsPagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                //Toast.makeText(MainActivity.this, getTabView(position).getId() + "", Toast.LENGTH_SHORT).show();
                PlaceholderFragment fragment = sectionsPagerAdapter.getCurrentFragment();
                if (DeviceManager.isTv() && !tabViews.isFocused()) {
                    fragment.scrollItemToTop();

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        TvTabLayout tabs = findViewById(R.id.tabs);
        tabs.setOnFocusChangeListener(this);
        tabs.setupWithViewPager(viewPager);
        tabViews = new TabViews(tabs, () -> sectionsPagerAdapter.getCurrentFragment().scrollItemToTop());
        ImageButton userButton = findViewById(R.id.user);
        userButton.setOnClickListener(v -> showUserSetting());
    }
    public boolean isTabFocused(){
        return tabViews.isFocused();
    }
    public void clearTabFocus(){
        tabViews.clearFocus();
    }
    public int getCurrentPage(){
        return viewPager.getCurrentItem();
    }
    private AlertDialog loginDialog = null;
    public void showUserSetting(){

        String username = SPUtils.get(this).getValue("username", "");
        new CustomDialog.Builder(this)
                .setTitle("用户设置")
                .setMessage("登录/查看观影历史/查看视频下载管理")
                .setList(Arrays.asList(username.isEmpty()?"未登录":"当前用户："+ username, "用户观看历史", "视频下载管理", "软件缓存管理"), null, -1)
                .setOnItemClickListener((item, o, which, dialog) -> {
                    if (which == 0){
                        View view = View.inflate(MainActivity.this, R.layout.dialog_login, null);

                        view.findViewById(R.id.btn_login).setOnClickListener(v -> {
                            String username1 = ((EditText) view.findViewById(R.id.et_user)).getText().toString();
                            String password = ((EditText) view.findViewById(R.id.et_password)).getText().toString();
                            LoginTool.login(MainActivity.this, username1, password, new LoginTool.OnLogin() {
                                @Override
                                public void success() {
                                    if (loginDialog != null) {
                                        loginDialog.hide();
                                        SPUtils.get(MainActivity.this).putValue("username", username1);
                                        Snackbar.make(viewPager, "登录成功", Snackbar.LENGTH_LONG).setAction("Action", null).setBackgroundTint(getResources().getColor(R.color.colorPrimary)).show();
                                    }
                                }

                                @Override
                                public void fail() {
                                    Snackbar.make(viewPager, "登录失败", Snackbar.LENGTH_LONG).setAction("Action", null).setBackgroundTint(getResources().getColor(R.color.colorPrimary)).show();
                                }
                            });

                        });
                        view.findViewById(R.id.btn_clear).setOnClickListener(v -> {
                            ((EditText) view.findViewById(R.id.et_user)).setText("");
                            ((EditText) view.findViewById(R.id.et_password)).setText("");
                        });
                        ((EditText)view.findViewById(R.id.et_user)).addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {

                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                String text = s.toString();
                                if (text.endsWith("\n")){
                                    ((EditText)view.findViewById(R.id.et_user)).setText(text.replace("\n", "").replace("\r", ""));
                                    view.findViewById(R.id.et_password).requestFocus();
                                }
                            }
                        });

                        ((EditText)view.findViewById(R.id.et_password)).addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {

                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                String text = s.toString();
                                if (text.endsWith("\n")){
                                    InputMethodManager manager = ((InputMethodManager)MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE));

                                    ((EditText)view.findViewById(R.id.et_password)).setText(text.replace("\n", "").replace("\r", ""));
                                    view.findViewById(R.id.btn_login).requestFocus();
                                    if (manager != null)
                                        manager.hideSoftInputFromWindow(view.findViewById(R.id.et_password).getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                                }
                            }
                        });
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this).setView(view);
                        loginDialog = builder.create();
                        loginDialog.setCancelable(false);
                        loginDialog.show();
                    }else if (which == 1){
                        if (!username.isEmpty()){
                            showHistory();
                        }
                    }else if (which == 2){
                        showDownloaded();
                    }else if (which == 3){
                        showCacheManager();
                    }
                    dialog.dismiss();
                })
                .create().show();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus){
            //ShadowDrawable.setShadowDrawable(v, 20, Color.WHITE, 10, 0, 0);
            LogUtil.i("TAG","onFocusChange" + v.getId());
            if (v.getId() == R.id.view_pager){
                LogUtil.i("TAG","onFocusChange");
            }

            //设置焦点框的位置和动画
            //Tools.focusAnimator(v,onFousView);
        }
    }

    private void showCacheManager(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("缓存管理");
        builder.setMessage("当前缓存总大小：" + FileUtils.getVideoCacheSize(MainActivity.this) + "（含视频缓存、弹幕缓存）\n是否清空所有缓存？");
        builder.setCancelable(true);
        builder.setPositiveButton("确定", (dialog, which) -> {
            dialog.dismiss();
            FileUtils.clearVideoCache(MainActivity.this);
            DanmuFile.init(MainActivity.this);
        });
        //设置反面按钮
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();     //创建AlertDialog对象
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
    }
    private final long mLastKeyDownTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            if (!tabViews.isFocused() && DeviceManager.isTv()){
                if (sectionsPagerAdapter.getCurrentFragment().inBanner()){
                    sectionsPagerAdapter.getCurrentFragment().exitBanner();
                }else
                    tabViews.focusCurrent();
                return true;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("是否退出程序");
            builder.setCancelable(true);
            builder.setPositiveButton("确定", (dialog, which) -> {
                dialog.dismiss();
                ActivityCollector.finishAll();
                //System.exit(0);
            });
            //设置反面按钮
            builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();     //创建AlertDialog对象
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();


            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU){
            showUserSetting();

            return false;
        }

        return super.onKeyDown(keyCode, event);
    }
    private void showDownloaded(){
        Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
        startActivity(intent);
    }
    private void showHistory(){
        new Thread(() -> {
            try {
                JSONArray hitems = History.getAll(MainActivity.this).getJSONArray("history");
                String[] hnames = new String[hitems.size()];
                String[] hurls = new String[hitems.size()];
                for (int i = 0; i < hitems.size(); i++){
                    JSONObject object = hitems.getJSONObject(i);
                    hnames[i] = Info.getOutPutWithType(object.getIntValue("type"),object.getString("name"));
                    hurls[i] = object.getString("url");
                }
                final String[] hurls1 = hurls;
                viewPager.post(
                        () -> new CustomDialog.Builder(MainActivity.this)
                                .setTitle("观看历史")
                                .setList(Arrays.asList(hnames), null, -1)
                                .setOnCancelListner(dialog -> {
                                    if (DeviceManager.isTv())
                                        showUserSetting();
                                })
                                .setOnItemClickListener((item, o, position, dialog) -> {
                                    dialog.dismiss();
                                    Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                                    intent.putExtra("url", hurls1[position]);
                                    MainActivity.this.startActivity(intent);


                                })
                                .create()
                                .show()

                );

            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();


    }

    class LocalReceiver extends BroadcastReceiver{
        private final int SIDE_LEFT = -1;
        private final int SIDE_NONE = 0;
        private final int SIDE_RIGHT = 1;

        @Override
        public void onReceive(Context context, Intent intent) {
            int side = intent.getIntExtra("side", 0);
            if (side != 0 && MainActivity.this.side == side){
                MainActivity.this.side = side;
                int index = viewPager.getCurrentItem();
                index = index + side;
                if (index > 3) {
                    return;
                }
                if (index < 0) {
                    return;
                }
                viewPager.setCurrentItem(index);

            }

        }
    }
}