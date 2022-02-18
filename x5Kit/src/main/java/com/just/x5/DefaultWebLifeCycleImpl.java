package com.just.x5;

import com.just.x5.util.AgentWebX5Utils;
import com.tencent.smtt.sdk.WebView;


/**
 * source code  https://github.com/Justson/AgentWebX5
 */

public class DefaultWebLifeCycleImpl implements IWebLifeCycle {
    private boolean pause = false;
    private WebView mWebView;
    DefaultWebLifeCycleImpl(WebView webView) {
        this.mWebView = webView;
    }

    @Override
    public void onResume() {
        if (this.mWebView != null) {
            this.mWebView.onResume();
            this.mWebView.resumeTimers();
            pause = false;
        }
    }

    @Override
    public void onPause() {
        if (this.mWebView != null) {
            this.mWebView.pauseTimers();
            this.mWebView.onPause();
            pause = true;
        }
    }

    @Override
    public void onDestroy() {
        if (this.mWebView != null) {
            this.mWebView.resumeTimers();
        }
        AgentWebX5Utils.clearWebView(this.mWebView);
    }

    @Override
    public boolean isPause() {
        return pause;
    }
}
