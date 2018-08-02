package biz.dealnote.xmpp.db.columns;

import android.provider.BaseColumns;

public class RosterColumns implements BaseColumns {

    public static final String TABLENAME = "roster";

    public static final String ACCOUNT_ID = "account_id";
    public static final String JID = "jid";
    public static final String RESOURCE = "resource";
    public static final String USER_ID = "contact_id";
    public static final String FLAGS = "flags";
    public static final String AVAILABLE_RECEIVE_MESSAGES = "available_receive_messages";
    public static final String IS_AWAY = "is_away";
    public static final String PRESENSE_MODE = "presence_mode";
    public static final String PRESENSE_TYPE = "presence_type";
    public static final String PRESENSE_STATUS = "presence_status";
    public static final String TYPE = "type";
    //public static final String STATUS = "status";
    public static final String NICK = "nick";
    public static final String PRIORITY = "priority";

    public static final String FULL_ID = TABLENAME + "." + _ID;
    public static final String FULL_ACCOUNT_ID = TABLENAME + "." + ACCOUNT_ID;
    public static final String FULL_JID = TABLENAME + "." + JID;
    public static final String FULL_RESOURCE = TABLENAME + "." + RESOURCE;
    public static final String FULL_CONTACT_ID = TABLENAME + "." + USER_ID;
    public static final String FULL_FLAGS = TABLENAME + "." + FLAGS;
    public static final String FULL_AVAILABLE_RECEIVE_MESSAGES = TABLENAME + "." + AVAILABLE_RECEIVE_MESSAGES;
    public static final String FULL_IS_AWAY = TABLENAME + "." + IS_AWAY;
    public static final String FULL_PRESENSE_MODE = TABLENAME + "." + PRESENSE_MODE;
    public static final String FULL_PRESENSE_TYPE = TABLENAME + "." + PRESENSE_TYPE;
    public static final String FULL_PRESENSE_STATUS = TABLENAME + "." + PRESENSE_STATUS;
    public static final String FULL_TYPE = TABLENAME + "." + TYPE;
    //public static final String FULL_STATUS = TABLENAME + "." + STATUS;
    public static final String FULL_NICK = TABLENAME + "." + NICK;
    public static final String FULL_PRIORITY = TABLENAME + "." + PRIORITY;

    public static final String FOREIGN_CONTACT_FIRST_NAME = "contact_first_name";
    public static final String FOREIGN_CONTACT_LAST_NAME = "contact_last_name";
    public static final String FOREIGN_CONTACT_MIDDLE_NAME = "contact_middle_name";
    public static final String FOREIGN_CONTACT_PREFIX = "contact_prefix";
    public static final String FOREIGN_CONTACT_SUFFIX = "contact_suffix";
    public static final String FOREIGN_CONTACT_EMAIL_HOME = "contact_email_home";
    public static final String FOREIGN_CONTACT_EMAIL_WORK = "contact_email_work";
    public static final String FOREIGN_CONTACT_ORGANIZATION = "contact_organization";
    public static final String FOREIGN_CONTACT_ORGANIZATION_UNIT = "contact_organization_unit";
    public static final String FOREIGN_CONTACT_PHOTO_MIME_TYPE = "contact_photo_mime_type";
    public static final String FOREIGN_CONTACT_PHOTO_HASH = "contact_photo_hash";
    public static final String FOREIGN_CONTACT_PHOTO = "contact_photo";
    public static final String FOREIGN_ACCOUNT_LOGIN = "account_login";
    public static final String FOREIGN_ACCOUNT_PASSWORD = "account_password";
    public static final String FOREIGN_ACCOUNT_DISABLE = "account_disable";
    public static final String FOREIGN_ACCOUNT_HOST = "account_host";
    public static final String FOREIGN_ACCOUNT_PORT = "account_port";
    public static final String FOREIGN_ACCOUNT_PUBLIC_KEY = "account_public_key";
    public static final String FOREIGN_ACCOUNT_PRIVATE_KEY = "account_private_key";

    private RosterColumns() {
    }
}
