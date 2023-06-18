package com.ivan200.photoadapter.utils


import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.reflect.KProperty

/**
 * Утилитный класс для использования `SavedStateViewModel`, и сохранения состояний вьюмодели
 *
 * @author ivan200
 * @since  25.03.2023
 */
object SavedStateUtils {

    /**
     * Lazy initialization of the saved state viewmodel, with parameters via lambda
     *
     * @param storeOwner A scope that owns ViewModelStore.
     * @param registryOwner A scope that owns SavedStateRegistry
     * @param viewModelCreator lambda for creating a viewmodel
     * @param T viewmodel type
     */
    inline fun <reified T : ViewModel> lazySavedStateViewModel(
        storeOwner: ViewModelStoreOwner,
        registryOwner: SavedStateRegistryOwner,
        crossinline viewModelCreator: (SavedStateHandle) -> T
    ): Lazy<T> = lazy {
        ViewModelProvider(storeOwner, object : AbstractSavedStateViewModelFactory(registryOwner, null) {
            override fun <T2 : ViewModel> create(key: String, modelClass: Class<T2>, handle: SavedStateHandle): T2 {
                @Suppress("UNCHECKED_CAST")
                return viewModelCreator.invoke(handle) as T2
            }
        })[T::class.java]
    }

    /**
     * Delegate for the value stored in [SavedStateHandle]
     *
     * @param handle  a handle to saved state passed down to [androidx.lifecycle.ViewModel]
     * @param default default value, if not found in [handle]
     * @param T       the type of the received value
     */
    class SavedValue<T>(private val handle: SavedStateHandle, val default: T) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = handle[property.name] ?: default
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = handle.set(property.name, value)
    }

    /**
     * Delegate for a nullable value stored in [SavedStateHandle]
     *
     * @param handle a handle to saved state passed down to [androidx.lifecycle.ViewModel]
     * @param T      the type of the received value
     */
    class SavedValueNullable<T>(private val handle: SavedStateHandle, val default: T? = null) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = handle[property.name] ?: default
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) = handle.set(property.name, value)
    }

    /**
     * Delegate for [LiveData] received via a [SavedStateHandle]
     */
    class SavedLiveData<T> private constructor(
        private val handle: SavedStateHandle,
        private val default: T?,
        private val hasInitialValue: Boolean
    ) {
        /** Creating or receiving live data from [handle] */
        constructor(handle: SavedStateHandle) : this(handle, null, false)

        /** Creating or receiving live data from [handle] with an [initialValue] */
        constructor(handle: SavedStateHandle, initialValue: T) : this(handle, initialValue, true)

        operator fun getValue(thisRef: Any?, property: KProperty<*>) = if (hasInitialValue) {
            @Suppress("UNCHECKED_CAST")
            handle.getLiveData(property.name, default as T)
        } else {
            handle.getLiveData(property.name)
        }
    }

    /**
     * Delegate for [StateFlow] received via a [SavedStateHandle]
     *
     * @param handle  a handle to saved state passed down to [androidx.lifecycle.ViewModel]
     * @param default default value, if not found in [handle]
     * @param T       the type of the received value
     */
    class SavedStateFlow<T>(private val handle: SavedStateHandle, val default: T) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = handle.getStateFlow(property.name, default)
    }
}