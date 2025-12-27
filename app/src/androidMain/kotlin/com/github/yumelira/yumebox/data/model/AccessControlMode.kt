package com.github.yumelira.yumebox.data.model

/**
 * 访问控制模式
 */
enum class AccessControlMode {
    /**
     * 允许所有访问
     */
    ALLOW_ALL,

    /**
     * 允许特定访问
     */
    ALLOW_SPECIFIC,

    /**
     * 拒绝特定访问
     */
    DENY_SPECIFIC
}
