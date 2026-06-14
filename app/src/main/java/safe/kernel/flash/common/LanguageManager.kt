package safe.kernel.flash.common

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    private const val PREFS_NAME = "rekf_settings"
    private const val KEY_LANGUAGE = "language"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCurrentLanguage(): String {
        return prefs?.getString(KEY_LANGUAGE, "zh") ?: "zh"
    }

    fun setLanguage(language: String) {
        prefs?.edit()?.putString(KEY_LANGUAGE, language)?.apply()
        val locale = when (language) {
            "en" -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.forLanguageTags("zh")
        }
        AppCompatDelegate.setApplicationLocales(locale)
    }

    fun applySavedLanguage() {
        setLanguage(getCurrentLanguage())
    }
}
