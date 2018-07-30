package biz.dealnote.xmpp.exception;

public class AccountAlreadyExistException extends Exception {

    public AccountAlreadyExistException(String detailMessage) {
        super(detailMessage);
    }
}
