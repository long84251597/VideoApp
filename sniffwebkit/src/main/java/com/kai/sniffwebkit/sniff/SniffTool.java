package com.kai.sniffwebkit.sniff;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.just.x5.AgentWebX5;
import com.just.x5.DefaultWebClient;
import com.just.x5.IWebSettings;
import com.just.x5.WebDefaultSettingsImpl;
import com.just.x5.builder.AgentBuilder;
import com.just.x5.util.AgentWebX5Utils;
import com.kai.sniffwebkit.LoadingView;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.export.external.interfaces.WebResourceError;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.jsoup.Connection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

//无界面webview资源嗅探器
public class SniffTool {
    private static boolean autoRelease = false;
    private static long releaseTimeout = 5 * 60 * 1000;
    private static String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36";
    private final Activity activity;
    private Callback mCallback;
    private AgentWebX5 agentWeb;
    private ViewGroup mainView;
    private String targetUrl;
    private long startTime = 0;
    //最终得到的目标资源
    private SniffingVideo sniffingVideo;
    private MySniffingFilter mFilter;
    private WebViewClient mWebViewClient;
    private LoadingView loadingView;
    private final String cachePath;

    //单例静态化
    @SuppressLint("StaticFieldLeak")
    private static SniffTool mSniffTool = null;

    private long timeout_sniff = 50 * 1000;//默认嗅探时间，超时即关闭页面
    private long timeout_js = 50 * 1000;//默认js加载时间，超时即关闭页面
    private static final int STATUS_SUCCESS = 0;
    private static final int STATUS_ERROR = -1;
    private static final int STATUS_TIMEOUT = 2;
    private static final int STATUS_TIMEOUT_JS = 3;
    private static final int STATUS_RELEASE = 4;
    private boolean reseted = false;
    private boolean openOtherApp = false;
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            removeCallbacksAndMessages(null);
            super.handleMessage(msg);
            String message = "";
            switch (msg.what){
                case STATUS_RELEASE:
                    destoryTool();
                    return;
                case STATUS_SUCCESS:
                    message = "嗅探成功";
                    agentWeb.getWebLifeCycle().onPause();
                    break;
                case STATUS_ERROR:
                    message = "接口连接失败";
                    break;
                case STATUS_TIMEOUT:
                    message = "嗅探超时";
                    break;
                case STATUS_TIMEOUT_JS:
                    message = "脚本嗅探超时";
                    break;
                default:break;
            }
            if (loadingView != null) {
                loadingView.setProgress(100, message);
                loadingView.hide();
            }
            Toast.makeText(activity, message + ",耗时：" + (System.currentTimeMillis() - startTime)/1000 + "秒", Toast.LENGTH_SHORT).show();
            completeSniff(msg.what);
        }
    };


    public static String getUserAgent() {
        return userAgent;
    }

    private SniffTool(Activity activity){
        this.activity = activity;
        mainView = activity.findViewById(android.R.id.content);
        cachePath = activity.getCacheDir().getAbsolutePath();
        //初始化好嗅探器，但先载入空白页
        mWebViewClient = new WebViewClient(){
            @Override
            public void onPageStarted(WebView webView, String s, Bitmap bitmap) {
                super.onPageStarted(webView, s, bitmap);
                sniffingVideo = null;
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                //如果主页面发生了错误，则通知报错
                if (request == null || !request.isForMainFrame()) {
                    return;
                }
                handler.sendEmptyMessage(STATUS_ERROR);
            }

            @Override
            public void onPageFinished(WebView webView, String s) {
                super.onPageFinished(webView, s);
                //当页面加载完成，就需要设定js超时
                if (!reseted)
                    handler.sendEmptyMessageDelayed(STATUS_TIMEOUT_JS, timeout_js);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                //super.onReceivedError(view, request, error);
                if (request !=null && request.isForMainFrame())
                    handler.sendEmptyMessage(STATUS_ERROR);
            }


            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String s) {
                return false;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();//證書不對的時候，繼續加載
            }

            @SuppressLint("SetTextI18n")
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
                try {
                    String url = webResourceRequest.getUrl().toString();
                    //只过滤除主页面外的http get请求
                    //如果含有根页面的网页和url=的解析页面都一律不过滤
                    if (webResourceRequest.isForMainFrame() || !webResourceRequest.getMethod().equals("GET") || !url.startsWith("http") || targetUrl.contains(url.replaceAll("\\?.*", "")) || url.contains("url=")){
                        return super.shouldInterceptRequest(webView, webResourceRequest);
                    }
                    Map<String, String> headers = webResourceRequest.getRequestHeaders();
                    SniffingVideo video =  mFilter.onFilter(webView, url, headers);
                    if (video.isVideo()){
                        Log.e("sniffed video:", video.getUrl());
                        sniffingVideo = video;
                        handler.sendEmptyMessage(STATUS_SUCCESS);
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
        AgentBuilder builder = AgentWebX5.with(activity);
        builder.setmWebViewClient(mWebViewClient);
        builder.setInterceptUnkownScheme(false);
        builder.setWebclientHelper(true);
        try {
            Method method = Class.forName("android.webkit.WebView").
                    getMethod("setWebContentsDebuggingEnabled", Boolean.TYPE);
            method.setAccessible(true);
            method.invoke(null, true);
        } catch (Exception e) {
            // do nothing
        }
        builder.setAgentWebParent(mainView, new LinearLayout.LayoutParams(0, 0)).useDefaultIndicator();
        builder.setmWebSettings(new WebDefaultSettingsImpl(){
            @Override
            public IWebSettings toSetting(WebView webView) {
                IWebSettings webSettings = super.toSetting(webView);
                initWebview(webSettings);
                return webSettings;
            }
        });
        if (openOtherApp)
            builder.setOpenOtherPage(DefaultWebClient.OpenOtherPageWays.ASK);
        builder.setmWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (loadingView != null)
                    loadingView.setProgress(newProgress - 5, "嗅探中");
                if (mCallback != null)
                    mCallback.onProgress(newProgress);
            }
        });
        agentWeb = builder.buildAgentWeb().go("about:blank");
        CookieManager.getInstance().setAcceptThirdPartyCookies(agentWeb.getWebCreator().get(), true);

    }
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebview(IWebSettings settings){
        WebSettings mWebSettings = settings.getWebSettings();
        mWebSettings.setUserAgentString(userAgent);
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setDatabaseEnabled(true);
        mWebSettings.setMediaPlaybackRequiresUserGesture(false);
        mWebSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        if (AgentWebX5Utils.checkNetwork(activity)) {
            //根据cache-control获取数据。
            mWebSettings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        } else {
            //没网，则从本地获取，即离线加载
            mWebSettings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        mWebSettings.setTextZoom(100);
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        //mWebSettings.setDefaultTextEncodingName("utf-8");//设置编码格式
        mWebSettings.setDefaultFontSize(16);
        mWebSettings.setMinimumFontSize(12);//设置 WebView 支持的最小字体大小，默认为 8
        mWebSettings.setUseWideViewPort(false);
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setAllowFileAccess(true);
        mWebSettings.setSupportZoom(false);
        mWebSettings.setGeolocationEnabled(true);
        mWebSettings.setAllowContentAccess(true);


        mWebSettings.setSupportMultipleWindows(false);

        mWebSettings.setAllowFileAccessFromFileURLs(false);
        mWebSettings.setAllowUniversalAccessFromFileURLs(false);
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        mWebSettings.setSupportMultipleWindows(false);
        mWebSettings.setLoadsImagesAutomatically(true);
        mWebSettings.setBlockNetworkImage(false);
        mWebSettings.setAppCachePath(cachePath);
        WebView.setWebContentsDebuggingEnabled(true);
        mWebSettings.setSaveFormData(false);
        mWebSettings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        mWebSettings.setDatabasePath(cachePath);
        mWebSettings.setGeolocationDatabasePath(cachePath);
        mWebSettings.setPluginState(WebSettings.PluginState.ON);
    }

    public static SniffTool getInstance(Activity activity){
        if (mSniffTool == null)
            mSniffTool = new SniffTool(activity);
        return mSniffTool;
    }

    public SniffTool setSniffTimeout(long timeout){
        this.timeout_sniff = timeout;
        return this;
    }

    public SniffTool setJsTimeout(long timeout){
        this.timeout_js = timeout;
        return this;
    }

    public SniffTool setCallback(Callback callback){
        this.mCallback = callback;
        return this;
    }

    public SniffTool setFilter(MySniffingFilter mFilter){
        this.mFilter = mFilter;
        return this;
    }

    public SniffTool userAgent(String userAgent){
        SniffTool.userAgent = userAgent;
        return this;
    }

    public SniffTool target(String url){
        this.targetUrl = url;
        return this;
    }

    public SniffTool bindLoadingView(LoadingView view){
        this.loadingView = view;
        return this;
    }

    public void start(){
        //mainview 延时触发超时器
        if (loadingView != null) {
            loadingView.show();
            loadingView.setProgress(1, "开始嗅探");
        }
        //如果暂停了记得了唤醒一下
        if (agentWeb.getWebLifeCycle().isPause())
            resume();
        handler.removeCallbacksAndMessages(null);
        //如果目前还有工作在进行
        if (!reseted)
            reset();
        handler.sendEmptyMessageDelayed(STATUS_TIMEOUT, timeout_sniff);
        reseted = false;
        if (agentWeb != null)
            agentWeb.getWebCreator().get().loadUrl(targetUrl);
        startTime = System.currentTimeMillis();
    }

    public interface Callback{
        void onSuccess(SniffingVideo video);
        void onFailed(int errorCode);
        void onProgress(int progress);
    }


    //在每次嗅探完成时要复位webview
    public void reset(){
        reseted = true;
        agentWeb.clearWebCache();
        agentWeb.getWebCreator().get().clearHistory();
        agentWeb.getWebCreator().get().loadUrl("about:blank");
    }

    //在活动销毁前记得调用
    private void destroy(){
        agentWeb.getWebLifeCycle().onDestroy();
        agentWeb = null;
        mCallback = null;
        mWebViewClient = null;
        targetUrl = null;
        handler.removeCallbacksAndMessages(null);
        mainView = null;
        sniffingVideo = null;
        mFilter = null;

    }

    private void pause(){
        handler.removeCallbacksAndMessages(null);
        reset();
        agentWeb.getWebLifeCycle().onPause();
        handler.removeCallbacksAndMessages(null);
        //暂停5分钟后自动销毁
        handler.sendEmptyMessageDelayed(STATUS_RELEASE, releaseTimeout);
    }

    private void resume(){
        agentWeb.getWebLifeCycle().onResume();
    }

    public static void destoryTool(){
        if (mSniffTool != null) {
            mSniffTool.destroy();
            mSniffTool = null;
        }
    }

    public static void pauseTool(){
        if (mSniffTool != null){
            mSniffTool.pause();
        }
    }

    public static void resumeTool(){
        if (mSniffTool != null){
            mSniffTool.resume();
        }
    }



    private void completeSniff(int errorCode){
        if (mCallback == null)
            return;

        if (reseted){
            return;
        }
        if (sniffingVideo != null){
            mCallback.onSuccess(sniffingVideo);
        }else {
            mCallback.onFailed(errorCode);
        }
        reset();
    }

    private class MyJavaScriptInterface implements JavascriptInterface {

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return null;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    }
}
