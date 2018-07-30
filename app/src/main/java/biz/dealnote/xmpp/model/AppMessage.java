package biz.dealnote.xmpp.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.util.ParcelUtils;

public final class AppMessage implements Parcelable, Identificable {

    public static final int UNKNOWN = 0;

    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_CHAT = 2;
    public static final int TYPE_GROUP_CHAT = 3;
    public static final int TYPE_HEADLINE = 4;
    public static final int TYPE_ERROR = 5;
    public static final int TYPE_INCOME_FILE = 6;
    public static final int TYPE_OUTGOING_FILE = 7;

    public static final int TYPE_SUBSCRIBE = 8;
    public static final int TYPE_SUBSCRIBED = 9;
    public static final int TYPE_UNSUBSCRIBE = 10;
    public static final int TYPE_UNSUBSCRIBED = 11;

    public static final int STATUS_SENT = 1;
    public static final int STATUS_IN_QUEUE = 2;
    public static final int STATUS_SENDING = 3;
    public static final int STATUS_ERROR = 4;

    public static final int STATUS_WAITING_FOR_REASON = 11;
    public static final int STATUS_CANCELLED = 12;
    public static final int STATUS_IN_PROGRESS = 13;
    public static final int STATUS_DONE = 14;

    public static final int STATUS_ACCEPTED = 15;
    public static final int STATUS_DECLINED = 16;

    public static final Creator<AppMessage> CREATOR = new Creator<AppMessage>() {
        @Override
        public AppMessage createFromParcel(Parcel in) {
            return new AppMessage(in);
        }

        @Override
        public AppMessage[] newArray(int size) {
            return new AppMessage[size];
        }
    };

    private int id;
    private int accountId;
    private int chatId;
    private String destination;
    private int senderId;
    private String senderJid;
    private String stanzaId;
    private int type;
    private String body;
    private int status;
    private boolean out;
    private boolean readState;
    private long date;
    private AppFile attachedFile;
    private int progress;

    // оптимизация (см. в isVoiceMessage())
    private Boolean isVoiceMessage;

    private boolean selected;

    public AppMessage() {
    }

    protected AppMessage(Parcel in) {
        id = in.readInt();
        accountId = in.readInt();
        chatId = in.readInt();
        destination = in.readString();
        senderId = in.readInt();
        senderJid = in.readString();
        stanzaId = in.readString();
        type = in.readInt();
        body = in.readString();
        status = in.readInt();
        out = in.readByte() != 0;
        readState = in.readByte() != 0;
        date = in.readLong();
        attachedFile = in.readParcelable(AppFile.class.getClassLoader());
        progress = in.readInt();
        isVoiceMessage = ParcelUtils.readObjectBoolean(in);
        selected = in.readByte() != 0;
    }

    public static String getMessageBody(Context context, int type, boolean out, String destination, String body) {
        switch (type) {
            case TYPE_NORMAL:
            case TYPE_CHAT:
            case TYPE_GROUP_CHAT:
                return body;
            case TYPE_SUBSCRIBE:
                if (out) {
                    return context.getString(R.string.out_message_subscribe_text, destination);
                } else {
                    return context.getString(R.string.income_message_subscribe_text, destination);
                }

            case TYPE_SUBSCRIBED:
                if (out) {
                    return context.getString(R.string.out_message_subscribed_text, destination);
                } else {
                    return context.getString(R.string.income_message_subscribed_text, destination);
                }

            case TYPE_UNSUBSCRIBE:
                if (out) {
                    return context.getString(R.string.out_message_unsubscribe_text, destination);
                } else {
                    return context.getString(R.string.income_message_unsubscribe_text, destination);
                }

            case TYPE_UNSUBSCRIBED:
                if (out) {
                    return context.getString(R.string.out_message_unsubscribed_text, destination);
                } else {
                    return context.getString(R.string.income_message_unsubscribed_text, destination);
                }

            case TYPE_INCOME_FILE:
                return context.getString(R.string.income_file);

            case TYPE_OUTGOING_FILE:
                return context.getString(R.string.outgoing_file);

            default:
                throw new IllegalArgumentException("Invalid message type");
        }
    }

    public boolean hasSavedFile() {
        return attachedFile != null;
    }

    public AppFile getAttachedFile() {
        return attachedFile;
    }

    public AppMessage setAttachedFile(AppFile file) {
        this.attachedFile = file;
        return this;
    }

    public int getSenderId() {
        return senderId;
    }

    public AppMessage setSenderId(int senderId) {
        this.senderId = senderId;
        return this;
    }

    public int getAccountId() {
        return accountId;
    }

    public AppMessage setAccountId(int accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getStanzaId() {
        return stanzaId;
    }

    public AppMessage setStanzaId(String stanzaId) {
        this.stanzaId = stanzaId;
        return this;
    }

    public int getType() {
        return type;
    }

    public AppMessage setType(int type) {
        this.type = type;
        return this;
    }

    public String getBody() {
        return body;
    }

    public AppMessage setBody(String body) {
        this.body = body;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public AppMessage setStatus(int status) {
        this.status = status;
        return this;
    }

    public boolean isOut() {
        return out;
    }

    public AppMessage setOut(boolean out) {
        this.out = out;
        return this;
    }

    public long getDate() {
        return date;
    }

    public AppMessage setDate(long date) {
        this.date = date;
        return this;
    }

    public boolean isReadState() {
        return readState;
    }

    public AppMessage setReadState(boolean readState) {
        this.readState = readState;
        return this;
    }

    public int getChatId() {
        return chatId;
    }

    public AppMessage setChatId(int chatId) {
        this.chatId = chatId;
        return this;
    }

    public String getSenderJid() {
        return senderJid;
    }

    public AppMessage setSenderJid(String senderJid) {
        this.senderJid = senderJid;
        return this;
    }

    public String getDestination() {
        return destination;
    }

    public AppMessage setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public int getProgress() {
        return progress;
    }

    public AppMessage setProgress(int progress) {
        this.progress = progress;
        return this;
    }

    public boolean isVoiceMessage() {
        if (isVoiceMessage != null) {
            return isVoiceMessage;
        }

        // таким образом мы сравниваем имя файла один раз
        isVoiceMessage = attachedFile != null && attachedFile.name.endsWith(".mp3");
        return isVoiceMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(accountId);
        dest.writeInt(chatId);
        dest.writeString(destination);
        dest.writeInt(senderId);
        dest.writeString(senderJid);
        dest.writeString(stanzaId);
        dest.writeInt(type);
        dest.writeString(body);
        dest.writeInt(status);
        dest.writeByte((byte) (out ? 1 : 0));
        dest.writeByte((byte) (readState ? 1 : 0));
        dest.writeLong(date);
        dest.writeParcelable(attachedFile, flags);
        dest.writeInt(progress);
        ParcelUtils.writeObjectBoolean(dest, isVoiceMessage);
        dest.writeByte((byte) (selected ? 1 : 0));
    }

    public boolean isSelected() {
        return selected;
    }

    public AppMessage setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppMessage message = (AppMessage) o;
        return id == message.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int getId() {
        return id;
    }

    public AppMessage setId(int id) {
        this.id = id;
        return this;
    }

    public String getMessageBody(Context context) {
        return getMessageBody(context, type, out, destination, body);
    }
}