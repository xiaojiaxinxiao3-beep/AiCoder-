package aicoder.assist;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;

/** Finds the running Minecraft and asks its JVM to load this same jar as the payload. */
public final class Injector {
    public static void main(String[] args) throws Exception {
        String payload = args.length > 0
            ? new File(args[0]).getAbsolutePath()
            : new File(Injector.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();

        for (VirtualMachineDescriptor vm : VirtualMachine.list()) {
            if (!vm.displayName().contains("net.minecraft.client.main.Main")) continue;

            System.out.println("Minecraft found, process " + vm.id());
            VirtualMachine machine = VirtualMachine.attach(vm);
            try {
                machine.loadAgent(payload);
            } finally {
                machine.detach();
            }
            System.out.println("AiCoder Assist injected. Press Right Shift in the game.");
            return;
        }
        System.out.println("Minecraft is not running. Start the game first, then run this again.");
    }
}
