package sc.fawanews.app.update

data class AppRelease(
    val versionName: String,
    val apkUrl: String,
    val apkName: String,
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
)

fun isNewerVersion(remote: String, local: String): Boolean {
    val remoteParts = versionParts(remote)
    val localParts = versionParts(local)
    val count = maxOf(remoteParts.size, localParts.size)
    for (index in 0 until count) {
        val remotePart = remoteParts.getOrElse(index) { 0 }
        val localPart = localParts.getOrElse(index) { 0 }
        if (remotePart != localPart) return remotePart > localPart
    }
    return false
}

fun selectReleaseApk(tagName: String, assets: List<ReleaseAsset>): AppRelease? {
    val apks = assets.filter { asset ->
        asset.name.endsWith(".apk", ignoreCase = true) &&
            !asset.name.contains("debug", ignoreCase = true)
    }
    val chosen = apks.firstOrNull { it.name.startsWith("FawaNews", ignoreCase = true) }
        ?: apks.firstOrNull()
        ?: return null
    val versionName = tagName.trim().removePrefix("v").removePrefix("V")
    if (versionName.isBlank()) return null
    return AppRelease(
        versionName = versionName,
        apkUrl = chosen.downloadUrl,
        apkName = chosen.name,
    )
}

private fun versionParts(version: String): List<Int> =
    version.trim()
        .removePrefix("v")
        .removePrefix("V")
        .split('.')
        .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
