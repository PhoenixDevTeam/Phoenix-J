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
import biz.dealnote.xmpp.model.Msg;

public class Messages {

    private static final String TAG = Messages.class.getSimpleName();

    public static Integer appPresenseTypeFromApi(Presence.Type type) {
        if (Presence.Type.subscribe.equals(type)) {
            return Msg.TYPE_SUBSCRIBE;
        } else if (Presence.Type.subscribed.equals(type)) {
            return Msg.TYPE_SUBSCRIBED;
        } else if (Presence.Type.unsubscribe.equals(type)) {
            return Msg.TYPE_UNSUBSCRIBE;
        } else if (Presence.Type.unsubscribed.equals(type)) {
            return Msg.TYPE_UNSUBSCRIBED;
        } else {
            return null;
        }
    }

    public static int getAppTypeFrom(Message.Type type) {
        switch (type) {
            case chat:
                return Msg.TYPE_CHAT;
            case normal:
                return Msg.TYPE_NORMAL;
            case groupchat:
                return Msg.TYPE_GROUP_CHAT;
            case headline:
                return Msg.TYPE_HEADLINE;
            case error:
                return Msg.TYPE_ERROR;
            default:
                return Msg.UNKNOWN;
        }
    }

    public static Message.Type getTypeFrom(int type) {
        switch (type) {
            case Msg.TYPE_CHAT:
                return Message.Type.chat;
            case Msg.TYPE_NORMAL:
                return Message.Type.normal;
            case Msg.TYPE_GROUP_CHAT:
                return Message.Type.groupchat;
            case Msg.TYPE_HEADLINE:
                return Message.Type.headline;
            case Msg.TYPE_ERROR:
                return Message.Type.error;
            default:
                return null;
        }
    }

    public static Msg map(Cursor cursor) {
        int type = cursor.getInt(cursor.getColumnIndex(MessagesColumns.TYPE));

        AppFile file = null;
        if (type == Msg.TYPE_INCOME_FILE || type == Msg.TYPE_OUTGOING_FILE) {
            String path = cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_PATH));

            file = new AppFile(TextUtils.isEmpty(path) ? null : Uri.parse(path),
                    cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_NAME)),
                    cursor.getLong(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_SIZE)));
            file.mime = cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_MIME));
            file.description = cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_DESCRIPTION));
        }

        return new Msg()
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
        contentValues.put(MessagesColumns.STATUS, Msg.STATUS_DONE);

        context.getContentResolver().update(ChatContentProvider.MESSAGES_CONTENT_URI, contentValues,
                MessagesColumns._ID + " = ?", new String[]{String.valueOf(mid)});
    }
}
