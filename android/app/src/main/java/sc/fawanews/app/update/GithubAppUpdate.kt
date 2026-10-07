package sc.fawanews.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import sc.fawanews.app.data.FawaRepository
import java.io.File
import java.util.concurrent.TimeUnit

class GithubAppUpdate(
    private val client: OkHttpClient = updateClient(),
) {
    suspend fun latestNewerThan(localVersion: String): AppRelease? = withContext(Dispatchers.IO) {
        runCatching {
            val release = fetchLatest() ?: return@runCatching null
            if (!isNewerVersion(release.versionName, localVersion)) null else release
        }.getOrNull()
    }

    suspend fun download(release: AppRelease, cacheDir: File): File = withContext(Dispatchers.IO) {
        val folder = File(cacheDir, "updates").apply { mkdirs() }
        val dest = File(folder, release.apkName)
        val request = Request.Builder()
            .url(release.apkUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/vnd.android.package-archive")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }
            val body = response.body ?: error("Empty update download")
            dest.outputStream().use { output ->
                body.byteStream().use { input -> input.copyTo(output) }
            }
        }
        dest
    }

    private fun fetchLatest(): AppRelease? {
        val request = Request.Builder()
            .url(LATEST_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            val parsed = json.decodeFromString<GithubReleaseJson>(body)
            return selectReleaseApk(
                tagName = parsed.tagName,
                assets = parsed.assets.map { ReleaseAsset(it.name, it.browserDownloadUrl) },
            )
        }
    }

    @Serializable
    private data class GithubReleaseJson(
        @SerialName("tag_name") val tagName: String,
        val assets: List<GithubAssetJson> = emptyList(),
    )

    @Serializable
    private data class GithubAssetJson(
        val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
    )

    companion object {
        private const val LATEST_URL =
            "https://api.github.com/repos/d1same/fawanews/releases/latest"
        private const val USER_AGENT = "Clutch-Android"

        private val json = Json { ignoreUnknownKeys = true }

        private fun updateClient(): OkHttpClient =
            FawaRepository.defaultClient().newBuilder()
                .readTimeout(2, TimeUnit.MINUTES)
                .build()
    }
}
