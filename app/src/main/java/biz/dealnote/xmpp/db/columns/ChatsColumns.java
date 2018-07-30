package biz.dealnote.xmpp.db.columns;

import android.provider.BaseColumns;

public class ChatsColumns implements BaseColumns {

    public static final String TABLENAME = "chats";
    public static final String ACCOUNT_ID = "account_id";
    public static final String DESTINATION = "destination";
    public static final String IS_GROUP_CHAT = "is_group_chat";
    public static final String TITLE = "title"; // for group chat only
    public static final String UNREAD_COUNT = "unread_count";
    public static final String INTERLOCUTOR_ID = "interlocutor_id";
    public static final String HIDDEN = "hidden";
    public static final String LAST_MESSAGE_TEXT = "lm_text";
    public static final String LAST_MESSAGE_TIME = "lm_time";
    public static final String LAST_MESSAGE_OUT = "lm_out";
    public static final String LAST_MESSAGE_TYPE = "lm_type";

    public static final String FULL_ID = TABLENAME + "." + _ID;
    public static final String FULL_ACCOUNT_ID = TABLENAME + "." + ACCOUNT_ID;
    public static final String FULL_DESTINATION = TABLENAME + "." + DESTINATION;
    public static final String FULL_IS_GROUP_CHAT = TABLENAME + "." + IS_GROUP_CHAT;
    public static final String FULL_TITLE = TABLENAME + "." + TITLE;
    public static final String FULL_UNREAD_COUNT = TABLENAME + "." + UNREAD_COUNT;
    public static final String FULL_INTERLOCUTOR_ID = TABLENAME + "." + INTERLOCUTOR_ID;
    public static final String FULL_HIDDEN = TABLENAME + "." + HIDDEN;
    public static final String FULL_LAST_MESSAGE_TEXT = TABLENAME + "." + LAST_MESSAGE_TEXT;
    public static final String FULL_LAST_MESSAGE_TIME = TABLENAME + "." + LAST_MESSAGE_TIME;
    public static final String FULL_LAST_MESSAGE_OUT = TABLENAME + "." + LAST_MESSAGE_OUT;
    public static final String FULL_LAST_MESSAGE_TYPE = TABLENAME + "." + LAST_MESSAGE_TYPE;

    public static final String FOREIGN_INTERLOCUTOR_JID = "interlocutor_jid";
    public static final String FOREIGN_INTERLOCUTOR_FIRST_NAME = "interlocutor_first_name";
    public static final String FOREIGN_INTERLOCUTOR_LAST_NAME = "interlocutor_last_name";
    public static final String FOREIGN_INTERLOCUTOR_PHOTO_HASH = "interlocutor_photo_hash";
    public static final String FOREIGN_INTERLOCUTOR_PHOTO = "interlocutor_photo";
    //public static final String FOREIGN_ACCOUNT_LOGIN = "account_login";
    //public static final String FOREIGN_ACCOUNT_PASSWORD = "account_password";
    //public static final String FOREIGN_ACCOUNT_HOST = "account_host";
    //public static final String FOREIGN_ACCOUNT_PORT = "account_port";
    //public static final String FOREIGN_ACCOUNT_DISABLE = "account_disable";
    //public static final String FOREIGN_ACCOUNT_PUBLIC_KEY = "account_public_key";
    //public static final String FOREIGN_ACCOUNT_PRIVATE_KEY = "account_private_key";

    private ChatsColumns() {
    }
}
