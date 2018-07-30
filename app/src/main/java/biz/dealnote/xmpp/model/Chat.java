package biz.dealnote.xmpp.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Chat implements Parcelable, Identificable {

    public static final Creator<Chat> CREATOR = new Creator<Chat>() {
        @Override
        public Chat createFromParcel(Parcel in) {
            return new Chat(in);
        }

        @Override
        public Chat[] newArray(int size) {
            return new Chat[size];
        }
    };

    private int id;
    private int accountId;
    private String destination;
    private boolean groupChat;
    private String title;
    private int unreadCount;
    private int interlocutorId;
    private boolean hidden;
    private Contact interlocutor;

    private String lastMessageText;
    private long lastMessageTime;
    private boolean lastMessageOut;
    private int lastMessageType;

    public Chat() {
    }

    private Chat(Parcel in) {
        id = in.readInt();
        accountId = in.readInt();
        destination = in.readString();
        groupChat = in.readByte() != 0;
        title = in.readString();
        unreadCount = in.readInt();
        interlocutorId = in.readInt();
        hidden = in.readByte() != 0;
        interlocutor = in.readParcelable(Contact.class.getClassLoader());
        lastMessageText = in.readString();
        lastMessageTime = in.readLong();
        lastMessageOut = in.readByte() != 0;
        lastMessageType = in.readInt();
    }

    public int getAccountId() {
        return accountId;
    }

    public Chat setAccountId(int accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getDestination() {
        return destination;
    }

    public Chat setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public boolean isGroupChat() {
        return groupChat;
    }

    public Chat setGroupChat(boolean groupChat) {
        this.groupChat = groupChat;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Chat setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public Chat setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
        return this;
    }

    public int getInterlocutorId() {
        return interlocutorId;
    }

    public Chat setInterlocutorId(int interlocutorId) {
        this.interlocutorId = interlocutorId;
        return this;
    }

    public Contact getInterlocutor() {
        return interlocutor;
    }

    public Chat setInterlocutor(Contact interlocutor) {
        this.interlocutor = interlocutor;
        return this;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public Chat setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
        return this;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public Chat setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
        return this;
    }

    public boolean isLastMessageOut() {
        return lastMessageOut;
    }

    public Chat setLastMessageOut(boolean lastMessageOut) {
        this.lastMessageOut = lastMessageOut;
        return this;
    }

    public int getLastMessageType() {
        return lastMessageType;
    }

    public Chat setLastMessageType(int lastMessageType) {
        this.lastMessageType = lastMessageType;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Chat setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(accountId);
        dest.writeString(destination);
        dest.writeByte((byte) (groupChat ? 1 : 0));
        dest.writeString(title);
        dest.writeInt(unreadCount);
        dest.writeInt(interlocutorId);
        dest.writeByte((byte) (hidden ? 1 : 0));
        dest.writeParcelable(interlocutor, flags);
        dest.writeString(lastMessageText);
        dest.writeLong(lastMessageTime);
        dest.writeByte((byte) (lastMessageOut ? 1 : 0));
        dest.writeInt(lastMessageType);
    }

    @Override
    public int getId() {
        return id;
    }

    public Chat setId(int id) {
        this.id = id;
        return this;
    }
}