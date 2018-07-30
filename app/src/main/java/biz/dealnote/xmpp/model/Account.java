package biz.dealnote.xmpp.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.security.KeyPair;

import biz.dealnote.xmpp.util.ParcelUtils;

public class Account implements Parcelable, Identificable {

    public static final Creator<Account> CREATOR = new Creator<Account>() {
        @Override
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

    public int id;
    public String login;
    public String password;
    public String host;
    public int port;
    public boolean disabled;
    public KeyPair keyPair;

    public Account(int id, String login, String password, String host, int port, boolean disabled, KeyPair keyPair) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.host = host;
        this.port = port;
        this.disabled = disabled;
        this.keyPair = keyPair;
    }

    protected Account(Parcel in) {
        id = in.readInt();
        login = in.readString();
        password = in.readString();
        host = in.readString();
        port = in.readInt();
        disabled = in.readByte() != 0;
        keyPair = ParcelUtils.readKeyPair(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return id == account.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(login);
        dest.writeString(password);
        dest.writeString(host);
        dest.writeInt(port);
        dest.writeByte((byte) (disabled ? 1 : 0));
        ParcelUtils.writeKeyPair(dest, keyPair);
    }

    public String buildBareJid() {
        return login + "@" + host;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", disabled=" + disabled +
                ", keyPair=" + keyPair +
                '}';
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
}
