package com.androkiosk.data

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

private const val FILE = "language"
private const val KEY = "tag"

enum class Language(val tag: String, val label: String) {
    English("en", "English"),
    Polish("pl", "Polski"),
}

object LanguagePreference {
    fun read(context: Context): Language {
        val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
        return Language.entries.firstOrNull { it.tag == stored } ?: deviceDefault()
    }

    fun write(context: Context, language: Language) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putString(KEY, language.tag) }
    }

    private fun deviceDefault() =
        Language.entries.firstOrNull { it.tag == Locale.getDefault().language } ?: Language.English
}

fun Context.withLanguage(language: Language): Context {
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    return createConfigurationContext(configuration)
}

tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
