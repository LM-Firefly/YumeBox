package com.github.yumelira.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.github.yumelira.yumebox.core.util.Parcelizer

@Serializable
data class SubscriptionInfo(
    @SerialName("Upload") val upload: Long = 0,
    @SerialName("Download") val download: Long = 0,
    @SerialName("Total") val total: Long = 0,
    @SerialName("Expire") val expire: Long = 0
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }
    override fun describeContents(): Int {
        return 0
    }
    companion object CREATOR : Parcelable.Creator<SubscriptionInfo> {
        override fun createFromParcel(parcel: Parcel): SubscriptionInfo {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }
        override fun newArray(size: Int): Array<SubscriptionInfo?> {
            return arrayOfNulls(size)
        }
    }
}

@Serializable
data class Provider(
    val name: String,
    val type: Type,
    val vehicleType: VehicleType,
    val updatedAt: Long,
    val path: String = "",
    val subscriptionInfo: SubscriptionInfo? = null,
) : Parcelable, Comparable<Provider> {
    enum class Type {
        Proxy, Rule
    }

    enum class VehicleType {
        HTTP, File, Inline, Compatible
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun compareTo(other: Provider): Int {
        return compareValuesBy(this, other, Provider::type, Provider::name)
    }

    companion object CREATOR : Parcelable.Creator<Provider> {
        override fun createFromParcel(parcel: Parcel): Provider {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<Provider?> {
            return arrayOfNulls(size)
        }
    }
}
