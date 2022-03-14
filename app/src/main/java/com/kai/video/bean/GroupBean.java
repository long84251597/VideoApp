package com.kai.video.bean;

import com.baozi.treerecyclerview.annotation.TreeDataType;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.kai.video.bean.item.GroupItem;
import com.kai.video.bean.item.VideoItem;
import com.kai.video.tool.file.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@TreeDataType(iClass = GroupItem.class)
public class GroupBean {
    private String videoType;
    private String videoName;
    private String groupName;
    private boolean expand = false;
    private boolean alive = false;
    private String poster;

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getPoster() {
        return poster;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isExpand() {
        return expand;
    }

    public void setExpand(boolean expand) {
        this.expand = expand;
    }

    private List<VideoBean> videoBeans = new ArrayList<>();
    public GroupBean(VideoTaskItem item){
        try {
            String[] params = item.getGroupName().split("\\|");
            videoType = params[1];
            videoName = params[0];
        }catch (Exception e){
            videoName = item.getTitle();
            videoType = "其他视频";
        }
        groupName = item.getGroupName();
        VideoBean videoBean = VideoBean.trans(item);
        videoBean.fetchFileSize();
        videoBeans.add(videoBean);
        if (item.getCoverPath() != null && new File(item.getCoverPath()).exists()){
            setPoster(item.getCoverPath());
        }else
            setPoster(item.getCoverUrl());
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getOutPut(){
        int finish = 0;
        for (VideoBean bean:videoBeans) {
            if (bean.isCompleted())
                finish++;
        }
        return finish + "/" + videoBeans.size();
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }

    public String getVideoType() {
        return videoType;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoBeans(List<VideoBean> videoBeans) {
        this.videoBeans = videoBeans;
    }

    public List<VideoBean> getVideoBeans() {
        return videoBeans;
    }

    public void add(VideoTaskItem item){
        VideoBean videoBean = VideoBean.trans(item);
        videoBean.fetchFileSize();
        videoBeans.add(videoBean);
        if (!item.getCoverUrl().isEmpty()){
            if (item.getCoverPath() != null && new File(item.getCoverPath()).exists()){
                setPoster(item.getCoverPath());
            }else
                setPoster(item.getCoverUrl());
        }
    }

    public void delete(VideoTaskItem item){
        for(int i = 0; i < videoBeans.size(); i++){
            GroupBean.VideoBean bean = videoBeans.get(i);
            if (bean.getTitle().equals(item.getTitle())){
                videoBeans.remove(i);
                break;
            }

        }
    }

    public boolean replace(VideoTaskItem item){
        for(int i = 0; i < videoBeans.size(); i++){
            GroupBean.VideoBean bean = videoBeans.get(i);
            if (bean.getTitle().equals(item.getTitle())){
                VideoBean videoBean = VideoBean.trans(item);
                if (item.isCompleted()){
                    videoBean.fetchFileSize();
                }
                videoBeans.set(i, videoBean);
                return true;
            }
        }
        return false;
    }

    @TreeDataType(iClass = VideoItem.class)
    public static class VideoBean{
        public String url;
        public String path;
        public float speed;
        public float percent;
        public int state;
        public boolean completed;
        public String title;
        public String groupName;
        public boolean hlsType;
        public String percentString;
        public String speedString;
        public String fileSize = "";
        public String coverPic = "";
        public String coverPath = "";

        public void setCoverPath(String coverPath) {
            this.coverPath = coverPath;
        }

        public String getCoverPath() {
            return coverPath;
        }

        public void setCoverPic(String coverPic) {
            this.coverPic = coverPic;
        }

        public String getCoverPic() {
            return coverPic;
        }

        public void fetchFileSize() {
            try {
                if (completed){
                    if (isHlsType())
                        fileSize =  FileUtils.getFileSize(new File(getPath()).getParentFile());
                    else
                        fileSize = FileUtils.getFileSize(new File(getPath()));
                }

            }catch (Exception e){

            }

        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public boolean isCompleted() {
            return completed;
        }

        public String getFileSize() {
            return fileSize;
        }

        public static VideoBean trans(VideoTaskItem item){
            VideoBean bean = new VideoBean();
            bean.setGroupName(item.getGroupName());
            bean.setPath(item.getFilePath());
            bean.setPercent(item.getPercent());
            bean.setTitle(item.getTitle());
            bean.setUrl(item.getUrl());
            bean.setState(item.getTaskState());
            bean.setHlsType(item.isHlsType() || item.getUrl().contains(".m3u8"));
            bean.setPercentString(item.getPercentString());
            bean.setSpeedString(item.getSpeedString());
            bean.setCompleted(item.isCompleted());
            bean.setCoverPic(item.getCoverUrl());
            bean.setCoverPath(item.getCoverPath());
            return bean;
        }

        public void setPercentString(String percentString) {
            this.percentString = percentString;
        }

        public void setSpeedString(String speedString) {
            this.speedString = speedString;
        }

        public String getSpeedString() {
            return speedString;
        }

        public String getPercentString() {
            return percentString;
        }

        public void setHlsType(boolean hlsType) {
            this.hlsType = hlsType;
        }

        public boolean isHlsType() {
            return hlsType;
        }

        public String getUrl() {
            return url;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public float getPercent() {
            return percent;
        }

        public void setPercent(float percent) {
            this.percent = percent;
        }

        public float getSpeed() {
            return speed;
        }

        public void setSpeed(float speed) {
            this.speed = speed;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
