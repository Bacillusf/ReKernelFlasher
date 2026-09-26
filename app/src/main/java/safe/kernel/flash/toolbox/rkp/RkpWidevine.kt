package safe.kernel.flash.toolbox.rkp

import android.media.DeniedByServerException
import android.media.MediaDrm
import android.media.UnsupportedSchemeException
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.UUID

/**
 * Widevine 证书分配（对应 True RKP 的 Widevine.java）。
 *
 * 与原工具的对应关系：原工具外层 APK 里有一份作者改写的 WidevineProvisioner，
 * 特征是「先试 China URL，超时才回退正式 URL」—— 已由真机实测坐实：
 *   I RkpdWidevine: Provisioning with China URL
 *       https://www.googleapis.cn/certificateprovisioning/v1/devicecertificates/create?key=...
 * 所谓 China URL 并不是第三方服务器，而是把平台给出的地址里的 ".com" 替换成 ".cn"。
 *
 * 本实现只依赖框架的 MediaDrm，不需要 AOSP rkpdapp 的类。
 */
class RkpWidevine(private val log: RkpLogger?) {

    fun provision() {
        var drm: MediaDrm? = null
        try {
            val d = MediaDrm(WIDEVINE_UUID)
            drm = d
            val request = d.provisionRequest
            val data = request.data
            val base = request.defaultUrl
            if (base.isNullOrEmpty()) {
                say("Widevine 未提供 provisioning URL，跳过")
                return
            }
            val cn = base.replace(".com", ".cn")
            val resp: ByteArray = if (cn != base) {
                say("使用 China URL 请求 Widevine 证书")
                try {
                    post(cn, data)
                } catch (_: SocketTimeoutException) {
                    say("China URL 超时，回退正式地址")
                    post(base, data)
                }
            } else {
                say("使用正式地址请求 Widevine 证书")
                post(base, data)
            }
            d.provideProvisionResponse(resp)
            say(RkpLevel.OK, "Widevine 证书分配成功（${resp.size} 字节）")
        } catch (_: UnsupportedSchemeException) {
            say("设备不支持 Widevine，跳过")
        } catch (_: DeniedByServerException) {
            say(RkpLevel.ERR, "Widevine 服务端拒绝了请求")
        } catch (e: IOException) {
            say(RkpLevel.ERR, "Widevine 分配失败：${e.message}")
        } catch (t: Throwable) {
            say(RkpLevel.ERR, "Widevine 异常：$t")
        } finally {
            runCatching { drm?.close() }
        }
    }

    @Throws(IOException::class)
    private fun post(urlStr: String, body: ByteArray): ByteArray {
        val payload = "$urlStr&signedRequest=${String(body)}"
        val conn = URL(payload).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.setChunkedStreamingMode(0)
        conn.setRequestProperty("User-Agent", userAgent())
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Connection", "close")
        conn.outputStream.use { it.write(ByteArray(0)) }

        val code = conn.responseCode
        val input: InputStream? = if (code == 200) conn.inputStream else conn.errorStream
        val buffer = ByteArrayOutputStream()
        input?.use { ins ->
            val buf = ByteArray(8192)
            while (true) {
                val n = ins.read(buf)
                if (n <= 0) break
                buffer.write(buf, 0, n)
            }
        }
        conn.disconnect()
        if (code != 200) throw IOException("HTTP $code")
        return buffer.toByteArray()
    }

    private fun userAgent(): String = listOf(
        Build.MODEL, Build.TYPE, Build.VERSION.INCREMENTAL, Build.ID
    ).joinToString("/")

    private fun say(s: String) = say(RkpLevel.NORMAL, s)

    private fun say(level: Int, s: String) {
        log?.log(level, s)
    }

    companion object {
        private const val TIMEOUT_MS = 20_000
        private val WIDEVINE_UUID: UUID =
            UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")
    }
}
