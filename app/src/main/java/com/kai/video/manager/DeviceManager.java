package com.kai.video.manager;

import android.app.UiModeManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.kai.video.R;

public class DeviceManager {
    public static int DEVICE_TV = 0;
    public static int DEVICE_PAD = 1;
    public static int DEVICE_PHONE = 2;
    private static int DEVICE = 0;
    private static String user_agent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36";

    public static void setDevice(int device) {
        DeviceManager.DEVICE = device;
        if (device ==  DEVICE_PHONE)
            user_agent = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.79 Mobile Safari/537.36";
    }

    public static boolean tv = false;
    public static String getUserAgent(){
        return user_agent;
    }
    public static int getDialogTheme(){
        if (isTv())
            return R.style.BannerDialog;
        else
            return R.style.CustomDialog;
    }
    public static boolean isTv() {
        return tv;
    }
    public static boolean isPhone(){
        return DEVICE == DEVICE_PHONE;
    }

    public static int getDevice() {
        return DEVICE;
    }

    public static void init(Context context){
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(ContextWrapper.UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            DeviceManager.tv = true;
            DeviceManager.setDevice(DEVICE_TV);
        }
        else {
            DeviceManager.tv = false;
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int type = telephony.getPhoneType();
            if (type == TelephonyManager.PHONE_TYPE_NONE) {
                DeviceManager.setDevice(DEVICE_PAD);
                Log.d("TAG", "is Tablet!");
            } else {
                DeviceManager.setDevice(DEVICE_PHONE);
                Log.d("TAG", "is phone!");

            }
        }
    }
}
