package com.kai.video.manager;

import android.content.Context;

import com.jeffmony.downloader.m3u8.M3U8;
import com.jeffmony.downloader.m3u8.M3U8Utils;
import com.kai.video.tool.application.SPUtils;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.cache.CacheFactory;
import com.shuyu.gsyvideoplayer.model.VideoOptionModel;
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager;
import com.shuyu.gsyvideoplayer.player.PlayerFactory;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;

import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MyPlayerManager {
    public static int KERNEL_IJK_SOFT = 0;
    public static int KERNEL_IJK_HARD = 1;
    public static int KERNEL_DEFAULT = 0;

    public static void setKernelDefault(int kernelDefault) {
        MyPlayerManager.KERNEL_DEFAULT = kernelDefault;
    }
    public static void setKernelDefault(boolean tv){
        MyPlayerManager.KERNEL_DEFAULT = tv?KERNEL_IJK_HARD:KERNEL_IJK_SOFT;
    }

    public static void changeMode(Context context, int mode){
        SPUtils spUtils = SPUtils.get(context);
        if (mode == KERNEL_IJK_SOFT || mode == KERNEL_IJK_HARD){
            CacheFactory.setCacheManager(ProxyCacheManager.class);
            List<VideoOptionModel> videoOptionModels = new ArrayList<>();
            PlayerFactory.setPlayManager(IjkPlayerManager.class);
            IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT);
            if (mode == KERNEL_IJK_HARD){
                GSYVideoType.setRenderType(GSYVideoType.SUFRACE);
                //根据设备类型设置是否开启环路过滤
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_mpeg4", 1));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_mpeg2", 1));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-avc", 1));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-all-videos", 1));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "videotoolbox", 0));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1));
                //videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1));

            }else {
                GSYVideoType.disableMediaCodec();
                GSYVideoType.disableMediaCodecTexture();
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0));
                videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "videotoolbox", 1));
                GSYVideoType.setRenderType(GSYVideoType.TEXTURE);
            }

            videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", "Roku/DVP‑9.10 (089.10E04121A)"));
            videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"reconnect",5));
            videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1));
            videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_timeout", -1));
            //videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"timeout",20));
            videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "async,cache,crypto,file,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtp,tcp,tls,udp,ijkurlhook,data,concat,subfile,udp,ffconcat"));
            videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "allowed_extensions", "ALL"));
            videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"safe",0));
            videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1));
            videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 0));
            //videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1));
            videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "fastseek"));
            //videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1));
            //videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 500 * 1024));
            //videoOptionModels.add(new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5));
            GSYVideoManager.instance().setOptionModelList(videoOptionModels);


        }
        spUtils.putValue("media", mode);
    }
    public static void loadMode(Context context){
        changeMode(context, SPUtils.get(context).getValue("media", KERNEL_DEFAULT));
    }
    public static void loadScreen(Context context){
        changeScreen(context, SPUtils.get(context).getValue("screen", 0));
    }
    public static void changeScreen(Context context, int mode){
        GSYVideoType.setShowType(mode);
        SPUtils.get(context).putValue("screen", mode);
    }

    public static int getCurrentKernel(Context context){
        return SPUtils.get(context).getValue("media", KERNEL_DEFAULT);
    }
    public static void loadDefault(Context context){
        changeMode(context, KERNEL_DEFAULT);
    }
    public static String[] getInfo(Context context){
        String[] infos = new String[3];
        switch (getCurrentKernel(context)){
            case 0:infos[0] = "IJK内核";infos[1] = "软解";infos[2] = "videotoolbox";break;
            case 1:infos[0] = "IJK内核";infos[1] = "硬解";infos[2] = "mediacodec";break;
            case 2:infos[0] = "KSY内核";infos[1] = "软解";infos[2] = "videotoolbox";break;
            case 3:infos[0] = "KSY内核";infos[1] = "硬解";infos[2] = "mediacodec";break;
        }
        return infos;
    }
    public static boolean isHardcodec(Context context){
        int current = getCurrentKernel(context);
        return current == KERNEL_IJK_HARD;
    }


}
