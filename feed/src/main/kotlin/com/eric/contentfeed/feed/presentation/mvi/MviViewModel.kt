package com.eric.contentfeed.feed.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val EFFECT_BUFFER_CAPACITY = 64

/** Small event-serialising MVI base used by every feature presentation model. */
abstract class MviViewModel<State : Any, Event : Any, Effect : Any>(
    initialState: State,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    private val eventChannel = Channel<Event>(Channel.UNLIMITED)

    // An explicit integer capacity, not Channel.BUFFERED: combining BUFFERED with a
    // non-SUSPEND overflow policy coerces capacity to 1, which would near-conflate
    // effects instead of buffering 64 of them.
    private val effectChannel =
        Channel<Effect>(
            capacity = EFFECT_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    val state: StateFlow<State> = mutableState.asStateFlow()

    // Routes can leave and re-enter composition when a root tab changes. A
    // receiveAsFlow can be collected again after the previous collector is
    // cancelled; consumeAsFlow permanently marks the channel as consumed.
    val effects: Flow<Effect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            eventChannel.receiveAsFlow().collect(::handleEvent)
        }
    }

    fun onEvent(event: Event) {
        eventChannel.trySend(event)
    }

    protected abstract suspend fun handleEvent(event: Event)

    protected fun updateState(reducer: (State) -> State) {
        mutableState.update(reducer)
    }

    protected fun emitEffect(effect: Effect) {
        effectChannel.trySend(effect)
    }
}
