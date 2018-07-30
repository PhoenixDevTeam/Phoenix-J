package biz.dealnote.xmpp.util;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

public class MYID {

    private static String instance;

    private MYID() {

    }

    public static String from(Context context) {
        if (TextUtils.isEmpty(instance)) {
            instance = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        return instance;
    }

}
