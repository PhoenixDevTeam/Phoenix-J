package biz.dealnote.xmpp.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class AppFile implements Parcelable {

    public static final Creator<AppFile> CREATOR = new Creator<AppFile>() {
        @Override
        public AppFile createFromParcel(Parcel in) {
            return new AppFile(in);
        }

        @Override
        public AppFile[] newArray(int size) {
            return new AppFile[size];
        }
    };
    public Uri uri;
    public String name;
    public long size;
    public String mime;
    public String description;

    public AppFile(Uri uri, String name) {
        this.uri = uri;
        this.name = name;
        this.size = 0;
    }

    public AppFile(Uri uri, String name, long size) {
        this.uri = uri;
        this.name = name;
        this.size = size;
    }

    protected AppFile(Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        name = in.readString();
        size = in.readLong();
        mime = in.readString();
        description = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeString(name);
        dest.writeLong(size);
        dest.writeString(mime);
        dest.writeString(description);
    }

    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getMime() {
        return mime;
    }

    public String getDescription() {
        return description;
    }

    public AppFile setUri(Uri uri) {
        this.uri = uri;
        return this;
    }

    public AppFile setName(String name) {
        this.name = name;
        return this;
    }

    public AppFile setSize(long size) {
        this.size = size;
        return this;
    }

    public AppFile setMime(String mime) {
        this.mime = mime;
        return this;
    }

    public AppFile setDescription(String description) {
        this.description = description;
        return this;
    }
}
