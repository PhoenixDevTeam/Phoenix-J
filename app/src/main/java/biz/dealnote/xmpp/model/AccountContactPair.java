package biz.dealnote.xmpp.model;

import android.os.Parcel;
import android.os.Parcelable;

public class AccountContactPair implements Parcelable {

    public static final Creator<AccountContactPair> CREATOR = new Creator<AccountContactPair>() {
        @Override
        public AccountContactPair createFromParcel(Parcel in) {
            return new AccountContactPair(in);
        }

        @Override
        public AccountContactPair[] newArray(int size) {
            return new AccountContactPair[size];
        }
    };

    public Account account;
    public User user;

    public AccountContactPair(Account account) {
        this.account = account;
    }

    protected AccountContactPair(Parcel in) {
        account = in.readParcelable(Account.class.getClassLoader());
        user = in.readParcelable(User.class.getClassLoader());
    }

    public AccountContactPair setUser(User user) {
        this.user = user;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(account, flags);
        dest.writeParcelable(user, flags);
    }

    @Override
    public String toString() {
        return "AccountContactPair{" +
                "account=" + account +
                ", user=" + user +
                '}';
    }
}
