package com.androkiosk.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.core.net.toUri

object KioskController {
    private const val HOME_ALIAS = "com.androkiosk.kiosk.KioskHomeAlias"

    fun isDefaultHome(context: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun setHomeAliasEnabled(context: Context, enabled: Boolean) {
        val alias = ComponentName(context.packageName, HOME_ALIAS)
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (context.packageManager.getComponentEnabledSetting(alias) == state) return
        context.packageManager.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP)
    }

    fun engage(activity: Activity) {
        if (lockTaskState(activity) == ActivityManager.LOCK_TASK_MODE_NONE) {
            runCatching { activity.startLockTask() }
        }
    }

    fun release(activity: Activity) {
        if (lockTaskState(activity) != ActivityManager.LOCK_TASK_MODE_NONE) {
            runCatching { activity.stopLockTask() }
        }
    }

    fun canBlockGestures(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun overlaySettings(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())

    private fun lockTaskState(context: Context) =
        context.getSystemService<ActivityManager>()?.lockTaskModeState
            ?: ActivityManager.LOCK_TASK_MODE_NONE

    fun homeChooser(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService<RoleManager>()
            if (roles?.isRoleAvailable(RoleManager.ROLE_HOME) == true && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }
}
