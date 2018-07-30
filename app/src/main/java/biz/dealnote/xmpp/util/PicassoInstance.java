package biz.dealnote.xmpp.util;

import android.annotation.SuppressLint;

import com.squareup.picasso.Picasso;

/**
 * Created by admin on 3/11/2018.
 * cashup-mobile-app
 */
public final class PicassoInstance {

    @SuppressLint("StaticFieldLeak")
    private static Picasso instance;

    public static Picasso get() {
        return instance;
    }

    public static void init(Picasso picasso){
        instance = picasso;
    }
}