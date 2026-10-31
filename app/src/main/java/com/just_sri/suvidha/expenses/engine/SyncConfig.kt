package com.just_sri.suvidha.expenses.engine

data class SyncConfig(
    val foregroundTimeoutMs: Long = 3500L,
    val relayTimeoutMs: Long = 8000L,
    val parallelRelayQueries: Boolean = true,
)
