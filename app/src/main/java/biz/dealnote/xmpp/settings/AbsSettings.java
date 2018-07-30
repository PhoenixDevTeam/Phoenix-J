package biz.dealnote.xmpp.settings;

import android.os.Parcel;
import android.os.Parcelable;

public class AbsSettings implements Parcelable {

    public static final Creator<AbsSettings> CREATOR = new Creator<AbsSettings>() {
        @Override
        public AbsSettings createFromParcel(Parcel in) {
            return new AbsSettings(in);
        }

        @Override
        public AbsSettings[] newArray(int size) {
            return new AbsSettings[size];
        }
    };
    public Section section;

    public AbsSettings(Section section) {
        this.section = section;
    }

    protected AbsSettings(Parcel in) {
        section = in.readParcelable(Section.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(section, flags);
    }

    public static class Section implements Parcelable {

        public static final Creator<Section> CREATOR = new Creator<Section>() {
            @Override
            public Section createFromParcel(Parcel in) {
                return new Section(in);
            }

            @Override
            public Section[] newArray(int size) {
                return new Section[size];
            }
        };

        public int id;
        public int titleRes;

        public Section(int id, int titleRes) {
            this.id = id;
            this.titleRes = titleRes;
        }

        protected Section(Parcel in) {
            id = in.readInt();
            titleRes = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(id);
            dest.writeInt(titleRes);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Section type = (Section) o;
            return id == type.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }
}
