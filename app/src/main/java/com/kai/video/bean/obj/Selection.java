package com.kai.video.bean.obj;

public class Selection{
    //默认不使用current
    private boolean current = false;
    private String videoTitle;
    private String title;
    private int type;
    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public boolean isCurrent() {
        return current;
    }

    //专用于视频内集数显示
    public static final String[] headers = new String[]{"【免费】 ", "【预告】 ", "【会员】 ", "【点播】 "};
    public String getVisibleTitle(){
        if (type < 0 && type > 3)
            return "";
        String header = headers[type];
        try {
            return header + "第" + Integer.parseInt(title) + "集";
        }catch (Exception e){
            e.printStackTrace();
            return header.replaceAll("\\s", "") + title.replaceAll("-", "");
        }
    }
}
