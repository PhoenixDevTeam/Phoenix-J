package biz.dealnote.xmpp.db.columns;

import android.provider.BaseColumns;

public class AccountsColumns implements BaseColumns {

    public static final String TABLENAME = "accounts";

    public static final String LOGIN = "login";
    public static final String PASSWORD = "password";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String DISABLE = "disable";
    public static final String PUBLIC_KEY = "public_key";
    public static final String PRIVATE_KEY = "private_key";

    public static final String FULL_ID = TABLENAME + "." + _ID;
    public static final String FULL_LOGIN = TABLENAME + "." + LOGIN;
    public static final String FULL_PASSWORD = TABLENAME + "." + PASSWORD;
    public static final String FULL_HOST = TABLENAME + "." + HOST;
    public static final String FULL_PORT = TABLENAME + "." + PORT;
    public static final String FULL_DISABLE = TABLENAME + "." + DISABLE;
    public static final String FULL_PUBLIC_KEY = TABLENAME + "." + PUBLIC_KEY;
    public static final String FULL_PRIVATE_KEY = TABLENAME + "." + PRIVATE_KEY;

    private AccountsColumns() {
    }
}
