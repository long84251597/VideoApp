package com.kai.video.manager;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.kai.video.R;
import com.kai.video.activity.InfoActivity;

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

    public static void getDevice(Activity activity, OnViewAttachListener onViewAttachListener) {
        if (DeviceManager.isTv()){
            activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
            activity.setContentView(onViewAttachListener.onInitTV());
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.getWindow().setStatusBarColor(activity.getColor(R.color.colorPrimary));
            }
            Configuration configuration = activity.getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                activity.setContentView(onViewAttachListener.onInitPad());
            }else
                activity.setContentView(onViewAttachListener.onInitPhone());
        }
    }

    public interface OnViewAttachListener{
        int onInitTV();
        int onInitPad();
        int onInitPhone();
    }

    public static int getSpanCount(Context context){
        if (DeviceManager.isTv()){
            return 7;
        }else{
            Configuration configuration = context.getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return 7;
            }else
                return 4;
        }
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
