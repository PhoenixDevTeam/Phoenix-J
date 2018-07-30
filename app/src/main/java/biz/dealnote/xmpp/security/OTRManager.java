package biz.dealnote.xmpp.security;

import android.content.Context;
import android.support.annotation.NonNull;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.io.SerializationUtils;
import net.java.otr4j.session.FragmenterInstructions;
import net.java.otr4j.session.InstanceTag;
import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionImpl;
import net.java.otr4j.session.SessionStatus;

import java.security.KeyPair;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestAdapter;
import biz.dealnote.xmpp.service.request.RequestFactory;
import biz.dealnote.xmpp.service.request.XmppRequestManager;
import biz.dealnote.xmpp.util.Logger;
import biz.dealnote.xmpp.util.Utils;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import static biz.dealnote.xmpp.util.Utils.isEmpty;

public class OTRManager extends OtrCryptoEngineImpl implements OtrEngineHost, OtrEngineListener, IOtrManager {

    private static final String TAG = OTRManager.class.getSimpleName();
    private static final String XMPP = "xmpp";

    private final Context context;
    private final Map<Account, Map<String, Session>> sessionsMap;
    private final PublishSubject<ISessionStateChangeEvent> stateChangesPublisher;

    public OTRManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.stateChangesPublisher = PublishSubject.create();
        this.sessionsMap = Collections.synchronizedMap(new HashMap<>());
    }

    @OtrState
    public int getSessionState(int accountId, String destination) {
        Session session = findSessionBy(accountId, destination);
        return session != null ? getAppStatusFor(session.getSessionStatus()) : OtrState.PLAINTEXT;
    }

    @OtrState
    private static int getAppStatusFor(SessionStatus sessionStatus) {
        switch (sessionStatus){
            case PLAINTEXT:
                return OtrState.PLAINTEXT;
            case ENCRYPTED:
                return OtrState.ENCRYPTED;
            case FINISHED:
                return OtrState.FINISHED;
            default:
                return OtrState.PLAINTEXT;
        }
    }

    public void endSession(Account account, String bareJid) throws OtrException {
        getSessionCreateIfNoExist(account, bareJid).endSession();
    }

    @Override
    public String[] encryptMessageBody(Account account, String bareJid, String body) throws OtrException {
        Session session = getSessionCreateIfNoExist(account, Utils.getBareJid(bareJid));
        if(session.getSessionStatus() != SessionStatus.ENCRYPTED){
            return new String[]{body};
        }

        return session.transformSending(body);
    }

    @Override
    public String handleInputMessage(Account account, String from, String body) {
        String bareJid = Utils.getBareJid(from);

        if (isEmpty(body)) {
            return body;
        }

        Session session = getSessionCreateIfNoExist(account, bareJid);

        try {
            String encryptedBody = session.transformReceiving(body);
            Logger.d(TAG, "procesInputMessage, encryptedBody: " + encryptedBody);

            if (isEmpty(encryptedBody)) {
                return null;
            }

            return encryptedBody;
        } catch (OtrException e) {
            Logger.e(TAG, "procesInputMessage, e: " + e);
            e.printStackTrace();
            return body;
        }
    }

    @Override
    public Observable<ISessionStateChangeEvent> observeStateChanges() {
        return stateChangesPublisher;
    }

    public void refreshSession(Account account, String bareJid) throws OtrException {
        getSessionCreateIfNoExist(account, bareJid).refreshSession();
    }

    public void startSession(Account account, String bareJid) throws OtrException {
        getSessionCreateIfNoExist(account, bareJid).startSession();
    }

    @NonNull
    private Session getSessionCreateIfNoExist(Account account, String bareJid) {
        Map<String, Session> accountSessions = sessionsMap.get(account);

        Session session = accountSessions == null ? null : accountSessions.get(bareJid);
        if (session != null) {
            Logger.d(TAG, "getSessionCreateIfNoExist, session already exist");
            return session;
        }

        session = new SessionImpl(new SessionID(account.buildBareJid(), bareJid, XMPP), this);
        session.addOtrEngineListener(this);

        prepareAccountSessions(account).put(bareJid, session);
        return session;
    }

    private Map<String, Session> prepareAccountSessions(Account account) {
        Map<String, Session> sessionMap = sessionsMap.get(account);
        if (sessionMap == null) {
            sessionMap = new HashMap<>();
            sessionsMap.put(account, sessionMap);
        }

        return sessionMap;
    }

    private Session findSessionBy(int accountId, String destination) {
        for (Map.Entry<Account, Map<String, Session>> entry : sessionsMap.entrySet()) {
            Account key = entry.getKey();
            if (key.id == accountId) {
                Map<String, Session> value = entry.getValue();
                return value.get(destination);
            }
        }

        return null;
    }

    @Override
    public void injectMessage(SessionID sessionID, String msg) throws OtrException {
        Logger.d(TAG, "injectMessage, sessionID: " + sessionID + ", msg: " + msg);

        Account account = findAccountByJid(sessionID.getAccountID());
        if (account == null) {
            throw new IllegalStateException("Account is not registered, jid: " + sessionID.getAccountID());
        }

        sendChatMessage(account, sessionID.getUserID(), msg);
    }

    private void sendChatMessage(@NonNull Account account, @NonNull String userJid, String msg) {
        Request request = RequestFactory.getMessageQuietSendRequest(account, userJid, msg, AppMessage.TYPE_CHAT);
        XmppRequestManager.from(context).execute(request, new RequestAdapter());
    }

    @Override
    public void unreadableMessageReceived(SessionID sessionID) throws OtrException {
        Logger.d(TAG, "unreadableMessageReceived, sessionID: " + sessionID);
    }

    @Override
    public void unencryptedMessageReceived(SessionID sessionID, String msg) throws OtrException {
        Logger.d(TAG, "unencryptedMessageReceived, sessionID: " + sessionID + ", msg: " + msg);
    }

    @Override
    public void showError(SessionID sessionID, String error) throws OtrException {
        Logger.d(TAG, "showError, sessionID: " + sessionID + ", error: " + error);
    }

    @Override
    public void smpError(SessionID sessionID, int tlvType, boolean cheated) throws OtrException {
        Logger.d(TAG, "smpError, sessionID: " + sessionID + ", tlvType: " + tlvType + ", cheated: " + cheated);
    }

    @Override
    public void smpAborted(SessionID sessionID) throws OtrException {
        Logger.d(TAG, "smpAborted, sessionID: " + sessionID);
    }

    @Override
    public void finishedSessionMessage(SessionID sessionID, String msgText) throws OtrException {
        Logger.d(TAG, "finishedSessionMessage, sessionID: " + sessionID + ", msgText: " + msgText);
    }

    @Override
    public void requireEncryptedMessage(SessionID sessionID, String msgText) throws OtrException {
        Logger.d(TAG, "requireEncryptedMessage, sessionID: " + sessionID + ", msgText: " + msgText);
    }

    @Override
    public OtrPolicy getSessionPolicy(SessionID sessionID) {
        Logger.d(TAG, "getSessionPolicy, sessionID: " + sessionID);

        OtrPolicyImpl policy = new OtrPolicyImpl();
        policy.setAllowV1(false);
        policy.setAllowV2(true);
        policy.setAllowV3(true);

        // все, кроме V1
        //return new OtrPolicyImpl(OtrPolicy.OTRL_POLICY_ALWAYS & ~OtrPolicy.ALLOW_V1);
        return policy;
    }

    @Override
    public FragmenterInstructions getFragmenterInstructions(SessionID sessionID) {
        Logger.d(TAG, "getFragmenterInstructions, sessionID: " + sessionID);

        // не фрагментировать сообщения
        return null;
    }

    @Override
    public KeyPair getLocalKeyPair(SessionID sessionID) throws OtrException {
        Logger.d(TAG, "getLocalKeyPair, sessionID: " + sessionID);

        Account account = findAccountByJid(sessionID.getAccountID());
        if (account == null) {
            throw new IllegalStateException("Account is not registered, jid: " + sessionID.getAccountID());
        }

        return account.keyPair;
    }

    private Account findAccountByJid(String accountJid) {
        for (Map.Entry<Account, ?> entry : sessionsMap.entrySet()) {
            Account key = entry.getKey();
            if (key.buildBareJid().equals(accountJid)) {
                return key;
            }
        }

        return null;
    }

    @Override
    public byte[] getLocalFingerprintRaw(SessionID sessionID) {
        Account account = findAccountByJid(sessionID.getAccountID());
        if (account == null) {
            throw new IllegalStateException("Account is not registered, jid: " + sessionID.getAccountID());
        }

        try {
            String fingerprint = getFingerprint(account.keyPair.getPublic());
            Logger.d(TAG, "getLocalFingerprintRaw, fingerprint: " + fingerprint);
            return SerializationUtils.hexStringToByteArray(fingerprint);
        } catch (OtrCryptoException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void askForSecret(SessionID sessionID, InstanceTag receiverTag, String question) {
        Logger.d(TAG, "askForSecret, sessionID: " + sessionID + ", receiverTag: " + receiverTag + ", question: " + question);
    }

    @Override
    public void verify(SessionID sessionID, String fingerprint, boolean approved) {
        Logger.d(TAG, "verify, sessionID: " + sessionID + ", fingerprint: " + fingerprint + ", approved: " + approved);
    }

    @Override
    public void unverify(SessionID sessionID, String fingerprint) {
        Logger.d(TAG, "unverify, sessionID: " + sessionID + ", fingerprint: " + fingerprint);
    }

    @Override
    public String getReplyForUnreadableMessage(SessionID sessionID) {
        Logger.d(TAG, "getReplyForUnreadableMessage, sessionID: " + sessionID);
        return null;
    }

    @Override
    public String getFallbackMessage(SessionID sessionID) {
        Logger.d(TAG, "getFallbackMessage, sessionID: " + sessionID);
        return context.getString(R.string.otr_fallback_message);
    }

    @Override
    public void messageFromAnotherInstanceReceived(SessionID sessionID) {
        Logger.d(TAG, "messageFromAnotherInstanceReceived, sessionID: " + sessionID);
    }

    @Override
    public void sessionStatusChanged(SessionID sessionID) {
        Logger.d(TAG, "sessionStatusChanged, sessionID: " + sessionID);

        Account account = null;
        String destination = null;
        Session session = null;

        for (Map.Entry<Account, Map<String, Session>> entry : sessionsMap.entrySet()) {
            Account key = entry.getKey();
            Map<String, Session> value = entry.getValue();

            for (Map.Entry<String, Session> nestedEntry : value.entrySet()) {
                String nestedKey = nestedEntry.getKey();
                Session nestedValue = nestedEntry.getValue();

                if (nestedValue.getSessionID().equals(sessionID)) {
                    account = key;
                    destination = nestedKey;
                    session = nestedValue;
                }
            }
        }

        if(session == null){
            throw new IllegalStateException("Session with id " + sessionID + " does not exist");
        }

        ISessionStateChangeEvent event = new SessionStateChangeEvent(
                account.getId(),
                destination,
                getAppStatusFor(session.getSessionStatus())
        );

        stateChangesPublisher.onNext(event);
    }

    @Override
    public void multipleInstancesDetected(SessionID sessionID) {
        Logger.d(TAG, "multipleInstancesDetected, sessionID: " + sessionID);
    }

    @Override
    public void outgoingSessionChanged(SessionID sessionID) {
        Logger.d(TAG, "outgoingSessionChanged, sessionID: " + sessionID);
    }

    private static final class SessionStateChangeEvent implements ISessionStateChangeEvent {

        final int accountId;

        final String bareJid;

        @OtrState
        final int state;

        private SessionStateChangeEvent(int accountId, String bareJid, int state) {
            this.accountId = accountId;
            this.bareJid = bareJid;
            this.state = state;
        }

        @Override
        public int getAccountId() {
            return accountId;
        }

        @Override
        public String getBareJid() {
            return bareJid;
        }

        @Override
        public int getState() {
            return state;
        }
    }
}
