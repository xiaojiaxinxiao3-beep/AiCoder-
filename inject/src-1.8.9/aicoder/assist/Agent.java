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
    private static final class Patch {
        final String owner, method, desc, hook;
        final String[] arguments;

        Patch(String owner, String method, String desc, String hook, String... arguments) {
            this.owner = owner;
            this.method = method;
            this.desc = desc;
            this.hook = hook;
            this.arguments = arguments;
        }
    }

    private static final String GAME_VERSION = "1.8.9";
    private static boolean loaded;

    /** Where the reason for a failed injection is left, so the injector can print it instead of a bare "failed". */
    static final String ERROR_PROPERTY = "aicoder.assist.error";

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        try {
            System.clearProperty(ERROR_PROPERTY);
            // A licence key from the injector. A plain system property, because under Fabric the client's
            // classes live in another class loader and this is the one place both can read.
            if (args != null && !args.trim().isEmpty()) System.setProperty("aicoder.assist.license.key", args.trim());
            start(inst);
        } catch (Throwable t) {
            System.setProperty(ERROR_PROPERTY, String.valueOf(t.getMessage() != null ? t.getMessage() : t));
            throw t;
        }
    }

    private static void start(Instrumentation inst) throws Exception {
        // 1.8.9 carries no version file, so the game is known by its classes: the obfuscated name its main
        // class has in 1.8.9, with the methods this build patches. The plain game keeps its classes on the
        // ordinary class path; started through LaunchWrapper (OptiFine does that) they live in its own loader.
        Names names = Names.load();
        Class<?> main = null;
        boolean forge = false;
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c.getName().equals(names.get("Minecraft").replace('/', '.'))) main = c;
            if (c.getName().equals("net.minecraft.client.Minecraft")) forge = true;
        }
        if (main == null) {
            throw new IllegalStateException("this download is for Minecraft " + GAME_VERSION + (forge
                ? ", and the running game is a Forge one: Forge renames the game's classes, which this download does not follow (the plain game and OptiFine are supported); "
                : ", but the running game does not look like it; ") + describeGame());
        }
        try {
            main.getDeclaredMethod(names.get("Minecraft.runTick"));
            main.getDeclaredMethod(names.get("Minecraft.dispatchKeypresses"));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("this download is for Minecraft " + GAME_VERSION + ", but the running game is another version; " + describeGame());
        }
        String launched = launchedVersion(main, names);
        java.util.regex.Matcher other = java.util.regex.Pattern.compile("(?<![0-9.])1\\.[0-9]+(\\.[0-9]+)?(?![0-9.])").matcher(launched);
        if (!launched.contains(GAME_VERSION) && other.find()) {
            throw new IllegalStateException("this download is for Minecraft " + GAME_VERSION + ", but the running game says it is " + launched + "; " + describeGame());
        }

        if (loaded) {
            System.out.println("[" + Client.NAME + "] already injected.");
            return;
        }
        loaded = true;

        ClassLoader game = main.getClassLoader();
        if (game != null && game != ClassLoader.getSystemClassLoader()) joinLaunchWrapper(game);

        List<Patch> patches = new ArrayList<>();
        patches.add(patch(names, "Minecraft.runTick", "tick"));
        patches.add(patch(names, "Minecraft.dispatchKeypresses", "key"));
        patches.add(patch(names, "EntityRenderer.updateCameraAndRender", "frame"));

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

    /** The version name the launcher started the game under, e.g. "1.8.9" or "1.8.9-OptiFine_HD_U_M5". */
    private static String launchedVersion(Class<?> main, Names names) {
        try {
            Object game = main.getDeclaredMethod(names.get("Minecraft.getMinecraft")).invoke(null);
            java.lang.reflect.Field field = main.getDeclaredField(names.get("Minecraft.launchedVersion"));
            field.setAccessible(true);
            return String.valueOf(field.get(game));
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * LaunchWrapper (what OptiFine starts the game through) loads the game's classes in a loader of its
     * own, which cannot see this jar. The jar is added to that loader's list, through the call the
     * wrapper itself offers; the names are the same as in the plain game.
     */
    private static void joinLaunchWrapper(ClassLoader game) throws Exception {
        if (!game.getClass().getName().equals("net.minecraft.launchwrapper.LaunchClassLoader")) {
            throw new IllegalStateException("the game runs inside a class loader this download does not know (" + game.getClass().getName() + "); " + describeGame());
        }
        java.net.URL self = Agent.class.getProtectionDomain().getCodeSource().getLocation();
        game.getClass().getMethod("addURL", java.net.URL.class).invoke(game, self);
        System.out.println("[" + Client.NAME + "] running under LaunchWrapper (OptiFine)");
    }

    /** What the game was started with, for the reason shown when injection is refused. */
    private static String describeGame() {
        String command = System.getProperty("sun.java.command", "");
        String main = command.isEmpty() ? "?" : command.split(" ")[0];
        return "started through " + main + ", Java " + System.getProperty("java.version");
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
