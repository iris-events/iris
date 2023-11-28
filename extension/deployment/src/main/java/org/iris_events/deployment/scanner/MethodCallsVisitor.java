package org.iris_events.deployment.scanner;

import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodCallsVisitor extends ClassVisitor {
    private final MethodInfo targetMethod;
    private boolean callsTarget;

    protected MethodCallsVisitor(MethodInfo targetMethod) {
        super(Opcodes.ASM9);
        this.targetMethod = targetMethod;
        this.callsTarget = false;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(Opcodes.ASM9) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor,
                    boolean isInterface) {
                // Check if the visited method matches the targetMethod
                if (methodName.equals(targetMethod.name())
                        && methodDescriptor.equals(targetMethod.descriptor())
                        && owner.equals(targetMethod.declaringClass().name().toString())) {
                    callsTarget = true;
                    // Optionally, you could log or perform actions here
                    System.out.println("Method calls the target method: " + targetMethod.name());
                }
            }
        };
    }

    public boolean callsTargetMethod() {
        return callsTarget;
    }
}
