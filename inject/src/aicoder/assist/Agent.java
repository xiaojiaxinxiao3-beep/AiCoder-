package aicoder.assist;

import aicoder.assist.asm.ClassReader;
import aicoder.assist.asm.ClassVisitor;
import aicoder.assist.asm.ClassWriter;
import aicoder.assist.asm.MethodVisitor;
import aicoder.assist.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point inside the game. The injector asks the running JVM to load this jar; the JVM then calls
 * {@link #agentmain}. From here a handful of the game's loaded classes are rewritten in place so that they
 * call into {@link Hooks} at the start of chosen methods. Only method bodies change, which is all a
 * running JVM allows.
 */
public final class Agent {
    /**
     * One call to insert at the start of a game method: which method, what to call, and which of its
     * arguments to pass on. An argument is a letter and a local variable slot: "A2" is the object in slot 2,
     * "I3" the int in slot 3. Slot 0 is {@code this}; a long or double takes two slots.
     */
    private record Patch(String owner, String method, String desc, String hook, String... arguments) {
    }

    private static boolean loaded;

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        if (loaded) {
            System.out.println("[" + Client.NAME + "] already injected.");
            return;
        }
        loaded = true;

        Names names = Names.load();
        List<Patch> patches = new ArrayList<>();
        patches.add(patch(names, "Minecraft.tick", "tick"));
        patches.add(patch(names, "Minecraft.startAttack", "attack"));
        patches.add(patch(names, "GameRenderer.render", "frame"));
        patches.add(patch(names, "MultiPlayerGameMode.useItemOn", "useBlock", "A2", "A3"));
        patches.add(patch(names, "MultiPlayerGameMode.useItem", "useItem", "A2"));
        patches.add(patch(names, "ClientLevel.addEntity", "entityAdded", "A1"));
        patches.add(patch(names, "KeyboardHandler.keyPress", "key", "I3", "A4"));

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

        int done = 0, wanted = 0;
        for (Class<?> c : inst.getAllLoadedClasses()) {
            boolean target = false;
            for (Patch p : patches) target |= c.getName().equals(p.owner.replace('/', '.'));
            if (!target) continue;

            wanted++;
            try {
                inst.retransformClasses(c);
                done++;
            } catch (Throwable t) {
                System.out.println("[" + Client.NAME + "] could not patch " + c.getName() + ": " + t);
            }
        }
        System.out.println("[" + Client.NAME + "] injected, patched " + done + " of " + wanted + " classes");
    }

    private static Patch patch(Names names, String key, String hook, String... slots) {
        String owner = key.substring(0, key.indexOf('.'));
        return new Patch(names.get(owner), names.get(key), names.get(key + ".desc"), hook, slots);
    }

    private static final class Inserter extends MethodVisitor {
        private final Patch patch;

        Inserter(MethodVisitor mv, Patch patch) {
            super(Opcodes.ASM9, mv);
            this.patch = patch;
        }

        @Override
        public void visitCode() {
            super.visitCode();

            StringBuilder desc = new StringBuilder("(");
            for (String argument : patch.arguments) {
                boolean isInt = argument.charAt(0) == 'I';
                super.visitVarInsn(isInt ? Opcodes.ILOAD : Opcodes.ALOAD, Integer.parseInt(argument.substring(1)));
                desc.append(isInt ? "I" : "Ljava/lang/Object;");
            }
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "aicoder/assist/Hooks", patch.hook, desc.append(")V").toString(), false);
        }
    }
}
