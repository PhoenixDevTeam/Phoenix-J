package biz.dealnote.xmpp.service

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Created by admin on 07.11.2016.
 * phoenix-for-xmpp
 */
class StringArray : Parcelable {

    val array: Array<String>

    constructor(vararg array : String) {
        this.array = Array(array.size) { index -> array[index]}
    }

    private constructor(parcel: Parcel) {
        array = parcel.createStringArray()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeStringArray(array)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as StringArray

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(array, that.array)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(array)
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<StringArray> = object : Parcelable.Creator<StringArray> {
            override fun createFromParcel(parcel: Parcel): StringArray {
                return StringArray(parcel)
            }

            override fun newArray(size: Int): Array<StringArray?> {
                return arrayOfNulls(size)
            }
        }
    }
}