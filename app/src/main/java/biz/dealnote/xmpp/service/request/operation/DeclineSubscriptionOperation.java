package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.MessageUpdate;
import biz.dealnote.xmpp.model.Msg;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;
import biz.dealnote.xmpp.util.Utils;

public class DeclineSubscriptionOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, InterruptedException {
        Account account = (Account) request.getParcelable(Extra.ACCOUNT);
        String jid = Utils.getBareJid(request.getString(Extra.JID));
        int mid = request.getInt(Extra.MESSAGE_ID);

        AbstractXMPPConnection connection = assertConnectionFor(xmppContext, account.id);

        Presence unsubscribed = new Presence(Presence.Type.unsubscribed);
        unsubscribed.setFrom(account.buildBareJid());
        unsubscribed.setTo(jid);

        connection.sendStanza(unsubscribed);

        Repositories.getInstance()
                .getMessages()
                .updateMessage(mid, MessageUpdate.simpleStatusChange(Msg.STATUS_DECLINED))
                .blockingAwait();
        return null;
    }
}
