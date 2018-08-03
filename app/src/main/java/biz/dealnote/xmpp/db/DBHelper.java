package biz.dealnote.xmpp.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import biz.dealnote.xmpp.db.columns.AccountsColumns;
import biz.dealnote.xmpp.db.columns.ChatsColumns;
import biz.dealnote.xmpp.db.columns.UsersColumns;
import biz.dealnote.xmpp.db.columns.MessagesColumns;
import biz.dealnote.xmpp.db.columns.RosterColumns;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "xmpp";

    private static final int DATABASE_VERSION = 3;

    private static DBHelper mInstance = null;

    private DBHelper(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
    }

    public synchronized static DBHelper getInstance(final Context ctx) {
        if (mInstance == null) {
            mInstance = new DBHelper(ctx);
        }

        return mInstance;
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAccountsTable(db);
        createContactsTable(db);
        createChatsTable(db);
        createMessagesTable(db);
        createRostersEntriesTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private void createRostersEntriesTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE [" + RosterColumns.TABLENAME + "] ( " +
                " [" + RosterColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + RosterColumns.ACCOUNT_ID + "] INTEGER NOT NULL, " +
                " [" + RosterColumns.JID + "] TEXT NOT NULL, " +
                " [" + RosterColumns.RESOURCE + "] TEXT, " +
                " [" + RosterColumns.USER_ID + "] INTEGER NOT NULL, " +
                " [" + RosterColumns.FLAGS + "] INTEGER, " +
                " [" + RosterColumns.AVAILABLE_RECEIVE_MESSAGES + "] BOOLEAN, " +
                " [" + RosterColumns.IS_AWAY + "] BOOLEAN, " +
                " [" + RosterColumns.PRESENSE_MODE + "] INTEGER, " +
                " [" + RosterColumns.PRESENSE_TYPE + "] INTEGER, " +
                " [" + RosterColumns.PRESENSE_STATUS + "] TEXT, " +
                " [" + RosterColumns.TYPE + "] INTEGER, " +
                //" [" + RosterColumns.STATUS + "] INTEGER, " +
                " [" + RosterColumns.NICK + "] TEXT, " +
                " [" + RosterColumns.PRIORITY + "] INTEGER, " +
                " CONSTRAINT [] UNIQUE ([" + RosterColumns.ACCOUNT_ID + "], [" + RosterColumns.USER_ID + "]) ON CONFLICT REPLACE " +
                " FOREIGN KEY([" + RosterColumns.USER_ID + "]) REFERENCES " + UsersColumns.TABLENAME + "([" + UsersColumns._ID + "]) " +
                " FOREIGN KEY([" + RosterColumns.ACCOUNT_ID + "]) REFERENCES " + AccountsColumns.TABLENAME + "([" + AccountsColumns._ID + "]) ON DELETE CASCADE ON UPDATE CASCADE);";
        db.execSQL(sql);
    }

    private void createAccountsTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE [" + AccountsColumns.TABLENAME + "] ( " +
                " [" + AccountsColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + AccountsColumns.LOGIN + "] TEXT, " +
                " [" + AccountsColumns.PASSWORD + "] TEXT, " +
                " [" + AccountsColumns.HOST + "] TEXT, " +
                " [" + AccountsColumns.PORT + "] INTEGER NOT NULL DEFAULT 5222, " +
                " [" + AccountsColumns.DISABLE + "] BOOLEAN, " +
                " [" + AccountsColumns.PUBLIC_KEY + "] BLOB, " +
                " [" + AccountsColumns.PRIVATE_KEY + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + AccountsColumns.LOGIN + "], [" + AccountsColumns.HOST + "], [" + AccountsColumns.PORT + "]) ON CONFLICT FAIL);";
        db.execSQL(sql);
    }

    private void createMessagesTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE [" + MessagesColumns.TABLENAME + "] ( " +
                " [" + MessagesColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + MessagesColumns.ACCOUNT_ID + "] INTEGER , " +
                " [" + MessagesColumns.CHAT_ID + "] INTEGER , " +
                " [" + MessagesColumns.DESTINATION + "] TEXT , " +
                " [" + MessagesColumns.SENDER_ID + "] INTEGER, " +
                " [" + MessagesColumns.SENDER_JID + "] TEXT, " +
                " [" + MessagesColumns.UNIQUE_SERVICE_ID + "] TEXT, " +
                " [" + MessagesColumns.TYPE + "] INTEGER, " +
                " [" + MessagesColumns.BODY + "] TEXT, " +
                " [" + MessagesColumns.STATUS + "] INTEGER, " +
                " [" + MessagesColumns.OUT + "] BOOLEAN, " +
                " [" + MessagesColumns.READ_STATE + "] BOOLEAN, " +
                " [" + MessagesColumns.DATE + "] BIGINT, " +
                " [" + MessagesColumns.WAS_ENCRYPTED + "] BOOLEAN, " +
                " [" + MessagesColumns.ATTACHED_FILE_PATH + "] TEXT, " +
                " [" + MessagesColumns.ATTACHED_FILE_NAME + "] TEXT, " +
                " [" + MessagesColumns.ATTACHED_FILE_SIZE + "] BIGINT, " +
                " [" + MessagesColumns.ATTACHED_FILE_MIME + "] TEXT, " +
                " [" + MessagesColumns.ATTACHED_FILE_DESCRIPTION + "] TEXT, " +
                " FOREIGN KEY([" + MessagesColumns.CHAT_ID + "]) " +
                " REFERENCES " + ChatsColumns.TABLENAME + "([" + ChatsColumns._ID + "]) ON DELETE CASCADE ON UPDATE CASCADE);";
        db.execSQL(sql);
    }

    private void createChatsTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE [" + ChatsColumns.TABLENAME + "] ( " +
                " [" + ChatsColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + ChatsColumns.ACCOUNT_ID + "] INTEGER, " +
                " [" + ChatsColumns.DESTINATION + "] TEXT, " +
                " [" + ChatsColumns.IS_GROUP_CHAT + "] BOOLEAN, " +
                " [" + ChatsColumns.TITLE + "] TEXT, " +
                " [" + ChatsColumns.UNREAD_COUNT + "] INTEGER NOT NULL DEFAULT 0, " +
                " [" + ChatsColumns.INTERLOCUTOR_ID + "] INTEGER, " +
                " [" + ChatsColumns.HIDDEN + "] BOOLEAN, " +
                " [" + ChatsColumns.LAST_MESSAGE_TEXT + "] TEXT, " +
                " [" + ChatsColumns.LAST_MESSAGE_TIME + "] BIGINT, " +
                " [" + ChatsColumns.LAST_MESSAGE_OUT + "] BOOLEAN, " +
                " [" + ChatsColumns.LAST_MESSAGE_TYPE + "] INTEGER, " +
                " CONSTRAINT [] UNIQUE ([" + ChatsColumns.ACCOUNT_ID + "], [" + ChatsColumns.DESTINATION + "]) ON CONFLICT FAIL " +
                " FOREIGN KEY([" + ChatsColumns.ACCOUNT_ID + "]) " +
                " REFERENCES " + AccountsColumns.TABLENAME + "([" + AccountsColumns._ID + "]) ON DELETE CASCADE ON UPDATE CASCADE);";
        db.execSQL(sql);
    }

    private void createContactsTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE [" + UsersColumns.TABLENAME + "] ( " +
                " [" + UsersColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + UsersColumns.JID + "] TEXT, " +
                " [" + UsersColumns.FIRST_NAME + "] TEXT, " +
                " [" + UsersColumns.LAST_NAME + "] TEXT, " +
                " [" + UsersColumns.MIDDLE_NAME + "] TEXT, " +
                " [" + UsersColumns.PREFIX + "] TEXT, " +
                " [" + UsersColumns.SUFFIX + "] TEXT, " +
                " [" + UsersColumns.EMAIL_HOME + "] TEXT, " +
                " [" + UsersColumns.EMAIL_WORK + "] TEXT, " +
                " [" + UsersColumns.ORGANIZATION + "] TEXT, " +
                " [" + UsersColumns.ORGANIZATION_UNIT + "] TEXT, " +
                " [" + UsersColumns.PHOTO_MIME_TYPE + "] TEXT, " +
                " [" + UsersColumns.PHOTO_HASH + "] TEXT, " +
                " [" + UsersColumns.PHOTO + "] BLOB, " +
                " [" + UsersColumns.LAST_VCARD_UPDATE_TIME + "] INTEGER, " +
                " CONSTRAINT [] UNIQUE ([" + UsersColumns.JID + "]) ON CONFLICT FAIL);";
        db.execSQL(sql);
    }
}
