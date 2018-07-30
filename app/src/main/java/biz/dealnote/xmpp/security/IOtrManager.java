package biz.dealnote.xmpp.security;

import net.java.otr4j.OtrException;

import biz.dealnote.xmpp.model.Account;
import io.reactivex.Observable;

/**
 * Created by admin on 24.04.2017.
 * phoenix-for-xmpp
 */
public interface IOtrManager {

    @OtrState
    int getSessionState(int accountId, String bareJid);

    void startSession(Account account, String bareJid) throws OtrException;

    void refreshSession(Account account, String bareJid) throws OtrException;

    void endSession(Account account, String bareJid) throws OtrException;

    String[] encryptMessageBody(Account account, String bareJid, String body) throws OtrException;

    String handleInputMessage(Account account, String from, String text);

    Observable<ISessionStateChangeEvent> observeStateChanges();

    interface ISessionStateChangeEvent {

        int getAccountId();

        String getBareJid();

        @OtrState
        int getState();
    }
}
