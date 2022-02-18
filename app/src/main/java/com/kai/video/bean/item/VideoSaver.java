package com.kai.video.bean.item;

import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

public class VideoSaver extends LitePalSupport {
    @Column(unique = true)
    String url;
    @Column(index = true)
    int api = 0;

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public int getApi() {
        return api;
    }

    public void setApi(int apiIndex) {
        this.api = apiIndex;
    }
}
