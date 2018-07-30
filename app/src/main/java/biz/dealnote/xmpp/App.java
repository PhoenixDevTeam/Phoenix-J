package biz.dealnote.xmpp;

import android.app.Application;
import android.support.annotation.NonNull;

import com.squareup.picasso.Picasso;

import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.util.PicassoAvatarHandler;
import biz.dealnote.xmpp.util.PicassoInstance;
import biz.dealnote.xmpp.util.PicassoLocalPhotosHandler;

/**
 * Created by ruslan.kolbasa on 31.10.2016.
 * phoenix_for_xmpp
 */
public class App extends Application {

    private static App sInstanse;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstanse = this;

        Picasso picasso = new Picasso.Builder(this)
                .addRequestHandler(new PicassoLocalPhotosHandler(this))
                .addRequestHandler(new PicassoAvatarHandler(Repositories.getInstance().getContactsRepository()))
                .build();

        PicassoInstance.init(picasso);
    }

    @NonNull
    public static App getInstance(){
        if(sInstanse == null){
            throw new IllegalStateException("App instance is null!!! WTF???");
        }

        return sInstanse;
    }
}