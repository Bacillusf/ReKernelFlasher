package safe.kernel.flash

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    // Compares version strings (e.g., v1.0.0 vs. v1.0.1)
    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

        return latestParts.zip(currentParts).any { (l, c) -> l > c }
    }

    suspend fun hasActiveInternetConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://connectivitycheck.gstatic.com/generate_204")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Android")
            connection.connectTimeout = 1500
            connection.connect()
            return@withContext connection.responseCode == 204
        } catch (e: IOException) {
            return@withContext false
        }
    }

    // Checks if an update is available
    suspend fun checkForUpdate(
        context: Context,
        currentVersion: String,
        onShowDialog: (String, List<String>, () -> Unit) -> Unit
    ) {
        val response = api.getLatestRelease()
        if (response.isSuccessful) {
            val release = response.body() ?: return
            val latestVersion = release.tagName.removePrefix("v")
            if (isNewer(latestVersion, currentVersion)) {
                val apk = release.assets.find { it.name.endsWith(".apk") } ?: return
                val dialogTitle = "New version: $latestVersion"
                val dialogLines = listOf(
                    "Changelog:",
                    *release.body.split("\n").toTypedArray()
                )
                val confirmAction = { downloadAndInstallApk(context, apk.downloadUrl, latestVersion) }
                onShowDialog(dialogTitle, dialogLines, confirmAction)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun downloadAndInstallApk(context: Context, url: String, latestVersion: String) {
        Toast.makeText(context, "Downloading Update in Background. Don't perform any operations till update is completed!", Toast.LENGTH_LONG).show()
        val request = DownloadManager.Request(Uri.parse(url))
        request.setTitle("Kernel Flasher Latest Download")
        request.setDescription("Downloading update...")
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Kernel_Flasher_$latestVersion.apk")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = manager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val apkUri = manager.getUriForDownloadedFile(id)
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(installIntent)
                }
            }
        }

        val appContext = context.applicationContext
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(receiver, intentFilter)
        }
    }
}
