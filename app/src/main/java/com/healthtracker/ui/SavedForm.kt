package com.healthtracker.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.Serializable

/**
 * Form state that survives Android closing the app in the background: it lives in the
 * [SavedStateHandle] and is saved on every change. A saved copy is reused only if [usable]
 * (e.g. not one captured mid-load). Returns the state and whether it was restored.
 */
fun <T : Serializable> SavedStateHandle.savedForm(
    key: String,
    initial: T,
    usable: (T) -> Boolean,
): Pair<MutableStateFlow<T>, Boolean> {
    val saved = get<T>(key)?.takeIf(usable)
    if (saved == null) remove<T>(key)
    return getMutableStateFlow(key, saved ?: initial) to (saved != null)
}
