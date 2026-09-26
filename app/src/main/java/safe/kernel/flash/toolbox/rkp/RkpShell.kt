package safe.kernel.flash.toolbox.rkp

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * root shell 执行层（对应 True RKP 的 Sh.java）。
 *
 * 为什么不用 libsu：本页面保留了「可自定义 su 路径」这一功能
 * （长按「应用配置」设置），所以需要能指定 su 可执行文件；
 * 同时需要把多条命令的 stdout 按命令分开取回（读取 5 个 getprop）。
 *
 * 实现方式：ProcessBuilder(su, "-c", script)，每条命令后打一个哨兵行，
 * 从而把合并的 stdout 切回逐条结果；stdout/stderr 分别收集，支持超时与逐行回调。
 */
object RkpShell {

    /** 单引号安全转义：拼进 shell 脚本时使用，避免注入 */
    fun q(s: String?): String = "'" + (s ?: "").replace("'", "'\\''") + "'"

    class Out {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        var code = -1
        var timeout = false
        var suDenied = false

        fun stdoutText(): String = stdout.joinToString("\n")
        fun stderrText(): String = stderr.joinToString("\n")
    }

    /**
     * 以 root 依次执行命令。
     *
     * @param suPath    su 可执行文件（可以是 "su"）
     * @param cmds      命令列表
     * @param timeoutMs 总超时
     * @param outSink   可空：stdout 逐行回调（后台线程）
     * @param errSink   可空：stderr 逐行回调（后台线程）
     */
    fun root(
        suPath: String?,
        cmds: List<String>,
        timeoutMs: Long,
        outSink: ((String) -> Unit)? = null,
        errSink: ((String) -> Unit)? = null,
    ): Out {
        val out = Out()
        val sentinel = "__TRKP_" + java.lang.Long.toHexString(System.nanoTime()) + "__"
        val script = buildString {
            cmds.forEach { cmd ->
                append(cmd).append('\n')
                append("echo ").append(sentinel).append('\n')
            }
        }

        val su = suPath?.trim().takeUnless { it.isNullOrEmpty() } ?: "su"
        val process = try {
            ProcessBuilder(su, "-c", script)
                .redirectErrorStream(false)
                .start()
        } catch (e: Exception) {
            out.suDenied = true
            out.stderr.add("无法启动 $su：${e.message}")
            return out
        }

        val tOut = pump(process.inputStream, out.stdout, sentinel, outSink, true)
        val tErr = pump(process.errorStream, out.stderr, null, errSink, false)

        val finished = try {
            process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!finished) {
            out.timeout = true
            process.destroyForcibly()
        }
        runCatching { tOut.join(2000) }
        runCatching { tErr.join(2000) }
        out.code = runCatching { process.exitValue() }.getOrDefault(-1)
        out.suDenied = looksDenied(out) && out.stdout.isEmpty()
        return out
    }

    fun root(suPath: String?, cmds: List<String>, timeoutMs: Long): Out =
        root(suPath, cmds, timeoutMs, null, null)

    private fun pump(
        input: InputStream,
        sink: MutableList<String>,
        sentinel: String?,
        cb: ((String) -> Unit)?,
        splitBySentinel: Boolean,
    ): Thread {
        val t = Thread({
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                val pending = StringBuilder()
                try {
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (splitBySentinel && sentinel != null) {
                            if (line.startsWith(sentinel)) {
                                sink.add(pending.toString())
                                pending.setLength(0)
                                continue
                            }
                            if (pending.isNotEmpty()) pending.append('\n')
                            pending.append(line)
                            cb?.invoke(line)
                        } else {
                            sink.add(line)
                            cb?.invoke(line)
                        }
                    }
                    if (splitBySentinel && pending.isNotEmpty()) sink.add(pending.toString())
                } catch (_: Exception) {
                    // 进程被杀时流会中断，属正常
                }
            }
        }, if (splitBySentinel) "rkp-stdout" else "rkp-stderr")
        t.isDaemon = true
        t.start()
        return t
    }

    private fun looksDenied(out: Out): Boolean {
        for (line in out.stderr) {
            val l = line.lowercase()
            if (l.contains("permission denied") || l.contains("not allowed") ||
                l.contains("no such file") || l.contains("inaccessible") ||
                l.contains("su: ") || l.contains("not found")
            ) {
                return true
            }
        }
        return out.code != 0 && out.stdout.isEmpty()
    }

    /** 取第 index 条命令的第一行结果（getprop 用） */
    fun line(out: Out, index: Int): String =
        out.stdout.getOrNull(index)?.trim().orEmpty()
}
