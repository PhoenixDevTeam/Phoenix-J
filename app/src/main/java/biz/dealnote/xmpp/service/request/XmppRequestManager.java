package biz.dealnote.xmpp.service.request;

import android.content.Context;

import biz.dealnote.xmpp.service.XmppService;

public final class XmppRequestManager extends AbsRequestManager {

    // Singleton management
    private static XmppRequestManager sInstance;

    private XmppRequestManager(Context context) {
        super(context.getApplicationContext(), XmppService.class);
    }

    public synchronized static XmppRequestManager from(Context context) {
        if (sInstance == null) {
            sInstance = new XmppRequestManager(context);
        }

        return sInstance;
    }
}