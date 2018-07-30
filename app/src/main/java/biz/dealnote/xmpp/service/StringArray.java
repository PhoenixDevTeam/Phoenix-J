package biz.dealnote.xmpp.service;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Arrays;

/**
 * Created by admin on 07.11.2016.
 * phoenix-for-xmpp
 */
public class StringArray implements Parcelable {

    private final String[] array;

    public StringArray(@NonNull String... array) {
        this.array = array;
    }

    protected StringArray(Parcel in) {
        array = in.createStringArray();
    }

    public static final Creator<StringArray> CREATOR = new Creator<StringArray>() {
        @Override
        public StringArray createFromParcel(Parcel in) {
            return new StringArray(in);
        }

        @Override
        public StringArray[] newArray(int size) {
            return new StringArray[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringArray(array);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringArray that = (StringArray) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(array, that.array);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }

    @NonNull
    public String[] getArray() {
        return array;
    }
}
