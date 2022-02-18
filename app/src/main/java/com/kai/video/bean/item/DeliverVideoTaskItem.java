package com.kai.video.bean.item;

import com.jeffmony.downloader.model.VideoTaskItem;
import com.kai.video.bean.GroupBean;

import java.io.Serializable;

public class DeliverVideoTaskItem implements Serializable {
    private String url;
    private String path;
    private float speed;
    private float percent;
    private int state;
    private String title;
    private String groupName;
    private boolean completed;

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
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

    public static DeliverVideoTaskItem packBean(GroupBean.VideoBean bean){
        DeliverVideoTaskItem deliverVideoTaskItem = new DeliverVideoTaskItem();
        deliverVideoTaskItem.setPath(bean.getPath());
        deliverVideoTaskItem.setPercent(bean.getPercent());
        deliverVideoTaskItem.setSpeed(bean.getSpeed());
        deliverVideoTaskItem.setState(bean.getState());
        deliverVideoTaskItem.setTitle(bean.getTitle());
        deliverVideoTaskItem.setUrl(bean.getUrl());
        deliverVideoTaskItem.setGroupName(bean.getGroupName());
        deliverVideoTaskItem.setCompleted(bean.getPercent() == 1);
        return deliverVideoTaskItem;
    }
    public static DeliverVideoTaskItem pack(VideoTaskItem item){
        DeliverVideoTaskItem deliverVideoTaskItem = new DeliverVideoTaskItem();
        deliverVideoTaskItem.setPath(item.getFilePath());
        deliverVideoTaskItem.setPercent(item.getPercent());
        deliverVideoTaskItem.setSpeed(item.getSpeed());
        deliverVideoTaskItem.setState(item.getTaskState());
        deliverVideoTaskItem.setTitle(item.getTitle());
        deliverVideoTaskItem.setUrl(item.getUrl());
        deliverVideoTaskItem.setGroupName(item.getGroupName());
        deliverVideoTaskItem.setCompleted(item.isCompleted());
        return deliverVideoTaskItem;
    }
    public static VideoTaskItem unpack(DeliverVideoTaskItem deliverVideoTaskItem){
        VideoTaskItem item = new VideoTaskItem(deliverVideoTaskItem.getUrl());
        item.setPercent(deliverVideoTaskItem.getPercent());
        item.setTitle(deliverVideoTaskItem.getTitle());
        item.setTaskState(deliverVideoTaskItem.getState());
        item.setSpeed(deliverVideoTaskItem.getSpeed());
        item.setFilePath(deliverVideoTaskItem.getPath());
        item.setGroupName(deliverVideoTaskItem.getGroupName());
        item.setIsCompleted(deliverVideoTaskItem.isCompleted());
        return item;
    }
}
