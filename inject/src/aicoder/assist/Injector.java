package aicoder.assist;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Asks the running Minecraft's JVM to load this same jar as the payload.
 *
 * <p>Given process ids (the Windows launcher passes those of every running Java program), each is tried in
 * turn. Without arguments the JVM's own list of Java programs is used; that list is empty for games started
 * with performance data switched off, which several third-party launchers do, so ids are the reliable way.
 */
public final class Injector {
    /**
     * The licence key for a build that needs one: from AiCoderAssist-license.txt beside this file, or asked
     * for here and saved there. Null when this build has no licence server set, or no key was given.
     */
    private static String licenceKey(File folder) {
        if (Injector.class.getResource("/aicoder/assist/license.properties") == null) return null;

        File file = new File(folder, "AiCoderAssist-license.txt");
        try {
            for (String line : java.nio.file.Files.readAllLines(file.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                String key = line.replace("\uFEFF", "").strip();
                if (key.matches("[A-Za-z0-9_-]{1,128}")) return key;
            }
        } catch (Exception ignored) {
            // No file yet
        }

        try {
            if (System.console() == null) return null;
            System.out.print("Licence key: ");
            String key = System.console().readLine();
            if (key == null || !(key = key.strip()).matches("[A-Za-z0-9_-]{1,128}")) return null;
            java.nio.file.Files.writeString(file.toPath(), key + System.lineSeparator(), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Saved to " + file.getName() + ".");
            return key;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        String payload = new File(Injector.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();

        // Everything shown in the window also goes to a text file beside the jar, so it can be sent along
        try {
            java.io.PrintStream file = new java.io.PrintStream(new java.io.FileOutputStream(new File(new File(payload).getParentFile(), "AiCoderAssist-log.txt")), true, "UTF-8");
            java.io.PrintStream window = System.out;
            System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
                @Override public void write(int b) { window.write(b); file.write(b); }
                @Override public void write(byte[] b, int off, int len) { window.write(b, off, len); file.write(b, off, len); }
                @Override public void flush() { window.flush(); file.flush(); }
            }, true));
        } catch (Exception ignored) {
            // A folder that cannot be written to: the window alone will do
        }

        // The game is told where this jar is as plain text, and on Windows a folder name outside ASCII
        // (Chinese, accents) can arrive garbled, so the game finds no file. Use a copy with a plain path then.
        if (!payload.chars().allMatch(c -> c < 128)) {
            for (String folder : new String[]{System.getProperty("java.io.tmpdir"), "C:\\Users\\Public", "C:\\ProgramData"}) {
                try {
                    if (folder == null || !folder.chars().allMatch(c -> c < 128) || !new File(folder).isDirectory()) continue;
                    File copy = new File(new File(folder, "AiCoderAssist"), new File(payload).getName().replaceAll("[^A-Za-z0-9._-]", "_"));
                    copy.getParentFile().mkdirs();
                    java.nio.file.Files.copy(new File(payload).toPath(), copy.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("This folder's name has non-English characters; using a copy at " + copy.getAbsolutePath());
                    payload = copy.getAbsolutePath();
                    break;
                } catch (Exception ignored) {
                    // Try the next place
                }
            }
        }

        String licenceKey = licenceKey(new File(Injector.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile());

        List<String> ids = new ArrayList<>();
        for (String arg : args) {
            if (arg.matches("\\d+")) ids.add(arg);
        }
        if (ids.isEmpty()) {
            for (VirtualMachineDescriptor vm : VirtualMachine.list()) ids.add(vm.id());
        }

        System.out.println("AiCoder Assist injector for Minecraft 1.21.11, running on Java " + System.getProperty("java.version"));
        String self = String.valueOf(ProcessHandle.current().pid());
        boolean sawGame = false;

        for (String id : ids) {
            if (id.equals(self)) continue;

            VirtualMachine machine;
            try {
                machine = VirtualMachine.attach(id);
            } catch (Throwable e) {
                // Also reached when this Java cannot attach at all (a runtime without the attach module)
                System.out.println("Process " + id + ": could not attach (" + e + ")");
                continue;
            }

            try {
                Properties properties = machine.getSystemProperties();
                String command = properties.getProperty("sun.java.command", "") + " " + properties.getProperty("java.class.path", "");
                if (!command.toLowerCase(Locale.ROOT).contains("minecraft")) continue;

                sawGame = true;
                System.out.println("Minecraft found (process " + id + "): " + properties.getProperty("sun.java.command", "").split(" ")[0] + ", Java " + properties.getProperty("java.version", "?"));
                machine.loadAgent(payload, licenceKey);
                System.out.println("AiCoder Assist is in. Press Right Shift in the game to open the menu.");
                System.exit(0);
            } catch (Exception e) {
                String reason = null;
                try {
                    reason = machine.getSystemProperties().getProperty(Agent.ERROR_PROPERTY);
                } catch (Exception ignored) {
                    // The plain message below is all there is
                }
                System.out.println("Could not inject into process " + id + ".");
                System.out.println("Reason: " + (reason != null ? reason : e.getMessage()));
                System.out.println("(" + e + ")");
                if (reason != null && reason.contains("this download is for Minecraft") && !reason.contains("for Minecraft 1.21.11,")) {
                    // The game still holds the classes of the package tried before; nothing loaded now can replace them
                    System.out.println();
                    System.out.println("A package for ANOTHER game version was run on this game earlier.");
                    System.out.println("Close Minecraft completely, start it again, and run only this one.");
                } else {
                    System.out.println("This build only works with Minecraft 1.21.11 started without mods.");
                    System.out.println("If a package for another version was run on this game before, close Minecraft,");
                    System.out.println("start it again and run only this one. Otherwise send this window as a screenshot.");
                }
            } finally {
                try {
                    machine.detach();
                } catch (Exception ignored) {
                    // Nothing to do if the game went away meanwhile
                }
            }
        }

        if (sawGame) System.exit(1);
        System.out.println("No running Minecraft was found among " + ids.size() + " Java program(s). Start Minecraft 1.21.11 first, then run this again.");
        System.exit(2);
    }
}
