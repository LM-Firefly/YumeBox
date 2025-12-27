package com.github.yumelira.yumebox.data.model

import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.serialization.Serializable
import java.util.*


@Serializable
enum class ProfileType {
    URL, FILE,
}

@Serializable
data class Subscription(
    val id: String = UUID.randomUUID().toString(),
    val name: String = MLang.ProfilesPage.Message.MySubscription,
    val provider: String = MLang.ProfilesPage.Provider.Unknown,
    val enabled: Boolean = true,
    val plan: String? = null,
    val expireAt: Long? = null,
    val usedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val remainingBytes: Long? = null,
    val subscribeUrl: String? = null,
    val updateUrl: String? = null,
    val exportUrl: String? = null,
    val lastUpdatedAt: Long? = null,
    val autoUpdate: Boolean = false
)

@Serializable
data class Profile(
    val id: String,
    val name: String = "",
    val config: String = "",
    val remoteUrl: String? = null,
    val type: ProfileType = ProfileType.URL,
    val enabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val provider: String? = null,
    val expireAt: Long? = null,
    val usedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val lastUpdatedAt: Long? = null,
    val autoUpdateMinutes: Int = 0,
) {
    fun getDisplayProvider(): String = when (type) {
        ProfileType.URL -> provider ?: MLang.ProfilesPage.Provider.Remote
        ProfileType.FILE -> MLang.ProfilesPage.Provider.Local
    }

    fun getInfoText(): String = when (type) {
        ProfileType.URL -> {
            buildString {
                if (totalBytes != null && totalBytes > 0) {
                    val usedPercent = (usedBytes * 100 / totalBytes).toInt()
                    append(MLang.ProfilesPage.Info.TrafficFormat.format(
                        com.github.yumelira.yumebox.common.util.ByteFormatter.format(usedBytes),
                        com.github.yumelira.yumebox.common.util.ByteFormatter.format(totalBytes),
                        usedPercent
                    ))
                } else if (usedBytes > 0) {
                    append(MLang.ProfilesPage.Info.UsedBytes.format(com.github.yumelira.yumebox.common.util.ByteFormatter.format(usedBytes)))
                } else {
                    append(MLang.ProfilesPage.Info.ClickToUpdate)
                }

                expireAt?.let { expire ->
                    val expireDate =
                        java.time.Instant.ofEpochMilli(expire).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    val now = java.time.LocalDate.now()
                    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, expireDate)

                    if (isNotEmpty()) append("\n")

                    if (daysLeft > 0) {
                        append(MLang.ProfilesPage.Info.ExpireAt.format(expireDate, daysLeft))
                    } else if (daysLeft == 0L) {
                        append(MLang.ProfilesPage.Info.ExpiresToday)
                    } else {
                        append(MLang.ProfilesPage.Info.Expired.format(expireDate))
                    }
                }

                lastUpdatedAt?.let { updated ->
                    append(MLang.ProfilesPage.Info.UpdatedSuffix.format(getRelativeTimeString(updated)))
                }
            }
        }

        ProfileType.FILE -> MLang.ProfilesPage.Info.Local
    }

    private fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)

        return when {
            diff < 60 * 1000 -> MLang.ProfilesPage.Time.JustNow
            minutes < 60 -> MLang.ProfilesPage.Time.MinutesAgo.format(minutes)
            hours < 24 -> MLang.ProfilesPage.Time.HoursAgo.format(hours)
            else -> {
                val days = diff / (1000 * 60 * 60 * 24)
                MLang.ProfilesPage.Time.DaysAgo.format(days)
            }
        }
    }

    fun shouldShowUpdateButton(): Boolean = type == ProfileType.URL

}
