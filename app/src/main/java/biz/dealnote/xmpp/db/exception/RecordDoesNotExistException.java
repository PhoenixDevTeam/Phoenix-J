package biz.dealnote.xmpp.db.exception;

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
public class RecordDoesNotExistException extends Exception {

    public RecordDoesNotExistException(String message) {
        super(message);
    }

    public RecordDoesNotExistException() {
    }
}
