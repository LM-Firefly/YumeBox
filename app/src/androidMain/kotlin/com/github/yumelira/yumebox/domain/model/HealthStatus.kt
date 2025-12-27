package com.github.yumelira.yumebox.domain.model

data class HealthStatus(
    val isHealthy: Boolean = true,
    val lastCheckTime: Long = System.currentTimeMillis(),
    val message: String? = null
)