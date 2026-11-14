package aicoder.inject;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;

/** Finds the running Minecraft and asks its JVM to load the payload jar. */
public final class Injector {
    public static void main(String[] args) throws Exception {
        String payload = new File(args.length > 0 ? args[0] : "aicoder-payload.jar").getAbsolutePath();

        for (VirtualMachineDescriptor vm : VirtualMachine.list()) {
            if (!vm.displayName().contains("net.minecraft.client.main.Main")) continue;

            System.out.println("Minecraft found, process " + vm.id());
            VirtualMachine machine = VirtualMachine.attach(vm);
            try {
                machine.loadAgent(payload);
            } finally {
                machine.detach();
            }
            System.out.println("Injected.");
            return;
        }
        System.out.println("Minecraft is not running.");
    }
}
