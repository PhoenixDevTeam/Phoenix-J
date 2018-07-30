package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.iqregister.AccountManager;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

public class ChangePasswordOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request)
            throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException,
            XMPPException.XMPPErrorException, SmackException.NotLoggedInException, InterruptedException {

        int accountId = request.getInt(Extra.ACCOUNT_ID);
        String password = request.getString(Extra.PASSWORD);

        AbstractXMPPConnection connection = assertConnectionFor(xmppContext, accountId);
        AccountManager accountManager = AccountManager.getInstance(connection);
        accountManager.changePassword(password);

        Repositories.Companion.getInstance()
                .getAccountsRepository()
                .changePassword(accountId, password)
                .blockingAwait();

        Bundle bundle = new Bundle();
        bundle.putString(Extra.PASSWORD, password);
        return bundle;
    }

}
