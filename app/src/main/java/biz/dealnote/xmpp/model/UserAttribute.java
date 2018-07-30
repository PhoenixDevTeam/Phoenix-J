package biz.dealnote.xmpp.model;

import android.os.Parcel;
import android.os.Parcelable;

public class UserAttribute implements Parcelable {

    public static final int FIRST_NAME = 1;
    public static final int LAST_NAME = 2;

    public static final Creator<UserAttribute> CREATOR = new Creator<UserAttribute>() {
        @Override
        public UserAttribute createFromParcel(Parcel in) {
            return new UserAttribute(in);
        }

        @Override
        public UserAttribute[] newArray(int size) {
            return new UserAttribute[size];
        }
    };

    public int type;
    public int title;
    public String value;

    public UserAttribute(int type, int title, String value) {
        this.type = type;
        this.title = title;
        this.value = value;
    }

    protected UserAttribute(Parcel in) {
        type = in.readInt();
        title = in.readInt();
        value = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeInt(title);
        dest.writeString(value);
    }
}
