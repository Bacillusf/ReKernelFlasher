package safe.kernel.flash.toolbox.rkp

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.os.MessageQueue
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import android.util.Log
import dalvik.system.PathClassLoader
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * app_process 入口类 —— 对应 True RKP 的 RkpMain.java / 原工具的 rp.rkp.rkpd。
 *
 * 启动方式（与实测到的原工具命令一致）：
 *   <helper> /system/bin/app_process -Djava.class.path='<APK>' / \
 *        --nice-name=com.android.rkpdapp safe.kernel.flash.toolbox.rkp.RkpMain '<rkpdapp.apk>' [dump]
 *
 * 做的事：
 *   1. 解除隐藏 API 限制
 *   2. ActivityThread.systemMain() 拿到 system context（需要 root）
 *   3. 找到设备上已安装的 com.android.rkpdapp / com.google.android.rkpdapp 的 Context
 *   4. 用动态代理替换 IPackageManager：让 hasSystemFeature("cn.google.services") 恒为 false
 *      —— 这是原工具为国内无 GMS 机型做的兼容
 *   5. 把自身挂成那个包的 Application
 *   6. 用 rkpdapp.apk 建 PathClassLoader（APEX 包的 Context ClassLoader 的 DexPathList 是空的，
 *      必须自建，否则会 ClassNotFoundException）
 *   7. 主线程空闲后在工作线程执行 RKP 分配 + Widevine 分配，然后 chown 数据目录并退出
 */
class RkpMain : Application() {

    /** 暴露给 companion 使用（attachBaseContext 是 protected） */
    fun attach(ctx: Context) {
        attachBaseContext(ctx)
    }

    companion object {
        private const val TAG = "TrueRkp"
        private val CANDIDATES = listOf(
            "com.android.rkpdapp",
            "com.google.android.rkpdapp",
        )

        @JvmStatic
        @Suppress("DEPRECATION")
        fun main(args: Array<String>) {
            val log = RkpStdoutLogger(TAG)

            try {
                Looper.prepareMainLooper()
                log.log(
                    if (RkpHiddenApi.exemptAll()) "已豁免隐藏 API 限制"
                    else "隐藏 API 豁免失败（继续尝试）"
                )

                val activityThread = Class.forName("android.app.ActivityThread")
                val thread = activityThread.getDeclaredMethod("systemMain").invoke(null)
                val sysCtx = activityThread.getDeclaredMethod("getSystemContext")
                    .invoke(thread) as Context

                var pkgCtx: Context? = null
                for (name in CANDIDATES) {
                    try {
                        pkgCtx = sysCtx.createPackageContext(name, 0)
                        break
                    } catch (_: PackageManager.NameNotFoundException) {
                        // 试下一个
                    }
                }
                if (pkgCtx == null) {
                    log.log(RkpLevel.ERR, "设备上没有 com.android.rkpdapp / com.google.android.rkpdapp，无法继续")
                    System.exit(2)
                    return
                }
                log.log("使用 ${pkgCtx.packageName} 的上下文")

                installPackageManagerProxy(activityThread, log)

                val self = RkpMain()
                self.attach(pkgCtx)
                swapApplication(pkgCtx, self, log)

                val rkpdApk = args.firstOrNull()?.let { File(it) }
                if (rkpdApk == null || !rkpdApk.exists()) {
                    log.log(RkpLevel.ERR, "缺少有效的 rkpdapp.apk 路径参数")
                    log.log(RkpLevel.ERR, "请把 rkpdapp.apk 放进 assets/ 重新构建，或在设备上安装 com.android.rkpdapp")
                    System.exit(3)
                    return
                }
                log.log("rkpdapp.apk：${rkpdApk.absolutePath}")

                // 关键：不能用 pkgCtx.classLoader 直接加载 rkpdapp 的类。
                // 当 rkpdapp 来自 APEX 时，createPackageContext() 拿到的 ClassLoader
                // 的 DexPathList 是空的（实测报错 DexPathList[[], ...]），
                // 必须自己用 rkpdapp.apk 的路径建 PathClassLoader。原工具也是这么做的。
                val cl: ClassLoader = try {
                    PathClassLoader(rkpdApk.absolutePath, pkgCtx.classLoader).also {
                        log.log("已为 rkpdapp 建立 PathClassLoader")
                    }
                } catch (t: Throwable) {
                    log.log(RkpLevel.WARN, "建立 PathClassLoader 失败，退回包上下文 ClassLoader：$t")
                    pkgCtx.classLoader
                }

                val dumpMode = args.size > 1 && args[1] == "dump"
                if (dumpMode) {
                    Thread({
                        RkpProvisioning(pkgCtx, cl, log).dump()
                        chownData(pkgCtx, log)
                        log.log("dump 完成")
                        System.exit(0)
                    }, "rkp-dump").start()
                } else {
                    Looper.getMainLooper().queue.addIdleHandler(
                        MessageQueue.IdleHandler {
                            Thread({ work(pkgCtx, cl, log) }, "rkp-work").start()
                            false
                        }
                    )
                }
                Looper.loop()
            } catch (t: Throwable) {
                Log.e(TAG, "rkpd 入口失败", t)
                System.err.println("rkpd 入口失败：$t")
                System.exit(1)
            }
        }

        private fun work(ctx: Context, cl: ClassLoader, log: RkpLogger) {
            try {
                log.log("开始 RKP 密钥分配")
                RkpProvisioning(ctx, cl, log).run()
            } catch (t: Throwable) {
                log.log(RkpLevel.ERR, "分配流程异常：$t")
            }
            chownData(ctx, log)
            log.log(RkpLevel.OK, "流程结束")
            System.exit(0)
        }

        /** 替换 IPackageManager，让 hasSystemFeature("cn.google.services") 返回 false */
        private fun installPackageManagerProxy(activityThread: Class<*>, log: RkpLogger) {
            try {
                val f = findField(activityThread, "sPackageManager")
                f.isAccessible = true
                val original = f.get(null)
                val ipm = Class.forName("android.content.pm.IPackageManager")
                // 用显式 SAM 构造，避免 lambda 标签/返回类型推断的坑
                val handler = InvocationHandler { _, method, args ->
                    if (method.name == "hasSystemFeature" &&
                        args != null && args.isNotEmpty() && args[0] == "cn.google.services"
                    ) {
                        false
                    } else {
                        try {
                            method.invoke(original, *(args ?: emptyArray<Any?>()))
                        } catch (e: InvocationTargetException) {
                            throw e.cause ?: e
                        }
                    }
                }
                val proxy = Proxy.newProxyInstance(activityThread.classLoader, arrayOf(ipm), handler)
                f.set(null, proxy)
                log.log("已接管 IPackageManager：hasSystemFeature(\"cn.google.services\") -> false")
            } catch (t: Throwable) {
                log.log(RkpLevel.WARN, "接管 IPackageManager 失败：$t")
            }
        }

        /** 把 rkpdapp 包的 Application 换成自己，避免它拿到 null */
        private fun swapApplication(pkgCtx: Context, app: Application, log: RkpLogger) {
            try {
                val mpi = findField(pkgCtx.javaClass, "mPackageInfo")
                mpi.isAccessible = true
                val loadedApk = mpi.get(pkgCtx)
                val mApp = findField(loadedApk.javaClass, "mApplication")
                mApp.isAccessible = true
                mApp.set(loadedApk, app)
                log.log("已挂载 Application")
            } catch (t: Throwable) {
                log.log(RkpLevel.WARN, "挂载 Application 失败（非致命）：$t")
            }
        }

        /** 把 root 写出来的数据文件属主改回 rkpdapp 的 uid（原工具也这么做） */
        private fun chownData(ctx: Context, log: RkpLogger) {
            try {
                val uid = ctx.applicationInfo.uid
                if (uid <= 0) return
                chownRecursive(ctx.dataDir, uid)
                ctx.createDeviceProtectedStorageContext()?.let { chownRecursive(it.dataDir, uid) }
                log.log("已把数据目录属主改回 $uid")
            } catch (t: Throwable) {
                log.log(RkpLevel.WARN, "chown 失败：$t")
            }
        }

        private fun chownRecursive(f: File, uid: Int) {
            try {
                val path = f.path
                val st: StructStat = Os.stat(path)
                if (st.st_uid == 0) Os.chown(path, uid, uid)
                if (OsConstants.S_ISDIR(st.st_mode)) {
                    f.listFiles()?.forEach { chownRecursive(it, uid) }
                }
            } catch (_: Throwable) {
                // 单个文件失败不影响整体
            }
        }

        @Throws(NoSuchFieldException::class)
        private fun findField(c: Class<*>, name: String): Field {
            var k: Class<*>? = c
            while (k != null) {
                try {
                    return k.getDeclaredField(name)
                } catch (_: NoSuchFieldException) {
                    k = k.superclass
                }
            }
            throw NoSuchFieldException(name)
        }
    }
}
