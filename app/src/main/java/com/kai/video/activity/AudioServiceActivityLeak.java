package com.kai.video.activity;

import android.content.Context;
import android.content.ContextWrapper;

/**
 * Fixes a leak caused by AudioManager using an Activity context.
 * Tracked at https://android-review.googlesource.com/#/c/140481/1 and
 * https://github.com/square/leakcanary/issues/205
 */
public class AudioServiceActivityLeak extends ContextWrapper {

    AudioServiceActivityLeak(Context base) {
        super(base);
    }

    public static ContextWrapper preventLeakOf(Context base) {
        return new AudioServiceActivityLeak(base);
    }

    @Override public Object getSystemService(String name) {
        if (Context.AUDIO_SERVICE.equals(name)) {
            return getApplicationContext().getSystemService(name);
        }
        return super.getSystemService(name);
    }
}