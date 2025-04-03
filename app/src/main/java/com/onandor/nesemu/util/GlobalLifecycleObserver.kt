package com.onandor.nesemu.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleOwner
import com.onandor.nesemu.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class GlobalLifecycleObserver(
    @DefaultDispatcher private val defaultScope: CoroutineScope
) : DefaultLifecycleObserver {

    private val _events: MutableSharedFlow<Event> = MutableSharedFlow()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private fun emit(event: Event) {
        defaultScope.launch { _events.emit(event) }
    }

    override fun onCreate(owner: LifecycleOwner) = emit(Event.ON_CREATE)

    override fun onStart(owner: LifecycleOwner) = emit(Event.ON_START)

    override fun onResume(owner: LifecycleOwner) = emit(Event.ON_RESUME)

    override fun onPause(owner: LifecycleOwner) = emit(Event.ON_PAUSE)

    override fun onStop(owner: LifecycleOwner) = emit(Event.ON_STOP)

    override fun onDestroy(owner: LifecycleOwner) = emit(Event.ON_DESTROY)
}