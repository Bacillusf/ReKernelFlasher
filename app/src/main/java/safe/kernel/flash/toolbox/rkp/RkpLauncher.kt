package safe.kernel.flash.toolbox.rkp

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

/**
 * 负责准备 rkpdapp.apk 并拼出与原工具等价的启动命令。
 *
 * 原工具的启动链路（已实测确认）：
 *   librkp.so  /system/bin/app_process -Djava.class.path='<APK>' / \
 *       --nice-name=com.android.rkpdapp rp.rkp.rkpd '<rkpdapp.apk>' [dump]
 *
 * 本集成把开头的 librkp.so 换成三选一（见 [wrap]）：
 *   1) 自带 libnsexec.so（同目录 jniLibs 里的自研 setns 助手）
 *   2) 系统 toybox 的 nsenter
 *   3) 直连（多数 root 环境本身已在 init 的 mount namespace 中）
 */
object RkpLauncher {

    const val ASSET = "rkpdapp.apk"
    const val MAIN_CLASS = "safe.kernel.flash.toolbox.rkp.RkpMain"
    const val NICE_NAME = "com.android.rkpdapp"

    /** 系统里可能存在的 rkpdapp 包名 */
    val CANDIDATE_PACKAGES = listOf(
        "com.android.rkpdapp",
        "com.google.android.rkpdapp",
    )

    /** 兜底的硬编码路径（优先用 PackageManager 查到的真实路径） */
    private val SYSTEM_RKPD = listOf(
        "/apex/com.android.rkpd/priv-app/rkpdapp/rkpdapp.apk",
        "/system/apex/com.android.rkpd/priv-app/rkpdapp/rkpdapp.apk",
        "/system/priv-app/rkpdapp/rkpdapp.apk",
    )

    /**
     * 取 rkpdapp.apk，按可靠性依次尝试：
     *   1) 内嵌 assets/rkpdapp.apk（释放到 cacheDir）
     *   2) PackageManager 查询设备上已安装 rkpdapp 的真实 APK 路径
     *   3) 若干硬编码的 APEX / priv-app 路径
     * 全部失败返回 null。
     */
    fun materialize(context: Context): File? {
        val dst = File(context.cacheDir, ASSET)
        try {
            context.assets.open(ASSET).use { input ->
                val len = input.available()
                if (dst.exists() && dst.length() == len.toLong()) return dst
                dst.outputStream().use { out -> input.copyTo(out) }
                return dst
            }
        } catch (_: Exception) {
            // assets 里没放，继续往下找
        }
        installedRkpdApp(context)?.let { return it }
        return systemRkpdApp()
    }

    /** 问 PackageManager 要已安装 rkpdapp 的 APK 路径（ApplicationInfo.sourceDir） */
    fun installedRkpdApp(context: Context): File? {
        val pm = context.packageManager
        for (pkg in CANDIDATE_PACKAGES) {
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                val path = ai.sourceDir
                if (!path.isNullOrEmpty()) {
                    val f = File(path)
                    if (f.exists() && f.length() > 0) return f
                }
            } catch (_: PackageManager.NameNotFoundException) {
                // 没装就试下一个
            } catch (_: Exception) {
                // 其它异常同样跳过
            }
        }
        return null
    }

    fun systemRkpdApp(): File? =
        SYSTEM_RKPD.map { File(it) }.firstOrNull { it.exists() }

    /** 自带 setns 助手的路径（jniLibs 里以 .so 之名打包的可执行文件） */
    fun nativeHelper(context: Context): String? {
        val f = File(context.applicationInfo.nativeLibraryDir, "libnsexec.so")
        return if (f.exists()) f.absolutePath else null
    }

    fun hasSystemNsenter(): Boolean = File("/system/bin/nsenter").exists()

    fun helperDescription(context: Context): String = when {
        nativeHelper(context) != null -> "libnsexec.so（自带）"
        hasSystemNsenter() -> "nsenter（系统 toybox）"
        else -> "直连（无命名空间切换）"
    }

    /** app_process 命令本体（不含前缀） */
    fun appProcess(context: Context, rkpdApk: File?, extra: String?): String {
        val sb = StringBuilder(256)
        sb.append("/system/bin/app_process")
        sb.append(" -Djava.class.path=").append(RkpShell.q(context.applicationInfo.publicSourceDir))
        sb.append(" / --nice-name=").append(NICE_NAME).append(' ')
        sb.append(MAIN_CLASS)
        rkpdApk?.let { sb.append(' ').append(RkpShell.q(it.absolutePath)) }
        val e = extra?.trim().orEmpty()
        if (e.isNotEmpty()) sb.append(' ').append(RkpShell.q(e))
        return sb.toString()
    }

    /** 按可用手段包一层命名空间切换 */
    fun wrap(context: Context, inner: String): String {
        nativeHelper(context)?.let { return RkpShell.q(it) + " " + inner }
        if (hasSystemNsenter()) return "nsenter -t 1 -m -- $inner"
        return inner
    }

    /** 完整命令 */
    fun fullCommand(context: Context, extra: String?): String =
        wrap(context, appProcess(context, materialize(context), extra))
}
