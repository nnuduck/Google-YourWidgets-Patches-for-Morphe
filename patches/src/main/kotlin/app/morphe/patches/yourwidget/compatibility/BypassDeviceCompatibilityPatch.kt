package app.morphe.patches.yourwidget.compatibility

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceXml
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.xml.XmlElement
import app.morphe.patches.yourwidget.Constants
import app.morphe.patches.yourwidget.geminiFeatureCheckFingerprint

private const val HIDE_FROM_PICKER_FLAG = "hide_from_picker"
private const val HIDE_FROM_PICKER_MASK = 0x2

@Suppress("unused")
val bypassDeviceCompatibilityPatch = patch(
    name = "Bypass device compatibility check",
    description = "Bypasses the runtime and manifest checks for Gemini Intelligence Experience, " +
        "unhides the widget from the system picker, and optionally adds a launcher shortcut.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_YOURWIDGET)

    val addLauncherShortcut by booleanOption(
        key = "addLauncherShortcut",
        default = true,
        title = "Add launcher shortcut",
        description = "Adds MAIN/LAUNCHER intent-filter to LlmWidgetConfigActivity so the configurator can also be opened from the app drawer.",
    )

    execute {
        // 1. Force GeminiFeatureCheckerImpl check to return true on unsupported devices.
        geminiFeatureCheckFingerprint.method.returnEarly(true)

        // 2. Clean up manifest <uses-feature> restriction and optionally add launcher entry.
        resourceXml("AndroidManifest.xml").use { manifest ->
            manifest.elements("uses-feature")
                .filter { it.attribute("android:name") == Constants.GEMINI_INTELLIGENCE_FEATURE }
                .toList()
                .forEach { it.remove() }

            if (addLauncherShortcut == true) {
                val configActivity = findConfigActivity(manifest.root)
                    ?: throw PatchException(
                        "Could not find activity ${Constants.CONFIG_ACTIVITY_NAME} in AndroidManifest.xml.",
                    )

                configActivity.setAttribute("android:exported", "true")
                ensureLauncherIntentFilter(configActivity)
            }
        }

        // 3. Unhide widget from Pixel Launcher / system widget picker.
        resourceXml("res/xml/llm_widget_info.xml").use { widgetInfo ->
            val root = widgetInfo.root
            val currentFeatures = root.attribute("android:widgetFeatures") ?: return@use
            val updatedFeatures = stripHideFromPickerFlag(currentFeatures)
            if (updatedFeatures.isNullOrEmpty()) {
                root.removeAttribute("android:widgetFeatures")
            } else {
                root.setAttribute("android:widgetFeatures", updatedFeatures)
            }
        }
    }
}

private fun findConfigActivity(manifestRoot: XmlElement): XmlElement? =
    manifestRoot.elements("application")
        .firstOrNull()
        ?.elements("activity")
        ?.firstOrNull { it.attribute("android:name") == Constants.CONFIG_ACTIVITY_NAME }

private fun ensureLauncherIntentFilter(activity: XmlElement) {
    val hasLauncherFilter = activity.elements("intent-filter").any { filter ->
        val hasMain = filter.elements("action")
            .any { it.attribute("android:name") == "android.intent.action.MAIN" }
        val hasLauncher = filter.elements("category")
            .any { it.attribute("android:name") == "android.intent.category.LAUNCHER" }
        hasMain && hasLauncher
    }

    if (!hasLauncherFilter) {
        activity.appendChild("intent-filter") {
            appendChild("action") {
                setAttribute("android:name", "android.intent.action.MAIN")
            }
            appendChild("category") {
                setAttribute("android:name", "android.intent.category.LAUNCHER")
            }
        }
    }
}

internal fun stripHideFromPickerFlag(rawFeatures: String): String? {
    val trimmed = rawFeatures.trim()
    if (trimmed.isEmpty()) return null

    // Handle hex bitmask ("0x3") or decimal bitmask if decoder leaves raw flags numeric.
    val numericMask = when {
        trimmed.startsWith("0x", ignoreCase = true) -> trimmed.substring(2).toIntOrNull(16)
        trimmed.all { it.isDigit() } -> trimmed.toIntOrNull(10)
        else -> null
    }
    if (numericMask != null) {
        val masked = numericMask and HIDE_FROM_PICKER_MASK.inv()
        return if (masked == 0) null else "0x${masked.toString(16)}"
    }

    val remaining = trimmed.split('|')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != HIDE_FROM_PICKER_FLAG }
    return if (remaining.isEmpty()) null else remaining.joinToString("|")
}
