package app.morphe.patches.yourwidget.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.classDefForEach
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceXml
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.xml.XmlElement
import app.morphe.patches.all.misc.gms.gmsCorePackageQueriesPatch
import app.morphe.patches.yourwidget.Constants
import app.morphe.patches.yourwidget.grpcCertPackageManagerFingerprint
import app.morphe.patches.yourwidget.googlePlayUtilityFingerprint
import app.morphe.patches.yourwidget.serviceCheckFingerprint
import app.morphe.patches.yourwidget.signingInfoSha1Fingerprint
import app.morphe.util.getEndEntityCertificate
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import java.security.MessageDigest
import java.util.Locale

private const val DEFAULT_VENDOR_GROUP_ID = "app.revanced"

private const val GOOGLE_GMS_PACKAGE = "com.google.android.gms"
private const val GOOGLE_GSF_PREFIX = "com.google.android.gsf."
private const val GOOGLE_GSF_CONTENT_PREFIX = "content://com.google.android.gsf."
private const val GOOGLE_PHENOTYPE_CONTENT_PREFIX = "content://com.google.android.gms.phenotype/"
private const val GOOGLE_MOBSTORE_ACTION = "com.google.android.mobstore.service.START"
private const val GOOGLE_READ_GSERVICES_PERMISSION =
    "com.google.android.providers.gsf.permission.READ_GSERVICES"
private const val ANDROID_GET_ACCOUNTS_PERMISSION = "android.permission.GET_ACCOUNTS"

private val EXACT_GMS_STRINGS_TO_REDIRECT = setOf(
    "com.google.android.gms.fonts",
    "com.google.android.gms.chimera",
    "com.google.android.gms.phenotype",
    "com.google.android.gms.auth.accounts",
    "com.google.android.gms.accountsettings.action.VIEW_SETTINGS",
    "com.google.android.gms.people.contactssync.service.THIRD_PARTY_START",
)

private data class StringReplacement(
    val index: Int,
    val registerA: Int,
    val opcode: Opcode,
    val replacement: String,
)

@Suppress("unused")
val redirectServicesToGmsCorePatch = patch(
    name = "Redirect services to GmsCore",
    description = "Redirects Google Play Services, Google account, GServices, Phenotype, and MobStore " +
        "calls to microG/GmsCore and spoofs the original signing SHA-1 for non-root installations.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_YOURWIDGET)

    dependsOn(gmsCorePackageQueriesPatch)

    val gmsCoreVendorGroupId by stringOption(
        key = "gmsCoreVendorGroupId",
        default = DEFAULT_VENDOR_GROUP_ID,
        title = "GmsCore vendor group ID",
        description = "Package prefix of your microG/GmsCore installation (e.g., app.revanced, com.mgoogle, org.microg).",
        required = true,
    ) {
        it != null && it.matches(Regex("^[a-z]\\w*(\\.[a-z]\\w*)+$"))
    }

    val spoofXAndroidCert by booleanOption(
        key = "spoofXAndroidCert",
        default = true,
        title = "Spoof X-Android-Cert header",
        description = "Returns the original Play Store certificate SHA-1 when computing X-Android-Cert headers.",
    )

    execute {
        val vendorGroupId = gmsCoreVendorGroupId!!
        val gmsPackageName = "$vendorGroupId.android.gms"

        // Dynamically derive original SHA-1 digest from the input APK signing certificate.
        val certBytes = getEndEntityCertificate(packageMetadata.signingCertificates).encoded
        val sha1Bytes = MessageDigest.getInstance("SHA-1").digest(certBytes)
        val sha1Lower = sha1Bytes.joinToString("") { "%02x".format(Locale.US, it) }
        val sha1Upper = sha1Lower.uppercase(Locale.US)

        // Resolve all GMS/certificate fingerprints before mutating any strings.
        val googlePlayUtilMethod = googlePlayUtilityFingerprint.method
        val serviceCheckMethod = serviceCheckFingerprint.method
        val grpcCertPmMethod = if (spoofXAndroidCert == true) {
            grpcCertPackageManagerFingerprint.method
        } else {
            null
        }
        val signingInfoSha1Method = if (spoofXAndroidCert == true) {
            signingInfoSha1Fingerprint.method
        } else {
            null
        }

        // Bypass GMS availability and signature validation checks.
        googlePlayUtilMethod.returnEarly(0)
        serviceCheckMethod.returnEarly()

        // Spoof X-Android-Cert SHA-1 for gRPC / Play API calls when re-signed with a user key.
        if (grpcCertPmMethod != null && signingInfoSha1Method != null) {
            signingInfoSha1Method.returnEarly(sha1Upper)

            val pmImpl = grpcCertPmMethod.implementation
                ?: throw PatchException("X-Android-Cert PackageManager helper has no implementation.")
            val totalRegs = pmImpl.registerCount
            // Static (PackageManager, String) method -> 2 parameter registers at the top of the frame.
            val localRegs = totalRegs - 2
            val packageNameReg = totalRegs - 1
            if (localRegs < 1 || (packageNameReg > 15 && localRegs < 2)) {
                throw PatchException(
                    "Insufficient registers in X-Android-Cert helper (total=$totalRegs, locals=$localRegs).",
                )
            }

            val injectedSmali = if (packageNameReg <= 15) {
                """
                    const-string v0, "${Constants.PACKAGE_NAME}"
                    invoke-virtual {v0, v$packageNameReg}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :not_target_pkg
                    const-string v0, "$sha1Upper"
                    return-object v0
                """.trimIndent()
            } else {
                """
                    const-string v0, "${Constants.PACKAGE_NAME}"
                    move-object/from16 v1, v$packageNameReg
                    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :not_target_pkg
                    const-string v0, "$sha1Upper"
                    return-object v0
                """.trimIndent()
            }

            grpcCertPmMethod.addInstructionsWithLabels(
                0,
                injectedSmali,
                ExternalLabel("not_target_pkg", grpcCertPmMethod.getInstruction(0)),
            )
        }

        // Single-pass DEX string transform across all methods.
        rewriteGmsStrings(vendorGroupId)

        // Update manifest permissions, <queries>, and microG spoof metadata.
        updateManifestForGmsCore(
            manifestXmlPath = "AndroidManifest.xml",
            vendorGroupId = vendorGroupId,
            gmsPackageName = gmsPackageName,
            originalSha1Lower = sha1Lower,
        )
    }
}

private fun rewriteGmsStrings(vendorGroupId: String) {
    classDefForEach { classDef ->
        if (classDef.type.startsWith("Lapp/morphe/extension/")) return@classDefForEach

        val pendingMethods = mutableListOf<Pair<Method, List<StringReplacement>>>()

        for (method in classDef.methods) {
            val impl = method.implementation ?: continue
            var edits: MutableList<StringReplacement>? = null

            impl.instructions.forEachIndexed { index, insn ->
                val opcode = insn.opcode
                if (opcode != Opcode.CONST_STRING && opcode != Opcode.CONST_STRING_JUMBO) {
                    return@forEachIndexed
                }

                val strRef = (insn as ReferenceInstruction).reference as? StringReference
                    ?: return@forEachIndexed
                val rewritten = transformGmsString(strRef.string, vendorGroupId)
                    ?: return@forEachIndexed

                val methodEdits = edits ?: mutableListOf<StringReplacement>().also { edits = it }
                methodEdits.add(
                    StringReplacement(
                        index = index,
                        registerA = (insn as OneRegisterInstruction).registerA,
                        opcode = opcode,
                        replacement = rewritten,
                    ),
                )
            }

            edits?.let { pendingMethods.add(method to it) }
        }

        if (pendingMethods.isEmpty()) return@classDefForEach

        val mutableClass = mutableClassDefBy(classDef)
        for ((origMethod, edits) in pendingMethods) {
            val mutableMethod = mutableClass.methods.firstOrNull { candidate ->
                candidate.name == origMethod.name &&
                    candidate.returnType == origMethod.returnType &&
                    candidate.parameterTypes == origMethod.parameterTypes
            } ?: throw PatchException(
                "Could not locate mutable method ${origMethod.definingClass}->${origMethod.name}.",
            )

            for (edit in edits) {
                val stringRef = ImmutableStringReference(edit.replacement)
                val replacementInsn = if (edit.opcode == Opcode.CONST_STRING_JUMBO) {
                    BuilderInstruction31c(Opcode.CONST_STRING_JUMBO, edit.registerA, stringRef)
                } else {
                    BuilderInstruction21c(Opcode.CONST_STRING, edit.registerA, stringRef)
                }
                mutableMethod.replaceInstruction(edit.index, replacementInsn)
            }
        }
    }
}

internal fun transformGmsString(value: String, vendorGroupId: String): String? {
    val gmsPkg = "$vendorGroupId.android.gms"
    val gsfPkg = "$vendorGroupId.android.gsf"

    return when {
        value == "com.google" -> vendorGroupId
        value == GOOGLE_GMS_PACKAGE -> gmsPkg
        value == GOOGLE_MOBSTORE_ACTION -> "$vendorGroupId.android.mobstore.service.START"
        value == GOOGLE_READ_GSERVICES_PERMISSION ->
            "$vendorGroupId.android.providers.gsf.permission.READ_GSERVICES"
        value.startsWith(GOOGLE_GSF_PREFIX) -> gsfPkg + value.removePrefix("com.google.android.gsf")
        value.startsWith(GOOGLE_GSF_CONTENT_PREFIX) ->
            "content://$gsfPkg" + value.removePrefix("content://com.google.android.gsf")
        value.startsWith(GOOGLE_PHENOTYPE_CONTENT_PREFIX) ->
            "content://$gmsPkg" + value.removePrefix("content://$GOOGLE_GMS_PACKAGE")
        value in EXACT_GMS_STRINGS_TO_REDIRECT ||
            (value.startsWith("$GOOGLE_GMS_PACKAGE.") &&
                (value.endsWith(".service.START") || value.endsWith(".services.START"))) ->
            gmsPkg + value.removePrefix(GOOGLE_GMS_PACKAGE)
        else -> null
    }
}

private fun updateManifestForGmsCore(
    manifestXmlPath: String,
    vendorGroupId: String,
    gmsPackageName: String,
    originalSha1Lower: String,
) {
    val redirectedReadGservices = "$vendorGroupId.android.providers.gsf.permission.READ_GSERVICES"
    val extendedAccessPermission = "$vendorGroupId.gms.EXTENDED_ACCESS"

    resourceXml(manifestXmlPath).use { manifest ->
        val root = manifest.root

        // Rewrite READ_GSERVICES uses-permission to the GmsCore vendor permission.
        root.elements("uses-permission").forEach { perm ->
            if (perm.attribute("android:name") == GOOGLE_READ_GSERVICES_PERMISSION) {
                perm.setAttribute("android:name", redirectedReadGservices)
            }
        }

        // Ensure runtime account & microG extended-access permissions are declared.
        ensureUsesPermission(root, ANDROID_GET_ACCOUNTS_PERMISSION)
        ensureUsesPermission(root, extendedAccessPermission)

        // Ensure selected GmsCore vendor package is also visible under <queries>.
        val queriesElement = root.elements("queries").firstOrNull()
            ?: root.appendChild("queries")
        val hasGmsQuery = queriesElement.elements("package")
            .any { it.attribute("android:name") == gmsPackageName }
        if (!hasGmsQuery) {
            queriesElement.appendChild("package") {
                setAttribute("android:name", gmsPackageName)
            }
        }

        // Configure microG signature spoofing metadata inside <application>.
        val application = root.elements("application").firstOrNull()
            ?: throw PatchException("AndroidManifest.xml is missing <application> element.")

        upsertMetaData(application, "$gmsPackageName.SPOOFED_PACKAGE_NAME", Constants.PACKAGE_NAME)
        upsertMetaData(application, "$gmsPackageName.SPOOFED_PACKAGE_SIGNATURE", originalSha1Lower)
        upsertMetaData(application, "$vendorGroupId.MICROG_PACKAGE_NAME", gmsPackageName)
    }
}

private fun ensureUsesPermission(manifestRoot: XmlElement, permissionName: String) {
    val exists = manifestRoot.elements("uses-permission")
        .any { it.attribute("android:name") == permissionName }
    if (!exists) {
        manifestRoot.appendChild("uses-permission") {
            setAttribute("android:name", permissionName)
        }
    }
}

private fun upsertMetaData(application: XmlElement, name: String, value: String) {
    application.elements("meta-data")
        .filter { it.attribute("android:name") == name }
        .toList()
        .forEach { it.remove() }

    application.appendChild("meta-data") {
        setAttribute("android:name", name)
        setAttribute("android:value", value)
    }
}
