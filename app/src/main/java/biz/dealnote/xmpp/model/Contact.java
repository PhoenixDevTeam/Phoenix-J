package biz.dealnote.xmpp.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.jivesoftware.smack.roster.packet.RosterPacket;

import biz.dealnote.xmpp.util.ParcelUtils;

public final class Contact implements Parcelable, Identificable {

    public static final int UNKNOWN = 0;

    /**
     * У пользователя нет подписки к контакту, нет подписки и к информации присутствия пользователя
     */
    public static final int TYPE_NONE = 1;

    /**
     * у пользователя есть подписка к информации присутствия контакта, но у контакта нет подписки к информации присутствия пользователя
     */
    public static final int TYPE_TO = 2;

    /**
     * у контакта есть подписка к информации присутствия пользователя, но у пользователя нет подписки к информации присутствия контакта
     */
    public static final int TYPE_FROM = 3;

    /**
     * у пользователя есть подписка к присутствию контакта, да и у контакта есть подписка к пользователю
     */
    public static final int TYPE_BOTH = 4;

    /**
     * пользователь желает, чтобы остановить получать обновления от абонента
     */
    public static final int TYPE_REMOVE = 5;

    public static final int PRESENCE_TYPE_AVAILABLE = 1;
    public static final int PRESENCE_TYPE_UNAVAILABLE = 2;
    public static final int PRESENCE_TYPE_SUBSCRIBE = 3;
    public static final int PRESENCE_TYPE_SUBSCRIBED = 4;
    public static final int PRESENCE_TYPE_UNSUBSCRIBE = 5;
    public static final int PRESENCE_TYPE_UNSUBSCRIBED = 6;
    public static final int PRESENCE_TYPE_ERROR = 7;
    public static final int PRESENCE_TYPE_PROBE = 8;

    public static final int PRESENSE_MODE_CHAT = 1;
    public static final int PRESENSE_MODE_AVAILABLE = 2;
    public static final int PRESENSE_MODE_AWAY = 3;
    public static final int PRESENSE_MODE_XA = 4;
    public static final int PRESENSE_MODE_DND = 5;

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    public int id;
    public AccountId accountId;

    public String jid;
    public User user;
    public int flags;
    public boolean availableToReceiveMessages;
    public boolean away;
    public Integer presenceMode;
    public Integer presenceType;
    public String presenceStatus;
    public Integer type;
    //public Integer status;
    public String nick;
    public int priority;

    public Contact() {
    }

    protected Contact(Parcel in) {
        id = in.readInt();
        accountId = in.readParcelable(AccountId.class.getClassLoader());
        jid = in.readString();
        user = in.readParcelable(User.class.getClassLoader());
        flags = in.readInt();
        availableToReceiveMessages = in.readByte() != 0;
        away = in.readByte() != 0;
        presenceMode = ParcelUtils.readObjectInteger(in);
        presenceType = ParcelUtils.readObjectInteger(in);
        presenceStatus = in.readString();
        type = ParcelUtils.readObjectInteger(in);
        //status = ParcelUtils.readObjectInteger(in);
        nick = in.readString();
        priority = in.readInt();
    }

   /* public static Integer apiStatusToAppStatus(RosterPacket.ItemStatus status) {
        if (status == null) {
            return null;
        }

        switch (status) {
            case subscribe:
                return STATUS_SUBSCRIBE;
            case unsubscribe:
                return STATUS_UNSUBSCRIBE;
        }

        return UNKNOWN;
    }*/

    public static Integer apiTypeToAppType(RosterPacket.ItemType type) {
        if (type == null) {
            return null;
        }

        switch (type) {
            case none:
                return TYPE_NONE;
            case to:
                return TYPE_TO;
            case from:
                return TYPE_FROM;
            case both:
                return TYPE_BOTH;
            case remove:
                return TYPE_REMOVE;
        }

        return UNKNOWN;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeParcelable(accountId, flags);
        dest.writeString(jid);
        dest.writeParcelable(user, flags);
        dest.writeInt(flags);
        dest.writeByte((byte) (availableToReceiveMessages ? 1 : 0));
        dest.writeByte((byte) (away ? 1 : 0));
        ParcelUtils.writeObjectInteger(dest, presenceMode);
        ParcelUtils.writeObjectInteger(dest, presenceType);
        dest.writeString(presenceStatus);
        ParcelUtils.writeObjectInteger(dest, type);
        //ParcelUtils.writeObjectInteger(dest, status);
        dest.writeString(nick);
        dest.writeInt(priority);
    }

    public boolean needSendSubscribePresence() {
        return presenceType == null;
    }

    @Override
    public int getId() {
        return id;
    }

    public String getJid() {
        return jid;
    }
}
