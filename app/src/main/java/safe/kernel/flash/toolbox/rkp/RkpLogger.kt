package safe.kernel.flash.toolbox.rkp

import android.util.Log

/** 日志级别（与界面着色一一对应） */
object RkpLevel {
    const val NORMAL = 0
    const val DIM = 1
    const val OK = 2
    const val ERR = 3
    const val WARN = 4
}

/**
 * 日志出口抽象。
 *
 * - 界面进程：实现把行追加到 Compose 状态列表并落盘
 * - rkpd 子进程（app_process 里跑）：实现写到 stdout（被父进程的 shell 捕获）+ logcat
 */
interface RkpLogger {
    fun log(level: Int, line: String)

    fun log(line: String) = log(RkpLevel.NORMAL, line)

    fun text(level: Int, text: String?) {
        if (text.isNullOrEmpty()) return
        text.split("\n").forEach { log(level, it) }
    }
}

/** 写到 stdout + logcat：给 app_process 子进程用 */
class RkpStdoutLogger(private val tag: String = "TrueRkp") : RkpLogger {
    override fun log(level: Int, line: String) {
        println(line)
        val priority = when (level) {
            RkpLevel.ERR -> Log.ERROR
            RkpLevel.WARN -> Log.WARN
            RkpLevel.OK -> Log.INFO
            else -> Log.DEBUG
        }
        Log.println(priority, tag, line)
    }
}
