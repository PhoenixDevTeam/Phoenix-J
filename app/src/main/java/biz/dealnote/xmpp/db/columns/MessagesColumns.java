package biz.dealnote.xmpp.db.columns;

import android.provider.BaseColumns;

public class MessagesColumns implements BaseColumns {

    public static final String TABLENAME = "messages";
    public static final String ACCOUNT_ID = "account_id";
    public static final String CHAT_ID = "chat_id";
    public static final String DESTINATION = "destination";
    public static final String SENDER_ID = "sender_id";
    public static final String SENDER_JID = "sender_jid";
    public static final String UNIQUE_SERVICE_ID = "stanza_id";
    public static final String TYPE = "type";
    public static final String BODY = "body";
    public static final String STATUS = "status";
    public static final String OUT = "out";
    public static final String READ_STATE = "read_state";
    public static final String DATE = "date";
    public static final String WAS_ENCRYPTED = "encrypted";

    public static final String ATTACHED_FILE_PATH = "attached_file_path";
    public static final String ATTACHED_FILE_NAME = "attached_file_name";
    public static final String ATTACHED_FILE_SIZE = "attached_file_size";
    public static final String ATTACHED_FILE_MIME = "attached_file_mime";
    public static final String ATTACHED_FILE_DESCRIPTION = "attached_file_description";

    public static final String FULL_ID = TABLENAME + "." + _ID;
    public static final String FULL_ACCOUNT_ID = TABLENAME + "." + ACCOUNT_ID;
    public static final String FULL_CHAT_ID = TABLENAME + "." + CHAT_ID;
    public static final String FULL_DESTINATION = TABLENAME + "." + DESTINATION;
    public static final String FULL_SENDER_ID = TABLENAME + "." + SENDER_ID;
    public static final String FULL_SENDER_JID = TABLENAME + "." + SENDER_JID;
    public static final String FULL_STANZAID = TABLENAME + "." + UNIQUE_SERVICE_ID;
    public static final String FULL_TYPE = TABLENAME + "." + TYPE;
    public static final String FULL_BODY = TABLENAME + "." + BODY;
    public static final String FULL_STATUS = TABLENAME + "." + STATUS;
    public static final String FULL_OUT = TABLENAME + "." + OUT;
    public static final String FULL_READ_STATE = TABLENAME + "." + READ_STATE;
    public static final String FULL_DATE = TABLENAME + "." + DATE;
    public static final String FULL_WAS_ENCRYPTED = TABLENAME + "." + WAS_ENCRYPTED;
    public static final String FULL_ATTACHED_FILE_PATH = TABLENAME + "." + ATTACHED_FILE_PATH;
    public static final String FULL_ATTACHED_FILE_NAME = TABLENAME + "." + ATTACHED_FILE_NAME;
    public static final String FULL_ATTACHED_FILE_SIZE = TABLENAME + "." + ATTACHED_FILE_SIZE;
    public static final String FULL_ATTACHED_FILE_MIME = TABLENAME + "." + ATTACHED_FILE_MIME;
    public static final String FULL_ATTACHED_FILE_DESCRIPTION = TABLENAME + "." + ATTACHED_FILE_DESCRIPTION;

    private MessagesColumns() {
    }
}
