package com.androkiosk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.androkiosk.AndrokioskApp
import com.androkiosk.data.KioskPreferences
import com.androkiosk.security.PinCrypto
import com.androkiosk.security.PinDigest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KioskViewModel(private val preferences: KioskPreferences) : ViewModel() {
    val settings = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setKioskEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setKioskEnabled(enabled) }
    }

    fun setPinningNoticeHidden(hidden: Boolean) {
        viewModelScope.launch { preferences.setPinningNoticeHidden(hidden) }
    }

    fun setTripleBack(enabled: Boolean) {
        viewModelScope.launch { preferences.setTripleBack(enabled) }
    }

    fun setPin(pin: String) {
        viewModelScope.launch { preferences.setPin(PinCrypto.digest(pin)) }
    }

    fun clearPin() {
        viewModelScope.launch { preferences.setPin(null) }
    }

    fun matches(pin: String, digest: PinDigest) = PinCrypto.matches(pin, digest)

    companion object {
        val Factory = viewModelFactory {
            initializer { KioskViewModel((this[APPLICATION_KEY] as AndrokioskApp).preferences) }
        }
    }
}
