package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Presence;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.AppRoster;
import biz.dealnote.xmpp.exception.CustomAppException;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

public class SendPresenseOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, InterruptedException {
        int accountId = request.getInt(Extra.ACCOUNT_ID);

        String from = request.getString(Extra.FROM);
        String to = request.getString(Extra.TO);
        int type = request.getInt(Extra.TYPE);

        Log.d(TAG, "SendPresenseOperation, accountId: " + accountId + ", from: " + from + ", to: " + to + ", type: " + type);

        AbstractXMPPConnection connection = assertConnectionFor(xmppContext, accountId);

        Presence.Type apiType = AppRoster.getApiPresenseTypeFrom(type);
        if (apiType == null) {
            throw new CustomAppException("Unknown presence type");
        }

        Presence presence = new Presence(apiType);
        if (!TextUtils.isEmpty(to)) {
            presence.setTo(to);
        }

        presence.setFrom(from);
        connection.sendStanza(presence);

        return null;
    }
}
