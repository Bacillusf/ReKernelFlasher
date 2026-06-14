package safe.kernel.flash.common

import android.app.Activity
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
        return prefs?.getString(KEY_LANGUAGE, "zh-CN") ?: "zh-CN"
    }

    fun setLanguage(language: String, activity: Activity) {
        prefs?.edit()?.putString(KEY_LANGUAGE, language)?.apply()
        val locale = when (language) {
            "en" -> LocaleListCompat.forLanguageTags("en")
            "zh-TW" -> LocaleListCompat.forLanguageTags("zh-TW")
            "zh-HK" -> LocaleListCompat.forLanguageTags("zh-HK")
            else -> LocaleListCompat.forLanguageTags("zh-CN")
        }
        AppCompatDelegate.setApplicationLocales(locale)
        activity.recreate()
    }

    fun applySavedLanguage() {
        val locale = when (getCurrentLanguage()) {
            "en" -> LocaleListCompat.forLanguageTags("en")
            "zh-TW" -> LocaleListCompat.forLanguageTags("zh-TW")
            "zh-HK" -> LocaleListCompat.forLanguageTags("zh-HK")
            else -> LocaleListCompat.forLanguageTags("zh-CN")
        }
        AppCompatDelegate.setApplicationLocales(locale)
    }
}
