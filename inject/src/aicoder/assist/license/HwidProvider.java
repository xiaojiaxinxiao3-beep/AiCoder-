package aicoder.assist.license;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * A fingerprint of this computer for the licence: the SHA-256 of a few identifiers of the machine itself.
 *
 * <p>What goes in: the machine id the operating system keeps, the board's UUID and the processor's id or
 * model. What never goes in: the user name, the computer's name, file paths, network addresses, or
 * anything read from the user's files. Only the hash leaves this class, and the hash cannot be turned
 * back into those identifiers.
 *
 * <p>When nothing at all can be read the result is null rather than a made-up value: a fingerprint that
 * is the same on every such computer would let one key be shared between them.
 */
public final class HwidProvider {
    private static String cached;

    private HwidProvider() {
    }

    /** 64 hex characters, or null when this computer gives nothing to identify it by. Slow the first time: it may start a system command. */
    public static synchronized String get() {
        if (cached != null) return cached;

        List<String> parts = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) windows(parts);
            else if (os.contains("mac")) mac(parts);
            else linux(parts);
        } catch (Throwable ignored) {
            // Whatever was collected before the failure still counts
        }
        if (parts.isEmpty()) return null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("aicoder-assist-hwid-v1".getBytes(StandardCharsets.UTF_8));
            for (String part : parts) {
                digest.update((byte) 0);
                digest.update(part.getBytes(StandardCharsets.UTF_8));
            }
            cached = HexFormat.of().formatHex(digest.digest());
            return cached;
        } catch (Exception e) {
            return null;
        }
    }

    private static void windows(List<String> parts) {
        // The id Windows gives the installation; readable without administrator rights
        add(parts, "guid", valueAfter(run("reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid"), "REG_SZ"));
        // The board's UUID and the processor's id
        String cim = run("powershell", "-NoProfile", "-NonInteractive", "-Command",
            "(Get-CimInstance Win32_ComputerSystemProduct).UUID; (Get-CimInstance Win32_Processor | Select-Object -First 1).ProcessorId");
        if (cim != null) {
            String[] lines = cim.strip().split("\\R+");
            if (lines.length > 0) add(parts, "board", lines[0]);
            if (lines.length > 1) add(parts, "cpu", lines[1]);
        }
    }

    private static void mac(List<String> parts) {
        String out = run("ioreg", "-rd1", "-c", "IOPlatformExpertDevice");
        add(parts, "uuid", quotedAfter(out, "IOPlatformUUID"));
        add(parts, "serial", quotedAfter(out, "IOPlatformSerialNumber"));
    }

    private static void linux(List<String> parts) {
        add(parts, "machine", read("/etc/machine-id"));
        add(parts, "board", read("/sys/class/dmi/id/product_uuid"));
        add(parts, "serial", read("/sys/class/dmi/id/board_serial"));
        String cpu = read("/proc/cpuinfo");
        if (cpu != null) {
            for (String line : cpu.split("\\R")) {
                if (line.startsWith("model name")) {
                    add(parts, "cpu", line.substring(line.indexOf(':') + 1));
                    break;
                }
            }
        }
    }

    /** Placeholder values some boards report instead of a real id are left out. */
    private static void add(List<String> parts, String name, String value) {
        if (value == null) return;
        value = value.strip();
        String lower = value.toLowerCase(Locale.ROOT);
        if (value.length() < 4 || lower.contains("to be filled") || lower.equals("none") || lower.equals("default string")
            || value.replace("0", "").replace("-", "").isEmpty() || value.replace("F", "").replace("f", "").replace("-", "").isEmpty()) return;
        parts.add(name + "=" + value);
    }

    private static String read(String path) {
        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8).strip();
        } catch (Exception e) {
            return null;
        }
    }

    private static String run(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            process.getOutputStream().close();
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && out.length() < 20000) out.append(line).append('\n');
            }
            if (!process.waitFor(8, TimeUnit.SECONDS)) process.destroyForcibly();
            return out.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String valueAfter(String text, String marker) {
        if (text == null) return null;
        int at = text.indexOf(marker);
        if (at < 0) return null;
        String rest = text.substring(at + marker.length());
        int end = rest.indexOf('\n');
        return (end < 0 ? rest : rest.substring(0, end)).strip();
    }

    private static String quotedAfter(String text, String name) {
        if (text == null) return null;
        int at = text.indexOf("\"" + name + "\"");
        if (at < 0) return null;
        int open = text.indexOf('"', text.indexOf('=', at));
        int close = open < 0 ? -1 : text.indexOf('"', open + 1);
        return close < 0 ? null : text.substring(open + 1, close);
    }
}
