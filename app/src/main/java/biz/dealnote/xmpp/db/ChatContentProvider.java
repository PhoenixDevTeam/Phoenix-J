package biz.dealnote.xmpp.db;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import biz.dealnote.xmpp.db.columns.AccountsColumns;
import biz.dealnote.xmpp.db.columns.ChatsColumns;
import biz.dealnote.xmpp.db.columns.ContactsColumns;
import biz.dealnote.xmpp.db.columns.MessagesColumns;
import biz.dealnote.xmpp.db.columns.RosterColumns;

public class ChatContentProvider extends ContentProvider {

    public static final String AUTHORITY = "biz.dealnote.xmpp.db.ChatContentProvider";

    static final int URI_ACCOUNTS = 3;
    static final int URI_ACCOUNTS_ID = 4;
    static final int URI_MESSAGES = 5;
    static final int URI_MESSAGES_ID = 6;
    static final int URI_CHATS = 9;
    static final int URI_CHATS_ID = 10;
    static final int URI_ROSTERS_ENTRIES = 11;
    static final int URI_ROSTERS_ENTRIES_ID = 12;

    static final String ACCOUNTS_PATH = "accounts";
    public static final Uri ACCOUNTS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ACCOUNTS_PATH);

    static final String MESSAGES_PATH = "messages";
    public static final Uri MESSAGES_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MESSAGES_PATH);

    static final String CHATS_PATH = "chats";
    public static final Uri CHATS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CHATS_PATH);

    static final String ROSTERS_ENTRIES_PATH = "rosters_entries";
    public static final Uri ROSTERS_ENTRIES_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ROSTERS_ENTRIES_PATH);

    static final String ACCOUNTS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + ACCOUNTS_PATH;
    static final String ACCOUNTS_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + "." + ACCOUNTS_PATH;

    static final String MESSAGES_CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + MESSAGES_PATH;
    static final String MESSAGES_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + "." + MESSAGES_PATH;

    static final String CHATS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + CHATS_PATH;
    static final String CHATS_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + "." + CHATS_PATH;

    static final String ROSTERS_ENTRIES_CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + ROSTERS_ENTRIES_PATH;
    static final String ROSTERS_ENTRIES_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + "." + ROSTERS_ENTRIES_PATH;

    private static final String TAG = ChatContentProvider.class.getSimpleName();

    private static final UriMatcher sUriMatcher;

    static Map<String, String> sAccountsProjectionMap;
    static Map<String, String> sMessagesProjectionMap;
    static Map<String, String> sChatsProjectionMap;
    static Map<String, String> sRosterEntriesProjectionMap;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, ACCOUNTS_PATH, URI_ACCOUNTS);
        sUriMatcher.addURI(AUTHORITY, ACCOUNTS_PATH + "/#", URI_ACCOUNTS_ID);
        sUriMatcher.addURI(AUTHORITY, MESSAGES_PATH, URI_MESSAGES);
        sUriMatcher.addURI(AUTHORITY, MESSAGES_PATH + "/#", URI_MESSAGES_ID);
        sUriMatcher.addURI(AUTHORITY, CHATS_PATH, URI_CHATS);
        sUriMatcher.addURI(AUTHORITY, CHATS_PATH + "/#", URI_CHATS_ID);
        sUriMatcher.addURI(AUTHORITY, ROSTERS_ENTRIES_PATH, URI_ROSTERS_ENTRIES);
        sUriMatcher.addURI(AUTHORITY, ROSTERS_ENTRIES_PATH + "/#", URI_ROSTERS_ENTRIES_ID);
    }

    static {
        sAccountsProjectionMap = new HashMap<>();
        sAccountsProjectionMap.put(AccountsColumns._ID, AccountsColumns.FULL_ID);
        sAccountsProjectionMap.put(AccountsColumns.LOGIN, AccountsColumns.FULL_LOGIN);
        sAccountsProjectionMap.put(AccountsColumns.PASSWORD, AccountsColumns.FULL_PASSWORD);
        sAccountsProjectionMap.put(AccountsColumns.HOST, AccountsColumns.FULL_HOST);
        sAccountsProjectionMap.put(AccountsColumns.PORT, AccountsColumns.FULL_PORT);
        sAccountsProjectionMap.put(AccountsColumns.DISABLE, AccountsColumns.FULL_DISABLE);
        sAccountsProjectionMap.put(AccountsColumns.PUBLIC_KEY, AccountsColumns.FULL_PUBLIC_KEY);
        sAccountsProjectionMap.put(AccountsColumns.PRIVATE_KEY, AccountsColumns.FULL_PRIVATE_KEY);

        sMessagesProjectionMap = new HashMap<>();
        sMessagesProjectionMap.put(MessagesColumns._ID, MessagesColumns.FULL_ID);
        sMessagesProjectionMap.put(MessagesColumns.ACCOUNT_ID, MessagesColumns.FULL_ACCOUNT_ID);
        sMessagesProjectionMap.put(MessagesColumns.CHAT_ID, MessagesColumns.FULL_CHAT_ID);
        sMessagesProjectionMap.put(MessagesColumns.DESTINATION, MessagesColumns.FULL_DESTINATION);
        sMessagesProjectionMap.put(MessagesColumns.SENDER_ID, MessagesColumns.FULL_SENDER_ID);
        sMessagesProjectionMap.put(MessagesColumns.SENDER_JID, MessagesColumns.FULL_SENDER_JID);
        sMessagesProjectionMap.put(MessagesColumns.UNIQUE_SERVICE_ID, MessagesColumns.FULL_STANZAID);
        sMessagesProjectionMap.put(MessagesColumns.TYPE, MessagesColumns.FULL_TYPE);
        sMessagesProjectionMap.put(MessagesColumns.BODY, MessagesColumns.FULL_BODY);
        sMessagesProjectionMap.put(MessagesColumns.STATUS, MessagesColumns.FULL_STATUS);
        sMessagesProjectionMap.put(MessagesColumns.OUT, MessagesColumns.FULL_OUT);
        sMessagesProjectionMap.put(MessagesColumns.READ_STATE, MessagesColumns.FULL_READ_STATE);
        sMessagesProjectionMap.put(MessagesColumns.DATE, MessagesColumns.FULL_DATE);
        sMessagesProjectionMap.put(MessagesColumns.WAS_ENCRYPTED, MessagesColumns.FULL_WAS_ENCRYPTED);
        sMessagesProjectionMap.put(MessagesColumns.ATTACHED_FILE_PATH, MessagesColumns.FULL_ATTACHED_FILE_PATH);
        sMessagesProjectionMap.put(MessagesColumns.ATTACHED_FILE_NAME, MessagesColumns.FULL_ATTACHED_FILE_NAME);
        sMessagesProjectionMap.put(MessagesColumns.ATTACHED_FILE_SIZE, MessagesColumns.FULL_ATTACHED_FILE_SIZE);
        sMessagesProjectionMap.put(MessagesColumns.ATTACHED_FILE_MIME, MessagesColumns.FULL_ATTACHED_FILE_MIME);
        sMessagesProjectionMap.put(MessagesColumns.ATTACHED_FILE_DESCRIPTION, MessagesColumns.FULL_ATTACHED_FILE_DESCRIPTION);

        sChatsProjectionMap = new HashMap<>();
        sChatsProjectionMap.put(ChatsColumns._ID, ChatsColumns.FULL_ID);
        sChatsProjectionMap.put(ChatsColumns.ACCOUNT_ID, ChatsColumns.FULL_ACCOUNT_ID);
        sChatsProjectionMap.put(ChatsColumns.DESTINATION, ChatsColumns.FULL_DESTINATION);
        sChatsProjectionMap.put(ChatsColumns.IS_GROUP_CHAT, ChatsColumns.FULL_IS_GROUP_CHAT);
        sChatsProjectionMap.put(ChatsColumns.TITLE, ChatsColumns.FULL_TITLE);
        sChatsProjectionMap.put(ChatsColumns.UNREAD_COUNT, ChatsColumns.FULL_UNREAD_COUNT);
        sChatsProjectionMap.put(ChatsColumns.INTERLOCUTOR_ID, ChatsColumns.FULL_INTERLOCUTOR_ID);
        sChatsProjectionMap.put(ChatsColumns.HIDDEN, ChatsColumns.FULL_HIDDEN);
        sChatsProjectionMap.put(ChatsColumns.LAST_MESSAGE_TEXT, ChatsColumns.FULL_LAST_MESSAGE_TEXT);
        sChatsProjectionMap.put(ChatsColumns.LAST_MESSAGE_TIME, ChatsColumns.FULL_LAST_MESSAGE_TIME);
        sChatsProjectionMap.put(ChatsColumns.LAST_MESSAGE_OUT, ChatsColumns.FULL_LAST_MESSAGE_OUT);
        sChatsProjectionMap.put(ChatsColumns.LAST_MESSAGE_TYPE, ChatsColumns.FULL_LAST_MESSAGE_TYPE);

        sChatsProjectionMap.put(ChatsColumns.FOREIGN_INTERLOCUTOR_JID, ContactsColumns.FULL_JID + " AS " + ChatsColumns.FOREIGN_INTERLOCUTOR_JID);
        sChatsProjectionMap.put(ChatsColumns.FOREIGN_INTERLOCUTOR_FIRST_NAME, ContactsColumns.FULL_FIRST_NAME + " AS " + ChatsColumns.FOREIGN_INTERLOCUTOR_FIRST_NAME);
        sChatsProjectionMap.put(ChatsColumns.FOREIGN_INTERLOCUTOR_LAST_NAME, ContactsColumns.FULL_LAST_NAME + " AS " + ChatsColumns.FOREIGN_INTERLOCUTOR_LAST_NAME);
        sChatsProjectionMap.put(ChatsColumns.FOREIGN_INTERLOCUTOR_PHOTO_HASH, ContactsColumns.FULL_PHOTO_HASH + " AS " + ChatsColumns.FOREIGN_INTERLOCUTOR_PHOTO_HASH);
        sChatsProjectionMap.put(ChatsColumns.FOREIGN_INTERLOCUTOR_PHOTO, ContactsColumns.FULL_PHOTO + " AS " + ChatsColumns.FOREIGN_INTERLOCUTOR_PHOTO);
        //sChatsProjectionMap.put(ChatsColumns.FOREIGN_ACCOUNT_LOGIN, AccountsColumns.FULL_LOGIN + " AS " + ChatsColumns.FOREIGN_ACCOUNT_LOGIN);
        //sChatsProjectionMap.put(ChatsColumns.FOREIGN_ACCOUNT_PASSWORD, AccountsColumns.FULL_PASSWORD + " AS " + ChatsColumns.FOREIGN_ACCOUNT_PASSWORD);
        //sChatsProjectionMap.put(ChatsColumns.FOREIGN_ACCOUNT_DISABLE, AccountsColumns.FULL_DISABLE + " AS " + ChatsColumns.FOREIGN_ACCOUNT_DISABLE);
        //sChatsProjectionMap.put(ChatsColumns.FOREIGN_ACCOUNT_HOST, AccountsColumns.FULL_HOST + " AS " + ChatsColumns.FOREIGN_ACCOUNT_HOST);
        //sChatsProjectionMap.put(ChatsColumns.FOREIGN_ACCOUNT_PORT, AccountsColumns.FULL_PORT + " AS " + ChatsColumns.FOREIGN_ACCOUNT_PORT);
        //sChatsProjectionMap.put(ChatsColumns.FOREIGN_ACCOUNT_PUBLIC_KEY, AccountsColumns.FULL_PUBLIC_KEY + " AS " + ChatsColumns.FOREIGN_ACCOUNT_PUBLIC_KEY);
        //sChatsProjectionMap.put(ChatsColumns.FOREIGN_ACCOUNT_PRIVATE_KEY, AccountsColumns.FULL_PRIVATE_KEY + " AS " + ChatsColumns.FOREIGN_ACCOUNT_PRIVATE_KEY);

        sRosterEntriesProjectionMap = new HashMap<>();
        sRosterEntriesProjectionMap.put(RosterColumns._ID, RosterColumns.FULL_ID);
        sRosterEntriesProjectionMap.put(RosterColumns.ACCOUNT_ID, RosterColumns.FULL_ACCOUNT_ID);
        sRosterEntriesProjectionMap.put(RosterColumns.JID, RosterColumns.FULL_JID);
        sRosterEntriesProjectionMap.put(RosterColumns.RESOURCE, RosterColumns.FULL_RESOURCE);
        sRosterEntriesProjectionMap.put(RosterColumns.CONTACT_ID, RosterColumns.FULL_CONTACT_ID);
        sRosterEntriesProjectionMap.put(RosterColumns.FLAGS, RosterColumns.FULL_FLAGS);
        sRosterEntriesProjectionMap.put(RosterColumns.AVAILABLE_RECEIVE_MESSAGES, RosterColumns.FULL_AVAILABLE_RECEIVE_MESSAGES);
        sRosterEntriesProjectionMap.put(RosterColumns.IS_AWAY, RosterColumns.FULL_IS_AWAY);
        sRosterEntriesProjectionMap.put(RosterColumns.PRESENSE_MODE, RosterColumns.FULL_PRESENSE_MODE);
        sRosterEntriesProjectionMap.put(RosterColumns.PRESENSE_TYPE, RosterColumns.FULL_PRESENSE_TYPE);
        sRosterEntriesProjectionMap.put(RosterColumns.PRESENSE_STATUS, RosterColumns.FULL_PRESENSE_STATUS);
        sRosterEntriesProjectionMap.put(RosterColumns.TYPE, RosterColumns.FULL_TYPE);
        sRosterEntriesProjectionMap.put(RosterColumns.NICK, RosterColumns.FULL_NICK);
        sRosterEntriesProjectionMap.put(RosterColumns.PRIORITY, RosterColumns.FULL_PRIORITY);

        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_FIRST_NAME, ContactsColumns.FULL_FIRST_NAME + " AS " + RosterColumns.FOREIGN_CONTACT_FIRST_NAME);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_LAST_NAME, ContactsColumns.FULL_LAST_NAME + " AS " + RosterColumns.FOREIGN_CONTACT_LAST_NAME);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_MIDDLE_NAME, ContactsColumns.FULL_MIDDLE_NAME + " AS " + RosterColumns.FOREIGN_CONTACT_MIDDLE_NAME);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_PREFIX, ContactsColumns.FULL_PREFIX + " AS " + RosterColumns.FOREIGN_CONTACT_PREFIX);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_SUFFIX, ContactsColumns.FULL_SUFFIX + " AS " + RosterColumns.FOREIGN_CONTACT_SUFFIX);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_EMAIL_HOME, ContactsColumns.FULL_EMAIL_HOME + " AS " + RosterColumns.FOREIGN_CONTACT_EMAIL_HOME);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_EMAIL_WORK, ContactsColumns.FULL_EMAIL_WORK + " AS " + RosterColumns.FOREIGN_CONTACT_EMAIL_WORK);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_ORGANIZATION, ContactsColumns.FULL_ORGANIZATION + " AS " + RosterColumns.FOREIGN_CONTACT_ORGANIZATION);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_ORGANIZATION_UNIT, ContactsColumns.FULL_ORGANIZATION_UNIT + " AS " + RosterColumns.FOREIGN_CONTACT_ORGANIZATION_UNIT);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_PHOTO_MIME_TYPE, ContactsColumns.FULL_PHOTO_MIME_TYPE + " AS " + RosterColumns.FOREIGN_CONTACT_PHOTO_MIME_TYPE);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_PHOTO_HASH, ContactsColumns.FULL_PHOTO_HASH + " AS " + RosterColumns.FOREIGN_CONTACT_PHOTO_HASH);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_CONTACT_PHOTO, ContactsColumns.FULL_PHOTO + " AS " + RosterColumns.FOREIGN_CONTACT_PHOTO);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_ACCOUNT_LOGIN, AccountsColumns.FULL_LOGIN + " AS " + RosterColumns.FOREIGN_ACCOUNT_LOGIN);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_ACCOUNT_PASSWORD, AccountsColumns.FULL_PASSWORD + " AS " + RosterColumns.FOREIGN_ACCOUNT_PASSWORD);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_ACCOUNT_DISABLE, AccountsColumns.FULL_DISABLE + " AS " + RosterColumns.FOREIGN_ACCOUNT_DISABLE);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_ACCOUNT_HOST, AccountsColumns.FULL_HOST + " AS " + RosterColumns.FOREIGN_ACCOUNT_HOST);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_ACCOUNT_PORT, AccountsColumns.FULL_PORT + " AS " + RosterColumns.FOREIGN_ACCOUNT_PORT);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_ACCOUNT_PUBLIC_KEY, AccountsColumns.FULL_PUBLIC_KEY + " AS " + RosterColumns.FOREIGN_ACCOUNT_PUBLIC_KEY);
        sRosterEntriesProjectionMap.put(RosterColumns.FOREIGN_ACCOUNT_PRIVATE_KEY, AccountsColumns.FULL_PRIVATE_KEY + " AS " + RosterColumns.FOREIGN_ACCOUNT_PRIVATE_KEY);
    }

    private DBHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = DBHelper.getInstance(getContext());
        return true;
    }

    @Override
    public
    @NonNull
    ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) {
        ContentProviderResult[] result = new ContentProviderResult[operations.size()];
        int i = 0;
        // Opens the database object in "write" mode.
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Begin a transaction
        db.beginTransaction();
        try {
            for (ContentProviderOperation operation : operations) {
                // Chain the result for back references
                result[i++] = operation.apply(this, result, i);
            }

            db.setTransactionSuccessful();
        } catch (OperationApplicationException e) {
            Log.d(TAG, "batch failed: " + e);
        } finally {
            db.endTransaction();
        }

        return result;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowID;
        Uri resultUri;

        int matchUri = sUriMatcher.match(uri);
        switch (matchUri) {
            case URI_ACCOUNTS:
                rowID = db.insert(AccountsColumns.TABLENAME, null, values);
                resultUri = ContentUris.withAppendedId(ACCOUNTS_CONTENT_URI, rowID);
                break;
            case URI_MESSAGES:
                rowID = db.insert(MessagesColumns.TABLENAME, null, values);
                resultUri = ContentUris.withAppendedId(MESSAGES_CONTENT_URI, rowID);
                break;
            case URI_CHATS:
                rowID = db.insert(ChatsColumns.TABLENAME, null, values);
                resultUri = ContentUris.withAppendedId(CHATS_CONTENT_URI, rowID);
                break;
            case URI_ROSTERS_ENTRIES:
                rowID = db.insert(RosterColumns.TABLENAME, null, values);
                resultUri = ContentUris.withAppendedId(ROSTERS_ENTRIES_CONTENT_URI, rowID);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Log.d(TAG, "resultUri=" + resultUri);
        safeNotifyChange(resultUri, null);
        return resultUri;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder _QB = new SQLiteQueryBuilder();
        int _TableType;

        switch (sUriMatcher.match(uri)) {
            case URI_ACCOUNTS:
                _QB.setTables(AccountsColumns.TABLENAME);
                _QB.setProjectionMap(sAccountsProjectionMap);
                _TableType = URI_ACCOUNTS;
                break;

            case URI_ACCOUNTS_ID:
                _QB.setTables(AccountsColumns.TABLENAME);
                _QB.setProjectionMap(sAccountsProjectionMap);
                _QB.appendWhere(AccountsColumns.FULL_ID + "=" + uri.getPathSegments().get(1));
                _TableType = URI_ACCOUNTS;
                break;

            case URI_MESSAGES:
                _QB.setTables(MessagesColumns.TABLENAME);
                _QB.setProjectionMap(sMessagesProjectionMap);
                _TableType = URI_MESSAGES;
                break;

            case URI_MESSAGES_ID:
                _QB.setTables(MessagesColumns.TABLENAME);
                _QB.setProjectionMap(sMessagesProjectionMap);
                _QB.appendWhere(MessagesColumns.FULL_ID + "=" + uri.getPathSegments().get(1));
                _TableType = URI_MESSAGES;
                break;

            case URI_CHATS:
                _QB.setTables(ChatsColumns.TABLENAME +
                        " LEFT OUTER JOIN " + ContactsColumns.TABLENAME + " ON " + ChatsColumns.FULL_INTERLOCUTOR_ID + " = " + ContactsColumns.FULL_ID);
                        //" LEFT OUTER JOIN " + AccountsColumns.TABLENAME + " ON " + ChatsColumns.FULL_ACCOUNT_ID + " = " + AccountsColumns.FULL_ID);
                _QB.setProjectionMap(sChatsProjectionMap);
                _TableType = URI_CHATS;
                break;

            case URI_CHATS_ID:
                _QB.setTables(ChatsColumns.TABLENAME +
                        " LEFT OUTER JOIN " + ContactsColumns.TABLENAME + " ON " + ChatsColumns.FULL_INTERLOCUTOR_ID + " = " + ContactsColumns.FULL_ID);
                        //" LEFT OUTER JOIN " + AccountsColumns.TABLENAME + " ON " + ChatsColumns.FULL_ACCOUNT_ID + " = " + AccountsColumns.FULL_ID);
                _QB.setProjectionMap(sChatsProjectionMap);
                _QB.appendWhere(ChatsColumns.FULL_ID + "=" + uri.getPathSegments().get(1));
                _TableType = URI_CHATS;
                break;

            case URI_ROSTERS_ENTRIES:
                _QB.setTables(RosterColumns.TABLENAME +
                        " LEFT OUTER JOIN " + ContactsColumns.TABLENAME + " ON " + RosterColumns.FULL_CONTACT_ID + " = " + ContactsColumns.FULL_ID +
                        " LEFT OUTER JOIN " + AccountsColumns.TABLENAME + " ON " + RosterColumns.FULL_ACCOUNT_ID + " = " + AccountsColumns.FULL_ID);
                _QB.setProjectionMap(sRosterEntriesProjectionMap);
                _TableType = URI_ROSTERS_ENTRIES;
                break;

            case URI_ROSTERS_ENTRIES_ID:
                _QB.setTables(RosterColumns.TABLENAME +
                        " LEFT OUTER JOIN " + ContactsColumns.TABLENAME + " ON " + RosterColumns.FULL_CONTACT_ID + " = " + ContactsColumns.FULL_ID +
                        " LEFT OUTER JOIN " + AccountsColumns.TABLENAME + " ON " + RosterColumns.FULL_ACCOUNT_ID + " = " + AccountsColumns.FULL_ID);
                _QB.setProjectionMap(sRosterEntriesProjectionMap);
                _QB.appendWhere(RosterColumns.FULL_ID + "=" + uri.getPathSegments().get(1));
                _TableType = URI_ROSTERS_ENTRIES;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        //Set your sort order here
        String _OrderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            // If no sort order is specified use the default
            switch (_TableType) {
                case URI_ACCOUNTS:
                    _OrderBy = AccountsColumns.FULL_ID + " ASC";
                    break;
                case URI_MESSAGES:
                    _OrderBy = MessagesColumns.FULL_ID + " ASC";
                    break;
                case URI_CHATS:
                    _OrderBy = ChatsColumns.FULL_ID + " ASC";
                    break;
                case URI_ROSTERS_ENTRIES:
                    _OrderBy = RosterColumns.FULL_ID + " ASC";
                    break;
                default:
                    throw new UnknownError("Unknown table type for sort order");
            }
        } else {
            _OrderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase _DB = dbHelper.getReadableDatabase();
        Cursor _Result = _QB.query(_DB, projection, selection, selectionArgs, null, null, _OrderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes

        if (getContext() != null) {
            _Result.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return _Result;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case URI_ACCOUNTS:
                return ACCOUNTS_CONTENT_TYPE;
            case URI_ACCOUNTS_ID:
                return ACCOUNTS_CONTENT_ITEM_TYPE;
            case URI_MESSAGES:
                return MESSAGES_CONTENT_TYPE;
            case URI_MESSAGES_ID:
                return MESSAGES_CONTENT_ITEM_TYPE;
            case URI_CHATS:
                return CHATS_CONTENT_TYPE;
            case URI_CHATS_ID:
                return CHATS_CONTENT_ITEM_TYPE;
            case URI_ROSTERS_ENTRIES:
                return ROSTERS_ENTRIES_CONTENT_TYPE;
            case URI_ROSTERS_ENTRIES_ID:
                return ROSTERS_ENTRIES_CONTENT_ITEM_TYPE;
        }

        return null;
    }


    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        String tbName;
        switch (sUriMatcher.match(uri)) {
            case URI_ACCOUNTS:
                tbName = AccountsColumns.TABLENAME;
                break;
            case URI_ACCOUNTS_ID:
                String accountId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = AccountsColumns._ID + " = " + accountId;
                } else {
                    selection = selection + " AND " + AccountsColumns._ID + " = " + accountId;
                }

                tbName = AccountsColumns.TABLENAME;
                break;
            case URI_MESSAGES:
                tbName = MessagesColumns.TABLENAME;
                break;
            case URI_MESSAGES_ID:
                String messageId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = MessagesColumns._ID + " = " + messageId;
                } else {
                    selection = selection + " AND " + MessagesColumns._ID + " = " + messageId;
                }

                tbName = MessagesColumns.TABLENAME;
                break;
            case URI_CHATS:
                tbName = ChatsColumns.TABLENAME;
                break;
            case URI_CHATS_ID:
                String chatId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = ChatsColumns._ID + " = " + chatId;
                } else {
                    selection = selection + " AND " + ChatsColumns._ID + " = " + chatId;
                }

                tbName = ChatsColumns.TABLENAME;
                break;
            case URI_ROSTERS_ENTRIES:
                tbName = RosterColumns.TABLENAME;
                break;
            case URI_ROSTERS_ENTRIES_ID:
                String rosterEntryId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = RosterColumns._ID + " = " + rosterEntryId;
                } else {
                    selection = selection + " AND " + RosterColumns._ID + " = " + rosterEntryId;
                }

                tbName = RosterColumns.TABLENAME;
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int cnt = db.delete(tbName, selection, selectionArgs);
        safeNotifyChange(uri, null);

        return cnt;
    }

    private void safeNotifyChange(Uri uri, ContentObserver contentObserver) {
        if (getContext() != null && getContext().getContentResolver() != null) {
            getContext().getContentResolver().notifyChange(uri, contentObserver);
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String tbName;
        switch (sUriMatcher.match(uri)) {
            case URI_ACCOUNTS:
                tbName = AccountsColumns.TABLENAME;
                break;
            case URI_ACCOUNTS_ID:
                String accountId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = AccountsColumns._ID + " = " + accountId;
                } else {
                    selection = selection + " AND " + AccountsColumns._ID + " = " + accountId;
                }

                tbName = AccountsColumns.TABLENAME;
                break;
            case URI_MESSAGES:
                tbName = MessagesColumns.TABLENAME;
                break;
            case URI_MESSAGES_ID:
                String messageId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = MessagesColumns._ID + " = " + messageId;
                } else {
                    selection = selection + " AND " + MessagesColumns._ID + " = " + messageId;
                }

                tbName = MessagesColumns.TABLENAME;
                break;
            case URI_CHATS:
                tbName = ChatsColumns.TABLENAME;
                break;
            case URI_CHATS_ID:
                String chatId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = ChatsColumns._ID + " = " + chatId;
                } else {
                    selection = selection + " AND " + ChatsColumns._ID + " = " + chatId;
                }

                tbName = ChatsColumns.TABLENAME;
                break;
            case URI_ROSTERS_ENTRIES:
                tbName = RosterColumns.TABLENAME;
                break;
            case URI_ROSTERS_ENTRIES_ID:
                String rosterEntryId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = RosterColumns._ID + " = " + rosterEntryId;
                } else {
                    selection = selection + " AND " + RosterColumns._ID + " = " + rosterEntryId;
                }

                tbName = RosterColumns.TABLENAME;
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int cnt = db.update(tbName, values, selection, selectionArgs);
        safeNotifyChange(uri, null);
        return cnt;
    }
}