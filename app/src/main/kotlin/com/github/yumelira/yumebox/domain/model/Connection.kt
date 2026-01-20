package com.github.yumelira.yumebox.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Connection(
    val id: String,
    val download: Long,
    val upload: Long,
    val chains: List<String>,
    val rule: String,
    val rulePayload: String,
    val start: String,
    val metadata: ConnectionMetadata,
    var downloadSpeed: Long = 0,
    var uploadSpeed: Long = 0
)

@Serializable
data class ConnectionMetadata(
    val network: String,
    val type: String,
    val sourceIP: String,
    val sourcePort: String,
    val destinationIP: String,
    val destinationPort: String,
    val host: String,
    val dnsMode: String,
    val process: String = "",
    val processPath: String = ""
)

@Serializable
data class ConnectionsSnapshot(
    val connections: List<Connection>? = null,
    val downloadTotal: Long,
    val uploadTotal: Long
)
