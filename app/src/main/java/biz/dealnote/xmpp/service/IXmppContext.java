package biz.dealnote.xmpp.service;

import android.support.annotation.NonNull;

import biz.dealnote.xmpp.security.IOtrManager;

/**
 * Created by admin on 06.11.2016.
 * phoenix-for-xmpp
 */
public interface IXmppContext {

    @NonNull
    IOldConnectionManager getConnectionManager();

    @NonNull
    IOtrManager getOtrManager();
}
