package com.kai.video.tool.application;

import com.kai.video.manager.DeviceManager;

/*
电视上的GC机制
 */
public class GC{
    public static void clean(){
        try {
            if (DeviceManager.isTv()) {
                System.runFinalization();
                System.gc();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
