package com.kai.video.bean.item;

public class CommendItem{
    private String name = "";
    private String subTitle = "";
    private String url = "";
    private String poster = "";

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPoster(String poster) {
        this.poster = poster;
        if (poster.startsWith("//"))
            this.poster = "http:" + poster;
    }

    public String getPoster() {
        return poster;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url.replace("m.mgtv.com", "www.mgtv.com");
    }

}
