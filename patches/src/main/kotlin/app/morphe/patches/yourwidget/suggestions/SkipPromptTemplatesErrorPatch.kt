package app.morphe.patches.yourwidget.suggestions

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patches.yourwidget.Constants
import app.morphe.patches.yourwidget.suggestedWidgetPromptsCoroutineFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import kotlin.math.min

private const val ANCHOR_HANDLE_FETCH_ERROR = "handleFetchError"
private const val ANCHOR_PERFORM_FETCH = "performFetchAndStoreData\$<anonymous>\$<anonymous>"
private const val BRANCH_SEARCH_WINDOW = 4

@Suppress("unused")
val skipPromptTemplatesErrorPatch = patch(
    name = "Skip prompt templates loading error",
    description = "Prevents SuggestedWidgetPromptsDataServiceImpl from failing with PERMISSION_DENIED " +
        "so bundled offline prompt suggestions still load in the configurator carousel.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_YOURWIDGET)

    execute {
        val coroutineMethod = suggestedWidgetPromptsCoroutineFingerprint.method
        val insns = coroutineMethod.instructions.toList()

        // Branch 1: In handleFetchError, skip the PERMISSION_DENIED check unconditionally
        // so error state is not latched into state.b.
        val handleFetchAnchorIdx = findStringInstructionIndex(insns, ANCHOR_HANDLE_FETCH_ERROR)
            ?: throw PatchException(
                "Could not locate string anchor '$ANCHOR_HANDLE_FETCH_ERROR' in prompt suggestions coroutine.",
            )
        val handleFetchBranchIdx = findAdjacentBranchIndex(
            insns = insns,
            anchorIndex = handleFetchAnchorIdx,
            targetOpcode = Opcode.IF_NE,
        ) ?: throw PatchException(
            "Could not locate IF_NE PERMISSION_DENIED branch near '$ANCHOR_HANDLE_FETCH_ERROR'.",
        )
        val handleFetchSkipTarget =
            (coroutineMethod.getInstruction(handleFetchBranchIdx) as BuilderOffsetInstruction).target
        coroutineMethod.replaceInstruction(
            handleFetchBranchIdx,
            BuilderInstruction10t(Opcode.GOTO, handleFetchSkipTarget),
        )

        // Branch 2: In performFetchAndStoreData$<anonymous>$<anonymous>, NOP out the
        // PERMISSION_DENIED rethrow guard so execution falls back to bundled static data.
        val performFetchAnchorIdx = findStringInstructionIndex(insns, ANCHOR_PERFORM_FETCH)
            ?: throw PatchException(
                "Could not locate string anchor '$ANCHOR_PERFORM_FETCH' in prompt suggestions coroutine.",
            )
        val performFetchBranchIdx = findAdjacentBranchIndex(
            insns = insns,
            anchorIndex = performFetchAnchorIdx,
            targetOpcode = Opcode.IF_EQ,
        ) ?: throw PatchException(
            "Could not locate IF_EQ PERMISSION_DENIED branch near '$ANCHOR_PERFORM_FETCH'.",
        )
        coroutineMethod.replaceInstruction(
            performFetchBranchIdx,
            BuilderInstruction10x(Opcode.NOP),
        )
    }
}

private fun findStringInstructionIndex(
    insns: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>,
    target: String,
): Int? = insns.indexOfFirst { insn ->
    (insn.opcode == Opcode.CONST_STRING || insn.opcode == Opcode.CONST_STRING_JUMBO) &&
        ((insn as? ReferenceInstruction)?.reference as? StringReference)?.string == target
}.takeIf { it >= 0 }

private fun findAdjacentBranchIndex(
    insns: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>,
    anchorIndex: Int,
    targetOpcode: Opcode,
): Int? {
    val searchEnd = min(insns.lastIndex, anchorIndex + BRANCH_SEARCH_WINDOW)
    for (instructionIndex in (anchorIndex + 1)..searchEnd) {
        if (insns[instructionIndex].opcode == targetOpcode) {
            return instructionIndex
        }
    }
    return null
}
