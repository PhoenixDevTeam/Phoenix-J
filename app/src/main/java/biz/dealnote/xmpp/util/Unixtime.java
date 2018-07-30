package biz.dealnote.xmpp.util;

public class Unixtime {

    public static long now() {
        return System.currentTimeMillis() / 1000;
    }

}
