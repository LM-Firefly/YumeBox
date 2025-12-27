package com.github.yumelira.yumebox.data.store

import com.tencent.mmkv.MMKV
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class ProfileLink(
    val id: String,
    val name: String,
    val url: String
)

enum class LinkOpenMode {
    IN_APP,
    EXTERNAL_BROWSER
}

class ProfileLinksStorage(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    private val json = Json { ignoreUnknownKeys = true }

    val linkOpenMode by enumFlow(LinkOpenMode.IN_APP)

    val links by jsonListFlow(
        default = emptyList(),
        decode = { str -> decodeFromString<List<ProfileLink>>(str) },
        encode = { value -> encodeToString(value) }
    )
    
    val defaultLinkId by strFlow(default = "")
}
