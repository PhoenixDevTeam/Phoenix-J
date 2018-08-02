package biz.dealnote.xmpp.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class User implements Parcelable, Identificable {

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    private int id;
    private String jid;
    private String firstName;
    private String lastName;
    private String middleName;
    private String prefix;
    private String suffix;
    private String emailHome;
    private String emailWork;
    private String organization;
    private String organizationUnit;
    private String photoMimeType;
    private String photoHash;

    public User() {

    }

    protected User(Parcel in) {
        id = in.readInt();
        jid = in.readString();
        firstName = in.readString();
        lastName = in.readString();
        middleName = in.readString();
        prefix = in.readString();
        suffix = in.readString();
        emailHome = in.readString();
        emailWork = in.readString();
        organization = in.readString();
        organizationUnit = in.readString();
        photoMimeType = in.readString();
        photoHash = in.readString();
    }

    public String getJid() {
        return jid;
    }

    public User setJid(String jid) {
        this.jid = jid;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public User setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public User setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getMiddleName() {
        return middleName;
    }

    public User setMiddleName(String middleName) {
        this.middleName = middleName;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    public User setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String getSuffix() {
        return suffix;
    }

    public User setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public String getEmailHome() {
        return emailHome;
    }

    public User setEmailHome(String emailHome) {
        this.emailHome = emailHome;
        return this;
    }

    public String getEmailWork() {
        return emailWork;
    }

    public User setEmailWork(String emailWork) {
        this.emailWork = emailWork;
        return this;
    }

    public String getOrganization() {
        return organization;
    }

    public User setOrganization(String organization) {
        this.organization = organization;
        return this;
    }

    public String getOrganizationUnit() {
        return organizationUnit;
    }

    public User setOrganizationUnit(String organizationUnit) {
        this.organizationUnit = organizationUnit;
        return this;
    }

    public String getPhotoMimeType() {
        return photoMimeType;
    }

    public User setPhotoMimeType(String photoMimeType) {
        this.photoMimeType = photoMimeType;
        return this;
    }

    public String getPhotoHash() {
        return photoHash;
    }

    public User setPhotoHash(String photoHash) {
        this.photoHash = photoHash;
        return this;
    }

    public String getDispayName() {
        if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName)) {
            return jid;
        }

        if (TextUtils.isEmpty(firstName)) {
            return lastName;
        }

        if (TextUtils.isEmpty(lastName)) {
            return firstName;
        }

        return firstName + " " + lastName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(jid);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(middleName);
        dest.writeString(prefix);
        dest.writeString(suffix);
        dest.writeString(emailHome);
        dest.writeString(emailWork);
        dest.writeString(organization);
        dest.writeString(organizationUnit);
        dest.writeString(photoMimeType);
        dest.writeString(photoHash);
    }

    @Override
    public int getId() {
        return id;
    }

    public User setId(int id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}