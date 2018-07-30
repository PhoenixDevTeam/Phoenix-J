package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.exception.CustomAppException;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.AbsXmppOperationManager;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

public abstract class AbsXmppOperation implements AbsXmppOperationManager.Operation {

    protected static final String TAG = AbsXmppOperation.class.getSimpleName();

    protected static AbstractXMPPConnection assertConnectionFor(IXmppContext xmppContext, int accountId) throws CustomAppException {
        AbstractXMPPConnection connection = xmppContext.getConnectionManager().findConnectionFor(accountId);
        if (connection == null || !connection.isConnected()) {
            throw new CustomAppException("No available connection for account: " + accountId);
        }

        return connection;
    }

    protected static Bundle buildSimpleSuccessResult(boolean success) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(Extra.SUCCESS, success);
        return bundle;
    }

    @Override
    public final Bundle execute(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException {
        try {
            return executeRequest(context, xmppContext, request);
        } catch (SmackException.NotLoggedInException | XMPPException.XMPPErrorException | SmackException.NoResponseException | SmackException.NotConnectedException e) {
            e.printStackTrace();
            throw new CustomAppException(e.getMessage());
        } catch (CustomAppException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomAppException("Unchecked error, e: " + e.getMessage());
        }
    }

    public abstract Bundle executeRequest(@NonNull Context context,
                                          @NonNull IXmppContext xmppContext,
                                          @NonNull Request request) throws Exception;
}
