package safe.kernel.flash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

interface GitHubApi {
    @GET("repos/Bacillusf/ReKernelFlasher/releases/latest")
    suspend fun getLatestRelease(): Response<AppUpdater.GitHubRelease>
}

object AppUpdater {

    data class GitHubAsset(
        val name: String,
        @SerializedName("browser_download_url") val downloadUrl: String
    )

    data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String,
        val body: String,
        val assets: List<GitHubAsset>
    )

    private val api: GitHubApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .build()
        .create(GitHubApi::class.java)

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(latestParts.size, currentParts.size)
        val lp = latestParts + List(maxLen - latestParts.size) { 0 }
        val cp = currentParts + List(maxLen - currentParts.size) { 0 }
        for (i in 0 until maxLen) {
            if (lp[i] > cp[i]) return true
            if (lp[i] < cp[i]) return false
        }
        return false
    }

    suspend fun hasActiveInternetConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://connectivitycheck.gstatic.com/generate_204")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Android")
            connection.connectTimeout = 1500
            connection.connect()
            connection.responseCode == 204
        } catch (e: IOException) { false }
    }

    data class UpdateInfo(
        val version: String,
        val body: String,
        val downloadUrl: String
    )

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? {
        return try {
            val response = api.getLatestRelease()
            if (response.isSuccessful) {
                val release = response.body() ?: return null
                val latestVersion = release.tagName.removePrefix("v")
                if (isNewer(latestVersion, currentVersion)) {
                    val apk = release.assets.find { it.name.endsWith(".apk") } ?: return null
                    UpdateInfo(latestVersion, release.body, apk.downloadUrl)
                } else null
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun downloadWithProgress(
        context: Context,
        url: String,
        version: String,
        progress: MutableState<Float>,
        onComplete: (File) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body ?: return@withContext
            val total = body.contentLength()
            val file = File(context.filesDir, "update_$version.apk")
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (total > 0) {
                            progress.value = downloaded.toFloat() / total
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) { onComplete(file) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun installApk(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "安装失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
