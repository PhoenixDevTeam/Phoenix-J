package biz.dealnote.xmpp.service;

/**
 * Created by admin on 08.11.2016.
 * phoenix-for-xmpp
 */
public class ServiceNotConnectedException extends RuntimeException {

    public ServiceNotConnectedException(String message) {
        super(message);
    }
}
