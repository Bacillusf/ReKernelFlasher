package safe.kernel.flash.common

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "rekf_settings"
    private const val KEY_LANGUAGE = "language"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getCurrentLanguage(): String {
        return prefs?.getString(KEY_LANGUAGE, "zh-CN") ?: "zh-CN"
    }

    fun setLanguage(language: String, activity: Activity) {
        prefs?.edit()?.putString(KEY_LANGUAGE, language)?.apply()
        Handler(Looper.getMainLooper()).postDelayed({
            activity.recreate()
        }, 100)
    }

    fun applyContextLocale(context: Context): Context {
        val language = getCurrentLanguage()
        val tag = when (language) {
            "en" -> "en"
            "zh-TW" -> "zh-TW"
            "zh-HK" -> "zh-HK"
            else -> "zh"
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
}
