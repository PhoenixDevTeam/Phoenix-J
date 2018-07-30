package biz.dealnote.xmpp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import biz.dealnote.xmpp.db.columns.MessagesColumns;
import biz.dealnote.xmpp.model.AppFile;
import biz.dealnote.xmpp.model.AppMessage;

public class Messages {

    private static final String TAG = Messages.class.getSimpleName();

    public static Integer appPresenseTypeFromApi(Presence.Type type) {
        if (Presence.Type.subscribe.equals(type)) {
            return AppMessage.TYPE_SUBSCRIBE;
        } else if (Presence.Type.subscribed.equals(type)) {
            return AppMessage.TYPE_SUBSCRIBED;
        } else if (Presence.Type.unsubscribe.equals(type)) {
            return AppMessage.TYPE_UNSUBSCRIBE;
        } else if (Presence.Type.unsubscribed.equals(type)) {
            return AppMessage.TYPE_UNSUBSCRIBED;
        } else {
            return null;
        }
    }

    public static int getAppTypeFrom(Message.Type type) {
        switch (type) {
            case chat:
                return AppMessage.TYPE_CHAT;
            case normal:
                return AppMessage.TYPE_NORMAL;
            case groupchat:
                return AppMessage.TYPE_GROUP_CHAT;
            case headline:
                return AppMessage.TYPE_HEADLINE;
            case error:
                return AppMessage.TYPE_ERROR;
            default:
                return AppMessage.UNKNOWN;
        }
    }

    public static Message.Type getTypeFrom(int type) {
        switch (type) {
            case AppMessage.TYPE_CHAT:
                return Message.Type.chat;
            case AppMessage.TYPE_NORMAL:
                return Message.Type.normal;
            case AppMessage.TYPE_GROUP_CHAT:
                return Message.Type.groupchat;
            case AppMessage.TYPE_HEADLINE:
                return Message.Type.headline;
            case AppMessage.TYPE_ERROR:
                return Message.Type.error;
            default:
                return null;
        }
    }

    public static AppMessage map(Cursor cursor) {
        int type = cursor.getInt(cursor.getColumnIndex(MessagesColumns.TYPE));

        AppFile file = null;
        if (type == AppMessage.TYPE_INCOME_FILE || type == AppMessage.TYPE_OUTGOING_FILE) {
            String path = cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_PATH));

            file = new AppFile(TextUtils.isEmpty(path) ? null : Uri.parse(path),
                    cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_NAME)),
                    cursor.getLong(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_SIZE)));
            file.mime = cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_MIME));
            file.description = cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_DESCRIPTION));
        }

        return new AppMessage()
                .setId(cursor.getInt(cursor.getColumnIndex(MessagesColumns._ID)))
                .setAccountId(cursor.getInt(cursor.getColumnIndex(MessagesColumns.ACCOUNT_ID)))
                .setChatId(cursor.getInt(cursor.getColumnIndex(MessagesColumns.CHAT_ID)))
                .setSenderId(cursor.getInt(cursor.getColumnIndex(MessagesColumns.SENDER_ID)))
                .setSenderJid(cursor.getString(cursor.getColumnIndex(MessagesColumns.SENDER_JID)))
                .setStanzaId(cursor.getString(cursor.getColumnIndex(MessagesColumns.UNIQUE_SERVICE_ID)))
                .setType(type)
                .setDestination(cursor.getString(cursor.getColumnIndex(MessagesColumns.DESTINATION)))
                .setBody(cursor.getString(cursor.getColumnIndex(MessagesColumns.BODY)))
                .setStatus(cursor.getInt(cursor.getColumnIndex(MessagesColumns.STATUS)))
                .setOut(cursor.getInt(cursor.getColumnIndex(MessagesColumns.OUT)) == 1)
                .setReadState(cursor.getInt(cursor.getColumnIndex(MessagesColumns.READ_STATE)) == 1)
                .setDate(cursor.getLong(cursor.getColumnIndex(MessagesColumns.DATE)))
                .setAttachedFile(file);
    }

    public static void appendAttachedFilePath(Context context, int mid, String path) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessagesColumns.ATTACHED_FILE_PATH, path);
        contentValues.put(MessagesColumns.STATUS, AppMessage.STATUS_DONE);

        context.getContentResolver().update(ChatContentProvider.MESSAGES_CONTENT_URI, contentValues,
                MessagesColumns._ID + " = ?", new String[]{String.valueOf(mid)});
    }
}
