package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.Messages;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
public class QuietMessageSendOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, XmppStringprepException, InterruptedException {
        Account account = (Account) request.getParcelable(Extra.ACCOUNT);
        String userJid = request.getString(Extra.JID);
        String messageText = request.getString(Extra.MESSAGE);
        int messageType = request.getInt(Extra.TYPE);

        assertConnectionFor(xmppContext, account.getId());

        Jid jid = JidCreate.bareFrom(userJid);
        Message message = new Message(jid, Messages.getTypeFrom(messageType));
        message.setBody(messageText);

        XMPPConnection connection = xmppContext.getConnectionManager().findConnectionFor(account.getId());
        connection.sendStanza(message);

        return null;
    }
}
