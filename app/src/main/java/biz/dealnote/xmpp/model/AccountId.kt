package biz.dealnote.xmpp.model

import android.os.Parcel
import android.os.Parcelable

class AccountId(val id: Int, val jid: String): Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(jid)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AccountId> {
        override fun createFromParcel(parcel: Parcel): AccountId {
            return AccountId(parcel)
        }

        override fun newArray(size: Int): Array<AccountId?> {
            return arrayOfNulls(size)
        }
    }
}