package biz.dealnote.xmpp.model;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class LocalPhoto implements Parcelable {

    public long id;
    public Uri uri;

    public LocalPhoto(long id, Uri uri) {
        this.id = id;
        this.uri = uri;
    }

    protected LocalPhoto(Parcel in) {
        id = in.readLong();
        uri = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<LocalPhoto> CREATOR = new Creator<LocalPhoto>() {
        @Override
        public LocalPhoto createFromParcel(Parcel in) {
            return new LocalPhoto(in);
        }

        @Override
        public LocalPhoto[] newArray(int size) {
            return new LocalPhoto[size];
        }
    };

    @Override
    public String toString() {
        return "LocalPhoto{" +
                "id=" + id +
                ", uri=" + uri +
                '}';
    }

    public Uri buildUriForPicasso() {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/images/media/"), id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeParcelable(uri, flags);
    }
}
