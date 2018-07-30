package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.DomainBareJid;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.service.IConnectionManager;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.util.Logger;

public class ConnectToAccountsOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) {

        IConnectionManager connectionManager = xmppContext.getConnectionManager();
        List<Account> accounts = Repositories.getInstance()
                .getAccountsRepository()
                .getAllActive()
                .blockingGet();

        ArrayList<Account> success = new ArrayList<>();
        for (Account account : accounts) {
            AbstractXMPPConnection connection = connectionManager.findConnectionFor(account.getId());

            try {
                if (connection == null) {
                    connection = connectionManager.registerConnectionFor(account);
                }

                if (!connection.isConnected()) {
                    connection.connect();

                    if (!connection.isAuthenticated()) {
                        connection.login();
                    }

                    success.add(account);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(connection);
            try {
                List<DomainBareJid> rooms = multiUserChatManager.getXMPPServiceDomains();
                for(DomainBareJid m : rooms){
                    Logger.d("ROOM", m.toString());
                }
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Extra.RESULT_LIST, success);
        return bundle;
    }
}
