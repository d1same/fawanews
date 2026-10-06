package sc.fawanews.app

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import sc.fawanews.app.R

/**
 * One APK: TV UI on leanback / television devices, phone UI otherwise.
 * Prefer the `values-television` bool (reliable on Shield); runtime checks are backup.
 */
fun Context.isTelevision(): Boolean {
    if (resources.getBoolean(R.bool.device_is_tv)) {
        return true
    }
    val pm = packageManager
    if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK_ONLY)) {
        return true
    }
    if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
        return true
    }
    val config = resources.configuration
    if ((config.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION) {
        return true
    }
    val uiMode = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}
