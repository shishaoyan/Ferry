package com.ssy.ferry_transform_trace

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * 2019-09-26
 * @author Mr.S
 */
class ToastMethodVisitor(api: Int, mv: MethodVisitor?) : MethodVisitor(api, mv) {


    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        desc: String?,
        itf: Boolean
    ) {
        if (opcode == Opcodes.INVOKEVIRTUAL && owner.equals(TOAST) && desc.equals("()V")) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SHADOW_TOAST, "show", "(L$TOAST;)V",false)
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf)
        }

    }


}

private const val TOAST = "android/widget/Toast"

private const val SHADOW_TOAST = "com/ssy/ferry_android_instrument/ShadowToast"