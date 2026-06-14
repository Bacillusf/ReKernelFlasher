package safe.kernel.flash.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    private const val PREFS_NAME = "rekf_settings"
    private const val KEY_LANGUAGE = "language"

    private var prefs: SharedPreferences? = null
    private var activityClass: Class<out Activity>? = null

    fun init(context: Context, activityClass: Class<out Activity>) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        this.activityClass = activityClass
    }

    fun getCurrentLanguage(): String {
        return prefs?.getString(KEY_LANGUAGE, "zh-CN") ?: "zh-CN"
    }

    fun setLanguage(language: String, context: Context) {
        prefs?.edit()?.putString(KEY_LANGUAGE, language)?.apply()
        val locale = when (language) {
            "en" -> LocaleListCompat.forLanguageTags("en")
            "zh-TW" -> LocaleListCompat.forLanguageTags("zh-TW")
            "zh-HK" -> LocaleListCompat.forLanguageTags("zh-HK")
            else -> LocaleListCompat.forLanguageTags("zh-CN")
        }
        AppCompatDelegate.setApplicationLocales(locale)
        val cls = activityClass ?: return
        val intent = Intent(context, cls).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        (context as? Activity)?.finish()
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
