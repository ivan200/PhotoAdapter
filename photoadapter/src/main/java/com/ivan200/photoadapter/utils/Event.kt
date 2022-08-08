package com.ivan200.photoadapter.utils

import java.util.concurrent.atomic.AtomicBoolean

/**
 *  Used as a wrapper for data that is exposed via a LiveData that represents an event.
 *
 * @author ivan200
 * @since 06.08.2022
 */
open class Event<out T>(val content: T) {

    private val handled = AtomicBoolean(false)
    val isHandled: Boolean get() = handled.get()

    /**
     * Returns the content and prevents its use again.
     */
    fun get(): T? = if (handled.compareAndSet(false, true)) content else null
}
