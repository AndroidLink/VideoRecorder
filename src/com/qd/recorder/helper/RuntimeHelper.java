package com.qd.recorder.helper;


import android.content.Context;
import android.os.PowerManager;

/**
 * Created by yangfeng on 14/12/21.
 */
public class RuntimeHelper {
    public static PowerManager.WakeLock acquireWakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                context.getClass().getSimpleName());
        wakeLock.acquire();
        return wakeLock;
    }

    public static void releaseWakeLock(PowerManager.WakeLock wakeLock) {
        if (wakeLock != null) {
            wakeLock.release();
        }
    }
}
