package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import net.java.otr4j.OtrException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.Injection;
import biz.dealnote.xmpp.exception.CustomAppException;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;
import biz.dealnote.xmpp.util.Utils;

public class StartOTRSessionOperation extends AbsXmppOperation {

    private static final String TAG = StartOTRSessionOperation.class.getSimpleName();

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException {
        Account account = (Account) request.getParcelable(Extra.ACCOUNT);
        String jid = Utils.getBareJid(request.getString(Extra.JID));

        Log.d(TAG, "account: " + account + ", jid: " + jid);

        try {
            Injection.INSTANCE.provideOtrManager().startSession(account, jid);
        } catch (OtrException e) {
            throw new CustomAppException(e.getMessage());
        }

        return null;
    }
}
