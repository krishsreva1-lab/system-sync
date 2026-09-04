package com.krish.systemsync.settings

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class GithubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body") val releaseNotes: String?,
    @SerializedName("assets") val assets: List<GithubAsset>
)

data class GithubAsset(
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("name") val name: String
)

// NEW: distinguishes "no update" from "check failed" so a failed check
// can never be shown to the user as "up to date".
sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: GithubRelease) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateManager {
    private const val GITHUB_API_URL = "https://api.github.com/repos/krishsreva1-lab/system-sync/releases/latest"

    fun getCurrentVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    private fun isVersionGreater(remote: String, local: String): Boolean {
        val rParts = remote.removePrefix("v").trim().split(".").map { it.toIntOrNull() ?: 0 }
        val lParts = local.removePrefix("v").trim().split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrNull(i) ?: 0
            val l = lParts.getOrNull(i) ?: 0
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    suspend fun checkForUpdate(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "SystemSYNC-Android-App") // Mandatory for GitHub API
                .build()

            client.newCall(request).execute().use { response ->
                Log.d("UpdateManager", "GitHub API response code: ${response.code}")

                if (!response.isSuccessful) {
                    val reason = if (response.code == 403 || response.code == 429) {
                        "GitHub API rate limit hit (code ${response.code})"
                    } else {
                        "GitHub API failed (code ${response.code})"
                    }
                    Log.e("UpdateManager", reason)
                    return@withContext UpdateCheckResult.Error(reason)
                }

                val bodyString = response.body?.string()
                    ?: return@withContext UpdateCheckResult.Error("Empty response body")
                Log.d("UpdateManager", "GitHub API body: $bodyString")

                val release = Gson().fromJson(bodyString, GithubRelease::class.java)
                val remoteVersion = release.tagName
                val localVersion = getCurrentVersion(context)

                Log.d("UpdateManager", "Remote version: $remoteVersion, Local version: $localVersion")

                if (isVersionGreater(remoteVersion, localVersion)) {
                    UpdateCheckResult.UpdateAvailable(release)
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Update check threw: ${e.message}", e)
            UpdateCheckResult.Error(e.message ?: "Unknown error while checking for updates")
        }
    }

    suspend fun downloadAndInstallApk(context: Context, downloadUrl: String, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val client = OkHttpClient()
            val request = Request.Builder().url(downloadUrl).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                val totalBytes = body.contentLength()

                val apkFile = File(context.cacheDir, "update.apk")
                if (apkFile.exists()) apkFile.delete()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloadedBytes = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                onProgress(downloadedBytes.toFloat() / totalBytes.toFloat())
                            }
                        }
                    }
                }

                // Trigger APK installation intent
                withContext(Dispatchers.Main) {
                    val apkUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                true
            }
        }.getOrDefault(false)
    }
}
