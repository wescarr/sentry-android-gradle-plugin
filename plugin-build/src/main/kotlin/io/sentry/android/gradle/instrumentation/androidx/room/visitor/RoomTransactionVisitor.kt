package io.sentry.android.gradle.instrumentation.androidx.room.visitor

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode

class RoomTransactionVisitor(
    className: String,
    api: Int,
    firstPassVisitor: MethodNode,
    private val originalVisitor: MethodVisitor,
    access: Int,
    descriptor: String?
) : AbstractRoomVisitor(
    className = className,
    api = api,
    originalVisitor = originalVisitor,
    access = access,
    descriptor = descriptor
) {

    private val label6 = Label()
    private val label7 = Label()
    private val label8 = Label()
    private val label9 = Label()

    private val labelsRemapTable = mutableMapOf<Label, Label>()
    private var skipVarVisit = false
    private var finallyVisitCount = 0

    init {
        val tryCatchBlock = firstPassVisitor.tryCatchBlocks.firstOrNull()
        if (tryCatchBlock != null) {
            labelsRemapTable[tryCatchBlock.start.label] = label0
            labelsRemapTable[tryCatchBlock.end.label] = label1
            labelsRemapTable[tryCatchBlock.handler.label] = label2
        }
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        if (opcode == Opcodes.INVOKEVIRTUAL) {
            when (name) {
                SET_TRANSACTION_SUCCESSFUL -> {
                    // the original method wants to return, but we intervene here to set status
                    originalVisitor.visitSetStatus(status = "OK", gotoIfNull = label6)
                    originalVisitor.visitLabel(label6)
                }
                END_TRANSACTION -> {
                    // room's finally block ends here, we add our code to finish the span

                    // we visit finally block 2 times - one for the positive path in control flow (try) one for negative (catch)
                    // hence we need to use different labels
                    val visitCount = ++finallyVisitCount
                    val label = if (visitCount == 1) label7 else label9
                    originalVisitor.visitFinallyBlock(gotoIfNull = label)
                    originalVisitor.visitLabel(label)
                }
            }
        }
    }

    override fun visitLabel(label: Label?) {
        // since we are rewriting try-catch blocks, we need to also remap the original labels with ours
        val remapped = labelsRemapTable.getOrDefault(label, label)

        // the original method does not have a catch block, but we add ours here
        if (remapped == label2 && !instrumenting.getAndSet(true)) {
            originalVisitor.visitCatchBlock(catchLabel = label2, throwLabel = label8)
            originalVisitor.visitStoreException(handler = label3, end = label4)
            instrumenting.set(false)
        } else {
            super.visitLabel(remapped)
        }
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        if (skipVarVisit) {
            skipVarVisit = false
            return
        }
        super.visitVarInsn(opcode, `var`)
    }

    override fun visitLocalVariable(
        name: String?,
        descriptor: String?,
        signature: String?,
        start: Label?,
        end: Label?,
        index: Int
    ) {
        val remappedStart = labelsRemapTable.getOrDefault(start, start)
        val remappedEnd = labelsRemapTable.getOrDefault(end, end)
        super.visitLocalVariable(name, descriptor, signature, remappedStart, remappedEnd, index)
    }

    companion object {
        private const val END_TRANSACTION = "endTransaction"
        private const val SET_TRANSACTION_SUCCESSFUL = "setTransactionSuccessful"
    }
}
