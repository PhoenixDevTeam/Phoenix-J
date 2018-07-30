package biz.dealnote.xmpp.util;

import android.util.Log;

import biz.dealnote.xmpp.BuildConfig;

/**
 * Created by ruslan.kolbasa on 01.11.2016.
 * phoenix_for_xmpp
 */
public class Logger {
    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static void d(String tag, String messsage){
        if(DEBUG){
            Log.d(tag, messsage);
        }
    }

    public static void e(String tag, String messsage){
        if(DEBUG){
            Log.e(tag, messsage);
        }
    }

    public static void wtf(String tag, String messsage) {
        if(DEBUG){
            Log.wtf(tag, messsage);
        }
    }
}
