package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.Injection;
import biz.dealnote.xmpp.db.Messages;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.exception.CustomAppException;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.model.MessageUpdate;
import biz.dealnote.xmpp.security.IOtrManager;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;

public class MessageSendOperation extends AbsXmppOperation {

    private static final String TAG = MessageSendOperation.class.getSimpleName();

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws Exception {
        AppMessage message = (AppMessage) request.getParcelable(Extra.MESSAGE);

        changeMessgeStatusImpl(context, message, AppMessage.STATUS_SENDING);

        try {
            AbstractXMPPConnection connection = assertConnectionFor(xmppContext, message.getAccountId());
            Account account = xmppContext.getConnectionManager().findAccountById(message.getAccountId());

            IOtrManager otrManager = Injection.INSTANCE.provideOtrManager();

            Log.d(TAG, "Try to send, body: " + message.getBody());
            String[] bodies = otrManager.encryptMessageBody(account, message.getDestination(), message.getBody());

            Jid jid = JidCreate.from(message.getDestination());

            Message forSend = new Message(jid, Messages.getTypeFrom(message.getType()));
            forSend.setBody(TextUtils.join("", bodies)); // Attempt to get length of null array
            forSend.setStanzaId(message.getStanzaId());
            connection.sendStanza(forSend);

            changeMessgeStatusImpl(context, message, AppMessage.STATUS_SENT);
        } catch (Exception e) {
            changeMessgeStatusImpl(context, message, AppMessage.STATUS_ERROR);
            throw new CustomAppException(e.getMessage());
        }

        return null;
    }

    private void changeMessgeStatusImpl(Context context, AppMessage message, int newStatus) {
        Repositories.getInstance()
                .getMessages()
                .updateMessage(message.getId(), MessageUpdate.simpleStatusChange(newStatus))
                .blockingAwait();
        message.setStatus(newStatus);
    }
}
