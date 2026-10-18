package com.example.core.discovery

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import java.util.Locale

/**
 * Data structure representing an installed application discovered on the physical Android device.
 */
data class DiscoveredApp(
    val label: String,
    val packageName: String,
    val launcherActivityName: String?,
    val isSystemApp: Boolean,
    val lastDiscoveredTimestampMs: Long = System.currentTimeMillis()
)

/**
 * Result of resolving a user prompt or label (e.g. "Calculator") against the real device package catalog.
 */
sealed class AppResolutionResult {
    data class ExactMatch(val app: DiscoveredApp) : AppResolutionResult()
    data class AmbiguousMatch(val query: String, val candidates: List<DiscoveredApp>) : AppResolutionResult()
    data class NotFound(val query: String, val errorReason: String) : AppResolutionResult()
}

/**
 * High-reliability Application Discovery Component.
 * Solves the critical issue where LocalAgent was failing to retrieve real Android application package names.
 *
 * Capabilities:
 * 1. Queries Android PackageManager for genuinely launchable applications (ACTION_MAIN + CATEGORY_LAUNCHER).
 * 2. Distinguishes human-readable label from the real Android package identifier.
 * 3. Resolves queries (e.g. "Calculator") safely without relying on hardcoded package guesses.
 * 4. Supports multi-app disambiguation when multiple apps share similar labels.
 * 5. Handles package visibility restrictions across Android 8.1 (API 27) through Android 14+ (API 34+).
 */
class AppDiscoveryManager(private val context: Context) {

    companion object {
        private const val TAG = "AppDiscoveryManager"
        private const val CACHE_TTL_MS = 60_000L // 1 minute fresh cache
    }

    private var cachedApps: List<DiscoveredApp> = emptyList()
    private var lastScanTimeMs: Long = 0L

    /**
     * Scans the Android PackageManager for all installed applications that expose a launcher activity.
     */
    @Synchronized
    fun discoverInstalledApps(forceRefresh: Boolean = false): List<DiscoveredApp> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedApps.isNotEmpty() && (now - lastScanTimeMs) < CACHE_TTL_MS) {
            return cachedApps
        }

        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager.MATCH_ALL
        } else {
            0
        }

        val resolvedList: List<ResolveInfo> = try {
            pm.queryIntentActivities(launcherIntent, flags)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query launcher activities from PackageManager: ${e.message}", e)
            emptyList()
        }

        val discovered = mutableListOf<DiscoveredApp>()
        for (resolveInfo in resolvedList) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val pkgName = activityInfo.packageName ?: continue
            val activityName = activityInfo.name

            val label = try {
                resolveInfo.loadLabel(pm)?.toString()?.trim() ?: pkgName
            } catch (e: Exception) {
                pkgName
            }

            val isSystem = (activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            discovered.add(
                DiscoveredApp(
                    label = label,
                    packageName = pkgName,
                    launcherActivityName = activityName,
                    isSystemApp = isSystem,
                    lastDiscoveredTimestampMs = now
                )
            )
        }

        // Sort by label for deterministic listing
        cachedApps = discovered.sortedBy { it.label.lowercase(Locale.ROOT) }
        lastScanTimeMs = now
        Log.i(TAG, "Discovered ${cachedApps.size} installed launchable applications on device.")
        return cachedApps
    }

    /**
     * Resolves an app name or search string (e.g. "Calculator", "Settings", "Camera")
     * against actual installed applications on this specific device.
     */
    fun resolveApp(query: String): AppResolutionResult {
        val apps = discoverInstalledApps()
        val cleanQuery = query.trim().lowercase(Locale.ROOT)

        if (apps.isEmpty()) {
            return AppResolutionResult.NotFound(
                query,
                "PackageManager returned zero launchable applications. Verify package visibility (<queries>) in AndroidManifest."
            )
        }

        // 1. Direct exact match on label
        val exactLabelMatches = apps.filter { it.label.lowercase(Locale.ROOT) == cleanQuery }
        if (exactLabelMatches.size == 1) {
            return AppResolutionResult.ExactMatch(exactLabelMatches.first())
        }

        // 2. Direct exact match on package name
        val exactPackageMatches = apps.filter { it.packageName.lowercase(Locale.ROOT) == cleanQuery }
        if (exactPackageMatches.size == 1) {
            return AppResolutionResult.ExactMatch(exactPackageMatches.first())
        }

        // 3. Normalized contains or prefix match
        val partialMatches = apps.filter {
            val labelLower = it.label.lowercase(Locale.ROOT)
            val pkgLower = it.packageName.lowercase(Locale.ROOT)
            labelLower.contains(cleanQuery) || pkgLower.contains(cleanQuery)
        }

        return when {
            partialMatches.size == 1 -> AppResolutionResult.ExactMatch(partialMatches.first())
            partialMatches.size > 1 -> AppResolutionResult.AmbiguousMatch(query, partialMatches)
            else -> AppResolutionResult.NotFound(
                query,
                "No installed application matching '$query' was found among ${apps.size} installed apps."
            )
        }
    }

    /**
     * Creates an explicit launch intent for a discovered app, verifying that it is launchable.
     */
    fun createLaunchIntent(app: DiscoveredApp): Intent? {
        val pm = context.packageManager
        // 1. Try explicit component intent
        if (!app.launcherActivityName.isNullOrEmpty()) {
            val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.launcherActivityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (explicitIntent.resolveActivity(pm) != null) {
                return explicitIntent
            }
        }

        // 2. Fallback to getLaunchIntentForPackage
        return pm.getLaunchIntentForPackage(app.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
