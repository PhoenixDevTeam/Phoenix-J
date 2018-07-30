package biz.dealnote.xmpp.security;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by admin on 08.11.2016.
 * phoenix-for-xmpp
 */
@IntDef({OtrState.PLAINTEXT, OtrState.ENCRYPTED, OtrState.FINISHED})
@Retention(RetentionPolicy.SOURCE)
public @interface OtrState {
    /**
     * This state indicates that outgoing messages are sent without encryption.
     * This is the state that is used before an OTR conversation is initiated. This
     * is the initial state, and the only way to subsequently enter this state is
     * for the user to explicitly request to do so via some UI operation.
     */
    int PLAINTEXT = 1;

    /**
     * This state indicates that outgoing messages are sent wasEncrypted. This is
     * the state that is used during an OTR conversation. The only way to enter
     * this state is for the authentication state machine to successfully
     * complete.
     */
    int ENCRYPTED = 2;

    /**
     * This state indicates that outgoing messages are not delivered at all.
     * This state is entered only when the other party indicates he has terminated
     * his side of the OTR conversation.
     */
    int FINISHED = 3;
}
