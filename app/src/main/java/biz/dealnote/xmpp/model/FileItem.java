package biz.dealnote.xmpp.model;

import android.os.Parcel;
import android.os.Parcelable;

public class FileItem implements Parcelable {

    public static final Creator<FileItem> CREATOR = new Creator<FileItem>() {
        @Override
        public FileItem createFromParcel(Parcel in) {
            return new FileItem(in);
        }

        @Override
        public FileItem[] newArray(int size) {
            return new FileItem[size];
        }
    };

    public boolean directory;
    public String file;
    public String details;
    public int icon;
    public boolean canRead;

    public FileItem(boolean directory, String file, String details, int icon, boolean canRead) {
        this.directory = directory;
        this.file = file;
        this.details = details;
        this.icon = icon;
        this.canRead = canRead;
    }

    protected FileItem(Parcel in) {
        directory = in.readByte() == (byte) 1;
        file = in.readString();
        details = in.readString();
        icon = in.readInt();
        canRead = in.readByte() == (byte) 1;
    }

    @Override
    public String toString() {
        return file;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(directory ? (byte) 1 : (byte) 0);
        dest.writeString(file);
        dest.writeString(details);
        dest.writeInt(icon);
        dest.writeByte(canRead ? (byte) 1 : (byte) 0);
    }
}
