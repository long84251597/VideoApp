package com.kai.video.bean.item;

import java.util.ArrayList;
import java.util.List;

public class NaviItem {
    private boolean HasHeader = false;
    private String header = "";
    private String name = "";
    private String url = "";
    private String poster = "";
    private List<CommendItem> extend = new ArrayList<>();

    public void setHasHeader(boolean hasHeader) {
        HasHeader = hasHeader;
    }

    public boolean isHasHeader() {
        return HasHeader;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getPoster() {
        return poster;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setExtend(List<CommendItem> extend) {
        this.extend = extend;
    }

    public List<CommendItem> getExtend() {
        return extend;
    }
}
