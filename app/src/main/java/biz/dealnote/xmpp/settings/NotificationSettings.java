package biz.dealnote.xmpp.settings;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;

import biz.dealnote.xmpp.R;

public class NotificationSettings extends AbsSettings implements Parcelable {

    public static final Creator<NotificationSettings> CREATOR = new Creator<NotificationSettings>() {
        @Override
        public NotificationSettings createFromParcel(Parcel in) {
            return new NotificationSettings(in);
        }

        @Override
        public NotificationSettings[] newArray(int size) {
            return new NotificationSettings[size];
        }
    };

    private static final String OPTIONS_DEVIMITER = ", ";
    public String key;
    public int notifyTileRes;
    public Value value;

    public NotificationSettings(Section section, String key, int notifyTileRes, Value value) {
        super(section);
        this.key = key;
        this.notifyTileRes = notifyTileRes;
        this.value = value;
    }

    protected NotificationSettings(Parcel in) {
        super(in);
        key = in.readString();
        notifyTileRes = in.readInt();
        value = in.readParcelable(Value.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(key);
        dest.writeInt(notifyTileRes);
        dest.writeParcelable(value, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static class Value implements Parcelable {

        public static final Creator<Value> CREATOR = new Creator<Value>() {
            @Override
            public Value createFromParcel(Parcel in) {
                return new Value(in);
            }

            @Override
            public Value[] newArray(int size) {
                return new Value[size];
            }
        };

        public boolean enable;
        public boolean vibro;
        public boolean light;
        public boolean sound;
        public Uri uri;

        public Value() {
        }

        protected Value(Parcel in) {
            enable = in.readByte() != 0;
            vibro = in.readByte() != 0;
            light = in.readByte() != 0;
            sound = in.readByte() != 0;
            uri = in.readParcelable(Uri.class.getClassLoader());
        }

        public Value setEnable(boolean enable) {
            this.enable = enable;
            return this;
        }

        public Value setVibro(boolean vibro) {
            this.vibro = vibro;
            return this;
        }

        public Value setLight(boolean light) {
            this.light = light;
            return this;
        }

        public Value setSound(boolean sound) {
            this.sound = sound;
            return this;
        }

        public Value setUri(Uri uri) {
            this.uri = uri;
            return this;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) (enable ? 1 : 0));
            dest.writeByte((byte) (vibro ? 1 : 0));
            dest.writeByte((byte) (light ? 1 : 0));
            dest.writeByte((byte) (sound ? 1 : 0));
            dest.writeParcelable(uri, flags);
        }

        public String buildInfoLine(Context context) {
            if (!enable) {
                return context.getString(R.string.disabled).toLowerCase();
            }

            ArrayList<String> options = new ArrayList<>();
            options.add(context.getString(R.string.enabled).toLowerCase());

            if (sound) {
                options.add(context.getString(R.string.sound).toLowerCase());
            }

            if (vibro) {
                options.add(context.getString(R.string.vibration).toLowerCase());
            }

            if (light) {
                options.add(context.getString(R.string.light).toLowerCase());
            }

            return TextUtils.join(OPTIONS_DEVIMITER, options);
        }
    }
}
