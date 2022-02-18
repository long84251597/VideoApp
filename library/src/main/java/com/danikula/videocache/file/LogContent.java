package com.danikula.videocache.file;

public class LogContent extends Object{
    private String body_type = "";
    private long date = 0;
    private String cacheId = "";
    private String net_type = "";
    private String clientIp = "";
    private String ldnsIp = "";
    private String serverIp = "";
    private String domain = "";
    private String url = "";
    private boolean visit_cache = false;
    public LogContent(){

    }

    public void setBody_type(String body_type) {
        this.body_type = body_type;
    }

    public String getBody_type() {
        return body_type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setCacheId(String cacheId) {
        this.cacheId = cacheId;
    }

    public String getCacheId() {
        return cacheId;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getDate() {
        return date;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public void setLdnsIp(String ldnsIp) {
        this.ldnsIp = ldnsIp;
    }

    public String getLdnsIp() {
        return ldnsIp;
    }

    public void setNet_type(String net_type) {
        this.net_type = net_type;
    }

    public String getNet_type() {
        return net_type;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getServerIp() {
        return serverIp;
    }

    public boolean isVisit_cache() {
        return visit_cache;
    }

    public void setVisit_cache(boolean visit_cache) {
        this.visit_cache = visit_cache;
    }
}
