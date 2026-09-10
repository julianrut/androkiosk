package com.androkiosk.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.androkiosk.security.PinCrypto
import com.androkiosk.security.PinDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferences by preferencesDataStore("kiosk")

private val KioskEnabled = booleanPreferencesKey("kiosk_enabled")
private val PinningNoticeHidden = booleanPreferencesKey("pinning_notice_hidden")
private val PinSalt = stringPreferencesKey("pin_salt")
private val PinMac = stringPreferencesKey("pin_mac")
private val TripleBack = booleanPreferencesKey("triple_back")
private val BrowserUrl = stringPreferencesKey("browser_url")

data class KioskSettings(
    val kioskEnabled: Boolean,
    val pinningNoticeHidden: Boolean,
    val tripleBack: Boolean,
    val browserUrl: String,
    val pin: PinDigest?,
)

class KioskPreferences(private val context: Context) {
    val settings: Flow<KioskSettings> = context.preferences.data.map { stored ->
        val salt = stored[PinSalt]
        val mac = stored[PinMac]
        KioskSettings(
            kioskEnabled = stored[KioskEnabled] == true,
            pinningNoticeHidden = stored[PinningNoticeHidden] == true,
            tripleBack = stored[TripleBack] != false,
            browserUrl = stored[BrowserUrl].orEmpty(),
            pin = if (salt != null && mac != null && PinCrypto.isReady()) PinDigest(salt, mac) else null,
        )
    }

    suspend fun setKioskEnabled(enabled: Boolean) {
        context.preferences.edit { it[KioskEnabled] = enabled }
    }

    suspend fun setPinningNoticeHidden(hidden: Boolean) {
        context.preferences.edit { it[PinningNoticeHidden] = hidden }
    }

    suspend fun setTripleBack(enabled: Boolean) {
        context.preferences.edit { it[TripleBack] = enabled }
    }

    suspend fun setBrowserUrl(url: String) {
        context.preferences.edit { it[BrowserUrl] = url }
    }

    suspend fun setPin(digest: PinDigest?) {
        context.preferences.edit {
            if (digest == null) {
                it.remove(PinSalt)
                it.remove(PinMac)
            } else {
                it[PinSalt] = digest.salt
                it[PinMac] = digest.mac
            }
        }
    }
}
