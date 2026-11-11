package app.morphe.patches.yourwidget

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    const val PACKAGE_NAME = "com.google.android.apps.yourwidget"

    const val VERSION_NAME = "1.78.1014747048.5-release"
    const val VERSION_CODE = 285899

    const val GEMINI_INTELLIGENCE_FEATURE =
        "com.google.android.feature.GEMINI_INTELLIGENCE_EXPERIENCE"
    const val CONFIG_ACTIVITY_NAME =
        "com.google.android.apps.ai.llmwidgets.widgetconfighome.LlmWidgetConfigActivity"

    val COMPATIBILITY_YOURWIDGET = Compatibility(
        packageName = PACKAGE_NAME,
        name = "Create My Widget",
        description = "Create My Widget (Google AI Widgets) bundle tested with $VERSION_NAME ($VERSION_CODE).",
        apkFileType = ApkFileType.APKS,
        signatures = setOf("5aaf36a918288f7ac42857fb9a87a7fd3646a7ddcfbd29cd7413b97fa19922c3"),
        targets = listOf(
            AppTarget(
                version = VERSION_NAME,
                versionCode = VERSION_CODE,
                minSdk = 37,
            ),
        ),
    )
}
