package app.launcher.data

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * #26 / #89 double-tap to lock.
 *
 * Route used: the device-admin lock — DevicePolicyManager.lockNow()
 * once the user has enabled this app as a device admin. That is the
 * only public API that locks the device from a third-party launcher;
 * there is no system lock intent to fire. When the admin is not yet
 * enabled, lock() opens the system device-admin enrollment screen
 * for this app instead of doing nothing silently, and returns false.
 *
 * The receiver + its policy XML (res/xml/device_admin.xml, force-lock
 * only) are declared in the manifest. The admin grants nothing else:
 * no wipe, no password rules — the policy file lists force-lock only.
 */
class LauncherDeviceAdminReceiver : DeviceAdminReceiver()

object DeviceLock {

    fun adminComponent(context: Context): ComponentName =
        ComponentName(context, LauncherDeviceAdminReceiver::class.java)

    fun isAdminActive(context: Context): Boolean {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return manager.isAdminActive(adminComponent(context))
    }

    /**
     * Locks the device now when the admin is active; otherwise opens
     * the system enrollment screen and returns false.
     */
    fun lock(context: Context): Boolean {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = adminComponent(context)
        if (!manager.isAdminActive(component)) {
            try {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Allow the launcher to lock the screen for double-tap to lock.",
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // No admin enrollment screen; the lock cannot run.
            }
            return false
        }
        return try {
            manager.lockNow()
            true
        } catch (e: Exception) {
            false
        }
    }
}
