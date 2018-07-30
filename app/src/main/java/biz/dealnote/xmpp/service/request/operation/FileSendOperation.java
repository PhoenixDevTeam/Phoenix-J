package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.Injection;
import biz.dealnote.xmpp.db.AppRoster;
import biz.dealnote.xmpp.exception.CustomAppException;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

public class FileSendOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException {
        int accountId = request.getInt(Extra.ACCOUNT_ID);
        int messageId = request.getInt(Extra.MESSAGE_ID);
        String to = request.getString(Extra.TO);

        Uri uri = (Uri) request.getParcelable(Extra.URI);
        String fileName = request.getString(Extra.FILENAME);
        String type = request.getString(Extra.TYPE);

        String resource = AppRoster.findRosterResource(context, to);

        AbstractXMPPConnection connection = assertConnectionFor(xmppContext, accountId);
        FileTransferManager manager = FileTransferManager.getInstanceFor(connection);

        try {
            EntityFullJid entityFullJid = JidCreate.entityFullFrom(to + "/" + resource);

            OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer(entityFullJid); //java.lang.IllegalArgumentException: The provided user id was not a full JID (i.e. with resource part)

            Injection.INSTANCE.provideTransferer()
                    .createOutgoingTransfer(messageId, transfer, uri, fileName, type);

            //FileTransferHelper fth = xmppContext.getFileTransferHelper();
            //fth.putOutgoingFileTransfer(messageId, outgoingFileTransfer, uri, fileName, type);
        } catch (Exception e) {
            throw new CustomAppException(e.getMessage());
        }

        return null;
    }

}
