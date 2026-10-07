package sc.fawanews.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppReleaseTest {
    @Test
    fun newerVersionIsHigherNumbersOnly() {
        assertTrue(isNewerVersion("1.0.11", "1.0.10"))
        assertTrue(isNewerVersion("v1.1.0", "1.0.10"))
        assertFalse(isNewerVersion("1.0.10", "1.0.10"))
        assertFalse(isNewerVersion("1.0.9", "1.0.10"))
    }

    @Test
    fun releaseApkSkipsDebugName() {
        val release = selectReleaseApk(
            tagName = "v1.0.11",
            assets = listOf(
                ReleaseAsset("app-debug.apk", "https://example.com/debug.apk"),
                ReleaseAsset("FawaNews-1.0.11.apk", "https://example.com/FawaNews-1.0.11.apk"),
            ),
        )
        assertEquals("1.0.11", release?.versionName)
        assertEquals("FawaNews-1.0.11.apk", release?.apkName)
    }

    @Test
    fun releaseApkPrefersClutchName() {
        val release = selectReleaseApk(
            tagName = "v1.1.0",
            assets = listOf(
                ReleaseAsset("notes.apk", "https://example.com/notes.apk"),
                ReleaseAsset("Clutch-1.1.0.apk", "https://example.com/Clutch-1.1.0.apk"),
            ),
        )
        assertEquals("Clutch-1.1.0.apk", release?.apkName)
    }

    @Test
    fun missingApkMeansNoUpdate() {
        assertNull(selectReleaseApk("v1.0.11", emptyList()))
    }
}
