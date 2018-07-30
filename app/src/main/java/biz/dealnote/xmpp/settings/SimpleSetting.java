package biz.dealnote.xmpp.settings;

import android.os.Parcel;
import android.os.Parcelable;

public class SimpleSetting extends AbsSettings implements Parcelable {

    public static final Creator<SimpleSetting> CREATOR = new Creator<SimpleSetting>() {
        @Override
        public SimpleSetting createFromParcel(Parcel in) {
            return new SimpleSetting(in);
        }

        @Override
        public SimpleSetting[] newArray(int size) {
            return new SimpleSetting[size];
        }
    };
    public int key;
    public int titleRes;

    public SimpleSetting(Section section, int key, int titleRes) {
        super(section);
        this.key = key;
        this.titleRes = titleRes;
    }

    protected SimpleSetting(Parcel in) {
        super(in);
        key = in.readInt();
        titleRes = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(key);
        dest.writeInt(titleRes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "SimpleSetting{" +
                "key=" + key +
                ", titleRes=" + titleRes +
                '}';
    }
}
