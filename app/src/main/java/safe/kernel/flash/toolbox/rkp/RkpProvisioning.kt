package safe.kernel.flash.toolbox.rkp

import android.content.Context
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.time.Instant

/**
 * RKP 密钥分配主流程（对应 True RKP 的 Provisioning.java）。
 *
 * 原工具的对应物是 rp.h0（RkpProvisioner，754 行，日志 tag 为 RkpdPeriodicProvisioner），
 * 它直接引用了内嵌 AOSP rkpdapp 的类。本实现不把 AOSP 的类打进 APK，而是**反射调用**
 * 运行期从 rkpdapp.apk 加载进来的那些类，因此对 AOSP 版本差异更宽容。
 *
 * 流程（与原工具实测日志一一对应）：
 *   1. Settings.getUrl / getDefaultUrl            —— 没有配置就直接退出
 *   2. ServiceManagerInterface.getAllInstances    —— 枚举 IRPC（TEE / StrongBox）
 *   3. RkpdDatabase -> ProvisionedKeyDao          —— 清理过期密钥
 *   4. ServerInterface.fetchGeekAndUpdate         —— 取 Geek（服务端配置）
 *   5. numExtraAttestationKeys == 0               —— 服务端要求清空密钥池
 *   6. Provisioner.clearBadAttestationKeys
 *   7. 逐个 SystemInterface 执行 provisionKeys
 *   8. Widevine 证书分配
 */
class RkpProvisioning(
    private val context: Context,
    private val classLoader: ClassLoader,
    private val log: RkpLogger?,
) {

    fun run() {
        try {
            doRun()
        } catch (t: Throwable) {
            line(RkpLevel.ERR, "RKP 分配异常：$t")
            t.cause?.let { line(RkpLevel.ERR, "  原因：$it") }
        }
    }

    @Throws(Exception::class)
    private fun doRun() {
        val settings = classLoader.loadClass("com.android.rkpdapp.utils.Settings")
        val defUrl = invokeStatic(settings, "getDefaultUrl") as? String
        val url = invokeStatic(settings, "getUrl", context) as? String
        line("RKP 服务器：${if (url.isNullOrEmpty()) "(未配置)" else url}")
        if (defUrl.isNullOrEmpty() || url.isNullOrEmpty()) {
            line("系统未配置 RKP 服务器地址，跳过")
            return
        }

        val svcMgr = classLoader.loadClass("com.android.rkpdapp.interfaces.ServiceManagerInterface")
        val instances = invokeStatic(svcMgr, "getAllInstances") as? Array<*>
        if (instances.isNullOrEmpty()) {
            line("没有可用的 IRPC HAL，跳过")
            return
        }
        line("发现 ${instances.size} 个 IRPC 实现")

        val attemptCls = classLoader.loadClass("com.android.rkpdapp.metrics.ProvisioningAttempt")
        val attempt = invokeStatic(attemptCls, "createScheduledAttemptMetrics", context)

        val dbCls = classLoader.loadClass("com.android.rkpdapp.database.RkpdDatabase")
        val db = invokeStatic(dbCls, "getDatabase", context)
        val dao = invoke(db, "provisionedKeyDao")
        val daoIface = classLoader.loadClass("com.android.rkpdapp.database.ProvisionedKeyDao")

        runCatching { invoke(dao, "deleteExpiringKeys", Instant.now()) }
            .onFailure { line(RkpLevel.WARN, "清理过期密钥失败：$it") }

        val serverCls = classLoader.loadClass("com.android.rkpdapp.interfaces.ServerInterface")
        val server = serverCls.getConstructor(Context::class.java, Boolean::class.javaPrimitiveType!!)
            .newInstance(context, true)
        val geek = try {
            invoke(server, "fetchGeekAndUpdate", attempt)
        } catch (t: Throwable) {
            line(RkpLevel.ERR, "从 RKP 服务器获取配置失败：${rootCause(t)}")
            return
        }

        val extra = (getField(geek, "numExtraAttestationKeys") as? Number)?.toInt() ?: 0
        line("服务端允许的额外密钥数：$extra")
        if (extra == 0) {
            invoke(dao, "deleteAllKeys")
            closeQuietly(attempt)
            line(RkpLevel.WARN, "服务端要求停用分配并清空密钥池")
            return
        }

        val provCls = classLoader.loadClass("com.android.rkpdapp.provisioner.Provisioner")
        val provisioner = provCls
            .getConstructor(Context::class.java, daoIface, Boolean::class.javaPrimitiveType!!)
            .newInstance(context, dao, true)

        runCatching { invoke(provisioner, "clearBadAttestationKeys", geek) }
            .onFailure { line(RkpLevel.WARN, "清理异常证明密钥失败：${rootCause(it)}") }

        var ok = 0
        for (iface in instances) {
            iface ?: continue
            line("开始分配：$iface")
            try {
                invoke(provisioner, "provisionKeys", attempt, iface, geek)
                ok++
                line(RkpLevel.OK, "分配成功：$iface")
            } catch (t: Throwable) {
                line(RkpLevel.ERR, "分配失败：${rootCause(t)}")
            }
        }
        closeQuietly(attempt)
        line(
            if (ok > 0) RkpLevel.OK else RkpLevel.WARN,
            "RKP 分配结束：成功 $ok/${instances.size}"
        )

        RkpWidevine(log).provision()
    }

    /** 只读模式：打印密钥池概况（对应原工具的 dump 分支） */
    fun dump() {
        try {
            val dbCls = classLoader.loadClass("com.android.rkpdapp.database.RkpdDatabase")
            val db = invokeStatic(dbCls, "getDatabase", context)
            val dao = invoke(db, "provisionedKeyDao")
            var keys: Any? = null
            for (name in listOf("getAll", "getAllKeys", "getAllForUser")) {
                keys = tryInvoke(dao, name)
                if (keys is List<*>) break
            }
            if (keys is List<*>) {
                line(RkpLevel.OK, "已分配密钥记录数：${keys.size}")
                keys.take(40).forEach { line("  $it") }
                if (keys.size > 40) line("… 其余省略")
            } else {
                line("该 rkpdapp 版本未提供可读的密钥列表方法，跳过")
            }
        } catch (t: Throwable) {
            line(RkpLevel.WARN, "读取密钥库失败：${rootCause(t)}")
        }
    }

    // ------------------------------------------------------------ 反射工具

    private fun invokeStatic(c: Class<*>, name: String, vararg args: Any?): Any? {
        val m = find(c, name, args, true)
            ?: throw NoSuchMethodException("${c.name}#$name")
        return m.invoke(null, *args)
    }

    private fun invoke(target: Any?, name: String, vararg args: Any?): Any? {
        val t = target ?: throw IllegalStateException("$name: 反射目标为 null")
        val m = find(t.javaClass, name, args, false)
            ?: throw NoSuchMethodException("${t.javaClass.name}#$name")
        return m.invoke(t, *args)
    }

    private fun tryInvoke(target: Any?, name: String, vararg args: Any?): Any? =
        runCatching { invoke(target, name, *args) }.getOrNull()

    @Throws(Exception::class)
    private fun getField(o: Any?, field: String): Any? {
        val target = o ?: throw IllegalStateException("$field: 反射目标为 null")
        var c: Class<*>? = target.javaClass
        while (c != null) {
            try {
                val f: Field = c.getDeclaredField(field)
                f.isAccessible = true
                return f.get(target)
            } catch (_: NoSuchFieldException) {
                c = c.superclass
            }
        }
        throw NoSuchFieldException(field)
    }

    /** 按名字 + 实参兼容性查找方法（含接口），避免写死 AOSP 的签名 */
    private fun find(
        c: Class<*>,
        name: String,
        args: Array<out Any?>,
        staticOnly: Boolean,
    ): Method? {
        var k: Class<*>? = c
        while (k != null) {
            for (m in k.declaredMethods) {
                if (m.name != name) continue
                if (staticOnly && !Modifier.isStatic(m.modifiers)) continue
                val ps = m.parameterTypes
                if (ps.size != args.size) continue
                var ok = true
                for (i in ps.indices) {
                    val a = args[i] ?: continue
                    if (!box(ps[i]).isAssignableFrom(a.javaClass)) {
                        ok = false
                        break
                    }
                }
                if (ok) {
                    m.isAccessible = true
                    return m
                }
            }
            for (itf in k.interfaces) {
                find(itf, name, args, staticOnly)?.let { return it }
            }
            k = k.superclass
        }
        return null
    }

    private fun box(c: Class<*>): Class<*> = when (c) {
        Int::class.java -> Int::class.javaObjectType
        Long::class.java -> Long::class.javaObjectType
        Boolean::class.java -> Boolean::class.javaObjectType
        Double::class.java -> Double::class.javaObjectType
        Float::class.java -> Float::class.javaObjectType
        Short::class.java -> Short::class.javaObjectType
        Byte::class.java -> Byte::class.javaObjectType
        Char::class.java -> Char::class.javaObjectType
        else -> c
    }

    private fun rootCause(t: Throwable): String {
        var c: Throwable = t
        while (c.cause != null && c.cause !== c) c = c.cause!!
        return c.toString()
    }

    private fun closeQuietly(o: Any?) {
        if (o == null) return
        runCatching { invoke(o, "close") }
    }

    private fun line(s: String) = line(RkpLevel.NORMAL, s)

    private fun line(level: Int, s: String) {
        log?.log(level, s)
    }
}
