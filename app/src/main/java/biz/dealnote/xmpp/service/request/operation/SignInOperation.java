package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import biz.dealnote.xmpp.Constants;
import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.Accounts;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.exception.AccountAlreadyExistException;
import biz.dealnote.xmpp.exception.CustomAppException;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.User;
import biz.dealnote.xmpp.service.IConnectionManager;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.exception.ConnectionAlreadyRegisteredException;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

public class SignInOperation extends AbsXmppOperation {

    private static final String TAG = SignInOperation.class.getSimpleName();

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, IOException, InterruptedException, ConnectionAlreadyRegisteredException {
        String login = request.getString(Extra.LOGIN);
        String password = request.getString(Extra.PASSWORD);
        String host = request.getString(Extra.HOST);
        int port = request.getInt(Extra.PORT);

        Account account = Accounts.findByLogin(context, login, host, port);

        if (account != null && !account.disabled) {
            throw new CustomAppException("Account already exist. Login cancelled");
        }

        if (account == null) {
            try {
                account = Accounts.put(context, login, password, host, port, true, generateKeyPair());
            } catch (AccountAlreadyExistException e) {
                throw new CustomAppException("Account already exist. Login cancelled");
            } catch (NoSuchAlgorithmException e) {
                throw new CustomAppException("Unable to generate public/private keys: " + e.getMessage());
            }
        }

        IConnectionManager connectionManager = xmppContext.getConnectionManager();
        AbstractXMPPConnection connection = connectionManager.registerConnectionFor(account);

        try {
            connection.connect();

            Log.d(TAG, "connect successfully");

            connection.login(login, password, Resourcepart.from(Constants.APP_RESOURCE));
            Log.d(TAG, "login successfully");

            VCard myVCard = findMyVCard(connection);
            if (myVCard == null) {
                myVCard = new VCard();
            }

            Repositories.getInstance()
                    .getUsersStorage()
                    .upsert(account.buildBareJid(), myVCard)
                    .blockingGet();

            User user = Repositories.getInstance()
                    .getUsersStorage()
                    .findByJid(account.buildBareJid())
                    .blockingGet()
                    .get();

            Bundle bundle = new Bundle();
            bundle.putParcelable(Extra.ACCOUNT, account);
            bundle.putParcelable(Extra.CONTACT, user);
            return bundle;
        } catch (XMPPException | IOException | SmackException e) {
            e.printStackTrace();

            connectionManager.unregisterFor(account.getId());
            connection.disconnect();

            throw new CustomAppException(e.getMessage());
        }
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance("DSA").genKeyPair();
    }

    private VCard findMyVCard(XMPPConnection connection) {
        VCardManager manager = VCardManager.getInstanceFor(connection);
        try {
            return manager.loadVCard();
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ClassCastException e){
            e.printStackTrace();
            return null;
        }
    }
}
