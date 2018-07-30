package biz.dealnote.xmpp.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AvatarResorce implements Parcelable {

    public static final Creator<AvatarResorce> CREATOR = new Creator<AvatarResorce>() {
        @Override
        public AvatarResorce createFromParcel(Parcel in) {
            return new AvatarResorce(in);
        }

        @Override
        public AvatarResorce[] newArray(int size) {
            return new AvatarResorce[size];
        }
    };

    public SparseArray<Entry> data;

    public AvatarResorce() {
        this.data = new SparseArray<>();
    }

    protected AvatarResorce(Parcel in) {
        int size = in.readInt();
        data = new SparseArray<>(size);

        for (int i = 0; i < size; i++) {
            int key = in.readInt();
            Entry obj = in.readParcelable(Entry.class.getClassLoader());
            data.put(key, obj);
        }
    }

    public Entry findById(int id) {
        return data.get(id);
    }

    public void putList(@NonNull List<Entry> entryList) {
        for (Entry entry : entryList) {
            data.put(entry.ownerId, entry);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(data.size());

        for (int i = 0; i < data.size(); i++) {
            int key = data.keyAt(i);
            Entry obj = data.get(key);

            dest.writeInt(key);
            dest.writeParcelable(obj, flags);
        }
    }

    @NonNull
    public Set<Integer> createContainedIdsSet() {
        Set<Integer> ids = new HashSet<>(data.size());
        for (int i = 0; i < data.size(); i++) {
            ids.add(data.keyAt(i));
        }

        return ids;
    }

    public static class Entry implements Parcelable {

        public static final Creator<Entry> CREATOR = new Creator<Entry>() {
            @Override
            public Entry createFromParcel(Parcel in) {
                return new Entry(in);
            }

            @Override
            public Entry[] newArray(int size) {
                return new Entry[size];
            }
        };

        public int ownerId;
        public String hash;

        public Entry(int ownerId, String hash) {
            this.ownerId = ownerId;
            this.hash = hash;
        }

        protected Entry(Parcel in) {
            this.ownerId = in.readInt();
            this.hash = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(ownerId);
            dest.writeString(hash);
        }
    }
}
