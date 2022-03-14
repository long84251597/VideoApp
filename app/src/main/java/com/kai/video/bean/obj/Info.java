package com.kai.video.bean.obj;

import java.util.ArrayList;
import java.util.List;

public class Info {
    private String coverPic = "";
    private int type = -1;
    public static int TYPE_TV = 0;//电视剧
    public static int TYPE_MOVIE = 1;//电影类
    public static int TYPE_CARTOON = 3;//动漫类
    public static int TYPE_ZY =2;//综艺类
    public static int TYPE_OTHER = -1;//其他
    private String period = "";
    private String pname = "";
    private boolean zongyi = false;
    private String videoType = "";
    private int count = 0;
    private String description = "";
    private String title = "";
    private String url = "";
    private String current = "0";
    private List<Selection> selections = new ArrayList<>();
    private String videoType_EN = "";
    private String series = "";
    private String name = "";
    private String season = "";
    private String current_text = "";

    public String getCoverPic() {
        return coverPic;
    }

    public void setCoverPic(String coverPic) {
        this.coverPic = coverPic;
    }

    public void setZongyi(boolean zongyi) {
        this.zongyi = zongyi;
    }

    public boolean isZongyi() {
        return zongyi;
    }

    public void setCurrentText(String current_text) {
        this.current_text = current_text;
    }

    public String getCurrentText() {
        return current_text;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getSeason() {
        return season;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getSeries() {
        return series;
    }

    public void setVideoType_EN(String videoType_EN) {
        this.videoType_EN = videoType_EN;
    }

    public String getVideoType_EN() {
        return videoType_EN;
    }

    public void setSelections(List<Selection> selections) {
        this.selections = selections;
    }

    public List<Selection> getSelections() {
        return selections;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public String getCurrent() {
        return current;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }

    public String getVideoType() {
        return videoType;
    }

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getPeriod() {
        return period;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getOutput(){
        switch (type){
            case 0:return "【电视剧】" + name;
            case 1:return "【电影】" + name;
            case 2:return "【综艺】" + name;
            case 3:return "【动漫】" + name;
            default:return "【其他】" + name;
        }
    }

    public static String getOutPutWithType(int type, String name){
        switch (type){
            case 0:return "【电视剧】" + name;
            case 1:return "【电影】" + name;
            case 2:return "【综艺】" + name;
            case 3:return "【动漫】" + name;
            default:return "【其他】" + name;
        }
    }
}
