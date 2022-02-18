package com.kai.video.bean.obj;

public class FloatingSimul {
    static long currentTime = 0;
    public static int TYPE_FORGROUND = 0;//前台播放
    public static int TYPE_BACKGROUND = 1;//后台播放，即小窗播放
    public static int TYPE_BACKGROUND_WITH_PAUSED = 2;//播放暂停并后台放置
    static int player_type = TYPE_BACKGROUND_WITH_PAUSED;

    public static void setPlayerType(int player_type) {
        FloatingSimul.player_type = player_type;
    }

    public static int getPlayerType() {
        return player_type;
    }


}
