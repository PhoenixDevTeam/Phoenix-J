package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.db.interfaces.IAccountsRepository;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.service.IOldConnectionManager;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

public class AccountDeleteOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException {
        int accountId = request.getInt(Extra.ACCOUNT_ID);

        IOldConnectionManager connectionManager = xmppContext.getConnectionManager();
        AbstractXMPPConnection connection = connectionManager.findConnectionFor(accountId);

        if (connection != null) {
            connectionManager.unregisterFor(accountId);

            ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(connection);
            reconnectionManager.disableAutomaticReconnection();

            if (connection.isConnected()) {
                connection.disconnect();
            }
        }

        IAccountsRepository repository = Repositories.getInstance().getAccountsRepository();

        Account account = repository
                .findById(accountId)
                .blockingGet();

        if (account != null) {
            repository.deleteById(accountId)
                    .blockingAwait();
        }

        return buildSimpleSuccessResult(true);
    }
}
