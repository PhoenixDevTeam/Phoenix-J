package biz.dealnote.xmpp.db.impl;

import android.content.ContentResolver;
import android.content.Context;
import android.support.annotation.NonNull;

import biz.dealnote.xmpp.db.Repositories;

/**
 * Created by ruslan.kolbasa on 01.11.2016.
 * phoenix_for_xmpp
 */
public class AbsRepository {

    static final String SQL_FALSE = "0";

    private final Repositories mRepositories;

    public AbsRepository(Repositories repositories) {
        this.mRepositories = repositories;
    }

    @NonNull
    Context getContext(){
        return mRepositories.getApplicationContext();
    }

    @NonNull
    Repositories getRepositories(){
        return mRepositories;
    }

    ContentResolver getContentResolver(){
        return mRepositories.getContentResolver();
    }

}
