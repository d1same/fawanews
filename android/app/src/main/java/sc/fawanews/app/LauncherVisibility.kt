package sc.fawanews.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Projectivy (and similar TV launchers) open "phone" apps (MAIN + LAUNCHER) in a narrow side panel.
 * On TV we disable the phone launcher alias so the app is treated as leanback-only and runs full screen.
 */
fun Context.applyLauncherVisibilityForDevice() {
    val pm = packageManager
    val phone = ComponentName(packageName, "$packageName.PhoneLauncher")
    val tv = ComponentName(packageName, "$packageName.TvLauncher")
    val flags = PackageManager.DONT_KILL_APP
    if (isTelevision()) {
        pm.setComponentEnabledSetting(
            phone,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            flags,
        )
        pm.setComponentEnabledSetting(
            tv,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            flags,
        )
    } else {
        pm.setComponentEnabledSetting(
            phone,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            flags,
        )
        pm.setComponentEnabledSetting(
            tv,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            flags,
        )
    }
}
