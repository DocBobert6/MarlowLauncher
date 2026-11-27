package app.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import android.provider.Settings
import app.launcher.model.AppCategory
import app.launcher.model.AppEntry
import app.launcher.model.AppShortcut
import app.launcher.model.IconPack
import org.xmlpull.v1.XmlPullParser
import java.io.File

/**
 * Installed-app access via LauncherApps (#11 drawer source of truth),
 * app shortcuts (#30), icon packs (#19) and launch / uninstall / app-info
 * actions (#86, #97).
 */
class AppRepository(private val context: Context) {

    private val launcherApps: LauncherApps
        get() = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val packageManager: PackageManager get() = context.packageManager

    /** All launchable activities for the current user, sorted by label. */
    fun loadApps(): List<AppEntry> {
        val user = Process.myUserHandle()
        return try {
            launcherApps.getActivityList(null, user).map { info ->
                val component = info.componentName.flattenToString()
                val appInfo = try {
                    packageManager.getApplicationInfo(info.componentName.packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
                val size = appInfo?.sourceDir?.let { File(it).length() } ?: 0L
                AppEntry(
                    component = component,
                    packageName = info.componentName.packageName,
                    label = info.label?.toString() ?: info.componentName.packageName,
                    installTime = try {
                        packageManager.getPackageInfo(info.componentName.packageName, 0).firstInstallTime
                    } catch (e: PackageManager.NameNotFoundException) {
                        0L
                    },
                    sizeBytes = size,
                )
            }.sortedBy { it.label.lowercase() }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun loadIcon(component: String): Drawable? {
        val cn = ComponentName.unflattenFromString(component) ?: return null
        return try {
            val user = Process.myUserHandle()
            launcherApps.getActivityList(null, user)
                .firstOrNull { it.componentName == cn }
                ?.getIcon(0)
        } catch (e: Exception) {
            null
        }
    }

    fun launchApp(component: String): Boolean {
        val cn = ComponentName.unflattenFromString(component) ?: return false
        return try {
            val user = Process.myUserHandle()
            launcherApps.startMainActivity(cn, user, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** #30 dynamic + manifest shortcuts for one app. */
    fun loadShortcuts(component: String): List<AppShortcut> {
        val cn = ComponentName.unflattenFromString(component) ?: return emptyList()
        return try {
            val query = LauncherApps.ShortcutQuery()
                .setActivity(cn)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            launcherApps.getShortcuts(query, Process.myUserHandle())
                ?.map { info: ShortcutInfo ->
                    AppShortcut(
                        id = info.id,
                        packageName = info.`package`,
                        shortLabel = info.shortLabel?.toString() ?: info.id,
                        longLabel = info.longLabel?.toString(),
                    )
                } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun launchShortcut(shortcut: AppShortcut): Boolean = try {
        launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, Process.myUserHandle())
        true
    } catch (e: Exception) {
        false
    }

    fun shortcutIcon(shortcut: AppShortcut): Drawable? = try {
        val query = LauncherApps.ShortcutQuery()
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
        val info = launcherApps.getShortcuts(query, Process.myUserHandle())
            ?.firstOrNull { it.id == shortcut.id && it.`package` == shortcut.packageName }
            ?: return null
        launcherApps.getShortcutIconDrawable(info, 0)
    } catch (e: Exception) {
        null
    }

    /** #86 uninstall intent (system confirm dialog). */
    fun uninstallIntent(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))

    /** #97 App info page. */
    fun appInfoIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

    /** #97 Screen time: Digital Wellbeing / usage-access settings.
     *  There is no per-app screen-time API for a launcher; this opens
     *  the system usage-access screen, which is the honest route. */
    fun screenTimeIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /** #12 heuristic auto-categorisation (App Library). */
    fun categorize(app: AppEntry): AppCategory {
        val haystack = (app.label + " " + app.packageName).lowercase()
        return when {
            listOf("mail", "gmail", "message", "sms", "phone", "dialer", "call", "chat", "whatsapp", "telegram", "signal", "meet", "zoom", "teams").any { it in haystack } -> AppCategory.COMMUNICATION
            listOf("game", "play games", "chess", "puzzle", "minecraft", "pokemon").any { it in haystack } -> AppCategory.GAMES
            listOf("music", "video", "youtube", "player", "photo", "camera", "gallery", "spotify", "netflix", "podcast", "tv").any { it in haystack } -> AppCategory.MEDIA
            listOf("instagram", "facebook", "twitter", "x ", "tiktok", "reddit", "threads", "snapchat", "discord").any { it in haystack } -> AppCategory.SOCIAL
            listOf("notes", "docs", "drive", "calendar", "calculator", "clock", "files", "office", "task", "keep").any { it in haystack } -> AppCategory.PRODUCTIVITY
            listOf("bank", "pay", "wallet", "finance", "stock", "crypto").any { it in haystack } -> AppCategory.FINANCE
            listOf("shop", "store", "amazon", "ebay", "market").any { it in haystack } -> AppCategory.SHOPPING
            // B11: the Tools tab could never appear — no branch
            // returned it. System tools and utilities land here.
            listOf("settings", "tool", "utility", "utilities", "file manager", "vpn", "flashlight", "torch", "compass", "level", "scanner", "cleaner", "optimizer", "backup", "launcher", "keyboard", "wallpaper").any { it in haystack } -> AppCategory.TOOLS
            else -> AppCategory.OTHER
        }
    }

    // ---- icon packs (#19) ----

    /** Packs advertise the Nova / ADW theme intent actions. */
    fun discoverIconPacks(): List<IconPack> {
        val found = LinkedHashMap<String, String>()
        for (action in listOf("com.novalauncher.THEME", "org.adw.launcher.THEMES")) {
            val intent = Intent(action)
            val matches = try {
                packageManager.queryIntentActivities(intent, 0)
            } catch (e: Exception) {
                emptyList()
            }
            for (info in matches) {
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(packageManager)?.toString() ?: pkg
                found[pkg] = label
            }
        }
        return found.entries.map { (pkg, label) -> IconPack(pkg, label) }.sortedBy { it.label.lowercase() }
    }

    /**
     * Parses a pack's appfilter.xml: component -> drawable name.
     * Returns an empty map when the pack has no appfilter (the pack is
     * still listed; its icons simply cannot be mapped per-app).
     */
    fun loadPackMapping(packPackage: String): Map<String, String> {
        return try {
            val res = packageManager.getResourcesForApplication(packPackage)
            val xmlId = res.getIdentifier("appfilter", "xml", packPackage)
            if (xmlId == 0) return emptyMap()
            val parser = res.getXml(xmlId)
            val out = LinkedHashMap<String, String>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) {
                        val flattened = component
                            .removePrefix("ComponentInfo{")
                            .removeSuffix("}")
                        out[flattened] = drawable
                    }
                }
                event = parser.next()
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** A pack drawable by name, for applying a per-app icon (#98). */
    fun loadPackDrawable(packPackage: String, drawableName: String): Drawable? {
        return try {
            val res = packageManager.getResourcesForApplication(packPackage)
            val id = res.getIdentifier(drawableName, "drawable", packPackage)
            if (id == 0) null else res.getDrawable(id, null)
        } catch (e: Exception) {
            null
        }
    }

    /** #99 Quickedit: alternative icons from the active pack — the
     *  pack's other drawables, most relevant first (same first letter
     *  as the app's label, then the rest), capped at 12. */
    fun quickEditSuggestions(packPackage: String, app: AppEntry, current: String?): List<String> {
        val mapping = loadPackMapping(packPackage)
        val currentDrawable = mapping[app.component]
        val all = mapping.values.distinct().filter { it != currentDrawable && it != current }
        val firstLetter = app.label.firstOrNull()?.lowercaseChar()
        val sameLetter = all.filter { it.firstOrNull()?.lowercaseChar() == firstLetter }
        val rest = all.filter { it !in sameLetter }
        return (sameLetter + rest).take(12)
    }

    /** Contact display name for a picked contact Uri (#95). */
    fun resolveContactName(uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.ContactsContract.Contacts.DISPLAY_NAME),
                null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
