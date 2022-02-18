package com.kai.video.manager;

import android.app.Activity;

import com.kai.video.tool.application.ApplicationDownloadTool;

import java.util.ArrayList;
import java.util.List;


public class ActivityCollector{


    public static List<Activity> activities = new ArrayList<>();
    public static void addActivity(Activity activity){
        activities.add(activity);
    }
    public static void removeActivity(Activity activity){
        activities.remove(activity);
    }
    public static void finishAll(){
        ApplicationDownloadTool.getInstance().destory();
        for (Activity activity: activities){
            try {
                activity.finish();
            } catch (Exception e){
                e.printStackTrace();
                break;
            }
        }
        try {
            System.gc();
            System.runFinalization();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
        }


    }
    public static void clearTop(Activity activity){
        for (int i = 0; i < activities.size(); i++) {
            Activity a = activities.get(i);
            if (a.getLocalClassName().equals(activity.getLocalClassName())){
                activities.set(i, activity);
                activities = activities.subList(0, i+1);
                break;
            }
        }
    }



}
