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

    private static final String GAME_VERSION = "1.21.11";
    private static boolean loaded;

    /** Where the reason for a failed injection is left, so the injector can print it instead of a bare "failed". */
    static final String ERROR_PROPERTY = "aicoder.assist.error";

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        try {
            System.clearProperty(ERROR_PROPERTY);
            // A licence key from the injector. A plain system property, because under Fabric the client's
            // classes live in another class loader and this is the one place both can read.
            if (args != null && !args.isBlank()) System.setProperty("aicoder.assist.license.key", args.strip());
            start(inst);
        } catch (Throwable t) {
            System.setProperty(ERROR_PROPERTY, String.valueOf(t.getMessage() != null ? t.getMessage() : t));
            throw t;
        }
    }

    private static void start(Instrumentation inst) throws Exception {
        // Where the game's classes live: the ordinary class path for the plain game, a loader of its own under Fabric
        ClassLoader game = ClassLoader.getSystemClassLoader();
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c.getName().equals("net.minecraft.client.main.Main") && c.getClassLoader() != null) game = c.getClassLoader();
        }
        boolean wrapped = game != ClassLoader.getSystemClassLoader();

        // The names this build knows belong to one game version only; in any other they would point at the wrong things
        String version = gameVersion(game);
        if (!GAME_VERSION.equals(version)) {
            throw new IllegalStateException("this download is for Minecraft " + GAME_VERSION + ", but the running game says it is " + version
                + (version.equals("unknown") ? " (no version.json where a plain game has one)" : "") + "; " + describeGame());
        }

        if (loaded) {
            System.out.println("[" + Client.NAME + "] already injected.");
            return;
        }
        loaded = true;

        Names names = wrapped ? joinFabric() : Names.load();
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

    /**
     * The version the running game says it is, read from the version.json inside its jar. Every
     * version.json on the class path is looked at: some launchers put other jars that carry one
     * (an installer, a wrapper) ahead of the game. The build's own version wins if any of them names it.
     */
    private static String gameVersion(ClassLoader game) {
        java.util.List<String> found = new ArrayList<>();
        try {
            java.util.Enumeration<java.net.URL> all = game.getResources("version.json");
            while (all.hasMoreElements()) {
                try (java.io.InputStream in = all.nextElement().openStream()) {
                    String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
                    if (m.find()) found.add(m.group(1));
                } catch (Exception ignored) {
                    // An unreadable one tells nothing either way
                }
            }
        } catch (Exception ignored) {
            // Falls through to "unknown"
        }
        if (found.contains(GAME_VERSION)) return GAME_VERSION;
        return found.isEmpty() ? "unknown" : String.join(" / ", found);
    }

    /**
     * Under Fabric Loader the game's classes have intermediary names and are loaded by Fabric's own class
     * loader, which cannot see this jar. So the copy of the client converted to those names (kept inside
     * this jar) is written out and added to Fabric's class path, through the call Fabric itself uses for mods.
     */
    private static Names joinFabric() throws Exception {
        Class<?> base;
        try {
            base = Class.forName("net.fabricmc.loader.impl.launch.FabricLauncherBase");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("the game runs under a mod loader this download does not support (only the plain game and Fabric); " + describeGame());
        }

        java.nio.file.Path copy = java.nio.file.Files.createTempFile("aicoder-assist-fabric-", ".jar");
        copy.toFile().deleteOnExit();
        try (java.io.InputStream in = Agent.class.getResourceAsStream("/aicoder/assist/fabric-payload.jar")) {
            if (in == null) throw new IllegalStateException("this download has no Fabric support; " + describeGame());
            java.nio.file.Files.copy(in, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        Object launcher = base.getMethod("getLauncher").invoke(null);
        Class.forName("net.fabricmc.loader.impl.launch.FabricLauncher").getMethod("addToClassPath", java.nio.file.Path.class, String[].class)
            .invoke(launcher, copy, new String[0]);
        System.out.println("[" + Client.NAME + "] running under Fabric Loader");
        return Names.load("names-intermediary.properties");
    }

    /** What the game was started with, for the reason shown when injection is refused. */
    private static String describeGame() {
        String command = System.getProperty("sun.java.command", "");
        String main = command.isEmpty() ? "?" : command.split(" ")[0];
        boolean vanillaMain = main.equals("net.minecraft.client.main.Main");
        return "started through " + main + (vanillaMain ? "" : " (not the plain game: a mod loader or a launcher wrapper)") + ", Java " + System.getProperty("java.version");
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
