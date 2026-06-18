package safe.kernel.flash.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class UiMode { Classic, Miuix }

object UiModeManager {
    private const val PREFS = "rekf_settings"
    private const val KEY = "ui_mode"

    var current by mutableStateOf(UiMode.Classic)
        private set

    fun init(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "classic")
        current = if (saved == "miuix") UiMode.Miuix else UiMode.Classic
    }

    fun setMode(mode: UiMode, context: Context) {
        current = mode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, if (mode == UiMode.Miuix) "miuix" else "classic").apply()
    }
}

val LocalUiMode = compositionLocalOf { UiMode.Classic }
