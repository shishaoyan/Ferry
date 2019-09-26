package com.ssy.ferry_transform_trace

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * 2019-09-26
 * @author Mr.S
 */
class ToastMethodVisitor(api: Int, mv: MethodVisitor?) : MethodVisitor(api, mv) {

//    mv.visitMethodInsn(INVOKESTATIC, "android/widget/Toast", "makeText", "(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;", false);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "android/widget/Toast", "show", "()V", false);

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, desc: String?) {
        if (opcode == Opcodes.INVOKEVIRTUAL && owner.equals(TOAST) && desc.equals("()V")) {
            mv.visitFieldInsn(Opcodes.INVOKESTATIC, SHADOW_TOAST, "show", "(L$TOAST;)V")
        } else {
            super.visitFieldInsn(opcode, owner, name, desc)
        }


    }


}

private const val TOAST = "android/widget/Toast"

private const val SHADOW_TOAST = "com/ssy/ferry/ShadowToast"