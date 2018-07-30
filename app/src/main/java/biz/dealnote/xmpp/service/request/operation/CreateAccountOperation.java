package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jxmpp.jid.parts.Localpart;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.exception.Codes;
import biz.dealnote.xmpp.exception.CustomAppException;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;
import de.duenndns.ssl.MemorizingTrustManager;

public class CreateAccountOperation extends AbsXmppOperation {

    private static final String TAG = CreateAccountOperation.class.getSimpleName();
    private static final int TIMEOUT = 30 * 1000;

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException {
        String host = request.getString(Extra.HOST);
        int port = request.getInt(Extra.PORT);
        String login = request.getString(Extra.LOGIN);
        String password = request.getString(Extra.PASSWORD);

        SSLContext sc = null;
        MemorizingTrustManager mtm;

        try {
            sc = SSLContext.getInstance("TLS");
            mtm = new MemorizingTrustManager(context);
            sc.init(null, new X509TrustManager[]{mtm}, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
                //.setServiceName(host)
                .setPort(port)
                .setConnectTimeout(TIMEOUT)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .setHost(host)
                .setCustomSSLContext(sc)
                //.setHostnameVerifier(mtm.wrapHostnameVerifier(new StrictHostnameVerifier()))
                .build();

        XMPPTCPConnection connection = new XMPPTCPConnection(conf);

        Log.d(TAG, "host: " + host + ", login: " + login + ", port: " + port + ", pass: " + password);

        try {
            connection.connect();
            AccountManager accountManager = AccountManager.getInstance(connection);
            accountManager.createAccount(Localpart.from(login), password);
        } catch (SmackException | IOException e) {
            e.printStackTrace();
            throw new CustomAppException(e.getMessage());
        } catch (XMPPException.XMPPErrorException e) {
            switch (e.getXMPPError().getCondition()) {
                case conflict:
                    throw new CustomAppException(context.getString(R.string.account_already_exist), Codes.ACCOUNT_ALREADY_EXIST);
                default:
                    throw new CustomAppException(e.getMessage());
            }
        } catch (XMPPException | InterruptedException e) {
            throw new CustomAppException(e.getMessage());
        } finally {
            connection.disconnect();
        }


        return null;
    }
}
