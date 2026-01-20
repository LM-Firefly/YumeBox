package com.github.yumelira.yumebox.substore

data class Request(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val body: String
)
