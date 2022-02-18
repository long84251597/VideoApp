package com.kai.video.tool.application;

import android.app.Application;
import android.content.Context;

import com.jeffmony.downloader.VideoDownloadConfig;
import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.common.DownloadConstants;
import com.kai.sniffwebkit.ad.AdBlocker;
import com.kai.video.bean.danmu.DanmuFile;
import com.kai.video.bean.obj.Api;
import com.kai.video.manager.DeviceManager;
import com.just.x5.util.FilePath;
import com.kai.video.tool.net.SearchKeyTool;
import com.kingsoft.media.httpcache.KSYProxyService;
import com.liulishuo.filedownloader.FileDownloader;
import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;

import org.litepal.LitePal;

import java.io.File;
import java.util.HashMap;

public class MyApplication extends Application {
    private KSYProxyService proxy;

    @Override
    public void onCreate() {
        super.onCreate();
        AdBlocker.init();
        ApplicationDownloadTool.getInstance().init(this);
        DeviceManager.init(this);
        SearchKeyTool.init();
        DanmuFile.init(getApplicationContext());
        LitePal.initialize(this);
        //初始化缓存简易下载器
        FileDownloader
                .setupOnApplicationOnCreate(this)
                .maxNetworkThreadCount(10);
        FileDownloader.enableAvoidDropFrame();
        File file =  FilePath.getFilePath(getApplicationContext(), "video");
        if (!file.exists()) {
            file.mkdir();
        }
        VideoDownloadConfig config = new VideoDownloadManager.Build(this)
                .setCacheRoot(file.getAbsolutePath())
                .setTimeOut(DownloadConstants.READ_TIMEOUT, DownloadConstants.CONN_TIMEOUT)
                .setConcurrentCount(DownloadConstants.CONCURRENT)
                .setIgnoreCertErrors(true)
                .setShouldM3U8Merged(false)
                .buildConfig();
        VideoDownloadManager.getInstance().initConfig(config);
        Api.loadApis();
        HashMap<String, Object> map = new HashMap<>();
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
        QbSdk.initTbsSettings(map);
    }


    public static KSYProxyService getProxy(Context context) {
        MyApplication app = (MyApplication) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }
    private KSYProxyService newProxy() {
        KSYProxyService service = new KSYProxyService(this);
        service.setCacheRoot(FilePath.getCachePath(getApplicationContext(), "ksy-cache"));
        service.setMaxCacheSize(1024 * 1024 * 1024);//设置缓存大小为1G
        service.setMaxFilesCount(10);//总缓存视频
        return new KSYProxyService(this);
    }


}
