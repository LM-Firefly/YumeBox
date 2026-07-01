package com.github.yumelira.yumebox.core.model

import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerialName

@Serializable(with = OverrideContentTypeSerializer::class)
enum class OverrideContentType(val extension: String) {
    @SerialName("yaml") Yaml("yaml"),
    @SerialName("js") JavaScript("js");

    companion object {
        fun fromExtension(extension: String?): OverrideContentType? =
            when (extension?.lowercase()?.removePrefix(".")) {
                "yaml",
                "yml" -> Yaml
                "js" -> JavaScript
                else -> null
            }

        fun fromFileName(fileName: String?): OverrideContentType? =
            fromExtension(fileName?.substringAfterLast('.', missingDelimiterValue = ""))
    }
}

object OverrideContentTypeSerializer : KSerializer<OverrideContentType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OverrideContentType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: OverrideContentType) {
        encoder.encodeString(value.extension)
    }
    override fun deserialize(decoder: Decoder): OverrideContentType {
        val raw = decoder.decodeString().trim()
        return when (raw.lowercase()) {
            "yaml", "yml" -> OverrideContentType.Yaml
            "js", "javascript" -> OverrideContentType.JavaScript
            else -> {
                runCatching { OverrideContentType.valueOf(raw) }
                    .getOrElse {
                        throw SerializationException("Unknown OverrideContentType: $raw")
                    }
            }
        }
    }
}
