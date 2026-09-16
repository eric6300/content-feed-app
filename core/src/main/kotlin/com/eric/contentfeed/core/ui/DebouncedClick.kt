package com.eric.contentfeed.core.ui

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

const val DEFAULT_CLICK_DEBOUNCE_MILLIS: Long = 300L

internal class ClickDebouncer(
    private val debounceMillis: Long,
    private val uptimeMillis: () -> Long = SystemClock::uptimeMillis,
) {
    private var lastAcceptedUptimeMillis: Long? = null

    fun shouldAccept(): Boolean {
        val now = uptimeMillis()
        val last = lastAcceptedUptimeMillis
        if (last != null && now - last < debounceMillis) return false
        lastAcceptedUptimeMillis = now
        return true
    }
}

// Ignores repeat taps within debounceMillis so a double-tap can't push the same destination twice.
@Composable
fun Modifier.click(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    debounceMillis: Long = DEFAULT_CLICK_DEBOUNCE_MILLIS,
    onClick: () -> Unit,
): Modifier {
    val debouncer = remember(debounceMillis) { ClickDebouncer(debounceMillis) }
    return clickable(enabled = enabled, onClickLabel = onClickLabel, role = role) {
        if (debouncer.shouldAccept()) onClick()
    }
}
