package org.mozilla.xiu.browser.fxa

data class SyncState(
    val error: Boolean = false,
    val idle:Boolean = false,
    val start:Boolean = false
    ):AccountState

sealed class SyncLooper {
}