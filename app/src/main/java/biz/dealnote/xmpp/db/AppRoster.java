package biz.dealnote.xmpp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.RosterEntry;

import java.security.KeyPair;
import java.util.ArrayList;

import biz.dealnote.xmpp.db.columns.RosterColumns;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AccountId;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.model.User;
import biz.dealnote.xmpp.util.Utils;

public class AppRoster {

    private static final String TAG = AppRoster.class.getSimpleName();

    public static ArrayList<Contact> getAllRosterEntries(Context context, String orderBy) {
        Cursor cursor = context.getContentResolver().query(ChatContentProvider.ROSTERS_ENTRIES_CONTENT_URI, null, null, null, orderBy);
        ArrayList<Contact> entries = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                entries.add(map(cursor));
            }

            cursor.close();
        }

        return entries;
    }

    /**
     * Сохранение контакта в базе банных
     *
     * @param accountId идентификатор аккаунта
     * @param entry     данные контакта
     */
    public static void mergeRosterEntry(Context context, int accountId, RosterEntry entry) {
        String bareJid = entry.getJid().asBareJid().toString();

        ContentValues cv = new ContentValues();
        cv.put(RosterColumns.TYPE, Contact.apiTypeToAppType(entry.getType()));
        cv.put(RosterColumns.NICK, entry.getName());

        // пытаемся обновить информацию
        int rows = context.getContentResolver().update(ChatContentProvider.ROSTERS_ENTRIES_CONTENT_URI, cv,
                RosterColumns.ACCOUNT_ID + " = ? AND " + RosterColumns.JID + " LIKE ?", new String[]{String.valueOf(accountId), bareJid});

        // если нет записей для обновления, то делаем вставку
        if (rows <= 0) {
            int contactId = Repositories.getInstance()
                    .getUsersStorage()
                    .getContactIdPutIfNotExist(bareJid)
                    .blockingGet();

            cv.put(RosterColumns.ACCOUNT_ID, accountId);
            cv.put(RosterColumns.JID, bareJid);
            cv.put(RosterColumns.CONTACT_ID, contactId);

            context.getContentResolver().insert(ChatContentProvider.ROSTERS_ENTRIES_CONTENT_URI, cv);
        }
    }

    public static boolean deleteRosterEntry(Context context, int accountId, String jid) {
        String normalJid = Utils.getBareJid(jid);
        String where = RosterColumns.ACCOUNT_ID + " = ? AND " + RosterColumns.JID + " LIKE ?";
        String[] args = new String[]{String.valueOf(accountId), normalJid};
        int count = context.getContentResolver().delete(ChatContentProvider.ROSTERS_ENTRIES_CONTENT_URI, where, args);
        return count > 0;
    }

    private static int getPresenceModeFrom(Presence.Mode mode) {
        if (mode == null) {
            return Contact.UNKNOWN;
        }

        switch (mode) {
            case chat:
                return Contact.PRESENSE_MODE_CHAT;
            case available:
                return Contact.PRESENSE_MODE_AVAILABLE;
            case away:
                return Contact.PRESENSE_MODE_AWAY;
            case xa:
                return Contact.PRESENSE_MODE_XA;
            case dnd:
                return Contact.PRESENSE_MODE_DND;
            default:
                return Contact.UNKNOWN;
        }
    }

    private static int getPresenceTypeFrom(Presence.Type type) {
        if (type == null) {
            return Contact.UNKNOWN;
        }

        switch (type) {
            case available:
                return Contact.PRESENCE_TYPE_AVAILABLE;
            case unavailable:
                return Contact.PRESENCE_TYPE_UNAVAILABLE;
            case subscribe:
                return Contact.PRESENCE_TYPE_SUBSCRIBE;
            case subscribed:
                return Contact.PRESENCE_TYPE_SUBSCRIBED;
            case unsubscribe:
                return Contact.PRESENCE_TYPE_UNSUBSCRIBE;
            case unsubscribed:
                return Contact.PRESENCE_TYPE_UNSUBSCRIBED;
            case error:
                return Contact.PRESENCE_TYPE_ERROR;
            case probe:
                return Contact.PRESENCE_TYPE_PROBE;
            default:
                return Contact.UNKNOWN;
        }
    }

    public static Presence.Type getApiPresenseTypeFrom(int type) {
        switch (type) {
            case Contact.PRESENCE_TYPE_AVAILABLE:
                return Presence.Type.available;
            case Contact.PRESENCE_TYPE_UNAVAILABLE:
                return Presence.Type.unavailable;
            case Contact.PRESENCE_TYPE_SUBSCRIBE:
                return Presence.Type.subscribe;
            case Contact.PRESENCE_TYPE_SUBSCRIBED:
                return Presence.Type.subscribed;
            case Contact.PRESENCE_TYPE_UNSUBSCRIBE:
                return Presence.Type.unsubscribe;
            case Contact.PRESENCE_TYPE_UNSUBSCRIBED:
                return Presence.Type.unsubscribed;
            case Contact.PRESENCE_TYPE_ERROR:
                return Presence.Type.error;
            case Contact.PRESENCE_TYPE_PROBE:
                return Presence.Type.probe;
            default:
                return null;
        }
    }

    public static Contact map(Cursor cursor) {
        byte[] pubKeyBytes = cursor.getBlob(cursor.getColumnIndex(RosterColumns.FOREIGN_ACCOUNT_PUBLIC_KEY));
        byte[] privKeyBytes = cursor.getBlob(cursor.getColumnIndex(RosterColumns.FOREIGN_ACCOUNT_PRIVATE_KEY));
        KeyPair keyPair = Accounts.restoreDSAKeyPairFrom(pubKeyBytes, privKeyBytes);

        Account account = new Account(
                cursor.getInt(cursor.getColumnIndex(RosterColumns.ACCOUNT_ID)),
                cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_ACCOUNT_LOGIN)),
                cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_ACCOUNT_PASSWORD)),
                cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_ACCOUNT_HOST)),
                cursor.getInt(cursor.getColumnIndex(RosterColumns.FOREIGN_ACCOUNT_PORT)),
                cursor.getInt(cursor.getColumnIndex(RosterColumns.FOREIGN_ACCOUNT_DISABLE)) == 1,
                keyPair);

        String jid = cursor.getString(cursor.getColumnIndex(RosterColumns.JID));

        User user = new User()
                .setId(cursor.getInt(cursor.getColumnIndex(RosterColumns.CONTACT_ID)))
                .setJid(jid)
                .setFirstName(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_FIRST_NAME)))
                .setLastName(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_LAST_NAME)))
                .setMiddleName(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_MIDDLE_NAME)))
                .setPrefix(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_PREFIX)))
                .setSuffix(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_SUFFIX)))
                .setEmailHome(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_EMAIL_HOME)))
                .setEmailWork(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_EMAIL_WORK)))
                .setOrganization(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_ORGANIZATION)))
                .setOrganizationUnit(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_ORGANIZATION_UNIT)))
                .setPhotoMimeType(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_PHOTO_MIME_TYPE)))
                .setPhotoHash(cursor.getString(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_PHOTO_HASH)));
                //.setAvatar(cursor.getBlob(cursor.getColumnIndex(RosterColumns.FOREIGN_CONTACT_PHOTO)));

        Contact entry = new Contact();

        entry.id = cursor.getInt(cursor.getColumnIndex(RosterColumns._ID));
        entry.accountId = new AccountId(account.id, account.login);
        entry.jid = jid;
        entry.user = user;
        entry.flags = cursor.getInt(cursor.getColumnIndex(RosterColumns.FLAGS));
        entry.availableToReceiveMessages = cursor.getInt(cursor.getColumnIndex(RosterColumns.AVAILABLE_RECEIVE_MESSAGES)) == 1;
        entry.away = cursor.getInt(cursor.getColumnIndex(RosterColumns.IS_AWAY)) == 1;

        int presenseModeColumnIndex = cursor.getColumnIndex(RosterColumns.PRESENSE_MODE);
        entry.presenceMode = cursor.isNull(presenseModeColumnIndex) ? null : cursor.getInt(presenseModeColumnIndex);

        int presenseTypeColumnIndex = cursor.getColumnIndex(RosterColumns.PRESENSE_TYPE);
        entry.presenceType = cursor.isNull(presenseTypeColumnIndex) ? null : cursor.getInt(presenseTypeColumnIndex);

        entry.presenceStatus = cursor.getString(cursor.getColumnIndex(RosterColumns.PRESENSE_STATUS));

        int typeColumnIndex = cursor.getColumnIndex(RosterColumns.TYPE);
        entry.type = cursor.isNull(typeColumnIndex) ? null : cursor.getInt(typeColumnIndex);

        //int statusColumnIndex = cursor.getColumnIndex(RosterColumns.STATUS);
        //entry.status = cursor.isNull(statusColumnIndex) ? null : cursor.getInt(statusColumnIndex);

        entry.nick = cursor.getString(cursor.getColumnIndex(RosterColumns.NICK));
        entry.priority = cursor.getInt(cursor.getColumnIndex(RosterColumns.PRIORITY));
        return entry;
    }

    public static void updateRosterEntryType(Context context, int accountId, @NonNull String from, int tagretType) {
        ContentValues cv = new ContentValues();
        cv.put(RosterColumns.TYPE, tagretType);
        String where = RosterColumns.ACCOUNT_ID + " = ? AND " + RosterColumns.JID + " LIKE ?";
        String[] args = {String.valueOf(accountId), from};
        context.getContentResolver().update(ChatContentProvider.ROSTERS_ENTRIES_CONTENT_URI, cv, where, args);
    }

    public static void mergeNewPresence(Context context, int accountId, Presence presence) {
        String bareJid = Utils.getBareJid(presence.getFrom().asBareJid().toString());

        Presence.Type type = presence.getType();
        Presence.Mode mode = presence.getMode();
        String status = presence.getStatus();

        ContentValues cv = new ContentValues();
        cv.put(RosterColumns.PRESENSE_TYPE, getPresenceTypeFrom(type));
        cv.put(RosterColumns.PRESENSE_MODE, getPresenceModeFrom(mode));
        cv.put(RosterColumns.PRESENSE_STATUS, status);

        String resource = null;

        Log.d(TAG, "mergeNewPresence, bareJid: " + bareJid + ", from: " + presence.getFrom());

        try {
            resource = presence.getFrom().getResourceOrNull().toString();
        } catch (Exception ignored) {
        }

        cv.put(RosterColumns.RESOURCE, resource);

        if (presence.getType() == Presence.Type.available) {
            cv.put(RosterColumns.AVAILABLE_RECEIVE_MESSAGES, Boolean.TRUE);
        }

        if (presence.getType() == Presence.Type.unavailable) {
            cv.put(RosterColumns.AVAILABLE_RECEIVE_MESSAGES, Boolean.FALSE);
        }

        if (Presence.Mode.available == mode || Presence.Mode.chat == mode) {
            cv.put(RosterColumns.IS_AWAY, Boolean.FALSE);
        } else if (presence.isAway()) {
            cv.put(RosterColumns.IS_AWAY, Boolean.TRUE);
        }


        Log.d(TAG, "mergeNewPresence, type: " + type + ", mode: " + mode + ", status: " + status + ", bareJid: " + bareJid);

        String where = RosterColumns.ACCOUNT_ID + " = ? AND " + RosterColumns.JID + " LIKE ?";
        String[] args = new String[]{String.valueOf(accountId), bareJid};
        context.getContentResolver().update(ChatContentProvider.ROSTERS_ENTRIES_CONTENT_URI, cv, where, args);
    }

    public static Contact findByJid(Context context, int accountId, String jid) {
        String bareJid = Utils.getBareJid(jid);

        String where = RosterColumns.ACCOUNT_ID + " = ? AND " + RosterColumns.FULL_JID + " LIKE ?";
        String[] args = {String.valueOf(accountId), bareJid};

        Cursor cursor = context.getContentResolver().query(ChatContentProvider.ROSTERS_ENTRIES_CONTENT_URI, null, where, args, null);

        Contact contact = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                contact = map(cursor);
            }

            cursor.close();
        }

        return contact;
    }

    public static String findRosterResource(Context context, String jid) {
        String normalJid = Utils.getBareJid(jid);

        String[] columns = {RosterColumns.RESOURCE};
        String where = RosterColumns.FULL_JID + " LIKE ?";
        String[] args = {normalJid};

        Cursor cursor = context.getContentResolver().query(ChatContentProvider.ROSTERS_ENTRIES_CONTENT_URI, columns, where, args, null);

        String resource = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                resource = cursor.getString(cursor.getColumnIndex(RosterColumns.RESOURCE));
            }

            cursor.close();
        }

        return resource;
    }
}
