package biz.dealnote.xmpp.settings;

import android.os.Parcel;
import android.os.Parcelable;

import biz.dealnote.xmpp.model.AccountContactPair;

public class AccountSettings extends AbsSettings implements Parcelable {

    public static final Creator<AccountSettings> CREATOR = new Creator<AccountSettings>() {
        @Override
        public AccountSettings createFromParcel(Parcel in) {
            return new AccountSettings(in);
        }

        @Override
        public AccountSettings[] newArray(int size) {
            return new AccountSettings[size];
        }
    };

    public AccountContactPair accountContactPair;

    public AccountSettings(Section section, AccountContactPair pair) {
        super(section);
        this.accountContactPair = pair;
    }

    protected AccountSettings(Parcel in) {
        super(in);
        accountContactPair = in.readParcelable(AccountContactPair.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(accountContactPair, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "AccountSettings{" +
                "accountContactPair=" + accountContactPair +
                '}';
    }
}
