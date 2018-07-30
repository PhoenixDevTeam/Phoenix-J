package biz.dealnote.xmpp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Random;

public class AppPrefs {

    private static final int ONLINE_DELAY = 60 * 1000;

    public static boolean checkOnline(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long last = preferences.getLong("last_online_check", 0);
        long now = System.currentTimeMillis();

        if (Math.abs(last - now) > ONLINE_DELAY) {
            preferences.edit().putLong("last_online_check", now).apply();
            return true;
        } else {
            return false;
        }
    }

    private static final String KEY_MESSAGE_STANZA_ID = "out_message_stanza_id";
    private static final Random RANDOM = new Random();

    public static String generateMessageStanzaId(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int target = preferences.getInt(KEY_MESSAGE_STANZA_ID, RANDOM.nextInt(123456)) + 1;

        preferences.edit().putInt(KEY_MESSAGE_STANZA_ID, target).apply();
        return MYID.from(context) + "@" + target;
    }
}
