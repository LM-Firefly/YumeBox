package com.github.yumelira.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.github.yumelira.yumebox.core.util.Parcelizer

@Serializable
data class UiConfiguration(
    @SerialName("port")
    val port: Int = 0,

    @SerialName("socks-port")
    val socksPort: Int = 0,

    @SerialName("redir-port")
    val redirPort: Int = 0,

    @SerialName("tproxy-port")
    val tproxyPort: Int = 0,

    @SerialName("mixed-port")
    val mixedPort: Int = 0,

    @SerialName("allow-lan")
    val allowLan: Boolean = false,

    @SerialName("ipv6")
    val ipv6: Boolean = false,

    @SerialName("mode")
    val mode: TunnelState.Mode = TunnelState.Mode.Rule,

    @SerialName("log-level")
    val logLevel: LogMessage.Level = LogMessage.Level.Info,

    @SerialName("external-controller")
    val externalController: String? = null,

    @SerialName("secret")
    val secret: String? = null
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UiConfiguration> {
        override fun createFromParcel(parcel: Parcel): UiConfiguration {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<UiConfiguration?> {
            return arrayOfNulls(size)
        }
    }
}
