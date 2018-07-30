package biz.dealnote.xmpp.db.exception;

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
public class AlreadyExistException extends Exception {

    public AlreadyExistException(String message) {
        super(message);
    }
}
