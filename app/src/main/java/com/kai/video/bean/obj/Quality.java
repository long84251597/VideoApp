package com.kai.video.bean.obj;

public class Quality {
    private int level = 0;
    private final int width;
    public Quality(int width){
        this.width = width;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        String name = "";
        return name;
    }

    public  String get(){
        if (width > 1920){
            level = 7;
            return "4K 蓝光HDR";
        }else if (width > 1280){
            level = 6;
            return "1080P 蓝光";
        }else if (width > 900){
            level = 5;
            return "720P 超清";
        }else if (width > 640){
            level = 4;
            return "640P 高清";
        }else if (width > 480){
            level = 3;
            return "480P 标清";
        }else if (width > 320){
            level = 2;
            return "320P 普清";
        }else if (width >0){
            level = 1;
            return "270P 流畅";
        }else {
            level = 0;
            return "";
        }
    }
}
