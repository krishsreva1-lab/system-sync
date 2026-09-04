package com.krish.systemsync.settings

import android.content.Context
import android.content.Intent
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

    suspend fun checkForUpdate(context: Context): GithubRelease? = withContext(Dispatchers.IO) {
        runCatching {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val bodyString = response.body?.string() ?: return@runCatching null
                val release = Gson().fromJson(bodyString, GithubRelease::class.java)
                
                val remoteVersion = release.tagName.removePrefix("v").trim()
                val localVersion = getCurrentVersion(context).trim()

                if (remoteVersion > localVersion) {
                    release
                } else {
                    null
                }
            }
        }.getOrNull()
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
