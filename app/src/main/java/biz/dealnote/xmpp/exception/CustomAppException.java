package biz.dealnote.xmpp.exception;

import biz.dealnote.xmpp.service.request.exception.CustomRequestException;

public class CustomAppException extends CustomRequestException {

    public int code;

    public CustomAppException(String detailMessage) {
        this(detailMessage, Codes.UNDEFINED);
    }

    public CustomAppException(String detailMessage, int code) {
        super(detailMessage);
        this.code = code;
    }
}
