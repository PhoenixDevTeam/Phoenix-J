package biz.dealnote.xmpp.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Contact implements Parcelable, Identificable {

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
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
    //private byte[] avatar;

    public Contact() {
    }

    protected Contact(Parcel in) {
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
        //avatar = in.createByteArray();
    }

    public String getJid() {
        return jid;
    }

    public Contact setJid(String jid) {
        this.jid = jid;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public Contact setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public Contact setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getMiddleName() {
        return middleName;
    }

    public Contact setMiddleName(String middleName) {
        this.middleName = middleName;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    public Contact setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String getSuffix() {
        return suffix;
    }

    public Contact setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public String getEmailHome() {
        return emailHome;
    }

    public Contact setEmailHome(String emailHome) {
        this.emailHome = emailHome;
        return this;
    }

    public String getEmailWork() {
        return emailWork;
    }

    public Contact setEmailWork(String emailWork) {
        this.emailWork = emailWork;
        return this;
    }

    public String getOrganization() {
        return organization;
    }

    public Contact setOrganization(String organization) {
        this.organization = organization;
        return this;
    }

    public String getOrganizationUnit() {
        return organizationUnit;
    }

    public Contact setOrganizationUnit(String organizationUnit) {
        this.organizationUnit = organizationUnit;
        return this;
    }

    public String getPhotoMimeType() {
        return photoMimeType;
    }

    public Contact setPhotoMimeType(String photoMimeType) {
        this.photoMimeType = photoMimeType;
        return this;
    }

    public String getPhotoHash() {
        return photoHash;
    }

    public Contact setPhotoHash(String photoHash) {
        this.photoHash = photoHash;
        return this;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "id=" + id +
                ", jid='" + jid + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                ", emailHome='" + emailHome + '\'' +
                ", emailWork='" + emailWork + '\'' +
                ", organization='" + organization + '\'' +
                ", organizationUnit='" + organizationUnit + '\'' +
                ", photoMimeType='" + photoMimeType + '\'' +
                ", photoHash='" + photoHash +
                '}';
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
        //dest.writeByteArray(avatar);
    }

    @Override
    public int getId() {
        return id;
    }

    public Contact setId(int id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return id == contact.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}
