package biz.dealnote.xmpp;

import android.app.Application;
import android.support.annotation.NonNull;

import com.squareup.picasso.Picasso;

import biz.dealnote.xmpp.db.Storages;
import biz.dealnote.xmpp.model.Msg;
import biz.dealnote.xmpp.util.PicassoAvatarHandler;
import biz.dealnote.xmpp.util.PicassoInstance;
import biz.dealnote.xmpp.util.PicassoLocalPhotosHandler;
import biz.dealnote.xmpp.util.RxUtils;

/**
 * Created by ruslan.kolbasa on 31.10.2016.
 * phoenix_for_xmpp
 */
public class App extends Application {

    private static App sInstanse;

    @NonNull
    public static App getInstance() {
        if (sInstanse == null) {
            throw new IllegalStateException("App instance is null!!! WTF???");
        }

        return sInstanse;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstanse = this;

        Picasso picasso = new Picasso.Builder(this)
                .addRequestHandler(new PicassoLocalPhotosHandler(this))
                .addRequestHandler(new PicassoAvatarHandler(Storages.getINSTANCE().getUsers()))
                .build();

        PicassoInstance.init(picasso);

        Storages.getINSTANCE().getMessages()
                .updateStatus(Msg.STATUS_SENDING, Msg.STATUS_ERROR)
                .compose(RxUtils.INSTANCE.applyCompletableIOToMainSchedulers())
                .subscribe(RxUtils.INSTANCE.dummy(), RxUtils.INSTANCE.ignore());
    }
}