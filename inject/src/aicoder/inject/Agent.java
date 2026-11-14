package aicoder.inject;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point inside the game. The injector asks the running JVM to load this jar; the JVM then calls
 * {@link #agentmain}. From here the game's own classes are rewritten in place so that they call into
 * {@link Hook} at a few chosen points. Only method bodies change, which is all a running JVM allows.
 */
public final class Agent {
    /** One call to insert: into which method, at its start or before every return, and what to call. */
    private record Patch(String owner, String method, String desc, boolean atReturn, String hook, String hookDesc, int[] loads) {
    }

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        Names names = Names.load();
        List<Patch> patches = new ArrayList<>();

        patches.add(new Patch(names.get("Minecraft"), names.get("Minecraft.tick"), "()V", false, "tick", "()V", new int[0]));
        patches.add(new Patch(names.get("Screen"), names.get("Screen.renderWithTooltipAndSubtitles"), names.get("Screen.renderWithTooltipAndSubtitles.desc"),
            true, "overlay", "(Ljava/lang/Object;)V", new int[]{1}));
        patches.add(new Patch(names.get("Gui"), names.get("Gui.render"), names.get("Gui.render.desc"),
            true, "overlay", "(Ljava/lang/Object;)V", new int[]{1}));

        Hook.init(names);

        ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> redefined, ProtectionDomain domain, byte[] bytes) {
                boolean wanted = false;
                for (Patch p : patches) wanted |= p.owner.equals(className);
                if (!wanted) return null;

                try {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                            for (Patch p : patches) {
                                if (p.owner.equals(className) && p.method.equals(name) && p.desc.equals(desc)) mv = new Inserter(mv, p);
                            }
                            return mv;
                        }
                    }, 0);
                    return writer.toByteArray();
                } catch (Throwable t) {
                    t.printStackTrace();
                    return null;
                }
            }
        };

        inst.addTransformer(transformer, true);
        List<Class<?>> targets = new ArrayList<>();
        for (Class<?> c : inst.getAllLoadedClasses()) {
            for (Patch p : patches) if (c.getName().equals(p.owner.replace('/', '.'))) targets.add(c);
        }
        int patched = 0;
        for (Class<?> target : targets) {
            try {
                inst.retransformClasses(target);
                patched++;
            } catch (Throwable t) {
                System.out.println("[AiCoder] could not patch " + target.getName() + ": " + t);
            }
        }
        System.out.println("[AiCoder] injected, patched " + patched + " of " + targets.size() + " classes");
    }

    private static final class Inserter extends MethodVisitor {
        private final Patch patch;

        Inserter(MethodVisitor mv, Patch patch) {
            super(Opcodes.ASM9, mv);
            this.patch = patch;
        }

        private void call() {
            for (int slot : patch.loads) super.visitVarInsn(Opcodes.ALOAD, slot);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "aicoder/inject/Hook", patch.hook, patch.hookDesc, false);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (!patch.atReturn) call();
        }

        @Override
        public void visitInsn(int opcode) {
            if (patch.atReturn && opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) call();
            super.visitInsn(opcode);
        }
    }
}
