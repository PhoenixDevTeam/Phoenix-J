package biz.dealnote.xmpp.fragment;

import android.os.Parcel;
import android.os.Parcelable;

public class FilesCriteria implements Parcelable {

    public static final Creator<FilesCriteria> CREATOR = new Creator<FilesCriteria>() {
        @Override
        public FilesCriteria createFromParcel(Parcel in) {
            return new FilesCriteria(in);
        }

        @Override
        public FilesCriteria[] newArray(int size) {
            return new FilesCriteria[size];
        }
    };

    public String destnation;
    public String[] exts;

    public FilesCriteria() {

    }

    protected FilesCriteria(Parcel in) {
        destnation = in.readString();
        exts = in.createStringArray();
    }

    public FilesCriteria setDestnation(String destnation) {
        this.destnation = destnation;
        return this;
    }

    public FilesCriteria setExts(String[] exts) {
        this.exts = exts;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(destnation);
        dest.writeStringArray(exts);
    }
}
