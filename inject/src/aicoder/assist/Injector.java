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
    public static void main(String[] args) throws Exception {
        String payload = new File(Injector.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();

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
                System.out.println("Minecraft found (process " + id + ").");
                machine.loadAgent(payload);
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
