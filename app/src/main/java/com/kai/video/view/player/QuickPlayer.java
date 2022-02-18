package com.kai.video.view.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.kai.video.R;
import com.kai.video.activity.DownloadActivity;
import com.kai.video.bean.obj.Quality;
import com.kai.video.tool.log.LogUtil;
import com.danikula.videocache.IPTool;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;

import java.io.File;
import java.util.Map;

public class QuickPlayer extends NormalGSYVideoPlayer implements PopupMenu.OnMenuItemClickListener {
    private TextView qualityView;
    private TextView contentTypeView;
    private TextView address;
    private View menu;
    public QuickPlayer(Context context){
        super(context);
    }
    public QuickPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.navi1:
                if (!mUrl.startsWith("http")){
                    Toast.makeText(mContext, "视频已经下载", Toast.LENGTH_SHORT).show();
                    break;
                }
                Toast.makeText(mContext, "视频开始下载", Toast.LENGTH_SHORT).show();
                VideoTaskItem item1 = new VideoTaskItem(mUrl, "",  mTitle, "浏览器|外部下载");
                VideoDownloadManager.getInstance().startDownload(item1, mMapHeadData);
                break;
            case R.id.navi2:
                shareFile(mUrl);
                break;
            case R.id.navi3:
                mContext.startActivity(new Intent(mContext, DownloadActivity.class));
                break;
        }
        return false;
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        menu = findViewById(R.id.menu);
        qualityView = findViewById(R.id.quality);
        contentTypeView = findViewById(R.id.contentType);
        address = findViewById(R.id.address);
        menu.setOnClickListener(v -> {
            showSetting();
        });
        seekTask = new SeekTask();
    }

    public void showSetting(){
        PopupMenu popup = new PopupMenu(mContext, menu, Gravity.BOTTOM);//第二个参数是绑定的那个view
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.tool1, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public void startAfterPrepared() {
        super.startAfterPrepared();
        setQuality();
    }

    public void setUp(String url, Map<String, String> headers, String contentType, String title){
        if (url.contains("api.subaibai.com"))
            url  = IPTool.getLocal() +  "/m3u8?url=" + url;
        setUp(url, false,null, headers, title);
        if (contentType == null)
            contentType = "video/mp4";
        else if (contentType.isEmpty())
            contentType = "application/octet-stream";
        setStartAfterPrepared(true);
        startPlayLogic();
        contentTypeView.setVisibility(VISIBLE);
        contentTypeView.setText(contentType);
        address.setVisibility(VISIBLE);
        address.setText(url.replaceAll("\\?.*", ""));
    }
    @Override
    public boolean setUp(String url, boolean cacheWithPlay, File cachePath, Map<String, String> mapHeadData, String title) {

        return super.setUp(url, cacheWithPlay, cachePath, mapHeadData, title);
    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_quick_player;
    }
    private void setQuality() {
        Quality quality = new Quality(getCurrentVideoWidth());
        String text = quality.get();
        int color = R.color.color_low;
        switch (text){
            case "4K 蓝光HDR":color = R.color.color_4k;break;
            case "1080P 蓝光":color = R.color.color_1080;break;
            case "720P 超清":color = R.color.color_720;break;
            default:break;
        }
        qualityView.setText(text);
        if (!text.isEmpty()){
            qualityView.setVisibility(VISIBLE);
        }else
            qualityView.setVisibility(INVISIBLE);
        qualityView.getBackground().setColorFilter(mContext.getResources().getColor(color), PorterDuff.Mode.SRC);


    }

    private void shareFile(String filePath) {
        if (filePath == null)
            return;
        Intent share_intent = new Intent();
        share_intent.setAction(Intent.ACTION_SEND);//设置分享行为
        if (filePath.startsWith("http")){
            if (contentTypeView.getText().equals("flv")){
                Toast.makeText(mContext, "FLV视频无法直接分享，请下载后分享文件", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "来自影视凯TV分享 <<" + mTitle + ">>" +
                    "播放链接：" + mUrl);
            share_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            share_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(Intent.createChooser(intent, mTitle));
        }else {
            File file = new File(filePath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".fileProvider", file);
                share_intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            } else {
                share_intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            }

            share_intent.setType("video/mp4");//设置分享内容的类型
            share_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            share_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(Intent.createChooser(share_intent, mTitle + "——视频文件分享"));
        }
    }
    private SeekTask seekTask;
    public void setSeekAdd(int add){
        if (seekTask.isCancelled()){
            seekTask = new SeekTask();
            seekTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else if (!seekTask.isStarted()){
            seekTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        seekTask.add(add);
    }
    @SuppressLint("StaticFieldLeak")
    public class SeekTask extends AsyncTask {
        private boolean started = false;
        private int progress = 0;
        private int remainTime = 2;
        private final Handler handler = new Handler();
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            started = true;
            showProgressDialog((float) 1, TvPlayer.translateLong(getGSYVideoManager().getCurrentPosition()), getGSYVideoManager().getBufferedPercentage(), TvPlayer.translateLong((long)getDuration()), 100);
            progress = (int) ((float)getCurrentPositionWhenPlaying()/(float) getDuration()*100);
            LogUtil.d("tag", "ss" + progress);
            mDialogProgressBar.setProgress(progress);
        }

        public boolean isStarted() {
            return started;
        }

        public void activateDelay() {
            this.remainTime = 1;
        }
        public void add(int a){
            if (a > 0)
                add();
            else
                sub();
        }
        public void add(){
            progress++;
            if (progress > 100)
                progress = 100;
            activateDelay();
            handler.post(() -> setDialogProgress(progress));
        }
        public void sub(){
            progress--;
            if (progress < 0)
                progress = 0;
            activateDelay();
            handler.post(() -> setDialogProgress(progress));
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Log.d("tag", remainTime + "s");
            while (remainTime >= 0){
                try {
                    Thread.sleep(500);
                    remainTime--;
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            dismissProgressDialog();
            if (progress == 0)
                seekTo(1000);
            else if (progress == 100)
                seekTo(getDuration() - 1000);
            else
                seekTo((long) getDuration() * progress /100);
            cancel(true);
        }
    }

    private void setDialogProgress(int progress){
        if (mDialogSeekTime != null)
            mDialogSeekTime.setText(TvPlayer.translateLong((long)getDuration() * progress / 100));
        if (mDialogProgressBar != null)
            mDialogProgressBar.setProgress(progress);

    }


}
