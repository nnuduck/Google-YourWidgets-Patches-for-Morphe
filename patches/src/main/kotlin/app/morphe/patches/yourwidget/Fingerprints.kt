package app.morphe.patches.yourwidget

import app.morphe.patcher.Fingerprint

/**
 * Runtime feature check inside GeminiFeatureCheckerImpl that verifies the
 * system feature `com.google.android.feature.GEMINI_INTELLIGENCE_EXPERIENCE`.
 */
internal val geminiFeatureCheckFingerprint = Fingerprint(
    isStatic = false,
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf(Constants.GEMINI_INTELLIGENCE_FEATURE),
)

/**
 * `GooglePlayServicesUtil.isGooglePlayServicesAvailable(Context, int): int`.
 */
internal val googlePlayUtilityFingerprint = Fingerprint(
    isStatic = true,
    returnType = "I",
    parameters = listOf("Landroid/content/Context;", "I"),
    strings = listOf(
        "The Google Play services resources were not found. Check your project configuration to ensure that the resources are included.",
        " requires Google Play services, but they are missing.",
    ),
)

/**
 * `GooglePlayServicesUtil.ensurePlayServicesAvailable(Context, int): void`.
 */
internal val serviceCheckFingerprint = Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "I"),
    strings = listOf(
        "GooglePlayServices not available due to error ",
        "Google Play Services not available",
    ),
)

/**
 * PackageManager-based SHA-1 digest helper used by gRPC metadata interceptor to
 * populate `X-Android-Cert` for GMS / Play API requests.
 */
internal val grpcCertPackageManagerFingerprint = Fingerprint(
    isStatic = true,
    returnType = "Ljava/lang/String;",
    parameters = listOf("Landroid/content/pm/PackageManager;", "Ljava/lang/String;"),
    strings = listOf(
        "Found no signatures",
        "Could not get application signature",
    ),
)

/**
 * `SigningInfo`-based SHA-1 digest helper used when computing `X-Android-Cert`
 * for the active application package.
 */
internal val signingInfoSha1Fingerprint = Fingerprint(
    isStatic = true,
    returnType = "Ljava/lang/String;",
    parameters = listOf("Landroid/content/pm/SigningInfo;"),
    strings = listOf(
        "SHA1",
        "SHA1 not found.",
    ),
)

/**
 * Coroutine state machine `invokeSuspend(Object)` in
 * `SuggestedWidgetPromptsDataServiceImpl` handling `handleFetchError` and
 * `performFetchAndStoreData$<anonymous>$<anonymous>`.
 */
internal val suggestedWidgetPromptsCoroutineFingerprint = Fingerprint(
    isStatic = false,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf(
        "handleFetchError",
        "performFetchAndStoreData\$<anonymous>\$<anonymous>",
        "Failed to fetch server static data on first launch; falling back to app static data",
    ),
)
