package safe.kernel.flash.toolbox.rkp

import java.lang.reflect.Method

/**
 * 解除隐藏 API 限制（对应 True RKP 的 HiddenApi.java）。
 *
 * 为什么需要：rkpd 入口要用到 ActivityThread.systemMain()、替换 IPackageManager 动态代理、
 * 反射写 mPackageInfo.mApplication 等隐藏成员，Android 9+ 默认会被拦截。
 *
 * 原理：用「反射去拿 Class.getDeclaredMethod」拿到 VMRuntime.setHiddenApiExemptions，
 * 传入前缀 "L"（豁免所有类型），避免直接引用隐藏符号。
 */
object RkpHiddenApi {

    fun exemptAll(): Boolean = try {
        // 注意：Kotlin 不允许对 java.lang.Class 或数组类型使用类字面量
        //（Class::class.java / Array<X>::class.java 都会编译失败），
        // 所以这里用 Class.forName 与 emptyArray<...>().javaClass 取得 Class 对象。
        val classClass = Class.forName("java.lang.Class")
        val classArrayClass = emptyArray<Class<*>>().javaClass      // Class<Array<Class<*>>>
        val stringArrayClass = emptyArray<String>().javaClass       // Class<Array<String>>
        val forName = classClass.getDeclaredMethod("forName", String::class.java)
        val getDeclaredMethod = classClass.getDeclaredMethod(
            "getDeclaredMethod", String::class.java, classArrayClass
        )
        val vmRuntime = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
        val getRuntime = getDeclaredMethod.invoke(
            vmRuntime, "getRuntime", emptyArray<Class<*>>()
        ) as Method
        val setExemptions = getDeclaredMethod.invoke(
            vmRuntime, "setHiddenApiExemptions", arrayOf<Class<*>>(stringArrayClass)
        ) as Method
        val runtime = getRuntime.invoke(null)
        setExemptions.invoke(runtime, arrayOf("L"))
        true
    } catch (_: Throwable) {
        false
    }
}
