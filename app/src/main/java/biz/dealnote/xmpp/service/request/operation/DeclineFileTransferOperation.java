package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.Injection;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.model.MessageUpdate;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.TransferNotFoundException;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

public class DeclineFileTransferOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, InterruptedException {
        int messageId = request.getInt(Extra.MESSAGE_ID);

        try {
            Injection.INSTANCE.provideTransferer()
                    .cancel(messageId);
        } catch (TransferNotFoundException e) {
            e.printStackTrace();
        }

        Injection.INSTANCE.provideRepositories()
                .getMessages()
                .updateMessage(messageId, MessageUpdate.simpleStatusChange(AppMessage.STATUS_CANCELLED))
                .blockingAwait();

        return null;
    }

}
