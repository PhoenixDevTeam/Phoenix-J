package biz.dealnote.xmpp.db.impl;

import android.content.ContentResolver;
import android.content.Context;
import android.support.annotation.NonNull;

import biz.dealnote.xmpp.db.Storages;

/**
 * Created by ruslan.kolbasa on 01.11.2016.
 * phoenix_for_xmpp
 */
public class AbsRepository {

    static final String SQL_FALSE = "0";

    private final Storages mStorages;

    public AbsRepository(Storages storages) {
        this.mStorages = storages;
    }

    @NonNull
    Context getContext(){
        return mStorages.getApplicationContext();
    }

    @NonNull
    Storages getRepositories(){
        return mStorages;
    }

    ContentResolver getContentResolver(){
        return mStorages.getContentResolver();
    }

}
