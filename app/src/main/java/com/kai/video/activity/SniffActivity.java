package com.kai.video.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.github.jasonhancn.tvcursor.TvCursorActivity;
import com.just.agentweb.AbsAgentWebSettings;
import com.just.agentweb.AgentWeb;
import com.just.agentweb.AgentWebUtils;
import com.just.agentweb.IAgentWebSettings;
import com.kai.sniffwebkit.ad.AdBlocker;
import com.kai.sniffwebkit.sniff.MySniffingFilter;
import com.kai.sniffwebkit.sniff.SniffingVideo;
import com.kai.video.R;
import com.kai.video.adapter.AutoCompleteAdapter;
import com.kai.video.manager.DeviceManager;
import com.kai.video.tool.application.SPUtils;
import com.kai.video.tool.net.SearchKeyTool;
import com.kai.video.view.dialog.CustomDialog;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SniffActivity extends TvCursorActivity implements PopupMenu.OnMenuItemClickListener {
    private View exit;
    private int adCounts = 0;
    private MySniffingFilter mFilter;
    private final List<SniffingVideo> videos = new ArrayList<>();
    private View menu;
    private AutoCompleteTextView searchView;
    private AgentWeb mAgentWeb;
    private ImageButton sniff;
    private AutoCompleteAdapter hinderAdapter;
    private View home;
    private View refresh;
    private View back;
    private View forth;
    private TextView ads;
    private String url;
    private AlertDialog loginDialog = null;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DeviceManager.getDevice(this, new DeviceManager.OnViewAttachListener() {
            @Override
            public int onInitTV() {
                return R.layout.activity_sniff;
            }

            @Override
            public int onInitLandPad() {
                return R.layout.activity_sniff;
            }

            @Override
            public int onInitLandPhone() {
                return R.layout.activity_sniff;
            }

            @Override
            public int onInitPortPad() {
                return R.layout.activity_sniff;
            }

            @Override
            public int onInitPortPhone() {
                return R.layout.activity_sniff;
            }
        });

        menu = findViewById(R.id.menu);
        searchView = findViewById(R.id.address);
        sniff = findViewById(R.id.sniff);
        home = findViewById(R.id.home);
        mFilter = new MySniffingFilter();
        back = findViewById(R.id.back);
        forth = findViewById(R.id.forth);
        refresh = findViewById(R.id.refresh);
        ads = findViewById(R.id.ads);
        exit = findViewById(R.id.exit);
        LinearLayout mWebContainer = findViewById(R.id.web_container);
        initViews();

        String wd = getIntent().getStringExtra("wd");
        String href = "";
        if (wd != null) {
            if (wd.startsWith("http"))
                href = wd;
            else
                href = "https://www.cupfox.com/search?key=" + URLEncoder.encode(wd);
        }else
            href = "https://www.dianyinggou.com";
        //如果是TV的话，就打开虚拟鼠标
        mAgentWeb = AgentWeb.with(this)
                .setAgentWebParent(mWebContainer, new LinearLayout.LayoutParams(-1, -1))
                .useDefaultIndicator()
                .setWebViewClient(webViewClient)
                .setAgentWebWebSettings(new AbsAgentWebSettings() {
                    @Override
                    protected void bindAgentWebSupport(AgentWeb agentWeb) {

                    }
                    @Override
                    public IAgentWebSettings toSetting(WebView webView) {
                        IAgentWebSettings iAgentWebSettings = super.toSetting(webView);
                        initWebview(iAgentWebSettings);
                        return iAgentWebSettings;
                    }
                })
                .createAgentWeb()
                .ready()
                .go(href);
        if (DeviceManager.isTv()){
            setScrollTargetView(mAgentWeb.getWebCreator().getWebView());
            showCursor();
        }
        mAgentWeb.getWebCreator().getWebView().requestFocus();
    }

    private String transToTv(String url){
        Connection.Response response = null;
        try {
            response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36")
                    .method(Connection.Method.GET)
                    .execute();
            return  response.url().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;

    }
    private void startJudgePlay(String url){
        boolean play = false;
        if (DeviceManager.isPhone()){
            if (url.startsWith("https://m.v.qq.com/play.html") || url.startsWith("https://m.iqiyi.com/v_") || url.startsWith("https://m.mgtv.com/b/") || url.startsWith("https://m.bilibili.com/bangumi/play"))
                play = true;
            if (play)
                url = transToTv(url);
        }
        if (url.startsWith("https://v.qq.com/x/cover") || url.startsWith("https://www.iqiyi.com/v_") || url.startsWith("https://www.mgtv.com/b/") || url.startsWith("https://www.bilibili.com/bangumi/play/"))
            play = true;
        String finalUrl = url;
        if (play)
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(SniffActivity.this);
                builder.setTitle("检测到app支持网站");
                builder.setMessage("是否立即跳转至播放页观看？");
                builder.setCancelable(true);
                builder.setPositiveButton("确定", (dialog, which) -> {
                    Intent intent = new Intent(SniffActivity.this, InfoActivity.class);
                    intent.putExtra("url", finalUrl);
                    startActivity(intent);
                    dialog.cancel();
                });
                //设置反面按钮
                builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();     //创建AlertDialog对象
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
            }
        });
    }

    private final com.just.agentweb.WebViewClient webViewClient = new com.just.agentweb.WebViewClient(){
        @Override
        public void onPageStarted(WebView webView, String s, Bitmap bitmap) {
            super.onPageStarted(webView, s, bitmap);
            BitmapDrawable drawable= new BitmapDrawable(bitmap); //获取图片
            drawable.setBounds(0, 0, 50, 50);
            searchView.setCompoundDrawables(drawable, null, null, null);
            getIntent().putExtra("wd", s);
            videos.clear();
            url = s;
            ads.post(() -> {
                searchView.setText(s);
                sniff.setImageResource(R.drawable.video);
                ads.setText("0");
                adCounts = 0;
            });

        }

        @Override
        public void onPageFinished(WebView webView, String s) {
            super.onPageFinished(webView, s);
            url = s;
            ads.post(() -> searchView.setText(webView.getTitle()));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    startJudgePlay(s);
                }
            }).start();

        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String s) {

            if (s.startsWith("http"))
                webView.loadUrl(s);
            return false;
        }


        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @SuppressLint("SetTextI18n")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
            try {
                String url = webResourceRequest.getUrl().toString();
                if (AdBlocker.isAd(url)) {
                    ads.post(() -> ads.setText((++adCounts) + ""));

                    //有广告的请求数据，我们直接返回空数据，注：不能直接返回null
                    return new WebResourceResponse(null, null, null);
                }
                if (webResourceRequest.isForMainFrame() || !webResourceRequest.getMethod().equals("GET")){
                    return super.shouldInterceptRequest(webView, webResourceRequest);
                }
                CookieManager cookieManager = CookieManager.getInstance();
                Map<String, String> headers = webResourceRequest.getRequestHeaders();
                String cookie = cookieManager.getCookie(url);
                if (cookie != null)
                    headers.put("Cookie", cookie);
                SniffingVideo video =  mFilter.onFilter(webView, url, headers);
                if (video.isVideo()){
                    Log.d("sniffed video:", video.getUrl());
                    add(video);
                    return new WebResourceResponse(null, null, null);
                }else if (video.isNull()){
                    return super.shouldInterceptRequest(webView, webResourceRequest);
                }else if (video.hasResponse()){
                    Connection.Response response = video.getResponse();
                    return new WebResourceResponse(video.getContentType().split("; ")[0], response.charset(), response.statusCode(), response.statusMessage(), response.headers(), response.bodyStream());
                }
                //信息不完整，只知道是视频，不知道类型
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return super.shouldInterceptRequest(webView, webResourceRequest);
        }
    };


    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.navi1:
                mAgentWeb.clearWebCache();
                mAgentWeb.getUrlLoader().loadUrl("http://www.549.tv");
                break;
            case R.id.navi2:
                mAgentWeb.clearWebCache();
                mAgentWeb.getUrlLoader().loadUrl("https://video.bqrdh.com");
                break;
            case R.id.search1:
                mAgentWeb.clearWebCache();
                mAgentWeb.getUrlLoader().loadUrl("https://www.cupfox.com");
                break;
            case R.id.search2:
                mAgentWeb.clearWebCache();
                mAgentWeb.getUrlLoader().loadUrl("https://www.dianyinggou.com");
                break;
            case R.id.avtv:
                if (!SPUtils.get(SniffActivity.this).getValue("username", "").equals("17723539610"))
                    break;

                View view = View.inflate(this, R.layout.dialog_secret, null);
                view.findViewById(R.id.btn_login).setOnClickListener(v -> {

                    String password = ((EditText) view.findViewById(R.id.et_password)).getText().toString();
                    if (password.equals("4548")){
                        if (loginDialog != null){
                            loginDialog.hide();
                            loginDialog.dismiss();
                        }
                        Intent intent = new Intent(SniffActivity.this, AvActivity.class);
                        startActivity(intent);
                    }else {
                        if (loginDialog != null){
                            loginDialog.hide();
                            loginDialog.dismiss();
                        }
                    }

                });
                view.findViewById(R.id.btn_clear).setOnClickListener(v -> finish());
                ((EditText)view.findViewById(R.id.et_password)).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setView(view);
                builder.setCancelable(false);
                loginDialog = builder.create();
                loginDialog.show();
                break;
        }
        return false;
    }

    private void showMenu(){
        PopupMenu popup = new PopupMenu(this, menu);//第二个参数是绑定的那个view
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.tool, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }
    private void initViews(){
        menu.setOnClickListener(v -> showMenu());
        searchView.setText(getIntent().getStringExtra("wd"));
        @SuppressLint("UseCompatLoadingForDrawables") Drawable drawable=getResources().getDrawable(R.drawable.search_phone); //获取图片
        drawable.setBounds(0, 0, 50, 50);
        searchView.setCompoundDrawables(drawable, null, null, null);
        searchView.setOnEditorActionListener((v, actionId, event) -> {
            if ((actionId == EditorInfo.IME_ACTION_SEARCH)) {//如果是搜索按钮
                String s = v.getText().toString();
                mAgentWeb.clearWebCache();
                if (s.startsWith("http")){
                    mAgentWeb.getUrlLoader().loadUrl(s);
                }else
                    mAgentWeb.getUrlLoader().loadUrl("https://www.dianyinggou.com/so/" + s);
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                View view = getWindow().peekDecorView();
                if (null != v) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
            return false;
        });
        searchView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus){
                String url = mAgentWeb.getWebCreator().getWebView().getUrl();
                searchView.setText(URLDecoder.decode(url));
                searchView.setSelection(URLDecoder.decode(url).length());
                if (DeviceManager.isTv()) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
                }
            }else
                searchView.setText(mAgentWeb.getWebCreator().getWebView().getTitle());
        });
        hinderAdapter = new AutoCompleteAdapter(this);
        hinderAdapter.setOnItemClickListener(s -> {
            searchView.setText(s, false);
            searchView.setSelection(s.length());
            searchView.dismissDropDown();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            View v = getWindow().peekDecorView();
            if (null != v) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            mAgentWeb.clearWebCache();
            mAgentWeb.getUrlLoader().loadUrl("https://www.dianyinggou.com/so/" + s);
        });
        searchView.setAdapter(hinderAdapter);
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                new Thread(() -> {
                    final List<String> results = SearchKeyTool.search(s.toString());
                    searchView.post(() -> {
                        hinderAdapter.setStringList(results);
                        hinderAdapter.notifyDataSetChanged();
                    });
                }).start();

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {



            }
        });
        back.setOnClickListener(v -> {
            mAgentWeb.getIEventHandler().back();
            sniff.setImageResource(R.drawable.video);
        });
        forth.setOnClickListener(v -> {
            mAgentWeb.getWebCreator().getWebView().goForward();
            sniff.setImageResource(R.drawable.video);
        });
        exit.setOnClickListener(v -> finish());
        refresh.setOnClickListener(v -> mAgentWeb.getWebCreator().getWebView().reload());
        sniff.setOnClickListener(v -> showSniffedList());
        home.setOnClickListener(v -> {
            mAgentWeb.clearWebCache();
            mAgentWeb.getUrlLoader().loadUrl("https://cn.bing.com");
        });
    }
    private void showSniffedList(){
        List<String> items = new ArrayList<>();
        for (SniffingVideo video: videos) {
            String contentType = video.getContentType().isEmpty()?"application/octet-stream":video.getContentType();
            items.add("[" + contentType + "] " + video.getUrl());
        }
        if (items.size() == 0){
            Toast.makeText(SniffActivity.this, "暂未嗅探到可播放资源", Toast.LENGTH_SHORT).show();
            return;
        }
        new CustomDialog.Builder(SniffActivity.this)
                .setTitle("嗅探列表")
                .setMessage("")
                .setList(items, null, -1)
                .setOnItemClickListener((item, o, position, dialog) -> startPlay(videos.get(position)))
                .create()
                .show();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebview(IAgentWebSettings settings){
        WebSettings mWebSettings = settings.getWebSettings();
        mWebSettings.setUserAgentString(DeviceManager.getUserAgent());
        mWebSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        if (AgentWebUtils.checkNetwork(this)) {
            //根据cache-control获取数据。
            mWebSettings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        } else {
            //没网，则从本地获取，即离线加载
            mWebSettings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        mWebSettings.setTextZoom(100);
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        mWebSettings.setDefaultTextEncodingName("utf-8");//设置编码格式
        mWebSettings.setDefaultFontSize(16);
        mWebSettings.setMinimumFontSize(12);//设置 WebView 支持的最小字体大小，默认为 8
        mWebSettings.setUseWideViewPort(true);
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setAllowFileAccess(true);
        mWebSettings.setSupportZoom(true);
        mWebSettings.setGeolocationEnabled(true);
        mWebSettings.setAllowContentAccess(true);
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setSupportMultipleWindows(false);
        mWebSettings.setMediaPlaybackRequiresUserGesture(false);
        mWebSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        mWebSettings.setMediaPlaybackRequiresUserGesture(true);
        mWebSettings.setAllowFileAccessFromFileURLs(true);
        mWebSettings.setAllowUniversalAccessFromFileURLs(true);
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        mWebSettings.setSupportMultipleWindows(false);
        mWebSettings.setLoadsImagesAutomatically(true);
        mWebSettings.setBlockNetworkImage(false);
        mWebSettings.setAppCacheEnabled(true);
        mWebSettings.setAppCachePath(getCacheDir().getAbsolutePath());
        mWebSettings.setDatabaseEnabled(true);
        mWebSettings.setDatabasePath(getCacheDir().getAbsolutePath());
        mWebSettings.setGeolocationDatabasePath(getDir("database", 0).getPath());
        mWebSettings.setGeolocationEnabled(true);
    }

    private void startPlay(SniffingVideo video){
        Intent intent = new Intent(this, PlayAcivity.class);
        Bundle extra = new Bundle();
        for (String key: video.getHeaders().keySet()) {
            extra.putString(key, video.getHeaders().get(key));
        }
        intent.putExtra("url", video.getUrl());
        intent.putExtra("title", mAgentWeb.getWebCreator().getWebView().getTitle());
        intent.putExtra("contentType", video.getContentType());
        intent.putExtra("extra", extra);
        startActivity(intent);
    }
    private Toast toast;
    private void add(SniffingVideo video){
        for (SniffingVideo video1:videos) {
            if (video1.getUrl() == null)
                continue;
            if (video1.getUrl().equals(video.getUrl()))
                return;
        }
        searchView.post(() -> {
            sniff.setImageResource(R.drawable.video_sniffed);
            if (toast == null)
                toast = Toast.makeText(SniffActivity.this, "嗅探到新视频，请点击红色播放按钮观看" + (DeviceManager.isTv()?"(TV请双击菜单键)":""), Toast.LENGTH_LONG);
            toast.show();
        });
        videos.add(video);
    }

    @Override
    protected void onDestroy() {
        mAgentWeb.destroy();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mAgentWeb.getWebLifeCycle().onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mAgentWeb.getWebLifeCycle().onResume();
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            if (!mAgentWeb.back()){
                finish();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            int count = event.getRepeatCount();
            Log.e("key", event.toString());
            showSniffedList();
        }
        return super.onKeyDown(keyCode, event);
    }




    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU){
            showSniffedList();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);

    }

}