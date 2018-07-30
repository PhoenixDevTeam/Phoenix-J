package biz.dealnote.xmpp.util;

import android.text.TextUtils;
import android.util.Log;

import biz.dealnote.xmpp.BuildConfig;

public class Exestime {

    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = Exestime.class.getSimpleName();

    public static void log(String method, long startTime, Object... params) {
        if (DEBUG) {
            if (params == null || params.length == 0) {
                Log.d(TAG, method + ", time: " + (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.d(TAG, method + ", time: " + (System.currentTimeMillis() - startTime) + " ms, params: [" + TextUtils.join(", ", params) + "]");
            }
        }
    }
}
