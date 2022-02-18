package com.just.x5.util;

import android.content.Context;
import android.os.Build;

import java.io.File;

public class FilePath {
    public static File getFilePath(Context context, String name){
        return context.getExternalFilesDir(name);
    }

    public static File getCachePath(Context context, String name) {
        if (name.isEmpty())
            return context.getExternalCacheDir();
        else
            return new File(context.getExternalCacheDir().getAbsolutePath() + "/" + name + "/");
    }
}